package com.blindtechnexus.freeeloquencetts

import android.media.AudioFormat
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import com.blindtechnexus.freeeloquencetts.pronunciation.DevanagariPronunciation
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

class EvvTtsService : TextToSpeechService() {

	private val engines = HashMap<Int, EvvEngine>()

	private val engineLock = Any()

	private val appliedRevisions = HashMap<Int, Int>()

	private val suspect = HashSet<Int>()

	private var settings: Settings? = null

	@Volatile
	private var active: Run? = null

	internal class Run {
		@Volatile
		var stopped = false
	}

	@Volatile
	private var loadedAbbreviationsVersion = Int.MIN_VALUE

	@Volatile
	private var loadedAbbreviations: List<Abbreviations.Entry> = emptyList()

	private class Substitutions(
		val index: WordIndex,
		val abbreviations: List<Abbreviations.Entry>
	)

	private val gate = Object()

	@Volatile
	private var feeder: Prefetcher? = null

	private var chunk: ByteArray? = null

	private var player: SynthesisRouting.Player? = null

	@Volatile
	private var playingItself = false

	private val speaking = AtomicInteger()

	@Volatile
	private var engineIsSuspect = false

	@Volatile
	private var suspectLanguage = Eci.ENGLISH_US

	private fun markSuspect(language: Int) {
		suspectLanguage = language
		engineIsSuspect = true
	}

	private val textWork: ExecutorService = Executors.newSingleThreadExecutor(
		java.util.concurrent.ThreadFactory { work ->
			Thread(work, "evv-text").apply { isDaemon = true }
		}
	)

	@Volatile
	private var alive = false

	private val wakeLockManager: PowerManager? by lazy { getSystemService(PowerManager::class.java) }
	private var wakeLock: PowerManager.WakeLock? = null
	private var tailLock: PowerManager.WakeLock? = null

	private var keepAlive: KeepAlive? = null

	private val main = Handler(Looper.getMainLooper())

	private val closeIdle = Runnable {
		if (speaking.get() > 0) return@Runnable
		val held = synchronized(engineLock) {
			if (speaking.get() > 0) return@synchronized emptyList<EvvEngine>()
			val given = ArrayList<EvvEngine>(suspect.size)
			for (language in suspect.toList()) {
				val instance = engines.remove(language) ?: continue
				appliedRevisions.remove(language)
				suspect.remove(language)
				given.add(instance)
			}
			given
		}

		if (held.isNotEmpty()) {
			Thread({
				for (instance in held) {
					try {
						instance.close()
					} catch (_: Throwable) {
					}
				}
				Log.i(TAG, "the quiet engine was given back; the next utterance opens a fresh one")
			}, "evv-give-back").also { it.isDaemon = true }.start()
		}
	}

	override fun onCreate() {
		super.onCreate()
		alive = true
		instance = this
		val made = Settings(this)
		settings = made
		keepAlive = KeepAlive(this)
		keepAlive?.sync(made.persistentNotification)
		warmUp()
	}

	override fun onDestroy() {
		super.onDestroy()
		alive = false
		if (instance === this) instance = null
		main.removeCallbacks(closeIdle)
		keepAlive?.release()
		keepAlive = null
		try { player?.release() } catch (_: Throwable) {}
		player = null
		val held = synchronized(engineLock) {
			val given = engines.values.toList()
			engines.clear()
			appliedRevisions.clear()
			suspect.clear()
			given
		}

		if (held.isNotEmpty()) {
			Thread({
				for (instance in held) {
					try {
						instance.close()
					} catch (_: Throwable) {
					}
				}
			}, "evv-destroy").also { it.isDaemon = true }.start()
		}
		releaseWakeLock(wakeLock)
		wakeLock = null
		releaseWakeLock(tailLock)
		tailLock = null
		textWork.shutdownNow()
	}

	private fun warmUp() {
		if (!EvvNative.loaded) return
		Thread({
			if (!alive) return@Thread
			substitutions()
			synchronized(engineLock) {
				if (!alive || speaking.get() > 0) return@synchronized
				val made = ensureEngineLocked(settings?.language ?: Eci.ENGLISH_US)
				if (made == null) return@synchronized
				settings?.let { applySettings(made, made.language, it) }
			}
		}, "evv-warm").also { it.isDaemon = true }.start()
	}

