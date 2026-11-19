package com.blindtechnexus.freeeloquencetts

object CodeReading {

	class Options(
		val camelCase: Boolean,
		val snakeCase: Boolean,
		val operators: Boolean,
		val braces: Boolean,
		val indentation: Boolean,
		val paths: Boolean,
		val urls: Boolean
	) {

		val none: Boolean
			get() = !(camelCase || snakeCase || operators || braces ||
				indentation || paths || urls)
	}

	val OFF = Options(false, false, false, false, false, false, false)

	val ALL = Options(true, true, true, true, true, true, true)

	private val OPERATORS = listOf(
		">>>" to "unsigned right shift",
		"..." to "dot dot dot",
		"!==" to "bang double equals",
		"===" to "triple equals",
		"<<" to "left shift",
		">>" to "right shift",
		"==" to "double equals",
		"!=" to "not equals",
		"<=" to "less than or equal to",
		">=" to "greater than or equal to",
		"++" to "plus plus",
		"--" to "minus minus",
		"+=" to "plus equals",
		"-=" to "minus equals",
		"*=" to "star equals",
		"/=" to "slash equals",
		"%=" to "percent equals",
		"&=" to "and equals",
		"|=" to "or equals",
		"^=" to "xor equals",
		"&&" to "and and",
		"||" to "or or",
		"!!" to "bang bang",
		"??" to "double question",
		"?." to "question dot",
		"->" to "arrow",
		"=>" to "fat arrow",
		"<-" to "left arrow",
		"::" to "double colon",
		".." to "dot dot",
		"=" to "equals",
		"+" to "plus",
		"-" to "minus",
		"*" to "star",
		"/" to "slash",
		"%" to "percent",
		"&" to "ampersand",
		"|" to "bar",
		"^" to "caret",
		"~" to "tilde",
		"!" to "bang",
		"?" to "question",
		"<" to "less than",
		">" to "greater than"
	)

	private val BRACES = listOf(
		'{' to "left brace",
		'}' to "right brace",
		'[' to "left bracket",
		']' to "right bracket",
		'(' to "left paren",
		')' to "right paren",
		';' to "semicolon",
		':' to "colon",
		',' to "comma",
		'.' to "dot",
		'#' to "hash",
		'@' to "at",
		'$' to "dollar",
		'`' to "backtick"
	).toMap()

	fun apply(text: String, options: Options): String {
		if (text.isEmpty() || options.none) return text
		var out = text
		try { if (options.urls) out = urls(out) } catch (_: Exception) {}
		try { if (options.paths) out = paths(out) } catch (_: Exception) {}
		try { if (options.camelCase || options.snakeCase) out = identifiers(out, options) } catch (_: Exception) {}
		try { if (options.operators || options.braces) out = marks(out, options) } catch (_: Exception) {}
		try { if (options.indentation) out = indentation(out) } catch (_: Exception) {}
		return out
	}

	private fun urls(text: String): String {
		if (text.indexOf("://") < 0) return text
		val out = StringBuilder(text.length + 32)
		var at = 0
		var changed = false
		while (at < text.length) {
			val afterScheme = schemeEnd(text, at)
			if (afterScheme < 0) {
				out.append(text[at])
				at++
				continue
			}
			changed = true
			out.append("url ")
			at = afterScheme

			var nameEnd = hostEnd(text, at)
			while (nameEnd > at && text[nameEnd - 1] == '.') nameEnd--
			var host = text.substring(at, nameEnd)
			at = nameEnd
			if (host.startsWith("www.")) host = host.substring(4)
			for (c in host) {
				if (c == '.') out.append(" dot ") else out.append(c)
			}

			at = addressRest(text, at, out)
		}
		return if (changed) settled(text, out.toString()) else text
	}

