package com.blindtechnexus.freeeloquencetts

import kotlin.math.abs
import kotlin.math.tanh

object AudioOptimization {

	const val BALANCED = 0

	const val VOLUME = 1

	const val QUALITY = 2

	val MODES = listOf(BALANCED, VOLUME, QUALITY)

	fun methodFor(mode: Int): Int = when (mode) {
		VOLUME -> Eci.UPSAMPLE_HOLD
		QUALITY -> Eci.UPSAMPLE_SINC
		else -> Eci.UPSAMPLE_CUBIC
	}

	fun targetPeak(mode: Int): Float = when (mode) {
		VOLUME -> 0.891f 
		QUALITY -> 0.501f 
		else -> 0.708f 
	}

	fun maxGain(mode: Int): Float = when (mode) {
		VOLUME -> 8.0f
		QUALITY -> 2.0f
		else -> 4.0f
	}

	fun minGain(mode: Int): Float = when (mode) {
		QUALITY -> 0.25f
		else -> 1.0f
	}

	fun ceiling(mode: Int): Float = when (mode) {
		VOLUME -> 0.98f
		QUALITY -> 0.89f
		else -> 0.95f
	}

	fun clamp(mode: Int): Int = mode.coerceIn(BALANCED, QUALITY)
}

class AudioOptimizer(mode: Int, sampleRateHz: Int) {

	private val mode = AudioOptimization.clamp(mode)

	private val target = AudioOptimization.targetPeak(this.mode)
	private val lowest = AudioOptimization.minGain(this.mode)
	private val highest = AudioOptimization.maxGain(this.mode)
	private val top = AudioOptimization.ceiling(this.mode)

	private val releaseSeconds: Float = if (this.mode == AudioOptimization.VOLUME) 0.06f else 0.15f

	private val rateHz = sampleRateHz.coerceAtLeast(1)

	private val blockerR: Float = blockerFor(rateHz)

	private var blockedIn = 0.0f
	private var blockedOut = 0.0f
	private var gain = 1.0f

	fun reset() {
		blockedIn = 0.0f
		blockedOut = 0.0f
		gain = 1.0f
	}

	fun process(chunk: ByteArray, count: Int): Int {
		val samples = count.coerceIn(0, chunk.size) / 2
		if (samples == 0) return 0

		var peak = 0.0f
		for (at in 0 until samples) {
			val low = chunk[at * 2].toInt() and 0xFF
			val high = chunk[at * 2 + 1].toInt()
			val signed = (high shl 8) or low
			val over = abs(signed / 32768.0f)
			if (over > peak) peak = over
		}
		val want = gainFor(peak, target, lowest, highest)

		gain = if (want < gain) want else gain + (want - gain) * releaseFor(samples, rateHz, releaseSeconds)
		val applying = gain
		for (at in 0 until samples) {
			val low = chunk[at * 2].toInt() and 0xFF
			val high = chunk[at * 2 + 1].toInt()
			val signed = (high shl 8) or low
			var v = signed / 32768.0f

			val out = v - blockedIn + blockerR * blockedOut
			blockedIn = v
			blockedOut = out
			v = out * applying

			val limited = top * tanh((v / top).toDouble()).toFloat()
			val back = (limited * 32768.0f).toInt().coerceIn(-32768, 32767)
			chunk[at * 2] = (back and 0xFF).toByte()
			chunk[at * 2 + 1] = ((back shr 8) and 0xFF).toByte()
		}
		return samples * 2
	}

	companion object {

		const val BLOCKER_HZ = 9.0

		const val BLOCKER_R = 0.995f

		fun blockerFor(rateHz: Int): Float =
			kotlin.math.exp(-2.0 * Math.PI * BLOCKER_HZ / rateHz.coerceAtLeast(1)).toFloat()

		fun releaseFor(samples: Int, rateHz: Int, seconds: Float): Float {
			if (samples <= 0 || seconds <= 0f) return 1.0f
			val span = samples.toDouble() / rateHz.coerceAtLeast(1)
			return (1.0 - kotlin.math.exp(-span / seconds)).toFloat().coerceIn(0f, 1f)
		}

		fun gainFor(peak: Float, target: Float, minGain: Float, maxGain: Float): Float {
			if (peak <= 1e-6f) return 1.0f
			return (target / peak).coerceIn(minGain, maxGain)
		}

		fun softClip(v: Float, ceiling: Float): Float =
			ceiling * tanh((v / ceiling).toDouble()).toFloat()

		fun peakOf(samples: ShortArray): Float {
			var peak = 0.0f
			for (s in samples) {
				val over = abs(s / 32768.0f)
				if (over > peak) peak = over
			}
			return peak
		}
	}
}
