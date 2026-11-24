package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CourseModule
import com.example.model.ModuleAssignment
import com.example.ui.theme.BgLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatAssessmentDeadline(ms: Long?): String {
    if (ms == null || ms <= 0L) return "No closing deadline"
    return try {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(ms))
    } catch (_: Exception) {
        "Scheduled cutoff"
    }
}

@Composable
internal fun DeadlinePresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NavyPrimary,
            selectedLabelColor = GoldMetallic
        ),
        modifier = Modifier.heightIn(min = 48.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignmentBuilderDialog(
    initialAssignment: ModuleAssignment?,
    modules: List<CourseModule>,
    defaultModuleId: String,
    authorUid: String,
    authorName: String,
    onDismiss: () -> Unit,
    onSave: (ModuleAssignment) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var selectedModuleId by remember(initialAssignment?.id, defaultModuleId) {
        mutableStateOf(initialAssignment?.moduleId ?: defaultModuleId)
    }
    var title by remember(initialAssignment?.id) { mutableStateOf(initialAssignment?.title ?: "") }
    var topic by remember(initialAssignment?.id) { mutableStateOf(initialAssignment?.topicDescription ?: "") }
    var instructions by remember(initialAssignment?.id) { mutableStateOf(initialAssignment?.instructions ?: "") }
    var maxMarksText by remember(initialAssignment?.id) {
        mutableStateOf((initialAssignment?.maxMarks ?: 20).toString())
    }
    var isFinalProject by remember(initialAssignment?.id) {
        mutableStateOf(initialAssignment?.isFinalProject ?: false)
    }
    var dueDateMs by remember(initialAssignment?.id) { mutableStateOf(initialAssignment?.dueDateMs) }
    var customHoursText by remember(initialAssignment?.id) { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("dialog_assignment_builder"),
            color = BgLight
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ADMIN ASSIGNMENT & DEADLINE BUILDER",
                                color = GoldMetallic,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (initialAssignment == null) "Create Module Assignment" else "Edit Assignment & Deadline",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .background(NavySecondary, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Target Module", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        modules.sortedBy { it.number }.forEach { mod ->
                            FilterChip(
                                selected = selectedModuleId == mod.id,
                                onClick = { selectedModuleId = mod.id },
                                label = { Text("M${mod.number}", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyPrimary,
                                    selectedLabelColor = GoldMetallic
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Assignment Title *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_assignment_builder_title")
                    )

                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Case Study / Question Prompt *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Submission Instructions") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = maxMarksText,
                            onValueChange = { maxMarksText = it.filter { c -> c.isDigit() } },
                            label = { Text("Max Marks") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_assignment_builder_marks")
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isFinalProject,
                                onCheckedChange = { isFinalProject = it },
                                colors = CheckboxDefaults.colors(checkedColor = NavyPrimary)
                            )
                            Text("Final Capstone", fontSize = 12.sp, color = NavyDark)
                        }
                    }

                    // Submission Deadline Selector
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Submission Deadline (Locks on Time Up)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Deadline: ${formatAssessmentDeadline(dueDateMs)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dueDateMs != null) GoldDark else Color.Gray
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DeadlinePresetChip("No Deadline", dueDateMs == null) {
                                    dueDateMs = null
                                    customHoursText = ""
                                }
                                listOf("+1h" to 1, "+6h" to 6, "+24h" to 24, "+3d" to 72, "+7d" to 168).forEach { (label, hrs) ->
                                    DeadlinePresetChip(label, false) {
                                        dueDateMs = System.currentTimeMillis() + hrs * 3_600_000L
                                        customHoursText = hrs.toString()
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = customHoursText,
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }
                                    customHoursText = digits
                                    val hrs = digits.toLongOrNull()
                                    dueDateMs = if (hrs != null && hrs > 0L) {
                                        System.currentTimeMillis() + hrs * 3_600_000L
                                    } else null
                                },
                                label = { Text("Custom Hours Until Deadline") },
                                placeholder = { Text("e.g. 72") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    validationError?.let { err ->
                        Text(text = err, color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (initialAssignment != null && !initialAssignment.isFinalProject && onDelete != null) {
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Assignment", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    validationError = "Please enter an assignment title."
                                    return@Button
                                }
                                val assignment = (initialAssignment ?: ModuleAssignment(
                                    id = "asg-${System.currentTimeMillis()}",
                                    moduleId = selectedModuleId,
                                    title = title.trim(),
                                    topicDescription = topic.trim(),
                                    instructions = instructions.trim(),
                                    createdByUid = authorUid,
                                    createdByName = authorName
                                )).copy(
                                    moduleId = selectedModuleId,
                                    title = title.trim(),
                                    topicDescription = topic.trim().ifBlank { "Complete the assigned supply chain analysis task." },
                                    instructions = instructions.trim().ifBlank { "Submit concise executive analysis before the deadline." },
                                    maxMarks = (maxMarksText.toIntOrNull() ?: 20).coerceIn(5, 200),
                                    isFinalProject = isFinalProject,
                                    isRequiredForCertification = isFinalProject || initialAssignment?.isRequiredForCertification == true,
                                    dueDateMs = dueDateMs
                                )
                                onSave(assignment)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_save_assignment_builder")
                        ) {
                            Text("Save Assignment & Sync", color = GoldMetallic, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
