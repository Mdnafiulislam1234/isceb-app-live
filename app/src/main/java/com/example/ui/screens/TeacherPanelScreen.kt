package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.AccountStatus
import com.example.model.AssignmentSubmission
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.ExamAttempt
import com.example.model.ModuleAssignment
import com.example.model.ModuleExam
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.icons.Assignment
import com.example.ui.icons.CloudUpload
import com.example.ui.icons.Quiz
import com.example.ui.icons.School
import com.example.ui.icons.Visibility
import com.example.ui.icons.VisibilityOff
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TeacherPanelScreen(
    currentUser: UserProfile,
    isAdmin: Boolean,
    modules: List<CourseModule>,
    classes: List<CourseClass>,
    moduleExams: List<ModuleExam>,
    moduleAssignments: List<ModuleAssignment>,
    examAttempts: List<ExamAttempt>,
    assignmentSubmissions: List<AssignmentSubmission>,
    registeredAccounts: List<UserProfile>,
    onBack: () -> Unit,
    onOpenModule: (CourseModule) -> Unit,
    onOpenClass: (CourseClass) -> Unit,
    onAddClass: (moduleId: String) -> Unit,
    onEditClass: (CourseClass) -> Unit,
    onToggleClassPublish: (classId: String) -> Unit,
    onUploadClassMedia: (CourseClass) -> Unit,
    onSaveExam: (ModuleExam) -> Unit,
    onSaveAssignment: (ModuleAssignment) -> Unit,
    onOpenAssignmentForGrading: (ModuleAssignment) -> Unit,
    onGradeSubmission: (submissionId: String, marks: Int, feedback: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val canAccessTeacherPanel = isAdmin || (
        currentUser.role == UserRole.TEACHER &&
            currentUser.accountStatus == AccountStatus.ACTIVE
        )

    val scopedModules = remember(modules, currentUser.assignedModuleIds, isAdmin) {
        if (isAdmin) modules.sortedBy { it.number }
        else modules.filter { currentUser.assignedModuleIds.contains(it.id) }.sortedBy { it.number }
    }
    val scopedModuleIds = remember(scopedModules) { scopedModules.map { it.id }.toSet() }
    val scopedClasses = remember(classes, scopedModuleIds) {
        classes.filter { scopedModuleIds.contains(it.moduleId) }
    }
    val scopedExams = remember(moduleExams, scopedModuleIds) {
        moduleExams.filter { scopedModuleIds.contains(it.moduleId) }
    }
    val scopedAssignments = remember(moduleAssignments, scopedModuleIds) {
        moduleAssignments.filter { scopedModuleIds.contains(it.moduleId) }
    }
    val scopedSubmissions = remember(assignmentSubmissions, scopedAssignments) {
        val asgIds = scopedAssignments.map { it.id }.toSet()
        assignmentSubmissions.filter {
            scopedModuleIds.contains(it.moduleId) || asgIds.contains(it.assignmentId)
        }.sortedWith(compareBy<AssignmentSubmission> { it.isGraded }.thenByDescending { it.submissionTimestamp })
    }
    val scopedAttempts = remember(examAttempts, scopedExams) {
        val examIds = scopedExams.map { it.id }.toSet()
        examAttempts.filter { scopedModuleIds.contains(it.moduleId) || examIds.contains(it.examId) }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Classes", "Assessments", "Grading")

    var editingExam by remember { mutableStateOf<ModuleExam?>(null) }
    var editingAssignment by remember { mutableStateOf<ModuleAssignment?>(null) }
    var creatingAssignmentForModuleId by remember { mutableStateOf<String?>(null) }
    var inlineGradingSubId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight)
            .testTag("screen_teacher_panel"),
        contentPadding = PaddingValues(bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_teacher_panel_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back from Teacher Panel",
                                tint = GoldMetallic
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Faculty Teacher Panel",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (isAdmin) {
                                    "Admin Oversight • Full 12-Module Scope"
                                } else {
                                    "${currentUser.fullName} • ${scopedModules.size} Assigned Module(s)"
                                },
                                color = GoldSoft,
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NavySecondary
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "ISCEB Faculty",
                                tint = GoldMetallic,
                                modifier = Modifier.padding(8.dp).size(22.dp)
                            )
                        }
                    }

                    if (canAccessTeacherPanel) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TeacherKpiChip(
                                label = "Modules",
                                value = "${scopedModules.size}",
                                modifier = Modifier.weight(1f)
                            )
                            TeacherKpiChip(
                                label = "Classes",
                                value = "${scopedClasses.size}",
                                modifier = Modifier.weight(1f)
                            )
                            TeacherKpiChip(
                                label = "Pending Review",
                                value = "${scopedSubmissions.count { !it.isGraded }}",
                                modifier = Modifier.weight(1f)
                            )
                            TeacherKpiChip(
                                label = "Students",
                                value = "${registeredAccounts.count { it.role == UserRole.STUDENT }}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (!canAccessTeacherPanel) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Restricted",
                            tint = DangerRed,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Faculty Access Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NavyDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Only active ISCEB Teachers assigned to modules or Administrators may access this portal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
            return@LazyColumn
        }

        if (scopedModules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("card_teacher_no_modules"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "No Modules Assigned Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NavyPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your faculty account is active, but an ISCEB Administrator has not assigned any SCM modules to your scope yet. Contact ISCEB Admin to assign modules.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
            return@LazyColumn
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
                        color = GoldDark
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("tab_teacher_${label.lowercase()}"),
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Scoped Modules & Classes Management
                items(scopedModules, key = { it.id }) { mod ->
                    val modClasses = scopedClasses.filter { it.moduleId == mod.id }.sortedBy { it.orderIndex }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("card_teacher_module_${mod.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Module ${mod.number} • ${mod.part.label}",
                                        color = GoldDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = mod.title,
                                        color = NavyDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Row {
                                    TextButton(
                                        onClick = { onOpenModule(mod) },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text("Open", color = NavyPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onAddClass(mod.id) },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_teacher_add_class_${mod.id}"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Class to ${mod.title}",
                                            tint = NavyPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Class", fontSize = 11.sp, color = NavyPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            modClasses.forEach { cls ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (cls.isPublished) BgLight else Color(0xFFFFF7ED),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "${cls.lectureCode.ifBlank { "#${cls.orderIndex}" }} • ${cls.title}",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = NavyDark
                                            )
                                            Text(
                                                text = buildString {
                                                    append(if (cls.isPublished) "Published" else "Draft / Unpublished")
                                                    append(" • ${cls.allAttachments.size} material(s)")
                                                },
                                                fontSize = 11.sp,
                                                color = if (cls.isPublished) SuccessGreen else GoldDark
                                            )
                                        }
                                        IconButton(
                                            onClick = { onToggleClassPublish(cls.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("btn_teacher_toggle_pub_${cls.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (cls.isPublished) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (cls.isPublished) "Unpublish Class" else "Publish Class",
                                                tint = if (cls.isPublished) SuccessGreen else GoldDark
                                            )
                                        }
                                        IconButton(
                                            onClick = { onUploadClassMedia(cls) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("btn_teacher_upload_${cls.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = "Upload Lecture Materials",
                                                tint = NavyPrimary
                                            )
                                        }
                                        IconButton(
                                            onClick = { onEditClass(cls) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("btn_teacher_edit_${cls.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Class",
                                                tint = NavySecondary
                                            )
                                        }
                                        TextButton(
                                            onClick = { onOpenClass(cls) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("View", fontSize = 11.sp, color = NavyPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Assessments & Exams Editor
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Scoped Module Exams (${scopedExams.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NavyDark
                            )
                            Text(
                                text = "Configure passing score threshold, duration, and attempt limits for your assigned modules.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            scopedExams.forEach { exam ->
                                val mod = scopedModules.find { it.id == exam.moduleId }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BgLight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Quiz,
                                            contentDescription = "Exam",
                                            tint = NavyPrimary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = exam.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = NavyDark
                                            )
                                            Text(
                                                text = "Module ${mod?.number ?: "?"} • ${exam.totalMarks} Marks • Pass ≥${exam.passingPercentage}% • ${exam.durationMinutes}m • Max ${exam.maxAttempts} attempts",
                                                fontSize = 11.sp,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { editingExam = exam },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_teacher_edit_exam_${exam.id}")
                                        ) {
                                            Text("Configure", fontSize = 11.sp, color = NavyPrimary)
                                        }
                                    }
                                }
                            }

                            editingExam?.let { targetExam ->
                                Spacer(modifier = Modifier.height(10.dp))
                                TeacherExamConfigEditor(
                                    exam = targetExam,
                                    onCancel = { editingExam = null },
                                    onSave = { updated ->
                                        onSaveExam(updated)
                                        editingExam = null
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Scoped Assignments (${scopedAssignments.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NavyDark
                                    )
                                    Text(
                                        text = "Manage coursework instructions and create new module tasks.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF475569)
                                    )
                                }
                                scopedModules.firstOrNull()?.let { defaultMod ->
                                    Button(
                                        onClick = { creatingAssignmentForModuleId = defaultMod.id },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_teacher_new_assignment")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "New Assignment", tint = GoldMetallic, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("New Task", fontSize = 12.sp, color = GoldMetallic, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            scopedAssignments.forEach { asg ->
                                val subCount = scopedSubmissions.count { it.assignmentId == asg.id }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BgLight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Assignment,
                                            contentDescription = "Assignment",
                                            tint = GoldDark
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = asg.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = NavyDark
                                            )
                                            Text(
                                                text = "${asg.moduleId.uppercase()} • ${asg.maxMarks} Marks • $subCount submission(s)",
                                                fontSize = 11.sp,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                        TextButton(
                                            onClick = { onOpenAssignmentForGrading(asg) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Grade ($subCount)", fontSize = 11.sp, color = NavyPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(
                                            onClick = { editingAssignment = asg },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Assignment", tint = NavySecondary)
                                        }
                                    }
                                }
                            }

                            val newModId = creatingAssignmentForModuleId
                            if (editingAssignment != null || newModId != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                TeacherAssignmentEditor(
                                    initialAssignment = editingAssignment,
                                    scopedModules = scopedModules,
                                    defaultModuleId = newModId ?: editingAssignment?.moduleId ?: scopedModules.first().id,
                                    authorUid = currentUser.authUid.ifBlank { currentUser.id },
                                    authorName = currentUser.fullName,
                                    onCancel = {
                                        editingAssignment = null
                                        creatingAssignmentForModuleId = null
                                    },
                                    onSave = { asg ->
                                        onSaveAssignment(asg)
                                        editingAssignment = null
                                        creatingAssignmentForModuleId = null
                                    }
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // Grading Queue & Module Analytics
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Exam Performance Summary (Assigned Modules)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NavyDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (scopedAttempts.isEmpty()) {
                                Text(
                                    text = "No exam attempts recorded yet for your assigned modules.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            } else {
                                val passed = scopedAttempts.count { it.isPassed }
                                val passRate = passed.toFloat() / scopedAttempts.size.toFloat()
                                Text(
                                    text = "$passed of ${scopedAttempts.size} attempt(s) passed (${(passRate * 100).toInt()}% pass rate)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = NavyPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { passRate },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = SuccessGreen,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Assignment Submissions Queue (${scopedSubmissions.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NavyDark,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (scopedSubmissions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = "No student assignment submissions waiting in your module queue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(scopedSubmissions, key = { it.submissionId }) { sub ->
                        val parentAsg = scopedAssignments.find { it.id == sub.assignmentId }
                        val maxMarks = parentAsg?.maxMarks ?: 20
                        val isEditingThis = inlineGradingSubId == sub.submissionId

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("card_teacher_submission_${sub.submissionId}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sub.studentName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NavyDark
                                        )
                                        Text(
                                            text = parentAsg?.title ?: sub.assignmentId,
                                            fontSize = 12.sp,
                                            color = GoldDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (sub.isGraded) Color(0xFFDCFCE7) else GoldSoft
                                    ) {
                                        Text(
                                            text = if (sub.isGraded) "Graded: ${sub.marksAwarded}/$maxMarks" else "Pending Review",
                                            color = if (sub.isGraded) SuccessGreen else GoldDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = sub.submissionText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155),
                                    maxLines = 4
                                )
                                if (!sub.attachedFileName.isNullOrBlank()) {
                                    Text(
                                        text = "Attachment: ${sub.attachedFileName}",
                                        fontSize = 11.sp,
                                        color = NavyPrimary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Submitted ${formatTeacherDate(sub.submissionTimestamp)}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                if (isEditingThis) {
                                    var marksText by remember(sub.submissionId) {
                                        mutableStateOf(sub.marksAwarded?.toString() ?: "")
                                    }
                                    var feedbackText by remember(sub.submissionId) {
                                        mutableStateOf(sub.feedback ?: "")
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = marksText,
                                        onValueChange = { marksText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Marks (0 - $maxMarks)") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_teacher_marks_${sub.submissionId}")
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = feedbackText,
                                        onValueChange = { feedbackText = it },
                                        label = { Text("Faculty Feedback") },
                                        minLines = 2,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_teacher_feedback_${sub.submissionId}")
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { inlineGradingSubId = null },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Cancel")
                                        }
                                        Button(
                                            onClick = {
                                                val numeric = (marksText.toIntOrNull() ?: 0).coerceIn(0, maxMarks)
                                                onGradeSubmission(sub.submissionId, numeric, feedbackText)
                                                inlineGradingSubId = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_teacher_submit_grade_${sub.submissionId}")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Save Grade", tint = GoldMetallic, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Save Grade & Lock", color = GoldMetallic, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { inlineGradingSubId = sub.submissionId },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_teacher_inline_grade_${sub.submissionId}")
                                        ) {
                                            Text(if (sub.isGraded) "Update Grade" else "Grade Here", fontSize = 12.sp, color = NavyPrimary)
                                        }
                                        parentAsg?.let { asg ->
                                            TextButton(
                                                onClick = { onOpenAssignmentForGrading(asg) },
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Text("Open Full View", fontSize = 12.sp, color = NavySecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherKpiChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = NavyDark
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = GoldMetallic,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
            Text(
                text = label,
                color = Color(0xFFCBD5E1),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TeacherExamConfigEditor(
    exam: ModuleExam,
    onCancel: () -> Unit,
    onSave: (ModuleExam) -> Unit
) {
    var title by remember(exam.id) { mutableStateOf(exam.title) }
    var passingPct by remember(exam.id) { mutableStateOf(exam.passingPercentage.toString()) }
    var durationMin by remember(exam.id) { mutableStateOf(exam.durationMinutes.toString()) }
    var maxAttempts by remember(exam.id) { mutableStateOf(exam.maxAttempts.toString()) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NavySurface.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Configure Exam: ${exam.id}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = NavyPrimary
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Exam Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = passingPct,
                    onValueChange = { passingPct = it.filter { c -> c.isDigit() } },
                    label = { Text("Pass %") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = durationMin,
                    onValueChange = { durationMin = it.filter { c -> c.isDigit() } },
                    label = { Text("Minutes") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxAttempts,
                    onValueChange = { maxAttempts = it.filter { c -> c.isDigit() } },
                    label = { Text("Max Tries") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            exam.copy(
                                title = title.trim().ifBlank { exam.title },
                                passingPercentage = (passingPct.toIntOrNull() ?: exam.passingPercentage).coerceIn(30, 100),
                                durationMinutes = (durationMin.toIntOrNull() ?: exam.durationMinutes).coerceIn(5, 180),
                                maxAttempts = (maxAttempts.toIntOrNull() ?: exam.maxAttempts).coerceIn(1, 20)
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_teacher_save_exam")
                ) {
                    Text("Save Exam Rules", color = GoldMetallic, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TeacherAssignmentEditor(
    initialAssignment: ModuleAssignment?,
    scopedModules: List<CourseModule>,
    defaultModuleId: String,
    authorUid: String,
    authorName: String,
    onCancel: () -> Unit,
    onSave: (ModuleAssignment) -> Unit
) {
    var selectedModuleId by remember(initialAssignment?.id, defaultModuleId) {
        mutableStateOf(initialAssignment?.moduleId ?: defaultModuleId)
    }
    var title by remember(initialAssignment?.id) { mutableStateOf(initialAssignment?.title ?: "") }
    var topic by remember(initialAssignment?.id) { mutableStateOf(initialAssignment?.topicDescription ?: "") }
    var instructions by remember(initialAssignment?.id) { mutableStateOf(initialAssignment?.instructions ?: "") }
    var maxMarksText by remember(initialAssignment?.id) { mutableStateOf((initialAssignment?.maxMarks ?: 20).toString()) }
    var isFinalProject by remember(initialAssignment?.id) { mutableStateOf(initialAssignment?.isFinalProject ?: false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NavySurface.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (initialAssignment == null) "Create Module Assignment" else "Edit Assignment",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = NavyPrimary
            )
            if (scopedModules.size > 1 && initialAssignment == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    scopedModules.take(4).forEach { mod ->
                        val selected = selectedModuleId == mod.id
                        Surface(
                            onClick = { selectedModuleId = mod.id },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) NavyPrimary else Color.White
                        ) {
                            Text(
                                text = "M${mod.number}",
                                color = if (selected) GoldMetallic else NavyDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Assignment Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Topic / Case Study Summary") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Submission Instructions") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = maxMarksText,
                    onValueChange = { maxMarksText = it.filter { c -> c.isDigit() } },
                    label = { Text("Max Marks") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (title.isBlank()) return@Button
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
                            topicDescription = topic.trim(),
                            instructions = instructions.trim().ifBlank { "Submit concise supply chain analysis report." },
                            maxMarks = (maxMarksText.toIntOrNull() ?: 20).coerceIn(5, 200),
                            isFinalProject = isFinalProject,
                            isRequiredForCertification = isFinalProject || initialAssignment?.isRequiredForCertification == true
                        )
                        onSave(assignment)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_teacher_save_assignment")
                ) {
                    Text("Save Assignment", color = GoldMetallic, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatTeacherDate(ts: Long): String {
    if (ts <= 0L) return "Recent"
    return try {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(ts))
    } catch (_: Exception) {
        "Recent"
    }
}
