package com.blindtechnexus.freeeloquencetts

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

object Abbreviations {

	private const val NAME = "abbreviations.dic"
	private const val MAX_LINE_LENGTH = 8192
	private const val MAX_ENTRIES = 20_000
	private const val MAX_KEY_LENGTH = 200
	private const val MAX_SAY_LENGTH = 2000

	const val WHOLE_WORD = 0

	const val STARTS_WITH = 1

	const val ANYWHERE = 2

	val MATCHES = listOf(WHOLE_WORD, STARTS_WITH, ANYWHERE)

	private val versionCount = AtomicInteger(0)

	val version: Int get() = versionCount.get()

	class Entry(
		val key: String,
		val say: String,
		val match: Int = WHOLE_WORD,
		val ignoreCase: Boolean = true
	) {

		fun sameAs(other: Entry): Boolean =
			key.equals(other.key, ignoreCase = ignoreCase || other.ignoreCase) &&
				match == other.match

		override fun toString(): String = "$key -> $say"
	}

	fun file(context: Context): File = File(DirectBoot.dictionaries(context), NAME)

	fun fileForWriting(context: Context): File = File(DirectBoot.dictionariesReady(context), NAME)

	fun entries(context: Context): List<Entry> = try {
		val f = file(context)
		if (!f.exists() || !f.canRead()) emptyList() else read(f)
	} catch (unreadable: Exception) {
		emptyList()
	}

	private fun read(f: File): List<Entry> =
		f.bufferedReader(Charsets.UTF_8).use { parseLines(it) }

	private fun parseLines(reader: BufferedReader): List<Entry> {
		val out = ArrayList<Entry>(64)
		var count = 0
		while (true) {
			val line = reader.readLine() ?: break
			if (count++ >= MAX_ENTRIES) break
			entryOf(line)?.let { out.add(it) }
		}
		return out
	}

	private fun entryOf(line: String): Entry? {
		if (line.isEmpty() || line.length > MAX_LINE_LENGTH) return null
		val first = line.indexOf('\t')
		if (first <= 0) return null
		val rest = line.substring(first + 1)
		val second = rest.indexOf('\t')
		val say = if (second < 0) rest else rest.substring(0, second)
		val tail = if (second < 0) "" else rest.substring(second + 1)
		val key = line.substring(0, first).trim()
		val said = say.trim()
		if (key.isEmpty() || said.isEmpty()) return null
		if (key.length > MAX_KEY_LENGTH || said.length > MAX_SAY_LENGTH) return null
		val columns = if (tail.isEmpty()) emptyList() else tail.split('\t')
		val match = matchOf(columns.getOrNull(0))
		val ignoreCase = columns.getOrNull(1)?.trim()?.lowercase() != "s"
		return Entry(key, said, match, ignoreCase)
	}

	private fun matchOf(column: String?): Int = when (column?.trim()?.lowercase()) {
		"s" -> STARTS_WITH
		"a" -> ANYWHERE
		else -> WHOLE_WORD
	}

	fun write(context: Context, entries: List<Entry>): Boolean = try {
		val target = fileForWriting(context)
		val tmp = File(target.parentFile, "${target.name}.tmp")
		tmp.bufferedWriter(Charsets.UTF_8).use { writer ->
			var count = 0
			for (entry in entries) {
				if (count++ >= MAX_ENTRIES) break
				val key = clean(entry.key)
				val say = cleanSay(entry.say)
				if (key.isEmpty() || say.isEmpty()) continue
				writer.write(key)
				writer.write('\t'.code)
				writer.write(say)
				writer.write('\t'.code)
				writer.write(matchColumn(entry.match))
				writer.write('\t'.code)
				writer.write(if (entry.ignoreCase) "i" else "s")
				writer.write('\n'.code)
			}
		}
		if (replaceFile(tmp, target)) {
			versionCount.incrementAndGet()
			true
		} else {
			false
		}
	} catch (unwritable: Exception) {
		false
	}

	private fun replaceFile(tmp: File, target: File): Boolean {
		if (tmp.renameTo(target)) return true
		return try {
			tmp.inputStream().use { from ->
				target.outputStream().use { to -> from.copyTo(to) }
			}
			tmp.delete()
			true
		} catch (failed: Exception) {
			runCatching { tmp.delete() }
			false
		}
	}

	private fun clean(text: String): String =
		text.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim()

	private fun cleanSay(text: String): String =
		text.replace('\n', ' ').replace('\r', ' ').trim()

	private fun matchColumn(match: Int): String = when (match) {
		STARTS_WITH -> "s"
		ANYWHERE -> "a"
		else -> "w"
	}

