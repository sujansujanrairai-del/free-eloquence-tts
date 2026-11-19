package com.blindtechnexus.freeeloquencetts

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat

class KeepAliveService : Service() {

	private var active = false

	override fun onCreate() {
		super.onCreate()
		active = true
		isRunning = true
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		KeepAliveNotification.ensureChannel(this)

		if (!Settings.flag(this, Settings.KEY_PERSISTENT_NOTIFICATION, false)) {
			stopAndQuit()
			return START_NOT_STICKY
		}
		if (!claim()) {

			stopAndQuit()
			return START_NOT_STICKY
		}

		KeepAliveRestarter.cancel(this)
		return START_STICKY
	}

	override fun onTaskRemoved(rootIntent: Intent?) {
		super.onTaskRemoved(rootIntent)
		if (!Settings.flag(this, Settings.KEY_PERSISTENT_NOTIFICATION, false)) return
		KeepAliveRestarter.schedule(this)
		Tracker.action(this, "the app was closed with the notification on; the keep-alive will be asked for again")
	}

	override fun onDestroy() {
		super.onDestroy()
		active = false
		isRunning = false
		KeepAliveNotification.release(this, detach = false)
	}

	override fun onBind(intent: Intent?): IBinder? = null

	val holding: Boolean get() = active

	private fun claim(): Boolean = KeepAliveNotification.claim(
		this,
		Settings.flag(this, Settings.KEY_NOTIFICATION_STOP, true)
	)

	private fun stopAndQuit() {
		KeepAliveRestarter.cancel(this)
		KeepAliveNotification.cancel(this)
		stopSelf()
	}

	companion object {

		private const val TAG = "freeeloquencetts"

		@Volatile
		var isRunning = false
			private set

		fun start(context: Context): Boolean = try {
			ContextCompat.startForegroundService(context, intent(context))
			true
		} catch (refused: IllegalStateException) {

			Log.w(TAG, "the system would not let the keep-alive start", refused)
			false
		} catch (refused: SecurityException) {
			Log.w(TAG, "the keep-alive was refused", refused)
			false
		}

		fun stop(context: Context) {
			KeepAliveRestarter.cancel(context)
			runCatching { context.stopService(intent(context)) }
		}

		private fun intent(context: Context) = Intent(context, KeepAliveService::class.java)
	}
}
