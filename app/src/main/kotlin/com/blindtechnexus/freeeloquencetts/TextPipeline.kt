package com.blindtechnexus.freeeloquencetts

import com.blindtechnexus.freeeloquencetts.pronunciation.DevanagariPronunciation

object TextPipeline {

	fun apply(
		raw: String,
		s: Settings,
		words: List<Pair<String, String>> = emptyList(),
		abbreviations: List<Abbreviations.Entry> = emptyList(),
		language: Int = Eci.ENGLISH_US
	): String = apply(
		raw,
		s,
		if (words.isEmpty()) WordIndex.EMPTY else WordIndex.of(words),
		abbreviations,
		language
	)

	fun apply(
		raw: String,
		s: Settings,
		index: WordIndex,
		abbreviations: List<Abbreviations.Entry> = emptyList(),
		language: Int = Eci.ENGLISH_US
	): String {
		if (raw.isEmpty()) return raw
		var text = raw
		try { if (s.readTextStyles) text = MarkupStyles.annotate(text) } catch (_: Throwable) {}
		try { if (s.stripSsml) text = Ssml.strip(text) } catch (_: Throwable) {}
		try { text = Emoji.apply(text, s.readEmoji) } catch (_: Throwable) {}
		try { text = index.apply(text) } catch (_: Throwable) {}
		if (s.expandAbbreviations) {
			try { text = Abbreviations.apply(text, abbreviations) } catch (_: Throwable) {}
		}
		try { text = CodeReading.apply(text, s.codeOptions()) } catch (_: Throwable) {}
		try { text = TextFixes.apply(text) } catch (_: Throwable) {}
		try { text = DevanagariPronunciation.apply(text) } catch (_: Throwable) {}
		if (language == Eci.ENGLISH_US || language == Eci.ENGLISH_UK) {
			try {
				text = EnglishPronunciation.apply(text, language == Eci.ENGLISH_UK)
			} catch (_: Throwable) {
			}
		}
		if (s.readOtpDigits) {
			try { text = Otp.apply(text) } catch (_: Throwable) {}
		}
		try { text = Numbers.apply(text, s.numberMode) } catch (_: Throwable) {}
		try { text = CapitalLetters.apply(text, s.capitalMode) } catch (_: Throwable) {}
		try { text = Spelling.apply(text, s.spellingMode) } catch (_: Throwable) {}
		return try { Symbols.apply(text, s.symbolMode, s.customSymbols) } catch (_: Throwable) { text }
	}

	fun pieces(
		raw: String,
		s: Settings,
		words: List<Pair<String, String>>,
		abbreviations: List<Abbreviations.Entry>,
		language: Int = Eci.ENGLISH_US
	): Sequence<TextPieces.Piece> = pieces(
		raw,
		s,
		if (words.isEmpty()) WordIndex.EMPTY else WordIndex.of(words),
		abbreviations,
		language
	)

	fun pieces(
		raw: String,
		s: Settings,
		index: WordIndex,
		abbreviations: List<Abbreviations.Entry>,
		language: Int = Eci.ENGLISH_US
	): Sequence<TextPieces.Piece> = TextPieces.pieceSequence(raw).map { piece ->
		val said = try {
			apply(piece.text, s, index, abbreviations, language)
		} catch (_: Throwable) {
			piece.text
		}
		if (said == piece.text) piece
		else if (said.isNotBlank()) TextPieces.Piece(said, piece.forced)
		else TextPieces.Piece("", piece.forced)
	}.filter { it.text.isNotEmpty() }
}
