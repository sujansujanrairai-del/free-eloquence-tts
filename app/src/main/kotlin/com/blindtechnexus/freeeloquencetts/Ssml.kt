package com.blindtechnexus.freeeloquencetts

object Ssml {

	private const val TAG_LIMIT = 200

	fun strip(text: String): String {
		if (text.isEmpty()) return text
		if ('<' !in text && '&' !in text) return text

		val sb = StringBuilder(text.length)
		var lastWasSpace = false
		var at = 0
		val n = text.length
		while (at < n) {
			val c = text[at]
			when {
				c == '<' -> {
					val close = tagEnd(text, at)
					if (close < 0) {

						sb.append(c)
						lastWasSpace = false
						at++
						continue
					}
					if (!lastWasSpace && sb.isNotEmpty()) {
						sb.append(' ')
						lastWasSpace = true
					}
					at = close + 1
				}

				c == '&' -> {
					val decoded = entity(text, at)
					if (decoded == null) {
						sb.append(c)
						lastWasSpace = false
						at++
					} else {
						for (d in decoded) {
							if (d.isWhitespace()) {
								if (!lastWasSpace) {
									sb.append(' ')
									lastWasSpace = true
								}
							} else {
								sb.append(d)
								lastWasSpace = false
							}
						}
						at += entityLength(text, at)
					}
				}

				c.isWhitespace() -> {
					if (!lastWasSpace) {
						sb.append(' ')
						lastWasSpace = true
					}
					at++
				}

				else -> {
					sb.append(c)
					lastWasSpace = false
					at++
				}
			}
		}
		return sb.toString().trim()
	}

	private fun tagEnd(text: String, at: Int): Int {
		if (at + 1 >= text.length) return -1
		val limit = minOf(text.length, at + TAG_LIMIT)
		val first = text[at + 1]
		if (!(first.isLetter() || first == '/' || first == '!' || first == '?')) return -1
		var nameStart = at + 1
		if (text[nameStart] == '/') nameStart++
		var nameEnd = nameStart
		while (nameEnd < text.length && nameEnd < limit &&
			(text[nameEnd].isLetterOrDigit() || text[nameEnd] == '-')
		) nameEnd++
		var hasEquals = false
		var j = nameEnd
		while (j < limit) {
			val c = text[j]
			if (c == '>') {
				if (j > nameEnd && !hasEquals && text[nameEnd].isWhitespace()) return -1
				return j
			}
			if (c == '=') hasEquals = true
			if (c == '<') return -1
			j++
		}
		return -1
	}

	private fun entity(text: String, at: Int): String? {
		val semi = text.indexOf(';', at + 1)
		if (semi < 0 || semi - at > 10) return null
		val body = text.substring(at + 1, semi)
		when (body) {
			"amp" -> return "&"
			"lt" -> return "<"
			"gt" -> return ">"
			"quot" -> return "\""
			"apos" -> return "'"
			"nbsp" -> return " "
			"mdash" -> return " - "
			"ndash" -> return " - "
			"hellip" -> return "..."
			"copy" -> return " copyright "
			"reg" -> return " registered "
			"trade" -> return " trademark "
			"deg" -> return " degrees "
		}

		if (body.startsWith("#")) {
			val code = if (body.length > 1 && (body[1] == 'x' || body[1] == 'X')) {
				body.substring(2).toIntOrNull(16)
			} else {
				body.substring(1).toIntOrNull()
			} ?: return null
			if (code <= 0 || code > 0x10FFFF) return null
			return try {
				String(Character.toChars(code))
			} catch (_: Throwable) {
				null
			}
		}
		return null
	}

	private fun entityLength(text: String, at: Int): Int {
		val semi = text.indexOf(';', at + 1)
		return if (semi < 0) 1 else semi - at + 1
	}
}
