package com.blindtechnexus.freeeloquencetts

object Numbers {

	const val AS_IS = 0
	const val DIGITS = 1
	const val PAIRS = 2
	const val TRIPLETS = 3

	val MODES = listOf(AS_IS, DIGITS, PAIRS, TRIPLETS)

	const val LONG_FROM = 6

	fun clamp(mode: Int): Int = if (MODES.contains(mode)) mode else AS_IS

	fun groupSize(mode: Int): Int = when (mode) {
		PAIRS -> 2
		TRIPLETS -> 3
		else -> 1
	}

	fun nameOf(mode: Int): String = when (clamp(mode)) {
		DIGITS -> "digits"
		PAIRS -> "pairs"
		TRIPLETS -> "triplets"
		else -> "as-is"
	}

	fun apply(text: String, mode: Int): String {
		val settled = clamp(mode)
		if (text.isEmpty()) return text

		if (!hasWork(text)) return text

		var changed = false
		val out = StringBuilder(text.length + text.length / 2 + 16)
		val n = text.length
		var i = 0
		while (i < n) {
			val c = text[i]
			if (c == '`') {

				val k = EvvEngine.annotationEnd(text, i)
				out.append(text, i, k)
				i = k
				continue
			}
			if (c == '+' && i + 1 < n && text[i + 1].isAsciiDigit()) {

				out.append("+ ")
				changed = true
				i++
				continue
			}
			if (!c.isAsciiDigit()) {
				out.append(c)
				i++
				continue
			}
			var j = i
			while (j < n && text[j].isAsciiDigit()) j++

			val groups = scanSeparators(text, j)
			val intEnd = j + groups.length

			if (isDecimalPoint(text, intEnd) || isFraction(text, i)) {
				if (groups.length > 0 && groups.corrupts) {

					out.append(text, i, j)
					var k = j
					while (k < intEnd) {
						if (text[k].isAsciiDigit()) out.append(text[k])
						k++
					}
					out.append(text[intEnd])
					k = intEnd + 1
					while (k < n && text[k].isAsciiDigit()) {
						out.append(text[k])
						k++
					}
					i = k
					changed = true
					continue
				}
				out.append(text, i, intEnd)
				i = intEnd
				continue
			}
			if (groups.length > 0 && !groups.corrupts) {

				out.append(text, i, intEnd)
				i = intEnd
				continue
			}
			if (groups.length > 0) {

				val digits = StringBuilder(j - i + groups.digits)
				digits.append(text, i, j)
				var k = j
				while (k < intEnd) {
					if (text[k].isAsciiDigit()) digits.append(text[k])
					k++
				}

				if (settled == AS_IS) out.append(digits)
				else groupDigits(digits, settled, out)
				changed = true
				i = intEnd
				continue
			}

			if (settled != AS_IS && !partOfSomethingElse(text, i, j)) {
				changed = groupDigits(text, i, j, settled, out) or changed
			} else {
				out.append(text, i, j)
			}
			i = j
		}
		return if (changed) out.toString() else text
	}

	private fun hasWork(text: String): Boolean {
		var run = 0
		val n = text.length
		var i = 0
		while (i < n) {
			val c = text[i]
			if (c.isAsciiDigit()) {
				run++
				if (run >= LONG_FROM) return true
			} else {

				if (run > 0 && (c == ',' || c == '.') && i + 3 < n &&
					text[i + 1].isAsciiDigit() && text[i + 2].isAsciiDigit() &&
					text[i + 3].isAsciiDigit() && (i + 4 >= n || !text[i + 4].isAsciiDigit())
				) {
					return true
				}
				run = 0
				if (c == '+' && i + 1 < n && text[i + 1].isAsciiDigit()) return true
			}
			i++
		}
		return false
	}

	private class Grouping(val length: Int, val digits: Int, val corrupts: Boolean)

	private val NO_GROUPING = Grouping(0, 0, false)

