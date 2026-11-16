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
	fun pronounce(word: String, language: SpokenLanguage): String =
		pronounce(word, language, allowSuffixes = true)

	/**
	 * The same, with the suffix pass made optional.
	 *
	 * The suffix pass calls back into here for the stem it found, and a stem
	 * that happens to end in a suffix of its own must not be cut twice: the
	 * second call comes with [allowSuffixes] off, so "काठमाडौंमा" is the entry
	 * for काठमाडौं plus मा and not a stem of a stem.
	 */
	private fun pronounce(word: String, language: SpokenLanguage, allowSuffixes: Boolean): String {
		if (word.isEmpty()) return word
		// The dictionaries are the exceptions, and the exceptions win. The two
		// are asked in the order the language being spoken says: a word both
		// lists carry -- समय, विद्यालय -- is said the Hindi way in Hindi and
		// the Nepali way in Nepali, and only falls back to the other list when
		// its own has never heard of it.
		val known = when (language) {
			SpokenLanguage.NEPALI -> NepaliLexicon.lookup(word) ?: HindiLexicon.lookup(word)
			else -> HindiLexicon.lookup(word) ?: NepaliLexicon.lookup(word)
		}
		known?.let { return it }
		// A word carrying a suffix the grammar added -- काठमाडौंमा, घरमा,
		// भारतको, बच्चाहरू -- is its stem said the way the stem is said, and
		// the suffix after it. The rules alone would say the letters of the
		// suffix but not delete the stem's own last vowel, which is where
		// "kaathumaadaummaa" came from.
		if (allowSuffixes) suffixSplit(word, language)?.let { return it }
		return sayByRules(word, language)
	}

	/**
	 * [word] cut into a stem that can be said well and the suffix after it, or
	 * null when no suffix of this language ends it.
	 *
	 * Longest suffix first, so माथि is "माथि" and not मा + थि. The stem has to
	 * be at least two letters, because a stem of one letter is a suffix that
	 * happened to be in the middle of a word.
	 */
	private fun suffixSplit(word: String, language: SpokenLanguage): String? {
		val suffixes = if (language == SpokenLanguage.NEPALI) NEPALI_SUFFIXES else HINDI_SUFFIXES
		for ((suffix, roman) in suffixes) {
			if (word.length < suffix.length + 2 || !word.endsWith(suffix)) continue
			val stem = word.substring(0, word.length - suffix.length)
			if (!Syllabifier.hasDevanagari(stem)) continue
			val said = try {
				pronounce(stem, language, allowSuffixes = false)
			} catch (_: Throwable) {
				continue
			}
			if (said.isEmpty()) continue
			return said + roman
		}
		return null
	}

	/** [word] said by the letter rules alone, which is what an unknown word
	 *  gets. */
	private fun sayByRules(word: String, language: SpokenLanguage): String {
		val syllables = Syllabifier.syllabify(word).toMutableList()
		if (syllables.isEmpty()) return ""
		// A word that opens with a cluster -- प्र, क्र, स्त, स्क -- carries a
		// vowel on the first consonant even though the virama took the written
		// one away: the virama joins the two letters into one onset, and
		// without a vowel in front of it the respelling is "pr", which an
		// English voice reads as two letters and not as a syllable. So the
		// schwa goes back on the opening syllable, where the deletion rule
		// below will still take it away again if it turns out to be the only
		// one -- प्रविधि is "pruvidhi", and नगर is still "nugur".
		val opening = syllables.firstOrNull()
		if (
			syllables.size > 1 && opening != null && opening.vowel == null &&
			!opening.writtenVowel && opening.onset.length > 1
		) {
			opening.vowel = Devanagari.SCHWA
		}
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
	 * The endings the grammar adds, longest first: the case markers, the
	 * postpositions, the plural and the emphatic. Written as the respelling the
	 * suffix itself is said with, so the stem's last vowel and the suffix's
	 * first consonant meet exactly where the speaker's tongue does.
	 */
	private val HINDI_SUFFIXES: List<Pair<String, String>> = listOf(
		"हरूले" to "hurooleh", "हरूको" to "hurookoh", "हरूमा" to "huroomaa",
		"हरू" to "huroo",
		"वाला" to "vaalaa", "वाले" to "vaaleh", "वाली" to "vaalee",
		"करने" to "kurneh", "करना" to "kurnaa", "करता" to "kurtaa",
		"में" to "mehn", "मे" to "meh", "से" to "seh", "को" to "koh",
		"का" to "kaa", "की" to "kee", "के" to "keh", "पर" to "pur",
		"तक" to "tuk", "ने" to "neh", "ओं" to "on", "ें" to "ain",
		"ों" to "on", "ों" to "on", "एं" to "ain"
	)

	/** The same for Nepali: the case markers it does not share with Hindi, its
	 *  plural, and the participial endings its verbs wear. */
	private val NEPALI_SUFFIXES: List<Pair<String, String>> = listOf(
		"हरूलाई" to "huroolaaee", "हरूसँग" to "huroosung", "हरूमा" to "huroomaa",
		"हरूको" to "hurookoh", "हरूले" to "hurooleh", "हरू" to "huroo",
		"लाई" to "laaee", "सँग" to "sung", "देखि" to "dehkhi", "बाट" to "baat",
		"सम्म" to "summ", "मा" to "maa", "को" to "koh", "का" to "kaa",
		"की" to "kee", "ले" to "leh", "लाई" to "laaee", "नै" to "nai"
	)

	/**
	 * Which of the two the script is being read as, from the words themselves.
	 *
	 * Used when nothing better has said so -- a preview on the settings screen,
	 * a sentence a caller wrote in Devanagari without asking for a language.
	 * Elsewhere the answer comes from
	 * [com.blindtechnexus.freeeloquencetts.LanguageIdentifier], which asks the
	 * same question of the same words: a handful of grammar words that only
	 * one of the two languages uses, counted, and the larger count winning.
	 * Hindi wins ties, because there is more Hindi in the world and a wrong
	 * guess between the two is heard as an accent rather than as a mistake.
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
