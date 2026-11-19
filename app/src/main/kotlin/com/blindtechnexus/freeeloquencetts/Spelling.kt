package com.blindtechnexus.freeeloquencetts

object Spelling {

	const val OFF = 0

	const val LETTERS = 1

	const val NATO = 2

	const val CAPITALS = 3

	const val CAPITALS_NATO = 4

	val MODES = listOf(OFF, LETTERS, NATO, CAPITALS, CAPITALS_NATO)

	private val ALPHABET = arrayOf(
		"Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf",
		"Hotel", "India", "Juliett", "Kilo", "Lima", "Mike", "November",
		"Oscar", "Papa", "Quebec", "Romeo", "Sierra", "Tango", "Uniform",
		"Victor", "Whiskey", "X-ray", "Yankee", "Zulu"
	)

	private val DIGITS = arrayOf(
		"Zero", "One", "Two", "Three", "Four",
		"Five", "Six", "Seven", "Eight", "Nine"
	)

	private const val MIN_ACRONYM = 3

	fun apply(text: String, mode: Int): String {
		if (mode == OFF || text.isEmpty()) return text
		val phonetic = mode == NATO || mode == CAPITALS_NATO
		val capitalsOnly = mode == CAPITALS || mode == CAPITALS_NATO
		val out = StringBuilder(text.length * 4)
		var at = 0
		var changed = false
		while (at < text.length) {
			val c = text[at]
			if (c == '`') {

				val end = EvvEngine.annotationEnd(text, at)
				out.append(text, at, end)
				at = end
				continue
			}
			if (!c.isLetterOrDigit()) {
				out.append(c)
				at++
				continue
			}
			val end = wordEnd(text, at)
			val word = text.substring(at, end)
			if (capitalsOnly && !isAcronym(word)) {
				out.append(word)
			} else {
				spell(word, phonetic, out)
				changed = true
			}
			at = end
		}
		return if (changed) out.toString() else text
	}

	private fun spell(word: String, phonetic: Boolean, out: StringBuilder) {
		var first = true
		for (c in word) {
			val said = nameOf(c, phonetic)
			if (!first) out.append(' ')
			out.append(said)
			first = false
		}
	}

	private fun nameOf(c: Char, phonetic: Boolean): String {
		if (!phonetic) return c.toString()
		return when {
			c in 'a'..'z' -> ALPHABET[c - 'a']
			c in 'A'..'Z' -> ALPHABET[c - 'A']
			c in '0'..'9' -> DIGITS[c - '0']
			else -> c.toString()
		}
	}

	private fun isAcronym(word: String): Boolean {
		if (word.length < MIN_ACRONYM) return false
		var letters = 0
		for (c in word) {
			if (c.isLowerCase()) return false
			if (c.isLetter()) letters++
		}
		return letters >= MIN_ACRONYM
	}

	private fun wordEnd(text: String, from: Int): Int {
		var j = from
		while (j < text.length && text[j].isLetterOrDigit()) j++
		return j
	}
}
