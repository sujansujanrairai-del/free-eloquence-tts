package com.blindtechnexus.freeeloquencetts

import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan

object TextStyles {

    private const val MAX_SPANS = 500

    fun annotate(text: CharSequence?): String {
        if (text == null) return ""
        return try {
            if (text !is Spanned) return text.toString()
            if (text.isEmpty()) return ""
            val spans = try {
                text.getSpans(0, text.length, Any::class.java)
            } catch (broken: Exception) {
                return text.toString()
            }
            if (spans.isEmpty()) return text.toString()
            val marks = ArrayList<Mark>(spans.size * 2)

            var replaced: BooleanArray? = null
            var count = 0
            for (span in spans) {
                if (count >= MAX_SPANS) break
                val label = try {
                    labelOf(span)
                } catch (broken: Exception) {
                    continue
                } ?: continue

                val bounds = try {
                    text.getSpanStart(span) to text.getSpanEnd(span)
                } catch (broken: Exception) {
                    continue
                }
                val from = bounds.first
                val to = bounds.second
                if (from < 0 || to <= from || from >= text.length) continue
                val start = from.coerceIn(0, text.length)
                val end = to.coerceIn(0, text.length)
                if (end <= start) continue
                if (span is ImageSpan) {

                    val described = imageOf(span)
                    if (replaced == null) replaced = BooleanArray(text.length)
                    for (at in start until end) replaced[at] = true
                    marks.add(Mark(start, false, true, described))
                    count++
                    continue
                }
                if (label.isEmpty()) {

                    val single = try {
                        singleOf(span)
                    } catch (broken: Exception) {
                        continue
                    } ?: continue
                    marks.add(Mark(start, false, true, single))
                } else {
                    marks.add(Mark(start, false, false, label))
                    marks.add(Mark(end, true, false, label))
                }
                count++
            }
            if (marks.isEmpty()) return text.toString()

            marks.sortWith(compareBy<Mark> { it.at }.thenBy { if (it.end) 0 else 1 })
            val out = StringBuilder(text.length + marks.size * 8)
            var i = 0
            var m = 0
            while (i <= text.length) {
                while (m < marks.size && marks[m].at == i) {
                    val mark = marks[m]
                    if (mark.end) out.append(" end ").append(mark.label).append(' ')
                    else out.append(' ').append(mark.label).append(' ')
                    m++
                }

                while (m < marks.size && marks[m].at < i) m++
                if (i < text.length) {

                    val skip = replaced
                    if (skip == null || !skip[i]) out.append(text[i])
                }
                i++
            }
            collapse(out.toString())
        } catch (broken: Throwable) {
            try {
                text.toString()
            } catch (failed: Throwable) {
                ""
            }
        }
    }

    private class Mark(val at: Int, val end: Boolean, val single: Boolean, val label: String)

    private fun labelOf(span: Any): String? = when (span) {
        is StyleSpan -> when (span.style and Typeface.BOLD_ITALIC) {
            Typeface.BOLD -> "bold"
            Typeface.ITALIC -> "italic"
            Typeface.BOLD_ITALIC -> "bold italic"
            else -> null
        }
        is UnderlineSpan -> "underline"
        is StrikethroughSpan -> "strikethrough"
        is SuperscriptSpan -> "superscript"
        is SubscriptSpan -> "subscript"
        is URLSpan -> "link"

        is ClickableSpan -> "link"
        is BulletSpan -> ""
        is QuoteSpan -> "quote"
        is TypefaceSpan -> fontOf(span)
        is RelativeSizeSpan -> {
            val proportion = span.sizeChange
            when {
                proportion >= 1.05f -> "larger text"
                proportion <= 0.95f -> "smaller text"
                else -> null
            }
        }
        is AbsoluteSizeSpan -> "different size text"
        is ForegroundColorSpan -> foregroundOf(span.foregroundColor)
        is BackgroundColorSpan -> backgroundOf(span.backgroundColor)
        is AlignmentSpan -> when (span.alignment) {
            Layout.Alignment.ALIGN_CENTER -> "centered"
            Layout.Alignment.ALIGN_OPPOSITE -> "right aligned"
            else -> null
        }
        else -> null
    }

    private fun singleOf(span: Any): String? = when (span) {
        is BulletSpan -> "bullet"
        else -> null
    }

    private fun imageOf(span: ImageSpan): String {
        val described = try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) span.contentDescription else null
        } catch (_: Throwable) {
            null
        }
        val said = try {
            span.source
        } catch (_: Throwable) {
            null
        }
        val label = (described?.takeIf { it.isNotBlank() } ?: said?.takeIf { it.isNotBlank() })
        if (label == null) return "image"
        val clean = label.filter { !it.isISOControl() }.trim().take(120)
        return if (clean.isEmpty()) "image" else "image $clean"
    }

    private fun fontOf(span: TypefaceSpan): String? {
        val family = try {
            span.family
        } catch (broken: Exception) {
            return null
        } ?: return "different font"
        if (family.isBlank()) return "different font"
        if (family.equals("monospace", ignoreCase = true)) return "monospace"
        val clean = family.filter { it.isLetterOrDigit() || it == ' ' }.trim().take(20)
        return if (clean.isEmpty()) "different font" else "font $clean"
    }

    private fun foregroundOf(color: Int): String? {
        if (alphaOf(color) < 24) return null
        val name = colorName(color) ?: return null

        if (name == "black" || name == "white") return null
        return name
    }

    private fun backgroundOf(color: Int): String? {
        if (alphaOf(color) < 24) return null
        val name = colorName(color) ?: return null
        if (name == "white") return null
        return "highlighted $name"
    }

    private fun alphaOf(color: Int): Int = (color ushr 24) and 0xFF

    private class NamedColor(val name: String, val red: Int, val green: Int, val blue: Int)

    private val COLORS = listOf(
        NamedColor("black", 0, 0, 0),
        NamedColor("white", 255, 255, 255),
        NamedColor("red", 255, 0, 0),
        NamedColor("green", 0, 128, 0),
        NamedColor("blue", 0, 0, 255),
        NamedColor("yellow", 255, 255, 0),
        NamedColor("cyan", 0, 255, 255),
        NamedColor("magenta", 255, 0, 255),
        NamedColor("gray", 128, 128, 128),
        NamedColor("orange", 255, 165, 0),
        NamedColor("purple", 128, 0, 128),
        NamedColor("brown", 165, 42, 42),
        NamedColor("pink", 255, 192, 203),
        NamedColor("lime", 0, 255, 0),
        NamedColor("navy", 0, 0, 128),
        NamedColor("teal", 0, 128, 128),
        NamedColor("maroon", 128, 0, 0),
        NamedColor("olive", 128, 128, 0)
    )

    private fun colorName(color: Int): String? {
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        var best: NamedColor? = null
        var bestBy = Int.MAX_VALUE
        for (named in COLORS) {
            val dr = red - named.red
            val dg = green - named.green
            val db = blue - named.blue
            val by = dr * dr + dg * dg + db * db
            if (by < bestBy) {
                bestBy = by
                best = named
            }
        }
        return best?.name
    }

    private fun collapse(text: String): String {
        val out = StringBuilder(text.length)
        var spaced = true
        for (c in text) {
            if (c.isWhitespace()) {
                if (!spaced) {
                    out.append(' ')
                    spaced = true
                }
            } else {
                out.append(c)
                spaced = false
            }
        }
        var end = out.length
        while (end > 0 && out[end - 1] == ' ') end--
        return if (end == out.length) out.toString() else out.substring(0, end)
    }
}
