package com.blindtechnexus.freeeloquencetts

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object UserDictionary {

	private const val NAME = "words.dic"
	private const val VERSION_NAME = "words.dic.ver"
	private const val MAX_LINE_LENGTH = 8192

	const val MAX_ENTRIES = 1_000_000

	private const val MAX_INDEX_BYTES = 96L * 1024 * 1024

	private val versionCount = AtomicInteger(0)

	@Volatile
	private var versionLoaded = false

	val version: Int get() = versionCount.get()

	class Snapshot(
		val version: Int,
		val index: WordIndex,
		val kept: Int,
		val dropped: Int
	)

	@Volatile
	private var builtKey: String? = null

	@Volatile
	private var builtIndex: WordIndex = WordIndex.EMPTY

	@Volatile
	private var builtKept: Int = 0

	@Volatile
	private var builtDropped: Int = 0

	private val buildLock = Any()

	private val builder: ExecutorService = Executors.newSingleThreadExecutor(
		ThreadFactory { work -> Thread(work, "evv-dictionary").apply { isDaemon = true } }
	)

	fun indexBudgetChars(): Int {
		val heap = try {
			Runtime.getRuntime().maxMemory()
		} catch (_: Throwable) {
			128L * 1024 * 1024
		}
		val chars = (heap / 64).coerceAtLeast(0L)
		return chars.coerceIn(
			MIN_BUDGET_CHARS.toLong(),
			WordIndex.DEFAULT_BUDGET_CHARS.toLong() * 2L
		).toInt()
	}

	private const val MIN_BUDGET_CHARS = 256 * 1024

	private fun indexEntries(): Int =
		(indexBudgetChars() / 16).coerceAtLeast(64)

	fun file(context: Context): File = File(DirectBoot.dictionaries(context), NAME)

	fun fileForWriting(context: Context): File {
		val into = DirectBoot.dictionariesReady(context)
		return File(into, NAME)
	}

	private fun versionFile(context: Context): File =
		File(DirectBoot.dictionaries(context), VERSION_NAME)

	private fun ensureVersion(context: Context) {
		if (versionLoaded) return
		synchronized(versionCount) {
			if (versionLoaded) return
			versionCount.set(readVersion(context))
			versionLoaded = true
		}
	}

	private fun readVersion(context: Context): Int = try {
		val f = versionFile(context)
		if (f.exists() && f.canRead()) {
			f.readText(Charsets.UTF_8).trim().toIntOrNull() ?: 0
		} else {
			0
		}
	} catch (unreadable: Exception) {
		0
	}

	private fun bumpVersion(context: Context): Int {
		ensureVersion(context)
		val next = versionCount.incrementAndGet()
		try {
			versionFile(context).writeText(next.toString(), Charsets.UTF_8)
		} catch (unwritable: Exception) {

		}
		return next
	}

	fun snapshot(context: Context): Snapshot {
		ensureVersion(context)
		val want = version
		val key = want.toString() + ":" + indexBudgetChars() + ":" +
			ExtraDictionaries.combinedKey(context)
		if (builtKey == key) return Snapshot(want, builtIndex, builtKept, builtDropped)
		synchronized(buildLock) {
			if (builtKey == key) return Snapshot(want, builtIndex, builtKept, builtDropped)
			val made = build(context)

			builtIndex = made.index
			builtKept = made.kept
			builtDropped = made.dropped
			builtKey = key
			return Snapshot(want, made.index, made.kept, made.dropped)
		}
	}

	private class Built(val index: WordIndex, val kept: Int, val dropped: Int)

	private fun build(context: Context): Built {
		val builder = WordIndex.Builder(indexBudgetChars(), indexEntries())
		try {
			readInto(file(context), builder, MAX_INDEX_BYTES)
		} catch (broken: Exception) {

		}
		try {
			ExtraDictionaries.indexInto(context, builder, MAX_INDEX_BYTES)
		} catch (broken: Exception) {

		}
		return Built(builder.build(), builder.size, builder.overflowed)
	}

	fun rebuildAsync(context: Context) {
		val app = context.applicationContext
		try {
			builder.submit(Runnable {
				try {
					snapshot(app)
				} catch (broken: Exception) {

				}
			})
		} catch (refused: Exception) {

		}
	}

	fun words(context: Context): List<Pair<String, String>> = entries(context, MAX_ENTRIES)

	fun entries(context: Context, cap: Int): List<Pair<String, String>> {
		return try {
			readAll(file(context), cap.coerceAtLeast(0))
		} catch (e: Exception) {
			emptyList()
		}
	}

	fun count(context: Context): Int {
		var n = 0
		return try {
			val f = file(context)
			if (!f.exists() || !f.canRead()) return 0
			f.bufferedReader(Charsets.UTF_8).use { reader ->
				var line: String?
				while (reader.readLine().also { line = it } != null) {
					if (n >= MAX_ENTRIES) break
					if (parseLine(line ?: continue) != null) n++
				}
			}
			n
		} catch (unreadable: Exception) {
			n
		}
	}

	private fun readAll(f: File, cap: Int): List<Pair<String, String>> {
		val out = ArrayList<Pair<String, String>>(1024)
		if (!f.exists() || !f.canRead()) return out
		f.bufferedReader(Charsets.UTF_8).use { reader ->
			var line: String?
			while (reader.readLine().also { line = it } != null) {
				if (out.size >= cap) break
				val l = line ?: continue
				parseLine(l)?.let { out.add(it) }
			}
		}
		return out
	}

	private fun readInto(f: File, builder: WordIndex.Builder, byteCap: Long) {
		if (!f.exists() || !f.canRead()) return
		var bytes = 0L
		f.bufferedReader(Charsets.UTF_8).use { reader ->
			var line: String?
			while (reader.readLine().also { line = it } != null) {
				val l = line ?: continue
				bytes += l.length + 1
				if (bytes > byteCap || builder.roomLeft <= 0) break
				val split = l.indexOf('\t')
				if (split <= 0) continue
				val word = l.substring(0, split).trim()
				if (word.isEmpty()) continue
				val say = l.substring(split + 1).trim()
				if (say.isEmpty()) continue
				builder.add(word, say)
			}
		}
	}

	private fun parseLine(line: String): Pair<String, String>? {
		if (line.isEmpty() || line.length > MAX_LINE_LENGTH) return null
		val split = line.indexOf('\t')
		if (split <= 0) return null
		val word = line.substring(0, split).trim()
		val say = line.substring(split + 1).trim()
		if (word.isEmpty() || say.isEmpty()) return null
		if (word.length > WordIndex.MAX_KEY_CHARS) return null
		if (say.length > WordIndex.MAX_SAY_CHARS) return null
		return word to say
	}

	fun write(context: Context, entries: List<Pair<String, String>>): Boolean {
		return try {
			val target = fileForWriting(context)
			val tmp = File(target.parentFile, "${target.name}.tmp")
			tmp.bufferedWriter(Charsets.UTF_8).use { w ->
				var count = 0
				for ((word, say) in entries) {
					if (count++ >= MAX_ENTRIES) break
					if (word.isEmpty() || say.isEmpty()) continue
					if (word.length > WordIndex.MAX_KEY_CHARS) continue
					if (say.length > WordIndex.MAX_SAY_CHARS) continue

					val cleanWord = word.replace("\n", " ").replace("\r", " ").replace("\t", " ").trim()
					val cleanSay = say.replace("\n", " ").replace("\r", " ").trim()
					if (cleanWord.isEmpty() || cleanSay.isEmpty()) continue
					w.write(cleanWord)
					w.write('\t'.code)
					w.write(cleanSay)
					w.write('\n'.code)
				}
			}
			if (!replace(tmp, target)) return false
			bumpVersion(context)

			rebuildAsync(context)
			true
		} catch (e: Exception) {
			false
		}
	}

	private fun replace(tmp: File, target: File): Boolean {
		if (tmp.renameTo(target)) return true
		return try {
			tmp.inputStream().use { from ->
				target.outputStream().use { to -> from.copyTo(to) }
			}
			tmp.delete()
			true
		} catch (failed: Exception) {
			false
		}
	}

	private fun rewrite(context: Context, drop: (String) -> Boolean, add: List<Pair<String, String>>): Boolean {
		val target = fileForWriting(context)
		val tmp = File(target.parentFile, "${target.name}.tmp")
		try {
			var written = 0
			val from = file(context)
			val writer = tmp.bufferedWriter(Charsets.UTF_8)
			try {
				if (from.exists() && from.canRead()) {
					from.bufferedReader(Charsets.UTF_8).use { reader ->
						var line: String?
						while (reader.readLine().also { line = it } != null) {
							val l = line ?: continue
							val parsed = parseLine(l)
							if (parsed == null) {
								writer.write(l)
								writer.write('\n'.code)
								continue
							}
							if (drop(parsed.first)) continue
							if (written >= MAX_ENTRIES) continue
							writer.write(l)
							writer.write('\n'.code)
							written++
						}
					}
				}
				for ((word, say) in add) {
					if (written >= MAX_ENTRIES) break
					if (word.isEmpty() || say.isEmpty()) continue
					if (word.length > WordIndex.MAX_KEY_CHARS) continue
					if (say.length > WordIndex.MAX_SAY_CHARS) continue
					val cleanWord = word.replace("\n", " ").replace("\r", " ").replace("\t", " ").trim()
					val cleanSay = say.replace("\n", " ").replace("\r", " ").trim()
					if (cleanWord.isEmpty() || cleanSay.isEmpty()) continue
					writer.write(cleanWord)
					writer.write('\t'.code)
					writer.write(cleanSay)
					writer.write('\n'.code)
					written++
				}
			} finally {
				writer.close()
			}
			if (!replace(tmp, target)) return false
		} catch (broken: Exception) {
			runCatching { tmp.delete() }
			return false
		}
		bumpVersion(context)
		rebuildAsync(context)
		return true
	}

	fun add(context: Context, word: String, say: String): Boolean {
		if (word.isBlank() || say.isBlank()) return false
		return try {
			rewrite(context, { sameWord(it, word) }, listOf(word.trim() to say.trim()))
		} catch (e: Exception) {
			false
		}
	}

	fun remove(context: Context, word: String): Boolean {
		return try {
			rewrite(context, { sameWord(it, word) }, emptyList())
		} catch (e: Exception) {
			false
		}
	}

	fun replace(context: Context, oldWord: String, word: String, say: String): Boolean {
		if (word.isBlank() || say.isBlank()) return false
		return try {
			rewrite(
				context,
				{ sameWord(it, oldWord) || sameWord(it, word) },
				listOf(word.trim() to say.trim())
			)
		} catch (e: Exception) {
			false
		}
	}

	fun merge(context: Context, entries: List<Pair<String, String>>): Int {
		if (entries.isEmpty()) return 0
		return try {
			val given = LinkedHashMap<String, Pair<String, String>>()
			for ((word, say) in entries) {
				if (given.size >= MAX_ENTRIES) break
				val cleanWord = word.trim()
				val cleanSay = say.trim()
				if (cleanWord.isEmpty() || cleanSay.isEmpty()) continue
				if (cleanWord.length > WordIndex.MAX_KEY_CHARS) continue
				if (cleanSay.length > WordIndex.MAX_SAY_CHARS) continue
				given[foldKey(cleanWord)] = cleanWord to cleanSay
			}
			if (given.isEmpty()) return 0
			val seen = HashSet<String>(Math.min(given.size, 1 shl 16))
			val ok = rewrite(
				context,
				{ key ->
					val folded = foldKey(key)
					val had = given.containsKey(folded)
					if (had && seen.size < MAX_ENTRIES) seen.add(folded)
					had
				},
				given.values.toList()
			)
			if (!ok) 0 else (given.size - seen.size).coerceAtLeast(0)
		} catch (e: Exception) {
			0
		}
	}

	fun parse(text: String): List<Pair<String, String>> {
		if (text.isEmpty()) return emptyList()
		return try {
			val out = ArrayList<Pair<String, String>>()
			for (line in text.lineSequence()) {
				if (out.size >= MAX_ENTRIES) break
				parseLine(line)?.let { out.add(it) }
			}
			out
		} catch (e: Exception) {
			emptyList()
		}
	}

	fun parseStream(input: InputStream): List<Pair<String, String>> {
		return try {
			val out = ArrayList<Pair<String, String>>(1024)
			BufferedReader(input.reader(Charsets.UTF_8), 8192).use { reader ->
				var line: String?
				var bytes = 0L
				while (reader.readLine().also { line = it } != null) {
					if (out.size >= MAX_ENTRIES) break
					val l = line ?: continue
					bytes += l.length + 1
					if (bytes > MAX_INDEX_BYTES) break
					parseLine(l)?.let { out.add(it) }
				}
			}
			out
		} catch (e: Exception) {
			emptyList()
		}
	}

	fun apply(text: String, index: WordIndex): String {
		if (text.isEmpty() || index.isEmpty) return text
		return try {
			index.apply(text)
		} catch (broken: Throwable) {
			text
		}
	}

	fun apply(text: String, entries: List<Pair<String, String>>): String {
		if (text.isEmpty() || entries.isEmpty()) return text
		return try {
			val ready = lastListIndex
			if (ready != null && lastList === entries) return ready.apply(text)
			val made = WordIndex.of(entries)
			lastList = entries
			lastListIndex = made
			made.apply(text)
		} catch (e: Throwable) {
			text
		}
	}

	@Volatile
	private var lastList: List<Pair<String, String>>? = null

	@Volatile
	private var lastListIndex: WordIndex? = null

	private fun sameWord(a: String, b: String): Boolean = a.equals(b, ignoreCase = true)
	private fun foldKey(word: String): String = word.lowercase()
}
