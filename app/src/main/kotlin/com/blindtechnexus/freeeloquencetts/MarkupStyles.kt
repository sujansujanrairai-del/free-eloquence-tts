package com.blindtechnexus.freeeloquencetts

object MarkupStyles {

	private const val TAG_LIMIT = 400

	private const val ALT_LIMIT = 120

	fun annotate(text: String): String {
		if (text.isEmpty()) return text
		var out = text
		if ('<' in out) out = tags(out)
		if ('*' in out || '_' in out || '#' in out) out = marks(out)
		return out
	}

	private fun tidy(text: String): String {
		val out = StringBuilder(text.length)
		var spaced = false
		for (c in text) {
			if (c.isWhitespace()) {
				spaced = true
				continue
			}
			if (spaced && out.isNotEmpty()) out.append(' ')
			spaced = false
			out.append(c)
		}
		return out.toString()
	}

	private class Tag(val start: String, val end: String)

	private val TAGS: Map<String, Tag> = buildMap(24) {
		for (name in listOf("b", "strong")) put(name, Tag("bold", "bold"))
		for (name in listOf("i", "em")) put(name, Tag("italic", "italic"))
		for (name in listOf("u", "ins")) put(name, Tag("underline", "underline"))
		for (name in listOf("s", "strike", "del")) put(name, Tag("strikethrough", "strikethrough"))
		for (name in listOf("code", "kbd", "samp", "tt")) put(name, Tag("code", "code"))
		put("sub", Tag("subscript", "subscript"))
		put("sup", Tag("superscript", "superscript"))
		put("a", Tag("link", "link"))
		put("q", Tag("quote", "quote"))
		put("blockquote", Tag("quote", "quote"))
		put("mark", Tag("highlighted", "highlighted"))
		for (level in 1..6) put("h$level", Tag("heading $level", "heading"))
	}

	private val BREAKS = setOf(
		"p", "div", "br", "tr", "td", "th", "table", "ul", "ol", "dl", "dt", "dd",
		"section", "article", "header", "footer", "nav", "main", "aside", "figure",
		"figcaption", "hr", "pre", "body", "html", "span", "font", "center", "thead",
		"tbody", "tfoot", "caption", "details", "summary", "fieldset", "form", "label"
	)

	private val VOID = setOf(
		"br", "hr", "img", "input", "meta", "link", "source", "wbr", "area", "col", "base", "embed"
	)

	private fun tags(text: String): String {
		val out = StringBuilder(text.length + 64)
		val open = ArrayList<String>(8)
		var at = 0
		var changed = false
		val n = text.length
		while (at < n) {
			val c = text[at]
			if (c != '<') {
				out.append(c)
				at++
				continue
			}
			val close = tagEnd(text, at)
			if (close < 0) {
				out.append(c)
				at++
				continue
			}
			val body = text.substring(at + 1, close)
			at = close + 1
			changed = true
			announce(body, out, open)
		}

		while (open.isNotEmpty()) {
			out.append(" end ").append(open.removeAt(open.size - 1)).append(' ')
		}
		return if (changed) tidy(out.toString()) else text
	}

	private fun announce(body: String, out: StringBuilder, open: MutableList<String>) {
		var at = 0
		val n = body.length
		if (n == 0) return

		if (body[0] == '!' || body[0] == '?') return
		val closing = body[0] == '/'
		if (closing) at = 1
		while (at < n && body[at].isWhitespace()) at++
		var end = at
		while (end < n && body[end].isLetterOrDigit()) end++
		if (end == at) return
		val name = body.substring(at, end).lowercase()

		if (name == "img") {
			if (closing) return
			val alt = attribute(body, "alt")
			out.append(" image ")
			if (alt != null) out.append(alt).append(' ')
			return
		}
		if (name == "li") {
			out.append(if (closing) " " else " bullet ")
			return
		}
		if (name in VOID || name in BREAKS) {
			out.append(' ')
			return
		}
		val tag = TAGS[name] ?: return
		if (closing) {
			val found = open.lastIndexOf(tag.end)
			if (found >= 0) {
				open.removeAt(found)
				out.append(" end ").append(tag.end).append(' ')
			}
			return
		}
		out.append(' ').append(tag.start).append(' ')
		open.add(tag.end)
	}

