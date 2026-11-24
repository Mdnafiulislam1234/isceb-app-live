package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import com.example.ui.icons.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.example.ui.icons.Quiz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.model.AssignmentSubmission
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.ExamAttempt
import com.example.model.ModuleAssignment
import com.example.model.ModuleExam
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModuleDetailScreen(
    module: CourseModule,
    classes: List<CourseClass>,
    exam: ModuleExam?,
    bestAttempt: ExamAttempt?,
    assignment: ModuleAssignment?,
    assignmentSubmission: AssignmentSubmission?,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onClassClick: (CourseClass) -> Unit,
    onToggleComplete: (CourseClass) -> Unit,
    onToggleBookmark: (CourseClass) -> Unit,
    onEditModule: (CourseModule) -> Unit,
    onDeleteModule: (CourseModule) -> Unit,
    onAddClass: (CourseModule) -> Unit,
    onEditClass: (CourseClass) -> Unit,
    onDeleteClass: (CourseClass) -> Unit,
    onMoveClassUp: (CourseClass) -> Unit,
    onMoveClassDown: (CourseClass) -> Unit,
    onToggleClassPublish: (CourseClass) -> Unit,
    onTakeExam: (ModuleExam) -> Unit,
    onOpenAssignment: (ModuleAssignment) -> Unit,
    canManageModule: Boolean = isAdmin,
    attemptsUsedCount: Int = 0,
    onCreateOrEditExam: () -> Unit = {},
    onCreateOrEditAssignment: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val moduleClasses = classes
        .filter { it.moduleId == module.id && (canManageModule || it.isPublished) }
        .sortedBy { it.orderIndex }
    val completedCount = moduleClasses.count { it.isCompleted }
    val progressRatio = if (moduleClasses.isNotEmpty()) completedCount.toFloat() / moduleClasses.size else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar & Back Button
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("btn_back_from_module")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to list",
                                tint = Color.White
                            )
                        }

                        // Module Part Badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoldMetallic.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = module.part.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GoldSoft,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NavySecondary)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "MODULE ${module.number}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GoldMetallic,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }

                        // Admin edit actions
                        if (isAdmin) {
                            Row {
                                IconButton(
                                    onClick = { onEditModule(module) },
                                    modifier = Modifier.testTag("btn_edit_module_${module.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Module",
                                        tint = GoldMetallic
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteModule(module) },
                                    modifier = Modifier.testTag("btn_delete_module_${module.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Module",
                                        tint = DangerRed
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = module.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = module.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFD6E2F0),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Section inside header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$completedCount of ${moduleClasses.size} Lectures Completed",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GoldSoft,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "${(progressRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = GoldMetallic,
                        trackColor = NavySurface
                    )
                }
            }
        }

        // Module Assessment & Examination Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                tint = GoldMetallic,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Module Assessments & Capstone",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Timed MCQ Examination & Practical Assignment",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (canManageModule) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCreateOrEditExam,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_module_manage_exam_${module.id}")
                            ) {
                                Text(
                                    text = if (exam != null) "Edit Exam & MCQ (${exam.questions.size})" else "+ Add Timed Exam",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            }
                            OutlinedButton(
                                onClick = onCreateOrEditAssignment,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_module_manage_assignment_${module.id}")
                            ) {
                                Text(
                                    text = if (assignment != null) "Edit Task & Deadline" else "+ Add Assignment",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Module Exam Row
                    if (exam != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgLight)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Module MCQ Exam",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = NavyPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (exam.isSyllabusFixedMarks) GoldMetallic.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.4f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (exam.isSyllabusFixedMarks) "${exam.totalMarks} Marks (Syllabus)" else "${exam.totalMarks} Marks (Configurable)",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = if (exam.isSyllabusFixedMarks) GoldDark else Color.DarkGray,
                                            modifier = Modifier.testTag("badge_exam_marks_${module.id}")
                                        )
                                    }
                                }

                                if (bestAttempt != null) {
                                    Text(
                                        text = "Best: ${bestAttempt.scoreMarks}/${bestAttempt.totalMarks} (${bestAttempt.percentage}%) • ${if (bestAttempt.isPassed) "PASSED" else "FAILED"} • Attempts: $attemptsUsedCount/${exam.maxAttempts}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (bestAttempt.isPassed) SuccessGreen else DangerRed
                                    )
                                } else {
                                    Text(
                                        text = "Duration: ${exam.durationMinutes} mins • Attempts: $attemptsUsedCount/${exam.maxAttempts} • Auto-submits",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = { onTakeExam(exam) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (bestAttempt?.isPassed == true) SuccessGreen else GoldMetallic,
                                    contentColor = NavyPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_take_module_exam_${module.id}")
                            ) {
                                Text(
                                    text = if (bestAttempt != null) "Review" else "Take Exam",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Module Assignment / Project Row
                    if (assignment != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgLight)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (assignment.isFinalProject) "Capstone Final Project" else "Module Practical Assignment",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = NavyPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(NavySurface.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${assignment.maxMarks} Marks",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = NavyPrimary,
                                            modifier = Modifier.testTag("badge_assignment_marks_${module.id}")
                                        )
                                    }
                                }

                                if (assignmentSubmission != null) {
                                    Text(
                                        text = if (assignmentSubmission.isGraded) "Grade: ${assignmentSubmission.marksAwarded}/${assignment.maxMarks} Marks" else "Submitted • Pending Review",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (assignmentSubmission.isGraded) SuccessGreen else GoldDark
                                    )
                                } else {
                                    val dueMs = assignment.dueDateMs
                                    val expired = dueMs != null && System.currentTimeMillis() > dueMs
                                    Text(
                                        text = when {
                                            expired -> "Time Up • Submissions Closed"
                                            dueMs != null -> "Deadline: ${com.example.ui.components.formatAssessmentDeadline(dueMs)}"
                                            else -> "Real-world case solution & upload"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (dueMs != null) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (expired) DangerRed else Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            OutlinedButton(
                                onClick = { onOpenAssignment(assignment) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_open_assignment_${module.id}")
                            ) {
                                Text(
                                    text = if (assignmentSubmission != null) "View" else "Submit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = NavyPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Faculty / Admin Add Class Bar
        if (canManageModule) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lectures in Module (${moduleClasses.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    )

                    Button(
                        onClick = { onAddClass(module) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyPrimary,
                            contentColor = GoldMetallic
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_add_class_to_module")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Lecture", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Lectures in Module (${moduleClasses.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Classes List
        itemsIndexed(moduleClasses) { index, cls ->
            val isFirst = index == 0
            val isLast = index == moduleClasses.size - 1

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ClassCard(
                    courseClass = cls,
                    isAdmin = canManageModule,
                    isFirst = isFirst,
                    isLast = isLast,
                    onClick = { onClassClick(cls) },
                    onToggleComplete = { onToggleComplete(cls) },
                    onToggleBookmark = { onToggleBookmark(cls) },
                    onEditClick = { onEditClass(cls) },
                    onDeleteClick = { onDeleteClass(cls) },
                    onMoveUp = { onMoveClassUp(cls) },
                    onMoveDown = { onMoveClassDown(cls) },
                    onTogglePublish = { onToggleClassPublish(cls) }
                )
            }
        }
    }
}