	private fun availabilityOf(lang: String?, country: String?): Int {
		val spoken = Eci.languageFor(lang, country) ?: return TextToSpeech.LANG_AVAILABLE
		return if (spoken.inCountry(country)) TextToSpeech.LANG_COUNTRY_AVAILABLE else TextToSpeech.LANG_AVAILABLE
	}

	private fun languageFor(request: SynthesisRequest, s: Settings): Int {
		if (s.forceLanguage) return s.language

		val named = request.voiceName
		if (!named.isNullOrEmpty()) Eci.languageOfVoiceName(named)?.let { return it.code }
		return Eci.languageFor(request.language, request.country)?.code ?: s.language
	}

	override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int =
		availabilityOf(lang, country)

	override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {

		val made = try {
			synchronized(engineLock) { ensureEngineLocked(languageFor(lang, country)) }
		} catch (_: Throwable) {
			null
		}
		if (made == null) return TextToSpeech.LANG_NOT_SUPPORTED
		return availabilityOf(lang, country)
	}

	private fun languageFor(lang: String?, country: String?): Int {
		val s = settings
		if (s != null && s.forceLanguage) return s.language
		return Eci.languageFor(lang, country)?.code ?: s?.language ?: Eci.ENGLISH_US
	}

	override fun onGetLanguage(): Array<String> {
		val spoken = Eci.languageOf(settings?.language ?: Eci.ENGLISH_US)
		return arrayOf(spoken.iso3Language, spoken.iso3Country, "")
	}

	private fun voiceName(preset: Int, language: Int = Eci.ENGLISH_US): String =
		Eci.voiceName(preset.coerceIn(Eci.PRESET_NAMES.indices), language)

	override fun onGetVoices(): MutableList<Voice> {

		val out = ArrayList<Voice>(Eci.PRESET_NAMES.size * Eci.LANGUAGES.size)
		for (language in Eci.LANGUAGES) {
			val locale = language.locale()
			for (preset in Eci.PRESET_NAMES.indices) {
				try {
					out.add(
						Voice(
							voiceName(preset, language.code),
							locale,
							Voice.QUALITY_NORMAL,
							Voice.LATENCY_VERY_LOW,
							false,
							emptySet()
						)
					)
				} catch (_: Throwable) {

				}
			}
		}
		return out
	}

	override fun onIsValidVoiceName(name: String?): Int =
		if (name.isNullOrEmpty()) TextToSpeech.ERROR else TextToSpeech.SUCCESS

	override fun onLoadVoice(name: String?): Int {
		if (name.isNullOrEmpty()) return TextToSpeech.ERROR
		val made = try {
			synchronized(engineLock) { ensureEngineLocked(languageFor(name)) }
		} catch (_: Throwable) {
			null
		}
		if (made == null) return TextToSpeech.ERROR
		return TextToSpeech.SUCCESS
	}

	private fun languageFor(name: String): Int {
		val s = settings
		if (s != null && s.forceLanguage) return s.language
		return Eci.languageOfVoiceName(name)?.code ?: s?.language ?: Eci.ENGLISH_US
	}

	override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String = try {
		voiceName(
			(settings?.voice ?: 0).coerceIn(Eci.PRESET_NAMES.indices),
			languageFor(lang, country)
		)
	} catch (_: Throwable) {
		voiceName(0)
	}

	private fun ensureEngineLocked(language: Int): EvvEngine? {
		engines[language]?.let { return it }
		val made = try {
			EvvEngine.open(language) ?: if (language != Eci.ENGLISH_US) EvvEngine.open(Eci.ENGLISH_US) else null
		} catch (_: Throwable) {
			null
		}
		if (made == null) return null
		engines[language] = made
		if (made.language != language) {

			engines[made.language] = made
		}
		try {
			settings?.let { applySettings(made, language, it) }
		} catch (_: Throwable) {

		}
		return made
	}

	private fun engineFor(language: Int): EvvEngine? {
		val made = ensureEngineLocked(language)
		if (made == null) return null
		val s = settings ?: return made
		if (appliedRevisions[language] != s.revision) {
			applySettings(made, language, s)
		}
		return made
	}

