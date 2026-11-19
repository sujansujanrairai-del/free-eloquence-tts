package com.blindtechnexus.freeeloquencetts

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

internal object UpdateInfo {

	const val CHANNEL = "https://raw.githubusercontent.com/blind-tech-nexus/remote-data-json/main/alt-eloquence-tts-update.json"

	private const val CONNECT_TIMEOUT_MS = 10000
	private const val READ_TIMEOUT_MS = 15000

	class Available(val version: String, val downloadUrl: String)

	fun parse(json: String, current: String): Available? {
		val published = runCatching { JSONObject(json) }.getOrNull() ?: return null
		val version = published.optString("version").trim()
		if (version.isEmpty()) return null
		if (version == current.trim()) return null
		val download = published.optString("url").trim()
		if (download.isEmpty()) return null
		if (!download.startsWith("https://")) {

			Log.e(TAG, "the published download address is not https, so it is not used")
			return null
		}
		return Available(version, download)
	}

	fun fetch(current: String, cancelled: AtomicBoolean): Result {

		if (cancelled.get()) return Result.Cancelled
		return try {
			val body = get(CHANNEL) ?: return Result.Unreachable
			if (cancelled.get()) return Result.Cancelled
			val found = parse(body, current)
			if (found == null) Result.UpToDate else Result.Found(found)
		} catch (failed: Exception) {
			Log.i(TAG, "nothing could be learned about updates", failed)
			Result.Unreachable
		}
	}

	sealed class Result {
		class Found(val available: Available) : Result()
		object UpToDate : Result()
		object Unreachable : Result()
		object Cancelled : Result()
	}

	private fun get(address: String): String? {
		var connection: HttpURLConnection? = null
		return try {
			connection = (URL(address).openConnection() as HttpURLConnection).apply {
				connectTimeout = CONNECT_TIMEOUT_MS
				readTimeout = READ_TIMEOUT_MS
				instanceFollowRedirects = true
				setRequestProperty("User-Agent", "alt-eloquence-tts")
				setRequestProperty("Accept", "application/json")
			}
			val code = connection.responseCode
			if (code !in 200..299) {
				Log.i(TAG, "the release channel answered $code")
				return null
			}
			connection.inputStream.use { input ->
				val out = java.io.ByteArrayOutputStream(MAX_BODY)
				val buffer = ByteArray(8 * 1024)
				var total = 0
				while (true) {
					val read = input.read(buffer)
					if (read < 0) break
					out.write(buffer, 0, read)
					total += read
					if (total > MAX_BODY) {
						Log.e(TAG, "the release channel sent more than a small file's worth")
						return null
					}
				}
				out.toString("UTF-8")
			}
		} catch (nothingThere: java.io.IOException) {
			Log.i(TAG, "the release channel could not be reached")
			null
		} finally {
			connection?.disconnect()
		}
	}

	fun openStream(address: String): HttpURLConnection? {
		if (!address.startsWith("https://")) return null
		return try {
			(URL(address).openConnection() as HttpURLConnection).apply {
				connectTimeout = CONNECT_TIMEOUT_MS
				readTimeout = READ_TIMEOUT_MS
				instanceFollowRedirects = true
				setRequestProperty("User-Agent", "alt-eloquence-tts")
			}
		} catch (nothingThere: java.io.IOException) {
			Log.i(TAG, "the download could not be started")
			null
		}
	}

	private const val TAG = "freeeloquencetts"

	private const val MAX_BODY = 64 * 1024
}
