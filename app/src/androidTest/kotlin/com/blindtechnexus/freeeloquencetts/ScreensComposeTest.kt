package com.blindtechnexus.freeeloquencetts

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreensComposeTest {

	@get:Rule
	val compose = createComposeRule()

	private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

	@Test
	fun theBatteryScreenBuildsItselfAndOffersEveryRow() {
		compose.setContent {
			MaterialTheme {
				BatteryScreen(onBack = {}, onRequestExemption = {})
			}
		}
		for (label in listOf(
			R.string.persistent_notification_label,
			R.string.notification_stop_label,
			R.string.start_on_boot_label,
			R.string.track_actions_label
		)) {
			compose.onNodeWithContentDescription(context.getString(label)).assertExists()
		}

		compose.onNodeWithText(context.getString(R.string.persistent_notification_label)).assertExists()
		compose.onNodeWithText(
			context.getString(R.string.battery_notification_off),
			substring = true
		).assertExists()

		for (label in listOf(
			R.string.foreground_while_speaking_label,
			R.string.wake_lock_label
		)) {
			compose.onNodeWithContentDescription(context.getString(label)).assertDoesNotExist()
		}
		for (label in listOf(
			R.string.wake_lock_tail_label,
			R.string.speech_lead_label,
			R.string.run_ahead_label,
			R.string.engine_release_label,
			R.string.engine_silence_label
		)) {
			compose.onNodeWithText(context.getString(label), substring = true).assertDoesNotExist()
		}
	}

	@Test
	fun theAboutScreenBuildsItselfAndOffersAnUpdateCheck() {
		compose.setContent {
			MaterialTheme {
				AboutScreen(onBack = {})
			}
		}
		compose.onNodeWithText(context.getString(R.string.about_update_check)).assertExists()
		compose.onNodeWithText(context.getString(R.string.about_whatsapp_action)).assertExists()
	}
}
