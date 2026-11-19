package com.blindtechnexus.freeeloquencetts

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.util.Locale

@Composable
internal fun UpdateDialogs() {
	val state = Updater.state
	val context = LocalContext.current
	val activity = context as? Activity

	var outcome by remember(state) { mutableStateOf<Updater.Outcome?>(null) }
	var acknowledged by remember(state) { mutableStateOf(false) }

	when (state) {
		is Updater.State.Available -> if (Updater.shouldShowDialog()) {
			AvailableDialog(state.version)
		}

		is Updater.State.Downloading -> DownloadingDialog(state)

		is Updater.State.Ready -> {

			LaunchedEffect(state) {
				activity?.let { outcome = Updater.install(it) }
			}
			ReadyDialog(outcome, acknowledged) { acknowledged = true }
		}

		else -> Unit
	}
}

@Composable
private fun AvailableDialog(version: String) {
	AlertDialog(
		onDismissRequest = { Updater.dismiss() },
		title = { Text(text = stringResource(R.string.update_available_title)) },
		text = {
			Column {
				Text(
					text = stringResource(R.string.update_available_message, version),
					style = MaterialTheme.typography.bodyLarge
				)
				Text(
					text = stringResource(R.string.update_download_question),
					style = MaterialTheme.typography.bodyLarge,
					modifier = Modifier.fillMaxWidth()
				)
			}
		},
		confirmButton = {
			TextButton(onClick = { Updater.download() }) {
				Text(text = stringResource(R.string.update_download))
			}
		},
		dismissButton = {
			TextButton(onClick = { Updater.ignore() }) {
				Text(text = stringResource(R.string.update_later))
			}
		}
	)
}

@Composable
private fun DownloadingDialog(state: Updater.State.Downloading) {
	val total = state.total
	val text = if (total > 0) {
		stringResource(
			R.string.update_progress_of,
			megabytes(state.done),
			megabytes(total)
		)
	} else {
		stringResource(R.string.update_progress_so_far, megabytes(state.done))
	}
	AlertDialog(
		onDismissRequest = { Updater.cancelDownload() },
		title = { Text(text = stringResource(R.string.update_downloading_title)) },
		text = {
			Column {
				Text(text = text, style = MaterialTheme.typography.bodyLarge)
				if (total > 0) {
					LinearProgressIndicator(
						progress = { Updater.fraction() },
						modifier = Modifier
							.fillMaxWidth()
							.padding(top = 12.dp)
					)
				} else {
					LinearProgressIndicator(
						modifier = Modifier
							.fillMaxWidth()
							.padding(top = 12.dp)
					)
				}
			}
		},
		confirmButton = {
			TextButton(onClick = { Updater.cancelDownload() }) {
				Text(text = stringResource(R.string.update_cancel))
			}
		}
	)
}

@Composable
private fun ReadyDialog(outcome: Updater.Outcome?, acknowledged: Boolean, onAcknowledge: () -> Unit) {
	if (acknowledged) return
	when (outcome) {
		Updater.Outcome.NeedsPermission -> AlertDialog(

			onDismissRequest = onAcknowledge,
			title = { Text(text = stringResource(R.string.update_permission_title)) },
			text = { Text(text = stringResource(R.string.update_permission_message)) },
			confirmButton = {
				TextButton(onClick = onAcknowledge) {
					Text(text = stringResource(R.string.update_ok))
				}
			}
		)

		Updater.Outcome.Refused, Updater.Outcome.NoFile -> AlertDialog(
			onDismissRequest = onAcknowledge,
			title = { Text(text = stringResource(R.string.update_ready_title)) },
			text = {
				Text(
					text = stringResource(
						if (outcome == Updater.Outcome.NoFile) R.string.update_nothing_to_install
						else R.string.update_refused
					)
				)
			},
			confirmButton = {
				TextButton(onClick = onAcknowledge) {
					Text(text = stringResource(R.string.update_ok))
				}
			}
		)

		else -> Unit
	}
}

internal fun megabytes(bytes: Long): String {
	val mb = bytes / (1024.0 * 1024.0)
	return if (bytes < 1024 * 100) {
		String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
	} else {
		String.format(Locale.getDefault(), "%.1f MB", mb)
	}
}
