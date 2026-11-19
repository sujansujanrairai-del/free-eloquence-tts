package com.blindtechnexus.freeeloquencetts

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SliderStepTest {

	@Test
	fun everyPercentOfEverySliderIsItsOwnNumber() {
		for (param in Settings.SLIDERS) {
			for (percent in 0..Eci.PERCENT_MAX) {
				assertEquals(
					"param $param lost $percent percent on the way through the engine's scale",
					percent,
					Eci.toPercent(param, Eci.fromPercent(param, percent))
				)
			}
		}
	}

	@Test
	fun aStepOfOneIsAudibleAtTheSlowEnd() {

		val slowest = SpeechRate.timesFor(Eci.fromPercent(Eci.VOICE_SPEED, 0))
		val five = SpeechRate.timesFor(Eci.fromPercent(Eci.VOICE_SPEED, 5))
		val one = SpeechRate.timesFor(Eci.fromPercent(Eci.VOICE_SPEED, 1))
		assertEquals(0.400, slowest, 0.001)
		assertEquals(0.481, five, 0.005)
		assertEquals(true, one > slowest && one < five)
	}
}
