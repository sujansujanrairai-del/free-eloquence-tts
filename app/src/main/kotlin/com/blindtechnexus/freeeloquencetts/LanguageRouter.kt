package com.blindtechnexus.freeeloquencetts

import com.blindtechnexus.freeeloquencetts.pronunciation.Devanagari
import com.blindtechnexus.freeeloquencetts.pronunciation.DevanagariPronunciation
import com.blindtechnexus.freeeloquencetts.pronunciation.SpokenLanguage

/**
 * One utterance, cut into the languages it is actually in.
 *
 * The engine speaks one language at a time, and a screen reader is handed text
 * that is not written that way: a message in Hindi with an English app name in
 * it, a page of English quoting a German sentence, a notification that is
 * Nepali and a number. Before this, the whole utterance went to the language the
 * caller named, and everything else was read as though it were that language --
 * which for Devanagari meant reading it as bytes and getting "naagarna".
 *
 * So the text is cut at the granularity the reader chose (see [DetectionMode]),
 * each cut is asked about, and the cuts are put back together with the language
 * each one is to be spoken in. Adjacent cuts in the same language are joined
 * again, so an English sentence of forty words costs one engine switch and not
 * forty.
 *
 * Nothing here can cost an utterance, and that is a property rather than a
 * hope: every uncertain answer keeps the caller's language, an empty answer
 * keeps the text it was given, and a plan that comes back with nothing
 * speakable in it is thrown away for the text as it arrived. A detector that is
 * missing, slow or wrong makes the voice plainer, never silent.
 *
 * Devanagari is the special case, and it is the reason this class is not just a
 * table of ISO codes. The engine has no Hindi module and no Nepali module, so a
 * Devanagari span is not spoken by another language at all: it is respelled
 * into the English letters that sound like it (see
 * [DevanagariPronunciation]) and then spoken by the English voice. The
 * detection is what decides *which* rules respell it -- Hindi or Nepali -- and
 * what tells the reader apart from an English sentence that happens to have a
 * Hindi word in it.
 *
 * Everything is best-effort. No model, a timeout, a text of nothing but
 * punctuation: each of those answers with the caller's own language, which is
 * the behaviour this app had before any of this existed.
 */
object LanguageRouter {

	/** A run of the utterance that will be spoken in one language.
	 *
	 *  [language] is an engine language from [Eci.LANGUAGES]; [text] is the
	 *  text ready to be handed to the pipeline, which for a Devanagari span is
	 *  already respelled. */
	class Segment(val language: Int, val text: String)

	/** How long a sentence may be before [DetectionMode.MIXED] takes it apart
	 *  into words, in characters. A sentence with a Devanagari word in it is
	 *  the one that needs the finer answer. */
	private const val MIXED_SENTENCE_LIMIT = 48

