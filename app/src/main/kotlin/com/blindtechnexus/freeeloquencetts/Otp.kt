package com.blindtechnexus.freeeloquencetts

object Otp {

	const val MIN_DIGITS = 4
	const val MAX_DIGITS = 8

	const val THRESHOLD = 3

	private const val NEAR = 40
	private const val FAR = 80

	class Code(val start: Int, val end: Int, val groups: List<String>) {
		val digits: String get() = groups.joinToString("")
	}

	private val STRONG = arrayOf(
		"otp", "one-time", "one time", "onetime",
		"verification", "verify", "verifying", "verif",
		"passcode", "pass code", "security code", "login code", "log in code", "sign-in code",
		"sign in code", "confirmation code", "confirm code", "authentication", "auth code",
		"2fa", "two-factor", "two factor", "2-step", "two-step", "activation code",
		"access code", "secret code", "validation code", "mfa",

		"bestätigungscode", "bestaetigungscode", "sicherheitscode", "verifizierung", "einmalpasswort",
		"einmal-passwort", "einmalcode", "tan",

		"código de verificación", "codigo de verificacion", "verificación", "verificacion",
		"clave de acceso", "contraseña de un solo uso",

		"code de vérification", "code de verification", "vérification", "code de sécurité",
		"code de securite", "mot de passe à usage unique",

		"codice di verifica", "verifica", "codice di sicurezza", "codice di accesso",

		"kod weryfikacyjny", "weryfikac", "kod jednorazowy", "jednorazow"
	)

	private val GENERAL = arrayOf(
		"code", "pin", "token", "código", "codigo", "clave", "codice", "kod", "passwort", "password"
	)

	private val HINTS = arrayOf(
		"is your", "is the", "your code", "enter", "use ", "do not share", "don't share",
		"dont share", "never share", "do not disclose", "expires", "expire", "valid for",
		"valid till", "valid until", "will expire", "no comparta", "ne partagez", "nicht weiter",
		"non condividere", "nie udostępniaj", "nie udostepniaj"
	)

	private val NOT_A_CODE = arrayOf(
		"order", "invoice", "tracking", "ticket", "reference", "ref.", "ref ", "account", "acct",
		"amount", "total", "balance", "price", "flight", "seat", "room", "zip", "postal",
		"postcode", "pincode", "pin code ", "extension", "ext.", "page", "year", "model",
		"serial", "id:", "id ", "no.", "number", "phone", "tel", "call", "dial", "sms to",
		"send to", "text to", "bestellung", "rechnung", "pedido", "factura", "commande",
		"facture", "ordine", "fattura", "zamówienie", "zamowienie"
	)

	private fun isGroupBreak(c: Char): Boolean =
		c == ' ' || c == '-' || c == '\u2011' || c == '\u2013' || c == '.' || c == ',' || c == '\u00A0'

	private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

	fun apply(text: String): String {
		if (text.isEmpty()) return text
		if (!hasFourDigits(text)) return text
		val found = find(text)
		if (found.isEmpty()) return text
		val out = StringBuilder(text.length + found.size * 16)
		var at = 0
		for (code in found) {
			out.append(text, at, code.start)
			for ((k, group) in code.groups.withIndex()) {
				if (k > 0) out.append(", ")
				out.append(Numbers.spaced(group))
			}
			at = code.end
		}
		out.append(text, at, text.length)
		return out.toString()
	}

	fun find(text: String): List<Code> {
		val lower = lowered(text)
		val candidates = candidates(text)
		if (candidates.isEmpty()) return emptyList()
		val whole = text.trim()
		val out = ArrayList<Code>(candidates.size)
		for (candidate in candidates) {
			if (score(text, lower, candidate, whole) >= THRESHOLD) out.add(candidate)
		}
		return out
	}

	fun scoreOf(text: String, candidate: Code): Int =
		score(text, lowered(text), candidate, text.trim())

	private fun hasFourDigits(text: String): Boolean {
		var run = 0
		for (c in text) {
			if (c.isAsciiDigit()) {
				if (++run >= MIN_DIGITS) return true
			} else {
				run = 0
			}
		}
		return false
	}

	private fun lowered(text: String): String {
		val chars = CharArray(text.length)
		for (i in text.indices) chars[i] = Character.toLowerCase(text[i])
		return String(chars)
	}

	private fun candidates(text: String): List<Code> {
		val n = text.length
		val out = ArrayList<Code>(2)
		var i = 0
		while (i < n) {
			val c = text[i]
			if (c == '`') {

				i = EvvEngine.annotationEnd(text, i)
				continue
			}
			if (!c.isAsciiDigit()) {
				i++
				continue
			}
			var j = i
			while (j < n && text[j].isAsciiDigit()) j++
			val first = j - i

			if (first > MAX_DIGITS) {
				i = j
				continue
			}
			val groups = ArrayList<String>(3)
			groups.add(text.substring(i, j))
			var end = j

			var total = first
			while (groups.size < 3 && end + 1 < n && isGroupBreak(text[end]) && text[end + 1].isAsciiDigit()) {
				var k = end + 1
				while (k < n && text[k].isAsciiDigit()) k++
				val size = k - end - 1
				if (size != first || total + size > MAX_DIGITS) break

				groups.add(text.substring(end + 1, k))
				total += size
				end = k
			}

			if (groups.size > 1 && end + 1 < n && isGroupBreak(text[end]) && text[end + 1].isAsciiDigit()) {
				i = end
				while (i < n && (text[i].isAsciiDigit() || isGroupBreak(text[i]))) i++
				continue
			}
			if (groups.size == 1 && first < MIN_DIGITS) {
				i = j
				continue
			}
			if (total in MIN_DIGITS..MAX_DIGITS && standsAlone(text, i, end)) {
				out.add(Code(i, end, groups))
			}
			i = end
		}
		return out
	}

