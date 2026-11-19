package com.blindtechnexus.freeeloquencetts

object TextFixes {

	private const val OPENERS = "~#\$%^*({|[<\"•"

	private val OPENERS_SET = OPENERS.toSet()
	private val MARKS = setOf(':', '.', '!', ';', ',', '?')

	fun apply(raw: String): String {
		if (raw.isEmpty()) return raw

		val text = webClean(raw)
		if (text.isEmpty()) return text

		var needLetterDigit = false
		var needOpener = false
		var needBracket = false
		var needMark = false
		for (c in text) {
			if (!needLetterDigit && c.isLetter()) needLetterDigit = true
			if (!needOpener && c in OPENERS_SET) needOpener = true
			if (!needBracket && c == '(') needBracket = true
			if (!needMark && c in MARKS) needMark = true
			if (needLetterDigit && needOpener && needBracket && needMark) break
		}
		if (!needLetterDigit && !needOpener && !needBracket && !needMark) return text

		val n = text.length
		val out = StringBuilder(n + 32)
		var i = 0
		while (i < n) {
			val c = text[i]

			if (c.isWhitespace()) {
				var j = i
				while (j < n && text[j].isWhitespace()) j++
				if (j < n) {
					val next = text[j]

					if (needBracket && next == '(' && j + 2 < n && text[j + 1] == 's' && text[j + 2] == ')') {

						if (out.isNotEmpty() && out.last().isLetter()) {

							i = j
							continue
						}
					}

					if (needMark && next in MARKS) {
						val afterNext = if (j + 1 < n) text[j + 1] else '\u0000'
						if (afterNext == '\u0000' || (!afterNext.isLetter() && !afterNext.isDigit())) {

							i = j
							continue
						}
					}
				}

				out.append(' ')
				i = j
				continue
			}

			if (c.isDigit() && out.isNotEmpty()) {
				val prev = out.last()
				if (prev.isLetter()) {
					out.append(' ')
				}
			}

			if (c in OPENERS_SET && out.isNotEmpty()) {
				val prev = out.last()
				if (prev.isLetter()) {

					if (prev != ' ') out.append(' ')
				}
			}

			out.append(c)
			i++
		}
		return out.toString()
	}

	private fun isWebNoise(c: Char): Boolean = when (c) {
		'\u00A0', '\u1680', '\u2000', '\u2001', '\u2002', '\u2003', '\u2004', '\u2005',
		'\u2006', '\u2007', '\u2008', '\u2009', '\u200A', '\u202F', '\u205F', '\u3000' -> true
		'\u200B', '\u200C', '\u200D', '\u2060', '\uFEFF', '\u00AD', '\uFFFC', '\u180E' -> true
		else -> false
	}

	private fun webClean(text: String): String {
		var needed = false
		for (c in text) {
			if (isWebNoise(c)) {
				needed = true
				break
			}
		}
		if (!needed) return text
		val out = StringBuilder(text.length)
		for (c in text) {
			when {
				c == '\u00A0' || c == '\u1680' || (c in '\u2000'..'\u200A') ||
					c == '\u202F' || c == '\u205F' || c == '\u3000' -> out.append(' ')
				c == '\u200B' || c == '\u200C' || c == '\u200D' || c == '\u2060' ||
					c == '\uFEFF' || c == '\u00AD' || c == '\uFFFC' || c == '\u180E' -> Unit
				else -> out.append(c)
			}
		}
		return out.toString()
	}
}
