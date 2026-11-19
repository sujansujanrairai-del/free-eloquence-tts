package com.blindtechnexus.freeeloquencetts

import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object Dictionaries {

	fun load(engine: EvvEngine, volume: Int, file: File, charset: java.nio.charset.Charset = Charsets.ISO_8859_1): Int {
		var taught = 0
		var refused = 0
		try {
			file.forEachLine(charset) { line ->
				val split = line.indexOf('\t')
				if (split > 0) {
					val key = line.substring(0, split).trim()
					val say = line.substring(split + 1).trim()
					if (key.isNotEmpty() && say.isNotEmpty()) {
						if (engine.teachWord(volume, key, say) == 0) taught++ else refused++
					}
				}
			}
		} catch (unreadable: Exception) {
			Log.e(TAG, "cannot read ${file.name}", unreadable)
			return taught
		}
		if (refused > 0) Log.e(TAG, "${file.name}: $refused of ${taught + refused} entries refused")
		return taught
	}

	fun count(file: File, charset: java.nio.charset.Charset = Charsets.ISO_8859_1): Int = try {
		file.useLines(charset) { lines -> lines.count { it.indexOf('\t') > 0 } }
	} catch (unreadable: Exception) {
		0
	}

	fun read(file: File): String {
		return try {
			if (!file.exists()) return ""
			if (file.length() > 10 * 1024 * 1024) {

				val bytes = file.inputStream().use { it.readNBytes(10 * 1024 * 1024) }
				decode(bytes)
			} else {
				val bytes = file.readBytes()
				decode(bytes)
			}
		} catch (e: Exception) {
			""
		}
	}

	fun decode(bytes: ByteArray): String = try {
		utf8(bytes) ?: String(bytes, Charsets.ISO_8859_1)
	} catch (e: Exception) {
		try { String(bytes, Charsets.ISO_8859_1) } catch (_: Exception) { "" }
	}

	fun decode(input: java.io.InputStream): String {
		return try {
			val bytes = input.readNBytes(10 * 1024 * 1024)
			decode(bytes)
		} catch (e: Exception) {
			""
		}
	}

	private fun utf8(bytes: ByteArray): String? = try {
		StandardCharsets.UTF_8.newDecoder()
			.onMalformedInput(CodingErrorAction.REPORT)
			.onUnmappableCharacter(CodingErrorAction.REPORT)
			.decode(ByteBuffer.wrap(bytes))
			.toString()
	} catch (broken: Exception) {
		null
	}

	private const val TAG = "freeeloquencetts"
}
