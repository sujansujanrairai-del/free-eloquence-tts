package com.blindtechnexus.freeeloquencetts

object Symbols {

	const val NONE = 0

	const val SOME = 1

	const val MOST = 2

	const val ALL = 3

	const val CUSTOM = 4

	val MODES = listOf(ALL, MOST, SOME, CUSTOM, NONE)

	private class Entry(val name: String, val level: Int)

	private val TABLE = mapOf(
		'$' to Entry("dollar", SOME),
		'%' to Entry("percent", SOME),
		'+' to Entry("plus", SOME),
		'=' to Entry("equals", SOME),
		'@' to Entry("at", SOME),
		'#' to Entry("number", SOME),
		'*' to Entry("asterisk", SOME),
		'&' to Entry("and", SOME),
		'/' to Entry("slash", SOME),
		'<' to Entry("less than", SOME),
		'>' to Entry("greater than", SOME),
		'|' to Entry("bar", SOME),
		'~' to Entry("tilda", SOME),
		'^' to Entry("caret", SOME),
		'_' to Entry("underscore", SOME),
		'\\' to Entry("backslash", SOME),
		'(' to Entry("left parenthesis", MOST),
		')' to Entry("right parenthesis", MOST),
		'[' to Entry("left bracket", MOST),
		']' to Entry("right bracket", MOST),
		'{' to Entry("left brace", MOST),
		'}' to Entry("right brace", MOST),
		'"' to Entry("quote", MOST),
		'\'' to Entry("apostrophe", MOST),
		'?' to Entry("question mark", MOST),
		'!' to Entry("exclamation point", MOST),
		';' to Entry("semicolon", MOST),
		':' to Entry("colon", MOST),
		'.' to Entry("dot", ALL),
		',' to Entry("comma", ALL),
		'€' to Entry("euro", SOME),
		'£' to Entry("pounds", SOME),
		'¥' to Entry("yen", SOME),
		'™' to Entry("trademark", SOME),
		'°' to Entry("degrees", SOME),
		'§' to Entry("section", SOME),
		'¶' to Entry("paragraph", SOME),
		'©' to Entry("copyright", SOME),
		'®' to Entry("registered", SOME),
		'±' to Entry("plus or minus", SOME),
		'×' to Entry("times", SOME),
		'÷' to Entry("divided by", SOME),
		'≤' to Entry("less than or equal to", SOME),
		'≥' to Entry("greater than or equal to", SOME),
		'≠' to Entry("not equal to", SOME)
	)

	private val PLAIN = mapOf(
		'‘' to "'", '’' to "'", '‚' to "'", '‛' to "'",
		'“' to "\"", '”' to "\"", '„' to "\"", '‟' to "\"",
		'‹' to "<", '›' to ">", '«' to "\"", '»' to "\"",
		'′' to "'", '″' to "\"", '⁄' to "/", '∕' to "/",
		'…' to "...",
		'•' to ".", '·' to "."
	)

	private val RELEVANT = BooleanArray(0x10000).apply {
		for (c in TABLE.keys) this[c.code] = true
		for (c in PLAIN.keys) this[c.code] = true
	}

	private val PLAIN_CHARS = BooleanArray(0x10000).apply {
		for (c in PLAIN.keys) this[c.code] = true
	}

	private const val PROSODY = ",.;:?!"

