package com.blindtechnexus.freeeloquencetts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

/**
 * What brings the notification back after the app has been swiped away.
 *
 * A foreground service survives a lot, and what it does not survive is the
 * person removing the app from the recent list: Android stops the service with
 * the task, and a voice that has been keeping itself alive for a screen reader
 * is gone without a word being said about it. Turning the task removal into a
 * stop is the system's decision and not this app's, but asking for the service
 * again a moment later is allowed, and it is what every keep-alive of this shape
 * does.
 *
 * An alarm rather than a direct start, for two reasons. Starting a foreground
 * service from the background is refused on Android 12 and later unless
 * something with an exemption is asking, and an alarm that the app set is one of
 * the cases the platform still allows. And the delay is not a delay to be
 * ashamed of: the person has just closed the app, and a notification that
 * reappears in the same frame is a notification that argues. A few seconds later
 * it is the app doing what its setting says.
 *
 * `setAndAllowWhileIdle` rather than `setExactAndAllowWhileIdle`: the exact
 * alarm is a permission (`SCHEDULE_EXACT_ALARM`) and a promise this feature does
 * not need to keep. A restart that arrives a minute late is a voice that comes
 * back; an alarm that throws because the permission was refused is a voice that
 * does not.
 */
class KeepAliveRestarter : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != ACTION_RESTART) return
		// The switch is the whole of the answer, read again rather than
		// assumed: it may have been turned off in the seconds since the alarm
		// was set, and bringing a notification back that has been refused is
		// the one thing the battery screen promises never to do.
		if (!Settings.flag(context, Settings.KEY_PERSISTENT_NOTIFICATION, false)) {
			Log.i(TAG, "the restart came due with the notification turned off, so nothing started")
			return
		}
		val started = KeepAliveService.start(context)
		Tracker.action(context, "the keep-alive came back after the app was closed: $started")
	}

	companion object {

		private const val TAG = "freeeloquencetts"

		/** What the alarm sends. Named after this app because the intent is
		 *  built with a component on it rather than as a question. */
		const val ACTION_RESTART = "com.blindtechnexus.freeeloquencetts.action.RESTART_KEEP_ALIVE"

		/** How long after the task is removed the service is asked for again.
		 *  Long enough not to be an argument, short enough that the phone has
		 *  not gone back to sleep in between. */
		const val DELAY_MS = 4000L

		private const val REQUEST = 4711

		/** Sets the alarm that will ask for the service again. Idempotent: the
		 *  pending intent is updated in place, so asking twice a second apart
		 *  leaves one alarm rather than two. */
		fun schedule(context: Context) {
			val alarm = context.getSystemService(AlarmManager::class.java) ?: return
			try {
				val at = SystemClock.elapsedRealtime() + DELAY_MS
				alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pending(context))
			} catch (refused: Throwable) {
				Log.w(TAG, "the restart could not be scheduled", refused)
			}
		}

		/** Takes the alarm away, for the switch being turned off and for the
		 *  service coming back by itself: a restart that is no longer wanted
		 *  is a notification the person did not ask for. */
		fun cancel(context: Context) {
			val alarm = context.getSystemService(AlarmManager::class.java) ?: return
			try {
				alarm.cancel(pending(context))
			} catch (_: Throwable) {
				// Nothing to cancel is not a failure.
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
