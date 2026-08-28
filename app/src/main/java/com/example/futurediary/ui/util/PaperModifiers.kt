package com.example.futurediary.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.futurediary.ui.theme.VintageLeather
import com.example.futurediary.ui.theme.VintagePaperLines
import com.example.futurediary.ui.theme.VintageParchment
import com.example.futurediary.ui.theme.VintageRedMargin

/**
 * Applies a consistent "Journal Page" style to any component.
 * This is an extension function that bundles multiple modifiers together.
 */
fun Modifier.journalPage(
    padding: Dp = 8.dp,
    cornerRadius: Dp = 12.dp,
    borderWidth: Dp = 2.dp,
    shadowElevation: Dp = 4.dp
): Modifier = this
    .padding(padding)
    .shadow(shadowElevation, RoundedCornerShape(cornerRadius))
    .border(borderWidth, VintageLeather, RoundedCornerShape(cornerRadius))
    .clip(RoundedCornerShape(cornerRadius))
    .background(VintageParchment)
    .padding(padding) // Inner padding for the content inside the "page"

/**
 * Draws notebook lines and a vertical margin on the background of a component.
 */
fun Modifier.drawNotebookLines(
    lineSpacing: Dp = 32.dp,
    lineColor: Color = VintagePaperLines,
    marginColor: Color = VintageRedMargin,
    marginOffset: Dp = 40.dp
): Modifier = this.drawBehind {
    val spacingPx = lineSpacing.toPx()
    val marginPx = marginOffset.toPx()
    
    // Draw vertical margin line
    drawLine(
        color = marginColor,
        start = Offset(marginPx, 0f),
        end = Offset(marginPx, size.height),
        strokeWidth = 2.dp.toPx()
    )
    
    // Draw horizontal lines
    var currentY = spacingPx
    var safetyCounter = 0
    while (currentY < size.height && safetyCounter < 500) { // Safety guard: max 500 lines
        drawLine(
            color = lineColor,
            start = Offset(0f, currentY),
            end = Offset(size.width, currentY),
            strokeWidth = 1.dp.toPx()
        )
        currentY += spacingPx
        safetyCounter++
    }
}
