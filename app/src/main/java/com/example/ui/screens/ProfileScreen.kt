package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AccountStatus
import com.example.model.ApkDownloadState
import com.example.model.AppUpdateConfig
import com.example.model.AppUpdateInfo
import com.example.model.AssignmentSubmission
import com.example.model.CertificateEligibility
import com.example.model.CertificateRecord
import com.example.model.CertificationEligibilityStatus
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.DeveloperProfile
import com.example.model.ExamAttempt
import com.example.model.FounderProfile
import com.example.model.ModuleAssignment
import com.example.model.ModuleExam
import com.example.model.StudentClassProgress
import com.example.model.SubmissionStatus
import com.example.model.UserProfile
import com.example.model.UserProgress
import com.example.model.UserRole
import com.example.ui.components.CertificatePreviewCard
import com.example.ui.icons.AdminPanelSettings
import com.example.ui.icons.Assignment
import com.example.ui.icons.ChevronRight
import com.example.ui.icons.Cloud
import com.example.ui.icons.CloudDone
import com.example.ui.icons.CloudOff
import com.example.ui.icons.Download
import com.example.ui.icons.EmojiEvents
import com.example.ui.icons.ErrorOutline
import com.example.ui.icons.Groups
import com.example.ui.icons.InstallMobile
import com.example.ui.icons.OpenInBrowser
import com.example.ui.icons.OpenInNew
import com.example.ui.icons.PhotoCamera
import com.example.ui.icons.PlayCircle
import com.example.ui.icons.Quiz
import com.example.ui.icons.School
import com.example.ui.icons.Security
import com.example.ui.icons.SystemUpdate
import com.example.ui.icons.Verified
import com.example.ui.theme.BgLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SuccessGreen
import com.example.util.ApkUpdateManager
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    currentUser: UserProfile,
    availableUsers: List<UserProfile>,
    isAdmin: Boolean,
    cloudAuthEmail: String? = null,
    appUpdateInfo: AppUpdateInfo = AppUpdateInfo(),
    apkDownloadState: ApkDownloadState = ApkDownloadState.Idle,
    onSwitchUser: (String) -> Unit,
    onAdminLoginClick: () -> Unit,
    onAdminLogoutClick: () -> Unit,
    onOpenVerification: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFirebaseGuide: () -> Unit,
    onResetDemoData: () -> Unit,
    onOpenPhoneInstall: () -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    onDownloadLatestApk: () -> Unit = {},
    onCancelApkDownload: () -> Unit = {},
    onInstallDownloadedApk: () -> Unit = {},
    onOpenWebDownloadPage: (String) -> Unit = {},
    onOpenDeveloperProfile: () -> Unit = {},
    onOpenFounderProfile: () -> Unit = {},
    onOpenMembers: () -> Unit = {},
    founderProfile: FounderProfile = FounderProfile(),
    developerProfile: DeveloperProfile = DeveloperProfile(),
    membersCount: Int = 1,
    onConnectGoogleCloudClick: () -> Unit = {},
    onDisconnectGoogleCloudClick: () -> Unit = {},
    onExportBackupJson: () -> String = { "{}" },
    onImportBackupJson: (String) -> Result<Int> = { Result.success(0) },
    onBackupMessage: (String) -> Unit = {},
    // LMS Upgrade parameters
    registeredAccounts: List<UserProfile> = emptyList(),
    isTeacher: Boolean = false,
    modules: List<CourseModule> = emptyList(),
    classes: List<CourseClass> = emptyList(),
    studentProgressMap: Map<String, StudentClassProgress> = emptyMap(),
    userProgress: UserProgress = UserProgress(),
    moduleExams: List<ModuleExam> = emptyList(),
    examAttempts: List<ExamAttempt> = emptyList(),
    moduleAssignments: List<ModuleAssignment> = emptyList(),
    assignmentSubmissions: List<AssignmentSubmission> = emptyList(),
    eligibility: CertificateEligibility = CertificateEligibility(false, 0, 0),
    issuedCertificates: List<CertificateRecord> = emptyList(),
    onRegisterOrSignInStudent: (
        fullName: String,
        email: String,
        org: String,
        designation: String,
        mobile: String,
        program: String,
        photoUri: Uri?
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onUpdateStudentProfile: (
        fullName: String,
        org: String,
        designation: String,
        mobile: String,
        program: String,
        photoUri: Uri?
    ) -> Unit = { _, _, _, _, _, _ -> },
    onSignOutAccount: () -> Unit = {},
    onActivateTeacher: (String) -> Unit = {},
    onOpenTeacherPanel: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    onModuleClick: (CourseModule) -> Unit = {},
    onClassClick: (CourseClass) -> Unit = {},
    onTakeExam: (ModuleExam) -> Unit = {},
    onOpenAssignment: (ModuleAssignment) -> Unit = {},
    onGenerateCertificate: () -> Unit = {},
    onOpenCertificatePdf: (CertificateRecord) -> Unit = {},
    onVerifyCertificate: (String) -> Unit = {},
    onPreviewDemoCertificate: () -> Unit = {},
    onDownloadCertificatePdf: (CertificateRecord) -> Unit = onOpenCertificatePdf,
    onDownloadDemoCertificatePdf: () -> Unit = {},
    demoCertificateEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isGuest = currentUser.id == "guest-unauthenticated"
    val teacherAccounts = remember(registeredAccounts, availableUsers) {
        (registeredAccounts + availableUsers)
            .distinctBy { it.id }
            .filter { it.role == UserRole.TEACHER && it.accountStatus == AccountStatus.ACTIVE }
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSecurityInfoDialog by remember { mutableStateOf(false) }
    var showAllModulesInDashboard by remember { mutableStateOf(false) }

    // Student photo picker state for registration & editing
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }
    var regOrg by remember { mutableStateOf("") }
    var regProgram by remember { mutableStateOf("Post Graduate Diploma (PGD) in Supply Chain Management (SCM)") }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val studentPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            if (!isGuest && !isAdmin) {
                onUpdateStudentProfile(
                    currentUser.fullName,
                    currentUser.organization,
                    currentUser.designation,
                    currentUser.mobileNumber,
                    currentUser.enrolledProgram,
                    uri
                )
            } else {
                pendingPhotoUri = uri
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val payload = onExportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                }
                onBackupMessage("Backup saved! Keep this .json file safe before uninstalling or switching devices.")
            } catch (e: Exception) {
                onBackupMessage("Backup export failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    onImportBackupJson(text)
                } else {
                    onBackupMessage("Selected backup file is empty.")
                }
            } catch (e: Exception) {
                onBackupMessage("Backup restore failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    // Derived LMS Student Metrics (100% genuine user data, zero fake stats)
    val publishedClasses = remember(classes) { classes.filter { it.isPublished } }
    val completedCount = publishedClasses.count { it.isCompleted || studentProgressMap[it.id]?.isCompleted == true }
    val totalClassesCount = publishedClasses.size.coerceAtLeast(1)
    val progressPct = ((completedCount.toFloat() / totalClassesCount.toFloat()) * 100f).toInt()
    val myExamAttempts = remember(examAttempts, currentUser.id) { examAttempts.filter { it.studentId == currentUser.id } }
    val passedExamsCount = remember(moduleExams, myExamAttempts) {
        moduleExams.count { ex -> myExamAttempts.any { it.examId == ex.id && it.isPassed } }
    }
    val mySubmissions = remember(assignmentSubmissions, currentUser.id) { assignmentSubmissions.filter { it.studentId == currentUser.id } }
    val submittedOrGradedCount = mySubmissions.size
    val myCertificates = remember(issuedCertificates, currentUser.id) {
        issuedCertificates.filter { it.studentId == currentUser.id }
    }
    val continueClass = remember(publishedClasses, studentProgressMap) {
        val lastActive = publishedClasses
            .filter { !it.isCompleted && (studentProgressMap[it.id]?.lastActiveTimeMs ?: 0L) > 0L }
            .maxByOrNull { studentProgressMap[it.id]?.lastActiveTimeMs ?: 0L }
        lastActive ?: publishedClasses.firstOrNull { !it.isCompleted } ?: publishedClasses.firstOrNull()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Official ISCEB Brand Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_isceb_logo),
                        contentDescription = "ISCEB - Connect. learn. Achieve.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(82.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ISCEB • Student Learning & Credentialing Portal",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldDark,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        // A. STUDENT IDENTITY CARD (Or Genuine Sign-In / Registration Portal when Guest)
        item {
            if (isGuest) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("card_guest_signin_portal"),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Student Account Sign-In & Enrollment",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.White)
                        )
                        Text(
                            text = "Sign in with Google or register your personal PGD student account below to save lessons, assessments, and verifiable ISCEB diplomas under your own Student ID.",
                            fontSize = 11.5.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = onConnectGoogleCloudClick,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_student_google_signin")
                        ) {
                            Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Continue with Google Account Sync", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Direct Student Registration",
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    fontSize = 13.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEFF3F8))
                                            .clickable {
                                                studentPhotoPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (pendingPhotoUri != null) {
                                            AsyncImage(
                                                model = pendingPhotoUri,
                                                contentDescription = "Selected Student Photo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Pick Photo", tint = NavyPrimary)
                                        }
                                    }
                                    TextButton(onClick = {
                                        studentPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }) {
                                        Text(if (pendingPhotoUri == null) "Upload Profile Photo" else "Change Selected Photo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                OutlinedTextField(
                                    value = regName,
                                    onValueChange = { regName = it },
                                    label = { Text("Full Name (printed on diploma)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_student_reg_name")
                                )
                                OutlinedTextField(
                                    value = regEmail,
                                    onValueChange = { regEmail = it },
                                    label = { Text("Registered Email Address") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_student_reg_email")
                                )
                                OutlinedTextField(
                                    value = regMobile,
                                    onValueChange = { regMobile = it },
                                    label = { Text("Mobile Number (optional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = regOrg,
                                    onValueChange = { regOrg = it },
                                    label = { Text("Company / Organization (optional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = regProgram,
                                    onValueChange = { regProgram = it },
                                    label = { Text("Enrolled Course / PGD Program") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        onRegisterOrSignInStudent(
                                            regName,
                                            regEmail,
                                            regOrg.ifBlank { "ISCEB Enrolled Professional" },
                                            "PGD Candidate",
                                            regMobile,
                                            regProgram,
                                            pendingPhotoUri
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_student_register_signin")
                                ) {
                                    Text("Create / Sign In Student Account", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("card_student_identity"),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isAdmin || currentUser.role == UserRole.ADMIN) {
                            val isFounderAccount = currentUser.id == "admin-2"
                            val customFile = (if (isFounderAccount) founderProfile.photoUri else developerProfile.photoUri)
                                .takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
                            val fallbackRes = if (isFounderAccount) R.drawable.img_admin_amirul else R.drawable.img_admin_nafiul
                            val photoDesc = "${currentUser.fullName} - ${currentUser.designation}"
                            Box(
                                modifier = Modifier
                                    .size(78.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, GoldMetallic, CircleShape)
                                    .background(Color.White)
                                    .testTag("img_admin_profile_photo"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (customFile != null) {
                                    AsyncImage(
                                        model = customFile,
                                        contentDescription = photoDesc,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, -0.35f)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = fallbackRes),
                                        contentDescription = photoDesc,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, -0.35f)
                                    )
                                }
                            }
                        } else {
                            val photoFile = currentUser.photoUrl.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, GoldMetallic, CircleShape)
                                    .background(GoldMetallic)
                                    .clickable {
                                        studentPhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("img_student_profile_photo"),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    photoFile != null -> AsyncImage(
                                        model = photoFile,
                                        contentDescription = "${currentUser.fullName} Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    currentUser.photoUrl.startsWith("http", ignoreCase = true) -> AsyncImage(
                                        model = currentUser.photoUrl,
                                        contentDescription = "${currentUser.fullName} Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    else -> Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "User Avatar",
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = currentUser.fullName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        if (currentUser.studentId.isNotBlank() && currentUser.role == UserRole.STUDENT) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Student ID: ${currentUser.studentId}",
                                color = GoldMetallic,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentUser.enrolledProgram.ifBlank { currentUser.designation.ifBlank { currentUser.organization } },
                            style = MaterialTheme.typography.bodySmall.copy(color = GoldSoft, fontSize = 11.sp)
                        )
                        Text(
                            text = buildString {
                                append(currentUser.email)
                                if (currentUser.mobileNumber.isNotBlank()) append(" • ${currentUser.mobileNumber}")
                            },
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1), fontSize = 10.5.sp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NavySurface)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            val badgeText = when {
                                isAdmin -> "ADMINISTRATOR (GOVERNANCE & PUBLISHING)"
                                isTeacher || currentUser.role == UserRole.TEACHER -> "VERIFIED FACULTY (${currentUser.assignedModuleIds.size} MODULES)"
                                currentUser.accountStatus == AccountStatus.SUSPENDED -> "ACCOUNT SUSPENDED"
                                currentUser.isVerified -> "VERIFIED STUDENT • ACTIVE"
                                else -> "STUDENT ACCOUNT • ${currentUser.accountStatus.displayLabel.uppercase()}"
                            }
                            Text(
                                text = badgeText,
                                color = if (currentUser.accountStatus == AccountStatus.SUSPENDED) DangerRed else GoldMetallic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        if (!isAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showEditProfileDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_edit_student_profile")
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { showSecurityInfoDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldSoft),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Security", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onSignOutAccount,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = DangerRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_student_signout")
                                ) {
                                    Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // B. LEARNING PROGRESS SUMMARY (4 KPI Cards)
        if (!isGuest) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Learning Progress Summary",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardMetricCard(
                            title = "Lessons Completed",
                            value = "$completedCount / ${publishedClasses.size}",
                            subtitle = "Verified modules",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetricCard(
                            title = "Course Progress",
                            value = "$progressPct%",
                            subtitle = "${userProgress.totalActiveLearningSeconds / 60}m active study",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardMetricCard(
                            title = "Assignments & Exams",
                            value = "$passedExamsCount/${moduleExams.size.coerceAtLeast(1)} Exams",
                            subtitle = "$submittedOrGradedCount/${moduleAssignments.size} Assignments submitted",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetricCard(
                            title = "Certificates Earned",
                            value = "${myCertificates.size}",
                            subtitle = eligibility.status.displayLabel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // C. CONTINUE LEARNING CARD
            if (continueClass != null) {
                item {
                    val prog = studentProgressMap[continueClass.id]
                    val mod = modules.find { it.id == continueClass.moduleId }
                    val resumeHint = buildString {
                        if ((prog?.lastPdfPage ?: 0) > 0) append("PDF Page ${prog?.lastPdfPage}")
                        if ((prog?.lastPlaybackPositionMs ?: 0L) > 0L) {
                            if (isNotEmpty()) append(" • ")
                            append("Media ${(prog?.lastPlaybackPositionMs ?: 0L) / 1000L}s")
                        }
                        if (isEmpty()) append("Ready to study (${continueClass.durationMinutes} min)")
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, GoldMetallic),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(NavyPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Continue Learning", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldDark)
                                    Text(
                                        text = "${mod?.let { "Mod ${it.number} • " } ?: ""}${continueClass.title}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = NavyPrimary
                                    )
                                    Text(resumeHint, fontSize = 10.5.sp, color = Color(0xFF64748B))
                                }
                            }
                            Button(
                                onClick = { onClassClick(continueClass) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // D. MY COURSES & MODULES PROGRESS
            if (modules.isNotEmpty()) {
                item {
                    val displayedModules = if (showAllModulesInDashboard) modules else modules.take(4)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "My PGD-SCM Modules (${modules.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                                )
                                TextButton(onClick = { showAllModulesInDashboard = !showAllModulesInDashboard }) {
                                    Text(if (showAllModulesInDashboard) "Collapse" else "View All ${modules.size}", fontSize = 11.sp)
                                }
                            }
                            displayedModules.forEach { mod ->
                                val modCls = publishedClasses.filter { it.moduleId == mod.id }
                                val modDone = modCls.count { it.isCompleted || studentProgressMap[it.id]?.isCompleted == true }
                                val ratio = if (modCls.isNotEmpty()) modDone.toFloat() / modCls.size else 0f
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BgLight)
                                        .clickable { onModuleClick(mod) }
                                        .padding(10.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            text = "Mod ${mod.number}: ${mod.title}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = NavyPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("$modDone/${modCls.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldDark)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                        color = if (ratio >= 1f) SuccessGreen else GoldMetallic,
                                        trackColor = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // E. ASSIGNMENTS & EXAMS HUB
            if (moduleExams.isNotEmpty() || moduleAssignments.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Assignments & Examinations Hub",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                            )
                            moduleExams.take(5).forEach { exam ->
                                val best = myExamAttempts.filter { it.examId == exam.id }.maxByOrNull { it.scoreMarks }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BgLight)
                                        .clickable { onTakeExam(exam) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(imageVector = Icons.Default.Quiz, contentDescription = null, tint = GoldDark, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(exam.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                                            Text(
                                                text = if (best != null) {
                                                    "Best: ${best.scoreMarks}/${best.totalMarks} (${best.percentage}%) • ${if (best.isPassed) "PASSED" else "RETRY"}"
                                                } else {
                                                    "Pending • ${exam.totalMarks} Marks"
                                                },
                                                fontSize = 10.5.sp,
                                                color = if (best?.isPassed == true) SuccessGreen else Color(0xFF64748B)
                                            )
                                        }
                                    }
                                    Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                }
                            }
                            moduleAssignments.take(4).forEach { asg ->
                                val sub = mySubmissions.find { it.assignmentId == asg.id }
                                val statusLabel = sub?.status?.displayLabel ?: SubmissionStatus.PENDING.displayLabel
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BgLight)
                                        .clickable { onOpenAssignment(asg) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(imageVector = Icons.Default.Assignment, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(asg.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                                            Text(
                                                text = if (sub?.isGraded == true) "Graded: ${sub.marksAwarded}/${asg.maxMarks}M" else "Status: $statusLabel",
                                                fontSize = 10.5.sp,
                                                color = if (sub?.isGraded == true) SuccessGreen else GoldDark
                                            )
                                        }
                                    }
                                    Text("View", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // F. 5-STATE CERTIFICATION ELIGIBILITY TRACKER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("card_profile_cert_eligibility"),
                    colors = CardDefaults.cardColors(containerColor = if (eligibility.isEligible) Color(0xFFFFFBEB) else Color.White),
                    border = BorderStroke(1.dp, if (eligibility.isEligible) GoldMetallic else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = GoldDark, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Certification Status",
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (eligibility.status == CertificationEligibilityStatus.ELIGIBLE_FOR_CERTIFICATION ||
                                            eligibility.status == CertificationEligibilityStatus.CERTIFICATE_ISSUED
                                        ) Color(0xFFDCFCE7) else Color(0xFFEFF6FF)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = eligibility.status.displayLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = if (eligibility.isEligible || eligibility.existingCertificate != null) SuccessGreen else NavyPrimary,
                                    modifier = Modifier.testTag("badge_profile_cert_status")
                                )
                            }
                        }
                        Text(
                            text = "Lessons: ${eligibility.completedRequiredCount}/${eligibility.totalRequiredCount} • Assessments: ${if (eligibility.mandatoryExamsPassed && eligibility.allQuizzesPassed) "Passed" else "Pending"} • Capstone: ${if (eligibility.finalProjectSubmitted) "Satisfied" else "Pending"}",
                            fontSize = 11.5.sp,
                            color = Color(0xFF334155)
                        )
                        if (eligibility.remainingRequirements.isNotEmpty() && eligibility.existingCertificate == null) {
                            eligibility.remainingRequirements.take(4).forEach { req ->
                                Text("• $req", fontSize = 10.5.sp, color = DangerRed)
                            }
                        }
                        if (eligibility.existingCertificate == null && eligibility.isEligible) {
                            Button(
                                onClick = onGenerateCertificate,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Generate Official Certificate & QR PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // G. MY CERTIFICATES
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("card_profile_my_certificates"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "My Issued Certificates (${myCertificates.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                        )
                        if (myCertificates.isEmpty()) {
                            Text(
                                text = "Complete all PGD curriculum requirements to automatically unlock your verifiable ISCEB diploma with QR code.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        } else {
                            myCertificates.forEach { cert ->
                                CertificatePreviewCard(record = cert)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { onDownloadCertificatePdf(cert) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onOpenCertificatePdf(cert) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open PDF", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { onVerifyCertificate(cert.certificateId) },
                                        modifier = Modifier.weight(0.8f)
                                    ) {
                                        Text("Verify", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        if (isAdmin || demoCertificateEnabled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = onPreviewDemoCertificate,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = GoldDark, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Preview Demo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onDownloadDemoCertificatePdf,
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Demo PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // H1. Google Cloud Auto-Sync Account Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("card_profile_cloud_sync"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (cloudAuthEmail != null) Color(0xFFDCFCE7) else Color(0xFFEFF3F8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (cloudAuthEmail != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (cloudAuthEmail != null) SuccessGreen else NavyPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google Cloud Auto-Sync",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                                )
                                Text(
                                    text = if (cloudAuthEmail != null) {
                                        "Connected: $cloudAuthEmail • App v${appUpdateInfo.currentAppVersionName} (${appUpdateInfo.currentAppVersionCode})"
                                    } else {
                                        "Sign in with Google for automatic live syllabus & progress sync across phones"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7F96), fontSize = 10.5.sp)
                                )
                            }
                        }

                        if (cloudAuthEmail != null) {
                            OutlinedButton(
                                onClick = onDisconnectGoogleCloudClick,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_profile_signout_cloud")
                            ) {
                                Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                            }
                        } else {
                            Button(
                                onClick = onConnectGoogleCloudClick,
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_profile_connect_google")
                            ) {
                                Text("Sign in with Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // H2. Role Portals (Teacher Panel / Admin Panel / Teacher Activation / Admin Auth)
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isTeacher || isAdmin) {
                    Button(
                        onClick = onOpenTeacherPanel,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_open_teacher_panel")
                    ) {
                        Icon(imageVector = Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Teacher Faculty Panel", fontWeight = FontWeight.Bold)
                    }
                }

                if (isAdmin) {
                    Button(
                        onClick = onOpenAdminPanel,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_open_admin_panel")
                    ) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Admin Governance Panel", fontWeight = FontWeight.Bold)
                    }
                }

                if (!isAdmin && !isTeacher && teacherAccounts.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Assigned Faculty Portal", fontWeight = FontWeight.Bold, color = NavyPrimary, fontSize = 13.sp)
                            teacherAccounts.forEach { teacher ->
                                FilterChip(
                                    selected = currentUser.id == teacher.id,
                                    onClick = { onActivateTeacher(teacher.id) },
                                    label = { Text("Activate Faculty: ${teacher.fullName} (${teacher.assignedModuleIds.size} mod)", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isAdmin) GoldMetallic else Color(0xFFEFF3F8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isAdmin) NavyPrimary else Color(0xFF6B7F96),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Administrator Management Mode",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                                    )
                                    Text(
                                        text = if (isAdmin) "Active: Full user, faculty, cert & curriculum governance" else "Protected with per-admin cryptographic authentication",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7F96), fontSize = 11.sp)
                                    )
                                }
                            }

                            if (isAdmin) {
                                Button(
                                    onClick = onAdminLogoutClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = DangerRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Logout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = onAdminLoginClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_admin_login_profile")
                                ) {
                                    Text("Login", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // H3. Dedicated In-App APK Update Center Card (Backup/Restore strictly Admin-Only!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("card_profile_app_update"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (appUpdateInfo.isBinaryUpdateRequired) GoldMetallic else NavyPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = if (appUpdateInfo.isBinaryUpdateRequired) NavyDark else GoldMetallic,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "App Updates & Online APK",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                                )
                                Text(
                                    text = "Installed: v${appUpdateInfo.currentAppVersionName} (${appUpdateInfo.currentAppVersionCode}) • Latest: v${appUpdateInfo.latestAvailableApkVersionName} (${appUpdateInfo.latestAvailableApkVersionCode})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (appUpdateInfo.isBinaryUpdateRequired) GoldDark else Color(0xFF6B7F96),
                                        fontSize = 10.5.sp,
                                        fontWeight = if (appUpdateInfo.isBinaryUpdateRequired) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                val portalUrl = appUpdateInfo.downloadPageUrl.ifBlank { AppUpdateConfig.DEFAULT_DOWNLOAD_PAGE_URL }
                                onOpenWebDownloadPage(portalUrl)
                            },
                            modifier = Modifier.testTag("btn_profile_open_download_page")
                        ) {
                            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Web Page", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }

                    if (appUpdateInfo.releaseNotes.isNotBlank()) {
                        Text(
                            text = "Release Notes: ${appUpdateInfo.releaseNotes}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 15.sp
                        )
                    }

                    when (val dlState = apkDownloadState) {
                        is ApkDownloadState.Checking -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NavyPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking update server...", fontSize = 11.sp, color = NavyPrimary)
                            }
                        }
                        is ApkDownloadState.Downloading -> {
                            val pct = if (dlState.progressFraction >= 0f) "${(dlState.progressFraction * 100).toInt()}%" else "..."
                            val bytesStr = if (dlState.totalBytes > 0L) {
                                "${ApkUpdateManager.formatByteSize(dlState.downloadedBytes)} / ${ApkUpdateManager.formatByteSize(dlState.totalBytes)}"
                            } else {
                                ApkUpdateManager.formatByteSize(dlState.downloadedBytes)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Downloading APK...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                Text("$bytesStr ($pct)", fontSize = 10.5.sp, color = GoldDark, fontWeight = FontWeight.Bold)
                            }
                            if (dlState.progressFraction >= 0f) {
                                LinearProgressIndicator(
                                    progress = { dlState.progressFraction },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = GoldMetallic,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = GoldMetallic,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }
                            TextButton(onClick = onCancelApkDownload, modifier = Modifier.align(Alignment.End)) {
                                Text("Cancel", color = DangerRed, fontSize = 11.sp)
                            }
                        }
                        is ApkDownloadState.Verifying -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = GoldDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying APK SHA-256 checksum...", fontSize = 11.sp, color = NavyPrimary)
                            }
                        }
                        is ApkDownloadState.ReadyToInstall -> {
                            Button(
                                onClick = onInstallDownloadedApk,
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_profile_install_apk")
                            ) {
                                Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Install Verified v${dlState.versionName}", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                        }
                        is ApkDownloadState.Error -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFFEF2F2)).padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dlState.message, fontSize = 10.5.sp, color = DangerRed)
                            }
                            if (dlState.canRetry) {
                                Button(
                                    onClick = onDownloadLatestApk,
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_profile_retry_apk")
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        is ApkDownloadState.Idle -> {
                            if (appUpdateInfo.lastCheckError != null) {
                                Text(text = appUpdateInfo.lastCheckError, fontSize = 10.sp, color = DangerRed)
                            }
                        }
                    }

                    if (apkDownloadState !is ApkDownloadState.Downloading &&
                        apkDownloadState !is ApkDownloadState.Verifying &&
                        apkDownloadState !is ApkDownloadState.ReadyToInstall
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onCheckUpdates,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_profile_check_updates")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check Updates", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                            Button(
                                onClick = onDownloadLatestApk,
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_profile_download_apk")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download Latest", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isAdmin) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { exportBackupLauncher.launch("ISCEB-backup-v${appUpdateInfo.currentAppVersionName}.json") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_profile_export_backup")
                            ) {
                                Text("Export Backup", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                            OutlinedButton(
                                onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_profile_import_backup")
                            ) {
                                Text("Restore Backup", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GoldDark)
                            }
                        }
                    }
                }
            }
        }

        // H4. Institutional Leadership, Registry & Settings
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Institutional Registry & Settings",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                )

                // ISCEB Leadership & Administration Panel Card
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_isceb_administration"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(NavyPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("ISCEB Leadership & Academic Board", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                                Text("Founders, Engineering & Faculty Directory", fontSize = 10.5.sp, color = GoldDark, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Founder Profile Card
                        val founderPhotoFile = founderProfile.photoUri.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgLight)
                                .border(1.dp, Color(0xFFD6E2F0), RoundedCornerShape(10.dp))
                                .clickable { onOpenFounderProfile() }
                                .padding(12.dp)
                                .testTag("card_profile_founder"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(46.dp).clip(CircleShape).border(1.5.dp, GoldMetallic, CircleShape).background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (founderPhotoFile != null) {
                                    AsyncImage(
                                        model = founderPhotoFile,
                                        contentDescription = founderProfile.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, -0.35f)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_admin_amirul),
                                        contentDescription = "${founderProfile.name} - ${founderProfile.designation}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, -0.35f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(founderProfile.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                                Text(founderProfile.designation, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = GoldDark)
                                Text("Tap to view full Founder Profile", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "View Founder Profile", tint = GoldDark)
                        }

                        // Developer Profile Card
                        val devPhotoFile = developerProfile.photoUri.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgLight)
                                .border(1.dp, Color(0xFFD6E2F0), RoundedCornerShape(10.dp))
                                .clickable { onOpenDeveloperProfile() }
                                .padding(12.dp)
                                .testTag("card_profile_developer"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(46.dp).clip(CircleShape).border(1.5.dp, GoldMetallic, CircleShape).background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (devPhotoFile != null) {
                                    AsyncImage(
                                        model = devPhotoFile,
                                        contentDescription = developerProfile.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, -0.35f)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_admin_nafiul),
                                        contentDescription = "${developerProfile.name} - ${developerProfile.designation}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, -0.35f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(developerProfile.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                                Text(developerProfile.designation, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = GoldDark)
                                Text("Tap to view full Developer Profile", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "View Developer Profile", tint = GoldDark)
                        }

                        // ISCEB Members Directory Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NavyPrimary)
                                .clickable { onOpenMembers() }
                                .padding(12.dp)
                                .testTag("card_profile_members"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape).background(GoldMetallic),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Groups, contentDescription = "ISCEB Members Directory", tint = NavyDark, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ISCEB Members Directory ($membersCount)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White)
                                Text(
                                    text = if (isAdmin) "View members • Add, edit, upload photos & reorder" else "Browse ISCEB board leadership & member profiles",
                                    fontSize = 10.sp,
                                    color = GoldSoft
                                )
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open ISCEB Members", tint = GoldMetallic)
                        }
                    }
                }

                // Share & Install Card
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPhoneInstall() }.testTag("btn_profile_phone_install"),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(GoldMetallic),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Share, Install & Cloud Sync (APK & OTA)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            Text("Wireless phone install guide & live syllabus sync", fontSize = 10.sp, color = GoldSoft)
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(16.dp))
                    }
                }

                // Certificate Verification Registry
                ActionSettingItem(
                    icon = Icons.Default.Verified,
                    title = "Certificate Verification Registry",
                    subtitle = "Verify diploma authenticity by unique identifier or inspect status",
                    onClick = onOpenVerification
                )

                // Curriculum & Completion Governance Settings (Admin Only)
                if (isAdmin) {
                    ActionSettingItem(
                        icon = Icons.Default.Settings,
                        title = "Curriculum Governance & Completion Rules",
                        subtitle = "Configure min learning time, playback %, mandatory quizzes, and certificate signers",
                        onClick = onOpenSettings
                    )
                    ActionSettingItem(
                        icon = Icons.Default.Cloud,
                        title = "Firebase Cloud Setup & Storage Rules",
                        subtitle = "Firestore security rules, storage zero-trust policies, and environment config",
                        onClick = onOpenFirebaseGuide
                    )
                    ActionSettingItem(
                        icon = Icons.Default.Security,
                        title = "Security & Upload Limits",
                        subtitle = "50MB file validation per item, multi-type study attachments, input sanitization",
                        onClick = onOpenFirebaseGuide
                    )
                    ActionSettingItem(
                        icon = Icons.Default.InstallMobile,
                        title = "Native Android & Cloud Auto-Sync",
                        subtitle = "In-place APK updates & live Firestore curriculum sync",
                        onClick = onOpenFirebaseGuide
                    )

                    // Reset Curriculum Data (Strictly Admin Only!)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResetDemoData() }
                            .testTag("card_admin_reset_curriculum"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7F7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Reset Curriculum to Initial Baseline State",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = DangerRed)
                                )
                                Text(
                                    text = "Restores 12 original modules & 48 classes (preserves accounts)",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888), fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Student Profile Edit Dialog (email & Student ID remain read-only for security)
    if (showEditProfileDialog) {
        var name by remember(currentUser.id) { mutableStateOf(currentUser.fullName) }
        var org by remember(currentUser.id) { mutableStateOf(currentUser.organization) }
        var designation by remember(currentUser.id) { mutableStateOf(currentUser.designation) }
        var mobile by remember(currentUser.id) { mutableStateOf(currentUser.mobileNumber) }
        var program by remember(currentUser.id) { mutableStateOf(currentUser.enrolledProgram) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold, color = NavyPrimary) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(value = org, onValueChange = { org = it }, label = { Text("Organization") }, singleLine = true)
                    OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text("Designation") }, singleLine = true)
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, singleLine = true)
                    OutlinedTextField(value = program, onValueChange = { program = it }, label = { Text("Enrolled Course / PGD Program") }, singleLine = true)
                    OutlinedButton(
                        onClick = {
                            studentPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Profile Photo")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStudentProfile(name, org, designation, mobile, program, null)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = { TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") } }
        )
    }

    // Account Security & Password Management Info Dialog
    if (showSecurityInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityInfoDialog = false },
            title = { Text("Account Security & Password Reset", fontWeight = FontWeight.Bold, color = NavyPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your ISCEB account (${currentUser.email}) uses Google Credential Manager single sign-on without storing raw passwords in the app.",
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "To change your password, recover your account, or manage 2-step verification, open your Google Account security settings or sign out and reconnect.",
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSecurityInfoDialog = false
                        onOpenWebDownloadPage("https://myaccount.google.com/security")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic)
                ) {
                    Text("Google Security")
                }
            },
            dismissButton = { TextButton(onClick = { showSecurityInfoDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = NavyPrimary)
            Text(subtitle, fontSize = 10.sp, color = GoldDark, maxLines = 1)
        }
    }
}

@Composable
private fun ActionSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF0F5FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = NavyPrimary))
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7F96), fontSize = 11.sp))
                }
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFB0C0D0), modifier = Modifier.size(18.dp))
        }
    }
}
