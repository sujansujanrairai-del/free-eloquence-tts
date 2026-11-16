package com.blindtechnexus.freeeloquencetts

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier as MlKitLanguageIdentifier
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Google's on-device language identification, asked many questions at once.
 *
 * ML Kit's language-id model is small, bundled in the app and answered on the
 * device: no network, no round trip, and a few milliseconds for a sentence. That
 * is what makes a per-word language decision affordable in a speech engine,
 * where the whole utterance is allowed a few dozen milliseconds before the
 * first syllable is late.
 *
 * Two things make it fast enough to use at word granularity:
 *
 * The questions are asked on a pool, not in a row. A mixed utterance is a dozen
 * spans and each answer is independent of the others, so four threads take four
 * chunks of the work rather than paying the model's latency a dozen times over.
 * The results come back in the order they went out, because the spans are a
 * sentence and their order is the sentence.
 *
 * Every answer has a deadline. A detector that has not answered in [TIMEOUT_MS]
 * answers null, which the router reads as "leave this span in the language it
 * was already in". A voice may be slightly wrong about a word; it may not be
 * late.
 *
 * The model is asked for a language and not for confidence: a confidence number
 * is only meaningful against a corpus, and what the caller does with a
 * low-confidence answer here -- split the sentence finer -- is decided from the
 * script and the shape of the text instead. See [LanguageRouter].
 */
internal object LanguageIdentifier {

	/** How long one span may take before it is answered in the language it
	 *  already had. */
	const val TIMEOUT_MS = 900L

	/** How many spans are asked about at once. Four is what a phone has cores
	 *  for; more threads than that only queue. */
	private const val THREADS = 4

	/** A span shorter than this is not a language: a stray "a" is English in
	 *  the middle of a German word. */
	private const val SHORTEST = 2

	/** Above this many spans the rest are answered in the language of the span
	 *  before them: a document can be a hundred thousand words and the voice
	 *  still has to start. */
	private const val MOST_SPANS = 320

	private val pool: ExecutorService = Executors.newFixedThreadPool(THREADS) { work ->
		Thread(work, "evv-language").apply { isDaemon = true }
	}

	private val client: MlKitLanguageIdentifier? by lazy {
		try {
			LanguageIdentification.getClient(
				LanguageIdentificationOptions.Builder()
					.setMinimumConfidence(0.30f)
					.build()
			)
		} catch (_: Throwable) {
			// A device with no model is a device without detection, not a
			// device without speech: the router falls back to the language the
			// caller asked for.
			null
		}
	}

	/** Whether this device can be asked at all. */
	fun available(): Boolean = client != null

	/** The ISO-639-1 code [text] is written in, or null for no answer. Blocks
	 *  the calling thread for no longer than [TIMEOUT_MS]. */
	fun detect(text: String): String? = block(text)

	/**
	 * The codes for [texts], in the same order.
	 *
	 * One thread per span, up to [THREADS] at a time, and a future per span so
	 * that a span that is slow cannot hold up the ones after it: what is
	 * waited for is the whole list, with each span's own deadline already
	 * applied inside the task.
	 */
	fun detectAll(texts: List<String>): List<String?> {
		if (texts.isEmpty()) return emptyList()
		if (!available()) return List(texts.size) { null }
		val futures = ArrayList<Future<String?>>(texts.size)
		var last: String? = null
		for ((at, text) in texts.withIndex()) {
			if (at >= MOST_SPANS) {
				// Past the cap the previous answer is used, which is right for
				// the shape that gets here: a document, in one language, with
				// one long sentence after another.
				futures.add(CompletedFuture(last))
				continue
			}
			val asked = try {
				pool.submit(Callable { block(text).also { last = it ?: last } })
			} catch (_: Throwable) {
				null
			}
			if (asked == null) {
				futures.add(CompletedFuture(last))
			} else {
				futures.add(asked)
			}
		}
		val out = ArrayList<String?>(futures.size)
		for (future in futures) {
			out.add(
				try {
					future.get(TIMEOUT_MS + 250L, TimeUnit.MILLISECONDS)
				} catch (_: Throwable) {
					null
				}
			)
		}
		return out
	}

	/** The blocking question, with every failure answered as "no answer". */
	private fun block(text: String): String? {
		val engine = client ?: return null
		val asked = text.trim()
		if (asked.length < SHORTEST) return null
		// A span of punctuation has no language, and asking costs a task.
		if (asked.none { it.isLetter() }) return null
		return try {
			Tasks.await(engine.identifyLanguage(asked), TIMEOUT_MS, TimeUnit.MILLISECONDS)
		} catch (_: Throwable) {
			null
		}
	}

	/** A future that is already answered, for the spans past the cap and for
	 *  a pool that refused the work. */
	private class CompletedFuture(private val value: String?) : Future<String?> {
		override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
		override fun isCancelled(): Boolean = false
		override fun isDone(): Boolean = true
		override fun get(): String? = value
		override fun get(timeout: Long, unit: TimeUnit): String? = value
	}
}
