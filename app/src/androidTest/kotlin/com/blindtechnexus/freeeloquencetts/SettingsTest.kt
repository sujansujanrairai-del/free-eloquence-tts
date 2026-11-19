package com.blindtechnexus.freeeloquencetts

import android.content.Context
import android.media.AudioAttributes
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsTest {

	private lateinit var settings: Settings

	@Before
	fun setUp() {
		settings = Settings(InstrumentationRegistry.getInstrumentation().targetContext)
		wipe()
	}

	@After
	fun tearDown() {
		wipe()
	}

	@Test
	fun aVoiceKeepsWhatWasChosenForIt() {
		settings.setShapeValue(REED, Eci.VOICE_PITCH_BASELINE, 55)
		assertEquals(55, settings.shapeValue(REED, Eci.VOICE_PITCH_BASELINE))

		settings.voice = GLEN
		settings.setShapeValue(GLEN, Eci.VOICE_PITCH_BASELINE, 80)
		settings.voice = REED
		assertEquals("Reed did not keep his pitch", 55, settings.shapeValue(REED, Eci.VOICE_PITCH_BASELINE))
		assertEquals("Glen did not keep his", 80, settings.shapeValue(GLEN, Eci.VOICE_PITCH_BASELINE))
	}

	@Test
	fun oneVoicesResetLeavesTheOthersAlone() {
		settings.setShapeValue(REED, Eci.VOICE_PITCH_BASELINE, 55)
		settings.setShapeValue(GLEN, Eci.VOICE_PITCH_BASELINE, 80)
		settings.clearShape(REED)
		assertNull("Reed was not reset", settings.shapeValue(REED, Eci.VOICE_PITCH_BASELINE))
		assertEquals("Glen was reset too", 80, settings.shapeValue(GLEN, Eci.VOICE_PITCH_BASELINE))
	}

	@Test
	fun aVoiceNobodyHasTouchedSaysSo() {
		assertFalse(settings.shapeIsCustom(REED))
		settings.setShapeValue(REED, Eci.VOICE_VOLUME, 40)
		assertTrue(settings.shapeIsCustom(REED))
		assertFalse("a change to Reed marked Glen", settings.shapeIsCustom(GLEN))
	}

	@Test
	fun speedIsSharedByEveryVoice() {
		settings.setShapeValue(REED, Eci.VOICE_SPEED, 65)
		assertEquals("Glen did not get the speed", 65, settings.shapeValue(GLEN, Eci.VOICE_SPEED))
		assertEquals(65, settings.shape(GLEN)[Eci.VOICE_SPEED])

		settings.clearShape(REED)
		assertEquals(65, settings.shapeValue(REED, Eci.VOICE_SPEED))
	}

	@Test
	fun everyVoiceHasASpeedEvenUntouched() {
		assertEquals(Eci.DEFAULT_SPEED, settings.shape(REED)[Eci.VOICE_SPEED])
	}

	@Test
	fun theBatteryAndStabilitySwitchesRememberTheirState() {

		remove(Settings.KEY_PERSISTENT_NOTIFICATION)
		assertFalse("running in the foreground starts on", settings.persistentNotification)
		assertFalse("speaking in the foreground starts on", settings.foregroundWhileSpeaking)
		assertFalse("the wake lock starts on", settings.wakeLock)
		assertFalse("action tracking starts on", settings.trackActions)
		settings.persistentNotification = true
		settings.foregroundWhileSpeaking = true
		settings.wakeLock = true
		settings.trackActions = true
		assertTrue(settings.persistentNotification)
		assertTrue(settings.foregroundWhileSpeaking)
		assertTrue(settings.wakeLock)
		assertTrue(settings.trackActions)
	}

	@Test
	fun theLanguageIsOneOfTheTwoAndFallsBackToAmerican() {
		remove(Settings.KEY_LANGUAGE)
		assertEquals("the language starts as British", Eci.ENGLISH_US, settings.language)
		settings.language = Eci.ENGLISH_UK
		assertEquals("British English was not remembered", Eci.ENGLISH_UK, settings.language)
		settings.language = 0x1234
		assertEquals("a foreign language did not fall back", Eci.ENGLISH_US, settings.language)
		settings.language = Eci.GERMAN
		assertEquals("German was not remembered", Eci.GERMAN, settings.language)
		settings.language = Eci.ENGLISH_US
	}

	@Test
	fun theSampleRateIsAlwaysOneTheEngineHas() {
		remove(Settings.KEY_SAMPLE_RATE)
		assertEquals(Settings.DEFAULT_SAMPLE_RATE, settings.sampleRateHz)
		settings.sampleRateHz = 22050
		assertEquals(22050, settings.sampleRateHz)
		settings.sampleRateHz = 23000
		assertEquals("an off rate did not snap", 22050, settings.sampleRateHz)
		Settings.store(InstrumentationRegistry.getInstrumentation().targetContext)
			.edit().putInt(Settings.KEY_SAMPLE_RATE, 12345).commit()
		assertEquals("a hand-written rate did not snap", 11025, settings.sampleRateHz)
		remove(Settings.KEY_SAMPLE_RATE)
	}

	@Test
	fun theCodesTheRoutingAndTheForcedLanguageAreRemembered() {
		remove(Settings.KEY_READ_OTP_DIGITS)
		remove(Settings.KEY_SYNTHESIS_ROUTING)
		remove(Settings.KEY_FORCE_LANGUAGE)
		assertTrue("codes start off", settings.readOtpDigits)
		assertEquals(SynthesisRouting.AUTOMATIC, settings.synthesisRouting)
		assertFalse("the language starts forced", settings.forceLanguage)
		settings.readOtpDigits = false
		assertFalse(settings.readOtpDigits)
		settings.synthesisRouting = SynthesisRouting.MEDIA
		assertEquals(SynthesisRouting.MEDIA, settings.synthesisRouting)
		settings.synthesisRouting = SynthesisRouting.ALARMS
		assertEquals(SynthesisRouting.ALARMS, settings.synthesisRouting)
		settings.synthesisRouting = 77
		assertEquals("a bad route did not fall back", SynthesisRouting.AUTOMATIC, settings.synthesisRouting)
		settings.forceLanguage = true
		assertTrue(settings.forceLanguage)
		remove(Settings.KEY_READ_OTP_DIGITS)
		remove(Settings.KEY_SYNTHESIS_ROUTING)
		remove(Settings.KEY_FORCE_LANGUAGE)
	}

	@Test
	fun theRoutingMenuStartsWithDefaultAndNamesItsChannels() {
		assertEquals("Default is not at the top", SynthesisRouting.AUTOMATIC, SynthesisRouting.CHOICES.first())
		assertTrue(SynthesisRouting.CHOICES.contains(SynthesisRouting.MEDIA))
		assertTrue(SynthesisRouting.CHOICES.contains(SynthesisRouting.ALARMS))
		assertTrue(SynthesisRouting.CHOICES.contains(SynthesisRouting.ACCESSIBILITY))

		assertTrue(SynthesisRouting.handsToFramework(SynthesisRouting.AUTOMATIC))
		assertFalse(SynthesisRouting.playsItself(SynthesisRouting.AUTOMATIC))

		assertEquals(AudioAttributes.USAGE_MEDIA, SynthesisRouting.usageOf(SynthesisRouting.MEDIA))
		assertEquals(AudioAttributes.USAGE_ALARM, SynthesisRouting.usageOf(SynthesisRouting.ALARMS))
		assertEquals(
			AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY,
			SynthesisRouting.usageOf(SynthesisRouting.ACCESSIBILITY)
		)
		assertTrue(SynthesisRouting.playsItself(SynthesisRouting.ALARMS))
	}

	@Test
	fun textStylesAreReadUntilSomebodySaysOtherwise() {
		remove(Settings.KEY_READ_TEXT_STYLES)
		assertTrue("reading text styles starts off", settings.readTextStyles)
		settings.readTextStyles = false
		assertFalse(settings.readTextStyles)
		settings.readTextStyles = true
		assertTrue(settings.readTextStyles)
	}

	private fun remove(key: String) {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		DirectBoot.storage(context)
			.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
			.edit().remove(key).apply()
	}

	private fun wipe() {
		for (voice in Eci.PRESET_NAMES.indices) settings.clearShape(voice)
		settings.speed = Eci.DEFAULT_SPEED
		settings.voice = REED
		settings.persistentNotification = false
		settings.foregroundWhileSpeaking = false
		settings.wakeLock = false
		settings.trackActions = false
	}

	private companion object {
		const val REED = 0
		const val GLEN = 4
	}
}
