package com.blindtechnexus.freeeloquencetts

import android.app.Activity
import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal object Updater {

	sealed class State {
		object Idle : State()
		object Checking : State()
		object UpToDate : State()
		object Unreachable : State()

		class Available(val version: String, val downloadUrl: String) : State()

		class Downloading(val done: Long, val total: Long) : State()

		class Ready(val file: File, val size: Long) : State()
		object Failed : State()

		object Dismissed : State()
	}

	var state by mutableStateOf<State>(State.Idle)
		private set

	var notice by mutableStateOf(Notice.NONE)
		private set

	enum class Notice { NONE, UP_TO_DATE, UNREACHABLE }

	var ignored: String? = null
		private set

	private val busy = AtomicBoolean(false)
	private val downloading = AtomicBoolean(false)
	private val cancelled = AtomicBoolean(false)

	private lateinit var appContext: Context
	private var askedThisLaunch = false

	fun attach(context: Context) {
		if (::appContext.isInitialized) return
		appContext = context.applicationContext
	}

	fun check(force: Boolean = false) {
		if (!::appContext.isInitialized) return
		if (!force) {
			if (askedThisLaunch) return
			askedThisLaunch = true
		}
		if (!busy.compareAndSet(false, true)) return

		cancelled.set(false)
		if (!force && state is State.Available) {
			busy.set(false)
			return
		}
		if (state !is State.Ready) state = State.Checking
		notice = Notice.NONE
		if (force) {

			ignored = null
		}
		val thread = Thread({
			val answer = try {
				UpdateInfo.fetch(BuildConfig.VERSION_NAME, cancelled)
			} catch (crash: Throwable) {

				Log.e(TAG, "the update check went wrong", crash)
				UpdateInfo.Result.Unreachable
			}
			busy.set(false)
			state = when (answer) {
				is UpdateInfo.Result.Found -> {
					val cached = ApkDownload.fileFor(appContext, answer.available.version)
					if (cached.exists() && Installer.isInstallable(appContext, cached) &&
						(force || state is State.Ready)
					) {

						State.Ready(cached, cached.length())
					} else {
						State.Available(answer.available.version, answer.available.downloadUrl)
					}
				}
				UpdateInfo.Result.UpToDate -> {
					if (force) notice = Notice.UP_TO_DATE
					State.UpToDate
				}
				else -> {
					if (force) notice = Notice.UNREACHABLE
					State.Unreachable
				}
			}
		}, "evv-update")
		thread.isDaemon = true
		thread.start()
	}

	fun ignore() {
		ignored = (state as? State.Available)?.version
		state = State.Dismissed
	}

	fun shouldShowDialog(): Boolean = when (val now = state) {
		is State.Available -> now.version != ignored
		is State.Downloading, is State.Ready -> true
		else -> false
	}

	fun download(): Boolean {
		val available = state as? State.Available ?: return false
		if (!downloading.compareAndSet(false, true)) return false
		cancelled.set(false)
		val file = ApkDownload.fileFor(appContext, available.version)
		if (file.exists() && Installer.isInstallable(appContext, file)) {

			downloading.set(false)
			state = State.Ready(file, file.length())
			return true
		}
		state = State.Downloading(0, -1)
		val thread = Thread({
			val outcome = try {
				ApkDownload.to(appContext, available.downloadUrl, file, cancelled) { progress ->
					state = State.Downloading(progress.done, progress.total)
				}
			} catch (crash: Throwable) {
				Log.e(TAG, "the download went wrong", crash)
				ApkDownload.Outcome.Failed
			}
			downloading.set(false)
			state = when (outcome) {
				is ApkDownload.Outcome.Installed -> State.Ready(outcome.file, outcome.size)
				ApkDownload.Outcome.Cancelled -> State.Dismissed
				else -> State.Failed
			}
		}, "evv-download")
		thread.isDaemon = true
		thread.start()
		return true
	}

	fun cancelDownload() {
		cancelled.set(true)
		downloading.set(false)
		(state as? State.Ready)?.let { runCatching { it.file.delete() } }
		if (state is State.Downloading) state = State.Dismissed
	}

	fun install(activity: Activity): Outcome {
		val ready = state as? State.Ready ?: return Outcome.NoFile
		if (Installer.blockedOnPermission(activity)) {
			return if (Installer.askToAllow(activity)) Outcome.NeedsPermission else Outcome.Refused
		}
		return if (Installer.install(activity, ready.file)) Outcome.Started else Outcome.Refused
	}

	sealed class Outcome {
		object Started : Outcome()
		object NeedsPermission : Outcome()
		object NoFile : Outcome()
		object Refused : Outcome()
	}

	fun fraction(): Float {
		val now = state as? State.Downloading ?: return 0f
		if (now.total <= 0) return 0f
		return (now.done.toFloat() / now.total.toFloat()).coerceIn(0f, 1f)
	}

	val currentVersion: String get() = BuildConfig.VERSION_NAME

	fun dismiss() {
		when (state) {
			is State.Downloading, is State.Ready -> Unit
			else -> state = State.Dismissed
		}
	}

	private const val TAG = "freeeloquencetts"
}
