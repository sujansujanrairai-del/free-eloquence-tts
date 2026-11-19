package com.blindtechnexus.freeeloquencetts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MarkupStylesTest {

	@Test
	fun boldAndItalicAreSaid() {
		assertEquals("bold sale end bold", MarkupStyles.annotate("<b>sale</b>"))
		assertEquals("bold sale end bold", MarkupStyles.annotate("<strong>sale</strong>"))
		assertEquals("italic sale end italic", MarkupStyles.annotate("<em>sale</em>"))
		assertEquals("strikethrough old end strikethrough", MarkupStyles.annotate("<s>old</s>"))
	}

	@Test
	fun aHeadingIsAHeading() {
		assertEquals("heading 2 Today end heading", MarkupStyles.annotate("<h2>Today</h2>"))
	}

	@Test
	fun aLinkIsALink() {
		assertEquals("link Shop end link", MarkupStyles.annotate("<a href=\"/cart\">Shop</a>"))
	}

	@Test
	fun anImageIsItsAlternativeText() {
		assertEquals("image a cat on a mat", MarkupStyles.annotate("<img alt=\"a cat on a mat\">"))

		assertEquals("image", MarkupStyles.annotate("<img src=\"x.png\">"))
	}

	@Test
	fun aLayoutTagSaysNothingButIsABreak() {
		assertEquals("one two", MarkupStyles.annotate("<div>one</div><div>two</div>"))
		assertEquals("one two", MarkupStyles.annotate("one<br />two"))
		assertEquals("bullet One bullet Two", MarkupStyles.annotate("<ul><li>One</li><li>Two</li></ul>"))
	}

	@Test
	fun anUnclosedTagIsClosedInTheWords() {

		assertEquals("bold sale end bold", MarkupStyles.annotate("<b>sale"))
	}

	@Test
	fun markdownMarksAreSaidTheSameWay() {
		assertEquals("bold sale end bold", MarkupStyles.annotate("**sale**"))
		assertEquals("italic sale end italic", MarkupStyles.annotate("*sale*"))
		assertEquals("bold sale end bold", MarkupStyles.annotate("__sale__"))
	}

	@Test
	fun anAsteriskThatIsNotAMarkIsLeftAlone() {

		assertEquals("2 * 3", MarkupStyles.annotate("2 * 3"))
		assertEquals("a * b * c", MarkupStyles.annotate("a * b * c"))
	}

	@Test
	fun proseWithABracketIsUntouched() {
		val text = "if (a < b) then stop"
		assertSame(text, MarkupStyles.annotate(text))
	}

	@Test
	fun textWithNoMarkupIsTheSameString() {
		val text = "Nothing here but words."
		assertSame(text, MarkupStyles.annotate(text))
	}
}
