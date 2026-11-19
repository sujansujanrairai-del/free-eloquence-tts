package com.blindtechnexus.freeeloquencetts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class EngineReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		when (intent.action) {
			ACTION_STOP -> stopSpeaking(context)
			Intent.ACTION_BOOT_COMPLETED,
			Intent.ACTION_LOCKED_BOOT_COMPLETED,
			Intent.ACTION_MY_PACKAGE_REPLACED -> afterBoot(context)
			else -> Unit
		}
	}

	private fun afterBoot(context: Context) {
		if (!Settings.flag(context, Settings.KEY_START_ON_BOOT, false)) return
		if (!Settings.flag(context, Settings.KEY_PERSISTENT_NOTIFICATION, false)) {

			Log.i(TAG, "start on boot is on but the persistent notification is not, so nothing started")
			return
		}
		KeepAliveService.start(context)
		Tracker.action(context, "the keep-alive came back after a restart")
	}

	private fun stopSpeaking(context: Context) {
		val service = EvvTtsService.instance
		if (service == null) {

			Log.i(TAG, "asked for silence with no engine running")
			return
		}
		service.stopSpeakingNow()
		Tracker.action(context, "silence asked for from the notification")
	}

	companion object {

		private const val TAG = "freeeloquencetts"

		const val ACTION_STOP = "com.blindtechnexus.freeeloquencetts.action.STOP_SPEECH"
	}
}
