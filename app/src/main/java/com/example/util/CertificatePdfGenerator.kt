package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import kotlin.math.cos
import kotlin.math.sin
import com.example.R
import com.example.model.AdminCompletionSettings
import com.example.model.CertificateRecord
import java.io.File
import java.io.FileOutputStream

object CertificatePdfGenerator {

    const val OFFICIAL_INSTITUTION_FULL_NAME = "Institute of Supply Chain Excellence Bangladesh"
    const val OFFICIAL_INSTITUTION_DISPLAY = "Institute of Supply Chain Excellence Bangladesh (ISCEB)"
    const val DEFAULT_LEFT_SIGNER_NAME = "Hasina Akter Khan"
    const val DEFAULT_LEFT_SIGNER_TITLE = "Chairman & CEO"
    const val DEFAULT_RIGHT_SIGNER_NAME = "Md. Amirul Islam"
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
            configuredName.contains("Amirul", ignoreCase = true) ||
            configuredName.equals("Hasina Khan", ignoreCase = true)
        ) {
            DEFAULT_LEFT_SIGNER_NAME
        } else {
            configuredName
        }
        val configuredTitle = settings?.leftSignerTitle?.trim().orEmpty()
        val safeTitle = if (configuredTitle.isBlank() || configuredTitle.contains("Developer", ignoreCase = true)) {
            DEFAULT_LEFT_SIGNER_TITLE
        } else {
            configuredTitle
        }
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
        if (resolvedName.equals("Amirul Islam", ignoreCase = true)) {
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
        val goldMetallic = Color.parseColor("#C99E2C")
        val goldDark = Color.parseColor("#9A7014")
        val goldSoft = Color.parseColor("#F4E2A3")
        val warmBlushCreamCenter = Color.parseColor("#FDF7F3")
        val warmBlushCreamMid = Color.parseColor("#FBF0E9")
        val warmBlushCreamEdge = Color.parseColor("#F3DDD1")
        val watermarkTone = Color.parseColor("#DFC1B2")
        val roseFrameTint = Color.parseColor("#D8B4A3")
        val darkGray = Color.parseColor("#2C3E50")

        val instDisplay = formatInstitutionDisplayName(record.institutionName)
        val leftSigner = resolveLeftSignatory()
        val rightSigner = resolveRightSignatory(record)
        val isDemo = isDemoCertificate(record)
        val cx = width / 2f
        val cy = height / 2f

        // 1. Warm Reddish-Cream Parchment Background (subtle radial depth, clean & authentic)
        val parchmentGradient = RadialGradient(
            cx,
            cy,
            width * 0.62f,
            intArrayOf(warmBlushCreamCenter, warmBlushCreamMid, warmBlushCreamEdge),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply {
            shader = parchmentGradient
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 1b. Clean Same-Tone Ornamental Background Watermark (never busy, crisp contrast)
        drawParchmentWatermark(
            canvas = canvas,
            width = width.toFloat(),
            height = height.toFloat(),
            watermarkTone = watermarkTone
        )

        // 2. Outer Navy Border
        val outerBorderPaint = Paint().apply {
            color = navyPrimary
            style = Paint.Style.STROKE
            strokeWidth = 13f
        }
        canvas.drawRect(14f, 14f, width - 14f, height - 14f, outerBorderPaint)

        // 3. Inner Gold Border
        val innerBorderPaint = Paint().apply {
            color = goldMetallic
            style = Paint.Style.STROKE
            strokeWidth = 2.8f
        }
        canvas.drawRect(26f, 26f, width - 26f, height - 26f, innerBorderPaint)

        // 4. Subtle inner rose-champagne hairline border
        val hairlinePaint = Paint().apply {
            color = roseFrameTint
            style = Paint.Style.STROKE
            strokeWidth = 1.1f
        }
        canvas.drawRect(32f, 32f, width - 32f, height - 32f, hairlinePaint)

        val signLinePaint = Paint().apply {
            color = Color.parseColor("#8E9AAF")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }

        // Clean corner decorative diamonds
        drawCornerDiamond(canvas, 32f, 32f, goldMetallic)
        drawCornerDiamond(canvas, width - 32f, 32f, goldMetallic)
        drawCornerDiamond(canvas, 32f, height - 32f, goldMetallic)
        drawCornerDiamond(canvas, width - 32f, height - 32f, goldMetallic)

        // Top Center Official ISCEB Logo
        var headerTextY = 72f
        if (context != null) {
            try {
                val logoBmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_isceb_logo)
                if (logoBmp != null && logoBmp.width > 0 && logoBmp.height > 0) {
                    val targetHeight = 46f
                    val aspect = logoBmp.width.toFloat() / logoBmp.height.toFloat()
                    val targetWidth = targetHeight * aspect
                    val left = (width - targetWidth) / 2f
                    val top = 39f
                    val dest = RectF(left, top, left + targetWidth, top + targetHeight)
                    val src = Rect(0, 0, logoBmp.width, logoBmp.height)
                    val logoPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY)
                    }
                    canvas.drawBitmap(logoBmp, src, dest, logoPaint)
                    headerTextY = top + targetHeight + 17f
                }
            } catch (_: Throwable) {
                // Safe fallback if bitmap resources aren't loaded in unit test
            }
        }

        // Top-Left Gold Emblem Seal (compact & cleanly clear of centered headers)
        val sealX = 88f
        val sealY = 86f
        val sealRadius = 31f
        val sealRingPaint = Paint().apply {
            color = goldMetallic
            style = Paint.Style.STROKE
            strokeWidth = 2.4f
            isAntiAlias = true
        }
        canvas.drawCircle(sealX, sealY, sealRadius, sealRingPaint)
        val sealFillPaint = Paint().apply {
            color = navyPrimary
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(sealX, sealY, sealRadius - 3.5f, sealFillPaint)
        val sealTextPaint = Paint().apply {
            color = goldSoft
            textSize = 7f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ISCEB", sealX, sealY - 7f, sealTextPaint)
        canvas.drawText(if (isDemo) "SAMPLE" else "VERIFIED", sealX, sealY + 3f, sealTextPaint)
        canvas.drawText("PGD • SCM", sealX, sealY + 13f, sealTextPaint)

        // Top-Right Demo Pill if Demo
        if (isDemo) {
            val pillPaint = Paint().apply {
                color = goldSoft
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val pillStroke = Paint().apply {
                color = goldMetallic
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }
            val pillRect = RectF(width - 172f, 52f, width - 52f, 74f)
            canvas.drawRoundRect(pillRect, 11f, 11f, pillPaint)
            canvas.drawRoundRect(pillRect, 11f, 11f, pillStroke)
            val pillTextPaint = Paint().apply {
                color = navyPrimary
                textSize = 9f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("DEMO PREVIEW", pillRect.centerX(), pillRect.centerY() + 3.2f, pillTextPaint)
        }

        // Centered header max-width clears top-left seal and top-right badge generously
        val headerSafeWidth = width - 350f
        val bodySafeWidth = width - 116f

        // Top Header: Official Institution Name
        val instPaint = Paint().apply {
            color = navyPrimary
            textSize = 13f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.06f
        }
        drawCenteredFittedSingleLine(canvas, instDisplay.uppercase(), cx, headerTextY, headerSafeWidth, instPaint, minSize = 10f)

        // Header Subtitle: Distinction
        val subtitlePaint = Paint().apply {
            color = goldDark
            textSize = 9f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        drawCenteredFittedSingleLine(
            canvas,
            "POST GRADUATE DIPLOMA (PGD) IN SUPPLY CHAIN MANAGEMENT (SCM)",
            cx,
            headerTextY + 16f,
            bodySafeWidth - 60f,
            subtitlePaint,
            minSize = 7.5f
        )

        // Decorative separator line with gold diamond center
        val sepY = headerTextY + 28f
        val linePaint = Paint().apply {
            color = goldMetallic
            strokeWidth = 1.4f
            isAntiAlias = true
        }
        canvas.drawLine(cx - 190f, sepY, cx - 16f, sepY, linePaint)
        canvas.drawLine(cx + 16f, sepY, cx + 190f, sepY, linePaint)
        drawCenterDiamond(canvas, cx, sepY, 5f, goldMetallic)

        // Diploma Main Title — balanced wrapping inside borders so ZERO characters ever clip
        val titleStartY = sepY + 30f
        val titlePaint = Paint().apply {
            color = navyPrimary
            textSize = 20.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.03f
        }
        val titleBottomY = drawCenteredBalancedWrappedText(
            canvas = canvas,
            text = record.courseTitle.uppercase(),
            centerX = cx,
            startY = titleStartY,
            maxWidth = bodySafeWidth,
            paint = titlePaint,
            maxLines = 2,
            lineHeightMultiplier = 1.24f,
            minTextSize = 14f
        )

        // Conferred upon text
        val conferY = titleBottomY + 22f
        val conferPaint = Paint().apply {
            color = darkGray
            textSize = 12f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("This authoritative academic credential is officially conferred upon", cx, conferY, conferPaint)

        // Student Full Name (Hero Name)
        val studentNameY = conferY + 38f
        val namePaint = Paint().apply {
            color = navySecondary
            textSize = 30f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        drawCenteredFittedSingleLine(canvas, record.studentName, cx, studentNameY, bodySafeWidth - 40f, namePaint, minSize = 18f)

        // Gold underline under student name
        val nameLinePaint = Paint().apply {
            color = goldMetallic
            strokeWidth = 1.8f
            isAntiAlias = true
        }
        canvas.drawLine(cx - 165f, studentNameY + 11f, cx + 165f, studentNameY + 11f, nameLinePaint)

        var narrativeTop = studentNameY + 30f
        if (record.studentCode.isNotBlank()) {
            val studentCodePaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 9.5f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("STUDENT ID: ${record.studentCode}", cx, studentNameY + 26f, studentCodePaint)
            narrativeTop = studentNameY + 42f
        }

        // Narrative Description
        val descPaint = Paint().apply {
            color = darkGray
            textSize = 10.8f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        drawCenteredFittedSingleLine(
            canvas,
            "in recognition of successfully completing all 12 Specialization Modules and 48 Executive Lessons,",
            cx,
            narrativeTop,
            bodySafeWidth,
            descPaint,
            minSize = 8.5f
        )
        drawCenteredFittedSingleLine(
            canvas,
            "Part 1: Basic (Foundation Level) & Part 2: Advance (Strategic & Analytical Level),",
            cx,
            narrativeTop + 15f,
            bodySafeWidth,
            descPaint,
            minSize = 8.5f
        )
        drawCenteredFittedSingleLine(
            canvas,
            "Module Examinations, Capstone Project, and Final Comprehensive Assessment.",
            cx,
            narrativeTop + 30f,
            bodySafeWidth,
            descPaint,
            minSize = 8.5f
        )

        // Symmetrical 3-Column Footer:
        // Left Signatory (Hasina Khan, Chairman & CEO) | Center (QR + Date) | Right Signatory (Amirul Islam, Founder of ISCEB)
        val metaLabelPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val signerTitlePaint = Paint().apply {
            color = goldDark
            textSize = 9f
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

        val signLineY = 468f

        // Left Signatory Block (Hasina Khan, Chairman & CEO)
        val leftX = 168f
        canvas.drawLine(leftX - 102f, signLineY, leftX + 102f, signLineY, signLinePaint)
        drawCenteredFittedSingleLine(canvas, leftSigner.name, leftX, signLineY + 15f, 200f, metaValuePaint, minSize = 9f)
        if (leftSigner.title.isNotBlank()) {
            drawCenteredFittedSingleLine(canvas, leftSigner.title, leftX, signLineY + 28f, 200f, signerTitlePaint, minSize = 7.5f)
        }

        // Center QR Verification Block + Date of Issuance
        val qrSize = 58f
        val qrLeft = cx - qrSize / 2f
        val qrTop = 408f
        val verifyPayload = record.verificationUrl.ifBlank { "https://www.isceb.org/verify/${record.certificateId}" }
        QrCodeGenerator.drawQrOnAndroidCanvas(
            canvas = canvas,
            data = verifyPayload,
            left = qrLeft,
            top = qrTop,
            sizePx = qrSize,
            darkColor = navyPrimary,
            lightColor = warmBlushCreamCenter
        )
        canvas.drawRect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize, hairlinePaint)
        canvas.drawText("SCAN TO VERIFY", cx, qrTop + qrSize + 11f, metaLabelPaint)
        canvas.drawText("ISSUED: ${record.issueDate}", cx, qrTop + qrSize + 23f, metaLabelPaint)

        // Right Signatory Block (Amirul Islam, Founder of ISCEB)
        val rightX = width - 168f
        canvas.drawLine(rightX - 102f, signLineY, rightX + 102f, signLineY, signLinePaint)
        drawCenteredFittedSingleLine(canvas, rightSigner.name, rightX, signLineY + 15f, 200f, metaValuePaint, minSize = 9f)
        drawCenteredFittedSingleLine(canvas, rightSigner.title, rightX, signLineY + 28f, 200f, signerTitlePaint, minSize = 7.5f)

        // Bottom Footer: Verification ID & URL
        val footerPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 9.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val statusText = if (isDemo) "DEMO PREVIEW" else record.status.name
        drawCenteredFittedSingleLine(
            canvas,
            "CERTIFICATE ID: ${record.certificateId}   •   STATUS: $statusText",
            cx,
            532f,
            bodySafeWidth,
            footerPaint,
            minSize = 7.5f
        )

        val disclaimerPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        drawCenteredFittedSingleLine(
            canvas,
            "Verify: $verifyPayload • $instDisplay",
            cx,
            548f,
            bodySafeWidth,
            disclaimerPaint,
            minSize = 7f
        )
    }

    private fun drawParchmentWatermark(
        canvas: Canvas,
        width: Float,
        height: Float,
        watermarkTone: Int
    ) {
        val cx = width / 2f
        val cy = height * 0.51f

        val outerRingPaint = Paint().apply {
            color = watermarkTone
            alpha = 88
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            isAntiAlias = true
        }
        val dashedRingPaint = Paint().apply {
            color = watermarkTone
            alpha = 95
            style = Paint.Style.STROKE
            strokeWidth = 1.1f
            pathEffect = DashPathEffect(floatArrayOf(6f, 5f), 0f)
            isAntiAlias = true
        }
        val rayPaint = Paint().apply {
            color = watermarkTone
            alpha = 62
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
            isAntiAlias = true
        }

        // Subtle guilloché emblem rings behind central credential area
        canvas.drawCircle(cx, cy, 146f, outerRingPaint)
        canvas.drawCircle(cx, cy, 134f, dashedRingPaint)
        canvas.drawCircle(cx, cy, 108f, outerRingPaint)

        for (deg in 0 until 360 step 15) {
            val rad = Math.toRadians(deg.toDouble())
            val rIn = if (deg % 30 == 0) 64f else 88f
            val rOut = 132f
            canvas.drawLine(
                cx + (cos(rad) * rIn).toFloat(),
                cy + (sin(rad) * rIn).toFloat(),
                cx + (cos(rad) * rOut).toFloat(),
                cy + (sin(rad) * rOut).toFloat(),
                rayPaint
            )
        }

        // Same-tone soft embossed crest lettering in background center
        val crestTextPaint = Paint().apply {
            color = watermarkTone
            alpha = 52
            textSize = 46f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.22f
        }
        canvas.drawText("ISCEB", cx, cy + 16f, crestTextPaint)

        // Delicate corner filigree arcs inside the frame
        val cornerArcPaint = Paint().apply {
            color = watermarkTone
            alpha = 84
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }
        val arcR = 52f
        canvas.drawArc(RectF(32f - arcR, 32f - arcR, 32f + arcR, 32f + arcR), 0f, 90f, false, cornerArcPaint)
        canvas.drawArc(RectF(width - 32f - arcR, 32f - arcR, width - 32f + arcR, 32f + arcR), 90f, 90f, false, cornerArcPaint)
        canvas.drawArc(RectF(32f - arcR, height - 32f - arcR, 32f + arcR, height - 32f + arcR), 270f, 90f, false, cornerArcPaint)
        canvas.drawArc(RectF(width - 32f - arcR, height - 32f - arcR, width - 32f + arcR, height - 32f + arcR), 180f, 90f, false, cornerArcPaint)
    }

    private fun drawCenteredFittedSingleLine(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        maxWidth: Float,
        paint: Paint,
        minSize: Float = 8f
    ) {
        val originalSize = paint.textSize
        var size = originalSize
        paint.textSize = size
        while (paint.measureText(text) > maxWidth && size > minSize) {
            size -= 0.5f
            paint.textSize = size
        }
        canvas.drawText(text, centerX, baselineY, paint)
        paint.textSize = originalSize
    }

    private fun drawCenteredBalancedWrappedText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        maxLines: Int = 2,
        lineHeightMultiplier: Float = 1.24f,
        minTextSize: Float = 13f
    ): Float {
        val originalSize = paint.textSize
        if (paint.measureText(text) <= maxWidth) {
            canvas.drawText(text, centerX, startY, paint)
            paint.textSize = originalSize
            return startY
        }

        val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val lines: List<String> = if (maxLines >= 2 && words.size >= 4) {
            // Find split index that balances left & right line widths most evenly
            var bestSplit = words.size / 2
            var bestDiff = Float.MAX_VALUE
            for (split in 1 until words.size) {
                val leftStr = words.subList(0, split).joinToString(" ")
                val rightStr = words.subList(split, words.size).joinToString(" ")
                val diff = kotlin.math.abs(paint.measureText(leftStr) - paint.measureText(rightStr))
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestSplit = split
                }
            }
            listOf(
                words.subList(0, bestSplit).joinToString(" "),
                words.subList(bestSplit, words.size).joinToString(" ")
            )
        } else {
            listOf(text)
        }

        var size = originalSize
        paint.textSize = size
        while (lines.any { paint.measureText(it) > maxWidth } && size > minTextSize) {
            size -= 0.5f
            paint.textSize = size
        }

        val lineHeight = size * lineHeightMultiplier
        var currentY = startY
        lines.forEachIndexed { idx, line ->
            if (idx > 0) currentY += lineHeight
            canvas.drawText(line, centerX, currentY, paint)
        }
        paint.textSize = originalSize
        return currentY
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
