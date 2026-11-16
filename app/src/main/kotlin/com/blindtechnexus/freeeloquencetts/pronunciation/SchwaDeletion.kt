package com.blindtechnexus.freeeloquencetts.pronunciation

/**
 * Which inherent vowels are said and which are not.
 *
 * This is the rule that a Western reader of Devanagari never applies and every
 * Hindi speaker applies without thinking. कमल is written ku-mu-lu and said
 * "kamal": the last inherent vowel is not there at all. The name of the rule is
 * schwa deletion, and Hindi does it at the end of a word almost always, and in
 * the middle of one word by word -- which is the part that is not derivable and
 * is why [HindiLexicon] exists.
 *
 * Two things are deliberately *not* done here, and both were tempting:
 *
 * Medial schwa is not deleted by rule. A rule that deletes every internal
 * schwa after the first syllable turns नगरने into "nagrne", which is not what
 * anybody says; a rule that only deletes when the letters either side allow it
 * turns गरम into "garm", which is right, and कमला into "kamla", which is wrong.
 * Hindi speakers do this by knowing the word, so the words that need it are
 * written down instead of guessed at.
 *
 * Nepali is not treated as Hindi with a flag. Nepali keeps more final schwas
 * than Hindi (its grammar writes them as part of the verb endings), so the
 * dictionary decides where the two differ, and the rule below -- which is the
 * right one for both -- is applied to both.
 */
internal object SchwaDeletion {

	/** Takes the final inherent vowel away where the language does. */
	fun apply(syllables: MutableList<Akshara>, language: SpokenLanguage) {
		if (syllables.size < 2) {
			// A one-syllable word keeps its vowel: "क" said as a bare "k" is
			// not a word, it is a consonant with nothing to carry it.
			return
		}
		val last = syllables[syllables.size - 1]
		if (!last.hasSchwa) return
		if (last.onset.isEmpty() && !last.independent) return
		// A word ending in a written vowel keeps it; there is nothing inherent
		// about a vowel somebody took the trouble to write.
		if (last.writtenVowel) return
		last.vowel = null
	}

	/**
	 * Whether a vowel written against a final consonant is one Nepali keeps
	 * where Hindi drops it. Used only by the dictionaries, and kept here so
	 * that the two languages' difference is written down in one place.
	 */
	fun keepsFinalSchwa(language: SpokenLanguage, word: String): Boolean = when (language) {
		SpokenLanguage.NEPALI -> word in NEPALI_FINAL_SCHWA
		else -> false
	}

	/**
	 * The Nepali words that keep a final schwa Hindi would drop, which is
	 * mostly the participial and infinitival endings: गर्नु, भन्नु, हुनु.
	 */
	private val NEPALI_FINAL_SCHWA: Set<String> = setOf(
		"गर्नु", "भन्नु", "हुनु", "जानु", "आउनु", "खानु", "पिनु", "सुन्नु",
		"पढ्नु", "लेख्नु", "दिनु", "लिनु", "बस्नु", "उठ्नु", "सुत्नु", "रुनु",
		"हस्नु", "गाउनु", "नाच्नु", "किन्नु", "बेच्नु", "गर्न", "भन्न", "हुन"
	)
}
