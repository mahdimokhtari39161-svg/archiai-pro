package com.archiaipro.reports

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.archiaipro.data.ProjectEntity
import java.io.File
import java.io.FileOutputStream

fun shareProjectPdf(context: Context, projects: List<ProjectEntity>) {
    val document = PdfDocument()
    val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas
    val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        color = android.graphics.Color.rgb(20, 34, 56)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; color = android.graphics.Color.DKGRAY }
    canvas.drawText("ArchiAI Pro - Project Report", 40f, 55f, heading)
    canvas.drawText("Number of projects: ${projects.size}", 40f, 83f, body)
    var y = 120f
    projects.forEachIndexed { index, project ->
        if (y > 760f) return@forEachIndexed
        heading.textSize = 14f
        canvas.drawText("${index+1}. ${project.name} | ${project.type}".take(85), 40f, y, heading)
        y += 22f
        listOf("Area: ${project.area} m2", "Location: ${project.location}", "Description: ${project.description}")
            .forEach { line -> if (y < 810f) { canvas.drawText(line.take(100), 48f, y, body); y += 17f } }
        y += 12f
    }
    document.finishPage(page)
    val folder = File(context.cacheDir, "reports").apply { mkdirs() }
    val output = File(folder, "archiai-project-report.pdf")
    FileOutputStream(output).use { document.writeTo(it) }
    document.close()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF report"))
}

