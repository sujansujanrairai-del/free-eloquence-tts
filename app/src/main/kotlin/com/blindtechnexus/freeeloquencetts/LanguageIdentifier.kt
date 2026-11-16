package com.blindtechnexus.freeeloquencetts

import java.text.Normalizer

/**
 * The language a span of text is written in, decided here rather than asked of
 * a library.
 *
 * It used to be ML Kit's language identification, and the shape of the answer
 * was the reason: a model that answers in milliseconds sounds like exactly what
 * a speech engine wants. What it costs is a native library of its own inside the
 * APK -- one megabyte for arm64-v8a alone, plus three more ABIs and a tensorflow
 * model -- and the packaging of that library is not ours. A .so that Gradle's
 * strip task rewrites on the way into the archive is a .so whose archive entry
 * can come out malformed, and a malformed entry is a warning from every tool
 * that reads the APK and, on the day it is worse than a warning, an app that
 * will not install or a library that will not load. Neither is a risk worth one
 * accuracy point on a decision the router already treats as a guess.
 *
 * So the question is answered with the evidence that is in the text, and the
 * answer is a plain function of it:
 *
 * The script comes first. Devanagari is Hindi or Nepali and nothing else, and
 * the two are told apart by the words that are only one of them (छ and हुन्छ
 * against है and नहीं) and by the nukta letters that Hindi writes and Nepali
 * does not. A span in Cyrillic, Greek, Arabic or CJK is a language with no
 * module in this engine, which is answered as "no answer" so that the caller's
 * own voice keeps it.
 *
 * Within the Latin script, the eight languages this app speaks are English,
 * German, Spanish, French and Italian, and each is recognised by its commonest
 * words and by the letters and endings that only it writes. A word is worth its
 * own length, so that "nicht" counts for more than "am" and a long unambiguous
 * word counts for more than a short one that several languages share. Nothing
 * is decided on a single ambiguous word: a score has to reach [LEAST] and has to
 * beat the runner-up by [MARGIN], and a span that cannot is answered "no
 * answer", which keeps the language the caller asked for. A guess that is wrong
 * is a small fault; a guess that is confidently wrong about a whole sentence is
 * the one worth avoiding.
 *
 * All of it is a table lookup over the words of a span, so a sentence costs
 * microseconds and needs no thread, no future and no deadline. That matters
 * because the router asks on the framework's one synthesis thread, where every
 * millisecond spent deciding is a millisecond of silence in front of the first
 * syllable.
 *
 * The answer is an ISO-639-1 code, lower case, or null for no answer. What the
 * caller does with it is [LanguageRouter]'s business: a Detected language it
 * has no module for is spoken in the language the request already had.
 */
internal object LanguageIdentifier {

	/** A span with fewer letters than this is not a language: a stray "a" in
	 *  the middle of a German word is not English. */
	private const val SHORTEST = 2

	/** What a language has to score before it is believed. Two short common
	 *  words, or one long distinctive one. */
	private const val LEAST = 6

	/**
	 * How far ahead of the runner-up the winner has to be, as a percentage.
	 *
	 * A hundred and twenty-five means the winner needs a quarter more evidence
	 * than the second: at that distance the answer is a sentence's worth of
	 * agreement rather than one lucky word, and below it the honest answer is
	 * that the span could be either.
	 */
	private const val MARGIN = 125

	/** The languages this engine has a module for, in the order the score array
	 *  is indexed. */
	private val LANGS = arrayOf("en", "de", "es", "fr", "it")

	/** Whether detection can be asked at all. It can: the whole of it is in
	 *  this file. The router still asks, because a build with the table empty
	 *  should fall back to the caller's language rather than guess. */
	fun available(): Boolean = COMMON.isNotEmpty()

	/** The ISO-639-1 code [text] is written in, or null for no answer. */
	fun detect(text: String): String? = decide(text)

	/**
	 * The codes for [texts], in the same order the spans came in.
	 *
	 * No pool and no futures any more: the answer for a span is arithmetic over
	 * its words, so the list is done in one pass and the order is the list's.
	 */
	fun detectAll(texts: List<String>): List<String?> =
		if (texts.isEmpty()) emptyList() else texts.map { decide(it) }

