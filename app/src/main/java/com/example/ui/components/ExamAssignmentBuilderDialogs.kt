package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CourseModule
import com.example.model.ExamQuestion
import com.example.model.ModuleExam
import com.example.util.BulkMcqQuestionParser
import com.example.ui.theme.BgLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.SuccessGreen

private data class EditableQuestionState(
    val id: String,
    var prompt: String,
    var optionA: String,
    var optionB: String,
    var optionC: String,
    var optionD: String,
    var correctIndex: Int,
    var marksText: String,
    var explanation: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamQuestionBuilderDialog(
    initialExam: ModuleExam?,
    modules: List<CourseModule>,
    defaultModuleId: String,
    authorUid: String,
    onDismiss: () -> Unit,
    onSave: (ModuleExam) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var selectedModuleId by remember(initialExam?.id, defaultModuleId) {
        mutableStateOf(initialExam?.moduleId ?: defaultModuleId)
    }
    var title by remember(initialExam?.id) { mutableStateOf(initialExam?.title ?: "") }
    var durationMinText by remember(initialExam?.id) {
        mutableStateOf((initialExam?.durationMinutes ?: 20).toString())
    }
    var passingPctText by remember(initialExam?.id) {
        mutableStateOf((initialExam?.passingPercentage ?: 70).toString())
    }
    var maxAttemptsText by remember(initialExam?.id) {
        mutableStateOf((initialExam?.maxAttempts ?: 3).toString())
    }
    var openUntilMs by remember(initialExam?.id) { mutableStateOf(initialExam?.openUntilMs) }
    var customHoursText by remember(initialExam?.id) { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val questionStates = remember(initialExam?.id) {
        val initialList = initialExam?.questions.orEmpty().mapIndexed { idx, q ->
            EditableQuestionState(
                id = q.id.ifBlank { "q_${System.currentTimeMillis()}_$idx" },
                prompt = q.question,
                optionA = q.options.getOrElse(0) { "" },
                optionB = q.options.getOrElse(1) { "" },
                optionC = q.options.getOrElse(2) { "" },
                optionD = q.options.getOrElse(3) { "" },
                correctIndex = q.correctOptionIndex.coerceIn(0, 3),
                marksText = q.marks.coerceAtLeast(1).toString(),
                explanation = q.explanation
            )
        }
        mutableStateListOf<EditableQuestionState>().apply {
            if (initialList.isNotEmpty()) addAll(initialList)
            else add(
                EditableQuestionState(
                    id = "q_${System.currentTimeMillis()}_0",
                    prompt = "",
                    optionA = "",
                    optionB = "",
                    optionC = "",
                    optionD = "",
                    correctIndex = 0,
                    marksText = "5",
                    explanation = ""
                )
            )
        }
    }

    var questionRenderTick by remember { mutableStateOf(0) }
    var showBulkImport by remember { mutableStateOf(false) }
    var bulkImportText by remember { mutableStateOf("") }
    var bulkImportStatus by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val applyBulkQuestions: (String) -> Unit = { rawText ->
        val parsed = BulkMcqQuestionParser.parse(rawText, defaultMarks = 5)
        if (parsed.questions.isEmpty()) {
            bulkImportStatus = "No numbered MCQ questions (1. Question / A. B. C. D.) found."
        } else {
            val isBlankSingle = questionStates.size == 1 &&
                questionStates[0].prompt.isBlank() &&
                questionStates[0].optionA.isBlank()
            if (isBlankSingle) questionStates.clear()
            parsed.questions.forEachIndexed { idx, q ->
                questionStates.add(
                    EditableQuestionState(
                        id = "q_${System.currentTimeMillis()}_${questionStates.size + idx}",
                        prompt = q.prompt,
                        optionA = q.options.getOrElse(0) { "" },
                        optionB = q.options.getOrElse(1) { "" },
                        optionC = q.options.getOrElse(2) { "" },
                        optionD = q.options.getOrElse(3) { "" },
                        correctIndex = q.correctIndex,
                        marksText = q.marks.toString(),
                        explanation = q.explanation
                    )
                )
            }
            questionRenderTick++
            bulkImportStatus = "Auto-filled ${parsed.questions.size} questions (${parsed.detectedAnswerCount}/${parsed.questions.size} answers detected)."
        }
    }

    val textFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            if (content.isNotBlank()) {
                bulkImportText = content
                showBulkImport = true
                applyBulkQuestions(content)
            } else {
                bulkImportStatus = "Could not read text from selected file. Paste question text below."
                showBulkImport = true
            }
        }
    }

    val computedMarks = remember(questionStates.size, questionRenderTick) {
        questionStates.sumOf { (it.marksText.toIntOrNull() ?: 5).coerceAtLeast(1) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("dialog_exam_builder"),
            color = BgLight
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
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
                                text = "ADMIN EXAM & MCQ QUESTION BUILDER",
                                color = GoldMetallic,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = if (initialExam == null) "Create Timed Module Exam" else "Edit Exam & Questions",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${questionStates.size} Question(s) • Total Marks: $computedMarks",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .background(NavySecondary, CircleShape)
                                .testTag("btn_close_exam_builder")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select Module", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
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
                        label = { Text("Exam Title *") },
                        placeholder = { Text("e.g. Module 4 Mid-Term Timed Assessment") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_exam_builder_title"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = durationMinText,
                            onValueChange = { durationMinText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Time Limit (Min)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_exam_builder_duration")
                        )
                        OutlinedTextField(
                            value = passingPctText,
                            onValueChange = { passingPctText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Pass %") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxAttemptsText,
                            onValueChange = { maxAttemptsText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Max Tries") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Closing Deadline Control
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Exam Window Cutoff (Auto-Lock Time Up)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Current Cutoff: ${formatAssessmentDeadline(openUntilMs)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (openUntilMs != null) GoldDark else Color.Gray
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DeadlinePresetChip("No Cutoff", openUntilMs == null) {
                                    openUntilMs = null
                                    customHoursText = ""
                                }
                                listOf("+1h" to 1, "+6h" to 6, "+24h" to 24, "+3d" to 72, "+7d" to 168).forEach { (label, hrs) ->
                                    DeadlinePresetChip(label, false) {
                                        openUntilMs = System.currentTimeMillis() + hrs * 3_600_000L
                                        customHoursText = hrs.toString()
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customHoursText,
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }
                                        customHoursText = digits
                                        val hrs = digits.toLongOrNull()
                                        openUntilMs = if (hrs != null && hrs > 0L) {
                                            System.currentTimeMillis() + hrs * 3_600_000L
                                        } else null
                                    },
                                    label = { Text("Custom Hours From Now") },
                                    placeholder = { Text("e.g. 48") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Smart Bulk Question Importer (Paste / Text File)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto-Import Questions (Paste / .txt)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = GoldMetallic
                                    )
                                    Text(
                                        text = "Paste numbered MCQs & options A–D to auto-fill all rows below.",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                TextButton(onClick = { showBulkImport = !showBulkImport }) {
                                    Text(
                                        text = if (showBulkImport) "Hide" else "Open",
                                        color = GoldMetallic,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (showBulkImport) {
                                OutlinedTextField(
                                    value = bulkImportText,
                                    onValueChange = { bulkImportText = it },
                                    label = { Text("Paste formatted MCQs (1. Question / A. B. C. D. / Answer: A)", color = Color.White.copy(alpha = 0.85f)) },
                                    minLines = 4,
                                    maxLines = 8,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { applyBulkQuestions(bulkImportText) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_bulk_import_apply")
                                    ) {
                                        Text("Auto-Fill", color = NavyPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { textFilePicker.launch(arrayOf("text/plain", "text/*")) },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text("Pick .txt File", color = GoldMetallic, fontSize = 12.sp)
                                    }
                                }
                                bulkImportStatus?.let { msg ->
                                    Text(text = msg, color = Color(0xFFBBF7D0), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // MCQ Question Builder Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MCQ Questions (${questionStates.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NavyPrimary
                        )
                        Button(
                            onClick = {
                                questionStates.add(
                                    EditableQuestionState(
                                        id = "q_${System.currentTimeMillis()}_${questionStates.size}",
                                        prompt = "",
                                        optionA = "",
                                        optionB = "",
                                        optionC = "",
                                        optionD = "",
                                        correctIndex = 0,
                                        marksText = "5",
                                        explanation = ""
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_add_mcq_question")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Question", color = GoldMetallic, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    questionStates.forEachIndexed { idx, qState ->
                        // Force recomposition on tick
                        keyedQuestionCard(
                            idx = idx,
                            qState = qState,
                            canRemove = questionStates.size > 1,
                            renderTick = questionRenderTick,
                            onMutate = { questionRenderTick++ },
                            onRemove = {
                                questionStates.removeAt(idx)
                                questionRenderTick++
                            }
                        )
                    }

                    validationError?.let { err ->
                        Text(text = err, color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (initialExam != null && !initialExam.isSyllabusFixedMarks && onDelete != null) {
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Custom Exam", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bottom Save Bar
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
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val cleanTitle = title.trim()
                                if (cleanTitle.isBlank()) {
                                    validationError = "Please enter an exam title."
                                    return@Button
                                }
                                val builtQuestions = questionStates.mapIndexedNotNull { idx, q ->
                                    val promptText = q.prompt.trim()
                                    if (promptText.isBlank()) return@mapIndexedNotNull null
                                    val opts = listOf(
                                        q.optionA.trim().ifBlank { "Option A" },
                                        q.optionB.trim().ifBlank { "Option B" },
                                        q.optionC.trim().ifBlank { "Option C" },
                                        q.optionD.trim().ifBlank { "Option D" }
                                    )
                                    ExamQuestion(
                                        id = q.id.ifBlank { "q_${System.currentTimeMillis()}_$idx" },
                                        question = promptText,
                                        options = opts,
                                        correctOptionIndex = q.correctIndex.coerceIn(0, 3),
                                        marks = (q.marksText.toIntOrNull() ?: 5).coerceIn(1, 100),
                                        explanation = q.explanation.trim().ifBlank { "Reviewed against ISCEB SCM standard." }
                                    )
                                }
                                if (builtQuestions.isEmpty()) {
                                    validationError = "Add at least one complete MCQ question."
                                    return@Button
                                }
                                val modNum = modules.find { it.id == selectedModuleId }?.number ?: 1
                                val totalMarks = if (initialExam?.isSyllabusFixedMarks == true && initialExam.totalMarks > 0) {
                                    initialExam.totalMarks
                                } else {
                                    builtQuestions.sumOf { it.marks }.coerceAtLeast(1)
                                }
                                val windowNote = if (openUntilMs != null) {
                                    "Closes ${formatAssessmentDeadline(openUntilMs)}"
                                } else {
                                    "Open Window (Auto-submits at 00:00)"
                                }
                                val updated = (initialExam ?: ModuleExam(
                                    id = "exam-custom-${System.currentTimeMillis()}",
                                    moduleId = selectedModuleId,
                                    title = cleanTitle,
                                    durationMinutes = 20,
                                    passingPercentage = 70,
                                    totalMarks = totalMarks,
                                    questions = builtQuestions,
                                    isSyllabusFixedMarks = false,
                                    createdByUid = authorUid
                                )).copy(
                                    moduleId = selectedModuleId,
                                    title = cleanTitle,
                                    durationMinutes = (durationMinText.toIntOrNull() ?: 20).coerceIn(1, 240),
                                    passingPercentage = (passingPctText.toIntOrNull() ?: 70).coerceIn(30, 100),
                                    maxAttempts = (maxAttemptsText.toIntOrNull() ?: 3).coerceIn(1, 20),
                                    totalMarks = totalMarks,
                                    questions = builtQuestions,
                                    openUntilMs = openUntilMs,
                                    scheduledWindowNote = windowNote,
                                    createdByUid = initialExam?.createdByUid?.ifBlank { authorUid } ?: authorUid
                                )
                                onSave(updated)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_save_exam_builder")
                        ) {
                            Text("Save Exam & Sync Live", color = GoldMetallic, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun keyedQuestionCard(
    idx: Int,
    qState: EditableQuestionState,
    canRemove: Boolean,
    renderTick: Int,
    onMutate: () -> Unit,
    onRemove: () -> Unit
) {
    val letters = listOf("A", "B", "C", "D")
    // Read renderTick so option & marks changes immediately refresh
    val tick = renderTick
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question #${idx + 1} (${tick * 0 + (qState.marksText.toIntOrNull() ?: 5)}M)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyDark
                )
                if (canRemove) {
                    TextButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_remove_mcq_question_$idx")
                    ) {
                        Text("Remove", color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            OutlinedTextField(
                value = qState.prompt,
                onValueChange = { qState.prompt = it; onMutate() },
                label = { Text("Question Prompt *") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            listOf(
                0 to qState.optionA,
                1 to qState.optionB,
                2 to qState.optionC,
                3 to qState.optionD
            ).forEach { (optIdx, valStr) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = qState.correctIndex == optIdx,
                        onClick = { qState.correctIndex = optIdx; onMutate() },
                        colors = RadioButtonDefaults.colors(selectedColor = SuccessGreen)
                    )
                    OutlinedTextField(
                        value = valStr,
                        onValueChange = { newText ->
                            when (optIdx) {
                                0 -> qState.optionA = newText
                                1 -> qState.optionB = newText
                                2 -> qState.optionC = newText
                                3 -> qState.optionD = newText
                            }
                            onMutate()
                        },
                        label = {
                            val correctTag = if (qState.correctIndex == optIdx) " (Correct)" else ""
                            Text("Option ${letters[optIdx]}$correctTag")
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = qState.marksText,
                    onValueChange = { qState.marksText = it.filter { c -> c.isDigit() }; onMutate() },
                    label = { Text("Marks") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = qState.explanation,
                    onValueChange = { qState.explanation = it; onMutate() },
                    label = { Text("Answer Explanation (Optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}
