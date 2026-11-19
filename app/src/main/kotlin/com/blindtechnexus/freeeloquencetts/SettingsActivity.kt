package com.blindtechnexus.freeeloquencetts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource

internal enum class Destination {
	WELCOME, MAIN, SETTINGS, ABOUT, DICTIONARY, DICTIONARY_FILE, ABBREVIATIONS, BATTERY, AUDIO
}

class SettingsActivity : ComponentActivity() {

	private var model: SettingsModel? = null

	private val firstLaunchPreferences by lazy {
		getSharedPreferences(FIRST_LAUNCH_PREFERENCES, MODE_PRIVATE)
	}

	private var destination by mutableStateOf(Destination.MAIN)

	private var enteredAt = Destination.MAIN

	private var openDictionaryFile by mutableStateOf("")

	private var asksForTheExemption by mutableStateOf(false)

	private var asksAboutTheDefaultEngine by mutableStateOf(false)

	private val notificationPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) {

		askAboutTheExemption()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val firstLaunch = !firstLaunchPreferences.getBoolean(FIRST_LAUNCH_COMPLETE, false)
		val configured = intent?.action == ACTION_CONFIGURE_ENGINE
		enteredAt = if (configured) Destination.SETTINGS else Destination.MAIN
		destination = if (firstLaunch) Destination.WELCOME else enteredAt

		if (intent?.getStringExtra(EXTRA_SCREEN) == SCREEN_BATTERY) {
			enteredAt = Destination.BATTERY
			destination = Destination.BATTERY
		}
		asksAboutTheDefaultEngine = !isDefaultTts()
		refresh()

		if (!firstLaunch) checkPermissions()

		Updater.attach(applicationContext)
		Updater.check()
		setContent {
			MaterialTheme {
				Surface {
					BackHandler(
						enabled = destination != Destination.WELCOME &&
							destination != Destination.MAIN
					) {
						back()
					}
					when (destination) {
						Destination.WELCOME -> WelcomeScreen(onContinue = ::continueFromWelcome)
						Destination.MAIN -> MainScreen(
							onConfigure = { destination = Destination.SETTINGS },
							onOpenSystemSpeechSettings = {
								SystemSpeechSettings.open(this@SettingsActivity)
							},
							onAbout = { destination = Destination.ABOUT },
							onAudio = { destination = Destination.AUDIO }
						)
						Destination.SETTINGS -> SettingsScreen(
							state = remember { settingsModel() },
							onBack = ::back,
							onOpenDictionary = { destination = Destination.DICTIONARY },
							onOpenAbbreviations = { destination = Destination.ABBREVIATIONS },
							onOpenBattery = { destination = Destination.BATTERY }
						)
						Destination.DICTIONARY -> DictionaryScreen(
							onBack = ::back,
							onOpenFile = { name ->
								openDictionaryFile = name
								destination = Destination.DICTIONARY_FILE
							}
						)
						Destination.DICTIONARY_FILE -> DictionaryFileScreen(
							name = openDictionaryFile,
							onBack = ::back,
							onDeleted = ::back
						)
						Destination.ABBREVIATIONS -> AbbreviationsScreen(onBack = ::back)
						Destination.BATTERY -> BatteryScreen(
							onBack = ::back,
							onRequestExemption = ::requestTheExemption
						)
						Destination.ABOUT -> AboutScreen(onBack = ::back)
						Destination.AUDIO -> AudioGeneratorScreen(onBack = ::back)
					}

					if (destination != Destination.WELCOME) {
						when {
							asksForTheExemption -> ExemptionDialog(
								onAllow = {
									asksForTheExemption = false
									requestTheExemption()
								},
								onRefuse = { asksForTheExemption = false }
							)
							asksAboutTheDefaultEngine -> DefaultEngineDialog(
								onOpen = {
									asksAboutTheDefaultEngine = false
									SystemSpeechSettings.open(this@SettingsActivity)
								},
								onRefuse = { asksAboutTheDefaultEngine = false }
							)

							else -> UpdateDialogs()
						}
					}
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()
		refresh()
	}

	override fun onStop() {
		super.onStop()

		model?.stopSpeaking()
	}

	override fun onDestroy() {
		super.onDestroy()
		model?.close()
		model = null
	}

	private fun back() {
		when (destination) {
			Destination.SETTINGS -> {
				model?.stopSpeaking()
				if (enteredAt == Destination.SETTINGS) finish() else destination = Destination.MAIN
			}
			Destination.DICTIONARY, Destination.ABBREVIATIONS ->
				destination = Destination.SETTINGS
			Destination.DICTIONARY_FILE -> destination = Destination.DICTIONARY
			Destination.AUDIO -> destination = Destination.MAIN
			Destination.BATTERY ->

				if (enteredAt == Destination.BATTERY) finish() else destination = Destination.SETTINGS
			Destination.ABOUT -> destination = Destination.MAIN

			Destination.WELCOME -> finish()
			Destination.MAIN -> finish()
		}
	}

	private fun continueFromWelcome() {
		firstLaunchPreferences.edit()
			.putBoolean(FIRST_LAUNCH_COMPLETE, true)
			.apply()
		destination = enteredAt
		checkPermissions()
	}

	private fun settingsModel(): SettingsModel =
		model ?: SettingsModel(this).also { model = it }

	private fun checkPermissions() {
		if (
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
			checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
		) {
			notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
		} else {
			askAboutTheExemption()
		}
	}

	private fun askAboutTheExemption() {
		if (BatteryExemption.isGranted(this)) return
		asksForTheExemption = true
	}

	private fun refresh() {
		if (BatteryExemption.isGranted(this)) asksForTheExemption = false
		if (isDefaultTts()) asksAboutTheDefaultEngine = false
	}

	private fun isDefaultTts(): Boolean = runCatching {
		android.provider.Settings.Secure.getString(
			contentResolver,
			android.provider.Settings.Secure.TTS_DEFAULT_SYNTH
		) == packageName
	}.getOrDefault(false)

	internal fun requestTheExemption() {
		if (!BatteryExemption.request(this)) {

			Log.e(TAG, "nothing here asks about battery optimization")
		}
	}

	internal companion object {
		const val TAG = "freeeloquencetts"

		const val ACTION_CONFIGURE_ENGINE = "android.speech.tts.engine.CONFIGURE_ENGINE"

		const val EXTRA_SCREEN = "com.blindtechnexus.freeeloquencetts.extra.SCREEN"

		const val SCREEN_BATTERY = "battery"

		const val FIRST_LAUNCH_PREFERENCES = "welcome"
		const val FIRST_LAUNCH_COMPLETE = "completed"
	}
}

@Composable
private fun ExemptionDialog(onAllow: () -> Unit, onRefuse: () -> Unit) {
	AlertDialog(
		onDismissRequest = onRefuse,
		title = { Text(text = stringResource(R.string.battery_title)) },
		text = { Text(text = stringResource(R.string.battery_message)) },
		confirmButton = {
			TextButton(onClick = onAllow) {
				Text(text = stringResource(R.string.battery_allow))
			}
		},
		dismissButton = {
			TextButton(onClick = onRefuse) {
				Text(text = stringResource(R.string.battery_later))
			}
		}
	)
}

@Composable
private fun DefaultEngineDialog(onOpen: () -> Unit, onRefuse: () -> Unit) {
	AlertDialog(
		onDismissRequest = onRefuse,
		title = { Text(text = stringResource(R.string.default_tts_title)) },
		text = { Text(text = stringResource(R.string.default_tts_message)) },
		confirmButton = {
			TextButton(onClick = onOpen) {
				Text(text = stringResource(R.string.set_as_default))
			}
		},
		dismissButton = {
			TextButton(onClick = onRefuse) {
				Text(text = stringResource(R.string.cancel))
			}
		}
	)
}
