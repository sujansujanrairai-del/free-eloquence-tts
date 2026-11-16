package com.blindtechnexus.freeeloquencetts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The settings screen, in the order somebody would change them.
 *
 * The screen used to be a list of every setting the engine has, in the order the
 * engine numbers them, with a sentence under each. That is the right shape for a
 * reference and the wrong shape for a person: the eight things most people ever
 * change were mixed in with the twenty they never do, and the way to change a
 * language was to find it among them.
 *
 * So it is two layers now. The top is what changes the voice you hear: the
 * language, the voice, the gender, whether the language is detected, and how
 * numbers and symbols are read. Everything else is behind one button directly
 * under the gender, and that button is a heading that a screen reader announces
 * as "collapsed" or "expanded" before it says anything else.
 *
 * Hindi and Nepali have no switch. Devanagari is respelled into sounds this
 * voice can say on every utterance, because that is what makes it intelligible
 * at all; a setting that could turn it off was a setting whose off position
 * made the text unreadable.
 *
 * Three things about it are deliberate and easy to undo by accident.
 *
 * Every description is three or four words. A setting whose explanation runs to
 * three lines is a setting nobody listens to before changing it, and the
 * sentence is there to be heard once, not to be studied. What each setting does
 * at length is in the README and in the code that implements it.
 *
 * A control is one stop. A dropdown is a button whose label carries the setting
 * and its value ("Language: English (United States)"), and the list it opens is
 * radio buttons with the chosen one marked. A switch says its name and whether
 * it is on, and the row it is on is the switch, so a finger that misses the
 * thumb does not miss the row.
 *
 * The sections are headings. TalkBack moves from heading to heading, so a
 * screen with four sections is four jumps rather than forty swipes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	state: SettingsModel,
	onBack: () -> Unit,
	onOpenDictionary: () -> Unit,
	onOpenAbbreviations: () -> Unit,
	onOpenBattery: () -> Unit
) {
	val context = LocalContext.current
	var notice by remember { mutableStateOf("") }
	Scaffold(
		topBar = {
			ScreenTopBar(
				title = stringResource(R.string.settings_title),
				onBack = onBack
			)
		}
	) { inset ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(inset)
				.verticalScroll(rememberScrollState())
				.padding(bottom = 24.dp)
		) {
			LiveNotice(text = notice, horizontal = 16.dp)
			VoiceSection(
				state = state,
				advanced = {
					VoiceStyleSection(state)
					QualitySection(state)
					CodeReadingSection(state)
					OtherSection(
						state = state,
						onReset = {
							state.resetVoice()
							notice = context.getString(
								R.string.voice_reset,
								Eci.PRESET_NAMES.getOrElse(state.voice) { "" }
							)
						}
					)
				}
			)
			ReadingSection(state)
			DictionarySection(
				onOpen = onOpenDictionary,
				onOpenAbbreviations = onOpenAbbreviations,
				state = state
			)
			BatterySection(onOpen = onOpenBattery)
			TestSection(state)
		}
	}
}

// ---- the first layer: what the voice is --------------------------------

/**
 * The language, the voice and the gender, then the advanced settings, and then
 * the two switches that decide whether the language is taken from the text.
 *
 * The advanced panel sits directly under gender because that is where the
 * voice stops being a choice of three lists and starts being a set of numbers:
 * somebody who has just picked who is speaking is exactly the person who wants
 * to tune how they sound, and the panel is one press away instead of six
 * sections down.
 *
 * The detection mode is under the detection switch and only while it is on: a
 * choice about how detection works is a choice nobody has while detection is
 * off, and a greyed-out control that says so is a stop a reader has to go past
 * to learn nothing.
 */
