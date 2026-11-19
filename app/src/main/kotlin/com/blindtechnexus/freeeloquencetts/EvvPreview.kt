package com.blindtechnexus.freeeloquencetts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

class EvvPreview() {

	private val thread = HandlerThread("evv-preview").apply { start() }
	private val work = Handler(thread.looper)

	@Volatile
	private var engine: EvvEngine? = null

	@Volatile
	private var track: AudioTrack? = null

	@Volatile
	private var generation = 0

	@Volatile
	private var english = Eci.ENGLISH_US

	@Volatile
	var bytesPlayed: Long = 0L
		private set

	@Volatile
	var playing: Boolean = false
		private set

	@Volatile
	var drained: Boolean = false
		private set

	fun presetShape(index: Int): Map<Int, Int> = engine?.presetShape(index) ?: emptyMap()

	fun open(language: Int = Eci.ENGLISH_US, then: (() -> Unit)? = null) {
		if (playing && engine?.language != language) stopSpeaking()
		work.post {
			english = language
			if (engine == null || engine?.language != language) {

				runCatching { engine?.close() }
				engine = EvvEngine.open(language)
			}
			then?.let { Handler(android.os.Looper.getMainLooper()).post(it) }
		}
	}

	fun say(
		text: String,
		voice: Int,
		shape: Map<Int, Int>,
		rateHz: Int,
		abbreviations: Boolean = false,
		optimization: Int = AudioOptimization.BALANCED
	) {

		stopSpeaking()
		val mine = generation
		work.post {
			if (mine != generation) return@post
			val e = try {
				engine ?: EvvEngine.open(english)?.also { engine = it } ?: return@post
			} catch (_: Throwable) {
				return@post
			}
			try {
				e.setSampleRate(rateHz)
				e.setUpsampleMethod(AudioOptimization.methodFor(optimization))
				e.setAbbreviations(abbreviations)
				e.applyVoice(voice, shape)
			} catch (_: Throwable) {
			}
			if (!e.speak(text, isStopped = { mine != generation })) {
				if (mine == generation) {
					Log.e(TAG, "the engine refused the sample")

					try {
						e.close()
					} catch (_: Throwable) {
					}
					if (engine === e) engine = null
				}
				return@post
			}
			play(e, mine, AudioOptimizer(optimization, e.sampleRateHz))
		}
	}

	private fun play(e: EvvEngine, mine: Int, optimizer: AudioOptimizer) {
		playing = true
		drained = false

		var t: AudioTrack? = null
		try {
			val rate = e.sampleRateHz
			val minimum = try {
				AudioTrack.getMinBufferSize(
					rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
				)
			} catch (_: Throwable) {
				0
			}
			val size = if (minimum > 0) maxOf(minimum, CHUNK * 4) else CHUNK * 4
			t = try {
				AudioTrack.Builder()
					.setAudioAttributes(
						AudioAttributes.Builder()
							.setUsage(AudioAttributes.USAGE_ASSISTANT)
							.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
							.build()
					)
					.setAudioFormat(
						AudioFormat.Builder()
							.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
							.setSampleRate(rate)
							.setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
							.build()
					)
					.setBufferSizeInBytes(size)
					.setTransferMode(AudioTrack.MODE_STREAM)
					.build()
			} catch (broken: Throwable) {
				Log.e(TAG, "no audio track for the sample", broken)
				null
			} ?: return
			track = t
			try {
				t.play()
			} catch (broken: Throwable) {
				Log.e(TAG, "the sample would not play", broken)
				return
			}
			val buffer = ByteArray(CHUNK)
			var written = 0L
			while (mine == generation) {
				val n = try {
					e.read(buffer)
				} catch (_: Throwable) {
					-1
				}
				if (n <= 0) break
				try {
					optimizer.process(buffer, n)
				} catch (_: Throwable) {
				}
				var at = 0
				while (at < n && mine == generation) {
					val wrote = try {
						t.write(buffer, at, n - at)
					} catch (_: Throwable) {
						break
					}
					if (wrote <= 0) break
					at += wrote
					bytesPlayed += wrote
					written += wrote / BYTES_PER_FRAME
				}
			}
			if (mine == generation) {

				drained = waitToDrain(t, mine, written, rate)
				runCatching { t.stop() }
			} else {
				runCatching { t.pause(); t.flush(); t.stop() }
			}
		} finally {
			val held = t
			if (held != null) {
				runCatching { held.release() }
				if (track === held) track = null
			}
			playing = false
		}
	}

	private fun waitToDrain(t: AudioTrack, mine: Int, frames: Long, rate: Int): Boolean {
		if (frames <= 0L) return true

		val giveUp = System.currentTimeMillis() + frames * 1000L / rate + MARGIN_MS
		while (mine == generation && System.currentTimeMillis() < giveUp) {
			val head = runCatching { t.playbackHeadPosition.toLong() and 0xFFFFFFFFL }
				.getOrElse { return false }
			if (head >= frames) return true
			try {
				Thread.sleep(SIP_MS)
			} catch (stopping: InterruptedException) {
				Thread.currentThread().interrupt()
				return false
			}
		}
		return false
	}

	fun stopSpeaking() {
		generation++
		engine?.stop()
		runCatching { track?.pause(); track?.flush() }
	}

	fun close() {
		stopSpeaking()
		work.post {
			engine?.close()
			engine = null
			thread.quitSafely()
		}
	}

	private companion object {
		const val TAG = "freeeloquencetts"
		const val CHUNK = 1024 
		const val BYTES_PER_FRAME = 2
		const val MARGIN_MS = 300L
		const val SIP_MS = 5L
	}
}
