package com.blindtechnexus.freeeloquencetts

class WordIndex private constructor(
    private val pool: CharArray,
    private val keyAt: IntArray,
    private val keyLen: IntArray,
    private val sayAt: IntArray,
    private val sayLen: IntArray,
    private val next: IntArray,
    private val table: IntArray,
    private val tableHash: IntArray,
    private val mask: Int,
    private val punctHead: IntArray,
    private val punctNext: IntArray,
    val size: Int,
    val dropped: Int,
    val pooledChars: Int
) {

    private val empty: Boolean = size == 0

    val isEmpty: Boolean get() = empty

    fun apply(text: String): String {
        if (empty || text.isEmpty()) return text
        val n = text.length
        var out: StringBuilder? = null
        var copied = 0
        var i = 0
        while (i < n) {
            val c = text[i]
            var end = -1
            var entry = -1
            if (isWordChar(c)) {

                if (i == 0 || !isWordChar(text[i - 1])) {
                    val hit = matchWord(text, i)
                    if (hit >= 0) {
                        end = (hit ushr 32).toInt()
                        entry = (hit and 0xFFFFFFFFL).toInt() - 1
                    }
                }
            } else if (punctHead.isNotEmpty()) {
                val hit = matchPunctuation(text, i)
                if (hit >= 0) {
                    end = (hit ushr 32).toInt()
                    entry = (hit and 0xFFFFFFFFL).toInt() - 1
                }
            }
            if (entry < 0 || end <= i) {
                i++
                continue
            }
            if (out == null) out = StringBuilder(n + 16)
            if (copied < i) out.append(text, copied, i)
            val at = sayAt[entry]
            out.append(pool, at, sayLen[entry])
            copied = end
            i = end
        }
        val answer = out ?: return text
        if (copied < n) answer.append(text, copied, n)
        return answer.toString()
    }

    private fun matchWord(text: String, from: Int): Long {
        val n = text.length
        var to = from
        while (to < n && isWordChar(text[to])) to++
        if (to == from) return -1L
        val hash = foldHash(text, from, to)
        var slot = hash and mask
        var best = -1L
        while (true) {
            val held = table[slot]
            if (held == 0) return best
            if (tableHash[slot] == hash) {
                var e = held - 1
                while (e >= 0) {
                    val end = compare(text, from, e)
                    if (end > 0) {
                        val packed = (end.toLong() shl 32) or (e + 1).toLong()
                        if (best < 0 || end > (best ushr 32).toInt()) best = packed
                    }
                    e = next[e]
                }
                return best
            }
            slot = (slot + 1) and mask
        }
    }

    private fun matchPunctuation(text: String, at: Int): Long {
        val bucket = fold(text[at]).code and (punctHead.size - 1)
        var e = punctHead[bucket] - 1
        var best = -1L
        while (e >= 0) {
            val end = compare(text, at, e)
            if (end > 0 && (best < 0 || end > (best ushr 32).toInt())) {
                best = (end.toLong() shl 32) or (e + 1).toLong()
            }
            e = punctNext[e]
        }
        return best
    }

    private fun compare(text: String, from: Int, entry: Int): Int {
        val n = text.length
        var t = from
        var k = keyAt[entry]
        val kEnd = k + keyLen[entry]
        while (k < kEnd) {
            if (pool[k] == ' ') {
                if (t >= n || !text[t].isWhitespace()) return -1
                while (t < n && text[t].isWhitespace()) t++
                while (k < kEnd && pool[k] == ' ') k++
                continue
            }
            if (t >= n) return -1
            if (fold(text[t]) != pool[k]) return -1
            t++
            k++
        }
        if (t < n && isWordChar(text[t])) return -1
        return t
    }

    private fun foldHash(text: String, from: Int, to: Int): Int {
        var hash = 1
        var i = from
        while (i < to) {
            hash = hash * 31 + fold(text[i]).code
            i++
        }
        return hash
    }

    private fun fold(c: Char): Char =
        if (c in 'A'..'Z') (c + 32) else Character.toLowerCase(c)

    private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '\''

    class Builder(
        private val budgetChars: Int = DEFAULT_BUDGET_CHARS,
        private val maxEntries: Int = DEFAULT_MAX_ENTRIES
    ) {

        private var pool = CharArray(1 shl 12)
        private var pooled = 0

        private var entryKeyAt = IntArray(256)
        private var entryKeyLen = IntArray(256)
        private var entrySayAt = IntArray(256)
        private var entrySayLen = IntArray(256)
        private var entryFirst = IntArray(256)
        private var entries = 0

        private var dropped = 0

        private var folded = CharArray(MAX_KEY_CHARS + 2)

        val size: Int get() = entries
        val overflowed: Int get() = dropped

        val roomLeft: Int get() = budgetChars - pooled

        fun add(word: String, say: String): Boolean {
            if (entries >= maxEntries) {
                dropped++
                return false
            }
            if (word.isEmpty() || say.isEmpty()) return false
            val key = fold(word)
            if (key.isEmpty()) return false
            if (key.length > MAX_KEY_CHARS) {
                dropped++
                return false
            }
            if (say.length > MAX_SAY_CHARS) {
                dropped++
                return false
            }
            val need = key.length + say.length
            if (pooled + need > budgetChars) {
                dropped++
                return false
            }
            if (entries == entryKeyAt.size) grow()
            if (pooled + need > pool.size) growPool(pooled + need)
            val keyAt = pooled
            key.toCharArray(pool, pooled)
            pooled += key.length
            val sayAt = pooled
            say.toCharArray(pool, pooled)
            pooled += say.length
            entryKeyAt[entries] = keyAt
            entryKeyLen[entries] = key.length
            entrySayAt[entries] = sayAt
            entrySayLen[entries] = say.length
            entryFirst[entries] = firstTokenLength(pool, keyAt, key.length)
            entries++
            return true
        }

        fun build(): WordIndex {
            if (entries == 0) {
                return WordIndex(
                    CharArray(0), IntArray(0), IntArray(0), IntArray(0), IntArray(0),
                    IntArray(0), IntArray(0), IntArray(0), 0,
                    IntArray(0), IntArray(0), 0, dropped, 0
                )
            }
            var capacity = Integer.highestOneBit(entries * 4 - 1) shl 1
            if (capacity < 16) capacity = 16
            var table = IntArray(capacity)
            var hashes = IntArray(capacity)
            var mask = capacity - 1
            val next = IntArray(entries) { -1 }

            for (e in entries - 1 downTo 0) {
                val length = entryFirst[e]
                if (length == 0) continue
                val hash = hashOf(pool, entryKeyAt[e], length)
                var slot = hash and mask
                while (true) {
                    val held = table[slot]
                    if (held == 0) {
                        table[slot] = e + 1
                        hashes[slot] = hash
                        break
                    }
                    if (hashes[slot] == hash) {

                        next[e] = held - 1
                        table[slot] = e + 1
                        break
                    }
                    slot = (slot + 1) and mask
                }
            }

            val punctHead = IntArray(PUNCT_BUCKETS)
            var punctCount = 0
            for (e in 0 until entries) {
                if (entryFirst[e] != 0) continue
                punctCount++
            }
            val punctNext = IntArray(entries) { -1 }
            if (punctCount > 0) {
                for (e in entries - 1 downTo 0) {
                    if (entryFirst[e] != 0) continue

                    val c = Character.toLowerCase(pool[entryKeyAt[e]])
                    val bucket = c.code and (PUNCT_BUCKETS - 1)
                    punctNext[e] = punctHead[bucket] - 1
                    punctHead[bucket] = e + 1
                }
            }
            return WordIndex(
                pool.copyOf(pooled),
                entryKeyAt.copyOf(entries),
                entryKeyLen.copyOf(entries),
                entrySayAt.copyOf(entries),
                entrySayLen.copyOf(entries),
                next,
                table,
                hashes,
                mask,
                if (punctCount == 0) IntArray(0) else punctHead,
                if (punctCount == 0) IntArray(0) else punctNext,
                entries,
                dropped,
                pooled
            )
        }

        private fun grow() {
            val want = entries * 2
            entryKeyAt = entryKeyAt.copyOf(want)
            entryKeyLen = entryKeyLen.copyOf(want)
            entrySayAt = entrySayAt.copyOf(want)
            entrySayLen = entrySayLen.copyOf(want)
            entryFirst = entryFirst.copyOf(want)
        }

        private fun growPool(need: Int) {
            var want = pool.size
            while (want < need) want *= 2
            pool = pool.copyOf(want)
        }

        private fun fold(word: String): String {
            if (folded.size < word.length + 1) folded = CharArray((word.length + 1) * 2)
            var n = 0
            var spaced = true
            for (c in word) {
                val foldedChar = when {
                    c in 'A'..'Z' -> (c + 32)
                    c == '\t' || c == '\n' || c == '\r' || c == '\u000B' || c == '\u000C' -> ' '
                    else -> Character.toLowerCase(c)
                }
                if (foldedChar == ' ') {
                    if (spaced) continue
                    spaced = true
                    if (n <= MAX_KEY_CHARS) folded[n++] = ' '
                    continue
                }
                spaced = false
                folded[n++] = foldedChar
                if (n > MAX_KEY_CHARS) break
            }
            while (n > 0 && folded[n - 1] == ' ') n--
            if (n == 0) return ""
            return String(folded, 0, n)
        }

        private fun firstTokenLength(text: CharArray, from: Int, length: Int): Int {
            var at = 0
            while (at < length) {
                val c = text[from + at]
                if (!(c.isLetterOrDigit() || c == '\'')) break
                at++
            }
            return at
        }

        private fun hashOf(text: CharArray, from: Int, length: Int): Int {
            var hash = 1
            for (at in 0 until length) hash = hash * 31 + text[from + at].code
            return hash
        }
    }

    companion object {

        val EMPTY: WordIndex = Builder(budgetChars = 0, maxEntries = 0).build()

        const val DEFAULT_BUDGET_CHARS = 4 * 1024 * 1024

        const val DEFAULT_MAX_ENTRIES = 250_000

        const val MAX_KEY_CHARS = 128

        const val MAX_SAY_CHARS = 400

        private const val PUNCT_BUCKETS = 256

        fun of(entries: List<Pair<String, String>>): WordIndex {
            if (entries.isEmpty()) return EMPTY
            val builder = Builder()
            for ((word, say) in entries) {
                if (!builder.add(word, say) && builder.roomLeft <= 0) break
            }
            return builder.build()
        }
    }
}
