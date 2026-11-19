package com.blindtechnexus.freeeloquencetts

object Prosody {

	fun prefix(phrasePrediction: Boolean): String = if (phrasePrediction) ON else OFF

	private const val ON = "`pp1 "
	private const val OFF = "`pp0 "
}
