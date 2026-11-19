package com.blindtechnexus.freeeloquencetts

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SsmlTest {

	@Test
	fun aTagInAMiddleOfTextIsTakenOut() {
		assertEquals(
			"Hello world",
			Ssml.strip("Hello <break time=\"1s\"/> world")
		)
	}

	@Test
	fun surroundingTagsGoAndTheWordsStay() {
		assertEquals("Hello world", Ssml.strip("<speak>Hello world</speak>"))
	}

	@Test
	fun tagsRunTogetherAreTakenOut() {
		assertEquals("a c d", Ssml.strip("a<b>c</b>d"))
	}

	@Test
	fun textWithoutTagsIsUntouched() {
		assertEquals("no tags here", Ssml.strip("no tags here"))
	}

	@Test
	fun aComparisonInProseIsNotATag() {

		assertEquals("3 < 5 > 4", Ssml.strip("3 < 5 > 4"))
	}

	@Test
	fun aStrayAngleWithNoCloseIsLeftForTheSymbols() {
		assertEquals("3 < 5", Ssml.strip("3 < 5"))
	}

	@Test
	fun anEntityIsSaidAsWhatItMeans() {

		assertEquals("fish and chips", Ssml.strip("fish &amp; chips"))
		assertEquals("caf\u00e9", Ssml.strip("caf&#233;"))
		assertEquals("caf\u00e9", Ssml.strip("caf&#xE9;"))

		assertEquals("a &weird; b", Ssml.strip("a &weird; b"))
	}
}
