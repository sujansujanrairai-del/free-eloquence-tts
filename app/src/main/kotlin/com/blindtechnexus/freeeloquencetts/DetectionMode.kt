package com.blindtechnexus.freeeloquencetts

/**
 * How finely the language of a text is decided.
 *
 * A page can be in one language, a paragraph in two and a line in three:
 * "iPhone का नाम" is one sentence, two scripts and two languages, and a voice
 * that reads all of it in English says the Hindi as though it were a spelling
 * mistake. The four modes are four answers to how much of that a reader wants,
 * and they cost different amounts of work:
 *
 * [CHARACTER] groups characters by script and decides each run. It is the
 * cheapest and the most literal: a run of Devanagari beside a run of Latin is
 * two languages even when it is one word.
 *
 * [WORD] decides each word. This is what a mixed sentence needs -- a Hindi noun
 * in an English sentence is a Hindi noun -- and it is the most detections per
 * character, which is why the detector is run on a pool rather than one at a
 * time.
 *
 * [SENTENCE] decides each sentence. That is what a page of quoted messages
 * needs: a whole line in another language, and no reason to ask about every
 * word of it.
 *
 * [MIXED] decides sentences first and only takes a sentence apart into words
 * when it is not certain enough to leave whole. It is the default because it is
 * the one that is right most of the time and cheap the rest of the time.
 */
object DetectionMode {

	const val CHARACTER = 0
	const val WORD = 1
	const val SENTENCE = 2
	const val MIXED = 3

	/** The four, in the order the screen offers them. */
	val MODES = listOf(CHARACTER, WORD, SENTENCE, MIXED)

	/** What is chosen until somebody chooses something else. */
	const val DEFAULT = MIXED

	/** [mode] when it is one of the four, and [DEFAULT] otherwise, so that a
	 *  preference written by another build is a mode rather than a crash. */
	fun clamp(mode: Int): Int = if (mode in CHARACTER..MIXED) mode else DEFAULT
}
