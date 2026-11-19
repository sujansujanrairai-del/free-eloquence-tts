package com.blindtechnexus.freeeloquencetts

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SettingsModel(context: Context) {

	private val app = context.applicationContext
	private val settings = Settings(app)

	private val preview: EvvPreview = EvvPreview()

	var language by mutableStateOf(settings.language)
		private set

	val languageIndex: Int
		get() = Eci.LANGUAGES.indexOfFirst { it.code == language }.coerceAtLeast(0)

	var voice by mutableStateOf(settings.voice)
		private set

	var expandAbbreviations by mutableStateOf(settings.expandAbbreviations)
		private set

	var codeMode by mutableStateOf(settings.codeMode)
		private set

	var codeCamelCase by mutableStateOf(settings.codeCamelCase)
		private set

	var codeSnakeCase by mutableStateOf(settings.codeSnakeCase)
		private set

	var codeOperators by mutableStateOf(settings.codeOperators)
		private set

	var codeBraces by mutableStateOf(settings.codeBraces)
		private set

	var codeIndentation by mutableStateOf(settings.codeIndentation)
		private set

	var codePaths by mutableStateOf(settings.codePaths)
		private set

	var codeUrls by mutableStateOf(settings.codeUrls)
		private set

	var spellingMode by mutableStateOf(settings.spellingMode)
		private set

	var abbreviations by mutableStateOf(settings.abbreviations)
		private set

	var sampleRateHz by mutableStateOf(settings.sampleRateHz)
		private set

	var audioOptimization by mutableStateOf(settings.audioOptimization)
		private set

	var pauses by mutableStateOf(settings.pauses)
		private set

	var phrasePrediction by mutableStateOf(settings.phrasePrediction)
		private set

	var symbolMode by mutableStateOf(settings.symbolMode)
		private set

	var customSymbols by mutableStateOf(settings.customSymbols)
		private set

	var readEmoji by mutableStateOf(settings.readEmoji)
		private set

	var numberMode by mutableStateOf(settings.numberMode)
		private set

	var readOtpDigits by mutableStateOf(settings.readOtpDigits)
		private set

	var forceLanguage by mutableStateOf(settings.forceLanguage)
		private set

	var autoDetectLanguage by mutableStateOf(settings.autoDetectLanguage)
		private set

	var detectionMode by mutableStateOf(settings.detectionMode)
		private set

	var synthesisRouting by mutableStateOf(settings.synthesisRouting)
		private set

	var stripSsml by mutableStateOf(settings.stripSsml)
		private set

	var readTextStyles by mutableStateOf(settings.readTextStyles)
		private set

	var capitalMode by mutableStateOf(settings.capitalMode)
		private set

	var forceRate by mutableStateOf(settings.forceRate)
		private set

	var forcePitch by mutableStateOf(settings.forcePitch)
		private set

	var forceVolume by mutableStateOf(settings.forceVolume)
		private set

	private val shape = mutableStateMapOf<Int, Int>().apply { putAll(settings.shape(settings.voice)) }

	val gender: Int get() = shape[Eci.VOICE_GENDER] ?: Eci.defaultGender(voice)

	init {
		openPreview()
	}

	private fun openPreview() {
		preview.open(language) {

			if (!settings.shapeIsCustom(settings.voice)) adoptVoice()
			shape.putAll(settings.shape(settings.voice))
		}
	}

	fun stopSpeaking() {
		preview.stopSpeaking()
	}

	fun close() {
		preview.close()
		settings.close()
	}

	fun percentOf(param: Int): Int = Eci.toPercent(param, shape[param] ?: 0)

	fun chooseVoice(which: Int) {
		voice = which
		settings.voice = which

		if (!settings.shapeIsCustom(which)) adoptVoice()
		shape.clear()
		shape.putAll(settings.shape(which))
	}

	fun chooseLanguage(code: Int) {
		val settled = Eci.languageOrDefault(code)
		settings.language = settled
		language = settled
		openPreview()
	}

	fun setPercent(param: Int, percent: Int) = setShape(param, Eci.fromPercent(param, percent))

	fun setShape(param: Int, value: Int) {
		val settled = Eci.clampVoice(param, value)
		if (shape[param] == settled) return
		shape[param] = settled
		settings.setShapeValue(voice, param, settled)
	}

	fun resetVoice() {
		settings.clearShape(voice)
		adoptVoice()
		shape.clear()
		shape.putAll(settings.shape(voice))
	}

	fun chooseAbbreviations(on: Boolean) {
		abbreviations = on
		settings.abbreviations = on
	}

	fun chooseExpandAbbreviations(on: Boolean) {
		expandAbbreviations = on
		settings.expandAbbreviations = on
	}

	fun chooseCodeMode(on: Boolean) {
		codeMode = on
		settings.codeMode = on
	}

	fun chooseCodeCamelCase(on: Boolean) {
		codeCamelCase = on
		settings.codeCamelCase = on
	}

	fun chooseCodeSnakeCase(on: Boolean) {
		codeSnakeCase = on
		settings.codeSnakeCase = on
	}

	fun chooseCodeOperators(on: Boolean) {
		codeOperators = on
		settings.codeOperators = on
	}

	fun chooseCodeBraces(on: Boolean) {
		codeBraces = on
		settings.codeBraces = on
	}

	fun chooseCodeIndentation(on: Boolean) {
		codeIndentation = on
		settings.codeIndentation = on
	}

	fun chooseCodePaths(on: Boolean) {
		codePaths = on
		settings.codePaths = on
	}

	fun chooseCodeUrls(on: Boolean) {
		codeUrls = on
		settings.codeUrls = on
	}

	fun chooseSpellingMode(mode: Int) {
		spellingMode = mode
		settings.spellingMode = mode
	}

	fun chooseReadEmoji(on: Boolean) {
		readEmoji = on
		settings.readEmoji = on
	}

	fun chooseNumberMode(mode: Int) {
		numberMode = mode
		settings.numberMode = mode
	}

	fun chooseReadOtpDigits(on: Boolean) {
		readOtpDigits = on
		settings.readOtpDigits = on
	}

	fun chooseForceLanguage(on: Boolean) {
		forceLanguage = on
		settings.forceLanguage = on
	}

	fun chooseAutoDetectLanguage(on: Boolean) {
		autoDetectLanguage = on
		settings.autoDetectLanguage = on
	}

	fun chooseDetectionMode(mode: Int) {
		val settled = DetectionMode.clamp(mode)
		detectionMode = settled
		settings.detectionMode = settled
	}

	fun chooseSynthesisRouting(route: Int) {
		val settled = SynthesisRouting.clamp(route)
		synthesisRouting = settled
		settings.synthesisRouting = settled
	}

	fun chooseStripSsml(on: Boolean) {
		stripSsml = on
		settings.stripSsml = on
	}

	fun chooseReadTextStyles(on: Boolean) {
		readTextStyles = on
		settings.readTextStyles = on
	}

	fun chooseCapitalMode(mode: Int) {
		capitalMode = mode
		settings.capitalMode = mode
	}

	fun choosePhrasePrediction(on: Boolean) {
		phrasePrediction = on
		settings.phrasePrediction = on
	}

	fun chooseSymbolMode(mode: Int) {
		symbolMode = mode
		settings.symbolMode = mode
	}

	fun chooseCustomSymbols(list: String) {
		customSymbols = list
		settings.customSymbols = list
	}

	fun chooseForceRate(on: Boolean) {
		forceRate = on
		settings.forceRate = on
	}

	fun chooseForcePitch(on: Boolean) {
		forcePitch = on
		settings.forcePitch = on
	}

	fun chooseForceVolume(on: Boolean) {
		forceVolume = on
		settings.forceVolume = on
	}

	fun choosePauses(mode: Int) {
		pauses = mode
		settings.pauses = mode
	}

	fun chooseSampleRate(hz: Int) {
		val settled = Eci.nearestSampleRate(hz)
		sampleRateHz = settled
		settings.sampleRateHz = settled
	}

	fun chooseAudioOptimization(mode: Int) {
		audioOptimization = mode
		settings.audioOptimization = mode
	}

	private fun previewTextOf(code: Int): Int = when (code) {
		Eci.GERMAN -> R.string.preview_text_de
		Eci.SPANISH_SPAIN, Eci.SPANISH_MEXICO -> R.string.preview_text_es
		Eci.FRENCH_FRANCE, Eci.FRENCH_CANADA -> R.string.preview_text_fr
		Eci.ITALIAN -> R.string.preview_text_it
		else -> R.string.preview_text
	}

	fun say() {

		val dictionary = runCatching { UserDictionary.snapshot(app) }.getOrNull()
		val text = TextPipeline.apply(
			app.getString(previewTextOf(language)),
			settings,
			dictionary?.index ?: WordIndex.EMPTY,
			Abbreviations.entries(app),
			language
		)
		val said = Prosody.prefix(settings.phrasePrediction) +
			Pauses.apply(text, settings.pauses, true)
		preview.say(said, voice, settings.shape(voice), sampleRateHz, settings.abbreviations, settings.audioOptimization)
	}

	private fun adoptVoice() {
		val own = preview.presetShape(settings.voice)
		if (own.isEmpty()) return

		settings.writeShape(settings.voice, own - Eci.VOICE_SPEED)
	}

	companion object {
		val RATES: List<Int> = Eci.SAMPLE_RATES.sorted()

	}
}
