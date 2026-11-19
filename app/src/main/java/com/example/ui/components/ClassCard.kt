package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.example.ui.icons.ArrowDownward
import com.example.ui.icons.ArrowUpward
import com.example.ui.icons.Article
import com.example.ui.icons.AttachFile
import com.example.ui.icons.Audiotrack
import com.example.ui.icons.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import com.example.ui.icons.Description
import androidx.compose.material.icons.filled.Edit
import com.example.ui.icons.Image
import androidx.compose.material.icons.filled.Lock
import com.example.ui.icons.PlayCircle
import com.example.ui.icons.Quiz
import com.example.ui.icons.Schedule
import com.example.ui.icons.Visibility
import com.example.ui.icons.VisibilityOff
import com.example.ui.icons.BookmarkBorder
import com.example.ui.icons.CheckCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClassStatus
import com.example.model.CourseClass
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.SuccessGreen

@Composable
fun ClassCard(
    courseClass: CourseClass,
    isAdmin: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleBookmark: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onTogglePublish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("class_card_${courseClass.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (courseClass.isCompleted) Color(0xFFF0FDF4) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Class Number Circle (01..48)
                    val displayCode = courseClass.lectureCode.ifBlank {
                        String.format("%02d", courseClass.orderIndex)
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (courseClass.isCompleted) SuccessGreen else NavyPrimary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayCode,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (courseClass.isCompleted) Color.White else GoldMetallic,
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Status Pill
                    when (courseClass.status) {
                        ClassStatus.COMPLETED -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("COMPLETED", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                            }
                        }
                        ClassStatus.IN_PROGRESS -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF9C3))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("IN PROGRESS (${courseClass.activeLearningSeconds}s)", color = Color(0xFF854D0E), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }
                        ClassStatus.LOCKED -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("LOCKED", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("PENDING", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Duration
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF7A8B9E),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${courseClass.durationMinutes}m",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF7A8B9E),
                                fontSize = 11.sp
                            )
                        )
                    }

                    if (!courseClass.isPublished) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFEAEA))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DRAFT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DangerRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                )
                            )
                        }
                    }
                }

                // Completion & Bookmark toggles
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_bookmark_${courseClass.id}")
                    ) {
                        Icon(
                            imageVector = if (courseClass.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark Class",
                            tint = if (courseClass.isBookmarked) GoldDark else Color(0xFF9FB2CA),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_complete_${courseClass.id}")
                    ) {
                        Icon(
                            imageVector = if (courseClass.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                            contentDescription = "Toggle Complete",
                            tint = if (courseClass.isCompleted) SuccessGreen else Color(0xFF9FB2CA),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = courseClass.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    fontSize = 15.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Summary
            Text(
                text = courseClass.summary,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF5A6E85),
                    lineHeight = 16.sp
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Media badges and quiz badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allItems = courseClass.allAttachments
                    val hasPdf = allItems.any { it.type == com.example.model.MediaType.PDF }
                    val hasImage = allItems.any { it.type == com.example.model.MediaType.IMAGE }
                    val hasDoc = allItems.any { it.type == com.example.model.MediaType.DOCUMENT }
                    val hasAudio = allItems.any { it.type == com.example.model.MediaType.AUDIO }
                    val hasVideo = allItems.any { it.type == com.example.model.MediaType.VIDEO }

                    if (hasPdf) {
                        MediaBadge(icon = Icons.Default.Description, label = "PDF", color = Color(0xFFD32F2F))
                    }
                    if (hasImage) {
                        MediaBadge(icon = Icons.Default.Image, label = "Image", color = Color(0xFF00796B))
                    }
                    if (hasDoc) {
                        MediaBadge(icon = Icons.Default.Article, label = "Doc", color = Color(0xFF1565C0))
                    }
                    if (hasAudio) {
                        MediaBadge(icon = Icons.Default.Audiotrack, label = "Audio", color = Color(0xFF0277BD))
                    }
                    if (hasVideo) {
                        MediaBadge(icon = Icons.Default.PlayCircle, label = "Video", color = Color(0xFF7B1FA2))
                    }
                    if (allItems.size > 1) {
                        MediaBadge(icon = Icons.Default.AttachFile, label = "${allItems.size} Files", color = GoldDark)
                    }
                    if (!courseClass.hasAnyMedia) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF0F3F7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Ready for Files",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF8C9DB0),
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }

                if (courseClass.quizPassed) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Quiz, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("QUIZ PASSED", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
            }

            // Admin Control Actions Toolbar
            if (isAdmin) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF2F6FA))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Admin Tools:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NavyPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = !isFirst,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Move Up",
                                tint = if (!isFirst) NavyPrimary else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onMoveDown,
                            enabled = !isLast,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Move Down",
                                tint = if (!isLast) NavyPrimary else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onTogglePublish,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (courseClass.isPublished) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Visibility",
                                tint = if (courseClass.isPublished) SuccessGreen else DangerRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Class",
                                tint = NavyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Class",
                                tint = DangerRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}
