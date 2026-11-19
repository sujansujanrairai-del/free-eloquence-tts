package com.blindtechnexus.freeeloquencetts

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmojiTest {

	@Test
	fun anEmojiIsNamedWhenRead() {
		assertEquals("grinning face", Emoji.apply("😀", true))
		assertEquals("Hello grinning face world", Emoji.apply("Hello 😀 world", true))
	}

	@Test
	fun anEmojiIsDroppedWhenNotRead() {
		assertEquals("", Emoji.apply("😀", false))
		assertEquals("Hello world", Emoji.apply("Hello 😀 world", false))
	}

	@Test
	fun aTypedEmoticonIsNamed() {
		assertEquals("smiley face", Emoji.apply(":-)", true))
		assertEquals("heart", Emoji.apply("<3", true))
	}

	@Test
	fun aTypedEmoticonIsDroppedWhenNotRead() {
		assertEquals("", Emoji.apply(":-)", false))
	}

	@Test
	fun plainTextIsUntouched() {
		assertEquals("Just words.", Emoji.apply("Just words.", true))
		assertEquals("Just words.", Emoji.apply("Just words.", false))
	}

	@Test
	fun anEmojiTheTableDoesNotKnowIsLeftAlone() {
		assertEquals("🦄", Emoji.apply("🦄", true))
	}
}
