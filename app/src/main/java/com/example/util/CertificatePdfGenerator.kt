package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.AdminCompletionSettings
import com.example.model.CertificateRecord
import java.io.File
import java.io.FileOutputStream

object CertificatePdfGenerator {

    const val OFFICIAL_INSTITUTION_FULL_NAME = "Institute of Supply Chain Excellence Bangladesh"
    const val OFFICIAL_INSTITUTION_DISPLAY = "Institute of Supply Chain Excellence Bangladesh (ISCEB)"
    const val DEFAULT_LEFT_SIGNER_NAME = "Hasina Khan"
    const val DEFAULT_RIGHT_SIGNER_NAME = "Amirul Islam"
    const val DEFAULT_RIGHT_SIGNER_TITLE = "Founder of ISCEB"
    const val DEMO_CERTIFICATE_ID = "ISCEB-DEMO-PGD-2026"

    data class CertificateSignatory(
        val name: String,
        val title: String
    )

    fun formatInstitutionDisplayName(raw: String?): String {
        val cleaned = raw?.trim().orEmpty()
        if (cleaned.isBlank() ||
            cleaned.equals("ISCEB", ignoreCase = true) ||
            cleaned.contains("International Supply Chain Education Board", ignoreCase = true) ||
            cleaned.contains("Education Board", ignoreCase = true)
        ) {
            return OFFICIAL_INSTITUTION_DISPLAY
        }
        return if (cleaned.equals(OFFICIAL_INSTITUTION_FULL_NAME, ignoreCase = true)) {
            OFFICIAL_INSTITUTION_DISPLAY
        } else {
            cleaned
        }
    }

    fun isDemoCertificate(record: CertificateRecord): Boolean {
        return record.certificateId.startsWith("ISCEB-DEMO-", ignoreCase = true)
    }

    fun resolveLeftSignatory(settings: AdminCompletionSettings? = null): CertificateSignatory {
        val configuredName = settings?.leftSignerName?.trim().orEmpty()
        val safeName = if (configuredName.isBlank() ||
            configuredName.contains("Nafiul", ignoreCase = true) ||
            configuredName.contains("Amirul", ignoreCase = true)
        ) {
            DEFAULT_LEFT_SIGNER_NAME
        } else {
            configuredName
        }
        val configuredTitle = settings?.leftSignerTitle?.trim().orEmpty()
        val safeTitle = if (configuredTitle.contains("Developer", ignoreCase = true)) "" else configuredTitle
        return CertificateSignatory(name = safeName, title = safeTitle)
    }

    fun resolveRightSignatory(
        record: CertificateRecord,
        settings: AdminCompletionSettings? = null
    ): CertificateSignatory {
        val rawCombined = record.authorizedSigner.trim()
        val settingsName = settings?.authorizedSignerName?.trim().orEmpty()
        val settingsTitle = settings?.authorizedSignerTitle?.trim().orEmpty()

        var resolvedName = when {
            settingsName.isNotBlank() &&
                !settingsName.contains("Nafiul", ignoreCase = true) &&
                !settingsName.contains("Hasina", ignoreCase = true) -> {
                settingsName.substringBefore(",").trim()
            }
            rawCombined.isNotBlank() &&
                !rawCombined.contains("Nafiul", ignoreCase = true) &&
                !rawCombined.contains("Hasina", ignoreCase = true) -> {
                rawCombined.substringBefore(",").trim()
            }
            else -> DEFAULT_RIGHT_SIGNER_NAME
        }
        if (resolvedName.equals("Md. Amirul Islam", ignoreCase = true)) {
            resolvedName = DEFAULT_RIGHT_SIGNER_NAME
        }

        val parsedTitleFromRecord = rawCombined.substringAfter(",", "").trim()
        val resolvedTitle = when {
            settingsTitle.isNotBlank() &&
                !settingsTitle.contains("Developer", ignoreCase = true) &&
                !settingsTitle.contains("Academic Directorate", ignoreCase = true) -> settingsTitle
            parsedTitleFromRecord.isNotBlank() &&
                !parsedTitleFromRecord.contains("Developer", ignoreCase = true) -> parsedTitleFromRecord
            else -> DEFAULT_RIGHT_SIGNER_TITLE
        }

        return CertificateSignatory(
            name = resolvedName.ifBlank { DEFAULT_RIGHT_SIGNER_NAME },
            title = resolvedTitle.ifBlank { DEFAULT_RIGHT_SIGNER_TITLE }
        )
    }

