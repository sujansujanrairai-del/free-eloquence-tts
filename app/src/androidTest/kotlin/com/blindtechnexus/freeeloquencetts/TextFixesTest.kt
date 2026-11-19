package com.blindtechnexus.freeeloquencetts

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextFixesTest {

	@Test
	fun aDigitAfterAWordGetsASpace() {
		assertEquals("teamtalk 5", TextFixes.apply("teamtalk5"))
		assertEquals("covid 19", TextFixes.apply("covid19"))
		assertEquals("win 10", TextFixes.apply("win10"))
	}

	@Test
	fun aLetterAfterADigitIsLeftAlone() {
		assertEquals("1st", TextFixes.apply("1st"))
		assertEquals("2nd", TextFixes.apply("2nd"))
		assertEquals("5G", TextFixes.apply("5G"))
	}

	@Test
	fun anOpeningMarkAfterAWordGetsASpace() {
		assertEquals("hello (world)", TextFixes.apply("hello(world)"))
		assertEquals("abc #", TextFixes.apply("abc#"))
		assertEquals("abc {", TextFixes.apply("abc{"))
	}

	@Test
	fun aClosingMarkIsLeftAlone() {
		assertEquals("abc)", TextFixes.apply("abc)"))
		assertEquals("abc]", TextFixes.apply("abc]"))
	}

	@Test
	fun theSSuffixIsPulledBackOn() {
		assertEquals("books(s)", TextFixes.apply("books (s)"))
	}

	@Test
	fun theSSuffixSurvivesBeingRightAlready() {
		assertEquals("books(s)", TextFixes.apply("books(s)"))
	}

	@Test
	fun aSpaceBeforeAMarkGoes() {
		assertEquals("word.", TextFixes.apply("word ."))
		assertEquals("word,", TextFixes.apply("word ,"))
	}

	@Test
	fun commasStayInRoundNumbers() {
		assertEquals("1,000,000", TextFixes.apply("1,000,000"))
		assertEquals("1,000,000 items", TextFixes.apply("1,000,000 items"))
		assertEquals("2,000,000,000", TextFixes.apply("2,000,000,000"))
	}

	@Test
	fun otherNumbersKeepTheirCommas() {
		assertEquals("12,345", TextFixes.apply("12,345"))
		assertEquals("September 5, 2026", TextFixes.apply("September 5, 2026"))
	}

	@Test
	fun theCharactersAWebPageCarriesAreTakenOut() {
		assertEquals("hello world", TextFixes.apply("hello\u00A0world"))
		assertEquals("hello world", TextFixes.apply("hello\u2009world"))
		assertEquals("hello world", TextFixes.apply("hello\u3000world"))

		assertEquals("teamtalk", TextFixes.apply("team\u200Btalk"))

		assertEquals("eloquence", TextFixes.apply("elo\u00ADquence"))
		assertEquals("hello", TextFixes.apply("\uFEFFhello"))

		assertEquals("see here", TextFixes.apply("see \uFFFC here"))
	}

	@Test
	fun theWebCharactersDoNotBreakTheOrdinaryFixes() {
		assertEquals("teamtalk 5", TextFixes.apply("teamtalk\u00A05"))
		assertEquals("word.", TextFixes.apply("word \u00A0."))
	}

	@Test
	fun ordinaryTextIsUntouched() {
		for (text in listOf("3.14", "1.5", "e.g.", "Navigate up", "")) {
			assertEquals(text, TextFixes.apply(text))
		}
	}

	@Test
	fun fixesRunBeforePauses() {
		val right = Pauses.apply(TextFixes.apply("Navigate up"), Pauses.ALL, true)
		assertTrue("the annotation did not survive: $right", right.endsWith("`p100"))
		assertEquals("Navigate up `p 100", TextFixes.apply(Pauses.apply("Navigate up", Pauses.ALL, true)))
	}

	@Test
	fun theSpellingActuallyStops() {
		val e = EvvEngine.open(ENGLISH)
		assertNotNull(e)
		try {
			e!!.setSampleRate(11025)
			e.applyVoice(0)
			for (text in listOf("teamtalk5", "windows11", "hello(world)")) {
				val before = say(e, text)
				val after = say(e, TextFixes.apply(text))
				android.util.Log.e(TAG, "$text: $before then $after")
				assertTrue("no shorter: $text, $before then $after", after < before * 0.9)
			}
		} finally {
			e?.close()
		}
	}

	@Test
	fun hexSoundsTheSameEitherWay() {
		assertEquals("0x 1F", TextFixes.apply("0x1F"))
	}

	private fun say(e: EvvEngine, text: String): Int {
		if (!e.speak(text)) return -1
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
		const val TAG = "evvfixes"
		const val ENGLISH = 0x00010000
	}
}
