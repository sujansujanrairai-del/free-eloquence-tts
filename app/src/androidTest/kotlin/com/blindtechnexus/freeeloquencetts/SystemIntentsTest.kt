package com.blindtechnexus.freeeloquencetts

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemIntentsTest {

	private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

	@Test
	fun whatsAppIsTheNumberTheTeamGave() {
		val ways = ExternalLinks.whatsAppWays()
		assertEquals(Intent.ACTION_VIEW, ways[0].action)

		assertEquals("https://wa.me/${ExternalLinks.WHATSAPP_NUMBER}", ways[0].data.toString())

		assertEquals(
			"https://api.whatsapp.com/send?phone=${ExternalLinks.WHATSAPP_NUMBER}",
			ways[1].data.toString()
		)
		assertEquals(
			"whatsapp://send?phone=${ExternalLinks.WHATSAPP_NUMBER}",
			ways[2].data.toString()
		)

		for (way in ways) {
			assertTrue(way.data.toString().contains(ExternalLinks.WHATSAPP_NUMBER))
			assertFalse(way.data.toString().contains("+${ExternalLinks.WHATSAPP_NUMBER}"))
		}
	}

	@Test
	fun thePhoneMakersBatteryScreenEndsWithAndroidsOwn() {

		val candidates = ManufacturerScreens.candidates()
		assertTrue(candidates.isNotEmpty())
		assertEquals(
			Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
			candidates.last().intent.action
		)

		for (candidate in candidates) {
			assertNull(candidate.intent.data)
		}
		for (candidate in candidates.dropLast(1)) {
			assertNotNull(candidate.intent.component)
		}
		assertEquals(
			context.packageName,
			ManufacturerScreens.appDetailsIntent(context).data?.lastPathSegment
		)
	}

	@Test
	fun makersAreMatchedByNameWhateverCaseTheyComeIn() {

		val htc = ManufacturerScreens.candidatesFor("HTC", "htc")
		assertTrue(htc.any { it.intent.component?.packageName == "com.htc.pitroad" })
		val lenovo = ManufacturerScreens.candidatesFor("LENOVO", "ZUK")
		assertTrue(lenovo.any { it.intent.component?.packageName == "com.lenovo.security" })
		val leeco = ManufacturerScreens.candidatesFor("LeEco", "LeEco")
		assertTrue(leeco.any { it.intent.component?.packageName == "com.letv.android.letvsafe" })
		val unknown = ManufacturerScreens.candidatesFor("SomePhone", "SomePhone")
		assertEquals(1, unknown.size)
		assertEquals(
			Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
			unknown.last().intent.action
		)
	}

	@Test
	fun thisAppIsWhereTheInstallerIsToldToLook() {

		val info = context.packageManager.getProviderInfo(
			android.content.ComponentName(context.packageName, "androidx.core.content.FileProvider"),
			0
		)
		assertEquals(Installer.authority(context), info.authority)
		assertTrue(info.grantUriPermissions)
		assertFalse(info.exported)

		val file = java.io.File(context.cacheDir, "update/probe.apk")
		try {
			file.parentFile?.mkdirs()
			file.writeBytes(ByteArray(4))
			val uri = androidx.core.content.FileProvider.getUriForFile(
				context, Installer.authority(context), file
			)
			assertEquals("content", uri.scheme)
			assertEquals(Installer.authority(context), uri.authority)
			assertTrue(uri.path!!.startsWith("/updates/"))
		} finally {
			file.delete()
		}
	}

	@Test
	fun telegramIsTheChannelTheTeamGave() {
		val ways = ExternalLinks.telegramWays()
		assertEquals(Intent.ACTION_VIEW, ways[0].action)
		assertEquals("https://t.me/${ExternalLinks.TELEGRAM_CHANNEL}", ways[0].data.toString())
		assertEquals(
			"tg://resolve?domain=${ExternalLinks.TELEGRAM_CHANNEL}",
			ways[1].data.toString()
		)
	}

	@Test
	fun emailIsAddressedAndAlreadyHasASubject() {
		val ways = ExternalLinks.emailWays(SUBJECT)

		assertEquals(Intent.ACTION_SENDTO, ways[0].action)
		assertEquals("mailto:${ExternalLinks.EMAIL_ADDRESS}", ways[0].data.toString())
		assertEquals(SUBJECT, ways[0].getStringExtra(Intent.EXTRA_SUBJECT))
		assertEquals(
			listOf(ExternalLinks.EMAIL_ADDRESS),
			ways[1].getStringArrayExtra(Intent.EXTRA_EMAIL)?.toList()
		)
	}

	@Test
	fun theExemptionIsAskedForByTheScreenThatNamesThisApp() {
		val ways = BatteryExemption.ways(context)
		assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, ways[0].action)
		assertEquals("package:${context.packageName}", ways[0].data.toString())

		assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, ways[1].action)
	}

	@Test
	fun theSpeechSettingsAreAskedForBeforeAnythingElse() {
		val ways = SystemSpeechSettings.ways()
		assertEquals("com.android.settings.TTS_SETTINGS", ways[0].action)
		assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, ways[1].action)
		assertEquals(Settings.ACTION_SETTINGS, ways[2].action)
	}

	@Test
	fun askingTheDeviceWhetherItWillLeaveTheEngineAloneDoesNotThrow() {

		BatteryExemption.isGranted(context)
	}

	private companion object {
		const val SUBJECT = "Free eloquence TTS"
	}
}
