package com.blindtechnexus.freeeloquencetts.pronunciation

/**
 * The Devanagari script, as data.
 *
 * The engine this app is built on speaks the eight Western languages IBM
 * finished and has no module for Hindi or Nepali. It reads single bytes of the
 * Windows Western code set, so a Devanagari letter handed to it is not a letter
 * at all: it is a byte the code set never assigned, and what came out was the
 * nearest English spelling of a word nobody had spelled -- which is exactly the
 * "naagarna" for "nagarne" this package exists to end.
 *
 * So the script is written down here, letter by letter, and turned into a
 * respelling the English voice pronounces the way the Devanagari sounds. The
 * respelling is English orthography rather than IPA, because orthography is
 * what the engine actually reads; the IPA is kept beside it so that every table
 * can be checked against a phonetics reference instead of against an ear.
 *
 * Three decisions are worth stating, because they are approximations and not
 * accidents:
 *
 * The inherent vowel is written "u". English "a" is the vowel of "cat"; the
 * Hindi schwa is the vowel of "cut". Writing नगर as "nagar" gets "nag-ar";
 * writing it as "nugur" gets the sound Hindi actually makes.
 *
 * Retroflex and dental stops share a spelling. An English voice has no
 * retroflex stop to be asked for, and a capital or a doubled letter buys a
 * spelling the engine reads letter by letter. So ट and त are both "t", and the
 * difference is lost in the same place every English speaker loses it.
 *
 * Aspiration is a written "h". ख is "kh", घ is "gh", थ is "th". English reads
 * "th" as the consonant of "thin", which is not the Hindi sound, but it is the
 * closest thing an English voice has, and it is what every romanisation of
 * Hindi has used for two hundred years.
 */
internal object Devanagari {

	/** The sign that takes the inherent vowel away and joins the next consonant. */
	const val VIRAMA = '\u094D'

	/** The nasal written above the line and pronounced with the vowel. */
	const val ANUSVARA = '\u0902'

	/** The nasal written with the moon-dot, which nasalises the vowel itself. */
	const val CHANDRABINDU = '\u0901'

	/** The light breath after a vowel. */
	const val VISARGA = '\u0903'

	/** The dot that turns a letter into another letter: क into क़. */
	const val NUKTA = '\u093C'

	/** The mark that stands for a vowel that sandhi has swallowed. */
	const val AVAGRAHA = '\u093D'

	/** The sacred syllable, written as one sign. */
	const val OM = '\u0950'

	/** The full stop of Devanagari, and its doubled form. */
	const val DANDA = '\u0964'
	const val DOUBLE_DANDA = '\u0965'

	/** The abbreviation sign and the high spacing dot. */
	const val ABBREVIATION_SIGN = '\u0970'
	const val HIGH_SPACING_DOT = '\u0971'

	/** The two joiners, which change how a conjunct is drawn and not how it is
	 *  said, and which must not reach the engine as anything at all. */
	const val ZERO_WIDTH_NON_JOINER = '\u200C'
	const val ZERO_WIDTH_JOINER = '\u200D'

	/** The vowel every consonant carries until something takes it away. */
	val SCHWA = Vowel('\u0905', "u", "\u0259", schwa = true)

	/**
	 * One vowel, written on its own or hanging off a consonant.
	 *
	 * [roman] is what the engine is given; [ipa] is what a phonetics reference
	 * calls it. [schwa] marks the inherent vowel, which is the one the schwa
	 * rules are allowed to delete.
	 */
	class Vowel(val letter: Char, val roman: String, val ipa: String, val schwa: Boolean = false)