	fun apply(text: String, mode: Int, custom: String): String {
		if (text.isEmpty()) return text
		if (mode == SOME) return withoutLoneCommas(text)
		var relevant = false
		var scan = 0
		while (scan < text.length) {
			val ch = text[scan]
			if (ch == '`') {
				scan = EvvEngine.annotationEnd(text, scan)
				continue
			}
			if (ch.code < RELEVANT.size && RELEVANT[ch.code]) {
				relevant = true
				break
			}
			scan++
		}

		if (!relevant) return text.trim()
		val listed = if (mode == CUSTOM) custom.filter { TABLE.containsKey(it) }.toSet() else emptySet()
		val source = normalize(text)
		val out = StringBuilder(source.length + 32)
		var at = 0
		while (at < source.length) {
			val c = source[at]
			if (c == '`') {

				val end = EvvEngine.annotationEnd(source, at)
				out.append(source, at, end)
				at = end
				continue
			}
			when {

				c == '\'' && betweenWordCharacters(source, at) -> out.append(c)

				c in ",.:" && betweenDigits(source, at) -> out.append(c)

				else -> {
					val entry = TABLE[c]
					when {
						entry == null -> out.append(c)

						mode == NONE || (mode == CUSTOM && c !in listed) ->
							if (c in PROSODY && attached(source, at) &&
								!betweenWordCharacters(source, at)
							) out.append(c) else out.append(' ')

						attached(source, at) && entry.level >= MOST && names(mode, entry, c, listed) ->
							out.append(' ').append(entry.name).append(' ')

						else -> out.append(c)
					}
				}
			}
			at++
		}
		return COLLAPSE.replace(out.toString().trim(), " ")
	}

	private fun withoutLoneCommas(text: String): String {
		if (text.indexOf(',') < 0) return text
		var out: StringBuilder? = null
		var at = 0
		val n = text.length
		while (at < n) {
			val c = text[at]
			if (c == '`') {
				val end = EvvEngine.annotationEnd(text, at)
				out?.append(text, at, end)
				at = end
				continue
			}
			if (c != ',') {
				out?.append(c)
				at++
				continue
			}

			if (betweenDigits(text, at)) {
				out?.append(c)
				at++
				continue
			}

			val before = at - 1
			val after = at + 1
			val worded = (before >= 0 && text[before].isLetterOrDigit()) ||
				(after < n && text[after].isLetterOrDigit())
			if (worded) {
				out?.append(c)
				at++
				continue
			}

			if (out == null) {
				out = StringBuilder(n)
				out.append(text, 0, at)
			}
			if (at > 0 && !text[at - 1].isWhitespace() && after < n) out.append(' ')
			at++
		}
		val built = out ?: return text
		return spaced(built)
	}

	private fun spaced(text: CharSequence): String {
		val out = StringBuilder(text.length)
		var spaces = 0
		for (c in text) {
			if (c == ' ' || c == '\t') {
				spaces++
				continue
			}
			if (spaces > 0 && c != '\n' && out.isNotEmpty() && out[out.length - 1] != '\n') out.append(' ')
			spaces = 0
			out.append(c)
		}
		return out.toString()
	}

	private fun names(mode: Int, entry: Entry, c: Char, listed: Set<Char>): Boolean = when (mode) {
		ALL -> true
		MOST -> entry.level == MOST
		CUSTOM -> c in listed
		else -> false
	}

	private fun normalize(text: String): String {
		var needed = false
		var scan = 0
		while (scan < text.length) {
			val ch = text[scan]
			if (ch == '`') {
				scan = EvvEngine.annotationEnd(text, scan)
				continue
			}
			val c = ch.code
			if (c < PLAIN_CHARS.size && PLAIN_CHARS[c]) {
				needed = true
				break
			}
			scan++
		}
		if (!needed) return text
		val out = StringBuilder(text.length)
		var at = 0
		while (at < text.length) {
			val c = text[at]
			if (c == '`') {
				val end = EvvEngine.annotationEnd(text, at)
				out.append(text, at, end)
				at = end
				continue
			}
			out.append(PLAIN[c] ?: c)
			at++
		}
		return out.toString()
	}

	private fun attached(text: String, at: Int): Boolean {
		val before = at > 0 && !text[at - 1].isWhitespace()
		val after = at < text.length - 1 && !text[at + 1].isWhitespace()
		return before || after
	}

	private fun betweenWordCharacters(text: String, at: Int): Boolean =
		at > 0 && at < text.length - 1 &&
			(text[at - 1].isLetterOrDigit() && text[at + 1].isLetterOrDigit())

	private fun betweenDigits(text: String, at: Int): Boolean =
		at > 0 && at < text.length - 1 &&
			(text[at - 1].isDigit() && text[at + 1].isDigit())

	private val COLLAPSE = Regex(" {2,}")
}
