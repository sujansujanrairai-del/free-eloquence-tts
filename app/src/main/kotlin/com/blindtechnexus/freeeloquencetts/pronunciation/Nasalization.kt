package com.blindtechnexus.freeeloquencetts.pronunciation

/**
 * The nasal that is written above the line, and the breath written after it.
 *
 * An anusvara is not an "n". It is a nasalisation of the vowel that takes its
 * place of articulation from the consonant after it: the ं of हिंदी is said
 * against the द, and the ं of थंब is said against the ब and comes out as an
 * "m". English has no nasal vowels, so what is written here is the consonant
 * that English would put in the same place -- "n" before most letters, "m"
 * before a labial, "ng" before a velar, "nj" before a palatal. That is the
 * closest an English voice can be asked for, and it is what makes "hindi"
 * come out as three syllables instead of "hid".
 *
 * A chandrabindu nasalises the vowel itself and has no consonant after it; it
 * is written as a final "n", which is the approximation every romanisation
 * uses.
 *
 * A visarga is a breath: "h" and nothing more, as in दुःख, "duhkh".
 */
internal object Nasalization {

	/** What is said after the vowel of [syllable], with [next] being the
	 *  syllable that follows it in the word, if there is one. */
	fun coda(syllable: Akshara, next: Akshara?): String {
		val nasal = syllable.nasal
		if (nasal == '\u0000') {
			return if (syllable.visarga) "h" else ""
		}
		if (nasal == Devanagari.CHANDRABINDU) {
			// The nasalisation belongs to the vowel. Writing "n" after it is
			// the closest an English voice gets, and it is stable: it never
			// becomes an "m" because a chandrabindu has no consonant to agree
			// with.
			return "n"
		}
		return place(next)
	}

	/** The nasal of the place of articulation of the next consonant. */
	private fun place(next: Akshara?): String {
		val onset = next?.onsetText().orEmpty()
		if (onset.isEmpty()) return "n"
		return when {
			// Labials: the lips are already closed, so the nasal is an "m".
			onset.startsWith("ph") || onset.startsWith("bh") || onset.startsWith("p") ||
				onset.startsWith("b") || onset.startsWith("m") || onset.startsWith("v") ||
				onset.startsWith("f") -> "m"
			// Velars: the back of the tongue is already up.
			onset.startsWith("kh") || onset.startsWith("gh") || onset.startsWith("k") ||
				onset.startsWith("g") -> "ng"
			// Palatals.
			onset.startsWith("chh") || onset.startsWith("ch") || onset.startsWith("jh") ||
				onset.startsWith("j") || onset.startsWith("sh") -> "nj"
			// Everything else -- the dentals, the retroflexes, the liquids and
			// the sibilants -- takes an "n".
			else -> "n"
		}
	}
}
