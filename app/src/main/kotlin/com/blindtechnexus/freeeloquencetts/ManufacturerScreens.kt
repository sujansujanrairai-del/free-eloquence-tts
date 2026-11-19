package com.blindtechnexus.freeeloquencetts

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

internal object ManufacturerScreens {

	class Candidate(val label: String, val intent: Intent)

	private fun named(label: String, vararg pairs: Pair<String, String>) =
		pairs.map { (pkg, cls) ->
			Candidate(label, Intent(Intent.ACTION_VIEW).setComponent(ComponentName(pkg, cls)))
		}

	fun candidates(): List<Candidate> =
		candidatesFor(Build.MANUFACTURER.orEmpty(), Build.BRAND.orEmpty())

	fun candidatesFor(manufacturer: String, brandName: String): List<Candidate> {
		val maker = manufacturer.lowercase(java.util.Locale.ROOT)
		val brand = brandName.lowercase(java.util.Locale.ROOT)
		val out = ArrayList<Candidate>()
		when {
			maker.contains("xiaomi") || maker.contains("redmi") || brand.contains("poco") ->
				out += named(
					"MIUI",
					"com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
					"com.miui.securitycenter" to "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
				)
			maker.contains("huawei") || maker.contains("honor") ->
				out += named(
					"the phone's own startup manager",
					"com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
					"com.huawei.systemmanager" to "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
					"com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
				)
			maker.contains("oppo") || maker.contains("realme") || maker.contains("oneplus") ->
				out += named(
					"ColorOS",
					"com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
					"com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
					"com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
					"com.oplus.securitypermission" to "com.oplus.securitypermission.startup.StartupAppListActivity"
				)
			maker.contains("vivo") || maker.contains("iqoo") ->
				out += named(
					"Funtouch",
					"com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
					"com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
					"com.iqoo.secure" to "com.iqoo.secure.ui.appoptimize.BackgroundStartUpActivity"
				)
			maker.contains("samsung") ->
				out += named(
					"One UI",
					"com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity",
					"com.samsung.android.sm_cn" to "com.samsung.android.sm.ui.battery.BatteryActivity",
					"com.samsung.android.sm" to "com.samsung.android.sm.ui.battery.BatteryActivity"
				)
			maker.contains("meizu") ->
				out += named(
					"Flyme",
					"com.meizu.safe" to "com.meizu.safe.security.SHOW_APPSEC"
				)
			maker.contains("asus") ->
				out += named(
					"Asus",
					"com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity",
					"com.asus.mobilemanager" to "com.asus.mobilemanager.MainActivity"
				)
			maker.contains("sony") ->
				out += named(
					"Sony",
					"com.sonyericans.autostartmgr" to "com.sonyericans.autostartmgr.LoadingScreen"
				)
			maker.contains("letv") || maker.contains("leeco") || maker.contains("lenovo") ->
				out += named(
					"Lenovo",
					"com.letv.android.letvsafe" to "com.letv.android.letvsafe.AutobootManageActivity",
					"com.lenovo.security" to "com.lenovo.security.purebackground.PureBackgroundActivity"
				)
			maker.contains("zuk") || maker.contains("motorola") ->

				out += named(
					"Lenovo",
					"com.lenovo.security" to "com.lenovo.security.purebackground.PureBackgroundActivity"
				)
			maker.contains("htc") ->
				out += named(
					"HTC",
					"com.htc.pitroad" to "com.htc.pitroad.landingpage.activity.LandingPageActivity"
				)
			maker.contains("tecno") || maker.contains("itel") || maker.contains("infinix") ->
				out += named(
					"HiOS",
					"com.transsion.securitycenter" to "com.transsion.securitycenter.startupmanage.StartupManageActivity",
					"com.transsion.phoenix" to "com.transsion.phoenix.ui.activity.AppManagerActivity"
				)
		}
		out += Candidate(
			"Android",

			Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
		)
		return out
	}

	fun hasOwnScreen(): Boolean = candidates().size > 1

	fun open(context: Context): Boolean {
		for (candidate in candidates()) {
			try {
				start(context, candidate.intent)
				Tracker.action(context, "opened ${candidate.label}'s screen for what may run in the background")
				return true
			} catch (nothing: ActivityNotFoundException) {

			} catch (refused: SecurityException) {

			}
		}
		Log.i(TAG, "no battery screen on this phone answered")
		return false
	}

	fun appDetails(context: Context): Boolean = try {
		start(context, appDetailsIntent(context))
		true
	} catch (nothing: ActivityNotFoundException) {
		false
	} catch (refused: SecurityException) {
		false
	}

	fun appDetailsIntent(context: Context) =
		Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
		.setData(Uri.fromParts("package", context.packageName, null))

	private fun start(context: Context, intent: Intent) {

		if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		context.startActivity(intent)
	}

	private const val TAG = "freeeloquencetts"
}
