package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.example.ui.icons.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ExamAttempt
import com.example.model.ModuleExam
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
fun ExamDialog(
    exam: ModuleExam,
    timeRemainingSeconds: Int,
    currentAnswers: Map<String, Int>,
    attemptResult: ExamAttempt?,
    isSubmitted: Boolean,
    onSelectAnswer: (questionId: String, optionIndex: Int) -> Unit,
    onSubmit: () -> Unit,
    onRetake: () -> Unit,
    onDismiss: () -> Unit
) {
    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)
    val isTimerCritical = timeRemainingSeconds in 1..120

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = BgLight
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgLight)
            ) {
                // Top Header Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (exam.isSyllabusFixedMarks) "OFFICIAL SYLLABUS EXAM" else "MODULE ASSESSMENT",
                                        color = GoldMetallic,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (exam.isSyllabusFixedMarks) GoldMetallic.copy(alpha = 0.2f) else NavySurface,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${exam.totalMarks} Marks",
                                            color = if (exam.isSyllabusFixedMarks) GoldSoft else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = exam.title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(NavySecondary, CircleShape)
                                    .testTag("btn_close_exam")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Status & Timer Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exam.scheduledWindowNote,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp
                            )

                            if (!isSubmitted) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(
                                            if (isTimerCritical) DangerRed.copy(alpha = 0.25f) else GoldMetallic.copy(alpha = 0.2f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isTimerCritical) DangerRed else GoldMetallic,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = "Timer",
                                        tint = if (isTimerCritical) DangerRed else GoldMetallic,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = timerText,
                                        color = if (isTimerCritical) DangerRed else GoldSoft,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Scrollable Questions / Results
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Result Banner if submitted
                    if (isSubmitted && attemptResult != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (attemptResult.isPassed) SuccessGreen.copy(alpha = 0.12f) else DangerRed.copy(alpha = 0.12f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (attemptResult.isPassed) SuccessGreen else DangerRed
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (attemptResult.isPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (attemptResult.isPassed) SuccessGreen else DangerRed,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (attemptResult.isPassed) "CONGRATULATIONS! EXAM PASSED" else "NEEDS IMPROVEMENT",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (attemptResult.isPassed) SuccessGreen else DangerRed
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Score: ${attemptResult.scoreMarks} / ${attemptResult.totalMarks} Marks (${attemptResult.percentage}%)",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = "Pass Requirement: ${exam.passingPercentage}% • ${if (attemptResult.isAutoSubmittedOnTimeout) "Auto-submitted on time expiry" else "Submitted on time"}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = onRetake,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_retake_exam")
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retake Examination")
                                }
                            }
                        }
                    }

                    // Questions Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSubmitted) "Answer Review & Explanations" else "Questions (${exam.questions.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        if (!isSubmitted) {
                            Text(
                                text = "${currentAnswers.size} of ${exam.questions.size} answered",
                                fontSize = 12.sp,
                                color = if (currentAnswers.size == exam.questions.size) SuccessGreen else Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Questions Render
                    exam.questions.forEachIndexed { index, q ->
                        val selectedOption = currentAnswers[q.id]
                        val isCorrect = selectedOption == q.correctOptionIndex

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exam_question_$index"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSubmitted) {
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCorrect) SuccessGreen else DangerRed
                                )
                            } else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "Q${index + 1}. ${q.question}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(NavySurface.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${q.marks}M",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                q.options.forEachIndexed { optIdx, optText ->
                                    val isThisSelected = selectedOption == optIdx
                                    val isThisCorrectAnswer = q.correctOptionIndex == optIdx

                                    val rowBg = when {
                                        !isSubmitted && isThisSelected -> GoldSoft.copy(alpha = 0.25f)
                                        isSubmitted && isThisCorrectAnswer -> SuccessGreen.copy(alpha = 0.15f)
                                        isSubmitted && isThisSelected && !isCorrect -> DangerRed.copy(alpha = 0.15f)
                                        else -> Color.Transparent
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(rowBg)
                                            .clickable(enabled = !isSubmitted) {
                                                onSelectAnswer(q.id, optIdx)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isThisSelected,
                                            onClick = if (!isSubmitted) { { onSelectAnswer(q.id, optIdx) } } else null,
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = if (isSubmitted) {
                                                    if (isThisCorrectAnswer) SuccessGreen else DangerRed
                                                } else GoldMetallic,
                                                unselectedColor = Color.Gray
                                            ),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = optText,
                                            fontSize = 13.sp,
                                            color = if (isSubmitted && isThisCorrectAnswer) SuccessGreen else NavyPrimary,
                                            fontWeight = if (isThisSelected || (isSubmitted && isThisCorrectAnswer)) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (isSubmitted) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(BgLight, RoundedCornerShape(6.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Explanation: ${q.explanation}",
                                            fontSize = 12.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar
                if (!isSubmitted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentAnswers.size} / ${exam.questions.size} answered",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = NavyPrimary
                            )

                            Button(
                                onClick = onSubmit,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldMetallic,
                                    contentColor = NavyPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_submit_exam")
                            ) {
                                Text("Submit Examination", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
