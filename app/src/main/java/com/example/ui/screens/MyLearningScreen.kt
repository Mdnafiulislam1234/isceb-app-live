package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AdminCompletionSettings
import com.example.model.CertificateEligibility
import com.example.model.CertificateRecord
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.UserProgress
import com.example.ui.components.CertificatePreviewCard
import com.example.ui.components.ClassCard
import com.example.ui.icons.Download
import com.example.ui.icons.EmojiEvents
import com.example.ui.icons.OpenInNew
import com.example.ui.icons.School
import com.example.ui.icons.Verified
import com.example.ui.theme.BgLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyLearningScreen(
    modules: List<CourseModule>,
    classes: List<CourseClass>,
    progress: UserProgress,
    eligibility: CertificateEligibility,
    issuedCertificates: List<CertificateRecord>,
    isAdmin: Boolean,
    onClassClick: (CourseClass) -> Unit,
    onToggleComplete: (CourseClass) -> Unit,
    onToggleBookmark: (CourseClass) -> Unit,
    onGenerateCertificate: () -> Unit,
    onOpenCertificatePdf: (CertificateRecord) -> Unit,
    onVerifyCertificate: (String) -> Unit,
    passedExamCount: Int = 0,
    totalRequiredExamCount: Int = 5,
    submittedAssignmentCount: Int = 0,
    adminSettings: AdminCompletionSettings = AdminCompletionSettings(),
    onPreviewDemoCertificate: () -> Unit = {},
    onDownloadCertificatePdf: (CertificateRecord) -> Unit = onOpenCertificatePdf,
    onDownloadDemoCertificatePdf: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Completed", "Bookmarked", "In Progress", "My Certificates")

    val completedList = classes.filter { it.isCompleted }
    val bookmarkedList = classes.filter { it.isBookmarked }
    val inProgressList = classes.filter { !it.isCompleted && (it.hasAnyMedia || it.activeLearningSeconds > 0) }

    val currentList = when (selectedTab) {
        0 -> completedList
        1 -> bookmarkedList
        2 -> inProgressList
        else -> emptyList()
    }

    val progressRatio = if (classes.isNotEmpty()) completedList.size.toFloat() / classes.size else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Diploma Progress Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(GoldMetallic),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Executive SCM Diploma Track",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Text(
                                    text = "Global Logistics & Operations Leadership",
                                    style = MaterialTheme.typography.bodySmall.copy(color = GoldSoft, fontSize = 11.sp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${completedList.size} of ${classes.size} Lessons Verified (${(progressRatio * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                        )
                        Text(
                            text = "${progress.totalActiveLearningSeconds / 60}m Active Study",
                            style = MaterialTheme.typography.labelSmall.copy(color = GoldMetallic, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GoldMetallic,
                        trackColor = NavySurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Verified LMS stats row (no hardcoded streak)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$passedExamCount/$totalRequiredExamCount Exams Passed",
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.School, contentDescription = null, tint = GoldSoft, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$submittedAssignmentCount Tasks • ${modules.size} Mods", color = Color.White, fontSize = 11.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${completedList.size} Complete", color = Color(0xFF81C784), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // CERTIFICATE ELIGIBILITY & GENERATION DASHBOARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (eligibility.isEligible) Color(0xFFFFFBEB) else Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (eligibility.isEligible) GoldMetallic else Color(0xFFE2E8F0)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = if (eligibility.isEligible) GoldDark else NavyPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Certification Eligibility",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (eligibility.existingCertificate != null || eligibility.isEligible) {
                                        Color(0xFFDCFCE7)
                                    } else {
                                        Color(0xFFEFF6FF)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = eligibility.status.displayLabel.uppercase(),
                                color = if (eligibility.existingCertificate != null || eligibility.isEligible) SuccessGreen else NavyPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.testTag("badge_learning_cert_status")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Required Lessons Completed: ${eligibility.completedRequiredCount} of ${eligibility.totalRequiredCount}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF334155))
                    )
                    Text(
                        text = "• Mandatory Assessments: ${if (eligibility.allQuizzesPassed && eligibility.mandatoryExamsPassed) "All Passed" else "Pending Completion"}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF334155))
                    )
                    Text(
                        text = "• Final Project & Capstone: ${if (eligibility.finalProjectSubmitted) "Satisfied" else "Pending"}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF334155))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!adminSettings.certificatesEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Official certificate issuance is currently disabled by Administrator settings.",
                                style = MaterialTheme.typography.bodySmall.copy(color = DangerRed, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (eligibility.existingCertificate != null) {
                        val cert = eligibility.existingCertificate
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NavyPrimary)
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("OFFICIAL DIPLOMA ISSUED", color = GoldSoft, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    Text(cert.issueDate, color = Color.White, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("ID: ${cert.certificateId}", color = GoldMetallic, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onDownloadCertificatePdf(cert) },
                                        modifier = Modifier.weight(1f).testTag("btn_download_existing_cert"),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyPrimary)
                                    ) {
                                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Download PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }
                                    OutlinedButton(
                                        onClick = { onOpenCertificatePdf(cert) },
                                        modifier = Modifier.weight(1f).testTag("btn_view_existing_cert"),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldSoft)
                                    ) {
                                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("View PDF", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                    }
                                    OutlinedButton(
                                        onClick = { onVerifyCertificate(cert.certificateId) },
                                        modifier = Modifier.weight(0.8f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text("Verify", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    } else if (eligibility.isEligible && adminSettings.certificatesEnabled) {
                        Button(
                            onClick = onGenerateCertificate,
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_generate_certificate"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Official Landscape PDF Certificate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                val reqText = if (eligibility.remainingRequirements.isNotEmpty()) {
                                    eligibility.remainingRequirements.joinToString("\n") { "• $it" }
                                } else {
                                    "Complete all ${eligibility.totalRequiredCount} lessons with minimum study times and pass assessments to unlock your verified Executive Diploma."
                                }
                                Text(
                                    text = reqText,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569), fontSize = 11.sp),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    if (isAdmin || adminSettings.demoCertificateEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().testTag("card_demo_certificate"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPreviewDemoCertificate,
                                modifier = Modifier.weight(1.1f).heightIn(min = 48.dp).testTag("btn_preview_demo_certificate"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = GoldDark, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Demo Preview", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                            if (isAdmin) {
                                Button(
                                    onClick = onDownloadDemoCertificatePdf,
                                    modifier = Modifier.weight(0.9f).heightIn(min = 48.dp).testTag("btn_download_demo_certificate"),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic)
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Demo PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 8.dp,
                containerColor = Color.White,
                contentColor = NavyPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldMetallic
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    val count = when (index) {
                        0 -> completedList.size
                        1 -> bookmarkedList.size
                        2 -> inProgressList.size
                        else -> issuedCertificates.size
                    }
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("tab_learning_$index"),
                        text = {
                            Text(
                                text = "$title ($count)",
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }
        }

        // Items List or Certificate List
        if (selectedTab == 3) {
            if (isAdmin || adminSettings.demoCertificateEnabled) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldMetallic)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Demo Executive PGD Certificate",
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Preview the official certificate layout from Institute of Supply Chain Excellence Bangladesh (ISCEB) with Left & Right Signatories, QR verification, and Landscape A4 PDF export.",
                                color = Color(0xFF475569),
                                fontSize = 11.5.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = onPreviewDemoCertificate,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = GoldDark, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Preview Demo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onDownloadDemoCertificatePdf,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic)
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            if (issuedCertificates.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.img_isceb_logo),
                                contentDescription = "ISCEB - Connect. learn. Achieve.",
                                modifier = Modifier.width(120.dp).height(64.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Certificates Issued Yet", fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Once you satisfy all curriculum completion rules, your issued ISCEB diplomas will appear here.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(issuedCertificates) { cert ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CertificatePreviewCard(record = cert)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onDownloadCertificatePdf(cert) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onOpenCertificatePdf(cert) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open PDF", fontSize = 11.5.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        } else {
            if (currentList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_isceb_logo),
                                contentDescription = "ISCEB - Connect. learn. Achieve.",
                                modifier = Modifier.width(110.dp).height(58.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = when (selectedTab) {
                                    0 -> "No completed lessons yet"
                                    1 -> "No saved bookmarks yet"
                                    else -> "No in-progress lessons"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Browse modules on the Home or Modules tab to begin studying.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7F96))
                            )
                        }
                    }
                }
            } else {
                items(currentList) { cls ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ClassCard(
                            courseClass = cls,
                            isAdmin = isAdmin,
                            isFirst = false,
                            isLast = false,
                            onClick = { onClassClick(cls) },
                            onToggleComplete = { onToggleComplete(cls) },
                            onToggleBookmark = { onToggleBookmark(cls) },
                            onEditClick = {},
                            onDeleteClick = {},
                            onMoveUp = {},
                            onMoveDown = {},
                            onTogglePublish = {}
                        )
                    }
                }
            }
        }
    }
}
