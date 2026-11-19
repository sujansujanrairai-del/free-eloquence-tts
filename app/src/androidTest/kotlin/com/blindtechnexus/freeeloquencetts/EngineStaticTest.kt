package com.blindtechnexus.freeeloquencetts

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EngineStaticTest {

	@Test
	fun blankSpeechIsBlank() {
		assertTrue(EvvEngine.isBlankSpeech(""))
		assertTrue(EvvEngine.isBlankSpeech("   "))
		assertTrue(EvvEngine.isBlankSpeech("`p200"))
		assertTrue(EvvEngine.isBlankSpeech("  `vbst+6  `p100 "))
	}

	@Test
	fun wordsAreNotBlank() {
		assertFalse(EvvEngine.isBlankSpeech("hello"))
		assertFalse(EvvEngine.isBlankSpeech("`p200 hello"))
		assertFalse(EvvEngine.isBlankSpeech("  a  "))
		assertFalse(EvvEngine.isBlankSpeech("`[das]"))
		assertFalse(EvvEngine.isBlankSpeech("`pp1 `[das] `p100"))
		assertTrue(EvvEngine.isBlankSpeech("`[]"))
	}
}