	/**
	 * The answer for one span, which is the whole of the decision.
	 */
	private fun decide(text: String): String? {
		val trimmed = text.trim()
		if (trimmed.length < SHORTEST) return null
		var letters = 0
		var devanagari = 0
		var latin = 0
		for (c in trimmed) {
			if (!c.isLetter()) continue
			letters++
			when {
				c in '\u0900'..'\u097F' -> devanagari++
				// Latin-1 letters to the end of Latin Extended-B: the letters
				// of the five languages below, accents and all. Anything else
				// that is a letter is a script this engine has no module for.
				c.code < 0x0250 -> latin++
			}
		}
		if (letters == 0) return null
		// Half or more of the letters in one script is that script's question.
		if (devanagari * 2 >= letters) return devanagariLanguage(trimmed)
		if (latin * 2 < letters) return null
		return latinLanguage(trimmed)
	}

	// ---- Devanagari --------------------------------------------------------

	/**
	 * Hindi or Nepali.
	 *
	 * The two share a script and most of a vocabulary, so what separates them
	 * is grammar: the Nepali छ, छन् and हुन्छ against the Hindi है, हैं and
	 * नहीं, and the case markers each language owns (लाई and बाट against में and
	 * को). Hindi also writes the nukta letters क़ ख़ ग़ ज़ फ़ ड़ ढ़, which Nepali
	 * leaves out, and a span with several of them is Hindi whatever the words
	 * say.
	 *
	 * A tie is Hindi, which is the larger body of text and the larger
	 * dictionary here; the respelling consults both dictionaries anyway, so a
	 * wrong answer costs an accent rather than a word.
	 */
	private fun devanagariLanguage(text: String): String {
		var nepali = 0
		var hindi = 0
		for (word in wordsOf(text)) {
			if (word in NEPALI_MARKERS) nepali += word.length.coerceIn(2, 6)
			if (word in HINDI_MARKERS) hindi += word.length.coerceIn(2, 6)
		}
		// The nukta is a dot under a letter and is written as its own code
		// point when the letter is typed rather than composed.
		for (c in text) {
			if (c == '\u093C') hindi += 4
		}
		return if (nepali > hindi) "ne" else "hi"
	}

	// ---- Latin script ------------------------------------------------------

	/**
	 * Which of the five Latin-script languages [text] is, or null.
	 *
	 * Three kinds of evidence, added up per language: the common words
	 * ([COMMON]), the letters only one of them writes ([LETTERS]), and the
	 * endings its words take ([ENDINGS]). The winner has to clear [LEAST] and
	 * lead by [MARGIN] before it is returned.
	 */
	private fun latinLanguage(text: String): String? {
		val scores = IntArray(LANGS.size)
		for (word in wordsOf(text)) {
			val normalized = fold(word)
			val weight = COMMON[normalized] ?: continue
			for (at in weight.indices) scores[at] += weight[at]
			// The ending is evidence of its own, but only for a word long
			// enough for the ending to be the end of something.
			if (normalized.length >= 5) {
				for (at in ENDINGS.indices) {
					for (ending in ENDINGS[at]) {
						if (normalized.endsWith(ending)) {
							scores[at] += 2
							break
						}
					}
				}
			}
		}
		// A folded comparison would call "für" and "fur" the same word, which
		// is what is wanted for the words; the letters are counted on the
		// text as it was written, which is the only place the umlaut lives.
		val written = text.lowercase()
		for (at in LETTERS.indices) {
			for (letter in LETTERS[at]) {
				val seen = written.count { it == letter }
				if (seen > 0) scores[at] += minOf(seen, 3) * 3
			}
		}

		var best = 0
		var second = 0
		var bestAt = -1
		for (at in scores.indices) {
			val score = scores[at]
			if (score > best) {
				second = best
				best = score
				bestAt = at
			} else if (score > second) {
				second = score
			}
		}
		if (bestAt < 0 || best < LEAST) return null
		if (best * 100 < second * MARGIN) return null
		return LANGS[bestAt]
	}