@Composable
private fun VoiceSection(state: SettingsModel, advanced: @Composable () -> Unit) {
	SectionHeading(text = stringResource(R.string.section_voice))
	SectionText(text = stringResource(R.string.section_voice_short_description))
	val languages = Eci.LANGUAGES.map { it.label }
	DropdownSetting(
		label = stringResource(R.string.language_label),
		value = languages.getOrElse(state.languageIndex) { languages[0] },
		options = languages,
		onChoose = { at ->
			state.chooseLanguage(Eci.LANGUAGES.getOrElse(at) { Eci.LANGUAGES[0] }.code)
		},
		description = stringResource(R.string.language_short_description)
	)
	val voices = Eci.PRESET_NAMES.toList()
	DropdownSetting(
		label = stringResource(R.string.voice_label),
		value = voices.getOrElse(state.voice) { "" },
		options = voices,
		onChoose = state::chooseVoice,
		description = stringResource(R.string.voice_short_description)
	)
	val genders = listOf(
		stringResource(R.string.gender_male),
		stringResource(R.string.gender_female)
	)
	DropdownSetting(
		label = stringResource(R.string.gender_label),
		value = genders.getOrElse(state.gender) { "" },
		options = genders,
		onChoose = { state.setShape(Eci.VOICE_GENDER, it) },
		description = stringResource(R.string.gender_short_description)
	)
	CollapsibleSection(
		title = stringResource(R.string.advanced_title),
		description = stringResource(R.string.advanced_description),
		content = advanced
	)
	SwitchRow(
		label = stringResource(R.string.force_language_label),
		description = stringResource(R.string.force_language_short_description),
		checked = state.forceLanguage,
		onChange = state::chooseForceLanguage
	)
	SwitchRow(
		label = stringResource(R.string.detect_language_label),
		description = stringResource(R.string.detect_language_short_description),
		checked = state.autoDetectLanguage,
		onChange = state::chooseAutoDetectLanguage
	)
	DetectionModeSetting(
		mode = state.detectionMode,
		onChoose = state::chooseDetectionMode,
		enabled = state.autoDetectLanguage
	)
}

/** How the words between the words are read. */
@Composable
private fun ReadingSection(state: SettingsModel) {
	SectionHeading(text = stringResource(R.string.section_text))
	SectionText(text = stringResource(R.string.section_reading_short_description))
	val numberModes = Numbers.MODES.map { stringResource(numberModeNameOf(it)) }
	DropdownSetting(
		label = stringResource(R.string.number_mode_label),
		value = stringResource(numberModeNameOf(state.numberMode)),
		options = numberModes,
		onChoose = { state.chooseNumberMode(Numbers.MODES.getOrElse(it) { Numbers.AS_IS }) },
		description = stringResource(R.string.numbers_short_description)
	)
	val spellingModes = Spelling.MODES.map { stringResource(spellingNameOf(it)) }
	DropdownSetting(
		label = stringResource(R.string.spelling_label),
		value = stringResource(spellingNameOf(state.spellingMode)),
		options = spellingModes,
		onChoose = { state.chooseSpellingMode(Spelling.MODES.getOrElse(it) { Spelling.OFF }) },
		description = stringResource(R.string.spelling_short_description)
	)
	val capitalModes = CapitalLetters.MODES.map { stringResource(capitalNameOf(it)) }
	DropdownSetting(
		label = stringResource(R.string.capital_label),
		value = stringResource(capitalNameOf(state.capitalMode)),
		options = capitalModes,
		onChoose = { state.chooseCapitalMode(CapitalLetters.MODES.getOrElse(it) { CapitalLetters.NONE }) },
		description = stringResource(R.string.capitals_short_description)
	)
	SwitchRow(
		label = stringResource(R.string.emoji_label),
		description = stringResource(R.string.emoji_short_description),
		checked = state.readEmoji,
		onChange = state::chooseReadEmoji
	)
	SwitchRow(
		label = stringResource(R.string.otp_label),
		description = stringResource(R.string.otp_short_description),
		checked = state.readOtpDigits,
		onChange = state::chooseReadOtpDigits
	)
	SwitchRow(
		label = stringResource(R.string.text_styles_label),
		description = stringResource(R.string.text_styles_short_description),
		checked = state.readTextStyles,
		onChange = state::chooseReadTextStyles
	)
	SwitchRow(
		label = stringResource(R.string.ssml_label),
		description = stringResource(R.string.ssml_short_description),
		checked = state.stripSsml,
		onChange = state::chooseStripSsml
	)
}

/** The two lists, each with its screen of its own. */
@Composable
private fun DictionarySection(
	state: SettingsModel,
	onOpen: () -> Unit,
	onOpenAbbreviations: () -> Unit
) {
	SectionHeading(text = stringResource(R.string.section_dictionaries))
	SectionText(text = stringResource(R.string.dictionaries_short_description))
	ActionRow(
		label = stringResource(R.string.dictionary_manager),
		description = stringResource(R.string.dictionary_manager_short_description),
		onClick = onOpen
	)
	SwitchRow(
		label = stringResource(R.string.abbreviations_label),
		description = stringResource(R.string.abbreviations_short_description),
		checked = state.abbreviations,
		onChange = state::chooseAbbreviations
	)
	ActionRow(
		label = stringResource(R.string.expansions_manager),
		description = stringResource(R.string.expansions_manager_short_description),
		onClick = onOpenAbbreviations
	)
	SwitchRow(
		label = stringResource(R.string.expand_abbreviations_label),
		description = stringResource(R.string.expand_abbreviations_short_description),
		checked = state.expandAbbreviations,
		onChange = state::chooseExpandAbbreviations
	)
}

