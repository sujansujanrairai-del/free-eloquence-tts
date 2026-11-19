package com.blindtechnexus.freeeloquencetts

import android.content.Context
import android.content.Intent
import android.provider.Settings

object SystemSpeechSettings {

	fun ways(): List<Intent> = listOf(
		Intent(TTS_SETTINGS),
		Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
		Intent(Settings.ACTION_SETTINGS)
	)

	fun open(context: Context): Boolean = startFirst(context, ways())

	private const val TTS_SETTINGS = "com.android.settings.TTS_SETTINGS"
}
