package com.blindtechnexus.freeeloquencetts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

object SynthesisRouting {

	const val AUTOMATIC = 0
	const val ACCESSIBILITY = 1
	const val MEDIA = 2
	const val ALL = 3
	const val ALARMS = 4

	val CHOICES = listOf(AUTOMATIC, MEDIA, ALARMS, ACCESSIBILITY, ALL)

	fun clamp(route: Int): Int = if (CHOICES.contains(route)) route else AUTOMATIC

	fun playsItself(route: Int): Boolean = clamp(route) != AUTOMATIC

	fun handsToFramework(route: Int): Boolean = clamp(route) == AUTOMATIC || clamp(route) == ALL

	fun usageOf(route: Int): Int = when (clamp(route)) {
		MEDIA -> AudioAttributes.USAGE_MEDIA

		ALARMS -> AudioAttributes.USAGE_ALARM
		else -> AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
	}

	class Player {
		@Volatile
		private var track: AudioTrack? = null
		private var rateHz = 0
		private var usage = -1
		private var framesWritten = 0L
		private var headAtBegin = 0L

		@Volatile
		private var cut = false

		@Synchronized
		fun begin(rateHz: Int, usage: Int, volume: Float): Boolean {
			cut = false
			var held = track
			if (held == null || this.rateHz != rateHz || this.usage != usage || held.state != AudioTrack.STATE_INITIALIZED) {
				release()
				held = make(rateHz, usage) ?: return false
				track = held
				this.rateHz = rateHz
				this.usage = usage
			}
			framesWritten = 0

			headAtBegin = try {
				held.playbackHeadPosition.toLong() and 0xFFFFFFFFL
			} catch (_: Throwable) {
				0L
			}
			try {
				held.setVolume(volume.coerceIn(0f, 1f))
			} catch (_: Throwable) {
			}
			try {
				if (held.playState != AudioTrack.PLAYSTATE_PLAYING) held.play()
			} catch (broken: Throwable) {
				Log.e(TAG, "the player would not start", broken)
				release()
				return false
			}
			return true
		}

		fun write(buffer: ByteArray, n: Int): Boolean {
			val held = track ?: return false
			var at = 0
			while (at < n && !cut) {
				val wrote = try {
					held.write(buffer, at, n - at)
				} catch (_: Throwable) {
					if (at > 0) synchronized(this) { if (!cut) framesWritten += (at / BYTES_PER_FRAME).toLong() }
					return false
				}
				if (wrote < 0) {
					if (at > 0) synchronized(this) { if (!cut) framesWritten += (at / BYTES_PER_FRAME).toLong() }
					return false
				}
				if (wrote == 0) {

					try { Thread.sleep(5) } catch (_: InterruptedException) {
						if (at > 0) synchronized(this) { if (!cut) framesWritten += (at / BYTES_PER_FRAME).toLong() }
						return false
					}
				}
				at += wrote
			}
			if (at > 0) {
				synchronized(this) {
					if (!cut) framesWritten += (at / BYTES_PER_FRAME).toLong()
				}
			}
			return !cut
		}

		fun drain() {
			val held = track ?: return
			val written = synchronized(this) { framesWritten }
			if (written <= 0) return
			var last = -1L
			var still = 0
			val deadline = System.nanoTime() + ((written * 1000L / maxOf(rateHz, 1)) + 2000L) * 1_000_000L
			while (!cut && System.nanoTime() < deadline) {
				val head = try {
					held.playbackHeadPosition.toLong() and 0xFFFFFFFFL
				} catch (_: Throwable) {
					return
				}
				val played = (head - headAtBegin) and 0xFFFFFFFFL
				if (played >= written) return
				if (head == last) {
					if (++still >= STALL_TICKS) return
				} else {
					still = 0
					last = head
				}
				try { Thread.sleep(TICK_MS) } catch (_: InterruptedException) { return }
			}
		}

		fun cut() {
			synchronized(this) {
				cut = true
				framesWritten = 0
			}
			val held = track ?: return
			try {
				held.pause()
				held.flush()
			} catch (_: Throwable) {
			}
		}

		@Synchronized
		fun release() {
			val held = track ?: return
			track = null
			try { held.pause() } catch (_: Throwable) {}
			try { held.flush() } catch (_: Throwable) {}
			try { held.release() } catch (_: Throwable) {}
		}

		private fun make(rateHz: Int, usage: Int): AudioTrack? {
			val minimum = try {
				AudioTrack.getMinBufferSize(rateHz, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
			} catch (_: Throwable) {
				-1
			}
			if (minimum <= 0) {
				Log.e(TAG, "no player at $rateHz Hz")
				return null
			}

			val bytes = maxOf(minimum, (rateHz * BYTES_PER_FRAME) / 3).let { it + (it and 1) }
			return try {
				val built = AudioTrack.Builder()
					.setAudioAttributes(
						AudioAttributes.Builder()
							.setUsage(usage)
							.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
							.build()
					)
					.setAudioFormat(
						AudioFormat.Builder()
							.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
							.setSampleRate(rateHz)
							.setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
							.build()
					)
					.setBufferSizeInBytes(bytes)
					.setTransferMode(AudioTrack.MODE_STREAM)
					.build()
				if (built.state != AudioTrack.STATE_INITIALIZED) {
					Log.e(TAG, "the player would not initialise at $rateHz Hz")
					try { built.release() } catch (_: Throwable) {}
					null
				} else {
					built
				}
			} catch (broken: Throwable) {
				Log.e(TAG, "the player would not build", broken)
				null
			}
		}

		companion object {
			private const val TAG = "freeeloquencetts"
			private const val BYTES_PER_FRAME = 2
			private const val TICK_MS = 10L
			private const val STALL_TICKS = 100
		}
	}
}
