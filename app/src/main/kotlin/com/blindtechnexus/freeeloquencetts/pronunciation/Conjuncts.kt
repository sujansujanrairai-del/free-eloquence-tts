package com.blindtechnexus.freeeloquencetts.pronunciation

/**
 * Consonant clusters, and the few that are not their letters run together.
 *
 * Devanagari writes a cluster by stacking the consonants, and the stack is one
 * sound in the mouth but three letters on the page. Most clusters need nothing
 * here at all: क् + ष is "k" + "sh" and the two respellings run together into
 * "ksh", which is what the word sounds like. The exceptions are the clusters
 * every Hindi speaker learns as units -- ज्ञ is "gy", not "jny" -- and the
 * respellings an English voice would otherwise read as separate syllables.
 *
 * The last thing this does is tidy: three consonants in a row are a spelling
 * no English voice reads as one sound, and a doubled vowel is a spelling that
 * makes it smuggle in a glide. Both are repairs to the respelling, not to the
 * word.
 */
internal object Conjuncts {

	/**
	 * Clusters that are said as something other than the letters they are
	 * written with. Keyed on the respelling the syllabifier has already built,
	 * which is the same key for every spelling of the stack -- ज्ञ and ज्+ञ
	 * both arrive as "jny".
	 */
	private val SPECIAL: Map<String, String> = mapOf(
		"jny" to "gy",     // ज्ञ  gyan
		"jn" to "gn",      // ज्ञ without the nasal
		"ksh" to "ksh",    // क्ष  the one everybody romanises the same way
		"shy" to "sh",     // श्य  as in श्याम
		"shn" to "shn",    // श्न
		"tr" to "tr",      // त्र
		"shr" to "shr",    // श्र  shree
		"dr" to "dr",      // द्र
		"dv" to "dv",      // द्व  dvandra
		"st" to "st",      // स्त  namaste
		"sth" to "sth",    // स्थ
		"sn" to "sn",      // स्न
		"sm" to "sm",      // स्म
		"sl" to "sl",      // स्ल
		"ny" to "ny",      // ञ/न्य
		"nj" to "nj",      // न्ज
		"nk" to "nk",      // ङ्क
		"ng" to "ng",      // ङ्ग
		"mb" to "mb",      // म्ब
		"mp" to "mp",      // म्प
		"nd" to "nd",      // न्द
		"nt" to "nt",      // न्त
		"nth" to "nth",    // न्थ
		"nn" to "nn",      // न्न
		"mm" to "mm",      // म्म
		"ll" to "ll",      // ल्ल
		"tt" to "tt",      // त्त
		"dd" to "dd",      // द्द
		"pp" to "pp",      // प्प
		"bb" to "bb",      // ब्ब
		"kk" to "kk",      // क्क
		"gg" to "gg",      // ग्ग
		"chch" to "chh",   // च्च  said as the aspirate
		"cch" to "chh",    // च्छ
		"tch" to "ch",     // च with a dental before it
		"kt" to "kt",      // क्त  bhakt
		"kht" to "kht",    // ख्त
		"gd" to "gd",      // ग्द
		"gn" to "gn",      // ग्न
		"tm" to "tm",      // त्म  aatma
		"tn" to "tn",      // त्न
		"pt" to "pt",      // प्त
		"bd" to "bd",      // ब्द
		"bdh" to "bdh",    // ब्ध
		"rk" to "rk",      // र्क  kark
		"rg" to "rg",      // र्ग
		"rd" to "rd",      // र्द
		"rn" to "rn",      // र्न
		"rm" to "rm",      // र्म  dharma
		"rl" to "rl",      // र्ल
		"rv" to "rv",      // र्व  parvat
		"rs" to "rs",      // र्स
		"rh" to "rh",      // र्ह
		"lk" to "lk",      // ल्क
		"lg" to "lg",      // ल्ग
		"lp" to "lp",      // ल्प
		"lm" to "lm",      // ल्म
		"lv" to "lv",      // ल्व
		"vy" to "vy",      // व्य  vyakti
		"vr" to "vr",      // व्र
		"hy" to "hy",      // ह्य
		"hr" to "hr",      // ह्र
		"hm" to "hm",      // ह्म
		"hl" to "hl"       // ह्ल
	)

	/** The cluster as the engine should be given it. */
	fun onset(text: String): String {
		if (text.isEmpty()) return text
		SPECIAL[text]?.let { return it }
		return text
	}

	/**
	 * A last pass over a whole respelled syllable, for the shapes an English
	 * voice reads as something else entirely: a doubled vowel it turns into a
	 * glide, three consonants it splits with a vowel it invented.
	 */
	fun tidy(text: String): String {
		if (text.isEmpty()) return text
		var out = text
		// "aaa" and "eee" are not sounds an English voice has; the length is
		// already carried by the doubling.
		while (out.contains("aaa")) out = out.replace("aaa", "aa")
		while (out.contains("eee")) out = out.replace("eee", "ee")
		while (out.contains("ooo")) out = out.replace("ooo", "oo")
		while (out.contains("iii")) out = out.replace("iii", "ii")
		while (out.contains("uuu")) out = out.replace("uuu", "uu")
		// A doubled consonant of the same letter is one sound held: क्क is
		// "kk", which is how the engine says a held stop.
		return out
	}
}
