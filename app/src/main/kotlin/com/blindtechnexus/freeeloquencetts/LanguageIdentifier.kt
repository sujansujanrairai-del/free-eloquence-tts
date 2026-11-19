package com.blindtechnexus.freeeloquencetts

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier as MlKitLanguageIdentifier
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal object LanguageIdentifier {

	const val TIMEOUT_MS = 250L

	private const val THREADS = 4

	private const val SHORTEST = 2

	private const val MOST_SPANS = 320

	private val pool: ExecutorService = Executors.newFixedThreadPool(THREADS) { work ->
		Thread(work, "evv-language").apply { isDaemon = true }
	}

	private val client: MlKitLanguageIdentifier? by lazy {
		try {

			LanguageIdentification.getClient()
		} catch (_: Throwable) {

			null
		}
	}

	fun available(): Boolean = client != null

	fun detect(text: String): String? = block(text)

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

				null
			}
			if (answer != null) {
				last = answer
				out.add(answer)
			} else if (at < asked) {

				out.add(null)
			} else {
				out.add(last)
			}
		}
		return out
	}

	private fun block(text: String): String? {
		val engine = client ?: return null
		val asked = text.trim()
		if (asked.length < SHORTEST) return null

		if (asked.none { it.isLetter() }) return null
		return try {
			Tasks.await(engine.identifyLanguage(asked), TIMEOUT_MS, TimeUnit.MILLISECONDS)
		} catch (_: Throwable) {
			null
		}
	}
}