	private fun standsAlone(text: String, start: Int, end: Int): Boolean {
		if (start > 0) {
			val before = text[start - 1]
			if (before.isLetterOrDigit()) return false
			if (before == '$' || before == '€' || before == '£' || before == '¥' || before == '₹' || before == '%') return false
			if (before == '/' || before == ':' || before == '@' || before == '_') return false
			if ((before == '.' || before == ',') && start > 1 && text[start - 2].isAsciiDigit()) return false
		}
		if (end < text.length) {
			val after = text[end]
			if (after.isLetterOrDigit()) return false
			if (after == '%' || after == '/' || after == '@' || after == '_') return false
			if (after == ':' && end + 1 < text.length && text[end + 1].isAsciiDigit()) return false
			if ((after == '.' || after == ',') && end + 1 < text.length && text[end + 1].isAsciiDigit()) return false
		}
		return true
	}

	private fun score(text: String, lower: String, candidate: Code, whole: String): Int {
		val n = lower.length
		val start = candidate.start
		val end = candidate.end
		val nearBefore = lower.substring(maxOf(0, start - NEAR), start)
		val farBefore = lower.substring(maxOf(0, start - FAR), maxOf(0, start - NEAR))
		val nearAfter = lower.substring(end, minOf(n, end + NEAR))
		val farAfter = lower.substring(minOf(n, end + NEAR), minOf(n, end + FAR))
		var score = 0

		var words = 0
		for (word in STRONG) {
			if (nearBefore.contains(word) || nearAfter.contains(word)) { words = maxOf(words, 3); break }
			if (farBefore.contains(word) || farAfter.contains(word)) words = maxOf(words, 2)
		}
		if (words < 3) {
			for (word in GENERAL) {
				if (containsWord(nearBefore, word) || containsWord(nearAfter, word)) { words = maxOf(words, 2); break }
				if (containsWord(farBefore, word) || containsWord(farAfter, word)) words = maxOf(words, 1)
			}
		}
		score += words

		val around = lower.substring(maxOf(0, start - FAR), minOf(n, end + FAR))
		var hints = 0
		for (hint in HINTS) {
			if (around.contains(hint)) hints++
			if (hints >= 2) break
		}
		score += hints
		if (nearBefore.contains("<#>") || lower.startsWith("<#>")) score += 2
		if (start >= 2 && lower[start - 1] == '-' && lower[start - 2] == 'g' && (start == 2 || !lower[start - 3].isLetterOrDigit())) score += 2
		val lead = nearBefore.trimEnd()
		if (lead.endsWith(":")) score += 1

		val digits = candidate.digits
		if (digits.length == 6) score += 1
		if (candidate.groups.size > 1) score += 1
		if (whole == text.substring(start, end) || whole == digits) score += 2

		if (digits.length == 4 && isYear(digits) && !lead.endsWith(":") && !endsWithCodeWord(lead)) score -= 2
		for (word in NOT_A_CODE) {
			if (endsNear(lead, word, 16)) { score -= 3; break }
		}

		if (otherDigitRuns(lower, start, end) >= 3) score -= 1
		return score
	}

	private fun containsWord(window: String, word: String): Boolean {
		var from = 0
		while (true) {
			val at = window.indexOf(word, from)
			if (at < 0) return false
			val before = if (at == 0) ' ' else window[at - 1]
			val afterAt = at + word.length
			val after = if (afterAt >= window.length) ' ' else window[afterAt]
			if (!before.isLetter() && !after.isLetter()) return true
			from = at + 1
		}
	}

	private fun isYear(digits: String): Boolean {
		val value = digits.toIntOrNull() ?: return false
		return value in 1900..2099
	}

	private fun endsWithCodeWord(lead: String): Boolean {
		val tail = lead.trimEnd(':', ' ', '-', '\t')
		return tail.endsWith("code") || tail.endsWith("otp") || tail.endsWith("pin") ||
			tail.endsWith("código") || tail.endsWith("codigo") || tail.endsWith("codice") || tail.endsWith("kod")
	}

	private fun endsNear(lead: String, word: String, within: Int): Boolean {
		val tail = if (lead.length > within) lead.substring(lead.length - within) else lead
		return tail.contains(word)
	}

	private fun otherDigitRuns(lower: String, start: Int, end: Int): Int {
		val from = maxOf(0, start - FAR)
		val to = minOf(lower.length, end + FAR)
		var runs = 0
		var run = 0
		var i = from
		while (i < to) {
			if (i in start until end) {
				i = end
				run = 0
				continue
			}
			if (lower[i].isAsciiDigit()) {
				run++
				if (run == MIN_DIGITS) runs++
			} else {
				run = 0
			}
			i++
		}
		return runs
	}
}
