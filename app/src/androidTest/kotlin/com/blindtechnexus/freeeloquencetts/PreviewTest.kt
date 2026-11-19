package com.blindtechnexus.freeeloquencetts

import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewTest {

	@Test
	fun theSampleReachesTheAudioTrack() {
		val preview = EvvPreview()
		try {
			preview.say("Testing one two three.", 0, emptyMap(), 11025)
			val until = System.currentTimeMillis() + WAIT_MS
			while (preview.bytesPlayed == 0L && System.currentTimeMillis() < until) {
				Thread.sleep(50)
			}
			assertTrue("the preview played nothing", preview.bytesPlayed > 0)
		} finally {
			preview.close()
		}
	}

	@Test
	fun theEndOfTheSampleIsHeard() {
		val preview = EvvPreview()
		try {
			preview.say(
				"This is a test of the current voice, and the end of it matters.",
				0, emptyMap(), 11025
			)
			val until = System.currentTimeMillis() + WAIT_MS
			while (!preview.playing && System.currentTimeMillis() < until) Thread.sleep(20)
			while (preview.playing && System.currentTimeMillis() < until) Thread.sleep(20)
			assertTrue("the sample never finished", !preview.playing)
			assertTrue("the tail was cut off", preview.drained)
		} finally {
			preview.close()
		}
	}

	private companion object {
		const val WAIT_MS = 15000
	}
}
