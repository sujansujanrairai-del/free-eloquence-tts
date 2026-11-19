package com.blindtechnexus.freeeloquencetts

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class UpdateTest {

	private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

	@Test
	fun theSameVersionIsNoUpdateAtAll() {
		assertNull(UpdateInfo.parse("""{"version":"1.1","url":"$HTTPS"}""", "1.1"))

		assertNull(UpdateInfo.parse("""{"version":" 1.1 ","url":"$HTTPS"}""", "1.1"))
	}

	@Test
	fun aDifferentVersionCarriesItselfAndTheAddress() {
		val found = UpdateInfo.parse("""{"version":"1.2","url":"$HTTPS"}""", "1.1")
		assertNotNull(found)
		assertEquals("1.2", found!!.version)
		assertEquals(HTTPS, found.downloadUrl)
	}

	@Test
	fun aNumberThatMovedBackwardsIsStillAMove() {

		assertNotNull(UpdateInfo.parse("""{"version":"1.0","url":"$HTTPS"}""", "1.1"))
		assertNotNull(UpdateInfo.parse("""{"version":"1.10","url":"$HTTPS"}""", "1.2"))
	}

	@Test
	fun nothingIsOfferedWhenOneOfTheTwoValuesIsMissing() {
		assertNull(UpdateInfo.parse("""{"url":"$HTTPS"}""", "1.1"))
		assertNull(UpdateInfo.parse("""{"version":"","url":"$HTTPS"}""", "1.1"))
		assertNull(UpdateInfo.parse("""{"version":"1.2"}""", "1.1"))
		assertNull(UpdateInfo.parse("""{"version":"1.2","url":""}""", "1.1"))
		assertNull(UpdateInfo.parse("", "1.1"))
		assertNull(UpdateInfo.parse("not json at all", "1.1"))

		assertNull(UpdateInfo.parse("[]", "1.1"))
	}

	@Test
	fun aPlainHttpAddressIsRefusedRatherThanWarnedAbout() {

		assertNull(UpdateInfo.parse("""{"version":"1.2","url":"http://example.com/a.apk"}""", "1.1"))
		assertNull(UpdateInfo.parse("""{"version":"1.2","url":"file:///sdcard/a.apk"}""", "1.1"))
		assertNull(UpdateInfo.parse("""{"version":"1.2","url":"  "}""", "1.1"))

		assertNull(UpdateInfo.openStream("http://example.com/a.apk"))
	}

	@Test
	fun theChannelIsTheOneThatWasAskedFor() {

		assertEquals(
			"https://raw.githubusercontent.com/blind-tech-nexus/remote-data-json" +
				"/main/alt-eloquence-tts-update.json",
			UpdateInfo.CHANNEL
		)
	}

	@Test
	fun theFileAlreadyPublishedIsWhatTheParserExpects() {

		val found = UpdateInfo.parse("""{"version":"1.0.0","url":"https://api.example.com/demo"}""", "1.1")
		assertNotNull(found)
		assertEquals("1.0.0", found!!.version)
		assertEquals("https://api.example.com/demo", found.downloadUrl)
	}

	@Test
	fun theUpdateGoesIntoTheCacheUnderANameOnlyThisAppWrites() {
		val file = ApkDownload.fileFor(context, "1.2 beta/3")
		assertTrue(file.path.startsWith(context.cacheDir.absolutePath))
		assertTrue(file.parentFile!!.name == "update")

		assertFalse(file.name.contains("/"))
		assertEquals("alt-eloquence-tts-1.2-beta-3.apk", file.name)
		val plain = ApkDownload.fileFor(context, "1.2")
		assertEquals("alt-eloquence-tts-1.2.apk", plain.name)
	}

	@Test
	fun sayingLaterDoesNotForgetAboutTheNextVersion() {

		assertFalse(Updater.shouldShowDialog())
	}

	@Test
	fun aCheckThatWasCancelledAsksForNothingAtAll() {

		val cancelled = AtomicBoolean(true)
		assertTrue(UpdateInfo.fetch("0.0", cancelled) is UpdateInfo.Result.Cancelled)
	}

	@Test
	fun aFileThatIsNotAPackageIsNeverReadyToInstall() {

		val partial = java.io.File(context.cacheDir, "update/probe-partial.apk")
		try {
			partial.parentFile?.mkdirs()
			partial.writeBytes(ByteArray(1024) { (it % 251).toByte() })
			assertFalse(Installer.isInstallable(context, partial))
			assertFalse(Installer.isInstallable(context, java.io.File(context.cacheDir, "update/no-such-file.apk")))
		} finally {
			partial.delete()
		}
	}

	private companion object {
		const val HTTPS = "https://example.com/alt-eloquence-tts.apk"
	}
}
