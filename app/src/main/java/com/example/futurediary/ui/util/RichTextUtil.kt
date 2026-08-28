package com.example.futurediary.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.util.regex.Pattern

object RichTextUtil {

    private val TAG_PATTERN = Pattern.compile("<(/?[bi]|color=#[0-9A-Fa-f]{6,8}|/color|font=handwriting|/font)>")

    /**
     * Converts markup string (e.g., "Hello <b>World</b>") to AnnotatedString.
     */
    fun parseMarkup(text: String): AnnotatedString = buildAnnotatedString {
        val matcher = TAG_PATTERN.matcher(text)
        var lastPos = 0
        
        // Stack to track active styles
        val stack = mutableListOf<Pair<String, Int>>() // Tag, StartPosition in AnnotatedString
        
        while (matcher.find()) {
            val tag = matcher.group()
            // Append plain text before tag
            append(text.substring(lastPos, matcher.start()))
            
            if (tag.startsWith("</")) {
                // Closing tag
                val tagName = tag.substring(2, tag.length - 1).split("=")[0]
                val index = stack.indexOfLast { it.first.startsWith(tagName) }
                if (index != -1) {
                    val (openedTag, start) = stack.removeAt(index)
                    val end = length
                    
                    val style = when {
                        openedTag == "b" -> SpanStyle(fontWeight = FontWeight.Bold)
                        openedTag == "i" -> SpanStyle(fontStyle = FontStyle.Italic)
                        openedTag.startsWith("color") -> {
                            val hex = openedTag.substringAfter("=")
                            SpanStyle(color = Color(android.graphics.Color.parseColor(hex)))
                        }
                        openedTag == "font=handwriting" -> SpanStyle(fontFamily = FontFamily.Serif)
                        else -> SpanStyle()
                    }
                    addStyle(style, start, end)
                }
            } else {
                // Opening tag
                val tagName = if (tag.contains("=")) tag.substring(1, tag.indexOf("=")) else tag.substring(1, tag.length - 1)
                val fullTagName = if (tag.contains("=")) tag.substring(1, tag.length - 1) else tagName
                stack.add(fullTagName to length)
            }
            lastPos = matcher.end()
        }
        append(text.substring(lastPos))
        
        // Close remaining tags at the end
        while (stack.isNotEmpty()) {
            val (openedTag, start) = stack.removeAt(stack.size - 1)
            val end = length
            val style = when {
                openedTag == "b" -> SpanStyle(fontWeight = FontWeight.Bold)
                openedTag == "i" -> SpanStyle(fontStyle = FontStyle.Italic)
                openedTag.startsWith("color") -> {
                    val hex = openedTag.substringAfter("=")
                    SpanStyle(color = Color(android.graphics.Color.parseColor(hex)))
                }
                openedTag == "font=handwriting" -> SpanStyle(fontFamily = FontFamily.Serif)
                else -> SpanStyle()
            }
            addStyle(style, start, end)
        }
    }

    /**
     * Strips all formatting tags from the markup.
     */
    fun stripFormatting(text: String): String {
        return text.replace(Regex("<[^>]*>"), "")
    }

    /**
     * Clears all styles from a specific range in an AnnotatedString.
     */
    fun clearStylesInRange(annotatedString: AnnotatedString, start: Int, end: Int): AnnotatedString {
        val builder = AnnotatedString.Builder(annotatedString.text)
        annotatedString.spanStyles.forEach { range ->
            // If the style is completely outside the range, keep it
            if (range.end <= start || range.start >= end) {
                builder.addStyle(range.item, range.start, range.end)
            } else {
                // Style overlaps with the cleared range.
                // Keep the part before the range
                if (range.start < start) {
                    builder.addStyle(range.item, range.start, start)
                }
                // Keep the part after the range
                if (range.end > end) {
                    builder.addStyle(range.item, end, range.end)
                }
            }
        }
        // Also copy paragraph styles and annotations if needed, but for now focus on spanStyles
        annotatedString.paragraphStyles.forEach { range ->
             builder.addStyle(range.item, range.start, range.end)
        }
        return builder.toAnnotatedString()
    }

    /**
     * Converts AnnotatedString back to markup.
     * Note: This is complex for arbitrary spans. We'll implement a simplified version
     * that works with the styles we apply (Bold, Italic, Color).
     */
    fun toMarkup(annotatedString: AnnotatedString): String {
        val text = annotatedString.text
        val result = StringBuilder()
        
        // We'll track which characters have which styles
        val isBold = BooleanArray(text.length)
        val isItalic = BooleanArray(text.length)
        val isHandwriting = BooleanArray(text.length)
        val colors = arrayOfNulls<String>(text.length)
        
        annotatedString.spanStyles.forEach { range ->
            for (i in range.start until range.end) {
                if (i >= text.length) continue
                if (range.item.fontWeight == FontWeight.Bold) isBold[i] = true
                if (range.item.fontStyle == FontStyle.Italic) isItalic[i] = true
                if (range.item.fontFamily == FontFamily.Serif) isHandwriting[i] = true
                if (range.item.color != Color.Unspecified) {
                    colors[i] = String.format("#%08X", range.item.color.toArgb())
                }
            }
        }
        
        var currentBold = false
        var currentItalic = false
        var currentHandwriting = false
        var currentColor: String? = null
        
        for (i in text.indices) {
            // Close tags if necessary (FILO order recommended, but diary use case is simple)
            if (currentColor != null && colors[i] != currentColor) {
                result.append("</color>")
                currentColor = null
            }
            if (currentHandwriting && !isHandwriting[i]) {
                result.append("</font>")
                currentHandwriting = false
            }
            if (currentItalic && !isItalic[i]) {
                result.append("</i>")
                currentItalic = false
            }
            if (currentBold && !isBold[i]) {
                result.append("</b>")
                currentBold = false
            }
            
            // Open tags if necessary
            if (isBold[i] && !currentBold) {
                result.append("<b>")
                currentBold = true
            }
            if (isItalic[i] && !currentItalic) {
                result.append("<i>")
                currentItalic = true
            }
            if (isHandwriting[i] && !currentHandwriting) {
                result.append("<font=handwriting>")
                currentHandwriting = true
            }
            if (colors[i] != null && colors[i] != currentColor) {
                currentColor = colors[i]
                result.append("<color=$currentColor>")
            }
            
            result.append(text[i])
        }
        
        // Final closes
        if (currentColor != null) result.append("</color>")
        if (currentHandwriting) result.append("</font>")
        if (currentItalic) result.append("</i>")
        if (currentBold) result.append("</b>")
        
        return result.toString()
    }
}