// ---- the second layer: everything else ---------------------------------

/** The seven numbers that make a voice a voice, each in a dialog of its own. */
@Composable
private fun VoiceStyleSection(state: SettingsModel) {
	SectionHeading(text = stringResource(R.string.section_behaviour))
	SectionText(text = stringResource(R.string.section_behaviour_short_description))
	for (param in Settings.BEHAVIOURS) {
		ParamButton(
			label = stringResource(setActionOf(param)),
			title = stringResource(dialogTitleOf(param)),
			description = stringResource(descriptionOf(param)),
			percent = state.percentOf(param),
			onPercent = { state.setPercent(param, it) }
		)
		when (param) {
			Eci.VOICE_SPEED -> SwitchRow(
				label = stringResource(R.string.force_over),
				description = stringResource(R.string.force_rate_short_description),
				checked = state.forceRate,
				onChange = state::chooseForceRate
			)
			Eci.VOICE_PITCH_BASELINE -> SwitchRow(
				label = stringResource(R.string.force_over),
				description = stringResource(R.string.force_pitch_short_description),
				checked = state.forcePitch,
				onChange = state::chooseForcePitch
			)
			Eci.VOICE_VOLUME -> SwitchRow(
				label = stringResource(R.string.force_over),
				description = stringResource(R.string.force_volume_short_description),
				checked = state.forceVolume,
				onChange = state::chooseForceVolume
			)
		}
	}
}

/** How the samples are finished, how fast they are made and where they go. */
@Composable
private fun QualitySection(state: SettingsModel) {
	SectionHeading(text = stringResource(R.string.section_quality))
	SectionText(text = stringResource(R.string.section_quality_short_description))
	val optimizations = AudioOptimization.MODES.map { stringResource(optimizationNameOf(it)) }
	DropdownSetting(
		label = stringResource(R.string.audio_optimization_label),
		value = stringResource(optimizationNameOf(state.audioOptimization)),
		options = optimizations,
		onChoose = { state.chooseAudioOptimization(AudioOptimization.MODES.getOrElse(it) { AudioOptimization.BALANCED }) },
		description = stringResource(R.string.optimization_short_description)
	)
	val rates = SettingsModel.RATES.map { stringResource(R.string.hertz, it) }
	val chosenRate = SettingsModel.RATES.indexOf(state.sampleRateHz).coerceAtLeast(0)
	DropdownSetting(
		label = stringResource(R.string.sample_rate_label),
		value = rates.getOrElse(chosenRate) { "" },
		options = rates,
		onChoose = { state.chooseSampleRate(SettingsModel.RATES.getOrElse(it) { Eci.SAMPLE_RATES[1] }) },
		description = stringResource(R.string.sample_rate_short_description)
	)
	val routes = SynthesisRouting.CHOICES.map { stringResource(routingNameOf(it)) }
	DropdownSetting(
		label = stringResource(R.string.routing_label),
		value = stringResource(routingNameOf(state.synthesisRouting)),
		options = routes,
		onChoose = { state.chooseSynthesisRouting(SynthesisRouting.CHOICES.getOrElse(it) { SynthesisRouting.AUTOMATIC }) },
		description = stringResource(R.string.routing_short_description)
	)
	SectionText(text = stringResource(routingDescriptionOf(state.synthesisRouting)))
}

