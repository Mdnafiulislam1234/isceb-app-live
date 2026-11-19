package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.example.ui.icons.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import com.example.ui.icons.EmojiEvents
import androidx.compose.material.icons.filled.Info
import com.example.ui.icons.LocalFireDepartment
import com.example.ui.icons.OpenInNew
import com.example.ui.icons.School
import com.example.ui.icons.Shield
import com.example.ui.icons.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.model.CertificateEligibility
import com.example.model.CertificateRecord
import com.example.model.CertificateStatus
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.UserProgress
import com.example.ui.components.CertificatePreviewCard
import com.example.ui.components.ClassCard
import com.example.ui.theme.BgLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SuccessGreen

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

                    // Study streak & stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("5 Days Streak", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.School, contentDescription = null, tint = GoldSoft, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("12 Modules Total", color = Color.White, fontSize = 11.sp)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                                text = "Certification Eligibility Dashboard",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (eligibility.isEligible) Color(0xFFDCFCE7) else Color(0xFFEFF6FF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (eligibility.isEligible) "ELIGIBLE FOR DIPLOMA" else "IN PROGRESS",
                                color = if (eligibility.isEligible) SuccessGreen else NavyPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Required Lessons Completed: ${eligibility.completedRequiredCount} of ${eligibility.totalRequiredCount}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF334155))
                    )
                    Text(
                        text = "• Mandatory Quizzes Status: ${if (eligibility.allQuizzesPassed) "All Passed" else "Assessments Pending"}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF334155))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (eligibility.existingCertificate != null) {
                        // Existing Certificate Banner
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
                                        onClick = { onOpenCertificatePdf(cert) },
                                        modifier = Modifier.weight(1f).testTag("btn_view_existing_cert"),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyPrimary)
                                    ) {
                                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("View PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onVerifyCertificate(cert.certificateId) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text("Verify ID", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else if (eligibility.isEligible) {
                        // Eligible: Generate Button
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
                        // Ineligible info
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Complete all 48 lessons with minimum study times and pass assessments to unlock your verified Executive Diploma.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569), fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = NavyPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldMetallic
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp))
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
                        text = {
                            Text(
                                text = "$title ($count)",
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        }
                    )
                }
            }
        }

        // Items List or Certificate List
        if (selectedTab == 3) {
            // My Certificates Tab
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
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        CertificatePreviewCard(record = cert)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        } else {
            // Classes list
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
                            isAdmin = false,
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
