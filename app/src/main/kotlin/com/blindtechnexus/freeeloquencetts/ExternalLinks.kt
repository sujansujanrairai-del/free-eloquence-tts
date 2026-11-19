package com.blindtechnexus.freeeloquencetts

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

internal fun startFirst(context: Context, candidates: List<Intent>): Boolean {

	val newTask = context !is Activity
	for (candidate in candidates) {
		try {
			if (newTask) candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			context.startActivity(candidate)
			return true
		} catch (nothing: ActivityNotFoundException) {

		} catch (refused: SecurityException) {

		}
	}
	return false
}

object ExternalLinks {

	const val WHATSAPP_NUMBER = "9779708340992"
	const val TELEGRAM_CHANNEL = "blindtechnexus"
	const val EMAIL_ADDRESS = "blindtechvisionary@gmail.com"

	fun whatsAppWays(): List<Intent> = listOf(
		view("https://wa.me/$WHATSAPP_NUMBER"),
		view("https://api.whatsapp.com/send?phone=$WHATSAPP_NUMBER"),
		view("whatsapp://send?phone=$WHATSAPP_NUMBER")
	)

	fun telegramWays(): List<Intent> = listOf(
		view("https://t.me/$TELEGRAM_CHANNEL"),
		view("tg://resolve?domain=$TELEGRAM_CHANNEL")
	)

	fun emailWays(subject: String): List<Intent> = listOf(
		Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$EMAIL_ADDRESS"))
			.putExtra(Intent.EXTRA_SUBJECT, subject),
		Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
			.putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL_ADDRESS))
			.putExtra(Intent.EXTRA_SUBJECT, subject)
	)

	fun whatsApp(context: Context): Boolean = startFirst(context, whatsAppWays())

	fun telegram(context: Context): Boolean = startFirst(context, telegramWays())

	fun email(context: Context, subject: String): Boolean =
		startFirst(context, emailWays(subject))

	private fun view(uri: String) = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
}