	/** The lower-cased words of [text], split where a letter stops being one:
	 *  punctuation, whitespace and digits all end a word. */
	private fun wordsOf(text: String): List<String> {
		val out = ArrayList<String>(text.length / 5 + 1)
		val word = StringBuilder()
		for (c in text) {
			if (c.isLetter()) {
				word.append(c)
			} else if (word.isNotEmpty()) {
				out.add(word.toString())
				word.setLength(0)
			}
		}
		if (word.isNotEmpty()) out.add(word.toString())
		return out
	}

	/**
	 * [word] with its accents taken off and in lower case, which is how the
	 * table is written: "für" and "fur", "café" and "cafe", "Città" and
	 * "citta". The table is smaller for it and a word typed without its
	 * accents is still recognised.
	 */
	private fun fold(word: String): String {
		val lower = word.lowercase()
		if (lower.all { it.code < 0x80 }) return lower
		return try {
			Normalizer.normalize(lower, Normalizer.Form.NFD)
				.replace(ACCENTS, "")
		} catch (_: Throwable) {
			lower
		}
	}

	private val ACCENTS = Regex("\\p{Mn}+")

	/**
	 * The common words of the five languages, each with the weight it carries.
	 *
	 * A word is worth its own length, up to six: "nicht" and "porque" are
	 * evidence, "a" and "o" barely are, and a word two languages share
	 * ("in", "no", "via") is written into both and settles nothing on its own.
	 *
	 * These are function words rather than a frequency list on purpose. The
	 * words at the top of a frequency list are the ones every language has a
	 * version of; the ones below are the ones that belong to one grammar.
	 */
	private val COMMON: Map<String, IntArray> = buildMap {
		fun add(at: Int, list: String) {
			for (word in list.split(' ')) {
				if (word.length < 2) continue
				val weight = getOrPut(word) { IntArray(LANGS.size) }
				weight[at] = maxOf(weight[at], word.length.coerceIn(2, 6))
			}
		}
		add(0, ENGLISH)
		add(1, GERMAN)
		add(2, SPANISH)
		add(3, FRENCH)
		add(4, ITALIAN)
	}

	/**
	 * The letters one of these languages writes and the others do not, or
	 * writes far more often.
	 */
	private val LETTERS: Array<String> = arrayOf(
		"",
		"ßäöü",
		"ñ¿¡",
		"œæç",
		"èàìòù"
	)

	/** The endings words of each language take, shortest useful form. */
	private val ENDINGS: Array<List<String>> = arrayOf(
		listOf("ing", "tion", "ness", "ment", "ould", "ight"),
		listOf("ung", "heit", "keit", "schaft", "lich", "chen", "isch", "eit"),
		listOf("ción", "cion", "ados", "adas", "iendo", "aron", "ísimo"),
		listOf("eux", "aux", "ait", "aient", "ique", "ette", "ement"),
		listOf("zione", "mente", "aggio", "gli", "ità", "iamo", "ato", "ito")
	)

	// ---- the word lists ----------------------------------------------------

	private const val ENGLISH =
		"the and that have with this from they will would there their what about " +
			"which when were been has had was are you your our his her its him she " +
			"they them these those than then them some such only also into over " +
			"under between through because while should could might must does did " +
			"done being been other another every each both few many most more less " +
			"very just still again before after during without within against " +
			"because people time world house water day night morning today " +
			"tomorrow yesterday hello please thanks thank sorry yes good great " +
			"work works working write read speak hear say said tell told know knew " +
			"think want need like make made take took give gave come came go went " +
			"see seen look looked find found let put keep kept help"

	private const val GERMAN =
		"der die das und ist sind nicht ich du er sie es wir ihr mit für auf von " +
			"zu den dem des ein eine einen einem einer eines im am an als auch aber " +
			"wenn dann noch nur schon sehr wie was wer wo warum weil kann können " +
			"muss müssen haben hat hatte habe wird werden wurde worden bei nach aus " +
			"um über unter vor durch gegen ohne seine seinen seinem ihren ihrem " +
			"unsere unser mein meine deine dein sich man mehr jetzt hier dort " +
			"heute morgen gestern danke bitte nein oder dass dieser diese dieses " +
			"kein keine zum zur vom beim jetzt immer wieder gut neue deutsch " +
			"deutschland sprache stadt straße welt jahr zeit mensch menschen " +
			"kinder haus wasser tag nacht arbeit"

