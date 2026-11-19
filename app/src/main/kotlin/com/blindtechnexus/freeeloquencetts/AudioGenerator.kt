package com.blindtechnexus.freeeloquencetts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioGenerator(context: Context) {

	private val app = context.applicationContext
	private val main = Handler(Looper.getMainLooper())

	@Volatile
	private var engine: EvvEngine? = null

	@Volatile
	private var generation = 0

	@Volatile
	private var working = false

	val busy: Boolean get() = working

	fun render(
		text: String,
		settings: Settings,
		progress: (doneChars: Int, totalChars: Int) -> Unit,
		done: (file: File?) -> Unit
	) {
		cancel()
		val mine = ++generation
		val total = text.length
		working = true
		Thread({
			var file: File? = null
			try {
				file = renderOnThread(text, total, settings, mine, progress)
			} catch (broken: Throwable) {
				Log.e(TAG, "the generation failed", broken)
				file = null
			} finally {
				if (mine == generation) working = false
				val made = if (mine == generation) file else null
				main.post { done(made) }
			}
		}, "evv-generate").also { it.isDaemon = true }.start()
	}

	private fun renderOnThread(
		text: String,
		total: Int,
		settings: Settings,
		mine: Int,
		progress: (doneChars: Int, totalChars: Int) -> Unit
	): File? {
		if (text.isBlank() || total > MAX_CHARS) return null
		if (!EvvNative.loaded) return null

		val e = EvvEngine.open(settings.language)?.also { engine = it } ?: return null
		try {
			e.setSampleRate(settings.sampleRateHz)
			e.setUpsampleMethod(AudioOptimization.methodFor(settings.audioOptimization))
			e.setAbbreviations(settings.abbreviations)
			e.applyVoice(settings.voice, settings.shape(settings.voice))
			val index = try {
				UserDictionary.snapshot(app).index
			} catch (broken: Throwable) {
				WordIndex.EMPTY
			}
			val abbreviations = try {
				Abbreviations.entries(app)
			} catch (broken: Throwable) {
				emptyList()
			}
			val prosody = Prosody.prefix(settings.phrasePrediction)
			val optimizer = AudioOptimizer(settings.audioOptimization, e.sampleRateHz)
			val samples = ByteArrayOutputStream(65536)
			val buffer = ByteArray(READ_CHUNK)
			var spokenChars = 0
			val pieces = TextPipeline.pieces(text, settings, index, abbreviations, settings.language).iterator()
			var held: TextPieces.Piece? = try {
				if (pieces.hasNext()) pieces.next() else null
			} catch (broken: Throwable) {
				null
			}
			if (held == null) return null
			while (mine == generation) {
				val piece = held ?: break
				val next = try {
					if (pieces.hasNext()) pieces.next() else null
				} catch (broken: Throwable) {
					null
				}
				val last = next == null
				var audio = try {
					prosody + Pauses.apply(piece.text, settings.pauses, last)
				} catch (broken: Throwable) {
					prosody + piece.text
				}
				if (piece.forced && !last) audio += Pauses.AT_A_BOUNDARY
				spokenChars += piece.text.length
				try {
					progress(spokenChars.coerceAtMost(total), total)
				} catch (broken: Throwable) {

				}
				if (EvvEngine.isBlankSpeech(audio)) {
					held = next
					continue
				}
				if (!e.speak(audio, isStopped = { mine != generation })) {
					Log.e(TAG, "the engine refused ${audio.length} chars of the generation")
					return null
				}
				while (mine == generation) {
					val n = e.read(buffer)
					if (n == EvvTtsService.TIMED_OUT) {
						Log.e(TAG, "the engine went silent in the middle of the generation")
						return null
					}
					if (n <= 0) break
					optimizer.process(buffer, n)
					samples.write(buffer, 0, n)
				}
				held = next
			}
			if (mine != generation) return null
			if (samples.size() == 0) return null
			return writeWav(samples.toByteArray(), e.sampleRateHz)
		} finally {
			engine = null
			try {
				e.close()
			} catch (broken: Throwable) {
			}
		}
	}

	fun cancel() {
		generation++
		try {
			engine?.stop()
		} catch (broken: Throwable) {
		}
	}

	private fun writeWav(samples: ByteArray, rateHz: Int): File? {
		return try {
			val into = File(app.cacheDir, AUDIO_DIR)
			if (!into.isDirectory) into.mkdirs()
			val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
			val file = File(into, "eloquence-$stamp.wav")
			DataOutputStream(FileOutputStream(file)).use { out ->
				val frames = samples.size / 2
				val data = frames * 2
				out.writeBytes("RIFF")
				out.writeIntLE(36 + data)
				out.writeBytes("WAVE")
				out.writeBytes("fmt ")
				out.writeIntLE(16)
				out.writeShortLE(1) 
				out.writeShortLE(1) 
				out.writeIntLE(rateHz)
				out.writeIntLE(rateHz * 2)
				out.writeShortLE(2) 
				out.writeShortLE(16) 
				out.writeBytes("data")
				out.writeIntLE(data)
				out.write(samples, 0, data)
			}
			file
		} catch (broken: Throwable) {
			Log.e(TAG, "the file could not be written", broken)
			null
		}
	}

	private fun DataOutputStream.writeIntLE(v: Int) {
		write(v and 0xFF)
		write((v ushr 8) and 0xFF)
		write((v ushr 16) and 0xFF)
		write((v ushr 24) and 0xFF)
	}

	private fun DataOutputStream.writeShortLE(v: Int) {
		write(v and 0xFF)
		write((v ushr 8) and 0xFF)
	}

	companion object {
		const val TAG = "freeeloquencetts"

		const val AUDIO_DIR = "audio"

		const val MAX_CHARS = 50_000

		private const val READ_CHUNK = 8192
	}
}
