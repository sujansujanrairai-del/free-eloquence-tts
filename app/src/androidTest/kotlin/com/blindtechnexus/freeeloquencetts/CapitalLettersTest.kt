package com.blindtechnexus.freeeloquencetts

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CapitalLettersTest {

	@Test
	fun noneLeavesTheEngineAlone() {
		assertEquals("Press A", CapitalLetters.apply("Press A", CapitalLetters.NONE))
	}

	@Test
	fun capSaysTheWordCap() {
		assertEquals("cap A", CapitalLetters.apply("A", CapitalLetters.CAP).trim())
		assertEquals("cap I", CapitalLetters.apply("I", CapitalLetters.CAP).trim())
	}

	@Test
	fun pitchUsesTheEnginesOwnAnnotation() {
		assertEquals(
			"`vbst+6 A `vbst-6",
			CapitalLetters.apply("A", CapitalLetters.PITCH).trim()
		)
	}

	@Test
	fun aWordIsLeftAlone() {
		assertEquals("Hello", CapitalLetters.apply("Hello", CapitalLetters.CAP))
		assertEquals("HELLO", CapitalLetters.apply("HELLO", CapitalLetters.CAP))
	}

	@Test
	fun aLineOfCapitalsIsLeftAlone() {
		assertEquals("IBM", CapitalLetters.apply("IBM", CapitalLetters.CAP))
		assertEquals("IBM", CapitalLetters.apply("IBM", CapitalLetters.PITCH))
	}

	@Test
	fun aContractionIsLeftAlone() {
		assertEquals("I'm fine", CapitalLetters.apply("I'm fine", CapitalLetters.CAP))
	}

	@Test
	fun aCapitalBeforeAStopIsCaught() {
		assertEquals("cap A .", CapitalLetters.apply("A.", CapitalLetters.CAP).trim())
	}
}
