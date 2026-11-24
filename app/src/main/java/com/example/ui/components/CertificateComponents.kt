package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.ui.icons.Block
import androidx.compose.material.icons.filled.CheckCircle
import com.example.ui.icons.Download
import com.example.ui.icons.ErrorOutline
import com.example.ui.icons.OpenInNew
import com.example.ui.icons.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import com.example.ui.icons.Shield
import com.example.ui.icons.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import com.example.R
import com.example.model.CertificateRecord
import com.example.model.CertificateStatus
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SuccessGreen
import com.example.util.CertificatePdfGenerator
import com.example.util.QrCodeGenerator

@Composable
fun QrCodeImage(
    data: String,
    modifier: Modifier = Modifier,
    darkColor: Color = NavyPrimary,
    lightColor: Color = Color.White
) {
    val matrix = remember(data) { QrCodeGenerator.generateQrMatrix(data) }
    Box(
        modifier = modifier
            .background(lightColor, RoundedCornerShape(6.dp))
            .border(1.dp, GoldMetallic.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val n = matrix.size
            if (n <= 0) return@Canvas
            val cell = minOf(size.width, size.height) / n.toFloat()
            drawRect(color = lightColor)
            for (r in 0 until n) {
                for (c in 0 until n) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell + 0.5f, cell + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

// ======================= CERTIFICATE PREVIEW CARD =======================
@Composable
fun CertificatePreviewCard(
    record: CertificateRecord,
    modifier: Modifier = Modifier
) {
    val institutionDisplay = remember(record.institutionName) {
        CertificatePdfGenerator.formatInstitutionDisplayName(record.institutionName)
    }
    val leftSigner = remember(record) {
        CertificatePdfGenerator.resolveLeftSignatory()
    }
    val rightSigner = remember(record.authorizedSigner) {
        CertificatePdfGenerator.resolveRightSignatory(record)
    }
    val isDemoCert = remember(record.certificateId) {
        CertificatePdfGenerator.isDemoCertificate(record)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("card_certificate_preview"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBF0E9), contentColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        val logoBitmap = ImageBitmap.imageResource(id = R.drawable.img_isceb_logo)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFDF7F3),
                            Color(0xFFFBF0E9),
                            Color(0xFFF3DDD1)
                        )
                    )
                )
                .border(6.dp, NavyPrimary, RoundedCornerShape(12.dp))
                .padding(6.dp)
                .border(2.dp, GoldMetallic, RoundedCornerShape(8.dp))
        ) {
            // Subtle same-tone warm reddish-cream ornamental watermark behind text
            Canvas(modifier = Modifier.matchParentSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.48f
                val maxR = minOf(size.width, size.height) * 0.34f
                val wmColor = Color(0xFFDFC1B2).copy(alpha = 0.42f)
                drawCircle(color = wmColor, radius = maxR, center = Offset(cx, cy), style = Stroke(width = 1.6f))
                drawCircle(color = wmColor, radius = maxR * 0.78f, center = Offset(cx, cy), style = Stroke(width = 1.2f))
                for (deg in 0 until 360 step 20) {
                    val rad = Math.toRadians(deg.toDouble())
                    drawLine(
                        color = wmColor.copy(alpha = 0.28f),
                        start = Offset(cx + (cos(rad) * (maxR * 0.48f)).toFloat(), cy + (sin(rad) * (maxR * 0.48f)).toFloat()),
                        end = Offset(cx + (cos(rad) * (maxR * 0.96f)).toFloat(), cy + (sin(rad) * (maxR * 0.96f)).toFloat()),
                        strokeWidth = 1f
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Official ISCEB Logo blended cleanly onto warm parchment background
                Canvas(
                    modifier = Modifier
                        .width(92.dp)
                        .height(64.dp)
                        .semantics { contentDescription = "ISCEB - Connect. learn. Achieve." }
                ) {
                    val imgAspect = logoBitmap.width.toFloat() / logoBitmap.height.toFloat().coerceAtLeast(1f)
                    val boxAspect = size.width / size.height.coerceAtLeast(1f)
                    val drawW: Float
                    val drawH: Float
                    if (imgAspect > boxAspect) {
                        drawW = size.width
                        drawH = size.width / imgAspect
                    } else {
                        drawH = size.height
                        drawW = size.height * imgAspect
                    }
                    val left = ((size.width - drawW) / 2f).roundToInt()
                    val top = ((size.height - drawH) / 2f).roundToInt()
                    drawImage(
                        image = logoBitmap,
                        dstOffset = IntOffset(left, top),
                        dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
                        blendMode = BlendMode.Multiply
                    )
                }

                if (isDemoCert) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(GoldSoft)
                            .border(1.dp, GoldMetallic, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DEMO CERTIFICATE PREVIEW",
                            color = NavyPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Institution Official Full Name
                Text(
                    text = institutionDisplay.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .testTag("text_cert_institution_name")
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "POST GRADUATE DIPLOMA (PGD) IN SUPPLY CHAIN MANAGEMENT (SCM)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GoldDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 0.4.sp,
                        lineHeight = 11.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = record.courseTitle.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyPrimary,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "This is officially conferred upon",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF475569),
                        fontSize = 10.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Student Name
                Text(
                    text = record.studentName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavySecondary,
                        fontSize = 20.sp
                    )
                )

                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(1.5.dp)
                        .background(GoldMetallic)
                )

                if (record.studentCode.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Student ID: ${record.studentCode}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "for verified mastery of all 12 Specialization Modules and 48 Executive Lessons in Global Supply Chain Operations, Logistics Strategy, Trade Finance, and Risk Architecture.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF334155),
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Seal, Date, and QR Verification Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Seal
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary)
                            .border(1.5.dp, GoldMetallic, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(14.dp))
                            Text(if (isDemoCert) "DEMO" else "VERIFIED", color = GoldSoft, fontSize = 5.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Date
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = record.issueDate, fontWeight = FontWeight.Bold, color = NavyPrimary, fontSize = 9.sp)
                        Box(modifier = Modifier.width(72.dp).height(1.dp).background(Color.LightGray))
                        Text(text = "ISSUED DATE", color = Color.Gray, fontSize = 6.5.sp)
                    }

                    // QR Verification badge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        QrCodeImage(
                            data = record.verificationUrl.ifBlank { "https://www.isceb.org/verify/${record.certificateId}" },
                            modifier = Modifier.size(48.dp)
                        )
                        Text(text = "SCAN QR", color = Color.Gray, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Symmetrical Two-Signatory Row (Left: Hasina Khan | Right: Amirul Islam, Founder of ISCEB)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Signatory (clean space above hairline for authorized signature)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cert_left_signatory"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(1.dp)
                                .background(Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = leftSigner.name,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            fontSize = 10.sp,
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (leftSigner.title.isNotBlank()) {
                            Text(
                                text = leftSigner.title,
                                color = GoldDark,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 7.5.sp,
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Right Signatory (Amirul Islam, Founder of ISCEB)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cert_right_signatory"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(1.dp)
                                .background(Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rightSigner.name,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            fontSize = 10.sp,
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = rightSigner.title,
                            color = GoldDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 7.5.sp,
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ID Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF3E2D8))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val footerStatus = if (isDemoCert) "DEMO PREVIEW" else record.status.name
                        Text(
                            text = "ID: ${record.certificateId}   •   STATUS: $footerStatus",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDemoCert) GoldDark else if (record.status == CertificateStatus.VALID) SuccessGreen else DangerRed
                        )
                        Text(
                            text = "Verify: ${record.verificationUrl.ifBlank { record.officialWebsite }}",
                            fontSize = 7.5.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// ======================= CERTIFICATE PREVIEW DIALOG =======================
@Composable
fun CertificatePreviewDialog(
    record: CertificateRecord,
    onDismiss: () -> Unit,
    onOpenPdf: () -> Unit,
    onVerifyClick: (String) -> Unit,
    onDownloadPdf: () -> Unit = onOpenPdf
) {
    val isDemoCert = remember(record.certificateId) {
        CertificatePdfGenerator.isDemoCertificate(record)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDemoCert) "Demo Executive Diploma" else "Executive Diploma Certificate",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CertificatePreviewCard(record = record)

                Text(
                    text = if (isDemoCert) {
                        "Preview of the official PGD Diploma template from ${CertificatePdfGenerator.OFFICIAL_INSTITUTION_FULL_NAME} (ISCEB). Save or open as Landscape A4 PDF."
                    } else {
                        "This certificate was generated on demand in high-resolution vector PDF format. It carries a unique non-guessable hash ID verified against the institutional registry."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B), fontSize = 11.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDownloadPdf,
                        modifier = Modifier.weight(1f).testTag("btn_download_pdf_cert"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = onOpenPdf,
                        modifier = Modifier.weight(1f).testTag("btn_open_pdf_cert"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open PDF", fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                    }
                }

                if (!isDemoCert) {
                    OutlinedButton(
                        onClick = { onVerifyClick(record.certificateId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify Certificate ID", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

// ======================= CERTIFICATE VERIFICATION SCREEN =======================
@Composable
fun CertificateVerificationScreen(
    searchQuery: String,
    verificationResult: CertificateRecord?,
    isSearched: Boolean,
    isAdmin: Boolean,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onBack: () -> Unit,
    onRevoke: (CertificateRecord) -> Unit,
    onReinstate: (CertificateRecord) -> Unit,
    onOpenPdf: (CertificateRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var queryText by remember { mutableStateOf(searchQuery) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = NavyPrimary,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_verification")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .width(46.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_isceb_logo),
                            contentDescription = "ISCEB - Connect. learn. Achieve.",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ISCEB Registry & Verification",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Verify the authenticity of any ISCEB executive diploma using its authoritative registry ID.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD6E2F0), fontSize = 12.sp)
                )
            }
        }

        // Search Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Enter Certificate Identifier",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = queryText,
                    onValueChange = {
                        queryText = it
                        onSearchChange(it)
                    },
                    placeholder = { Text("e.g. ISCEB-SCM-...", fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = GoldDark)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_verification_cert_id"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onSearchSubmit(queryText) },
                    modifier = Modifier.fillMaxWidth().testTag("btn_verify_cert"),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify Credential Authenticity", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Results Section
        if (isSearched) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (verificationResult != null) {
                    val cert = verificationResult
                    val isValid = cert.status == CertificateStatus.VALID

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Status Banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isValid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Block,
                                        contentDescription = null,
                                        tint = if (isValid) SuccessGreen else DangerRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isValid) "CREDENTIAL STATUS: VALID" else "CREDENTIAL STATUS: REVOKED",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isValid) SuccessGreen else DangerRed,
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    text = "OFFICIAL RECORD",
                                    color = if (isValid) SuccessGreen else DangerRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!isValid && cert.revocationReason != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Revocation Reason: ${cert.revocationReason}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = DangerRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Detail Rows + QR Verification Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    VerificationDetailRow("Student Name:", cert.studentName)
                                    if (cert.studentCode.isNotBlank()) {
                                        VerificationDetailRow("Student ID:", cert.studentCode)
                                    }
                                    VerificationDetailRow("Course Title:", cert.courseTitle)
                                    VerificationDetailRow("Certificate ID:", cert.certificateId)
                                    VerificationDetailRow("Issue Date:", cert.issueDate)
                                    VerificationDetailRow("Completed Classes:", "${cert.completedClassesCount} of ${cert.totalRequiredClasses} Classes (100% Curricular Mastery)")
                                    VerificationDetailRow("Institution:", CertificatePdfGenerator.formatInstitutionDisplayName(cert.institutionName))
                                    val rightSig = CertificatePdfGenerator.resolveRightSignatory(cert)
                                    val leftSig = CertificatePdfGenerator.resolveLeftSignatory()
                                    VerificationDetailRow("Signatories:", "${leftSig.name}, ${leftSig.title} (Left) • ${rightSig.name}, ${rightSig.title} (Right)")
                                    VerificationDetailRow("Verification URL:", cert.verificationUrl.ifBlank { "https://www.isceb.org/verify/${cert.certificateId}" })
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    QrCodeImage(
                                        data = cert.verificationUrl.ifBlank { "https://www.isceb.org/verify/${cert.certificateId}" },
                                        modifier = Modifier.size(74.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("QR Verify", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenPdf(cert) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open PDF", fontSize = 11.sp)
                                }

                                if (isAdmin) {
                                    if (isValid) {
                                        Button(
                                            onClick = { onRevoke(cert) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                                        ) {
                                            Text("Revoke", fontSize = 11.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = { onReinstate(cert) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White)
                                        ) {
                                            Text("Reinstate", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Not Found Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Certificate Not Found",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No authoritative record exists matching identifier '$queryText'. Verify that the ID was copied accurately without spaces.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7F96),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun VerificationDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            ),
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

// ======================= REVOCATION REASON DIALOG =======================
@Composable
fun RevokeCertificateDialog(
    certificate: CertificateRecord,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("Violation of academic integrity standard") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = DangerRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Revoke Certificate", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DangerRed))
            }
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to revoke diploma '${certificate.certificateId}' issued to ${certificate.studentName}?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = NavyPrimary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Revocation") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DangerRed)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
            ) {
                Text("Revoke Certificate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
