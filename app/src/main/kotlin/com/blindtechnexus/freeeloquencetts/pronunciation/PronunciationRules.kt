package com.blindtechnexus.freeeloquencetts.pronunciation

/**
 * The road from a word of Devanagari to the letters the engine is given.
 *
 * Four things happen, in this order, and the order is the whole of it:
 *
 * The word is looked up first. A word somebody has written down is said the way
 * it was written down, with no rule allowed to argue: that is what an exception
 * is.
 *
 * The letters are cut into syllables, because the script gives no other unit to
 * work with.
 *
 * The inherent vowels the language does not say are taken away. This has to
 * happen after the syllables exist, because it is which vowel is where that
 * decides it, and before anything is spelled out, because a vowel that is not
 * said must not be written.
 *
 * Each syllable is spelled: the cluster, then its vowel, then whatever nasal or
 * breath follows it. The result is the respelling.
 *
 * What this does *not* do is choose languages. It is handed the one to use, and
 * [DevanagariPronunciation] guesses when nobody says, which is the case for a
 * word a caller dropped into an English sentence.
 */
internal object PronunciationRules {

	/** [word] as the engine should be told to say it. */
	fun pronounce(word: String, language: SpokenLanguage): String {
		if (word.isEmpty()) return word
		// The dictionaries are the exceptions, and the exceptions win.
		HindiLexicon.lookup(word)?.let { return it }
		NepaliLexicon.lookup(word)?.let { return it }
		val syllables = Syllabifier.syllabify(word).toMutableList()
		if (syllables.isEmpty()) return ""
		SchwaDeletion.apply(syllables, language)
		if (SchwaDeletion.keepsFinalSchwa(language, word)) {
			// A Nepali verb ending whose final vowel is written down as part
			// of the grammar: put back what the rule took away.
			syllables.lastOrNull()?.takeIf { it.onset.isNotEmpty() }?.let { last ->
				if (!last.writtenVowel && last.vowel == null) last.vowel = Devanagari.SCHWA
			}
		}
		val out = StringBuilder(word.length + 8)
		for (at in syllables.indices) {
			val syllable = syllables[at]
			out.append(Conjuncts.onset(syllable.onsetText()))
			syllable.vowel?.let { out.append(it.roman) }
			out.append(Nasalization.coda(syllable, syllables.getOrNull(at + 1)))
		}
		return Conjuncts.tidy(out.toString())
	}

	/**
	 * Which of the two the script is being read as, from the words themselves.
	 *
	 * Used when nothing better has said so -- a preview on the settings screen,
	 * a sentence a caller wrote in Devanagari without asking for a language.
	 * ML Kit is asked first everywhere else, and this is deliberately the
	 * cheap second: a handful of grammar words that only one of the two
	 * languages uses, counted, and the larger count winning. Hindi wins ties,
	 * because there is more Hindi in the world and a wrong guess between the
	 * two is heard as an accent rather than as a mistake.
	 */
	fun guessLanguage(text: String): SpokenLanguage {
		var hindi = 0
		var nepali = 0
		for (word in text.split(' ', '\t', '\n', '\u0964', '\u0965', ',', ';', ':', '?', '!')) {
			if (word.isEmpty()) continue
			when {
				word in NEPALI_MARKERS -> nepali++
				word in HINDI_MARKERS -> hindi++
			}
		}
		return if (nepali > hindi) SpokenLanguage.NEPALI else SpokenLanguage.HINDI
	}

	/**
	 * The words that mark Nepali: its copula, its participial endings and the
	 * postpositions it does not share with Hindi.
	 */
	val NEPALI_MARKERS: Set<String> = setOf(
		"छ", "छन्", "छु", "छौ", "छैन", "छैनन्", "हुन्छ", "हुन्छन्", "हुन्छु",
		"हुनुहुन्छ", "हुने", "हुन", "होइन", "थियो", "थिए", "गर्छ", "गर्छन्",
		"गर्छु", "गर्ने", "गर्नु", "भयो", "भए", "भन्छ", "भन्ने", "आउँछ",
		"जान्छ", "दिन्छ", "पाउँछ", "सक्छ", "पर्छ", "रहन्छ", "रहेको", "गरेको",
		"भएको", "दिएको", "आएको", "गएको", "लाई", "बाट", "देखि", "सँग", "पनि",
		"अनि", "किनभने", "होस्", "होला", "तपाईं", "तपाई", "हामी", "उनी",
		"उहाँ", "यो", "त्यो", "कसरी", "कहिले", "धेरै", "थोरै", "सबै", "केही"
	)

	/**
	 * The words that mark Hindi: its copula and the postpositions that Nepali
	 * writes differently.
	 */
	val HINDI_MARKERS: Set<String> = setOf(
		"है", "हैं", "हूँ", "हूं", "था", "थी", "थे", "होगा", "होगी", "होंगे",
		"हुआ", "हुई", "हुए", "होता", "होती", "होते", "करना", "करता", "करती",
		"करते", "किया", "जाना", "जाता", "जाती", "जाते", "गया", "गई", "गए",
		"आना", "आता", "आती", "आते", "देना", "दिया", "लेना", "लिया", "चाहिए",
		"चाहता", "सकता", "सकते", "रहा", "रही", "रहे", "का", "की", "के", "को",
		"से", "में", "पर", "तक", "लिए", "साथ", "बिना", "और", "या", "लेकिन",
		"मगर", "क्योंकि", "अगर", "तो", "भी", "ही", "नहीं", "सब", "कुछ",
		"बहुत", "थोड़ा", "ज़्यादा", "ज्यादा", "अब", "फिर", "आज", "कल", "अभी",
		"पहले", "बाद", "यहाँ", "वहाँ", "वह", "वो", "यह", "ये", "मैं", "मुझे",
		"मेरा", "मेरी", "मेरे", "हम", "हमें", "तुम", "आप", "आपको"
	)
}