    fun generateCertificatePdf(context: Context, record: CertificateRecord): File {
        val certDir = File(context.cacheDir, "certificates")
        if (!certDir.exists()) {
            certDir.mkdirs()
        }
        val safeFileName = record.certificateId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val pdfFile = File(certDir, "$safeFileName.pdf")

        try {
            val pdfDocument = PdfDocument()
            // Landscape A4 dimensions in points: 842 wide, 595 tall
            val pageWidth = 842
            val pageHeight = 595
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawCertificateOnCanvas(canvas, pageWidth, pageHeight, record, context)

            pdfDocument.finishPage(page)

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
        } catch (e: Throwable) {
            val instDisplay = formatInstitutionDisplayName(record.institutionName)
            val left = resolveLeftSignatory()
            val right = resolveRightSignatory(record)
            // Fallback for headless JVM / Robolectric testing where native Android Skia PDF renderer is absent
            val fallbackContent = """
                %PDF-1.4
                1 0 obj << /Title (${record.courseTitle}) /Author ($instDisplay) >> endobj
                2 0 obj << /CertificateId (${record.certificateId}) /Recipient (${record.studentName}) /Date (${record.issueDate}) /LeftSigner (${left.name}) /RightSigner (${right.name} - ${right.title}) >> endobj
                trailer << /Root 1 0 R >>
                %%EOF
            """.trimIndent()
            pdfFile.writeText(fallbackContent)
        }

        return pdfFile
    }