/** Source code, said as code: a master switch and the seven things it does. */
@Composable
private fun CodeReadingSection(state: SettingsModel) {
	SectionHeading(text = stringResource(R.string.section_code))
	SectionText(text = stringResource(R.string.section_code_short_description))
	SwitchRow(
		label = stringResource(R.string.code_mode_label),
		description = stringResource(R.string.code_mode_short_description),
		checked = state.codeMode,
		onChange = state::chooseCodeMode
	)
	SwitchRow(
		label = stringResource(R.string.code_camel_label),
		description = stringResource(R.string.code_camel_short_description),
		checked = state.codeCamelCase,
		onChange = state::chooseCodeCamelCase
	)
	SwitchRow(
		label = stringResource(R.string.code_snake_label),
		description = stringResource(R.string.code_snake_short_description),
		checked = state.codeSnakeCase,
		onChange = state::chooseCodeSnakeCase
	)
	SwitchRow(
		label = stringResource(R.string.code_operators_label),
		description = stringResource(R.string.code_operators_short_description),
		checked = state.codeOperators,
		onChange = state::chooseCodeOperators
	)
	SwitchRow(
		label = stringResource(R.string.code_braces_label),
		description = stringResource(R.string.code_braces_short_description),
		checked = state.codeBraces,
		onChange = state::chooseCodeBraces
	)
	SwitchRow(
		label = stringResource(R.string.code_indent_label),
		description = stringResource(R.string.code_indent_short_description),
		checked = state.codeIndentation,
		onChange = state::chooseCodeIndentation
	)
	SwitchRow(
		label = stringResource(R.string.code_paths_label),
		description = stringResource(R.string.code_paths_short_description),
		checked = state.codePaths,
		onChange = state::chooseCodePaths
	)
	SwitchRow(
		label = stringResource(R.string.code_urls_label),
		description = stringResource(R.string.code_urls_short_description),
		checked = state.codeUrls,
		onChange = state::chooseCodeUrls
	)
}

/** The gaps, the symbols, the phrase pauses, and putting the voice back. */
@Composable
private fun OtherSection(state: SettingsModel, onReset: () -> Unit) {
	SectionHeading(text = stringResource(R.string.section_other))
	SectionText(text = stringResource(R.string.section_other_short_description))
	val pauseModes = listOf(
		stringResource(R.string.pauses_keep),
		stringResource(R.string.pauses_end),
		stringResource(R.string.pauses_all)
	)
	DropdownSetting(
		label = stringResource(R.string.pauses_label),
		value = pauseModes.getOrElse(state.pauses) { "" },
		options = pauseModes,
		onChoose = state::choosePauses,
		description = stringResource(R.string.pauses_short_description)
	)
	val symbolModes = Symbols.MODES.map { stringResource(nameOf(it)) }
	DropdownSetting(
		label = stringResource(R.string.symbol_mode_label),
		value = stringResource(nameOf(state.symbolMode)),
		options = symbolModes,
		onChoose = { state.chooseSymbolMode(Symbols.MODES.getOrElse(it) { Symbols.SOME }) },
		description = stringResource(R.string.symbols_short_description)
	)
	SwitchRow(
		label = stringResource(R.string.intonation_pauses_label),
		description = stringResource(R.string.intonation_short_description),
		checked = state.phrasePrediction,
		onChange = state::choosePhrasePrediction
	)
	ActionRow(
		label = stringResource(R.string.reset_label),
		description = stringResource(R.string.reset_short_description),
		onClick = onReset
	)
}

/** Where the keep-alive lives, behind a button because it is a screen of its
 *  own. */
@Composable
private fun BatterySection(onOpen: () -> Unit) {
	SectionHeading(text = stringResource(R.string.section_battery))
	ActionRow(
		label = stringResource(R.string.battery_open),
		description = stringResource(R.string.battery_short_description),
		onClick = onOpen
	)
}

/** Hearing it. Nothing on this screen speaks by itself, so this is the only
 *  way to know what a change did. */
@Composable
private fun TestSection(state: SettingsModel) {
	SectionHeading(text = stringResource(R.string.section_test))
	SectionText(text = stringResource(R.string.test_short_description))
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp)
	) {
		Button(onClick = state::say, modifier = Modifier.fillMaxWidth()) {
			Text(
				text = stringResource(R.string.speak_label),
				style = MaterialTheme.typography.labelLarge
			)
		}
		Gap(12.dp)
		// Worth its own button rather than being left to the screen reader
		// interrupting itself: this preview speaks into an AudioTrack of its
		// own, which nothing else stops.
		OutlinedButton(onClick = state::stopSpeaking, modifier = Modifier.fillMaxWidth()) {
			Text(
				text = stringResource(R.string.stop_label),
				style = MaterialTheme.typography.labelLarge
			)
		}
	}
}

// ---- which word goes with which setting --------------------------------