	fun add(context: Context, entry: Entry): Boolean {
		if (entry.key.isBlank() || entry.say.isBlank()) return false
		return try {
			val kept = entries(context).filterNot { it.sameAs(entry) }.toMutableList()
			if (kept.size >= MAX_ENTRIES) false
			else kept.add(entry).let { write(context, kept) }
		} catch (broken: Exception) {
			false
		}
	}

	fun remove(context: Context, entry: Entry): Boolean = try {
		write(context, entries(context).filterNot { it.sameAs(entry) })
	} catch (broken: Exception) {
		false
	}

	fun replace(context: Context, was: Entry, entry: Entry): Boolean {
		if (entry.key.isBlank() || entry.say.isBlank()) return false
		return try {
			val kept = entries(context).filterNot { it.sameAs(was) || it.sameAs(entry) }.toMutableList()
			kept.add(entry)
			write(context, kept)
		} catch (broken: Exception) {
			false
		}
	}

	fun parse(text: String): List<Entry> {
		if (text.isEmpty()) return emptyList()
		return try {
			val out = ArrayList<Entry>()
			var count = 0
			for (line in text.lineSequence()) {
				if (count++ >= MAX_ENTRIES) break
				entryOf(line)?.let { out.add(it) }
			}
			out
		} catch (broken: Exception) {
			emptyList()
		}
	}

	fun parseStream(input: InputStream): List<Entry> = try {
		BufferedReader(input.reader(Charsets.UTF_8), 8192).use { parseLines(it) }
	} catch (broken: Exception) {
		emptyList()
	}

	fun format(entries: List<Entry>): String = buildString {
		for (entry in entries) {
			append(clean(entry.key))
			append('\t')
			append(cleanSay(entry.say))
			append('\t')
			append(matchColumn(entry.match))
			append('\t')
			append(if (entry.ignoreCase) "i" else "s")
			append('\n')
		}
	}

	@Volatile
	private var cachedEntries: List<Entry>? = null

	@Volatile
	private var cachedMatcher: Replacer? = null

	private val cacheLock = Any()

	fun apply(text: String, entries: List<Entry>): String {
		if (text.isEmpty() || entries.isEmpty()) return text
		return try {
			val replacer = synchronized(cacheLock) {
				val held = cachedMatcher
				if (held != null && cachedEntries === entries) held
				else Replacer(entries).also {
					cachedMatcher = it
					cachedEntries = entries
				}
			}
			replacer.apply(text)
		} catch (broken: Exception) {
			text
		}
	}

	private class Replacer(entries: List<Entry>) {

		private class Node {
			val next = HashMap<Char, Node>()
			var entries: MutableList<Entry>? = null
		}

		private val root = Node()
		private val empty: Boolean

		init {
			var count = 0
			for (entry in entries) {
				if (count++ >= MAX_ENTRIES) break
				if (entry.key.isEmpty() || entry.say.isEmpty()) continue
				var node = root
				for (c in entry.key) {
					node = node.next.getOrPut(c.lowercaseChar()) { Node() }
				}
				(node.entries ?: ArrayList<Entry>(1).also { node.entries = it }).add(entry)
			}
			empty = root.next.isEmpty()
		}

		fun apply(text: String): String {
			if (empty) return text
			val length = text.length
			val out = StringBuilder(length + 32)
			var at = 0
			var changed = false
			while (at < length) {
				val found = longest(text, at)
				if (found == null) {
					out.append(text[at])
					at++
					continue
				}
				out.append(found.first.say)
				at += found.second
				changed = true
			}
			return if (changed) out.toString() else text
		}

		private fun longest(text: String, from: Int): Pair<Entry, Int>? {
			val before = from == 0 || !isWordChar(text[from - 1])
			var node = root
			var best: Entry? = null
			var bestLength = 0
			var at = from
			while (at < text.length) {
				node = node.next[text[at].lowercaseChar()] ?: break
				at++
				val here = node.entries ?: continue
				val afterWord = at >= text.length || !isWordChar(text[at])
				for (entry in here) {
					val allowed = when (entry.match) {
						ANYWHERE -> true
						STARTS_WITH -> before
						else -> before && afterWord
					}
					if (!allowed) continue
					if (!entry.ignoreCase && !matchesCase(text, from, at, entry.key)) continue
					if (at - from > bestLength) {
						best = entry
						bestLength = at - from
					}
				}
			}
			val winner = best ?: return null
			return winner to bestLength
		}

		private fun matchesCase(text: String, from: Int, to: Int, key: String): Boolean {
			if (to - from != key.length) return false
			for (offset in 0 until key.length) {
				if (text[from + offset] != key[offset]) return false
			}
			return true
		}

		private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '\''
	}
}
