package com.blindtechnexus.freeeloquencetts

import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreensTest {

	private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

	private val firstLaunch = context.getSharedPreferences(
		SettingsActivity.FIRST_LAUNCH_PREFERENCES, Context.MODE_PRIVATE
	)

	private var wasRead = false

	@Before
	fun rememberWhetherTheWelcomeScreenHasBeenRead() {
		wasRead = firstLaunch.getBoolean(SettingsActivity.FIRST_LAUNCH_COMPLETE, false)
	}

	@After
	fun putItBack() {
		firstLaunch.edit().putBoolean(SettingsActivity.FIRST_LAUNCH_COMPLETE, wasRead).apply()
	}

	@Test
	fun theWelcomeScreenBuildsItself() {
		sayItHasNotBeenRead()
		launch(action = null)
	}

	@Test
	fun theMainScreenBuildsItself() {
		sayItHasBeenRead()
		launch(action = null)
	}

	@Test
	fun theSettingsScreenBuildsItself() {
		sayItHasBeenRead()

		launch(action = SettingsActivity.ACTION_CONFIGURE_ENGINE)
	}

	@Test
	fun everySliderTheEngineHasIsOnTheScreen() {
		assertEquals(
			"the screen shows sliders the engine does not have, or is missing one",
			Settings.SLIDERS.sorted(),
			Settings.BEHAVIOURS.sorted()
		)
	}

	private fun launch(action: String?) {
		val intent = Intent(context, SettingsActivity::class.java).apply {
			if (action != null) setAction(action)
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		ActivityScenario.launch<SettingsActivity>(intent).use { scenario ->
			assertTrue(
				"the screen never got past being created, it is ${scenario.state}",
				scenario.state.isAtLeast(Lifecycle.State.CREATED)
			)
		}
	}

	private fun sayItHasBeenRead() = write(true)

	private fun sayItHasNotBeenRead() = write(false)

	private fun write(read: Boolean) {
		firstLaunch.edit().putBoolean(SettingsActivity.FIRST_LAUNCH_COMPLETE, read).apply()
	}
}
