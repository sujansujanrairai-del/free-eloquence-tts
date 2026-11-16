package com.blindtechnexus.freeeloquencetts.pronunciation

/**
 * A word of Devanagari, cut into the syllables it is said in.
 *
 * The state machine is small because the script is: a consonant carries the
 * inherent vowel, a matra replaces it, a virama takes it away and lets the next
 * consonant join the onset, and a nasal or a visarga is written after the vowel
 * it belongs to. Everything else -- the nukta on a letter, the two joiners, the
 * Vedic accents -- changes how the word is drawn or is a mark with no sound,
 * and is consumed without being said.
 *
 * The one subtlety is the virama, which is decided by lookahead rather than by
 * the letter before it: क in कमल ends its syllable with the inherent vowel, and
 * the same क in क्रम is only the first half of the onset "kr". So a consonant
 * leaves its syllable open to a virama, and a virama closes it to a vowel but
 * not to another consonant.
 */
internal object Syllabifier {

	/** The syllable list of [word], in order. A word with no Devanagari letter
	 *  in it comes back empty. */
	fun syllabify(word: String): List<Akshara> {
		val out = ArrayList<Akshara>(word.length)
		var current: Akshara? = null

		/** Whether the next consonant may join the onset: true only right after
		 *  a virama, which is the only thing that takes a vowel away. */
		var clusterOpen = false

		fun flush() {
			val held = current
			if (held != null && !held.isEmpty) out.add(held)
			current = null
			clusterOpen = false
		}

		fun start(): Akshara {
			val made = Akshara()
			current = made
			return made
		}

		var i = 0
		while (i < word.length) {
			val c = word[i]
			when {
				Devanagari.isConsonant(c) -> {
					val base = c
					var roman = Devanagari.CONSONANTS.getValue(base).roman
					// A nukta is a letter of its own: क़ is "q", not "k".
					if (i + 1 < word.length && word[i + 1] == Devanagari.NUKTA) {
						roman = Devanagari.NUKTA_FORMS[base] ?: roman
						i++
					}
					if (!clusterOpen || current == null) {
						if (current != null) flush()
						start().onset.append(roman)
					} else {
						current?.onset?.append(roman)
					}
					clusterOpen = false
				}

				c == Devanagari.VIRAMA -> {
					// The vowel goes and the next consonant may join.
					current?.vowel = null
					clusterOpen = true
				}

				Devanagari.isMatra(c) -> {
					val held = current ?: start()
					held.vowel = Devanagari.MATRAS[c]
					held.writtenVowel = true
					clusterOpen = false
				}

				Devanagari.isIndependent(c) -> {
					if (current != null) flush()
					val held = start()
					held.vowel = Devanagari.INDEPENDENT[c]
					held.independent = true
					held.writtenVowel = true
					clusterOpen = false
				}

				c == Devanagari.ANUSVARA || c == Devanagari.CHANDRABINDU -> {
					val held = current
					if (held != null) held.nasal = c
				}

				c == Devanagari.VISARGA -> {
					current?.visarga = true
				}

				c == Devanagari.OM -> {
					if (current != null) flush()
					start().onset.append("om").also { current?.vowel = null }
					flush()
				}

				c == Devanagari.AVAGRAHA -> {
					// An avagraha is a swallowed vowel. Saying nothing is
					// right; joining the two halves is what the eye does.
					clusterOpen = false
				}

				c == Devanagari.ZERO_WIDTH_JOINER || c == Devanagari.ZERO_WIDTH_NON_JOINER -> {
					// Decides how the glyph is drawn, not how it is said.
				}

				c == Devanagari.NUKTA -> {
					// A nukta with no letter before it is a stray mark.
				}

				Devanagari.isDevanagari(c) || Devanagari.isSilentMark(c) -> {
					// Punctuation and accents are the caller's to deal with;
					// inside a word they are not said.
				}

				else -> {
					// Anything else -- a Latin letter, a digit, a hyphen --
					// ends the Devanagari run.
					flush()
				}
			}
			i++
		}
		flush()
		return out
	}

	/** Whether [word] holds at least one letter of the script. */
	fun hasDevanagari(word: String): Boolean {
		for (c in word) if (Devanagari.isLetter(c) && Devanagari.isDevanagari(c)) return true
		return false
	}
}
