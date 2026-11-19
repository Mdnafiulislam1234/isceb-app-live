package com.example.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// ======================= CERTIFICATE PREVIEW CARD =======================
@Composable
fun CertificatePreviewCard(
    record: CertificateRecord,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFCFD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(6.dp, NavyPrimary, RoundedCornerShape(12.dp))
                .padding(6.dp)
                .border(2.dp, GoldMetallic, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Official ISCEB Logo
                Image(
                    painter = painterResource(id = R.drawable.img_isceb_logo),
                    contentDescription = "ISCEB - Connect. learn. Achieve.",
                    modifier = Modifier
                        .width(92.dp)
                        .height(68.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Institution Name
                Text(
                    text = record.institutionName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                )

                Text(
                    text = "POST GRADUATE DIPLOMA (PGD) IN SUPPLY CHAIN MANAGEMENT (SCM)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GoldDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 0.8.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = record.courseTitle.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyPrimary,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
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

                Spacer(modifier = Modifier.height(8.dp))

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

                // Seal and signatures
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Seal
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary)
                            .border(1.5.dp, GoldMetallic, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(16.dp))
                            Text("VERIFIED", color = GoldSoft, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Date
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = record.issueDate, fontWeight = FontWeight.Bold, color = NavyPrimary, fontSize = 9.sp)
                        Box(modifier = Modifier.width(70.dp).height(1.dp).background(Color.LightGray))
                        Text(text = "DATE OF ISSUANCE", color = Color.Gray, fontSize = 7.sp)
                    }

                    // Signer
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = record.authorizedSigner.take(24), fontWeight = FontWeight.Bold, color = NavyPrimary, fontSize = 9.sp)
                        Box(modifier = Modifier.width(90.dp).height(1.dp).background(Color.LightGray))
                        Text(text = "AUTHORIZED SIGNER", color = Color.Gray, fontSize = 7.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ID Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ID: ${record.certificateId}   •   STATUS: ${record.status.name}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (record.status == CertificateStatus.VALID) SuccessGreen else DangerRed
                        )
                        Text(
                            text = "Registry: ${record.officialWebsite}",
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
    onVerifyClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Executive Diploma Certificate", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = NavyPrimary))
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
                    text = "This certificate was generated on demand in high-resolution vector PDF format. It carries a unique non-guessable hash ID verified against the institutional registry.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B), fontSize = 11.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenPdf,
                        modifier = Modifier.weight(1f).testTag("btn_open_pdf_cert"),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onVerifyClick(record.certificateId) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify ID", fontSize = 12.sp)
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

                            // Detail Rows
                            VerificationDetailRow("Student Name:", cert.studentName)
                            VerificationDetailRow("Course Title:", cert.courseTitle)
                            VerificationDetailRow("Certificate ID:", cert.certificateId)
                            VerificationDetailRow("Issue Date:", cert.issueDate)
                            VerificationDetailRow("Completed Classes:", "${cert.completedClassesCount} of ${cert.totalRequiredClasses} Classes (100% Curricular Mastery)")
                            VerificationDetailRow("Institution:", cert.institutionName)
                            VerificationDetailRow("Authorized Signer:", cert.authorizedSigner)

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
