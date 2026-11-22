package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.example.ui.icons.Article
import com.example.ui.icons.AttachFile
import com.example.ui.icons.Audiotrack
import com.example.ui.icons.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import com.example.ui.icons.CloudUpload
import androidx.compose.material.icons.filled.Delete
import com.example.ui.icons.Description
import com.example.ui.icons.Download
import com.example.ui.icons.DownloadDone
import com.example.ui.icons.Image
import androidx.compose.material.icons.filled.Info
import com.example.ui.icons.OpenInNew
import com.example.ui.icons.PlayCircle
import com.example.ui.icons.Quiz
import com.example.ui.icons.Schedule
import com.example.ui.icons.Shield
import com.example.ui.icons.Timer
import com.example.ui.icons.Videocam
import com.example.ui.icons.BookmarkBorder
import com.example.ui.icons.CheckCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdminCompletionSettings
import com.example.model.ClassStatus
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.LessonMedia
import com.example.model.MediaType
import com.example.model.StudentClassProgress
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.PdfViewerCard
import com.example.ui.components.VideoPlayerCard
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
fun ClassDetailScreen(
    courseClass: CourseClass,
    module: CourseModule?,
    allModuleClasses: List<CourseClass>,
    studentProgress: StudentClassProgress?,
    adminSettings: AdminCompletionSettings,
    isAdmin: Boolean,
    isPlayingAudio: Boolean,
    audioProgressMs: Long,
    audioDurationMs: Long,
    audioSpeed: Float,
    isPlayingVideo: Boolean,
    videoPositionMs: Long,
    videoDurationMs: Long,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onConfirmPdfRead: () -> Unit,
    onTakeQuiz: () -> Unit,
    onAdminToggleCompletion: () -> Unit,
    onPlayPauseAudio: () -> Unit,
    onSeekAudio: (Long) -> Unit,
    onSkipAudioSeconds: (Int) -> Unit,
    onAudioSpeedChange: (Float) -> Unit,
    onPlayPauseVideo: () -> Unit,
    onSeekVideo: (Long) -> Unit,
    onOpenUploadDialog: (MediaType) -> Unit,
    onDeleteMedia: (MediaType) -> Unit,
    onDeleteAttachment: (String) -> Unit = {},
    onOpenAttachment: (LessonMedia) -> Unit = {},
    onOpenExternalAttachment: (LessonMedia) -> Unit = onOpenAttachment,
    onDownloadAttachment: (LessonMedia) -> Unit = {},
    onResolveMediaFile: suspend (LessonMedia) -> java.io.File? = { null },
    onLoadDemoMedia: () -> Unit,
    onNavigateClass: (CourseClass) -> Unit,
    canManageClass: Boolean = isAdmin,
    canDownloadMedia: Boolean = courseClass.allowMediaDownload || isAdmin || canManageClass,
    onPdfPageStudyTick: (currentPage: Int, totalPages: Int, activeDeltaSec: Long, idleDeltaSec: Long) -> Unit = { _, _, _, _ -> },
    onVideoPlaybackProgress: (positionMs: Long, durationMs: Long, isPlaying: Boolean) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val sortedClasses = allModuleClasses.sortedBy { it.orderIndex }
    val currentIndex = sortedClasses.indexOfFirst { it.id == courseClass.id }
    val prevClass = if (currentIndex > 0) sortedClasses[currentIndex - 1] else null
    val nextClass = if (currentIndex >= 0 && currentIndex < sortedClasses.size - 1) sortedClasses[currentIndex + 1] else null

    val isCompleted = studentProgress?.status == ClassStatus.COMPLETED || courseClass.isCompleted
    val activeSec = studentProgress?.activeLearningSeconds ?: 0L
    val reqTime = courseClass.minStudySecondsOverride ?: adminSettings.minLearningTimeSeconds
    val timeProgress = (activeSec.toFloat() / reqTime.toFloat()).coerceIn(0f, 1f)
    val timeMet = activeSec >= reqTime

    val playbackPercent = studentProgress?.maxPlaybackPercent ?: 0f
    val playbackMet = playbackPercent >= adminSettings.minPlaybackPercent

    val pdfConfirmed = studentProgress?.isPdfConfirmed == true
    val pdfSec = studentProgress?.pdfReadingSeconds ?: 0L
    val visitedPagesCount = studentProgress?.visitedPdfPages?.size ?: 0
    val totalPdfPagesCount = studentProgress?.totalPdfPages ?: 0
    val pdfTimeMet = pdfSec >= adminSettings.minPdfReadingSeconds
    val pdfPagesMet = !adminSettings.requirePdfAllPagesVisited || totalPdfPagesCount == 0 || visitedPagesCount >= totalPdfPagesCount
    val canConfirmPdf = canManageClass || (pdfTimeMet && pdfPagesMet)
    val quizPassed = studentProgress?.quizPassed == true || courseClass.quizPassed

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("btn_back_from_class")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onToggleBookmark) {
                                Icon(
                                    imageVector = if (courseClass.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (courseClass.isBookmarked) GoldMetallic else Color.White
                                )
                            }

                            if (isCompleted) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SuccessGreen)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("VERIFIED COMPLETE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (module != null) {
                        val classBadge = courseClass.lectureCode.ifBlank {
                            String.format("%02d", courseClass.orderIndex)
                        }
                        Text(
                            text = "MODULE ${String.format("%02d", module.number)} • CLASS $classBadge • ${module.title.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GoldSoft,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = courseClass.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = GoldSoft, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${courseClass.durationMinutes} Minutes Comprehensive Study",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFD6E2F0), fontSize = 12.sp)
                        )
                    }
                }
            }
        }

        // AUTHORITATIVE COMPLETION RULES STATUS CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCompleted) Color(0xFFF0FDF4) else Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCompleted) SuccessGreen else GoldMetallic.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isCompleted) SuccessGreen else GoldDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCompleted) "Authoritative Completion Verified" else "Class Completion Criteria",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted) SuccessGreen else NavyPrimary
                                )
                            )
                        }

                        if (isCompleted && studentProgress?.completionTimestamp != null) {
                            Text(
                                text = "Recorded in DB",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Active Learning Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• Active Study Time: ${activeSec}s / ${reqTime}s required",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (timeMet) FontWeight.Bold else FontWeight.Normal,
                                color = if (timeMet) SuccessGreen else NavyPrimary
                            )
                        )
                        if (timeMet) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { timeProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (timeMet) SuccessGreen else GoldMetallic,
                        trackColor = Color(0xFFE2E8F0)
                    )
                    Text("Paused or backgrounded time is not counted.", fontSize = 9.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Playback / PDF criteria
                    if (courseClass.videoMedia != null || courseClass.audioMedia != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• Playback Reached: ${playbackPercent.toInt()}% / ${adminSettings.minPlaybackPercent.toInt()}% required",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (playbackMet) FontWeight.Bold else FontWeight.Normal,
                                    color = if (playbackMet) SuccessGreen else NavyPrimary
                                )
                            )
                            if (playbackMet) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (courseClass.pdfMedia != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (pdfConfirmed) {
                                        "• PDF Lecture Document: Confirmed Read (${pdfSec}s)"
                                    } else {
                                        "• PDF Reading: ${pdfSec}s / ${adminSettings.minPdfReadingSeconds}s" +
                                            if (totalPdfPagesCount > 0) " • Pages $visitedPagesCount/$totalPdfPagesCount" else ""
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (pdfConfirmed) FontWeight.Bold else FontWeight.Normal,
                                        color = if (pdfConfirmed) SuccessGreen else NavyPrimary
                                    )
                                )
                                if (!pdfConfirmed && !canConfirmPdf) {
                                    val remSec = (adminSettings.minPdfReadingSeconds - pdfSec).coerceAtLeast(0L)
                                    Text(
                                        text = if (remSec > 0L) "Read PDF actively for ${remSec}s more to unlock confirmation." else "Browse all PDF pages to unlock confirmation.",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            if (!pdfConfirmed) {
                                OutlinedButton(
                                    onClick = onConfirmPdfRead,
                                    enabled = canConfirmPdf,
                                    modifier = Modifier.height(32.dp).testTag("btn_confirm_pdf_read"),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Confirm Read", fontSize = 10.sp)
                                }
                            } else {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // 3. Quiz criteria
                    if (adminSettings.isQuizRequired) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (quizPassed) "• Comprehension Quiz: PASSED (Score: ${studentProgress?.quizScore ?: 100}%)" else "• Comprehension Quiz: Mandatory (Passing: ${adminSettings.quizPassingScore}%)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (quizPassed) FontWeight.Bold else FontWeight.Normal,
                                    color = if (quizPassed) SuccessGreen else NavyPrimary
                                )
                            )

                            Button(
                                onClick = onTakeQuiz,
                                modifier = Modifier.height(30.dp).testTag("btn_take_quiz"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (quizPassed) Color(0xFFDCFCE7) else NavyPrimary,
                                    contentColor = if (quizPassed) SuccessGreen else GoldMetallic
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(if (quizPassed) "Retake Quiz" else "Take Quiz", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Faculty / Admin sign-off override row
                    if (canManageClass) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isAdmin) "Admin Sign-off Override:" else "Faculty Completion Override:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            TextButton(onClick = onAdminToggleCompletion) {
                                Text(if (isCompleted) "Mark Incomplete" else "Mark Complete Directly", fontSize = 10.sp, color = if (isCompleted) DangerRed else SuccessGreen)
                            }
                        }
                    }
                }
            }
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Executive Summary", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = courseClass.summary, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF334155), lineHeight = 20.sp))

                    if (courseClass.learningObjectives.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "Key Learning Objectives", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = NavyPrimary))
                        Spacer(modifier = Modifier.height(6.dp))
                        courseClass.learningObjectives.forEach { obj ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                Text("• ", color = GoldDark, fontWeight = FontWeight.Bold)
                                Text(text = obj, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569), fontSize = 12.sp, lineHeight = 16.sp))
                            }
                        }
                    }
                }
            }
        }

        // Attached Study Files & Interactive Media Section
        item {
            val allFiles = courseClass.allAttachments
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Study Materials & Attachments (${allFiles.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            fontSize = 16.sp
                        )
                    )
                    if (canManageClass) {
                        Button(
                            onClick = { onOpenUploadDialog(MediaType.PDF) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldMetallic,
                                contentColor = NavyDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_header_upload_files")
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload Files", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Unified Downloadable & Previewable Attachment List (PDF, Image, Document, Audio, Video)
                allFiles.forEachIndexed { idx, item ->
                    val badgeColor = when (item.type) {
                        MediaType.PDF -> Color(0xFFD32F2F)
                        MediaType.IMAGE -> Color(0xFF00796B)
                        MediaType.DOCUMENT -> Color(0xFF1565C0)
                        MediaType.AUDIO -> Color(0xFF0277BD)
                        MediaType.VIDEO -> Color(0xFF7B1FA2)
                    }
                    val badgeIcon = when (item.type) {
                        MediaType.PDF -> Icons.Default.Description
                        MediaType.IMAGE -> Icons.Default.Image
                        MediaType.DOCUMENT -> Icons.Default.Article
                        MediaType.AUDIO -> Icons.Default.Audiotrack
                        MediaType.VIDEO -> Icons.Default.PlayCircle
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attachment_card_$idx"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(badgeColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = badgeIcon,
                                            contentDescription = item.type.displayLabel,
                                            tint = badgeColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.displayTitle,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyPrimary,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${item.type.displayLabel} • ${item.fileSizeFormatted} • ${item.fileName}",
                                            color = Color(0xFF64748B),
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }

                                if (canManageClass) {
                                    IconButton(
                                        onClick = { onDeleteAttachment(item.id) },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("btn_delete_attachment_$idx")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete attachment",
                                            tint = DangerRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (item.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.description,
                                    color = Color(0xFF475569),
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenAttachment(item) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_open_attachment_$idx"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    val openLabel = when (item.type) {
                                        MediaType.AUDIO -> "Play Audio"
                                        MediaType.VIDEO -> "Play Video"
                                        MediaType.PDF -> "Read PDF"
                                        else -> "Preview / Open"
                                    }
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(openLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                }

                                if (canDownloadMedia) {
                                    Button(
                                        onClick = { onDownloadAttachment(item) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_download_attachment_$idx"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NavyPrimary,
                                            contentColor = GoldMetallic
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Inline Interactive PDF / Audio / Video Cards
                if (courseClass.pdfMedia != null) {
                    PdfViewerCard(
                        pdfMedia = courseClass.pdfMedia,
                        isAdmin = canManageClass,
                        onReplace = { onOpenUploadDialog(MediaType.PDF) },
                        onDelete = { onDeleteMedia(MediaType.PDF) },
                        onResolvePdfFile = onResolveMediaFile,
                        onOpenFullscreen = onOpenAttachment,
                        onOpenExternal = onOpenExternalAttachment,
                        onDownload = onDownloadAttachment,
                        initialPage = (studentProgress?.lastPdfPage ?: 1).coerceAtLeast(1),
                        canDownload = canDownloadMedia,
                        onPageStudyTick = onPdfPageStudyTick
                    )
                }

                if (courseClass.audioMedia != null) {
                    AudioPlayerCard(
                        audioMedia = courseClass.audioMedia,
                        isPlaying = isPlayingAudio,
                        progressMs = audioProgressMs,
                        durationMs = audioDurationMs,
                        playbackSpeed = audioSpeed,
                        isAdmin = canManageClass,
                        onPlayPause = onPlayPauseAudio,
                        onSeek = onSeekAudio,
                        onSkipSeconds = onSkipAudioSeconds,
                        onSpeedChange = onAudioSpeedChange,
                        onReplace = { onOpenUploadDialog(MediaType.AUDIO) },
                        onDelete = { onDeleteMedia(MediaType.AUDIO) }
                    )
                }

                if (courseClass.videoMedia != null) {
                    VideoPlayerCard(
                        videoMedia = courseClass.videoMedia,
                        isPlaying = isPlayingVideo,
                        positionMs = videoPositionMs,
                        durationMs = videoDurationMs,
                        isAdmin = canManageClass,
                        onPlayPause = onPlayPauseVideo,
                        onSeek = onSeekVideo,
                        onReplace = { onOpenUploadDialog(MediaType.VIDEO) },
                        onDelete = { onDeleteMedia(MediaType.VIDEO) },
                        onResolveVideoFile = onResolveMediaFile,
                        onOpenFullscreen = onOpenAttachment,
                        onOpenExternal = onOpenExternalAttachment,
                        onDownload = onDownloadAttachment,
                        canDownload = canDownloadMedia,
                        onPlaybackProgress = onVideoPlaybackProgress
                    )
                }

                if (!courseClass.hasAnyMedia) {
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
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = GoldDark, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Materials Uploaded Yet", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Faculty and admins can upload PDFs, images, documents, audio lectures, or videos for this class.", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7F96), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp))
                        }
                    }
                }
            }
        }

        // Faculty / Admin Multi-File Upload Panel
        if (canManageClass) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Admin Media Upload Center", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                            }
                            Text(
                                text = "18MB Cloud / 50MB Device",
                                style = MaterialTheme.typography.labelSmall.copy(color = GoldSoft),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onOpenUploadDialog(MediaType.PDF) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_admin_multi_upload_center"),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Upload multiple files, PDF, image, document, audio, video",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val useSingleRow = maxWidth >= 420.dp
                            val optionItems = listOf(
                                Triple(MediaType.IMAGE, "Image", Icons.Default.Image) to "btn_upload_opt_image",
                                Triple(MediaType.DOCUMENT, "Document", Icons.Default.Article) to "btn_upload_opt_document",
                                Triple(MediaType.AUDIO, "Audio", Icons.Default.Audiotrack) to "btn_upload_opt_audio",
                                Triple(MediaType.VIDEO, "Video", Icons.Default.Videocam) to "btn_upload_opt_video"
                            )

                            if (useSingleRow) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    optionItems.forEach { (item, tag) ->
                                        val (type, label, icon) = item
                                        Button(
                                            onClick = { onOpenUploadDialog(type) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag(tag),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    optionItems.chunked(2).forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowItems.forEach { (item, tag) ->
                                                val (type, label, icon) = item
                                                Button(
                                                    onClick = { onOpenUploadDialog(type) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .testTag(tag),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = label,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onLoadDemoMedia,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldSoft),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldMetallic.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Attach Sample Lecture Pack (PDF + Audio + Video)",
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Navigation Footer
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (prevClass != null) {
                    OutlinedButton(onClick = { onNavigateClass(prevClass) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous", fontSize = 12.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (nextClass != null) {
                    Button(onClick = { onNavigateClass(nextClass) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = NavySecondary, contentColor = GoldMetallic), shape = RoundedCornerShape(10.dp)) {
                        Text("Next Lesson", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