	/** The vowels as they hang off a consonant: का, कि, की and the rest. */
	val MATRAS: Map<Char, Vowel> = listOf(
		Vowel('\u093E', "aa", "a\u02D0"),      // ा  kaam
		Vowel('\u093F', "i", "\u026A"),        // ि  din
		Vowel('\u0940', "ee", "i\u02D0"),      // ी  teen
		Vowel('\u0941', "u", "\u028A"),        // ु  dul
		Vowel('\u0942', "oo", "u\u02D0"),      // ू  phool
		Vowel('\u0943', "ri", "r\u0329"),      // ृ  krip
		Vowel('\u0944', "ri", "r\u0329"),      // ॄ
		Vowel('\u0945', "e", "e"),             // ॅ
		Vowel('\u0946', "e", "e"),             // ॆ
		Vowel('\u0947', "eh", "e\u02D0"),      // े  ghar meh
		Vowel('\u0948', "ai", "\u025B\u02D0"), // ै  hai
		Vowel('\u0949', "o", "\u0252"),        // ॉ  doktar
		Vowel('\u094A', "oh", "o\u02D0"),      // ो  Dostoh
		Vowel('\u094B', "oh", "o\u02D0"),      // ो
		Vowel('\u094C', "au", "\u0254\u02D0"), // ौ  nau
		Vowel('\u094E', "e", "e"),             // ॎ
		Vowel('\u094F', "au", "\u0254\u02D0")  // ॏ
	).associateBy { it.letter }

	/** The vowels as they stand alone at the front of a word. */
	val INDEPENDENT: Map<Char, Vowel> = listOf(
		Vowel('\u0905', "u", "\u0259", schwa = true), // अ  the schwa itself
		Vowel('\u0906', "aa", "a\u02D0"),             // आ  aam
		Vowel('\u0907', "i", "\u026A"),               // इ  imli
		Vowel('\u0908', "ee", "i\u02D0"),             // ई  eesha
		Vowel('\u0909', "u", "\u028A"),               // उ  upar
		Vowel('\u090A', "oo", "u\u02D0"),             // ऊ  oon
		Vowel('\u090B', "ri", "r\u0329"),             // ऋ  rishi
		Vowel('\u090C', "li", "l\u0329"),             // ऌ
		Vowel('\u090D', "e", "e"),                    // ऍ
		Vowel('\u090E', "e", "e"),                    // ऎ
		Vowel('\u090F', "eh", "e\u02D0"),             // ए  ek
		Vowel('\u0910', "ai", "\u025B\u02D0"),        // ऐ  aisa
		Vowel('\u0911', "o", "\u0252"),               // ऑ
		Vowel('\u0912', "oh", "o\u02D0"),             // ऒ
		Vowel('\u0913', "oh", "o\u02D0"),             // ओ  oh
		Vowel('\u0914', "au", "\u0254\u02D0")         // औ  aurat
	).associateBy { it.letter }

	/**
	 * One consonant: what it is called, how the engine is told to say it, and
	 * the three facts the rules need about it -- whether it is a stop, a nasal,
	 * or a liquid, which is what decides how an anusvara in front of it is
	 * pronounced.
	 */
	class Consonant(
		val letter: Char,
		val roman: String,
		val ipa: String,
		val stop: Boolean = false,
		val nasal: Boolean = false,
		val liquid: Boolean = false,
		val fricative: Boolean = false
	)

