package com.example.futurediary.ui.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.futurediary.data.model.DiaryEntryWithImages
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object JournalPdfExporter {

    fun exportToPdf(context: Context, entries: List<DiaryEntryWithImages>): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.DEFAULT_BOLD
            textSize = 24f
        }
        val datePaint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.GRAY
        }
        val bodyPaint = Paint().apply {
            textSize = 14f
        }

        // Page settings: A4 size
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        entries.forEach { entryWithImages ->
            val entry = entryWithImages.entry
            var myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var myPage = pdfDocument.startPage(myPageInfo)
            var canvas = myPage.canvas

            var yPos = 50f

            // Draw Title
            canvas.drawText(entry.title, 50f, yPos, titlePaint)
            yPos += 30f

            // Draw Date
            val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
            canvas.drawText(sdf.format(Date(entry.date)), 50f, yPos, datePaint)
            yPos += 40f

            // Draw Content (Simplified: doesn't handle rich text markup or wrapping perfectly yet)
            val plainContent = RichTextUtil.stripFormatting(entry.content)
            val lines = plainContent.split("\n")
            lines.forEach { line ->
                // Basic text wrapping could be added here
                canvas.drawText(line, 50f, yPos, bodyPaint)
                yPos += 20f
                
                // If page full, we should technically start a new page
                if (yPos > pageHeight - 50) {
                    pdfDocument.finishPage(myPage)
                    pageNumber++
                    myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    myPage = pdfDocument.startPage(myPageInfo)
                    canvas = myPage.canvas
                    yPos = 50f
                }
            }

            pdfDocument.finishPage(myPage)
            pageNumber++
        }

        val fileName = "MyJournal_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