private fun setActionOf(param: Int): Int = when (param) {
	Eci.VOICE_SPEED -> R.string.set_rate
	Eci.VOICE_PITCH_BASELINE -> R.string.set_pitch
	Eci.VOICE_PITCH_FLUCTUATION -> R.string.set_inflection
	Eci.VOICE_BREATHINESS -> R.string.set_breathiness
	Eci.VOICE_HEAD_SIZE -> R.string.set_head_size
	Eci.VOICE_ROUGHNESS -> R.string.set_roughness
	else -> R.string.set_volume
}

private fun dialogTitleOf(param: Int): Int = when (param) {
	Eci.VOICE_SPEED -> R.string.dialog_rate_title
	Eci.VOICE_PITCH_BASELINE -> R.string.dialog_pitch_title
	Eci.VOICE_PITCH_FLUCTUATION -> R.string.dialog_inflection_title
	Eci.VOICE_BREATHINESS -> R.string.dialog_breathiness_title
	Eci.VOICE_HEAD_SIZE -> R.string.dialog_head_size_title
	Eci.VOICE_ROUGHNESS -> R.string.dialog_roughness_title
	else -> R.string.dialog_volume_title
}

private fun descriptionOf(param: Int): Int = when (param) {
	Eci.VOICE_SPEED -> R.string.rate_description
	Eci.VOICE_PITCH_BASELINE -> R.string.pitch_description
	Eci.VOICE_PITCH_FLUCTUATION -> R.string.inflection_description
	Eci.VOICE_BREATHINESS -> R.string.breathiness_description
	Eci.VOICE_HEAD_SIZE -> R.string.head_size_description
	Eci.VOICE_ROUGHNESS -> R.string.roughness_description
	else -> R.string.volume_description
}

private fun numberModeNameOf(mode: Int): Int = when (mode) {
	Numbers.DIGITS -> R.string.number_digits
	Numbers.PAIRS -> R.string.number_pairs
	Numbers.TRIPLETS -> R.string.number_triplets
	else -> R.string.number_as_is
}

private fun routingNameOf(route: Int): Int = when (route) {
	SynthesisRouting.ACCESSIBILITY -> R.string.routing_accessibility
	SynthesisRouting.MEDIA -> R.string.routing_media
	SynthesisRouting.ALARMS -> R.string.routing_alarms
	SynthesisRouting.ALL -> R.string.routing_all
	else -> R.string.routing_automatic
}

private fun routingDescriptionOf(route: Int): Int = when (route) {
	SynthesisRouting.ACCESSIBILITY -> R.string.routing_accessibility_description
	SynthesisRouting.MEDIA -> R.string.routing_media_description
	SynthesisRouting.ALARMS -> R.string.routing_alarms_description
	SynthesisRouting.ALL -> R.string.routing_all_description
	else -> R.string.routing_automatic_description
}

private fun optimizationNameOf(mode: Int): Int = when (AudioOptimization.clamp(mode)) {
	AudioOptimization.VOLUME -> R.string.audio_optimization_volume
	AudioOptimization.QUALITY -> R.string.audio_optimization_quality
	else -> R.string.audio_optimization_balanced
}

private fun spellingNameOf(mode: Int): Int = when (mode) {
	Spelling.LETTERS -> R.string.spelling_letters
	Spelling.NATO -> R.string.spelling_nato
	Spelling.CAPITALS -> R.string.spelling_capitals
	Spelling.CAPITALS_NATO -> R.string.spelling_capitals_nato
	else -> R.string.spelling_off
}

private fun capitalNameOf(mode: Int): Int = when (mode) {
	CapitalLetters.CAP -> R.string.capital_cap
	CapitalLetters.PITCH -> R.string.capital_pitch
	else -> R.string.capital_none
}

private fun nameOf(mode: Int): Int = when (mode) {
	Symbols.ALL -> R.string.symbol_mode_all
	Symbols.MOST -> R.string.symbol_mode_most
	Symbols.SOME -> R.string.symbol_mode_some
	Symbols.CUSTOM -> R.string.symbol_mode_custom
	else -> R.string.symbol_mode_none
}

// ---- the pieces the screen is made of ----------------------------------

/**
 * One setting: the sentence that says what it is for, and the control.
 *
 * Shared with the battery and abbreviations screens, which have settings of
 * their own and the same reason for a description above each: a control's label
 * does not always carry its own meaning, and a description folded into the
 * label would be said again on every visit with no way past it.
 */