	private fun scanSeparators(text: String, from: Int): Grouping {
		val n = text.length
		if (from >= n) return NO_GROUPING
		val separator = text[from]
		if (separator != ',' && separator != '.') return NO_GROUPING
		var j = from
		var groups = 0
		var lastGroupZero = true
		while (j < n && text[j] == separator) {
			if (j + 3 >= n) break
			if (!text[j + 1].isAsciiDigit() || !text[j + 2].isAsciiDigit() || !text[j + 3].isAsciiDigit()) break

			if (j + 4 < n && text[j + 4].isAsciiDigit()) return NO_GROUPING
			if (text[j + 1] != '0' || text[j + 2] != '0' || text[j + 3] != '0') lastGroupZero = false
			groups++
			j += 4
		}
		if (groups == 0) return NO_GROUPING

		if (separator == '.' && groups < 2) return NO_GROUPING

		val corrupts = separator == ',' && groups >= 2 && lastGroupZero
		return Grouping(j - from, groups * 3, corrupts)
	}

	private fun isDecimalPoint(text: String, at: Int): Boolean {
		if (at >= text.length) return false
		val point = text[at]
		if (point != '.' && point != ',') return false
		return at + 1 < text.length && text[at + 1].isAsciiDigit()
	}

	private fun isFraction(text: String, start: Int): Boolean {
		if (start < 2) return false
		val point = text[start - 1]
		if (point != '.' && point != ',') return false
		return text[start - 2].isAsciiDigit()
	}

	private fun partOfSomethingElse(text: String, start: Int, end: Int): Boolean {
		val before = if (start > 0) text[start - 1] else ' '
		if (before.isLetter() || before == '/' || before == '\\' || before == '@' ||
			before == '_'
		) return true
		val after = if (end < text.length) text[end] else ' '
		if (after.isLetter() || after == '/' || after == '\\' || after == '@' ||
			after == '_'
		) return true

		if (after == '.' && end + 1 < text.length && text[end + 1].isAsciiDigit()) return true
		if (before == '.' && start >= 2 && text[start - 2].isAsciiDigit()) return true
		return false
	}

	private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

	private fun groupDigits(text: String, start: Int, end: Int, mode: Int, out: StringBuilder): Boolean {
		val length = end - start

		if (length < LONG_FROM) {
			out.append(text, start, end)
			return false
		}
		if (mode == DIGITS) {
			var k = start
			while (k < end) {
				if (k > start) out.append(' ')
				out.append(text[k])
				k++
			}
			return true
		}

		val group = groupSize(mode)
		var p = start
		var first = true
		while (p < end) {
			val size = minOf(group, end - p)
			if (!first) out.append(' ')
			first = false
			if (size > 1 && text[p] == '0') {

				out.append(text[p])
				for (k in p + 1 until p + size) {
					out.append(' ').append(text[k])
				}
			} else {
				out.append(text, p, p + size)
			}
			p += size
		}
		return true
	}

	private fun groupDigits(digits: CharSequence, mode: Int, out: StringBuilder): Boolean {
		val length = digits.length
		if (length < LONG_FROM) {
			out.append(digits)
			return false
		}
		if (mode == DIGITS) {
			for (k in 0 until length) {
				if (k > 0) out.append(' ')
				out.append(digits[k])
			}
			return true
		}
		val group = groupSize(mode)
		var p = 0
		var first = true
		while (p < length) {
			val size = minOf(group, length - p)
			if (!first) out.append(' ')
			first = false
			if (size > 1 && digits[p] == '0') {
				out.append(digits[p])
				for (k in p + 1 until p + size) {
					out.append(' ').append(digits[k])
				}
			} else {
				out.append(digits, p, p + size)
			}
			p += size
		}
		return true
	}

	fun spaced(digits: CharSequence): String {
		val out = StringBuilder(digits.length * 2)
		for (k in digits.indices) {
			if (k > 0) out.append(' ')
			out.append(digits[k])
		}
		return out.toString()
	}
}
