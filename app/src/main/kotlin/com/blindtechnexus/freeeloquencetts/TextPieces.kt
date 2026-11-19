package com.blindtechnexus.freeeloquencetts

object TextPieces {

	const val PIECE_CAP = 250

	class Piece(val text: String, val forced: Boolean)

	private const val CLOSERS = "\"')]}»”’"

	private const val ENDERS = "?!…。！？"

	fun endsSentence(word: String): Boolean = endsSentence(word, 0, word.length)

	fun endsSentence(text: CharSequence, from: Int, to: Int): Boolean {
		var end = to
		while (end > from && text[end - 1] in CLOSERS) end--
		if (end == from) return false
		val last = text[end - 1]
		if (last in ENDERS) return true
		if (last != '.' || end - from < 2 || text[end - 2].isDigit()) return false
		var inside = false
		for (at in from until end - 1) {
			if (text[at] == '.') {
				inside = true
				break
			}
		}
		if (inside) return false
		return !(end - 1 - from <= 3 && text[from].isUpperCase())
	}

	fun split(text: String): List<String> = pieces(text).map { it.text }

	fun pieces(text: String): List<Piece> = pieceSequence(text).toList()

	fun pieceSequence(text: String): Sequence<Piece> = sequence {
		if (text.length <= PIECE_CAP && !text.any { it.isWhitespace() }) {
			yield(Piece(text, false))
			return@sequence
		}
		var held: Piece? = null
		var yielded = false
		var start = 0
		var sentence = false
		var at = 0
		while (at < text.length) {
			val space = text[at].isWhitespace()
			val from = at
			while (at < text.length && text[at].isWhitespace() == space) at++
			if (!space) {
				sentence = endsSentence(text, from, at)
				continue
			}

			if (sentence || at - start >= PIECE_CAP) {
				val previous = held
				if (previous != null) {
					yield(previous)
					yielded = true
				}
				held = Piece(text.substring(start, at), !sentence)
				start = at
			}
			sentence = false
		}
		if (start < text.length) {
			val rest = text.substring(start)
			val previous = held
			when {
				rest.isNotBlank() -> {
					if (previous != null) {
						yield(previous)
						yielded = true
					}
					held = Piece(rest, false)
				}

				previous != null -> held = Piece(previous.text + rest, previous.forced)
				else -> held = null
			}
		}
		val last = held
		if (last != null) {
			yield(last)
			yielded = true
		}

		if (!yielded) yield(Piece(text, false))
	}
}