@Composable
internal fun Described(description: String?, control: @Composable () -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
	) {
		description?.let { said ->
			Text(
				text = said,
				style = MaterialTheme.typography.bodyMedium,
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 4.dp)
			)
		}
		control()
	}
}

/**
 * A setting that is a choice, as a button that opens the list of them: the
 * dropdown of this screen, made out of a menu so that it needs no anchor
 * experimentals and reads the same to a screen reader as any other menu.
 *
 * The button carries the setting and its value together. A button named only
 * "Language" is a button whose value has to be looked for; one named "Language:
 * English (United States)" says both, and the menu it opens marks the chosen
 * one with a tick that the row's own text already says.
 */
@Composable
internal fun DropdownSetting(
	label: String,
	value: String,
	options: List<String>,
	onChoose: (Int) -> Unit,
	description: String? = null,
	enabled: Boolean = true
)
{
	var open by remember { mutableStateOf(false) }
	val shown = stringResource(R.string.dropdown_value, label, value)
	Described(description = description) {
		Box(modifier = Modifier.fillMaxWidth()) {
			OutlinedButton(
				onClick = { if (enabled) open = true },
				enabled = enabled,
				modifier = Modifier
					.fillMaxWidth()
					.semantics {
						contentDescription = shown
						role = Role.Button
					}
			) {
				Text(
					text = shown,
					style = MaterialTheme.typography.labelLarge,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
			DropdownMenu(
				expanded = open,
				onDismissRequest = { open = false }
			) {
				options.forEachIndexed { at, option ->
					DropdownMenuItem(
						text = { Text(text = option) },
						onClick = {
							onChoose(at)
							open = false
						},
						leadingIcon = {
							if (at == options.indexOfFirst { it == value }) {
								Icon(
									imageVector = Icons.Filled.Check,
									contentDescription = null
								)
							}
						}
					)
				}
			}
		}
	}
}

/**
 * The detection mode, as a mode selector rather than a dropdown of four words:
 * the names do not explain themselves, so the button opens a dialog in which
 * each mode has one short line above its name.
 *
 * It is the only control on the screen disabled by another, and that is
 * deliberate: with detection off there is no mode to choose, and a button that
 * still opened would be offering a setting that does nothing.
 */
@Composable
private fun DetectionModeSetting(mode: Int, onChoose: (Int) -> Unit, enabled: Boolean) {
	val names = listOf(
		stringResource(R.string.mode_character),
		stringResource(R.string.mode_word),
		stringResource(R.string.mode_sentence),
		stringResource(R.string.mode_mixed)
	)
	val blurbs = listOf(
		stringResource(R.string.mode_character_short_description),
		stringResource(R.string.mode_word_short_description),
		stringResource(R.string.mode_sentence_short_description),
		stringResource(R.string.mode_mixed_short_description)
	)
	val chosen = DetectionMode.MODES.indexOf(DetectionMode.clamp(mode))
	var showing by remember { mutableStateOf(false) }
	val shown = stringResource(R.string.detect_mode_value, names.getOrElse(chosen) { names[0] })
	Described(description = stringResource(R.string.detect_mode_short_description)) {
		OutlinedButton(
			onClick = { if (enabled) showing = true },
			enabled = enabled,
			modifier = Modifier
				.fillMaxWidth()
				.semantics {
					contentDescription = shown
					role = Role.Button
				}
		) {
			Text(
				text = shown,
				style = MaterialTheme.typography.labelLarge,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
	if (showing) {
		ModeDialog(
			chosen = chosen,
			names = names,
			blurbs = blurbs,
			onChoose = { at ->
				onChoose(DetectionMode.MODES.getOrElse(at) { DetectionMode.DEFAULT })
				showing = false
			},
			onDismiss = { showing = false }
		)
	}
}

/** The four modes, each with its one line above the name it belongs to. */
@Composable
private fun ModeDialog(
	chosen: Int,
	names: List<String>,
	blurbs: List<String>,
	onChoose: (Int) -> Unit,
	onDismiss: () -> Unit
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(text = stringResource(R.string.detect_mode_label)) },
		text = {
			Column(
				modifier = Modifier
					.selectableGroup()
					.verticalScroll(rememberScrollState())
			) {
				names.forEachIndexed { at, name ->
					val selected = at == chosen
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.selectable(
								selected = selected,
								onClick = { onChoose(at) },
								role = Role.RadioButton
							)
							.padding(vertical = 10.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						RadioButton(selected = selected, onClick = null)
						Column(modifier = Modifier.padding(start = 12.dp)) {
							Text(
								text = blurbs.getOrElse(at) { "" },
								style = MaterialTheme.typography.bodySmall
							)
							Text(
								text = name,
								style = MaterialTheme.typography.bodyLarge
							)
						}
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) {
				Text(text = stringResource(R.string.cancel))
			}
		}
	)
}

/**
 * A section that opens and closes, and says which it is.
 *
 * The state is in the semantics and not only in the arrow, because the arrow is
 * drawn and a screen reader does not see it. TalkBack reads the name, then
 * "collapsed" or "expanded", then that it is a button -- which is the whole of
 * what somebody who cannot see needs in order to know whether the advanced
 * settings are in front of them or behind a press.
 *
 * The state is saved, so leaving the screen and coming back finds the section
 * as it was left rather than shut again.
 */
@Composable
internal fun CollapsibleSection(
	title: String,
	description: String,
	content: @Composable () -> Unit
) {
	var expanded by rememberSaveable { mutableStateOf(false) }
	val state = stringResource(if (expanded) R.string.expanded else R.string.collapsed)
	Column(modifier = Modifier.fillMaxWidth()) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable { expanded = !expanded }
				.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
				.clearAndSetSemantics {
					contentDescription = title
					stateDescription = state
					role = Role.Button
					onClick {
						expanded = !expanded
						true
					}
				},
			verticalAlignment = Alignment.CenterVertically
		) {
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = title,
					style = MaterialTheme.typography.titleMedium,
					modifier = Modifier.semantics { heading() }
				)
				Text(
					text = description,
					style = MaterialTheme.typography.bodyMedium
				)
			}
			Text(
				text = if (expanded) "\u25B2" else "\u25BC",
				style = MaterialTheme.typography.titleMedium
			)
		}
		if (expanded) content()
	}
}

/**
 * A button that carries its own value: "Voice (Reed) selected".
 *
 * The label says what the setting is and what it is set to, because a button
 * whose name is only "Change" or "Choose" is a button whose setting has to be
 * guessed at. What the button does is said as well: the state carries "Click to
 * change", which a screen reader reads between the label and the button's own
 * "double tap to activate".
 */
@Composable
internal fun ValueButton(shown: String, onClick: () -> Unit) {
	val change = stringResource(R.string.action_click_to_change)
	OutlinedButton(
		onClick = onClick,
		modifier = Modifier
			.fillMaxWidth()
			.semantics {
				contentDescription = shown
				stateDescription = change
			}
	) {
		Text(text = shown, style = MaterialTheme.typography.labelLarge)
	}
}

/** A setting that is a choice, as a button that opens the list of them. Kept
 *  for the screens that show a list in a dialog rather than a dropdown. */
@Composable
internal fun ChoiceButton(
	title: String,
	shown: String,
	options: List<String>,
	chosen: Int,
	onChoose: (Int) -> Unit,
	description: String? = null
) {
	var open by remember { mutableStateOf(false) }
	Described(description = description) {
		ValueButton(shown = shown, onClick = { open = true })
	}
	if (open) {
		AlertDialog(
			onDismissRequest = { open = false },
			title = { Text(text = title) },
			text = {
				Column(modifier = Modifier.selectableGroup()) {
					options.forEachIndexed { at, option ->
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.selectable(
									selected = at == chosen,
									onClick = {
										onChoose(at)
										open = false
									},
									role = Role.RadioButton
								)
								.padding(vertical = 12.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							RadioButton(selected = at == chosen, onClick = null)
							Text(
								text = option,
								modifier = Modifier.padding(start = 12.dp),
								style = MaterialTheme.typography.bodyLarge
							)
						}
					}
				}
			},
			confirmButton = {
				TextButton(onClick = { open = false }) {
					Text(text = stringResource(R.string.cancel))
				}
			}
		)
	}
}

/** A label, a switch, and the state of it, as one control. */
@Composable
internal fun SwitchRow(
	label: String,
	checked: Boolean,
	onChange: (Boolean) -> Unit,
	description: String? = null
) {
	val state = stringResource(if (checked) R.string.switch_on else R.string.switch_off)
	Described(description = description) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable { onChange(!checked) }
				.padding(vertical = 8.dp)
				.clearAndSetSemantics {
					contentDescription = label
					stateDescription = state
					role = Role.Switch
				},
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = label,
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier.weight(1f)
			)
			Switch(checked = checked, onCheckedChange = null)
		}
	}
}