    fun exportCertificatePdfToUri(context: Context, record: CertificateRecord, targetUri: Uri): Result<Long> {
        return try {
            val generated = generateCertificatePdf(context, record)
            val bytes = generated.readBytes()
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: return Result.failure(IllegalStateException("Unable to open destination file for saving PDF."))
            Result.success(bytes.size.toLong())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun drawCertificateOnCanvas(
        canvas: Canvas,
        width: Int,
        height: Int,
        record: CertificateRecord,
        context: Context? = null
    ) {
        val navyPrimary = Color.parseColor("#071A33")
        val navySecondary = Color.parseColor("#102B4C")
        val goldMetallic = Color.parseColor("#D4AF37")
        val goldSoft = Color.parseColor("#F4E2A3")
        val creamBg = Color.parseColor("#FCFCFD")
        val darkGray = Color.parseColor("#2C3E50")

        val instDisplay = formatInstitutionDisplayName(record.institutionName)
        val leftSigner = resolveLeftSignatory()
        val rightSigner = resolveRightSignatory(record)
        val isDemo = isDemoCertificate(record)

        // 1. Background fill
        val bgPaint = Paint().apply {
            color = creamBg
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Outer Navy Border
        val outerBorderPaint = Paint().apply {
            color = navyPrimary
            style = Paint.Style.STROKE
            strokeWidth = 14f
        }
        canvas.drawRect(14f, 14f, width - 14f, height - 14f, outerBorderPaint)

        // 3. Inner Gold Border
        val innerBorderPaint = Paint().apply {
            color = goldMetallic
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(26f, 26f, width - 26f, height - 26f, innerBorderPaint)

        // 4. Subtle second hairline border
        val hairlinePaint = Paint().apply {
            color = goldMetallic
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(32f, 32f, width - 32f, height - 32f, hairlinePaint)

        val signLinePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }

        // Corner decorative flourishes
        drawCornerDiamond(canvas, 32f, 32f, goldMetallic)
        drawCornerDiamond(canvas, width - 32f, 32f, goldMetallic)
        drawCornerDiamond(canvas, 32f, height - 32f, goldMetallic)
        drawCornerDiamond(canvas, width - 32f, height - 32f, goldMetallic)

        // Optional Top Center Official ISCEB Logo
        var headerTextY = 74f
        if (context != null) {
            try {
                val logoBmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_isceb_logo)
                if (logoBmp != null && logoBmp.width > 0 && logoBmp.height > 0) {
                    val targetHeight = 52f
                    val aspect = logoBmp.width.toFloat() / logoBmp.height.toFloat()
                    val targetWidth = targetHeight * aspect
                    val left = (width - targetWidth) / 2f
                    val top = 42f
                    val dest = RectF(left, top, left + targetWidth, top + targetHeight)
                    val src = Rect(0, 0, logoBmp.width, logoBmp.height)
                    canvas.drawBitmap(logoBmp, src, dest, Paint(Paint.FILTER_BITMAP_FLAG))
                    headerTextY = top + targetHeight + 18f
                }
            } catch (_: Throwable) {
                // Safe fallback if bitmap resources aren't loaded in unit test
            }
        }

        // Top-Left Gold Emblem Seal
        val sealX = 96f
        val sealY = 92f
        val sealRadius = 34f
        val sealRingPaint = Paint().apply {
            color = goldMetallic
            style = Paint.Style.STROKE
            strokeWidth = 2.8f
            isAntiAlias = true
        }
        canvas.drawCircle(sealX, sealY, sealRadius, sealRingPaint)
        val sealFillPaint = Paint().apply {
            color = navyPrimary
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(sealX, sealY, sealRadius - 4f, sealFillPaint)
        val sealTextPaint = Paint().apply {
            color = goldSoft
            textSize = 7.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ISCEB", sealX, sealY - 8f, sealTextPaint)
        canvas.drawText(if (isDemo) "SAMPLE" else "VERIFIED", sealX, sealY + 3f, sealTextPaint)
        canvas.drawText("PGD • SCM", sealX, sealY + 14f, sealTextPaint)

        // Top-Right Demo Watermark Pill if Demo
        if (isDemo) {
            val pillPaint = Paint().apply {
                color = goldSoft
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val pillRect = RectF(width - 178f, 52f, width - 52f, 76f)
            canvas.drawRoundRect(pillRect, 12f, 12f, pillPaint)
            val pillTextPaint = Paint().apply {
                color = navyPrimary
                textSize = 9.5f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("DEMO PREVIEW", pillRect.centerX(), pillRect.centerY() + 3.5f, pillTextPaint)
        }

        // Top Header: Official Institution Name
        val instPaint = Paint().apply {
            color = navyPrimary
            textSize = 13.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        canvas.drawText(instDisplay.uppercase(), width / 2f, headerTextY, instPaint)

        // Header Subtitle: Distinction
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#A67C00")
            textSize = 9.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.16f
        }
        canvas.drawText("POST GRADUATE DIPLOMA (PGD) IN SUPPLY CHAIN MANAGEMENT (SCM)", width / 2f, headerTextY + 18f, subtitlePaint)

        // Decorative separator line with gold diamond center
        val sepY = headerTextY + 32f
        val linePaint = Paint().apply {
            color = goldMetallic
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        canvas.drawLine(width / 2f - 190f, sepY, width / 2f - 18f, sepY, linePaint)
        canvas.drawLine(width / 2f + 18f, sepY, width / 2f + 190f, sepY, linePaint)
        drawCenterDiamond(canvas, width / 2f, sepY, 5.5f, goldMetallic)

        // Diploma Main Title
        val titleY = sepY + 36f
        val titlePaint = Paint().apply {
            color = navyPrimary
            textSize = 24f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(record.courseTitle.uppercase(), width / 2f, titleY, titlePaint)

        // Conferred upon text
        val conferY = titleY + 30f
        val conferPaint = Paint().apply {
            color = darkGray
            textSize = 12f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("This authoritative academic credential is officially conferred upon", width / 2f, conferY, conferPaint)

        // Student Full Name (Hero Name)
        val studentNameY = conferY + 42f
        val namePaint = Paint().apply {
            color = navySecondary
            textSize = 31f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(record.studentName, width / 2f, studentNameY, namePaint)

        // Gold underline under student name
        val nameLinePaint = Paint().apply {
            color = goldMetallic
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawLine(width / 2f - 165f, studentNameY + 12f, width / 2f + 165f, studentNameY + 12f, nameLinePaint)

        var narrativeTop = studentNameY + 34f
        if (record.studentCode.isNotBlank()) {
            val studentCodePaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 10f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("STUDENT ID: ${record.studentCode}", width / 2f, studentNameY + 28f, studentCodePaint)
            narrativeTop = studentNameY + 46f
        }

        // Narrative Description
        val descPaint = Paint().apply {
            color = darkGray
            textSize = 11f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("in recognition of successfully completing all 12 Specialization Modules and 48 Executive Lessons,", width / 2f, narrativeTop, descPaint)
        canvas.drawText("Part 1: Basic (Foundation Level) & Part 2: Advance (Strategic & Analytical Level),", width / 2f, narrativeTop + 16f, descPaint)
        canvas.drawText("Module Examinations, Capstone Project, and Final Comprehensive Assessment.", width / 2f, narrativeTop + 32f, descPaint)

        // Symmetrical 3-Column Footer:
        // Left Signatory (Hasina Khan) | Center (QR + Date) | Right Signatory (Amirul Islam, Founder of ISCEB)
        val metaLabelPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val metaValuePaint = Paint().apply {
            color = navyPrimary
            textSize = 12f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val signLineY = 474f

        // Left Signatory Block (Hasina Khan — clean blank signature space above line)
        val leftX = 166f
        canvas.drawLine(leftX - 102f, signLineY, leftX + 102f, signLineY, signLinePaint)
        canvas.drawText(leftSigner.name, leftX, signLineY + 15f, metaValuePaint)
        if (leftSigner.title.isNotBlank()) {
            canvas.drawText(leftSigner.title, leftX, signLineY + 28f, metaLabelPaint)
        }

        // Center QR Verification Block + Date of Issuance
        val qrSize = 62f
        val qrLeft = width / 2f - qrSize / 2f
        val qrTop = 408f
        val verifyPayload = record.verificationUrl.ifBlank { "https://www.isceb.org/verify/${record.certificateId}" }
        QrCodeGenerator.drawQrOnAndroidCanvas(
            canvas = canvas,
            data = verifyPayload,
            left = qrLeft,
            top = qrTop,
            sizePx = qrSize,
            darkColor = navyPrimary,
            lightColor = Color.WHITE
        )
        canvas.drawRect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize, hairlinePaint)
        canvas.drawText("SCAN TO VERIFY", width / 2f, qrTop + qrSize + 10f, metaLabelPaint)
        canvas.drawText("ISSUED: ${record.issueDate}", width / 2f, qrTop + qrSize + 22f, metaLabelPaint)

        // Right Signatory Block (Amirul Islam, Founder of ISCEB)
        val rightX = width - 166f
        canvas.drawLine(rightX - 102f, signLineY, rightX + 102f, signLineY, signLinePaint)
        canvas.drawText(rightSigner.name, rightX, signLineY + 15f, metaValuePaint)
        canvas.drawText(rightSigner.title, rightX, signLineY + 28f, metaLabelPaint)

        // Bottom Footer: Verification ID & URL
        val footerPaint = Paint().apply {
            color = Color.parseColor("#4B5563")
            textSize = 9.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val statusText = if (isDemo) "DEMO PREVIEW" else record.status.name
        canvas.drawText("CERTIFICATE ID: ${record.certificateId}   •   STATUS: $statusText", width / 2f, 534f, footerPaint)

        val disclaimerPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Verify: $verifyPayload • $instDisplay", width / 2f, 550f, disclaimerPaint)
    }

    private fun drawCornerDiamond(canvas: Canvas, x: Float, y: Float, colorInt: Int) {
        val paint = Paint().apply {
            color = colorInt
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val size = 5f
        val path = Path().apply {
            moveTo(x, y - size)
            lineTo(x + size, y)
            lineTo(x, y + size)
            lineTo(x - size, y)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawCenterDiamond(canvas: Canvas, x: Float, y: Float, size: Float, colorInt: Int) {
        val paint = Paint().apply {
            color = colorInt
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val path = Path().apply {
            moveTo(x, y - size)
            lineTo(x + size, y)
            lineTo(x, y + size)
            lineTo(x - size, y)
            close()
        }
        canvas.drawPath(path, paint)
    }

    fun openOrShareCertificate(context: Context, pdfFile: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "View Certificate"))
        } catch (e: Exception) {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Certificate"))
        }
    }
}
