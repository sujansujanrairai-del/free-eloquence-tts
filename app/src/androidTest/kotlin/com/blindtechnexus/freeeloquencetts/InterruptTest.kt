package com.blindtechnexus.freeeloquencetts

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class InterruptTest {

	private class Report : UtteranceProgressListener() {
		val done: MutableList<String> = Collections.synchronizedList(ArrayList())
		val cut: MutableList<String> = Collections.synchronizedList(ArrayList())

		override fun onStart(id: String) = Unit

		override fun onDone(id: String) {
			done.add(id)
		}

		override fun onStop(id: String, interrupted: Boolean) {
			cut.add(id)
		}

		@Deprecated("the framework still calls it")
		override fun onError(id: String) = Unit
	}

	@Test
	fun eachRequestCutsOffTheOneBefore() {
		val report = Report()
		val tts = connect()
		try {
			tts.setOnUtteranceProgressListener(report)
			for (turn in 1..TURNS) {
				tts.speak(ITEMS[turn % ITEMS.size], TextToSpeech.QUEUE_FLUSH, null, "turn$turn")
				Thread.sleep(SWIPE_MS)
			}
			waitFor { report.done.contains("turn$TURNS") }
			assertEquals(
				"the last one should be the only one heard out",
				listOf("turn$TURNS"),
				report.done.toList()
			)
			assertEquals(
				"every earlier one should have been cut off",
				(1 until TURNS).map { "turn$it" },
				report.cut.toList()
			)
		} finally {
			tts.shutdown()
		}
	}

	@Test
	fun aLongUtteranceGivesWayQuickly() {
		val report = Report()
		val tts = connect()
		try {
			tts.setOnUtteranceProgressListener(report)
			tts.speak(PARAGRAPH, TextToSpeech.QUEUE_FLUSH, null, "long")
			Thread.sleep(SWIPE_MS)
			val asked = System.currentTimeMillis()
			tts.speak("Next item.", TextToSpeech.QUEUE_FLUSH, null, "short")
			waitFor { report.cut.contains("long") }
			val took = System.currentTimeMillis() - asked
			assertTrue("the paragraph was never cut off", report.cut.contains("long"))
			assertTrue("it took ${took}ms to give way", took < GIVE_WAY_MS)
		} finally {
			tts.shutdown()
		}
	}

	@Test
	fun synthesisDoesNotRunAheadOfTheSpeech() {
		EvvTtsService.mostAudioHandedMs = 0
		val report = Report()
		val tts = connect()
		try {
			tts.setOnUtteranceProgressListener(report)
			tts.speak(PARAGRAPH, TextToSpeech.QUEUE_FLUSH, null, "long")
			Thread.sleep(SPEAKING_MS)
			tts.speak("Next item.", TextToSpeech.QUEUE_FLUSH, null, "short")
			waitFor { report.cut.contains("long") }
			val handed = EvvTtsService.mostAudioHandedMs
			assertTrue("the paragraph was never cut off", report.cut.contains("long"))

			assertTrue(
				"${handed}ms of audio was handed over in ${SPEAKING_MS}ms of speaking",
				handed < AHEAD_LIMIT_MS
			)
		} finally {
			tts.shutdown()
		}
	}

	private fun waitFor(until: Long = SETTLE_MS, what: () -> Boolean) {
		val stop = System.currentTimeMillis() + until
		while (!what() && System.currentTimeMillis() < stop) Thread.sleep(20)
	}

	private fun connect(): TextToSpeech {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val ready = CountDownLatch(1)
		val made = TextToSpeech(context, { status ->
			if (status == TextToSpeech.SUCCESS) ready.countDown()
		}, context.packageName)
		assertTrue("the engine never connected", ready.await(20, TimeUnit.SECONDS))
		return made
	}

	private companion object {
		const val TURNS = 5
		const val SWIPE_MS = 250L
		const val SETTLE_MS = 8000L
		const val GIVE_WAY_MS = 1500L
		const val SPEAKING_MS = 700L
		const val AHEAD_LIMIT_MS = 1500L
		val ITEMS = listOf(
			"Settings, button, one of four.",
			"Navigate up, button, two of four.",
			"Wireless and networks, two bars, three of four.",
			"Battery, eighty percent remaining, four of four."
		)
		val PARAGRAPH = ("The quick brown fox jumps over the lazy dog. " +
			"Every good boy deserves favour, and the rain in Spain stays mainly in the plain. ")
			.repeat(6)
	}
}
