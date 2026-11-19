package com.blindtechnexus.freeeloquencetts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

class KeepAliveRestarter : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != ACTION_RESTART) return

		if (!Settings.flag(context, Settings.KEY_PERSISTENT_NOTIFICATION, false)) {
			Log.i(TAG, "the restart came due with the notification turned off, so nothing started")
			return
		}
		val started = KeepAliveService.start(context)
		Tracker.action(context, "the keep-alive came back after the app was closed: $started")
	}

	companion object {

		private const val TAG = "freeeloquencetts"

		const val ACTION_RESTART = "com.blindtechnexus.freeeloquencetts.action.RESTART_KEEP_ALIVE"

		const val DELAY_MS = 4000L

		private const val REQUEST = 4711

		fun schedule(context: Context) {
			val alarm = context.getSystemService(AlarmManager::class.java) ?: return
			try {
				val at = SystemClock.elapsedRealtime() + DELAY_MS
				alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pending(context))
			} catch (refused: Throwable) {
				Log.w(TAG, "the restart could not be scheduled", refused)
			}
		}

		fun cancel(context: Context) {
			val alarm = context.getSystemService(AlarmManager::class.java) ?: return
			try {
				alarm.cancel(pending(context))
			} catch (_: Throwable) {

			}
		}

		private fun pending(context: Context): PendingIntent {
			val intent = Intent(context, KeepAliveRestarter::class.java).setAction(ACTION_RESTART)
			val flags = PendingIntent.FLAG_UPDATE_CURRENT or
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
			return PendingIntent.getBroadcast(context, REQUEST, intent, flags)
		}
	}
}