	private fun applySettings(target: EvvEngine, language: Int, s: Settings) {
		try {
			target.setSampleRate(s.sampleRateHz)
		} catch (_: Throwable) {
		}
		try {
			target.setUpsampleMethod(AudioOptimization.methodFor(s.audioOptimization))
		} catch (_: Throwable) {
		}
		try {
			target.setAbbreviations(s.abbreviations)
		} catch (_: Throwable) {
		}
		try {
			target.applyVoice(s.voice, s.shape(s.voice))
		} catch (_: Throwable) {
		}
		appliedRevisions[language] = s.revision
	}

	@Synchronized
	private fun substitutions(): Substitutions {
		val dictionary = runCatching { UserDictionary.snapshot(this) }.getOrNull()
		val index = dictionary?.index ?: WordIndex.EMPTY
		val revision = Abbreviations.version
		if (revision != loadedAbbreviationsVersion) {
			loadedAbbreviations = runCatching { Abbreviations.entries(this) }
				.getOrNull() ?: loadedAbbreviations
			loadedAbbreviationsVersion = revision
		}
		return Substitutions(index, loadedAbbreviations)
	}

	override fun onStop() = stopSpeakingNow()

	internal fun stopSpeakingNow() {
		val mine = active
		synchronized(gate) {

			mine?.stopped = true
			gate.notifyAll()
		}

		feeder?.cancel()

		try { player?.cut() } catch (_: Throwable) {}

		for (held in synchronized(engineLock) { engines.values.toList() }) {
			runCatching { held.stop() }
				.onFailure { Log.e(TAG, "the abort did not reach the engine", it) }
		}
	}

	override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
		if (request == null || callback == null) return

