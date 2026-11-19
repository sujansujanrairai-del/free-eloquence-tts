package com.blindtechnexus.freeeloquencetts

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SymbolsTest {

	@Test
	fun someLeavesTheEngineAlone() {
		assertEquals("Hello, world. 50% (yes)", Symbols.apply("Hello, world. 50% (yes)", Symbols.SOME, ""))
	}

	@Test
	fun someSilencesACommaThatStandsOnItsOwn() {

		assertEquals("This is everything", Symbols.apply("This is , everything", Symbols.SOME, ""))
		assertEquals("one two", Symbols.apply("one , two", Symbols.SOME, ""))
		assertEquals("a b", Symbols.apply("a ,, b", Symbols.SOME, ""))

		assertEquals("Hello, world.", Symbols.apply("Hello, world.", Symbols.SOME, ""))

		assertEquals("1,000 units", Symbols.apply("1,000 units", Symbols.SOME, ""))
	}

	@Test
	fun allNamesThePunctuationTheEngineLeavesSilent() {
		assertEquals("Hello comma world dot", Symbols.apply("Hello, world.", Symbols.ALL, ""))
		assertEquals("Hello dot World question mark", Symbols.apply("Hello. World?", Symbols.ALL, ""))
	}

	@Test
	fun mostNamesBracketsButNotSentencePunctuation() {
		assertEquals(
			"Hello left parenthesis world right parenthesis .",
			Symbols.apply("Hello (world).", Symbols.MOST, "")
		)
		assertEquals("Hello, world.", Symbols.apply("Hello, world.", Symbols.MOST, ""))
	}

	@Test
	fun theEnginesOwnSymbolsAreNeverReplaced() {

		assertEquals("50% of $5", Symbols.apply("50% of $5", Symbols.ALL, ""))
		assertEquals("50% of $5", Symbols.apply("50% of $5", Symbols.MOST, ""))

		assertEquals("a\\b", Symbols.apply("a\\b", Symbols.ALL, ""))
	}

	@Test
	fun noneSilencesTheBackslashTheEngineNames() {

		assertEquals("a b", Symbols.apply("a\\b", Symbols.NONE, ""))
		assertEquals("C: Users", Symbols.apply("C:\\Users", Symbols.NONE, ""))

		assertEquals("C:\\Users", Symbols.apply("C:\\Users", Symbols.CUSTOM, "\\"))
	}

	@Test
	fun noneSilencesEverythingButThePauses() {
		assertEquals("50 of 5", Symbols.apply("50% of $5", Symbols.NONE, ""))
		assertEquals("a b and c d", Symbols.apply("a=b and c+d", Symbols.NONE, ""))
		assertEquals("user example com", Symbols.apply("user@example.com", Symbols.NONE, ""))

		assertEquals("Hello. World?", Symbols.apply("Hello. World?", Symbols.NONE, ""))

		assertEquals("Is this OK", Symbols.apply("Is this OK ?", Symbols.NONE, ""))
	}

	@Test
	fun numbersAndContractionsBelongToTheEngine() {
		val numbers = "It's 3.14 at 2:30, 1,000 units"
		assertEquals(numbers, Symbols.apply(numbers, Symbols.NONE, ""))

		assertEquals(
			"It's 3.14 at 2:30 comma 1,000 units",
			Symbols.apply(numbers, Symbols.ALL, "")
		)
		assertEquals("don't stop", Symbols.apply("don't stop", Symbols.ALL, ""))
	}

	@Test
	fun customNamesOnlyWhatIsOnTheList() {

		assertEquals("50 of $5", Symbols.apply("50% of $5", Symbols.CUSTOM, "$"))

		assertEquals(
			"a# left parenthesis b dot c!",
			Symbols.apply("a# (b). c!", Symbols.CUSTOM, "#(.")
		)

		assertEquals("a b", Symbols.apply("a (b)", Symbols.CUSTOM, " "))
	}

	@Test
	fun typographicSymbolsBehaveLikePlainOnes() {
		assertEquals("quote hello quote", Symbols.apply("“hello”", Symbols.MOST, ""))
		assertEquals("Wait...", Symbols.apply("Wait…", Symbols.NONE, ""))
	}

	@Test
	fun aSymbolStandingAloneIsTheEnginesOwnToName() {
		assertEquals("* # ?", Symbols.apply("* # ?", Symbols.ALL, ""))
		assertEquals("* # ?", Symbols.apply("* # ?", Symbols.MOST, ""))
	}

	@Test
	fun theBacktickIsNeverTouched() {

		assertEquals("`p1 hello", Symbols.apply("`p1 hello", Symbols.NONE, ""))
		assertEquals("`p1 hello", Symbols.apply("`p1 hello", Symbols.ALL, ""))
		assertEquals("`vbst+6 A `vbst-6", Symbols.apply("`vbst+6 A `vbst-6", Symbols.NONE, ""))
		assertEquals("`[das]", Symbols.apply("`[das]", Symbols.NONE, ""))
		assertEquals("`[das]", Symbols.apply("`[das]", Symbols.ALL, ""))
		assertEquals("one\ntwo", Symbols.apply("one\n , two", Symbols.SOME, ""))
	}
}
