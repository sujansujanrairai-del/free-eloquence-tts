package com.blindtechnexus.freeeloquencetts

object DetectionMode {

	const val CHARACTER = 0
	const val WORD = 1
	const val SENTENCE = 2
	const val MIXED = 3

	val MODES = listOf(CHARACTER, WORD, SENTENCE, MIXED)

	const val DEFAULT = MIXED

	fun clamp(mode: Int): Int = if (mode in CHARACTER..MIXED) mode else DEFAULT
}