	private fun addressRest(text: String, from: Int, out: StringBuilder): Int {
		var at = from
		while (at < text.length) {
			val c = text[at]
			if (c.isLetterOrDigit()) {
				out.append(c)
				at++
				continue
			}
			when (c) {
				'/', '\\' -> out.append(" slash ")
				'-' -> out.append(" dash ")
				'_' -> out.append(' ')
				':' -> out.append(" colon ")
				'?' -> out.append(" question mark ")
				'=' -> out.append(" equals ")
				'&' -> out.append(" and ")
				'#' -> out.append(" hash ")
				'%' -> out.append(" percent ")
				'+' -> out.append(" plus ")
				'@' -> out.append(" at ")
				'~' -> out.append(" tilde ")

				'.' -> if (at + 1 >= text.length || text[at + 1].isWhitespace()) {
					return at
				} else {
					out.append(" dot ")
				}
				else -> return at
			}
			at++
		}
		return at
	}

	private fun schemeEnd(text: String, at: Int): Int {
		if (at > 0 && isIdentifierChar(text[at - 1])) return -1
		var j = at
		while (j < text.length && (text[j].isLetterOrDigit() || text[j] == '+' ||
				text[j] == '-' || text[j] == '.')
		) j++
		if (j == at || j > at + 12) return -1
		if (j + 2 < text.length && text[j] == ':' && text[j + 1] == '/' && text[j + 2] == '/') {
			return j + 3
		}
		return -1
	}

	private fun hostEnd(text: String, from: Int): Int {
		var j = from
		while (j < text.length) {
			val c = text[j]
			if (c.isLetterOrDigit() || c == '.' || c == '-' || c == '_') j++ else break
		}
		return j
	}

	private fun paths(text: String): String {
		if (text.indexOf('/') < 0 && text.indexOf('\\') < 0) return text
		val out = StringBuilder(text.length + 64)
		var at = 0
		var changed = false
		while (at < text.length) {
			val end = if (isPathStart(text, at)) tokenEnd(text, at) else -1
			if (end < 0 || !looksLikePath(text, at, end)) {
				out.append(text[at])
				at++
				continue
			}
			changed = true
			out.append("path ")
			for (j in at until end) {
				when (val c = text[j]) {

					'/', '\\' -> if (j > at) out.append(" slash ")
					'.' -> out.append(" dot ")
					':' -> out.append(" colon ")
					'_' -> out.append(' ')
					else -> out.append(c)
				}
			}
			at = end
		}
		return if (changed) settled(text, out.toString()) else text
	}

	private fun isPathStart(text: String, at: Int): Boolean {
		if (at > 0 && !text[at - 1].isWhitespace()) return false
		val c = text[at]
		return c == '/' || c == '~' || c == '.' || c.isLetterOrDigit()
	}

	private fun tokenEnd(text: String, from: Int): Int {
		var j = from
		while (j < text.length) {
			val c = text[j]
			if (c.isLetterOrDigit() || c == '/' || c == '\\' || c == '.' ||
				c == '_' || c == '-' || c == '+' || c == '~' || c == ':'
			) j++ else break
		}
		return j
	}

	private fun looksLikePath(text: String, from: Int, to: Int): Boolean {
		if (to - from < 3) return false
		var separators = 0
		var extension = false
		for (j in from until to) {
			val c = text[j]
			if (c == '/' || c == '\\') {
				if (j > from) separators++
				continue
			}

			if (c == '.' && j > from && j + 1 < to &&
				text[j - 1].isLetterOrDigit() && text[j + 1].isLetterOrDigit()
			) extension = true
		}
		if (separators < 1) return false
		val first = text[from]
		if (first == '/' || first == '~') return true
		if (first == '.' && from + 1 < to &&
			(text[from + 1] == '/' || text[from + 1] == '\\')
		) return true
		if (first.isLetter() && from + 2 < to && text[from + 1] == ':' &&
			(text[from + 2] == '/' || text[from + 2] == '\\')
		) return true
		return extension
	}

	private fun identifiers(text: String, options: Options): String {
		val out = StringBuilder(text.length + 32)
		var changed = false
		var at = 0
		while (at < text.length) {
			val c = text[at]
			if (!c.isLetter() && c != '_') {
				out.append(c)
				at++
				continue
			}
			val end = identifierEnd(text, at)
			val word = text.substring(at, end)
			val split = splitIdentifier(word, options)
			if (split !== word) changed = true
			out.append(split)
			at = end
		}
		return if (changed) out.toString() else text
	}