/** Something to press, with what it does said above it. A button of its own
 *  rather than a row that behaves like one, so that its label needs no
 *  semantics of ours to be announced as one. */
@Composable
internal fun ActionRow(label: String, description: String?, onClick: () -> Unit) {
	Described(description = description) {
		OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
			Text(text = label, style = MaterialTheme.typography.labelLarge)
		}
	}
}

/**
 * A button that opens one of the numeric settings in a dialog of its own.
 *
 * The bar lives in the dialog rather than on the screen, so the screen is a
 * list of things to change rather than a wall of numbers, and the change is
 * Saved or Cancelled rather than made on the way past. [onPercent] is called
 * once, by Save.
 */
@Composable
private fun ParamButton(
	label: String,
	title: String,
	description: String,
	percent: Int,
	onPercent: (Int) -> Unit
) {
	var open by remember { mutableStateOf(false) }
	Described(description = description) {
		OutlinedButton(
			onClick = { open = true },
			modifier = Modifier.fillMaxWidth()
		) {
			Text(text = label, style = MaterialTheme.typography.labelLarge)
		}
	}
	if (open) {
		SliderDialog(
			title = title,
			percent = percent,
			onPercent = onPercent,
			onDismiss = { open = false }
		)
	}
}

/**
 * A label, a bar and a number, as one thing, inside the dialog that owns it.
 *
 * clearAndSetSemantics throws away what the label and the bar would each have
 * said and puts one control there instead: the name, the value, the range it
 * moves in, and how to move it. Without it a reader finds the text and the bar
 * separately and neither says what the other is.
 *
 * The bar takes the keyboard and the bar inside it does not. A bar that keeps a
 * focus of its own is a second place for the keyboard to be, invisible to a
 * screen reader because the semantics here are the row's.
 *
 * A key press moves it by one. TalkBack adjusts any slider by a twentieth of the
 * range it is given, so its own arrow key asks for five percent here whatever
 * the steps say; five percent of the speed scale is the difference between too
 * slow to bear and faster than was wanted, so a request to move is read as a
 * request to move one.
 */
