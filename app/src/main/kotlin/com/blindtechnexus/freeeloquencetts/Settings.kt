package com.blindtechnexus.freeeloquencetts

import android.content.Context
import android.content.SharedPreferences

class Settings(context: Context) {

	private val prefs: SharedPreferences = DirectBoot.storage(context)
		.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

	@Volatile
	var revision: Int = 0
		private set

	private val watcher = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }

	init {
		carryOverTheOldKeys()
		prefs.registerOnSharedPreferenceChangeListener(watcher)
	}

	fun close() {
		prefs.unregisterOnSharedPreferenceChangeListener(watcher)
	}

	var voice: Int
		get() = prefs.getInt(KEY_VOICE, 0).coerceIn(0, Eci.PRESET_NAMES.lastIndex)
		set(value) = prefs.edit().putInt(KEY_VOICE, value).apply()

	var language: Int
		get() = Eci.languageOrDefault(prefs.getInt(KEY_LANGUAGE, Eci.ENGLISH_US))
		set(value) = prefs.edit().putInt(KEY_LANGUAGE, Eci.languageOrDefault(value)).apply()

	var sampleRateHz: Int
		get() = Eci.nearestSampleRate(prefs.getInt(KEY_SAMPLE_RATE, DEFAULT_SAMPLE_RATE))
		set(value) = prefs.edit().putInt(KEY_SAMPLE_RATE, Eci.nearestSampleRate(value)).apply()

	var synthesisRouting: Int
		get() = SynthesisRouting.clamp(prefs.getInt(KEY_SYNTHESIS_ROUTING, SynthesisRouting.AUTOMATIC))
		set(value) = prefs.edit().putInt(KEY_SYNTHESIS_ROUTING, SynthesisRouting.clamp(value)).apply()

	var forceLanguage: Boolean
		get() = prefs.getBoolean(KEY_FORCE_LANGUAGE, false)
		set(value) = prefs.edit().putBoolean(KEY_FORCE_LANGUAGE, value).apply()

	var autoDetectLanguage: Boolean
		get() = prefs.getBoolean(KEY_AUTO_DETECT_LANGUAGE, true)
		set(value) = prefs.edit().putBoolean(KEY_AUTO_DETECT_LANGUAGE, value).apply()

	var detectionMode: Int
		get() = DetectionMode.clamp(prefs.getInt(KEY_DETECTION_MODE, DetectionMode.DEFAULT))
		set(value) = prefs.edit().putInt(KEY_DETECTION_MODE, DetectionMode.clamp(value)).apply()

	var audioOptimization: Int
		get() = AudioOptimization.clamp(prefs.getInt(KEY_AUDIO_OPTIMIZATION, AudioOptimization.BALANCED))
		set(value) = prefs.edit().putInt(KEY_AUDIO_OPTIMIZATION, AudioOptimization.clamp(value)).apply()

	var abbreviations: Boolean
		get() = prefs.getBoolean(KEY_ABBREVIATIONS, false)
		set(value) = prefs.edit().putBoolean(KEY_ABBREVIATIONS, value).apply()

	var readEmoji: Boolean
		get() = prefs.getBoolean(KEY_EMOJI, true)
		set(value) = prefs.edit().putBoolean(KEY_EMOJI, value).apply()

	var numberMode: Int
		get() = Numbers.clamp(prefs.getInt(KEY_NUMBER_MODE, Numbers.AS_IS))
		set(value) = prefs.edit().putInt(KEY_NUMBER_MODE, Numbers.clamp(value)).apply()

	var readOtpDigits: Boolean
		get() = prefs.getBoolean(KEY_READ_OTP_DIGITS, true)
		set(value) = prefs.edit().putBoolean(KEY_READ_OTP_DIGITS, value).apply()

	var stripSsml: Boolean
		get() = prefs.getBoolean(KEY_STRIP_SSML, true)
		set(value) = prefs.edit().putBoolean(KEY_STRIP_SSML, value).apply()

	var readTextStyles: Boolean
		get() = prefs.getBoolean(KEY_READ_TEXT_STYLES, true)
		set(value) = prefs.edit().putBoolean(KEY_READ_TEXT_STYLES, value).apply()

	var capitalMode: Int
		get() = prefs.getInt(KEY_CAPITAL_MODE, CapitalLetters.NONE).coerceIn(CapitalLetters.NONE, CapitalLetters.PITCH)
		set(value) = prefs.edit().putInt(KEY_CAPITAL_MODE, value.coerceIn(CapitalLetters.NONE, CapitalLetters.PITCH)).apply()

	var expandAbbreviations: Boolean
		get() = prefs.getBoolean(KEY_EXPAND_ABBREVIATIONS, true)
		set(value) = prefs.edit().putBoolean(KEY_EXPAND_ABBREVIATIONS, value).apply()

	var codeMode: Boolean
		get() = prefs.getBoolean(KEY_CODE_MODE, false)
		set(value) = prefs.edit().putBoolean(KEY_CODE_MODE, value).apply()

	var codeCamelCase: Boolean
		get() = prefs.getBoolean(KEY_CODE_CAMEL, true)
		set(value) = prefs.edit().putBoolean(KEY_CODE_CAMEL, value).apply()

	var codeSnakeCase: Boolean
		get() = prefs.getBoolean(KEY_CODE_SNAKE, true)
		set(value) = prefs.edit().putBoolean(KEY_CODE_SNAKE, value).apply()

	var codeOperators: Boolean
		get() = prefs.getBoolean(KEY_CODE_OPERATORS, true)
		set(value) = prefs.edit().putBoolean(KEY_CODE_OPERATORS, value).apply()

	var codeBraces: Boolean
		get() = prefs.getBoolean(KEY_CODE_BRACES, true)
		set(value) = prefs.edit().putBoolean(KEY_CODE_BRACES, value).apply()

	var codeIndentation: Boolean
		get() = prefs.getBoolean(KEY_CODE_INDENT, false)
		set(value) = prefs.edit().putBoolean(KEY_CODE_INDENT, value).apply()

	var codePaths: Boolean
		get() = prefs.getBoolean(KEY_CODE_PATHS, true)
		set(value) = prefs.edit().putBoolean(KEY_CODE_PATHS, value).apply()

	var codeUrls: Boolean
		get() = prefs.getBoolean(KEY_CODE_URLS, true)
		set(value) = prefs.edit().putBoolean(KEY_CODE_URLS, value).apply()

	fun codeOptions(): CodeReading.Options =
		if (!codeMode) CodeReading.OFF else CodeReading.Options(
			camelCase = codeCamelCase,
			snakeCase = codeSnakeCase,
			operators = codeOperators,
			braces = codeBraces,
			indentation = codeIndentation,
			paths = codePaths,
			urls = codeUrls
		)

	var spellingMode: Int
		get() = prefs.getInt(KEY_SPELLING_MODE, Spelling.OFF).coerceIn(Spelling.OFF, Spelling.CAPITALS_NATO)
		set(value) = prefs.edit()
			.putInt(KEY_SPELLING_MODE, value.coerceIn(Spelling.OFF, Spelling.CAPITALS_NATO)).apply()

	var persistentNotification: Boolean
		get() = prefs.getBoolean(KEY_PERSISTENT_NOTIFICATION, false)
		set(value) = prefs.edit().putBoolean(KEY_PERSISTENT_NOTIFICATION, value).apply()

	var startOnBoot: Boolean
		get() = prefs.getBoolean(KEY_START_ON_BOOT, false)
		set(value) = prefs.edit().putBoolean(KEY_START_ON_BOOT, value).apply()

	var notificationStopButton: Boolean
		get() = prefs.getBoolean(KEY_NOTIFICATION_STOP, true)
		set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_STOP, value).apply()

	var foregroundWhileSpeaking: Boolean
		get() = prefs.getBoolean(KEY_FOREGROUND_WHILE_SPEAKING, false)
		set(value) = prefs.edit().putBoolean(KEY_FOREGROUND_WHILE_SPEAKING, value).apply()

	var wakeLock: Boolean
		get() = prefs.getBoolean(KEY_WAKE_LOCK, false)
		set(value) = prefs.edit().putBoolean(KEY_WAKE_LOCK, value).apply()

	var wakeLockTailMs: Int
		get() = prefs.getInt(KEY_WAKE_LOCK_TAIL, 0).coerceAtLeast(0)
		set(value) = prefs.edit().putInt(KEY_WAKE_LOCK_TAIL, value.coerceAtLeast(0)).apply()

	var releaseEngineAfterS: Int
		get() = prefs.getInt(KEY_RELEASE_ENGINE, 0).coerceAtLeast(0)
		set(value) = prefs.edit().putInt(KEY_RELEASE_ENGINE, value.coerceAtLeast(0)).apply()

	var speechLeadMs: Int
		get() = prefs.getInt(KEY_SPEECH_LEAD, Pacing.DEFAULT_MS).coerceIn(Pacing.MIN_MS, Pacing.MAX_MS)
		set(value) = prefs.edit().putInt(KEY_SPEECH_LEAD, value.coerceIn(Pacing.MIN_MS, Pacing.MAX_MS)).apply()

	var runAheadOnLongText: Boolean
		get() = prefs.getBoolean(KEY_RUN_AHEAD, true)
		set(value) = prefs.edit().putBoolean(KEY_RUN_AHEAD, value).apply()

	var engineSilenceMs: Int
		get() = prefs.getInt(KEY_ENGINE_SILENCE, DEFAULT_SILENCE_MS).coerceIn(MIN_SILENCE_MS, MAX_SILENCE_MS)
		set(value) = prefs.edit()
			.putInt(KEY_ENGINE_SILENCE, value.coerceIn(MIN_SILENCE_MS, MAX_SILENCE_MS)).apply()

	var trackActions: Boolean
		get() = prefs.getBoolean(KEY_TRACK_ACTIONS, false)
		set(value) = prefs.edit().putBoolean(KEY_TRACK_ACTIONS, value).apply()

	var pauses: Int
		get() = prefs.getInt(KEY_PAUSES, Pauses.ALL).coerceIn(Pauses.KEEP, Pauses.ALL)
		set(value) = prefs.edit().putInt(KEY_PAUSES, value).apply()

	var phrasePrediction: Boolean
		get() = prefs.getBoolean(KEY_PHRASE_PREDICTION, false)
		set(value) = prefs.edit().putBoolean(KEY_PHRASE_PREDICTION, value).apply()

	var symbolMode: Int
		get() = prefs.getInt(KEY_SYMBOL_MODE, Symbols.SOME).coerceIn(Symbols.NONE, Symbols.CUSTOM)
		set(value) = prefs.edit().putInt(KEY_SYMBOL_MODE, value.coerceIn(Symbols.NONE, Symbols.CUSTOM)).apply()

	var customSymbols: String
		get() = prefs.getString(KEY_CUSTOM_SYMBOLS, "") ?: ""
		set(value) = prefs.edit().putString(KEY_CUSTOM_SYMBOLS, value).apply()

	var forceRate: Boolean
		get() = prefs.getBoolean(KEY_FORCE_RATE, false)
		set(value) = prefs.edit().putBoolean(KEY_FORCE_RATE, value).apply()

	var forcePitch: Boolean
		get() = prefs.getBoolean(KEY_FORCE_PITCH, false)
		set(value) = prefs.edit().putBoolean(KEY_FORCE_PITCH, value).apply()

	var forceVolume: Boolean
		get() = prefs.getBoolean(KEY_FORCE_VOLUME, false)
		set(value) = prefs.edit().putBoolean(KEY_FORCE_VOLUME, value).apply()

	var speed: Int
		get() = Eci.clampVoice(Eci.VOICE_SPEED, prefs.getInt(KEY_SPEED, Eci.DEFAULT_SPEED))
		set(value) = prefs.edit().putInt(KEY_SPEED, Eci.clampVoice(Eci.VOICE_SPEED, value)).apply()

	fun shapeValue(voice: Int, param: Int): Int? {
		if (param == Eci.VOICE_SPEED) return speed
		val key = keyOf(voice, param)
		return if (prefs.contains(key)) Eci.clampVoice(param, prefs.getInt(key, 0)) else null
	}

	fun setShapeValue(voice: Int, param: Int, value: Int) {
		if (param == Eci.VOICE_SPEED) {
			speed = value
			return
		}
		prefs.edit().putInt(keyOf(voice, param), Eci.clampVoice(param, value)).apply()
	}

	fun shape(voice: Int): Map<Int, Int> {
		val out = LinkedHashMap<Int, Int>()
		for (param in PER_VOICE) {
			val key = keyOf(voice, param)
			if (prefs.contains(key)) out[param] = Eci.clampVoice(param, prefs.getInt(key, 0))
		}
		out[Eci.VOICE_SPEED] = speed
		return out
	}

	fun writeShape(voice: Int, values: Map<Int, Int>) {
		val edit = prefs.edit()
		for (param in PER_VOICE) values[param]?.let { edit.putInt(keyOf(voice, param), it) }
		edit.apply()
		values[Eci.VOICE_SPEED]?.let { speed = it }
	}

	fun shapeIsCustom(voice: Int): Boolean = PER_VOICE.any { prefs.contains(keyOf(voice, it)) }

	fun clearShape(voice: Int) {
		val edit = prefs.edit()
		for (param in PER_VOICE) edit.remove(keyOf(voice, param))
		edit.apply()
	}

	private fun keyOf(voice: Int, param: Int) = "voice${voice}_param$param"

	private fun carryOverTheOldKeys() {
		val old = Eci.NUM_VOICE_PARAMS.let { 0 until it }.map { "voice_param_$it" }
		if (old.none { prefs.contains(it) }) return
		val edit = prefs.edit()
		val was = prefs.getInt(KEY_VOICE, 0).coerceIn(0, Eci.PRESET_NAMES.lastIndex)
		for (param in 0 until Eci.NUM_VOICE_PARAMS) {
			val from = "voice_param_$param"
			if (!prefs.contains(from)) continue
			val value = prefs.getInt(from, 0)
			if (param == Eci.VOICE_SPEED) {
				edit.putInt(KEY_SPEED, Eci.clampVoice(param, value))
			} else {
				edit.putInt(keyOf(was, param), Eci.clampVoice(param, value))
			}
			edit.remove(from)
		}
		edit.apply()
	}

	companion object {
		const val KEY_VOICE = "voice_preset"
		const val KEY_LANGUAGE = "language_dialect"
		const val KEY_FORCE_LANGUAGE = "force_language"
		const val KEY_SYNTHESIS_ROUTING = "synthesis_routing"
		const val KEY_AUTO_DETECT_LANGUAGE = "auto_detect_language"
		const val KEY_DETECTION_MODE = "detection_mode"
		const val KEY_READ_OTP_DIGITS = "read_otp_digits"
		const val KEY_SAMPLE_RATE = "sample_rate_hz"
		const val KEY_AUDIO_OPTIMIZATION = "audio_optimization"
		const val KEY_ABBREVIATIONS = "abbreviations"
		const val KEY_SPEED = "speed"
		const val KEY_PAUSES = "pauses"
		const val KEY_PHRASE_PREDICTION = "phrase_prediction"
		const val KEY_SYMBOL_MODE = "symbol_mode"
		const val KEY_CUSTOM_SYMBOLS = "custom_symbols"
		const val KEY_FORCE_RATE = "force_rate"
		const val KEY_FORCE_PITCH = "force_pitch"
		const val KEY_FORCE_VOLUME = "force_volume"
		const val KEY_EMOJI = "emoji"
		const val KEY_NUMBER_MODE = "number_mode"
		const val KEY_STRIP_SSML = "strip_ssml"
		const val KEY_READ_TEXT_STYLES = "read_text_styles"
		const val KEY_CAPITAL_MODE = "capital_mode"
		const val KEY_EXPAND_ABBREVIATIONS = "expand_abbreviations"
		const val KEY_CODE_MODE = "code_mode"
		const val KEY_CODE_CAMEL = "code_camel_case"
		const val KEY_CODE_SNAKE = "code_snake_case"
		const val KEY_CODE_OPERATORS = "code_operators"
		const val KEY_CODE_BRACES = "code_braces"
		const val KEY_CODE_INDENT = "code_indentation"
		const val KEY_CODE_PATHS = "code_paths"
		const val KEY_CODE_URLS = "code_urls"
		const val KEY_SPELLING_MODE = "spelling_mode"
		const val KEY_PERSISTENT_NOTIFICATION = "persistent_notification"
		const val KEY_WAKE_LOCK = "wake_lock"
		const val KEY_TRACK_ACTIONS = "track_actions"
		const val KEY_START_ON_BOOT = "start_on_boot"
		const val KEY_NOTIFICATION_STOP = "notification_stop"
		const val KEY_FOREGROUND_WHILE_SPEAKING = "foreground_while_speaking"
		const val KEY_WAKE_LOCK_TAIL = "wake_lock_tail_ms"
		const val KEY_RELEASE_ENGINE = "release_engine_after_s"
		const val KEY_SPEECH_LEAD = "speech_lead_ms"
		const val KEY_RUN_AHEAD = "run_ahead_on_long_text"
		const val KEY_ENGINE_SILENCE = "engine_silence_ms"
		const val DEFAULT_SAMPLE_RATE = 11025

		const val DEFAULT_SILENCE_MS = 15000
		const val MIN_SILENCE_MS = 1000
		const val MAX_SILENCE_MS = 300000

		fun store(context: Context): SharedPreferences = DirectBoot.storage(context)
			.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

		fun flag(context: Context, key: String, default: Boolean): Boolean =
			store(context).getBoolean(key, default)

		fun number(context: Context, key: String, default: Int): Int =
			store(context).getInt(key, default)

		val SHAPE = listOf(
			Eci.VOICE_GENDER,
			Eci.VOICE_SPEED,
			Eci.VOICE_PITCH_BASELINE,
			Eci.VOICE_PITCH_FLUCTUATION,
			Eci.VOICE_HEAD_SIZE,
			Eci.VOICE_ROUGHNESS,
			Eci.VOICE_BREATHINESS,
			Eci.VOICE_VOLUME
		)

		val PER_VOICE = SHAPE.filter { it != Eci.VOICE_SPEED }

		val SLIDERS = SHAPE.filter { it != Eci.VOICE_GENDER }

		val BEHAVIOURS = listOf(
			Eci.VOICE_SPEED,
			Eci.VOICE_PITCH_BASELINE,
			Eci.VOICE_PITCH_FLUCTUATION,
			Eci.VOICE_BREATHINESS,
			Eci.VOICE_HEAD_SIZE,
			Eci.VOICE_ROUGHNESS,
			Eci.VOICE_VOLUME
		)
	}
}