	private fun tagEnd(text: String, at: Int): Int {
		if (at + 1 >= text.length) return -1
		val limit = minOf(text.length, at + TAG_LIMIT)
		val first = text[at + 1]
		if (first == '!' || first == '?') {
			var k = at + 2
			while (k < limit) {
				val c = text[k]
				if (c == '>') return k
				if (c == '<') return -1
				k++
			}
			return -1
		}
		var nameStart = at + 1
		if (text[nameStart] == '/') nameStart++
		if (nameStart >= text.length || !text[nameStart].isLetter()) return -1
		var nameEnd = nameStart
		while (nameEnd < text.length && text[nameEnd].isLetterOrDigit()) nameEnd++
		val name = text.substring(nameStart, nameEnd).lowercase()
		if (name != "li" && name != "img" && name !in TAGS && name !in BREAKS && name !in VOID) return -1

		var hasEquals = false
		var k = nameEnd
		while (k < limit) {
			val c = text[k]
			if (c == '>') {
				if (k > nameEnd && !hasEquals && text[nameEnd].isWhitespace()) {
					var rest = nameEnd
					while (rest < k && text[rest].isWhitespace()) rest++
					val selfClose = rest == k - 1 && text[rest] == '/'
					if (rest < k && !selfClose) return -1
				}
				return k
			}
			if (c == '=') hasEquals = true
			if (c == '<') return -1
			k++
		}
		return -1
	}

	private fun attribute(body: String, name: String): String? {
		var at = body.indexOf(name, ignoreCase = true)
		while (at > 0 && (body[at - 1].isLetterOrDigit() || body[at - 1] == '-')) {
			at = body.indexOf(name, at + 1, ignoreCase = true)
		}
		if (at < 0) return null
		var i = at + name.length
		while (i < body.length && (body[i] == ' ' || body[i] == '=')) i++
		if (i >= body.length) return null
		val quote = body[i]
		if (quote != '"' && quote != '\'') return null
		val close = body.indexOf(quote, i + 1)
		if (close < 0) return null
		val clean = StringBuilder(64)
		for (k in i + 1 until close) {
			val c = body[k]
			if (c.isLetterOrDigit() || c == '.' || c == '\'' || c == ',') clean.append(c)
			else if (c.isWhitespace() || c == '-' || c == '_' || c == '/') clean.append(' ')
			if (clean.length >= ALT_LIMIT) break
		}
		val answer = clean.toString().trim()
		return if (answer.isEmpty()) null else answer
	}

	private fun marks(text: String): String {
		val out = StringBuilder(text.length + 32)
		var changed = false
		var at = 0
		val n = text.length
		while (at < n) {
			val c = text[at]
			if (c != '\n' && c != '*' && c != '_' && c != '#') {
				out.append(c)
				at++
				continue
			}
			val lineStart = at == 0 || text[at - 1] == '\n'
			if (c == '#' && lineStart) {
				var j = at
				var hashes = 0
				while (j < n && text[j] == '#') {
					hashes++
					j++
				}
				if (hashes in 1..6 && j < n && text[j] == ' ') {
					val close = text.indexOf('\n', j)
					val stop = if (close < 0) n else close
					val line = text.substring(j + 1, stop).trim()
					out.append(" heading ").append(hashes).append(' ').append(line)
					out.append(" end heading ")
					at = stop
					changed = true
					continue
				}
				out.append(c)
				at++
				continue
			}
			if (c == '*' || c == '_') {
				val double = at + 1 < n && text[at + 1] == c
				val width = if (double) 2 else 1
				val from = at + width
				val closer = closer(text, from, c, double)
				if (closer > from && !text[from].isWhitespace() && !text[closer - 1].isWhitespace()) {
					val label = if (double) "bold" else "italic"
					out.append(' ').append(label).append(' ')
					out.append(text, from, closer)
					out.append(" end ").append(label).append(' ')
					at = closer + width
					changed = true
					continue
				}
			}
			out.append(c)
			at++
		}
		return if (changed) tidy(out.toString()) else text
	}

	private fun closer(text: String, from: Int, mark: Char, double: Boolean): Int {
		var at = from
		val n = text.length
		while (at < n) {
			val c = text[at]
			if (c == '\n' && !double) return -1
			if (c == mark) {
				val pair = at + 1 < n && text[at + 1] == mark
				if (double && pair) return at
				if (!double && !pair && (at == 0 || text[at - 1] != mark)) return at
			}
			at++
		}
		return -1
	}
}
