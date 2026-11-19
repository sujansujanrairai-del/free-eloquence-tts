package com.blindtechnexus.freeeloquencetts

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InterruptEnduranceTest {

	@Test
	fun threeHundredInterruptionsLeaveTheEngineWorking() {
		val e = EvvEngine.open(ENGLISH)
		assertNotNull(e)
		try {
			e!!.setSampleRate(11025)
			e.applyVoice(0)
			val whole = wholeUtterance(e)
			assertTrue("nothing was said to begin with", whole > 0)
			val buffer = ByteArray(8192)
			for (round in 1..ROUNDS) {
				assertTrue("round $round was refused", e.speak(WORDS[round % WORDS.size]))
				e.read(buffer)
				e.stop()
			}

			val after = wholeUtterance(e)
			assertTrue(
				"after $ROUNDS interruptions a whole utterance was worth $after, not $whole",
				after == whole
			)
		} finally {
			e?.close()
		}
	}

	private fun wholeUtterance(e: EvvEngine): Int {
		if (!e.speak("The quick brown fox.")) return -1
		var total = 0
		val buffer = ByteArray(4096)
		while (true) {
			val n = e.read(buffer)
			if (n <= 0) break
			total += n
		}
		return total
	}

	private companion object {
		const val ENGLISH = 0x00010000
		const val ROUNDS = 300
		val WORDS = listOf("Paperback", "YouTube", "Messages", "GitHub", "Settings", "Calendar")
	}
}
