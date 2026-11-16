package com.blindtechnexus.freeeloquencetts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * The six pages somebody reads before they trust a voice with their phone.
 *
 * It used to be one long page that listed the app's features, which is not what
 * a welcome screen is for: a feature list is a reference, and a person meeting
 * an app for the first time does not want a reference, they want to know what
 * it will do for them and what it will ask of them. So it is six pages of one
 * idea each, with a Next and a Back that say what they do, and a count that says
 * where in the six the reader is.
 *
 * The navigation is deliberately two buttons rather than a swipe: a screen
 * reader can reach a button, and a swipe gesture belongs to the screen reader
 * before it belongs to the page. Back is absent on the first page rather than
 * disabled, because a stop that cannot be used is a stop to go past.
 *
 * The last page is where the permissions are explained, because the last page is
 * the one before they are asked for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(onContinue: () -> Unit, revisit: Boolean = false) {
	var page by rememberSaveable { mutableStateOf(0) }
	val pages = listOf(
		Page(
			icon = Icons.Filled.RecordVoiceOver,
			title = R.string.welcome_p1_title,
			body = R.string.welcome_p1_body
		),
		Page(
			icon = Icons.Filled.Verified,
			title = R.string.welcome_p2_title,
			body = R.string.welcome_p2_body
		),
		Page(
			icon = Icons.Filled.Language,
			title = R.string.welcome_p3_title,
			body = R.string.welcome_p3_body
		),
		Page(
			icon = Icons.Filled.Web,
			title = R.string.welcome_p4_title,
			body = R.string.welcome_p4_body
		),
		Page(
			icon = Icons.Filled.Tune,
			title = R.string.welcome_p5_title,
			body = R.string.welcome_p5_body
		),
		Page(
			icon = Icons.Filled.Notifications,
			title = R.string.welcome_p6_title,
			body = R.string.welcome_p6_body
		)
	)
	val last = pages.lastIndex
	val shown = pages[page.coerceIn(0, last)]
	Scaffold(
		topBar = { ScreenTopBar(title = stringResource(R.string.app_name)) }
	) { inset ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(inset)
				.padding(horizontal = 24.dp)
		) {
			Column(
				modifier = Modifier
					.weight(1f)
					.verticalScroll(rememberScrollState())
					.padding(top = 8.dp)
			) {
				Text(
					text = stringResource(R.string.welcome_page_of, page + 1, pages.size),
					style = MaterialTheme.typography.labelLarge,
					modifier = Modifier
						.fillMaxWidth()
						.padding(bottom = 24.dp)
				)
				Icon(
					imageVector = shown.icon,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary,
					modifier = Modifier.size(56.dp)
				)
				Text(
					text = stringResource(shown.title),
					style = MaterialTheme.typography.headlineSmall,
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 20.dp, bottom = 12.dp)
						.semantics { heading() }
				)
				Paragraph(text = stringResource(shown.body))
				if (page == last) {
					// The permissions are asked for on the way past this page
					// the first time; read again from the main screen, there is
					// nothing coming next to warn about.
					if (!revisit) Paragraph(text = stringResource(R.string.welcome_permissions))
					Paragraph(text = stringResource(R.string.welcome_after))
				}
			}
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 16.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				if (page > 0) {
					OutlinedButton(
						onClick = { page = (page - 1).coerceAtLeast(0) },
						modifier = Modifier
							.weight(1f)
							.padding(end = 8.dp)
					) {
						Text(
							text = stringResource(R.string.welcome_back),
							style = MaterialTheme.typography.labelLarge
						)
					}
				}
				Button(
					onClick = {
						if (page < last) page++ else onContinue()
					},
					modifier = Modifier.weight(1f)
				) {
					Text(
						text = if (page < last) {
							stringResource(R.string.welcome_next)
						} else if (revisit) {
							stringResource(R.string.welcome_back_to_main)
						} else {
							stringResource(R.string.welcome_continue)
						},
						style = MaterialTheme.typography.labelLarge
					)
				}
			}
		}
	}
}

/** One page: a picture that says nothing, a title that says everything, and a
 *  paragraph under it. The icon is silent to a screen reader on purpose. */
private class Page(val icon: ImageVector, val title: Int, val body: Int)