	/**
	 * The utterance, cut and labelled.
	 *
	 * [caller] is the language the request already resolved to, and it is what
	 * every uncertain answer falls back to.
	 */
	fun plan(raw: String, s: Settings, caller: Int): List<Segment> {
		if (raw.isEmpty()) return listOf(Segment(caller, raw))
		if (!s.autoDetectLanguage) return listOf(Segment(caller, raw))
		if (!LanguageIdentifier.available()) return listOf(Segment(caller, raw))
		val spans = try {
			spansOf(raw, DetectionMode.clamp(s.detectionMode))
		} catch (_: Throwable) {
			return listOf(Segment(caller, raw))
		}
		if (spans.size <= 1) {
			// One span is still worth asking about -- a paragraph in Hindi
			// that a caller handed over in English is the common case -- but
			// there is nothing to cut.
			val only = spans.firstOrNull() ?: raw
			if (EvvEngine.isBlankSpeech(only)) return listOf(Segment(caller, raw))
			val iso = LanguageIdentifier.detect(only)
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
		// Nothing usable came back -- every span was reduced to nothing, or the
		// list came back empty -- so the text is spoken as it was given, in the
		// language the caller named. An utterance is never answered with
		// silence because a detector or a rewrite had nothing to say.
		if (merged.isEmpty()) return listOf(Segment(caller, raw))
		if (merged.none { !EvvEngine.isBlankSpeech(it.text) }) return listOf(Segment(caller, raw))
		return merged
	}

	/** One span, with its language decided and its script respelled if it is
	 *  Devanagari. */
	private fun settle(text: String, iso: String?, caller: Int): Segment {
		if (text.isEmpty()) return Segment(caller, text)
		val devanagari = DevanagariPronunciation.hasDevanagari(text)
		val language = languageFor(iso, text, devanagari, caller)
		// Hindi and Nepali are always respelled: there is no switch any more,
		// because handing the engine the script as it stands is not a voice,
		// it is bytes it has no letters for. See [Settings].
		if (!devanagari) return Segment(language, text)
		val respelled = try {
			DevanagariPronunciation.apply(text, SpokenLanguage.of(iso))
		} catch (_: Throwable) {
			text
		}
		// A respelling that came back empty (a word the rules could make
		// nothing of, a span of nothing but marks) is answered with the text
		// as it was, which is at worst an accent rather than a gap in the
		// sentence.
		if (respelled.isBlank()) return Segment(language, text)
		return Segment(language, respelled)
	}

	/**
	 * Which engine language [iso] is to be spoken in.
	 *
	 * The eight the engine has answer themselves, in the dialect the caller
	 * was already using when there is a choice of two. A Devanagari span is
	 * answered in English, because after respelling it is English letters --
	 * the Hindi and the Nepali are in the respelling, not in the module. And
	 * anything else -- a language with no module, an answer that never came --
	 * stays in the caller's language, which is the voice that was going to
	 * speak it anyway.
	 */
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
			// A language with no module is read by the voice that was already
			// going to read it. Silence would be worse than an accent.
			else -> caller
		}
	}

	/** Joins [segment] to the last one when they are in the same language, so
	 *  that a sentence in one language is one engine call and not forty. */
	private fun append(into: MutableList<Segment>, segment: Segment) {
		val last = into.lastOrNull()
		if (last != null && last.language == segment.language) {
			into[into.size - 1] = Segment(last.language, last.text + segment.text)
		} else {
			into.add(segment)
		}
	}

	// ---- cutting the text -------------------------------------------------

	private fun spansOf(raw: String, mode: Int): List<String> = when (mode) {
		DetectionMode.CHARACTER -> characterSpans(raw)
		DetectionMode.WORD -> wordSpans(raw)
		DetectionMode.SENTENCE -> sentenceSpans(raw)
		else -> mixedSpans(raw)
	}

	/**
	 * Runs of characters of the same script, which is as fine as a character
	 * can honestly decide. A single Devanagari letter has no language; a run
	 * of them has one, and a run beside a run of Latin is the boundary a
	 * reader can hear.
	 */
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
				// Whitespace belongs to the span before it, so that the text
				// put back together is the text that came in.
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

	/** Words with the whitespace after them, so the sentence survives being
	 *  cut into words. */
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

	/** Sentences: a sentence ends at the punctuation that ends one, in either
	 *  script, and the whitespace after it belongs to the sentence. */
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

	/**
	 * Sentences, then the words of a sentence that is too mixed to be left
	 * whole.
	 *
	 * The test for "too mixed" is deliberately not a confidence number: a
	 * sentence that has both scripts in it, or that is long enough to be a
	 * paragraph, is the one where a sentence-level answer loses something. A
	 * short sentence in one script is left whole, which is what makes this mode
	 * cheap on ordinary text.
	 */
	private fun mixedSpans(raw: String): List<String> {
		val out = ArrayList<String>()
		for (sentence in sentenceSpans(raw)) {
			val devanagari = DevanagariPronunciation.hasDevanagari(sentence)
			val latin = sentence.any { it.isLetter() && it.code < 0x0900 }
			val long = sentence.length > MIXED_SENTENCE_LIMIT
			if ((devanagari && latin) || (devanagari && long)) {
				out.addAll(wordSpans(sentence))
			} else {
				out.add(sentence)
			}
		}
		return out
	}
}
