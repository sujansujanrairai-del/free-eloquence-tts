package com.blindtechnexus.freeeloquencetts.pronunciation

/**
 * Devanagari text, turned into letters this engine can say.
 *
 * This is the one door the rest of the app uses. It is total, it never throws,
 * and text with no Devanagari in it comes back as the same object rather than as
 * a copy: the ordinary English sentence a screen reader reads should pay one
 * scan of its characters and nothing else.
 *
 * What comes out is a respelling and not a translation. "नमस्ते" becomes
 * "nuhmusteh", which the English voice says the way a Hindi speaker says the
 * word -- not perfectly, because an English voice has no retroflex stops and no
 * nasal vowels, but recognisably, and with the vowels in the right places. The
 * alternative was what this app used to do: hand the engine bytes it had no
 * letters for and let it read the word as though it were English spelling,
 * which is where "naagarna" came from.
 *
 * Everything that is not a Devanagari word is left exactly as it was found,
 * including the spaces around it, so "कल मिलते हैं" comes back as
 * "kul milteh hain" with the same word boundaries it started with.
 */
object DevanagariPronunciation {

	/** Whether [text] holds any letter of the script. */
	fun hasDevanagari(text: String): Boolean {
		for (c in text) {
			if (Devanagari.isDevanagari(c) && Devanagari.isLetter(c)) return true
		}
		return false
	}

	/** Whether [word] is a word of the script. */
	fun isDevanagariWord(word: String): Boolean = Syllabifier.hasDevanagari(word)

	/**
	 * [text] with every Devanagari word respelled, and everything else left
	 * alone.
	 *
	 * [language] is which of the two sets of rules to use. Left null -- which
	 * is what a caller that has no language detection in front of it does --
	 * the text is guessed at from its own grammar words, and Hindi wins a tie.
	 */
	fun apply(text: String, language: SpokenLanguage? = null): String {
		if (text.isEmpty() || !hasDevanagari(text)) return text
		val chosen = language ?: PronunciationRules.guessLanguage(text)
		val out = StringBuilder(text.length + 24)
		val word = StringBuilder(16)

		fun flushWord() {
			if (word.isEmpty()) return
			val said = try {
				PronunciationRules.pronounce(word.toString(), chosen)
			} catch (_: Throwable) {
				// One word is not the sentence. What is left of it is what
				// the rules could make of it, and silence is not better.
				""
			}
			out.append(said)
			word.setLength(0)
		}

		var at = 0
		while (at < text.length) {
			val c = text[at]
			when {
				Devanagari.isLetter(c) -> word.append(c)
				c == Devanagari.ZERO_WIDTH_JOINER || c == Devanagari.ZERO_WIDTH_NON_JOINER ->
					if (word.isNotEmpty()) word.append(c)
				Devanagari.DIGITS.containsKey(c) -> {
					flushWord()
					out.append(Devanagari.DIGITS.getValue(c))
				}
				c == Devanagari.DANDA || c == Devanagari.DOUBLE_DANDA -> {
					// The full stop of the script is a full stop here too, so
					// that the sentence the engine is given ends where the
					// sentence ended.
					flushWord()
					out.append(". ")
				}
				Devanagari.isDevanagari(c) -> {
					// Punctuation and marks with no sound: the word ends here
					// and nothing is written for the mark itself.
					flushWord()
					out.append(' ')
				}
				else -> {
					flushWord()
					out.append(c)
				}
			}
			at++
		}
		flushWord()
		// A run of Devanagari punctuation can leave two spaces behind, and the
		// engine reads a double space as a slightly longer pause than the
		// writer meant.
		return collapseSpaces(out)
	}

	/** One whole word, said. Used by the caller that has already decided which
	 *  language a span is in and does not want the guess. */
	fun pronounceWord(word: String, language: SpokenLanguage): String =
		PronunciationRules.pronounce(word, language)

	/** The language a run of the script is most likely to be, for a caller with
	 *  no detector in front of it. */
	fun guessLanguage(text: String): SpokenLanguage = PronunciationRules.guessLanguage(text)

	private fun collapseSpaces(text: StringBuilder): String {
		var spaced = false
		var at = 0
		while (at < text.length) {
			if (text[at] == ' ') {
				if (spaced) {
					text.deleteCharAt(at)
					continue
				}
				spaced = true
			} else {
				spaced = false
			}
			at++
		}
		return text.toString()
	}
}
