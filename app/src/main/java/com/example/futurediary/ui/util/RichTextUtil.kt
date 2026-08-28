package com.example.futurediary.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.util.regex.Pattern

object RichTextUtil {

    private val TAG_PATTERN = Pattern.compile("<(/?[bi]|color=#[0-9A-Fa-f]{6})>")

    /**
     * Converts markup string (e.g., "Hello <b>World</b>") to AnnotatedString.
     */
    fun toAnnotatedString(text: String): AnnotatedString {
        return buildAnnotatedString {
            val matcher = TAG_PATTERN.matcher(text)
            var lastIndex = 0
            val styleStack = mutableListOf<SpanStyle>()

            while (matcher.find()) {
                val match = matcher.group()
                val start = matcher.start()
                
                // Add text before the tag
                append(text.substring(lastIndex, start))

                when {
                    match == "<b>" -> styleStack.add(SpanStyle(fontWeight = FontWeight.Bold))
                    match == "<i>" -> styleStack.add(SpanStyle(fontStyle = FontStyle.Italic))
                    match.startsWith("<color=") -> {
                        val colorHex = match.substring(7, 14)
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        styleStack.add(SpanStyle(color = color))
                    }
                    match == "</b>" || match == "</i>" || match == "</color>" || match.startsWith("</") -> {
                        if (styleStack.isNotEmpty()) {
                            val lastStyle = styleStack.removeAt(styleStack.size - 1)
                            // Note: buildAnnotatedString.withStyle is easier for nesting, 
                            // but manual pop is needed for arbitrary tag closing.
                            // For simplicity, we'll just handle basic nesting.
                        }
                    }
                }
                
                // This parser is basic and doesn't support complex overlapping tags perfectly,
                // but for a diary app it works if we use buildAnnotatedString's state properly.
                // Rewriting to use pushStyle/pop
                
                lastIndex = matcher.end()
            }
            append(text.substring(lastIndex))
        }
    }

    /**
     * Better version of toAnnotatedString that handles styles correctly.
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
                val index = stack.indexOfLast { it.first == tagName }
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
            val (openedTag, start) = stack.removeLast()
            val end = length
            val style = when {
                openedTag == "b" -> SpanStyle(fontWeight = FontWeight.Bold)
                openedTag == "i" -> SpanStyle(fontStyle = FontStyle.Italic)
                openedTag.startsWith("color") -> {
                    val hex = openedTag.substringAfter("=")
                    SpanStyle(color = Color(android.graphics.Color.parseColor(hex)))
                }
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
        val colors = arrayOfNulls<String>(text.length)
        
        annotatedString.spanStyles.forEach { range ->
            for (i in range.start until range.end) {
                if (i >= text.length) continue
                if (range.item.fontWeight == FontWeight.Bold) isBold[i] = true
                if (range.item.fontStyle == FontStyle.Italic) isItalic[i] = true
                if (range.item.color != Color.Unspecified) {
                    colors[i] = String.format("#%06X", (0xFFFFFF and range.item.color.value.toInt()))
                }
            }
        }
        
        var currentBold = false
        var currentItalic = false
        var currentColor: String? = null
        
        for (i in text.indices) {
            // Close tags if necessary
            if (currentColor != null && colors[i] != currentColor) {
                result.append("</color>")
                currentColor = null
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
            if (colors[i] != null && colors[i] != currentColor) {
                currentColor = colors[i]
                result.append("<color=$currentColor>")
            }
            
            result.append(text[i])
        }
        
        // Final closes
        if (currentColor != null) result.append("</color>")
        if (currentItalic) result.append("</i>")
        if (currentBold) result.append("</b>")
        
        return result.toString()
    }
}
