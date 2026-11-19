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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import com.example.ui.icons.HelpOutline
import com.example.ui.icons.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CourseClass
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.SuccessGreen

@Composable
fun QuizDialog(
    courseClass: CourseClass,
    passingScorePercent: Int,
    onDismiss: () -> Unit,
    onSubmitScore: (scorePercent: Int) -> Unit
) {
    val questions = courseClass.quizQuestions
    val userAnswers = remember { mutableStateMapOf<String, Int>() }
    var submitted by remember { mutableStateOf(false) }
    var calculatedScore by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavyPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            tint = GoldMetallic,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Class Mastery Assessment",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        )
                        Text(
                            text = "Passing Threshold: $passingScorePercent%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GoldDark,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Verify comprehension of '${courseClass.title}'. Answers are evaluated authoritatively to unlock class completion.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
                )

                if (submitted) {
                    val isPassed = calculatedScore >= passingScorePercent
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPassed) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPassed) SuccessGreen else DangerRed
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isPassed) "ASSESSMENT PASSED" else "NEEDS REVIEW",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPassed) SuccessGreen else DangerRed
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your Score: $calculatedScore%",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NavyPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isPassed) "Completion requirement satisfied! Checkmark recorded." else "Score is below $passingScorePercent%. Review the explanations below and retry.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF334155),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )
                        }
                    }
                }

                // Question items
                questions.forEachIndexed { qIndex, q ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Question ${qIndex + 1} of ${questions.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GoldDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = q.question,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            q.options.forEachIndexed { optIndex, optText ->
                                val isSelected = userAnswers[q.id] == optIndex
                                val isCorrect = q.correctOptionIndex == optIndex

                                val optBorderColor = when {
                                    submitted && isCorrect -> SuccessGreen
                                    submitted && isSelected && !isCorrect -> DangerRed
                                    isSelected -> GoldMetallic
                                    else -> Color(0xFFE2E8F0)
                                }

                                val optBgColor = when {
                                    submitted && isCorrect -> Color(0xFFDCFCE7)
                                    submitted && isSelected && !isCorrect -> Color(0xFFFEE2E2)
                                    isSelected -> Color(0xFFFFFBEB)
                                    else -> Color.White
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(optBgColor)
                                        .border(1.dp, optBorderColor, RoundedCornerShape(8.dp))
                                        .clickable(enabled = !submitted) {
                                            userAnswers[q.id] = optIndex
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { if (!submitted) userAnswers[q.id] = optIndex },
                                        enabled = !submitted,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = NavyPrimary,
                                            unselectedColor = Color.Gray
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = optText,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = NavyPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    )
                                }
                            }

                            if (submitted) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Explanation: ${q.explanation}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF475569),
                                        fontSize = 11.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!submitted) {
                Button(
                    onClick = {
                        var correctCount = 0
                        questions.forEach { q ->
                            if (userAnswers[q.id] == q.correctOptionIndex) correctCount++
                        }
                        val score = if (questions.isNotEmpty()) (correctCount * 100) / questions.size else 100
                        calculatedScore = score
                        submitted = true
                        onSubmitScore(score)
                    },
                    enabled = userAnswers.size == questions.size,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyPrimary,
                        contentColor = GoldMetallic
                    ),
                    modifier = Modifier.testTag("btn_submit_quiz")
                ) {
                    Text("Submit Answers (${userAnswers.size}/${questions.size})", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic)
                ) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (submitted && calculatedScore < passingScorePercent) {
                TextButton(onClick = {
                    submitted = false
                    userAnswers.clear()
                }) {
                    Text("Retry Assessment", color = NavyPrimary)
                }
            } else if (!submitted) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF6B7F96))
                }
            }
        }
    )
}
