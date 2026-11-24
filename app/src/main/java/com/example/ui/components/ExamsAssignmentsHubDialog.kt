package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AssignmentSubmission
import com.example.model.CourseModule
import com.example.model.ExamAttempt
import com.example.model.ModuleAssignment
import com.example.model.ModuleExam
import com.example.ui.icons.Assignment
import com.example.ui.icons.Quiz
import com.example.ui.theme.BgLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.SuccessGreen

@Composable
fun ExamsAssignmentsHubDialog(
    modules: List<CourseModule>,
    exams: List<ModuleExam>,
    assignments: List<ModuleAssignment>,
    examAttempts: List<ExamAttempt>,
    assignmentSubmissions: List<AssignmentSubmission>,
    currentUserId: String,
    onTakeExam: (ModuleExam) -> Unit,
    onOpenAssignment: (ModuleAssignment) -> Unit,
    onDismiss: () -> Unit
) {
    val moduleTitleById = modules.associate { it.id to it.title }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 620.dp)
                .testTag("dialog_exams_assignments_hub"),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyDark)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exams & Assignments",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${exams.size} Assessments • ${assignments.size} Practical Assignments",
                            color = GoldSoft,
                            fontSize = 11.5.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Exams and Assignments",
                            tint = Color.White
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Quiz,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MODULE & FINAL EXAMS (${exams.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NavyPrimary
                                )
                            )
                        }
                    }

                    items(exams, key = { it.id }) { exam ->
                        val userAttempts = examAttempts.filter {
                            it.examId == exam.id && it.studentId == currentUserId
                        }
                        val best = userAttempts.maxByOrNull { it.scoreMarks }
                        val modTitle = moduleTitleById[exam.moduleId] ?: "PGD-SCM Curriculum"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFD6E2F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exam.title,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$modTitle • ${exam.totalMarks} Marks • ${exam.durationMinutes} min",
                                        color = Color(0xFF475569),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (exam.openUntilMs != null) {
                                        val expired = System.currentTimeMillis() > exam.openUntilMs
                                        Text(
                                            text = if (expired) "Time Up • Window Closed" else "Closes: ${formatAssessmentDeadline(exam.openUntilMs)}",
                                            color = if (expired) DangerRed else GoldDark,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                    if (best != null) {
                                        Text(
                                            text = "Best: ${best.scoreMarks}/${best.totalMarks} (${best.percentage}%)" +
                                                if (best.isPassed) " • PASSED" else " • Retry",
                                            color = if (best.isPassed) SuccessGreen else GoldDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onTakeExam(exam) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NavyPrimary,
                                        contentColor = GoldMetallic
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    val isExpiredNoAttempt = best == null && exam.openUntilMs != null && System.currentTimeMillis() > exam.openUntilMs
                                    Text(
                                        text = when {
                                            best != null -> "Retake"
                                            isExpiredNoAttempt -> "Closed"
                                            else -> "Start"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Assignment,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PRACTICAL ASSIGNMENTS (${assignments.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NavyPrimary
                                )
                            )
                        }
                    }

                    items(assignments, key = { it.id }) { assignment ->
                        val sub = assignmentSubmissions.find {
                            it.assignmentId == assignment.id && it.studentId == currentUserId
                        }
                        val modTitle = moduleTitleById[assignment.moduleId] ?: "PGD-SCM Curriculum"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFD6E2F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = assignment.title,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$modTitle • ${assignment.maxMarks} Marks",
                                        color = Color(0xFF475569),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val dueMs = assignment.dueDateMs
                                    val isExpired = dueMs != null && System.currentTimeMillis() > dueMs
                                    if (dueMs != null) {
                                        Text(
                                            text = if (isExpired) "Time Up • Deadline Passed" else "Due: ${formatAssessmentDeadline(dueMs)}",
                                            color = if (isExpired) DangerRed else GoldDark,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                    Text(
                                        text = when {
                                            sub?.isGraded == true -> "Graded: ${sub.marksAwarded}/${assignment.maxMarks}"
                                            sub != null -> "Submitted • Awaiting Grading"
                                            isExpired -> "Closed (Time Up)"
                                            else -> "Pending Submission"
                                        },
                                        color = when {
                                            sub?.isGraded == true -> SuccessGreen
                                            sub != null -> NavySecondary
                                            isExpired -> DangerRed
                                            else -> GoldDark
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                ) {
                                    Button(
                                        onClick = { onOpenAssignment(assignment) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GoldMetallic,
                                            contentColor = NavyDark
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (sub != null) "View" else "Open",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = NavyPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
