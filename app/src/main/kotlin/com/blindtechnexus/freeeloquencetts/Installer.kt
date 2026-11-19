package com.blindtechnexus.freeeloquencetts

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

internal object Installer {

	const val AUTHORITY_SUFFIX = ".fileprovider"

	fun authority(context: Context): String = context.packageName + AUTHORITY_SUFFIX

	fun canRequest(context: Context): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
		return runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(true)
	}

	fun askToAllow(context: Context): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
		val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
			.setData(Uri.fromParts("package", context.packageName, null))
		return try {
			start(context, intent)
			true
		} catch (nothing: ActivityNotFoundException) {
			Log.e(TAG, "nothing here asks about installing unknown apps")
			false
		} catch (refused: SecurityException) {
			Log.e(TAG, "the system would not say about installing unknown apps", refused)
			false
		}
	}

	fun install(activity: Activity, file: File): Boolean {
		val uri = try {
			FileProvider.getUriForFile(activity, authority(activity), file)
		} catch (notShared: IllegalArgumentException) {

			Log.e(TAG, "the update file is not where the file provider publishes", notShared)
			return false
		}
		val intent = Intent(Intent.ACTION_VIEW)
			.setDataAndType(uri, "application/vnd.android.package-archive")
			.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		return try {
			activity.startActivity(intent)
			Tracker.action(activity, "handed an update to the installer")
			true
		} catch (nothing: ActivityNotFoundException) {

			Log.e(TAG, "nothing on this device installs packages")
			false
		} catch (refused: SecurityException) {

			Log.e(TAG, "the installer was not given the file", refused)
			false
		}
	}

	fun blockedOnPermission(context: Context): Boolean = !canRequest(context)

	fun isInstallable(context: Context, file: File): Boolean =
		archiveVersion(context, file).isNotEmpty()

	fun archiveVersion(context: Context, file: File): String = runCatching {
		context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.versionName.orEmpty()
	}.getOrDefault("")

	private fun start(context: Context, intent: Intent) {
		if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		context.startActivity(intent)
	}

	private const val TAG = "freeeloquencetts"
}