		val previous = active
		val run = Run()
		active = run
		synchronized(gate) {
			previous?.stopped = true
			gate.notifyAll()
		}
		val s = settings
		if (s == null) {
			try {
				callback.error(TextToSpeech.ERROR_SERVICE)
			} catch (_: Throwable) {
			}
			return
		}
		speaking.incrementAndGet()
		main.removeCallbacks(closeIdle)
		try {

			keepAlive?.sync(s.persistentNotification)
			if (!EvvNative.loaded) {
				try {
					callback.error(TextToSpeech.ERROR_NOT_INSTALLED_YET)
				} catch (_: Throwable) {
				}
				return
			}

			val language = try {
				languageFor(request, s)
			} catch (_: Throwable) {
				s.language
			}
			val target = try {
				synchronized(engineLock) { engineFor(language) }
			} catch (_: Throwable) {
				null
			}
			if (target == null) {
				try {
					callback.error(TextToSpeech.ERROR_SERVICE)
				} catch (_: Throwable) {
				}
				return
			}
			try {
				say(target, request, callback, s, run)
			} catch (failed: Throwable) {

				Log.e(TAG, "the utterance was lost", failed)
				try {
					callback.error(TextToSpeech.ERROR_SYNTHESIS)
				} catch (broken: Throwable) {

				}
			}
		} finally {
			speaking.decrementAndGet()
			afterAnUtterance()
		}
	}

	private fun say(
		target: EvvEngine,
		request: SynthesisRequest,
		callback: SynthesisCallback,
		s: Settings,
		run: Run
	) {
		try { target.setAbbreviations(s.abbreviations) } catch (_: Exception) {}
		try {
			if (!s.forceRate) target.setRatePercent(request.speechRate)
			if (!s.forcePitch) target.setPitchPercent(request.pitch)
		} catch (_: Exception) {}
		if (s.forceVolume) {
			try { s.shape(s.voice)[Eci.VOICE_VOLUME]?.let { v -> target.setVolume(v) } } catch (_: Exception) {}
		}

		val lock = acquireWakeLock()
		try {
			val rawText = styledText(request, s)
			if (rawText.isEmpty()) {
				try {
					callback.start(target.sampleRateHz, AudioFormat.ENCODING_PCM_16BIT, 1)
					callback.done()
				} catch (_: Throwable) {

				}
				return
			}
			if (run.stopped) return
			val opening = Opening(callback, target.sampleRateHz)
			val pace = Pace(target.sampleRateHz * BYTES_PER_SAMPLE)
			val buffer = bufferFor(callback)

			val route = s.synthesisRouting
			var toFramework = SynthesisRouting.handsToFramework(route)
			var own: SynthesisRouting.Player? = null
			if (SynthesisRouting.playsItself(route)) {
				own = playerFor(route, target.sampleRateHz, request)
				if (own == null) toFramework = true
			}
			playingItself = own != null
			val prosody = Prosody.prefix(s.phrasePrediction)
			val lead = Pacing.DEFAULT_MS.toLong()
			val replacements = substitutions()

			val optimizer = AudioOptimizer(s.audioOptimization, target.sampleRateHz)

			val segments = try {
				LanguageRouter.plan(rawText, s, target.language)
			} catch (_: Throwable) {
				listOf(LanguageRouter.Segment(target.language, rawText))
			}

			try {
				for (segment in segments) {
					if (run.stopped) break

					val engine = engineForSegment(target, segment.language, request, s)
					val work = Prefetcher(segment.text, s, replacements, prosody, segment.language)
					feeder = work
					try {

						var failures = 0
						while (!run.stopped) {

							val audio = work.take() ?: break
							if (run.stopped) break

							if (EvvEngine.isBlankSpeech(audio)) continue
							try {
								if (!engine.speak(audio, isStopped = { run.stopped })) {

									if (run.stopped) return
									Log.e(TAG, "engine refused ${audio.length} chars")
									markSuspect(engine.language)
									try {
										if (opening.began) callback.done() else callback.error(TextToSpeech.ERROR_SYNTHESIS)
									} catch (_: Throwable) {
									}
									return
								}
								when (pump(engine, callback, pace, opening, buffer, optimizer, Stability.ENGINE_SILENCE_MS, lead, own, toFramework, run)) {
									Pump.Next -> Unit
									Pump.Over -> { remember(pace); finish(callback, opening, run); return }

									Pump.Gone -> {
										remember(pace)
										if (!run.stopped) finish(callback, opening, run)
										return
									}
								}
								failures = 0
							} catch (failed: Throwable) {

								Log.e(TAG, "a piece failed (${audio.length} chars) and was skipped", failed)
								failures++
								try {
									engine.stop()
								} catch (broken: Throwable) {

								}
								markSuspect(engine.language)
								if (failures >= MAX_PIECE_FAILURES) {
									remember(pace)
									finish(callback, opening, run)
									return
								}
							}
						}
					} catch (failed: Throwable) {

						Log.e(TAG, "a segment failed and the utterance was finished", failed)
						markSuspect(engine.language)
						try {
							engine.stop()
						} catch (broken: Throwable) {

						}
						remember(pace)
						finish(callback, opening, run)
						return
					} finally {

						work.cancel()
						if (feeder === work) feeder = null
					}
				}
			} finally {
				playingItself = false
			}

			if (!opening.began && !run.stopped) {
				Log.w(TAG, "nothing of the utterance survived the text road; saying it as it came")
				speakAsItCame(
					target, rawText, s, callback, pace, opening, buffer, optimizer,
					own, toFramework, run
				)
			}
			remember(pace)
			finish(callback, opening, run)
		} finally {
			releaseWakeLock(lock)
		}
	}

	private fun speakAsItCame(
		target: EvvEngine,
		rawText: String,
		s: Settings,
		callback: SynthesisCallback,
		pace: Pace,
		opening: Opening,
		buffer: ByteArray,
		optimizer: AudioOptimizer,
		own: SynthesisRouting.Player?,
		toFramework: Boolean,
		run: Run
	) {
		val plain = try {
			DevanagariPronunciation.apply(rawText)
		} catch (_: Throwable) {
			rawText
		}
		val prosody = Prosody.prefix(false)
		val lead = Pacing.DEFAULT_MS.toLong()
		val pieces = try {
			TextPieces.pieceSequence(plain).toList()
		} catch (_: Throwable) {
			emptyList()
		}
		for ((at, piece) in pieces.withIndex()) {
			if (run.stopped) return
			val last = at == pieces.lastIndex
			val audio = try {
				prosody + Pauses.apply(piece.text, s.pauses, last)
			} catch (_: Throwable) {
				prosody + piece.text
			}
			if (EvvEngine.isBlankSpeech(audio)) continue
			if (!target.speak(audio, isStopped = { run.stopped })) return
			val outcome = pump(
				target, callback, pace, opening, buffer, optimizer,
				Stability.ENGINE_SILENCE_MS, lead, own, toFramework, run
			)
			if (outcome != Pump.Next) return
		}
	}

	private fun engineForSegment(
		target: EvvEngine,
		language: Int,
		request: SynthesisRequest,
		s: Settings
	): EvvEngine {
		if (language == target.language) return target
		val other = try {
			synchronized(engineLock) { engineFor(language) }
		} catch (_: Throwable) {
			null
		} ?: return target
		try { other.setAbbreviations(s.abbreviations) } catch (_: Throwable) {}
		try {
			if (!s.forceRate) other.setRatePercent(request.speechRate)
			if (!s.forcePitch) other.setPitchPercent(request.pitch)
		} catch (_: Throwable) {}
		if (s.forceVolume) {
			try { s.shape(s.voice)[Eci.VOICE_VOLUME]?.let { other.setVolume(it) } } catch (_: Throwable) {}
		}
		return other
	}

	private inner class Prefetcher(
		raw: String,
		s: Settings,
		replacements: Substitutions,
		prosody: String,
		private val language: Int
	) {

		private val gate = Object()

		private var ready: String? = null

		private var finished = false

		private var cancelled = false

		private var future: Future<*>? = null

		init {
			val job = try {
				textWork.submit(Runnable { produce(raw, s, replacements, prosody) })
			} catch (_: Throwable) {
				null
			}
			future = job
			if (job == null) {

				synchronized(gate) {
					finished = true
					gate.notifyAll()
				}
			}
		}

		private fun produce(raw: String, s: Settings, replacements: Substitutions, prosody: String) {
			try {

				val pieces: Iterator<TextPieces.Piece> = if (raw.length > STREAM_FROM_CHARS) {
					TextPipeline.pieces(raw, s, replacements.index, replacements.abbreviations, language).iterator()
				} else {
					val text = try {
						TextPipeline.apply(raw, s, replacements.index, replacements.abbreviations, language)
					} catch (_: Throwable) {
						raw
					}
					TextPieces.pieceSequence(text).iterator()
				}
				var held = next(pieces) ?: return
				while (!isCancelled()) {
					val following = next(pieces)
					val last = following == null
					var audio = try {
						prosody + Pauses.apply(held.text, s.pauses, last)
					} catch (_: Throwable) {
						prosody + held.text
					}
					if (held.forced && !last) audio += Pauses.AT_A_BOUNDARY
					if (!EvvEngine.isBlankSpeech(audio)) publish(audio)
					held = following ?: break
				}
			} catch (broken: Throwable) {

				Log.e(TAG, "the text of an utterance could not be prepared", broken)
			} finally {
				finish()
			}
		}

		private fun next(pieces: Iterator<TextPieces.Piece>): TextPieces.Piece? = try {
			if (pieces.hasNext()) pieces.next() else null
		} catch (_: Throwable) {
			null
		}

		fun take(): String? {
			synchronized(gate) {
				while (ready == null && !finished && !cancelled) {
					try {
						gate.wait(WAIT_MS)
					} catch (stopping: InterruptedException) {
						Thread.currentThread().interrupt()
						return null
					}
				}
				val held = ready
				ready = null
				gate.notifyAll()
				return held
			}
		}

		private fun publish(audio: String) {
			synchronized(gate) {
				while (ready != null && !cancelled) {
					try {
						gate.wait(WAIT_MS)
					} catch (stopping: InterruptedException) {
						Thread.currentThread().interrupt()
						return
					}
				}
				if (cancelled) return
				ready = audio
				gate.notifyAll()
			}
		}

		private fun finish() {
			synchronized(gate) {
				finished = true
				gate.notifyAll()
			}
		}

		private fun isCancelled(): Boolean = cancelled || Thread.currentThread().isInterrupted

		fun cancel() {
			synchronized(gate) {
				cancelled = true
				ready = null
				finished = true
				gate.notifyAll()
			}
			try {
				future?.cancel(true)
			} catch (_: Throwable) {
			}
		}
	}

	private fun styledText(request: SynthesisRequest, s: Settings): String {
		return try {
			val sequence = request.charSequenceText ?: return ""
			if (s.readTextStyles) TextStyles.annotate(sequence) else sequence.toString()
		} catch (broken: Throwable) {
			try {
				request.charSequenceText?.toString().orEmpty()
			} catch (failed: Throwable) {
				""
			}
		}
	}

	private fun finish(callback: SynthesisCallback, opening: Opening, run: Run) {

		if (playingItself) {
			playingItself = false
			try { if (!run.stopped) player?.drain() } catch (_: Throwable) {}
		}

		if (opening.began) {
			try {
				callback.done()
			} catch (_: Throwable) {
			}
		} else if (!run.stopped) {
			try {
				if (opening.open()) callback.done() else callback.error(TextToSpeech.ERROR_SYNTHESIS)
			} catch (_: Throwable) {
			}
		}
	}

	private enum class Pump { Next, Over, Gone }

	private fun pump(
		target: EvvEngine,
		callback: SynthesisCallback,
		pace: Pace,
		opening: Opening,
		buffer: ByteArray,
		optimizer: AudioOptimizer,
		silenceMs: Int,
		leadMs: Long,
		own: SynthesisRouting.Player? = null,
		toFramework: Boolean = true,
		run: Run
	): Pump {
		while (!run.stopped) {
			val n = target.read(buffer, silenceMs)
			if (n == TIMED_OUT) {

				Log.e(TAG, "the engine went silent after ${pace.handedMs()}ms of audio")

				target.stop()

				markSuspect(target.language)
				return Pump.Over
			}
			if (n <= 0) return Pump.Next

			optimizer.process(buffer, n)
			if (!opening.open()) {
				target.stop()
				return Pump.Gone
			}
			if (own != null && !own.write(buffer, n)) {

				target.stop()
				return Pump.Over
			}
			if (toFramework && !deliver(callback, buffer, n, run)) {
				target.stop()
				return Pump.Gone
			}
			pace.handed(n)

			if (toFramework && !hold(pace, leadMs, run)) return Pump.Over
		}
		return Pump.Next
	}

	private fun deliver(callback: SynthesisCallback, buffer: ByteArray, n: Int, run: Run): Boolean {
		var tries = 0
		while (!run.stopped) {
			val took = try {
				callback.audioAvailable(buffer, 0, n)
			} catch (_: Throwable) {

				TextToSpeech.ERROR
			}
			if (took == TextToSpeech.SUCCESS) return true
			if (++tries >= DELIVER_TRIES) return false
			if (!waitUpTo(DELIVER_WAIT_MS, run)) return false
		}
		return false
	}

	private fun remember(pace: Pace) {
		if (pace.handedMs() > mostAudioHandedMs) mostAudioHandedMs = pace.handedMs()
	}

	private fun afterAnUtterance() {
		if (engineIsSuspect) {
			engineIsSuspect = false

			synchronized(engineLock) { suspect.add(suspectLanguage) }
			main.removeCallbacks(closeIdle)
			main.post(closeIdle)
		}
		if (Stability.WAKE_LOCK && Stability.WAKE_LOCK_TAIL_MS > 0) {
			try {
				val held = tailLock
				if (held == null) {
					val made = wakeLockManager?.newWakeLock(
						PowerManager.PARTIAL_WAKE_LOCK, "$TAG:wake:tail"
					)?.apply {
						setReferenceCounted(false)

						runCatching { acquire(Stability.WAKE_LOCK_TAIL_MS.toLong()) }
					}
					tailLock = made
				} else {
					runCatching { if (held.isHeld) held.release() }
					runCatching { held.acquire(Stability.WAKE_LOCK_TAIL_MS.toLong()) }
				}
			} catch (_: Throwable) {

			}
		}
		val after = Stability.RELEASE_ENGINE_AFTER_S
		if (after > 0) {
			main.removeCallbacks(closeIdle)
			main.postDelayed(closeIdle, after * 1000L)
		}
	}

	private fun acquireWakeLock(): PowerManager.WakeLock? {
		if (!Stability.WAKE_LOCK) return null
		val held = wakeLock
		if (held != null) {
			try {
				if (held.isHeld) return held
			} catch (_: Throwable) {
			}
		}
		val made = try {
			wakeLockManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG:wake")?.apply {
				setReferenceCounted(false)
				runCatching { acquire(WAKELOCK_TIMEOUT_MS) }
			}
		} catch (_: Throwable) {
			null
		}
		wakeLock = made
		return made
	}

	private fun releaseWakeLock(lock: PowerManager.WakeLock?) {
		try {
			if (lock != null && lock.isHeld) lock.release()
		} catch (_: Throwable) {
		}
	}

	private fun hold(pace: Pace, leadMs: Long, run: Run): Boolean {
		while (!run.stopped) {
			val ahead = pace.ahead() - leadMs
			if (ahead <= 0) return true
			if (!waitUpTo(ahead, run)) return false
		}
		return false
	}

	private fun waitUpTo(ms: Long, run: Run): Boolean {
		if (run.stopped) return false
		if (ms <= 0L) return true
		val deadline = System.nanoTime() + ms * 1_000_000L
		synchronized(gate) {
			while (!run.stopped) {
				val left = (deadline - System.nanoTime()) / 1_000_000L
				if (left <= 0L) return true
				try {
					gate.wait(left)
				} catch (stopping: InterruptedException) {
					Thread.currentThread().interrupt()
					return false
				}
			}
			return false
		}
	}

	private fun bufferFor(callback: SynthesisCallback): ByteArray {
		val want = try {
			callback.maxBufferSize
		} catch (_: Throwable) {
			DEFAULT_CHUNK
		}.coerceIn(MIN_CHUNK, MAX_CHUNK).let { it - (it and 1) }

		val held = chunk
		if (held != null && held.size == want) return held
		return ByteArray(want).also { chunk = it }
	}

	private fun playerFor(route: Int, rateHz: Int, request: SynthesisRequest): SynthesisRouting.Player? {
		val held = player ?: SynthesisRouting.Player().also { player = it }

		val volume = try {
			request.params?.getFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f) ?: 1f
		} catch (_: Throwable) {
			1f
		}
		val ready = try {
			held.begin(rateHz, SynthesisRouting.usageOf(route), volume)
		} catch (_: Throwable) {
			false
		}
		return if (ready) held else null
	}

	private class Opening(private val callback: SynthesisCallback, private val rateHz: Int) {
		var began = false
			private set

		fun open(): Boolean {
			if (began) return true
			began = try {
				callback.start(rateHz, AudioFormat.ENCODING_PCM_16BIT, 1) == TextToSpeech.SUCCESS
			} catch (_: Throwable) {
				false
			}
			return began
		}
	}

	private class Pace(private val bytesPerSecond: Int) {
		private val from = System.nanoTime()
		private var bytes = 0L

		fun handed(more: Int) {
			bytes += more
		}

		fun handedMs(): Long = if (bytesPerSecond <= 0) 0 else bytes * 1000L / bytesPerSecond

		fun ahead(): Long = handedMs() - (System.nanoTime() - from) / 1_000_000L
	}

	internal companion object {

		@Volatile
		var instance: EvvTtsService? = null

		@Volatile
		var mostAudioHandedMs: Long = 0

		const val TAG = "freeeloquencetts"
		const val MIN_CHUNK = 1024
		const val MAX_CHUNK = 65536
		const val DEFAULT_CHUNK = 4096
		const val BYTES_PER_SAMPLE = 2

		const val WAIT_MS = 50L

		const val STREAM_FROM_CHARS = 2000

		const val TIMED_OUT = -2

		const val LEAD_MS = Pacing.DEFAULT_MS.toLong()

		const val SLICE_MS = 1L

		const val DELIVER_TRIES = 2000
		const val DELIVER_WAIT_MS = 1L

	const val WAKELOCK_TIMEOUT_MS = 10 * 60 * 1000L

	const val MAX_PIECE_FAILURES = 3
	}
}
