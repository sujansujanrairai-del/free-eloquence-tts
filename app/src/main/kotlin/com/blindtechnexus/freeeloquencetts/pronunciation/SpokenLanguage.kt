package com.blindtechnexus.freeeloquencetts.pronunciation

/**
 * Which language the script is being read as.
 *
 * Hindi and Nepali share Devanagari and share most of their words, and they do
 * not share their grammar, their recent vocabulary or their pronunciation of
 * the letters that matter most: ज्ञ is "gy" in Hindi and "gya" in Nepali, ऋ is
 * "ri" in both, and the final schwa is dropped in both but kept in different
 * places. So the rules are written twice where they differ and once where they
 * do not, and this is the name of the choice.
 *
 * [iso] is what [com.blindtechnexus.freeeloquencetts.LanguageIdentifier]
 * answers for the language, which is what lets a detected span and this
 * package agree without a table in between.
 */
enum class SpokenLanguage(val iso: String, val label: String) {

	/** Hindi, as spoken in Delhi and written in Devanagari. */
	HINDI("hi", "Hindi"),

	/** Nepali, as spoken in Kathmandu and written in Devanagari. */
	NEPALI("ne", "Nepali");

	companion object {

		/** The language [iso] names, or Hindi for anything else: Hindi is the
		 *  larger body of text, and a Devanagari word with no answer is more
		 *  likely Hindi than nothing. */
		fun of(iso: String?): SpokenLanguage = when (iso?.lowercase()) {
			"ne", "nep" -> NEPALI
			else -> HINDI
		}
	}
}