	private const val SPANISH =
		"el la los las un una unos unas de del y pero que en con por para es son " +
			"está están estaba era fue ser estar hay muy más menos también como " +
			"cuando donde quien porque si no me mi tu su mis tus sus nosotros " +
			"ellos ellas usted ustedes yo tú él ella esto este esta ese esa esos " +
			"esas al le les lo se te nos hacer hace tiene tengo había han sido " +
			"mismo ahora aquí hoy bien mal grande pequeño nuevo bueno gracias hola " +
			"adiós sí año años día días tiempo gente mundo ciudad español españa " +
			"méxico mañana ayer noche agua casa vida trabajo para mucho poco todos " +
			"nada siempre nunca también"

	private const val FRENCH =
		"le la les un une des du de et ou mais que qui quoi dont où quand comme " +
			"si ne pas plus moins très aussi être avoir est sont était été ai as " +
			"avons avez ont fais fait dire dit ce cet cette ces mon ma mes ton ta " +
			"tes son sa ses notre nos votre vos leur leurs je tu il elle on nous " +
			"vous ils elles au aux avec pour par sur sous dans entre sans chez " +
			"tout tous toute toutes bien mal maintenant ici aujourd demain hier " +
			"année jour temps gens monde ville français france merci bonjour " +
			"revoir oui non eau maison vie travail beaucoup peu rien toujours " +
			"jamais aussi encore vraiment"

	private const val ITALIAN =
		"il lo la i gli le un uno una di del della dei delle e ma che chi cui " +
			"non più meno molto anche come quando dove perché se essere è sono era " +
			"stato stata ho hai ha abbiamo avete hanno fare fatto dire detto questo " +
			"questa questi queste quello quella mio mia tuo tua suo sua nostro " +
			"vostro loro io lui lei noi voi essi con per su tra fra senza tutto " +
			"tutti tutta tutte bene male adesso qui oggi domani ieri anno giorno " +
			"tempo gente mondo città italiano italia grazie ciao sì acqua casa " +
			"vita lavoro molto poco niente sempre mai anche ancora davvero"


	/** Words that are Nepali and not Hindi, or that are so much more often
	 *  Nepali that their presence is the answer. */
	private val NEPALI_MARKERS: Set<String> = setOf(
		"छ", "छन्", "छु", "छौ", "छौं", "हो", "हुन्छ", "हुन्", "हुन्थ्यो",
		"गर्नु", "गर्न", "गर्छ", "गर्छन्", "भयो", "भएको", "थियो", "थिए",
		"लाई", "बाट", "पनि", "हरू", "हरु", "सक्छ", "दिनु", "भने", "जस्तो",
		"धेरै", "राम्रो", "नराम्रो", "ठूलो", "सानो", "हामी", "तपाईं", "तपाई",
		"उहाँ", "उनी", "काठमाडौं", "नेपाल", "नेपाली", "अनि", "किन", "के",
		"केही", "सबै", "अहिले", "पछि", "बजार", "गाउँ", "माया"
	)

	/** Words that are Hindi and not Nepali, or that are so much more often
	 *  Hindi that their presence is the answer. */
	private val HINDI_MARKERS: Set<String> = setOf(
		"है", "हैं", "हूँ", "हूं", "था", "थी", "थे", "नहीं", "नही", "का",
		"की", "को", "के", "में", "से", "और", "होता", "होती", "होते", "करना",
		"करता", "करती", "करते", "रहा", "रही", "रहे", "मैं", "मैंने", "आप",
		"यह", "वह", "ये", "वे", "क्या", "बहुत", "अच्छा", "अच्छी", "लोग",
		"बात", "भारत", "दिल्ली", "मुंबई", "हिन्दी", "हिंदी", "आज", "कल",
		"अभी", "क्यों", "कैसे", "कहाँ", "सकता", "सकती", "चाहिए", "रुपये"
	)
}