	/** क to ह, in the order the script teaches them. */
	val CONSONANTS: Map<Char, Consonant> = listOf(
		// The five stops, each in its five places: unvoiced, aspirated,
		// voiced, voiced aspirated, and the nasal of the place.
		Consonant('\u0915', "k", "k", stop = true),      // क
		Consonant('\u0916', "kh", "k\u02B0", stop = true), // ख
		Consonant('\u0917', "g", "\u0261", stop = true), // ग
		Consonant('\u0918', "gh", "\u0261\u02B1", stop = true), // घ
		Consonant('\u0919', "ng", "\u014B", nasal = true), // ङ

		Consonant('\u091A', "ch", "t\u0361\u0283", stop = true), // च
		Consonant('\u091B', "chh", "t\u0361\u0283\u02B0", stop = true), // छ
		Consonant('\u091C', "j", "d\u0361\u0292", stop = true), // ज
		Consonant('\u091D', "jh", "d\u0361\u0292\u02B1", stop = true), // झ
		Consonant('\u091E', "ny", "\u0272", nasal = true), // ञ

		Consonant('\u091F', "t", "\u0288", stop = true),   // ट
		Consonant('\u0920', "th", "\u0288\u02B0", stop = true), // ठ
		Consonant('\u0921', "d", "\u0256", stop = true),   // ड
		Consonant('\u0922', "dh", "\u0256\u02B1", stop = true), // ढ
		Consonant('\u0923', "n", "\u0273", nasal = true),  // ण

		Consonant('\u0924', "t", "t\u032A", stop = true),  // त
		Consonant('\u0925', "th", "t\u032A\u02B0", stop = true), // थ
		Consonant('\u0926', "d", "d\u032A", stop = true),  // द
		Consonant('\u0927', "dh", "d\u032A\u02B1", stop = true), // ध
		Consonant('\u0928', "n", "n", nasal = true),       // न

		Consonant('\u092A', "p", "p", stop = true),        // प
		Consonant('\u092B', "ph", "p\u02B0", stop = true), // फ
		Consonant('\u092C', "b", "b", stop = true),        // ब
		Consonant('\u092D', "bh", "b\u02B1", stop = true), // भ
		Consonant('\u092E', "m", "m", nasal = true),       // म

		Consonant('\u092F', "y", "j", liquid = true),      // य
		Consonant('\u0930', "r", "r", liquid = true),      // र
		Consonant('\u0932', "l", "l", liquid = true),      // ल
		Consonant('\u0933', "l", "\u026D", liquid = true), // ळ
		Consonant('\u0934', "l", "\u026D", liquid = true), // ऴ
		Consonant('\u0935', "v", "\u028B", liquid = true), // व
		Consonant('\u0936', "sh", "\u0283", fricative = true), // श
		Consonant('\u0937', "sh", "\u0282", fricative = true), // ष
		Consonant('\u0938', "s", "s", fricative = true),   // स
		Consonant('\u0939', "h", "\u0266", fricative = true)   // ह
	).associateBy { it.letter }

	/**
	 * The letters a nukta turns into, which are letters of their own in Hindi
	 * and are only spelled with a dot in the Unicode chart: क़ is not क.
	 */
	val NUKTA_FORMS: Map<Char, String> = mapOf(
		'\u0915' to "q",   // क़  qaum
		'\u0916' to "kh",  // ख़  khabar
		'\u0917' to "g",   // ग़  gareeb
		'\u091C' to "z",   // ज़  zindagi
		'\u0921' to "r",   // ड़  bara
		'\u0922' to "rh",  // ढ़  badhiya
		'\u092B' to "f",   // फ़  feel
		'\u092F' to "y",   // य़
		'\u0928' to "n"    // ऩ
	)

	/** The ten digits, which are read as the digits they are. */
	val DIGITS: Map<Char, Char> = mapOf(
		'\u0966' to '0', '\u0967' to '1', '\u0968' to '2', '\u0969' to '3',
		'\u096A' to '4', '\u096B' to '5', '\u096C' to '6', '\u096D' to '7',
		'\u096E' to '8', '\u096F' to '9'
	)

	/** Marks that carry no sound of their own and are dropped: the Vedic
	 *  accents, the inverted candrabindu, the two Vedic vowel signs, the
	 *  abbreviation sign and the high spacing dot. */
	private val SILENT_MARKS = setOf(
		'\u0900', '\u0951', '\u0952', '\u0953', '\u0954', '\u0955', '\u0956',
		'\u0957', '\u0962', '\u0963', '\u0970', '\u0971'
	)

	fun isConsonant(c: Char): Boolean = CONSONANTS.containsKey(c)

	fun isMatra(c: Char): Boolean = MATRAS.containsKey(c)

	fun isIndependent(c: Char): Boolean = INDEPENDENT.containsKey(c)

	fun isCombining(c: Char): Boolean =
		c == VIRAMA || c == NUKTA || c == ANUSVARA || c == CHANDRABINDU || c == VISARGA ||
			isMatra(c)

	/** Whether [c] is a letter or a mark of the script, which is what a word
	 *  of Devanagari is made of. */
	fun isLetter(c: Char): Boolean =
		isConsonant(c) || isMatra(c) || isIndependent(c) || isCombining(c) || c == AVAGRAHA

	/** Whether [c] is anywhere in the Devanagari block, punctuation included. */
	fun isDevanagari(c: Char): Boolean = c.code in 0x0900..0x097F

	fun isSilentMark(c: Char): Boolean = c in SILENT_MARKS
}
