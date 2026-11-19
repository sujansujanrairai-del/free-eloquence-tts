package com.blindtechnexus.freeeloquencetts

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreamingPipelineTest {

	private lateinit var context: Context
	private lateinit var settings: Settings

	@Before
	fun setUp() {
		context = InstrumentationRegistry.getInstrumentation().targetContext
		settings = Settings(context)
	}

	@Test
	fun thePiecesAreThePiecesTheListGave() {
		for (text in SAMPLES) {
			val asList = TextPieces.pieces(text)
			val asSequence = TextPieces.pieceSequence(text).toList()
			assertEquals("different number of pieces for ${show(text)}", asList.size, asSequence.size)
			for (at in asList.indices) {
				assertEquals("piece $at of ${show(text)}", asList[at].text, asSequence[at].text)
				assertEquals("piece $at of ${show(text)}", asList[at].forced, asSequence[at].forced)
			}

			assertEquals(text, asSequence.joinToString("") { it.text })
		}
	}

	@Test
	fun askingForTheFirstPieceCostsOnePieceOfWork() {
		val document = sentence(400)
		var made = 0
		val stream = TextPipeline
			.pieces(document, settings, emptyList(), emptyList())
			.onEach { made++ }
		stream.take(2).toList()
		assertEquals(2, made)

		val all = made + stream.drop(2).count()
		assertTrue("a document this long has more than two pieces in it", all > 2)
	}

	@Test
	fun aLongDocumentIsCutIntoItsPiecesAndNotIntoFewer() {
		val document = sentence(400)
		assertTrue(document.length > EvvTtsService.STREAM_FROM_CHARS)
		val raw = TextPieces.pieceSequence(document).toList()
		val streamed = TextPipeline
			.pieces(document, settings, emptyList(), emptyList())
			.toList()

		assertEquals(raw.size, streamed.size)
	}

	@Test
	fun theFirstPieceIsOnePieceAndNotTheWholeDocument() {
		val document = sentence(400)
		val streamed = TextPipeline
			.pieces(document, settings, emptyList(), emptyList())
			.toList()

		assertTrue(streamed.size > 50)
		assertTrue(
			"the first piece is ${streamed.first().text.length} characters " +
				"out of a document of ${document.length}",
			streamed.first().text.length < document.length / 20
		)
	}

	@Test
	fun aShortTextIsNotStreamed() {

		val short = "Settings."
		assertTrue(short.length < EvvTtsService.STREAM_FROM_CHARS)
		assertEquals(
			TextPipeline.apply(short, settings, emptyList(), emptyList()),
			TextPipeline.pieces(short, settings, emptyList(), emptyList())
				.toList()
				.joinToString("") { it.text }
		)
	}

	@Test
	fun nothingIsLostOnTheWayThrough() {
		for (text in SAMPLES) {
			if (text.isBlank()) continue
			val said = TextPipeline
				.pieces(text, settings, emptyList(), emptyList())
				.toList()
			assertTrue("a text is never reduced to nothing by being cut up", said.isNotEmpty())
			assertTrue(
				"every piece of ${show(text)} says something",
				said.all { it.text.isNotBlank() }
			)
		}
	}

	private fun sentence(times: Int): String = buildString {
		for (at in 1..times) {
			append("This is the sentence number ")
			append(at)
			append(" of a document that goes on for some way. ")
		}
	}

	private fun show(text: String): String =
		if (text.length > 40) text.take(37) + "..." else text

	private companion object {
		val SAMPLES = listOf(
			"",
			"Settings.",
			"One two three. Four five six. Seven eight nine.",
			"Mr. Jones and J. R. R. Tolkien met at 3.15 on the U.S. side.",
			"   leading and trailing whitespace   ",
			"a word that goes on and on with no punctuation at all",
			"First line.\nSecond line.\n\nThird paragraph.",
			"trailing space only.   "
		)
	}
}
