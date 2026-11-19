package com.blindtechnexus.freeeloquencetts

import org.junit.Assert.assertEquals
import org.junit.Test

class CodeReadingTest {

	@Test
	fun camelCaseNamesBecomeWords() {
		assertEquals("get element by id", say("getElementById", camel = true, snake = true))
		assertEquals("parse http response", say("parseHTTPResponse", camel = true, snake = true))

		assertEquals("HTTP", say("HTTP", camel = true, snake = true))
	}

	@Test
	fun snakeCaseNamesBecomeWords() {
		assertEquals("my total count", say("my_total_count", camel = true, snake = true))
		assertEquals("my_total_count", say("my_total_count", camel = true, snake = false))
	}

	@Test
	fun proseKeepsItsCapitals() {

		assertEquals("Hello world", say("Hello world", camel = true, snake = true))
	}

	@Test
	fun anOperatorIsMetAsTheWholeOfItself() {
		assertEquals("a not equals b", say("a != b", operators = true))
		assertEquals("x triple equals y", say("x === y", operators = true))
		assertEquals("i plus plus", say("i++", operators = true))
		assertEquals("a arrow b", say("a -> b", operators = true))
		assertEquals("std double colon vector", say("std::vector", operators = true))
	}

	@Test
	fun bracketsAndMarksAreNamed() {
		assertEquals(
			"if left paren x right paren left brace",
			say("if (x) {", braces = true)
		)
		assertEquals("a semicolon b", say("a; b", braces = true))
	}

	@Test
	fun aPathIsSaidAsAPath() {
		assertEquals(
			"path home slash user slash notes dot txt",
			say("/home/user/notes.txt", paths = true)
		)
		assertEquals(
			"path src slash main slash App dot kt",
			say("src/main/App.kt", paths = true)
		)

		assertEquals("and/or", say("and/or", paths = true))
	}

	@Test
	fun anAddressIsSaidAsAnAddress() {

		assertEquals(
			"url example dot com slash a slash b",
			say("https://www.example.com/a/b", urls = true)
		)
		assertEquals(
			"url example dot com slash a slash b",
			say("https://www.example.com/a/b", urls = true, operators = true)
		)
		assertEquals(
			"url x dot org slash p question mark q equals 1 and r equals 2",
			say("https://x.org/p?q=1&r=2", urls = true)
		)

		assertEquals("see url example dot com.", say("see https://example.com.", urls = true))

		assertEquals("a and b", say("a and b", urls = true))
	}

	@Test
	fun indentationIsACount() {
		assertEquals("indent 1 foo", say("\tfoo", indentation = true, operators = false))
		assertEquals("indent 4 foo", say("    foo", indentation = true, operators = false))

		assertEquals(
			"indent 1 foo\nindent 2 bar",
			say("\tfoo\n\t\tbar", indentation = true, operators = false)
		)
	}

	@Test
	fun nothingHappensWhenTheMasterSwitchIsOff() {
		val text = "if (a != b) { getElementById(); }"
		assertEquals(text, CodeReading.apply(text, CodeReading.OFF))
	}

	@Test
	fun indentationAndPathsDoNotUndoEachOther() {

		assertEquals(
			"indent 1 at path com slash App dot kt",
			say("\tat com/App.kt", paths = true, indentation = true)
		)
	}

	private fun say(
		text: String,
		camel: Boolean = false,
		snake: Boolean = false,
		operators: Boolean = false,
		braces: Boolean = false,
		indentation: Boolean = false,
		paths: Boolean = false,
		urls: Boolean = false
	): String = CodeReading.apply(
		text,
		CodeReading.Options(
			camelCase = camel,
			snakeCase = snake,
			operators = operators,
			braces = braces,
			indentation = indentation,
			paths = paths,
			urls = urls
		)
	)
}
