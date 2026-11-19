package com.blindtechnexus.freeeloquencetts

import android.app.Service
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

class KeepAlive(private val service: Service) {

	private val handler = Handler(Looper.getMainLooper())

	private var wanted = false

	fun sync(enabled: Boolean) {
		if (enabled == wanted) return
		wanted = enabled
		if (enabled) {
			KeepAlive.ensureChannel(service)
			KeepAliveService.start(service)
		} else {
			KeepAliveService.stop(service)

			handler.post {
				if (!wanted) KeepAliveNotification.cancel(service)
			}
		}
	}

	fun isWanted(): Boolean = wanted

	fun release() {
		wanted = false
		handler.removeCallbacksAndMessages(null)
	}

	companion object {
		const val TAG = "freeeloquencetts"
		const val CHANNEL_ID = KeepAliveNotification.CHANNEL_ID
		const val ID = KeepAliveNotification.ID

		const val CHECK_MS = 0L

		private val handler = Handler(Looper.getMainLooper())

		fun sync(context: Context, enabled: Boolean) {
			KeepAlive.ensureChannel(context)
			if (enabled) {
				KeepAliveService.start(context)

				handler.postDelayed({
					if (enabled && !KeepAliveService.isRunning) {
						Log.w(TAG, "the keep-alive service would not start; the voice is unprotected until the engine speaks")
					}
				}, 1000L)
			} else {
				KeepAliveService.stop(context)
				KeepAliveNotification.cancel(context)
			}
		}

		fun ensureChannel(context: Context) = KeepAliveNotification.ensureChannel(context)
	}
}

object Tracker {

	const val TAG = "freeeloquencetts-actions"

	fun action(settings: Settings?, what: String): Boolean {
		if (settings == null || !settings.trackActions) return false
		Log.i(TAG, what)
		return true
	}

	fun action(context: Context, what: String) {
		if (!Settings.flag(context, Settings.KEY_TRACK_ACTIONS, false)) return
		Log.i(TAG, what)
	}
}
