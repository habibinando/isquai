package com.isqu.ai.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File

data class ExportChatRow(val role: String, val message: String, val timestampIso: String)

object ChatExporter {
    fun exportToCsv(context: Context, fileName: String, rows: List<ExportChatRow>): File {
        val outFile = File(context.getExternalFilesDir(null), "$fileName.csv")
        val sb = StringBuilder("﻿timestamp,role,message\n")
        rows.forEach { row ->
            val escaped = row.message.replace(""", """")
            sb.append(""${row.timestampIso}","${row.role}","$escaped"\n")
        }
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        return outFile
    }

    fun exportToPdf(context: Context, fileName: String, rows: List<ExportChatRow>): File {
        val outFile = File(context.getExternalFilesDir(null), "$fileName.pdf")
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val paint = Paint().apply { textSize = 11f }
        var y = 48f

        page.canvas.drawText("ISQU AI - Chat Report (com.isqu.ai)", 40f, y, paint)
        y += 24f

        rows.forEach { row ->
            if (y < 800f) {
                page.canvas.drawText("[${row.role}] ${row.message.take(72)}", 40f, y, paint)
                y += 18f
            }
        }
        doc.finishPage(page)
        outFile.outputStream().use { doc.writeTo(it) }
        doc.close()
        return outFile
    }
}
