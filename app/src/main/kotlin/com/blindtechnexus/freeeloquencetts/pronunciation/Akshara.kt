package com.blindtechnexus.freeeloquencetts.pronunciation

/**
 * One syllable of Devanagari: an onset, a vowel, and -- sometimes -- a nasal
 * breath or a visarga after it.
 *
 * Devanagari writes syllables, not letters, and the difference is the whole of
 * why it cannot be transliterated one character at a time. कमल is three
 * syllables -- ku, mu, la -- and the "a" of each is not written at all: it is
 * what a consonant carries until a matra takes it away. नगरने is nu-gu-ru-ne,
 * and the last of those is written with a matra and the middle two are not.
 *
 * The onset is a string of respellings rather than a list of letters because a
 * conjunct is one sound and not two: क्ष is "ksh", not "k" then "sh". See
 * [Conjuncts] for the handful that are not simply their letters run together.
 */
internal class Akshara {

	/** The consonant cluster the syllable starts with, already respelled. */
	val onset = StringBuilder()

	/** The vowel, or null when a virama has taken it away. */
	var vowel: Devanagari.Vowel? = Devanagari.SCHWA

	/** [Devanagari.ANUSVARA] or [Devanagari.CHANDRABINDU] when one is written,
	 *  and nought otherwise. */
	var nasal: Char = '\u0000'

	/** Whether a visarga follows the vowel. */
	var visarga: Boolean = false

	/** Whether this syllable came from a vowel standing on its own, which is
	 *  the case where a word begins with अ, आ, इ and the rest. */
	var independent: Boolean = false

	/** Whether the syllable's consonant carries a vowel that was written. */
	var writtenVowel: Boolean = false

	/** Whether anything at all has been put in this syllable yet. */
	val isEmpty: Boolean get() = onset.isEmpty() && !independent && nasal == '\u0000' && !visarga

	/** Whether the vowel is the inherent schwa, which is the one the schwa
	 *  rules may take away. */
	val hasSchwa: Boolean get() = vowel?.schwa == true

	/** The onset as the engine is given it. */
	fun onsetText(): String = onset.toString()

	override fun toString(): String =
		"${onset}${vowel?.roman ?: "-"}${if (nasal == '\u0000') "" else "~"}"
}
