package com.blindtechnexus.freeeloquencetts

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.languageid.LanguageIdentification
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
 *
 * This library once took the blame for a warning about a malformed ZIP entry in
 * the APK. It was not to blame: the entry named in the warning changes with
 * whichever library happens to sit at the wrong offset after packaging, and it
 * is the packaging that decides that, not the library. Every entry of the APK
 * is now read back and compared against its own central directory record by
 * tools/check-apk-entries.py, so an entry that cannot be read fails the build
 * whichever library it belongs to.
 */
internal object LanguageIdentifier {

	/**
	 * How long one span may take before it is answered in the language it
	 * already had.
	 *
	 * It was nine hundred milliseconds, which is an eternity in front of the
	 * first syllable: the question is asked on the framework's one synthesis
	 * thread, so every one of those milliseconds is silence the person who
	 * asked for speech hears. The bundled model answers a sentence in single
	 * digit milliseconds; a quarter of a second is already the answer of a
	 * model that has gone wrong, and a wrong language for one word is a smaller
	 * fault than a late sentence.
	 */
	const val TIMEOUT_MS = 250L

	/** How many spans are asked about at once. Four is what a phone has cores
	 *  for; more threads than that only queue. */
	private const val THREADS = 4

	/** A span shorter than this is not a language: a stray "a" is English in
	 *  the middle of a German word. */
	private const val SHORTEST = 2

	/** Above this many spans the rest are answered in the language of the last
	 *  span that was answered: a document can be a hundred thousand words and
	 *  the voice still has to start. */
	private const val MOST_SPANS = 320

	private val pool: ExecutorService = Executors.newFixedThreadPool(THREADS) { work ->
		Thread(work, "evv-language").apply { isDaemon = true }
	}

	private val client: MlKitLanguageIdentifier? by lazy {
		try {
			// The default confidence floor of this release is what the router
			// wants: a language it is not sure about comes back as "und", and
			// the caller's own language is kept for it. See [LanguageRouter].
			LanguageIdentification.getClient()
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
	 * One task per span, up to [THREADS] running at a time, and a future per
	 * span so that a span that is slow cannot hold up the ones after it: what
	 * is waited for is the whole list, with each span's own deadline already
	 * applied inside the task.
	 *
	 * The answers are read in the order the spans came in and the language of
	 * the last answered span is carried from there, not from the task that
	 * happened to finish first: the tasks run in parallel and a span is only
	 * ever answered in the language of the span before it, never in the
	 * language of a span that comes later in the sentence.
	 */
	fun detectAll(texts: List<String>): List<String?> {
		if (texts.isEmpty()) return emptyList()
		if (!available()) return List(texts.size) { null }
		val asked = minOf(texts.size, MOST_SPANS)
		val futures = ArrayList<Future<String?>?>(asked)
		for (at in 0 until asked) {
			futures.add(
				try {
					val text = texts[at]
					pool.submit(Callable { block(text) })
				} catch (_: Throwable) {
					// A pool that refused the work is a span without an answer,
					// not an utterance without speech.
					null
				}
			)
		}
		val out = ArrayList<String?>(texts.size)
		var last: String? = null
		for (at in texts.indices) {
			val answer = if (at < asked) {
				try {
					futures[at]?.get(TIMEOUT_MS + 100L, TimeUnit.MILLISECONDS)
				} catch (_: Throwable) {
					null
				}
			} else {
				// Past the cap: the document is one language and this span is
				// part of it.
				null
			}
			if (answer != null) {
				last = answer
				out.add(answer)
			} else if (at < asked) {
				// No answer for this span: the router keeps the caller's own
				// language for it, so nothing is carried forward from here.
				out.add(null)
			} else {
				out.add(last)
			}
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
}
