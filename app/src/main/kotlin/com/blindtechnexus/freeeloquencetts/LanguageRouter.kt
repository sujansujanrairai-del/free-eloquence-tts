package com.blindtechnexus.freeeloquencetts

import com.blindtechnexus.freeeloquencetts.pronunciation.Devanagari
import com.blindtechnexus.freeeloquencetts.pronunciation.DevanagariPronunciation
import com.blindtechnexus.freeeloquencetts.pronunciation.PronunciationRules
import com.blindtechnexus.freeeloquencetts.pronunciation.SpokenLanguage

object LanguageRouter {

	class Segment(val language: Int, val text: String)

	private const val MIXED_SENTENCE_LIMIT = 48

	fun plan(raw: String, s: Settings, caller: Int): List<Segment> {
		if (raw.isEmpty()) return listOf(Segment(caller, raw))
		if (!s.autoDetectLanguage) return listOf(Segment(caller, raw))
		val spans = try {
			spansOf(raw, DetectionMode.clamp(s.detectionMode))
		} catch (_: Throwable) {
			return listOf(Segment(caller, raw))
		}
		if (spans.size <= 1) {
			val only = spans.firstOrNull() ?: raw
			if (EvvEngine.isBlankSpeech(only)) return listOf(Segment(caller, raw))
			val iso = try {
				LanguageIdentifier.detect(only)
			} catch (_: Throwable) {
				null
			}
			val one = settle(only, iso, caller)
			if (EvvEngine.isBlankSpeech(one.text)) return listOf(Segment(caller, raw))
			return listOf(one)
		}
		val codes = try {
			LanguageIdentifier.detectAll(spans)
		} catch (_: Throwable) {
			List(spans.size) { null }
		}
		val merged = ArrayList<Segment>(spans.size)
		for (at in spans.indices) {
			val segment = settle(spans[at], codes.getOrNull(at), caller)
			append(merged, segment)
		}
		if (merged.isEmpty()) return listOf(Segment(caller, raw))
		if (merged.none { !EvvEngine.isBlankSpeech(it.text) }) return listOf(Segment(caller, raw))
		return merged
	}

	private fun settle(text: String, iso: String?, caller: Int): Segment {
		if (text.isEmpty()) return Segment(caller, text)
		val devanagari = DevanagariPronunciation.hasDevanagari(text)
		val language = languageFor(iso, text, devanagari, caller)
		if (!devanagari) return Segment(language, text)
		val spoken = spokenLanguageOf(text, iso)
		val respelled = try {
			DevanagariPronunciation.apply(text, spoken)
		} catch (_: Throwable) {
			text
		}
		if (respelled.isBlank()) return Segment(language, text)
		return Segment(language, respelled)
	}

	private fun spokenLanguageOf(text: String, iso: String?): SpokenLanguage {
		val guessed = try {
			PronunciationRules.guessLanguage(text)
		} catch (_: Throwable) {
			SpokenLanguage.HINDI
		}
		val code = iso?.lowercase()
		if (code == "ne" && guessed == SpokenLanguage.HINDI) return SpokenLanguage.NEPALI
		return guessed
	}

	private fun languageFor(iso: String?, text: String, devanagari: Boolean, caller: Int): Int {
		val code = iso?.lowercase()
		if (devanagari) {
			return if (caller == Eci.ENGLISH_UK) Eci.ENGLISH_UK else Eci.ENGLISH_US
		}
		return when (code) {
			"en" -> if (caller == Eci.ENGLISH_UK) Eci.ENGLISH_UK else Eci.ENGLISH_US
			"de" -> Eci.GERMAN
			"es" -> if (caller == Eci.SPANISH_MEXICO) Eci.SPANISH_MEXICO else Eci.SPANISH_SPAIN
			"fr" -> if (caller == Eci.FRENCH_CANADA) Eci.FRENCH_CANADA else Eci.FRENCH_FRANCE
			"it" -> Eci.ITALIAN
			else -> caller
		}
	}

	private fun append(into: MutableList<Segment>, segment: Segment) {
		val last = into.lastOrNull()
		if (last != null && last.language == segment.language) {
			into[into.size - 1] = Segment(last.language, last.text + segment.text)
		} else {
			into.add(segment)
		}
	}

	private fun spansOf(raw: String, mode: Int): List<String> = when (mode) {
		DetectionMode.CHARACTER -> characterSpans(raw)
		DetectionMode.WORD -> wordSpans(raw)
		DetectionMode.SENTENCE -> sentenceSpans(raw)
		else -> mixedSpans(raw)
	}

	private fun characterSpans(raw: String): List<String> {
		val out = ArrayList<String>()
		val span = StringBuilder()
		var kind = -1

		fun flush() {
			if (span.isNotEmpty()) {
				out.add(span.toString())
				span.setLength(0)
			}
			kind = -1
		}

		for (c in raw) {
			val here = when {
				c.isWhitespace() -> -2
				Devanagari.isDevanagari(c) && Devanagari.isLetter(c) -> 1
				c.isLetterOrDigit() -> 0
				else -> 2
			}
			if (here == -2) {
				span.append(c)
				continue
			}
			if (kind != -1 && here != kind) flush()
			kind = here
			span.append(c)
		}
		flush()
		return out
	}

	private fun wordSpans(raw: String): List<String> {
		val out = ArrayList<String>()
		val span = StringBuilder()
		for (c in raw) {
			span.append(c)
			if (c.isWhitespace()) {
				out.add(span.toString())
				span.setLength(0)
			}
		}
		if (span.isNotEmpty()) out.add(span.toString())
		return out
	}

	private fun sentenceSpans(raw: String): List<String> {
		val out = ArrayList<String>()
		val span = StringBuilder()
		for (c in raw) {
			span.append(c)
			if (c == '.' || c == '!' || c == '?' || c == '\u0964' || c == '\u0965' || c == '\n') {
				out.add(span.toString())
				span.setLength(0)
			}
		}
		if (span.isNotEmpty()) out.add(span.toString())
		return out
	}

	private fun mixedSpans(raw: String): List<String> {
		val out = ArrayList<String>()
		for (sentence in sentenceSpans(raw)) {
			val devanagari = DevanagariPronunciation.hasDevanagari(sentence)
			val latin = hasLatinLetters(sentence)
			val long = sentence.length > MIXED_SENTENCE_LIMIT
			when {
				devanagari && latin -> out.addAll(scriptRuns(sentence))
				devanagari && long -> out.addAll(wordSpans(sentence))
				!devanagari && latin && long -> out.addAll(wordSpans(sentence))
				else -> out.add(sentence)
			}
		}
		return out
	}

	private fun hasLatinLetters(text: String): Boolean {
		for (c in text) {
			if (c in 'A'..'Z' || c in 'a'..'z') return true
			if (c.code in 0x00C0..0x024F && c.isLetter()) return true
		}
		return false
	}

	private fun scriptRuns(sentence: String): List<String> {
		val out = ArrayList<String>()
		val span = StringBuilder()
		var kind = -1

		fun flush() {
			if (span.isNotEmpty()) {
				out.add(span.toString())
				span.setLength(0)
			}
			kind = -1
		}

		for (c in sentence) {
			val here = when {
				Devanagari.isDevanagari(c) && Devanagari.isLetter(c) -> 1
				c.isLetter() -> 0
				else -> -1
			}
			if (here == -1) {
				span.append(c)
				continue
			}
			if (kind != -1 && here != kind) flush()
			kind = here
			span.append(c)
		}
		flush()
		return out
	}
}
