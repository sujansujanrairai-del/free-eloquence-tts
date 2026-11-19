package com.blindtechnexus.freeeloquencetts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object BatteryExemption {

	fun isGranted(context: Context): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
		val power = context.getSystemService(PowerManager::class.java) ?: return true
		return runCatching { power.isIgnoringBatteryOptimizations(context.packageName) }
			.getOrDefault(false)
	}

	fun ways(context: Context): List<Intent> = listOf(
		Intent(
			Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
			Uri.parse("package:${context.packageName}")
		),
		Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
	)

	fun request(context: Context): Boolean = startFirst(context, ways(context))
}
