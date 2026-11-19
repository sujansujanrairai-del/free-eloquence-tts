package com.blindtechnexus.freeeloquencetts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
	onConfigure: () -> Unit,
	onOpenSystemSpeechSettings: () -> Boolean,
	onAbout: () -> Unit,
	onAudio: () -> Unit
) {

	val nothingOpensSpeechSettings = stringResource(R.string.main_system_unavailable)
	var notice by remember { mutableStateOf("") }
	Scaffold(
		topBar = { ScreenTopBar(title = stringResource(R.string.app_name)) }
	) { inset ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(inset)
				.verticalScroll(rememberScrollState())
				.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
		) {
			LiveNotice(text = notice)
			Text(
				text = stringResource(R.string.main_tagline),
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 24.dp)
			)

			Step(
				description = stringResource(R.string.main_configure_description),
				action = stringResource(R.string.main_configure_action),
				onClick = onConfigure
			)
			Step(
				description = stringResource(R.string.main_system_description),
				action = stringResource(R.string.main_system_action),
				onClick = {
					if (!onOpenSystemSpeechSettings()) notice = nothingOpensSpeechSettings
				}
			)
			Step(
				description = stringResource(R.string.main_about_description),
				action = stringResource(R.string.main_about_action),
				onClick = onAbout
			)
			Step(
				description = stringResource(R.string.main_audio_description),
				action = stringResource(R.string.main_audio_action),
				onClick = onAudio
			)
		}
	}
}

@Composable
private fun Step(description: String, action: String, onClick: () -> Unit) {
	Text(
		text = description,
		style = MaterialTheme.typography.bodyLarge,
		modifier = Modifier.fillMaxWidth()
	)
	Gap(12.dp)
	Button(
		onClick = onClick,
		modifier = Modifier.fillMaxWidth()
	) {
		Text(text = action, style = MaterialTheme.typography.labelLarge)
	}
	Gap(24.dp)
}
