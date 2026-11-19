package com.blindtechnexus.freeeloquencetts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log

object KeepAliveNotification {

	const val CHANNEL_ID = "keep_alive"
	const val ID = 1

	fun ensureChannel(context: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
		val manager = context.getSystemService(NotificationManager::class.java) ?: return
		val channel = NotificationChannel(
			CHANNEL_ID,
			context.getString(R.string.keep_alive_channel),
			NotificationManager.IMPORTANCE_LOW
		)
		channel.description = context.getString(R.string.keep_alive_channel_description)
		channel.setShowBadge(false)
		manager.createNotificationChannel(channel)
	}

	fun build(context: Context, withStop: Boolean): Notification {
		val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Notification.Builder(context, CHANNEL_ID)
		} else {
			@Suppress("DEPRECATION")
			Notification.Builder(context)
		}
		builder
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.setContentTitle(context.getString(R.string.keep_alive_title))
			.setContentText(context.getString(R.string.keep_alive_text))
			.setOngoing(true)
			.setShowWhen(false)
			.setContentIntent(openSettings(context))
		if (withStop) {
			builder.addAction(
				Notification.Action.Builder(
					null,
					context.getString(R.string.keep_alive_stop),
					stopSpeech(context)
				).build()
			)
		}
		return builder.build()
	}

	fun claim(service: Service, withStop: Boolean): Boolean = try {
		val notification = build(service, withStop)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			service.startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
		} else {
			service.startForeground(ID, notification)
		}
		true
	} catch (refused: SecurityException) {
		Log.e(TAG, "the system refused the keep-alive notification", refused)
		false
	} catch (refused: IllegalStateException) {

		Log.e(TAG, "the system would not let this be a foreground service", refused)
		false
	}

	fun release(service: Service, detach: Boolean) {
		runCatching {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				service.stopForeground(
					if (detach) Service.STOP_FOREGROUND_DETACH else Service.STOP_FOREGROUND_REMOVE
				)
			} else {

				@Suppress("DEPRECATION")
				service.stopForeground(true)
			}
		}
	}

	fun isPosted(context: Context): Boolean {
		val manager = context.getSystemService(NotificationManager::class.java) ?: return false
		return manager.activeNotifications.any { it.id == ID }
	}

	fun notificationsAllowed(context: Context): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
		return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
			PackageManager.PERMISSION_GRANTED
	}

	fun cancel(context: Context) {
		val manager = context.getSystemService(NotificationManager::class.java) ?: return
		runCatching { manager.cancel(ID) }
	}

	private fun openSettings(context: Context): PendingIntent {
		val intent = Intent(context, SettingsActivity::class.java)
			.putExtra(SettingsActivity.EXTRA_SCREEN, SettingsActivity.SCREEN_BATTERY)
			.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
		return PendingIntent.getActivity(context, REQUEST_OPEN, intent, flags())
	}

	private fun stopSpeech(context: Context): PendingIntent {
		val intent = Intent(context, EngineReceiver::class.java).setAction(EngineReceiver.ACTION_STOP)
		return PendingIntent.getBroadcast(context, REQUEST_STOP, intent, flags())
	}

	private fun flags(): Int {
		var want = PendingIntent.FLAG_UPDATE_CURRENT
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) want = want or PendingIntent.FLAG_IMMUTABLE
		return want
	}

	private const val TAG = "freeeloquencetts"
	private const val REQUEST_OPEN = 20
	private const val REQUEST_STOP = 21
}
