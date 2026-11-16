package com.blindtechnexus.freeeloquencetts

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * What the app is, who made it and how to reach them.
 *
 * Written as a list of short statements rather than paragraphs, because this is
 * a screen somebody opens with a question -- is it free, does it send anything,
 * who do I tell when it breaks -- and a paragraph is a thing to listen to in
 * order to find the answer in it. Each statement is one line on the screen and
 * one stop for a reader, and the headings say what the group of them is.
 *
 * The contact buttons say what they open and nothing else. The description of
 * each is the stop before it, which is where a reader who does not know what
 * WhatsApp is finds out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
	val context = LocalContext.current
	val subject = stringResource(R.string.about_email_subject)
	val noWhatsApp = stringResource(
		R.string.about_unavailable, stringResource(R.string.about_whatsapp_action)
	)
	val noTelegram = stringResource(
		R.string.about_unavailable, stringResource(R.string.about_telegram_action)
	)
	val noEmail = stringResource(
		R.string.about_unavailable, stringResource(R.string.about_email_action)
	)
	var notice by remember { mutableStateOf("") }
	Scaffold(
		topBar = {
			ScreenTopBar(
				title = stringResource(R.string.about_title),
				onBack = onBack
			)
		}
	) { inset ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(inset)
				.verticalScroll(rememberScrollState())
				.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
		) {
			LiveNotice(text = notice)

			SectionHeading(text = stringResource(R.string.about_what_heading), horizontal = 0.dp)
			Plain(text = stringResource(R.string.about_what_voice))
			Plain(text = stringResource(R.string.about_what_free))
			Plain(text = stringResource(R.string.about_what_private))
			Plain(text = stringResource(R.string.about_made_by))

			SectionHeading(text = stringResource(R.string.about_contact_heading), horizontal = 0.dp)
			Plain(text = stringResource(R.string.about_contact_hint))
			Contact(
				description = stringResource(R.string.about_contact_whatsapp),
				action = stringResource(R.string.about_whatsapp_action),
				onOpen = { ExternalLinks.whatsApp(context) },
				onRefused = { notice = noWhatsApp }
			)
			Contact(
				description = stringResource(R.string.about_contact_telegram),
				action = stringResource(R.string.about_telegram_action),
				onOpen = { ExternalLinks.telegram(context) },
				onRefused = { notice = noTelegram }
			)
			Contact(
				description = stringResource(R.string.about_contact_email),
				action = stringResource(R.string.about_email_action),
				onOpen = { ExternalLinks.email(context, subject) },
				onRefused = { notice = noEmail }
			)

			SectionHeading(text = stringResource(R.string.about_updates_heading), horizontal = 0.dp)
			Plain(text = stringResource(R.string.about_update_line))
			var manualCheck by remember { mutableStateOf(false) }
			val currentState = Updater.state
			val currentNotice = Updater.notice
			LaunchedEffect(currentState, currentNotice) {
				if (!manualCheck) return@LaunchedEffect
				val isLatest = currentNotice == Updater.Notice.UP_TO_DATE ||
					currentState is Updater.State.UpToDate
				if (isLatest) {
					Toast.makeText(
						context,
						context.getString(R.string.about_update_answer_current, BuildConfig.VERSION_NAME),
						Toast.LENGTH_SHORT
					).show()
					manualCheck = false
				} else if (currentState is Updater.State.Unreachable || currentState is Updater.State.Failed) {
					manualCheck = false
				}
			}
			ActionRow(
				label = stringResource(R.string.about_update_check),
				description = stringResource(R.string.about_update_check_description),
				onClick = {
					manualCheck = true
					Updater.check(force = true)
					val now = Updater.state
					val noticeNow = Updater.notice
					if (now is Updater.State.UpToDate || noticeNow == Updater.Notice.UP_TO_DATE) {
						Toast.makeText(
							context,
							context.getString(R.string.about_update_answer_current, BuildConfig.VERSION_NAME),
							Toast.LENGTH_SHORT
						).show()
						manualCheck = false
					}
				}
			)

			SectionHeading(text = stringResource(R.string.about_build_heading), horizontal = 0.dp)
			Plain(text = stringResource(R.string.about_version_line, BuildConfig.VERSION_NAME))
		}
	}
}

/** One statement, which is a paragraph with nothing after it: the headings are
 *  what separate one group of them from the next. */
@Composable
private fun Plain(text: String) {
	Text(
		text = text,
		style = MaterialTheme.typography.bodyLarge,
		modifier = Modifier
			.fillMaxWidth()
			.padding(bottom = 8.dp)
	)
}

@Composable
private fun Contact(
	description: String,
	action: String,
	onOpen: () -> Boolean,
	onRefused: () -> Unit
) {
	Text(
		text = description,
		style = MaterialTheme.typography.bodyMedium,
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 8.dp)
	)
	Gap(8.dp)
	OutlinedButton(
		onClick = { if (!onOpen()) onRefused() },
		modifier = Modifier.fillMaxWidth()
	) {
		Text(text = action, style = MaterialTheme.typography.labelLarge)
	}
	Gap(16.dp)
}