	private fun identifierEnd(text: String, from: Int): Int {
		var j = from
		while (j < text.length && (text[j].isLetterOrDigit() || text[j] == '_')) j++
		return j
	}

	private fun splitIdentifier(word: String, options: Options): String {
		if (word.length < 2) return word
		var body = word
		if (options.snakeCase && body.indexOf('_') >= 0) {
			body = body.replace('_', ' ').trim()
			if (body.isEmpty()) return word
		}
		if (!options.camelCase) return body

		if (body.none { it.isLowerCase() }) return body
		val out = StringBuilder(body.length + 8)
		for (at in body.indices) {
			val c = body[at]

			if (at == 0 || !c.isUpperCase()) {
				out.append(c)
				continue
			}

			val previous = body[at - 1]
			val next = if (at + 1 < body.length) body[at + 1] else '\u0000'
			val startsPart = (!previous.isUpperCase() && !previous.isDigit()) ||
				(previous.isUpperCase() && next.isLowerCase())
			if (startsPart) out.append(' ')
			out.append(c.lowercaseChar())
		}
		return out.toString()
	}

	private fun marks(text: String, options: Options): String {
		var out = text
		if (options.operators) out = operators(out)
		if (options.braces) out = braces(out)
		return out
	}

	private fun operators(text: String): String {
		val out = StringBuilder(text.length + 64)
		var at = 0
		var changed = false
		while (at < text.length) {
			val found = operatorAt(text, at)
			if (found == null) {
				out.append(text[at])
				at++
				continue
			}
			changed = true
			out.append(' ').append(found.second).append(' ')
			at += found.first.length
		}
		return if (changed) settled(text, out.toString()) else text
	}

	private fun operatorAt(text: String, at: Int): Pair<String, String>? {
		for ((token, name) in OPERATORS) {
			if (text.startsWith(token, at)) return token to name
		}
		return null
	}

	private fun braces(text: String): String {
		var relevant = false
		for (at in text.indices) {
			if (BRACES.containsKey(text[at])) {
				relevant = true
				break
			}
		}
		if (!relevant) return text
		val out = StringBuilder(text.length + 64)
		for (at in text.indices) {
			val name = BRACES[text[at]]
			if (name == null) out.append(text[at])
			else out.append(' ').append(name).append(' ')
		}
		return settled(text, out.toString())
	}

	private fun settled(source: String, built: String): String {
		var out = COLLAPSE.replace(built, " ")
		if (out.startsWith(" ") && source.isNotEmpty() && !source[0].isWhitespace()) {
			out = out.substring(1)
		}
		if (out.endsWith(" ") && source.isNotEmpty() && !source[source.length - 1].isWhitespace()) {
			out = out.dropLast(1)
		}
		return out
	}

	private fun indentation(text: String): String {
		var needed = false
		for (at in text.indices) {
			val c = text[at]
			if (c != ' ' && c != '\t') continue

			if (at == 0 || text[at - 1] == '\n') {
				needed = true
				break
			}
		}
		if (!needed) return text
		val out = StringBuilder(text.length + 32)
		var atLineStart = true
		var index = 0
		while (index < text.length) {
			val c = text[index]
			if (c == '\n') {
				out.append(c)
				atLineStart = true
				index++
				continue
			}
			if (!atLineStart || (c != ' ' && c != '\t')) {
				out.append(c)
				atLineStart = false
				index++
				continue
			}
			var end = index
			var count = 0
			while (end < text.length && (text[end] == ' ' || text[end] == '\t')) {
				count++
				end++
			}
			atLineStart = false

			if (end < text.length) out.append("indent ").append(count).append(' ')
			index = end
		}
		return out.toString()
	}

	private fun isIdentifierChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

	private val COLLAPSE = Regex(" {2,}")
}
