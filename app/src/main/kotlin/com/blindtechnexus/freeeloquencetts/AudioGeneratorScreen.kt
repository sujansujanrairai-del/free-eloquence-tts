package com.blindtechnexus.freeeloquencetts

import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioGeneratorScreen(onBack: () -> Unit) {
	val context = LocalContext.current
	val generator = remember { AudioGenerator(context) }

	val settings = remember { Settings(context) }
	val turn = remember { AtomicInteger(0) }
	var input by remember { mutableStateOf("") }
	var notice by remember { mutableStateOf("") }
	var generating by remember { mutableStateOf(false) }
	var ready by remember { mutableStateOf<File?>(null) }

	var playback by remember { mutableStateOf(Playback.IDLE) }
	val player = remember { Player() }

	DisposableEffect(Unit) {
		onDispose {
			player.release()
			generator.cancel()
			settings.close()
		}
	}

	val saveFile = rememberLauncherForActivityResult(
		ActivityResultContracts.CreateDocument("audio/wav")
	) { uri ->
		val file = ready
		if (uri == null || file == null) return@rememberLauncherForActivityResult
		Thread {
			val saved = try {
				context.contentResolver.openOutputStream(uri)?.use { sink ->
					file.inputStream().use { it.copyTo(sink) }
					true
				} ?: false
			} catch (e: Exception) {
				false
			}
			(context as? android.app.Activity)?.runOnUiThread {
				val said = if (saved) {
					context.getString(R.string.audio_saved)
				} else {
					context.getString(R.string.audio_save_failed)
				}
				notice = said
				Toast.makeText(context, said, Toast.LENGTH_SHORT).show()
			}
		}.apply { isDaemon = true }.start()
	}

	fun stopPlayback() {
		player.release()
		playback = Playback.IDLE
	}

	fun startPlayback(file: File) {
		player.release()
		try {
			val sounding = MediaPlayer()
			sounding.setDataSource(file.absolutePath)
			sounding.setOnCompletionListener { playback = Playback.DONE }
			sounding.setOnErrorListener { _, _, _ ->
				player.release()
				playback = Playback.IDLE
				val said = context.getString(R.string.audio_play_failed)
				notice = said
				Toast.makeText(context, said, Toast.LENGTH_SHORT).show()
				true
			}
			sounding.prepare()
			sounding.start()
			player.media = sounding
			playback = Playback.PLAYING
		} catch (_: Throwable) {
			player.release()
			playback = Playback.IDLE
			val said = context.getString(R.string.audio_play_failed)
			notice = said
			Toast.makeText(context, said, Toast.LENGTH_SHORT).show()
		}
	}

	fun togglePlayback() {
		val file = ready ?: return
		when (playback) {
			Playback.PLAYING -> {
				try {
					player.media?.pause()
					playback = Playback.PAUSED
				} catch (_: Throwable) {
					startPlayback(file)
				}
			}
			Playback.PAUSED -> {
				try {
					player.media?.start()
					playback = Playback.PLAYING
				} catch (_: Throwable) {
					startPlayback(file)
				}
			}
			Playback.DONE -> {
				try {
					player.media?.seekTo(0)
					player.media?.start()
					playback = Playback.PLAYING
				} catch (_: Throwable) {
					startPlayback(file)
				}
			}
			Playback.IDLE -> startPlayback(file)
		}
	}

	fun generate() {
		if (generating) return

		stopPlayback()
		if (input.isBlank()) {
			val said = context.getString(R.string.audio_empty)
			notice = said
			Toast.makeText(context, said, Toast.LENGTH_SHORT).show()
			return
		}
		if (input.length > AudioGenerator.MAX_CHARS) {
			val said = context.getString(R.string.audio_too_long, AudioGenerator.MAX_CHARS)
			notice = said
			Toast.makeText(context, said, Toast.LENGTH_SHORT).show()
			return
		}
		val mine = turn.incrementAndGet()
		generating = true
		ready = null
		notice = context.getString(R.string.audio_generating, 0, input.length)
		val text = input
		generator.render(
			text = text,
			settings = settings,
			progress = { doneChars, totalChars ->
				val said = context.getString(R.string.audio_generating, doneChars, totalChars)
				(context as? android.app.Activity)?.runOnUiThread {
					if (mine == turn.get()) notice = said
				}
			},
			done = { file ->
				if (mine != turn.get()) return@render
				generating = false
				playback = Playback.IDLE
				ready = file
				val said = if (file != null) {
					context.getString(R.string.audio_ready)
				} else {
					context.getString(R.string.audio_failed)
				}
				notice = said
				Toast.makeText(context, said, Toast.LENGTH_SHORT).show()
			}
		)
	}

	fun share() {
		val file = ready ?: return
		try {
			val uri = FileProvider.getUriForFile(context, Installer.authority(context), file)
			val intent = Intent(Intent.ACTION_SEND).apply {
				type = "audio/wav"
				putExtra(Intent.EXTRA_STREAM, uri)
				addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			}
			context.startActivity(
				Intent.createChooser(intent, context.getString(R.string.audio_share))
			)
		} catch (e: Exception) {
			val said = context.getString(R.string.audio_save_failed)
			notice = said
			Toast.makeText(context, said, Toast.LENGTH_SHORT).show()
		}
	}

	Scaffold(
		topBar = {
			ScreenTopBar(
				title = stringResource(R.string.audio_title),
				onBack = onBack
			)
		}
	) { inset ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(inset)
				.verticalScroll(rememberScrollState())
				.padding(bottom = 24.dp)
		) {
			LiveNotice(text = notice, horizontal = 16.dp)
			SectionText(text = stringResource(R.string.audio_description))
			OutlinedTextField(
				value = input,
				onValueChange = { input = it },
				label = { Text(text = stringResource(R.string.audio_text_hint)) },
				keyboardOptions = KeyboardOptions(
					capitalization = KeyboardCapitalization.Sentences
				),
				minLines = 6,
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp)
			)
			Gap(12.dp)
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp)
			) {
				Button(
					onClick = ::generate,
					enabled = !generating,
					modifier = Modifier.fillMaxWidth()
				) {
					Text(
						text = stringResource(R.string.audio_generate),
						style = MaterialTheme.typography.labelLarge
					)
				}
				if (ready != null && !generating) {
					Gap(8.dp)
					OutlinedButton(
						onClick = ::togglePlayback,
						modifier = Modifier.fillMaxWidth()
					) {
						Text(
							text = stringResource(
								when (playback) {
									Playback.PLAYING -> R.string.audio_pause
									Playback.DONE -> R.string.audio_replay
									else -> R.string.audio_play
								}
							),
							style = MaterialTheme.typography.labelLarge
						)
					}
					Gap(8.dp)
					Row(modifier = Modifier.fillMaxWidth()) {
						OutlinedButton(
							onClick = ::share,
							modifier = Modifier.weight(1f)
						) {
							Text(
								text = stringResource(R.string.audio_share),
								style = MaterialTheme.typography.labelLarge
							)
						}
						Spacer(modifier = Modifier.width(8.dp))
						OutlinedButton(
							onClick = {
								saveFile.launch(context.getString(R.string.audio_file_name))
							},
							modifier = Modifier.weight(1f)
						) {
							Text(
								text = stringResource(R.string.audio_save),
								style = MaterialTheme.typography.labelLarge
							)
						}
					}
				}
			}
		}
	}
}

private enum class Playback {
	IDLE, PLAYING, PAUSED, DONE
}

private class Player {

	var media: MediaPlayer? = null

	fun release() {
		val sounding = media
		media = null
		if (sounding == null) return
		try {
			sounding.reset()
		} catch (_: Throwable) {
		}
		try {
			sounding.release()
		} catch (_: Throwable) {
		}
	}
}
