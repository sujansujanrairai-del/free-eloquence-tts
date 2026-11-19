package com.blindtechnexus.freeeloquencetts

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

internal object ApkDownload {

	class Progress(val done: Long, val total: Long)

	sealed class Outcome {
		class Installed(val file: File, val size: Long) : Outcome()
		object Cancelled : Outcome()
		object Failed : Outcome()
	}

	fun fileFor(context: Context, version: String): File {
		val directory = File(context.cacheDir, DIRECTORY)
		runCatching { directory.mkdirs() }
		return File(directory, "alt-eloquence-tts-" + sanitize(version) + ".apk")
	}

	private fun sanitize(version: String): String =
		version.map { if (it.isLetterOrDigit() || it == '.' || it == '-' || it == '_') it else '-' }
			.joinToString("").take(40)

	fun to(
		context: Context,
		address: String,
		file: File,
		cancelled: AtomicBoolean,
		onProgress: (Progress) -> Unit
	): Outcome {
		runCatching { if (file.exists()) file.delete() }
		val connection = UpdateInfo.openStream(address) ?: return Outcome.Failed
		var written = 0L
		try {
			val code = connection.responseCode
			if (code !in 200..299) {
				Log.e(TAG, "the download answered $code")
				return Outcome.Failed
			}
			val declared = connection.contentLength
			val total = if (declared > 0) declared.toLong() else -1L
			connection.inputStream.use { input ->
				file.outputStream().buffered(BUFFER).use { output ->
					val buffer = ByteArray(BUFFER)
					var lastReport = 0L
					while (true) {
						if (cancelled.get()) {
							file.delete()
							return Outcome.Cancelled
						}
						val read = input.read(buffer)
						if (read < 0) break
						output.write(buffer, 0, read)
						written += read

						val now = System.currentTimeMillis()
						if (now - lastReport >= REPORT_MS || (total > 0 && written >= total)) {
							lastReport = now
							onProgress(Progress(written, total))
						}
					}
				}
			}
			if (total > 0 && written < total) {

				Log.e(TAG, "the download ended at $written of $total bytes")
				file.delete()
				return Outcome.Failed
			}
			if (!isAnApk(context, file)) {
				Log.e(TAG, "what came down is not a package this phone could install")
				file.delete()
				return Outcome.Failed
			}
			onProgress(Progress(written, if (total > 0) total else written))
			return Outcome.Installed(file, written)
		} catch (stopped: IOException) {
			Log.i(TAG, "the download was broken off", stopped)
			runCatching { file.delete() }
			return if (cancelled.get()) Outcome.Cancelled else Outcome.Failed
		} finally {
			runCatching { connection.disconnect() }
		}
	}

	private fun isAnApk(context: Context, file: File): Boolean = runCatching {
		context.packageManager.getPackageArchiveInfo(file.absolutePath, 0) != null
	}.getOrDefault(false)

	private const val TAG = "freeeloquencetts"
	private const val DIRECTORY = "update"
	private const val BUFFER = 64 * 1024
	private const val REPORT_MS = 120L
}
