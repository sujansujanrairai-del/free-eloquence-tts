package com.blindtechnexus.freeeloquencetts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpellingTest {

	@Test
	fun aWordIsSpelledLetterByLetter() {
		assertEquals("c a t", Spelling.apply("cat", Spelling.LETTERS))
		assertEquals("H e l l o , w o r l d", Spelling.apply("Hello, world", Spelling.LETTERS))
	}

	@Test
	fun aWordIsSpelledByItsNatoName() {
		assertEquals("Charlie Alpha Tango", Spelling.apply("cat", Spelling.NATO))

		assertEquals("Alpha One Bravo Two", Spelling.apply("A1B2", Spelling.NATO))
	}

	@Test
	fun theCapitalsSettingKnowsAnAcronymFromAWord() {
		assertEquals("The D O J met", Spelling.apply("The DOJ met", Spelling.CAPITALS))
		assertEquals("The Delta Oscar Juliett met", Spelling.apply("The DOJ met", Spelling.CAPITALS_NATO))

		assertEquals("OK is fine", Spelling.apply("OK is fine", Spelling.CAPITALS))
		assertEquals("I am", Spelling.apply("I am", Spelling.CAPITALS))

		assertEquals("NASA and nasa", Spelling.apply("NASA and nasa", Spelling.CAPITALS))
	}

	@Test
	fun offLeavesTheTextAlone() {
		val text = "NASA met the DOJ at 3.15"
		assertEquals(text, Spelling.apply(text, Spelling.OFF))
	}

	@Test
	fun engineAnnotationsAreLeftForTheEngine() {
		assertEquals(
			" `vbst+6 Alpha `vbst-6 ",
			Spelling.apply(" `vbst+6 A `vbst-6 ", Spelling.NATO)
		)
		assertEquals(
			"`[das] c a t",
			Spelling.apply("`[das] cat", Spelling.LETTERS)
		)
	}

	@Test
	fun everyModeIsInModeS() {
		assertEquals(5, Spelling.MODES.size)
		assertTrue(Spelling.MODES.contains(Spelling.OFF))
		assertTrue(Spelling.MODES.contains(Spelling.CAPITALS_NATO))

		assertEquals(Spelling.MODES.distinct().size, Spelling.MODES.size)
	}
}