@Composable
private fun SliderDialog(
	title: String,
	percent: Int,
	onPercent: (Int) -> Unit,
	onDismiss: () -> Unit
) {
	var chosen by remember { mutableStateOf(percent) }
	val spoken = stringResource(R.string.percent_spoken, chosen)
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(text = title) },
		text = {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.onKeyEvent { press ->
						if (press.type != KeyEventType.KeyDown) return@onKeyEvent false
						when (press.key) {
							Key.DirectionLeft, Key.DirectionDown -> {
								chosen = (chosen - 1).coerceIn(0, Eci.PERCENT_MAX)
								true
							}
							Key.DirectionRight, Key.DirectionUp -> {
								chosen = (chosen + 1).coerceIn(0, Eci.PERCENT_MAX)
								true
							}
							Key.MoveHome -> {
								chosen = 0
								true
							}
							Key.MoveEnd -> {
								chosen = Eci.PERCENT_MAX
								true
							}
							else -> false
						}
					}
					.focusable()
					.clearAndSetSemantics {
						contentDescription = title
						stateDescription = spoken
						progressBarRangeInfo = ProgressBarRangeInfo(chosen.toFloat(), 0f..100f, 99)
						setProgress { want ->
							val target = want.roundToInt()
							if (target != chosen) {
								chosen = (chosen + if (target > chosen) 1 else -1)
									.coerceIn(0, Eci.PERCENT_MAX)
							}
							true
						}
					}
			) {
				Text(
					text = stringResource(R.string.percent, chosen),
					style = MaterialTheme.typography.labelLarge,
					modifier = Modifier.fillMaxWidth()
				)
				Slider(
					modifier = Modifier.focusProperties { canFocus = false },
					value = chosen.toFloat(),
					onValueChange = { chosen = it.roundToInt() },
					valueRange = 0f..100f,
					steps = 0
				)
			}
		},
		confirmButton = {
			TextButton(onClick = {
				onPercent(chosen)
				onDismiss()
			}) {
				Text(text = stringResource(R.string.save))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(text = stringResource(R.string.cancel))
			}
		}
	)
}
