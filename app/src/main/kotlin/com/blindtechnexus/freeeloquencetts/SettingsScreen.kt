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

@Composable
private fun BatterySection(onOpen: () -> Unit) {
	SectionHeading(text = stringResource(R.string.section_battery))
	ActionRow(
		label = stringResource(R.string.battery_open),
		description = stringResource(R.string.battery_short_description),
		onClick = onOpen
	)
}

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

		OutlinedButton(onClick = state::stopSpeaking, modifier = Modifier.fillMaxWidth()) {
			Text(
				text = stringResource(R.string.stop_label),
				style = MaterialTheme.typography.labelLarge
			)
		}
	}
}

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

@Composable
internal fun ActionRow(label: String, description: String?, onClick: () -> Unit) {
	Described(description = description) {
		OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
			Text(text = label, style = MaterialTheme.typography.labelLarge)
		}
	}
}

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
