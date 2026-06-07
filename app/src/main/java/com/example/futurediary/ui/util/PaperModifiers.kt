package com.example.futurediary.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.futurediary.ui.theme.VintagePaperLines
import com.example.futurediary.ui.theme.VintageRedMargin

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
    while (currentY < size.height) {
        drawLine(
            color = lineColor,
            start = Offset(0f, currentY),
            end = Offset(size.width, currentY),
            strokeWidth = 1.dp.toPx()
        )
        currentY += spacingPx
    }
}
