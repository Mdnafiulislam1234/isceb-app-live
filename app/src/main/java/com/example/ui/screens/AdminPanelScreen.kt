package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccountStatus
import com.example.model.AdminCompletionSettings
import com.example.model.AssignmentSubmission
import com.example.model.AuditLogEntry
import com.example.model.CertificateRecord
import com.example.model.CertificateStatus
import com.example.model.CourseModule
import com.example.model.UserProfile
import com.example.model.UserRole
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.ui.icons.AdminPanelSettings
import com.example.ui.icons.Block
import com.example.ui.icons.Download
import com.example.ui.icons.EmojiEvents
import com.example.ui.icons.Groups
import com.example.ui.icons.OpenInNew
import com.example.ui.icons.School
import com.example.ui.icons.Verified
import com.example.ui.theme.BgLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPanelScreen(
    currentUser: UserProfile,
    isAdmin: Boolean,
    modules: List<CourseModule>,
    registeredAccounts: List<UserProfile>,
    issuedCertificates: List<CertificateRecord>,
    assignmentSubmissions: List<AssignmentSubmission>,
    auditLogs: List<AuditLogEntry>,
    adminSettings: AdminCompletionSettings,
    onBack: () -> Unit,
    onOpenAdminSettings: () -> Unit,
    onOpenTeacherPanel: () -> Unit,
    onOpenVerification: () -> Unit,
    onCreateOrUpdateTeacher: (
        fullName: String,
        email: String,
        designation: String,
        organization: String,
        assignedModuleIds: Set<String>
    ) -> Unit,
    onUpdateUserAccess: (
        userId: String,
        role: UserRole,
        accountStatus: AccountStatus,
        isVerified: Boolean,
        assignedModuleIds: Set<String>
    ) -> Unit,
    onActivateTeacherSession: (userId: String) -> Unit,
    onReissueCertificate: (studentId: String, reason: String) -> Unit,
    onRevokeCertificate: (CertificateRecord) -> Unit,
    onReinstateCertificate: (certId: String) -> Unit,
    onOpenCertificatePdf: (CertificateRecord) -> Unit,
    onToggleCertificatesEnabled: (Boolean) -> Unit = {},
    onToggleDemoCertificateEnabled: (Boolean) -> Unit = {},
    onPreviewDemoCertificate: () -> Unit = {},
    onDownloadDemoCertificatePdf: () -> Unit = {},
    onDownloadCertificatePdf: (CertificateRecord) -> Unit = onOpenCertificatePdf,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Users", "Certificates", "Audit")

    var showTeacherForm by remember { mutableStateOf(false) }
    var teacherName by remember { mutableStateOf("") }
    var teacherEmail by remember { mutableStateOf("") }
    var teacherDesignation by remember { mutableStateOf("Senior SCM Faculty") }
    var teacherOrg by remember { mutableStateOf("ISCEB Faculty") }
    var selectedTeacherModules by remember { mutableStateOf(setOf<String>()) }

    var editingUserId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight)
            .testTag("screen_admin_panel"),
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
                                .testTag("btn_admin_panel_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back from Admin Panel",
                                tint = GoldMetallic
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ISCEB Admin Control Panel",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (isAdmin) "${currentUser.fullName} • Administrator Session" else "Restricted Portal",
                                color = GoldSoft,
                                fontSize = 12.sp
                            )
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = NavySecondary) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Control",
                                tint = GoldMetallic,
                                modifier = Modifier.padding(8.dp).size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!isAdmin) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("card_admin_panel_denied"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Admin Auth Required", tint = DangerRed, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Administrator Authentication Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NavyDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please sign in as an authorized ISCEB Administrator before accessing user governance, certificate controls, or audit logs.",
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
                            .testTag("tab_admin_${label.lowercase()}"),
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
                // Overview Dashboard
                item {
                    val studentCount = registeredAccounts.count { it.role == UserRole.STUDENT }
                    val teacherCount = registeredAccounts.count { it.role == UserRole.TEACHER }
                    val pendingGrades = assignmentSubmissions.count { !it.isGraded }
                    val validCerts = issuedCertificates.count { it.status == CertificateStatus.VALID }

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminMetricTile("Registered Students", "$studentCount", Modifier.weight(1f))
                            AdminMetricTile("Faculty Teachers", "$teacherCount", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminMetricTile("Pending Grading", "$pendingGrades", Modifier.weight(1f))
                            AdminMetricTile("Valid Certificates", "$validCerts", Modifier.weight(1f))
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
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Quick Governance Actions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NavyDark
                            )
                            Text(
                                text = "Rules: Min ${adminSettings.minLearningTimeSeconds}s study • Quiz ≥${adminSettings.quizPassingScore}% • ${adminSettings.requiredModuleIds.size} required modules.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                            Button(
                                onClick = onOpenAdminSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_admin_open_settings")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Curriculum Settings", tint = GoldMetallic)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Completion Rules & Live Cloud Publish", color = GoldMetallic, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onOpenTeacherPanel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_admin_open_teacher_view")
                            ) {
                                Icon(Icons.Default.School, contentDescription = "Teacher Workspace", tint = NavyPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Faculty Grading & Module Controls", color = NavyPrimary, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onOpenVerification,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_admin_open_verification")
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = "Verification Registry", tint = GoldDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Public Certificate Verification Registry", color = NavyDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            1 -> {
                // Users & Teachers Management
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Groups, contentDescription = "Faculty Setup", tint = NavyPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Add / Assign Faculty Teacher",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NavyDark
                                    )
                                }
                                TextButton(
                                    onClick = { showTeacherForm = !showTeacherForm },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_admin_toggle_teacher_form")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Toggle Teacher Form", tint = GoldDark)
                                    Text(if (showTeacherForm) "Close" else "Assign", color = GoldDark, fontWeight = FontWeight.Bold)
                                }
                            }

                            AnimatedVisibility(visible = showTeacherForm) {
                                Column(
                                    modifier = Modifier.padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = teacherName,
                                        onValueChange = { teacherName = it },
                                        label = { Text("Teacher Full Name *") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                                        modifier = Modifier.fillMaxWidth().testTag("input_admin_teacher_name")
                                    )
                                    OutlinedTextField(
                                        value = teacherEmail,
                                        onValueChange = { teacherEmail = it },
                                        label = { Text("Teacher Email *") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                                        modifier = Modifier.fillMaxWidth().testTag("input_admin_teacher_email")
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = teacherDesignation,
                                            onValueChange = { teacherDesignation = it },
                                            label = { Text("Designation") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = teacherOrg,
                                            onValueChange = { teacherOrg = it },
                                            label = { Text("Organization") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Text(
                                        text = "Assign Module Permissions (${selectedTeacherModules.size} selected)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = NavyDark
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        modules.sortedBy { it.number }.forEach { mod ->
                                            val checked = selectedTeacherModules.contains(mod.id)
                                            FilterChip(
                                                selected = checked,
                                                onClick = {
                                                    selectedTeacherModules = if (checked) {
                                                        selectedTeacherModules - mod.id
                                                    } else {
                                                        selectedTeacherModules + mod.id
                                                    }
                                                },
                                                label = { Text("M${mod.number}", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = NavyPrimary,
                                                    selectedLabelColor = GoldMetallic
                                                ),
                                                modifier = Modifier.testTag("chip_teacher_mod_${mod.id}")
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onCreateOrUpdateTeacher(
                                                teacherName.trim(),
                                                teacherEmail.trim(),
                                                teacherDesignation.trim(),
                                                teacherOrg.trim(),
                                                selectedTeacherModules
                                            )
                                            if (teacherName.isNotBlank() && teacherEmail.isNotBlank()) {
                                                showTeacherForm = false
                                                teacherName = ""
                                                teacherEmail = ""
                                                selectedTeacherModules = emptySet()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_admin_save_teacher")
                                    ) {
                                        Text("Save & Authorize Teacher", color = GoldMetallic, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Registered Users & Staff Directory (${registeredAccounts.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NavyDark,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (registeredAccounts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = "No student or teacher accounts registered yet. Students sign up on Profile, or you can authorize a Teacher above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(registeredAccounts, key = { it.id }) { account ->
                        val isEditing = editingUserId == account.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("card_admin_user_${account.id}"),
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
                                            text = account.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NavyDark
                                        )
                                        Text(
                                            text = "${account.email} • ${account.studentId.ifBlank { account.id }}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (account.accountStatus == AccountStatus.SUSPENDED) Color(0xFFFEE2E2) else GoldSoft
                                    ) {
                                        Text(
                                            text = "${account.role.name} • ${account.accountStatus.displayLabel}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (account.accountStatus == AccountStatus.SUSPENDED) DangerRed else GoldDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (account.role == UserRole.TEACHER) {
                                    Text(
                                        text = "Assigned Modules: ${account.assignedModuleIds.sorted().joinToString(", ").ifBlank { "None" }}",
                                        fontSize = 11.sp,
                                        color = NavyPrimary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { editingUserId = if (isEditing) null else account.id },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_admin_edit_user_${account.id}")
                                    ) {
                                        Text(if (isEditing) "Close" else "Manage Access", fontSize = 11.sp, color = NavyPrimary)
                                    }
                                    if (account.role == UserRole.TEACHER && account.accountStatus == AccountStatus.ACTIVE) {
                                        TextButton(
                                            onClick = { onActivateTeacherSession(account.id) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Preview as Teacher", fontSize = 11.sp, color = GoldDark, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (isEditing) {
                                    var draftRole by remember(account.id) { mutableStateOf(account.role) }
                                    var draftStatus by remember(account.id) { mutableStateOf(account.accountStatus) }
                                    var draftVerified by remember(account.id) { mutableStateOf(account.isVerified) }
                                    var draftMods by remember(account.id) { mutableStateOf(account.assignedModuleIds) }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Role", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = NavyDark)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(UserRole.STUDENT, UserRole.TEACHER).forEach { r ->
                                            FilterChip(
                                                selected = draftRole == r,
                                                onClick = { draftRole = r },
                                                label = { Text(r.name, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                    Text("Account Status", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = NavyDark)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        AccountStatus.entries.forEach { st ->
                                            FilterChip(
                                                selected = draftStatus == st,
                                                onClick = { draftStatus = st },
                                                label = { Text(st.displayLabel, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = draftVerified,
                                            onCheckedChange = { draftVerified = it },
                                            colors = CheckboxDefaults.colors(checkedColor = NavyPrimary)
                                        )
                                        Text("Verified ISCEB Account", fontSize = 12.sp, color = NavyDark)
                                    }
                                    if (draftRole == UserRole.TEACHER) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            modules.sortedBy { it.number }.forEach { mod ->
                                                val assigned = draftMods.contains(mod.id)
                                                FilterChip(
                                                    selected = assigned,
                                                    onClick = {
                                                        draftMods = if (assigned) draftMods - mod.id else draftMods + mod.id
                                                    },
                                                    label = { Text("M${mod.number}", fontSize = 10.sp) }
                                                )
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            onUpdateUserAccess(account.id, draftRole, draftStatus, draftVerified, draftMods)
                                            editingUserId = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_admin_save_user_${account.id}")
                                    ) {
                                        Text("Apply Role & Status", color = GoldMetallic, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Demo Certificate & Certificate Governance Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("card_admin_demo_certificate"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = "Demo Certificate", tint = GoldDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Demo Certificate & Diploma Controls",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NavyDark
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Enable Official Certificates", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = NavyPrimary)
                                    Text("Allow eligible learners to claim official PGD diplomas", fontSize = 10.5.sp, color = Color(0xFF64748B))
                                }
                                Switch(
                                    checked = adminSettings.certificatesEnabled,
                                    onCheckedChange = onToggleCertificatesEnabled,
                                    modifier = Modifier.testTag("switch_admin_certificates_enabled"),
                                    colors = SwitchDefaults.colors(checkedThumbColor = GoldMetallic, checkedTrackColor = NavyPrimary)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Enable Demo Certificate Preview", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = NavyPrimary)
                                    Text("Show sample PGD certificate preview & PDF download in Learning", fontSize = 10.5.sp, color = Color(0xFF64748B))
                                }
                                Switch(
                                    checked = adminSettings.demoCertificateEnabled,
                                    onCheckedChange = onToggleDemoCertificateEnabled,
                                    modifier = Modifier.testTag("switch_admin_demo_cert_enabled"),
                                    colors = SwitchDefaults.colors(checkedThumbColor = GoldMetallic, checkedTrackColor = NavyPrimary)
                                )
                            }

                            Text(
                                text = "Signatories: Left = ${adminSettings.leftSignerName} • Right = ${adminSettings.authorizedSignerName} (${adminSettings.authorizedSignerTitle})",
                                fontSize = 11.sp,
                                color = Color(0xFF475569)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = onPreviewDemoCertificate,
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("btn_admin_preview_demo_cert")
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = GoldDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Preview Demo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                }
                                Button(
                                    onClick = onDownloadDemoCertificatePdf,
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("btn_admin_download_demo_cert"),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download Demo PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Certificates Control
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = "Certificates", tint = GoldDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Issue / Re-issue Diploma by Student",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NavyDark
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val studentAccounts = registeredAccounts.filter { it.role == UserRole.STUDENT }
                            if (studentAccounts.isEmpty()) {
                                Text(
                                    text = "No enrolled student accounts found yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            } else {
                                studentAccounts.forEach { st ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(st.fullName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NavyDark)
                                            Text(st.studentId.ifBlank { st.email }, fontSize = 11.sp, color = Color(0xFF64748B))
                                        }
                                        OutlinedButton(
                                            onClick = { onReissueCertificate(st.id, "Administrative Diploma Issuance") },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_admin_reissue_${st.id}")
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Issue Certificate", tint = NavyPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Re-issue", fontSize = 11.sp, color = NavyPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Issued Certificates Registry (${issuedCertificates.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NavyDark,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (issuedCertificates.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = "No ISCEB certificates issued yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(issuedCertificates, key = { it.certificateId }) { cert ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("card_admin_cert_${cert.certificateId}"),
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
                                            text = cert.studentName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NavyDark
                                        )
                                        Text(
                                            text = cert.certificateId,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = GoldDark
                                        )
                                        Text(
                                            text = "Issued ${cert.issueDate} • ${cert.completedClassesCount}/${cert.totalRequiredClasses} lectures",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (cert.status == CertificateStatus.VALID) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                    ) {
                                        Text(
                                            text = cert.status.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cert.status == CertificateStatus.VALID) SuccessGreen else DangerRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                if (cert.status == CertificateStatus.REVOKED && !cert.revocationReason.isNullOrBlank()) {
                                    Text(
                                        text = "Reason: ${cert.revocationReason}",
                                        fontSize = 11.sp,
                                        color = DangerRed,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onDownloadCertificatePdf(cert) },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Download PDF", tint = NavyPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save PDF", fontSize = 11.sp, color = NavyPrimary)
                                    }
                                    OutlinedButton(
                                        onClick = { onOpenCertificatePdf(cert) },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = "Open PDF", tint = NavyPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open", fontSize = 11.sp, color = NavyPrimary)
                                    }
                                    if (cert.status == CertificateStatus.VALID) {
                                        OutlinedButton(
                                            onClick = { onRevokeCertificate(cert) },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_admin_revoke_${cert.certificateId}")
                                        ) {
                                            Icon(Icons.Default.Block, contentDescription = "Revoke Certificate", tint = DangerRed, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Revoke", fontSize = 11.sp, color = DangerRed)
                                        }
                                    } else {
                                        Button(
                                            onClick = { onReinstateCertificate(cert.certificateId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_admin_reinstate_${cert.certificateId}")
                                        ) {
                                            Text("Reinstate", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Audit Logs
                item {
                    Text(
                        text = "Administrative Audit Trail (${auditLogs.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NavyDark,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (auditLogs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = "No administrative or faculty actions logged in this workspace yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(auditLogs.sortedByDescending { it.timestampMs }, key = { it.logId }) { log ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("card_audit_${log.logId}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.actionType,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = GoldDark
                                    )
                                    Text(
                                        text = formatAdminTimestamp(log.timestampMs),
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.summary,
                                    fontSize = 12.sp,
                                    color = NavyDark,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Actor: ${log.actorName} (${log.actorRole.name})",
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = NavyPrimary
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF475569),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatAdminTimestamp(ts: Long): String {
    if (ts <= 0L) return "Now"
    return try {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(ts))
    } catch (_: Exception) {
        "Now"
    }
}
