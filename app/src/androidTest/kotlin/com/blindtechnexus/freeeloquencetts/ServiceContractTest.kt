package com.blindtechnexus.freeeloquencetts

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceContractTest {

	private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

	@Test
	fun theManifestDeclaresThePermissionsTheKeepAliveAndWakeLockNeed() {
		val info = context.packageManager.getPackageInfo(
			context.packageName,
			PackageManager.GET_PERMISSIONS
		)
		val asked = info.requestedPermissions?.toSet().orEmpty()

		assertTrue("FOREGROUND_SERVICE is not declared", asked.contains(Manifest.permission.FOREGROUND_SERVICE))
		assertTrue(
			"FOREGROUND_SERVICE_SPECIAL_USE is not declared",
			asked.contains(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
		)

		assertTrue("WAKE_LOCK is not declared", asked.contains(Manifest.permission.WAKE_LOCK))
		assertTrue("POST_NOTIFICATIONS is not declared", asked.contains(Manifest.permission.POST_NOTIFICATIONS))

		assertTrue("INTERNET is not declared", asked.contains(Manifest.permission.INTERNET))
		assertTrue(
			"REQUEST_INSTALL_PACKAGES is not declared",
			asked.contains(Manifest.permission.REQUEST_INSTALL_PACKAGES)
		)

		assertTrue(
			"REQUEST_IGNORE_BATTERY_OPTIMIZATIONS is not declared",
			asked.contains(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
		)
	}

	@Test
	fun theNotificationIsHeldUpByAServicethatNothingElseStops() {
		val component = ComponentName(context, KeepAliveService::class.java)
		val info = context.packageManager.getServiceInfo(component, PackageManager.GET_META_DATA)
		assertFalse("the keep-alive service must not be exported", info.exported)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			assertTrue(
				"the keep-alive service must be a special-use foreground service",
				info.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE != 0
			)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			val subtype = runCatching {
				context.packageManager
					.getProperty(PackageManager.PROPERTY_SPECIAL_USE_FGS_SUBTYPE, component)
					.getString()
			}.getOrNull()
			assertFalse("the keep-alive service must say what it is for", subtype.isNullOrBlank())
		}
	}

	@Test
	fun theReceiverIsAwakeForTheBootAndNobodyElsesButton() {
		val component = ComponentName(context, EngineReceiver::class.java)
		val receiver = context.packageManager.getReceiverInfo(component, 0)
		assertFalse("the receiver must not be exported", receiver.exported)
		for (action in listOf(
			Intent.ACTION_BOOT_COMPLETED,
			Intent.ACTION_LOCKED_BOOT_COMPLETED,
			Intent.ACTION_MY_PACKAGE_REPLACED
		)) {
			val taken = context.packageManager.queryBroadcastReceivers(Intent(action), 0)
				.any { it.activityInfo?.name == component.className }
			assertTrue("nothing takes $action", taken)
		}

		val stop = Intent(EngineReceiver.ACTION_STOP).setComponent(component)
		assertEquals(component.className, stop.component?.className)
	}

	@Test
	fun theServiceIsASpecialUseForegroundService() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
		val service = serviceInfo()
		assertTrue(
			"the service must declare specialUse so startForeground can promote it",
			service.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE != 0
		)
	}

	@Test
	fun theServiceNamesWhatItsForegroundUseIsFor() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
		val component = ComponentName(context, EvvTtsService::class.java)
		val subtype = runCatching {
			context.packageManager
				.getProperty(PackageManager.PROPERTY_SPECIAL_USE_FGS_SUBTYPE, component)
				.getString()
		}.getOrNull()
		assertNotNull("the special-use subtype is not declared", subtype)
		assertFalse("the special-use subtype is empty", subtype.isNullOrBlank())
	}

	@Test
	fun theKeepAliveAndWakeLockConstantsAgreeWithTheCodeThatRunsOnThem() {
		assertEquals("the notification channel must have a name", "keep_alive", KeepAlive.CHANNEL_ID)
		assertEquals("the notification id must be stable", 1, KeepAlive.ID)

		assertEquals(
			"the notification must not be checked on a clock",
			0L,
			KeepAlive.CHECK_MS
		)

		assertEquals("the wake lock ceiling moved", 10 * 60 * 1000L, EvvTtsService.WAKELOCK_TIMEOUT_MS)

		assertTrue("a pacing slice is bigger than the lead", EvvTtsService.SLICE_MS < EvvTtsService.LEAD_MS)
		assertTrue("a chunk is smaller than a slice of audio", EvvTtsService.MIN_CHUNK < EvvTtsService.MAX_CHUNK)
		assertEquals("samples are not 16-bit", 2, EvvTtsService.BYTES_PER_SAMPLE)
	}

	@Test
	fun trackingFollowsTheSwitch() {
		val settings = Settings(context)
		settings.trackActions = false
		assertFalse("tracking was on while the switch was off", Tracker.action(settings, "off"))
		settings.trackActions = true
		assertTrue("tracking was off while the switch was on", Tracker.action(settings, "on"))
		settings.trackActions = false
	}

	private fun serviceInfo(): ServiceInfo =
		context.packageManager.getServiceInfo(
			ComponentName(context, EvvTtsService::class.java),
			PackageManager.GET_META_DATA
		)
}
