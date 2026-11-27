package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutCloudSync
import com.example.model.CertificateRecord
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.LessonMedia
import com.example.model.MediaType
import com.example.model.ModuleAssignment
import com.example.model.ModuleExam
import com.example.ui.components.AdminLoginDialog
import com.example.ui.components.AdminSettingsDialog
import com.example.ui.components.AppBottomBar
import com.example.ui.components.AppDesktopSidebar
import com.example.ui.components.AppNavigationRail
import com.example.ui.components.AppTopBar
import com.example.ui.components.AssignmentBuilderDialog
import com.example.ui.components.AssignmentDialog
import com.example.ui.components.CertificatePreviewDialog
import com.example.ui.components.CertificateVerificationScreen
import com.example.ui.components.CreateEditClassDialog
import com.example.ui.components.CreateEditModuleDialog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.DocumentPreviewDialog
import com.example.ui.components.ExamDialog
import com.example.ui.components.ExamQuestionBuilderDialog
import com.example.ui.components.ExamsAssignmentsHubDialog
import com.example.ui.components.FirebaseGuideDialog
import com.example.ui.components.ImagePreviewDialog
import com.example.ui.components.PdfPreviewDialog
import com.example.ui.components.PhoneInstallDialog
import com.example.ui.components.QuizDialog
import com.example.ui.components.RevokeCertificateDialog
import com.example.ui.components.UploadMediaDialog
import com.example.ui.components.VideoPreviewDialog
import com.example.ui.screens.AddEditMemberDialog
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.ClassDetailScreen
import com.example.ui.screens.DeveloperProfileScreen
import com.example.ui.screens.FounderProfileScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemberDetailScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.ModuleDetailScreen
import com.example.ui.screens.ModulesScreen
import com.example.ui.screens.MyLearningScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TeacherPanelScreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.util.ApkInstallLaunchResult
import com.example.viewmodel.NavTab
import com.example.viewmodel.Screen
import com.example.viewmodel.SupplyChainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SupplyChainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: SupplyChainViewModel) {
    val isInitializing by viewModel.isInitializing.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val studentProgressMap by viewModel.studentProgressMap.collectAsStateWithLifecycle()
    val adminSettings by viewModel.adminSettings.collectAsStateWithLifecycle()
    val issuedCertificates by viewModel.issuedCertificates.collectAsStateWithLifecycle()
    val certificateEligibility by viewModel.certificateEligibility.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminLoggedIn.collectAsStateWithLifecycle()
    val isTeacher by viewModel.isTeacher.collectAsStateWithLifecycle()
    val registeredAccounts by viewModel.registeredAccounts.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()

    val isPlayingAudio by viewModel.isPlayingAudio.collectAsStateWithLifecycle()
    val audioProgressMs by viewModel.audioProgressMs.collectAsStateWithLifecycle()
    val audioDurationMs by viewModel.audioDurationMs.collectAsStateWithLifecycle()
    val audioSpeed by viewModel.audioSpeed.collectAsStateWithLifecycle()

    val isPlayingVideo by viewModel.isPlayingVideo.collectAsStateWithLifecycle()
    val videoPositionMs by viewModel.videoPositionMs.collectAsStateWithLifecycle()
    val videoDurationMs by viewModel.videoDurationMs.collectAsStateWithLifecycle()

    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val uploadByteStatus by viewModel.uploadByteStatus.collectAsStateWithLifecycle()
    val uploadError by viewModel.uploadError.collectAsStateWithLifecycle()

    // Dialog & Modal states
    val showAdminLogin by viewModel.showAdminLoginDialog.collectAsStateWithLifecycle()
    val showCreateModule by viewModel.showCreateModuleDialog.collectAsStateWithLifecycle()
    val editingModule by viewModel.editingModule.collectAsStateWithLifecycle()
    val showCreateClass by viewModel.showCreateClassDialog.collectAsStateWithLifecycle()
    val editingClass by viewModel.editingClass.collectAsStateWithLifecycle()
    val deleteConfirmation by viewModel.deleteConfirmation.collectAsStateWithLifecycle()
    val showFirebaseGuide by viewModel.showFirebaseGuideDialog.collectAsStateWithLifecycle()
    val showAdminSettings by viewModel.showAdminSettingsDialog.collectAsStateWithLifecycle()
    val activeQuizClass by viewModel.activeQuizClass.collectAsStateWithLifecycle()
    val previewCert by viewModel.previewCertificate.collectAsStateWithLifecycle()
    val revokeTargetCert by viewModel.revokeTargetCert.collectAsStateWithLifecycle()

    val publishManifest by viewModel.publishManifest.collectAsStateWithLifecycle()
    val pendingUpdateManifest by viewModel.pendingUpdateManifest.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
    val apkDownloadState by viewModel.apkDownloadState.collectAsStateWithLifecycle()
    val cloudAuthUser by viewModel.cloudAuthUser.collectAsStateWithLifecycle()

    val moduleExams by viewModel.moduleExams.collectAsStateWithLifecycle()
    val examAttempts by viewModel.examAttempts.collectAsStateWithLifecycle()
    val moduleAssignments by viewModel.moduleAssignments.collectAsStateWithLifecycle()
    val assignmentSubmissions by viewModel.assignmentSubmissions.collectAsStateWithLifecycle()
    val activeExam by viewModel.activeExam.collectAsStateWithLifecycle()
    val examTimeRemainingSeconds by viewModel.examTimeRemainingSeconds.collectAsStateWithLifecycle()
    val examCurrentAnswers by viewModel.examCurrentAnswers.collectAsStateWithLifecycle()
    val activeExamResult by viewModel.activeExamResult.collectAsStateWithLifecycle()
    val isExamSubmitted by viewModel.isExamSubmitted.collectAsStateWithLifecycle()
    val activeAssignment by viewModel.activeAssignment.collectAsStateWithLifecycle()

    val verificationSearchQuery by viewModel.verificationSearchQuery.collectAsStateWithLifecycle()
    val verificationResult by viewModel.verificationResult.collectAsStateWithLifecycle()
    val isVerificationSearched by viewModel.isVerificationSearched.collectAsStateWithLifecycle()
    val developerProfile by viewModel.developerProfile.collectAsStateWithLifecycle()
    val founderProfile by viewModel.founderProfile.collectAsStateWithLifecycle()
    val iscebMembers by viewModel.iscebMembers.collectAsStateWithLifecycle()
    val showAddMemberDialog by viewModel.showAddMemberDialog.collectAsStateWithLifecycle()
    val editingMember by viewModel.editingMember.collectAsStateWithLifecycle()
    val adminSummaries by viewModel.adminSummaries.collectAsStateWithLifecycle()
    val isAnyAdminConfigured by viewModel.isAnyAdminConfigured.collectAsStateWithLifecycle()
    val latestRecoveryKeyNotice by viewModel.latestRecoveryKeyNotice.collectAsStateWithLifecycle()
    val latestCoAdminInviteNotice by viewModel.latestCoAdminInviteNotice.collectAsStateWithLifecycle()
    val previewPdfMedia by viewModel.previewPdfMedia.collectAsStateWithLifecycle()
    val previewImageMedia by viewModel.previewImageMedia.collectAsStateWithLifecycle()
    val previewVideoMedia by viewModel.previewVideoMedia.collectAsStateWithLifecycle()
    val previewDocumentMedia by viewModel.previewDocumentMedia.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val triggerGoogleSignIn: () -> Unit = {
        onGoogleSignInClicked(
            context = context.findActivity() ?: context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = { user -> viewModel.onCloudSignInSuccess(user) },
            onAuthError = { err -> viewModel.onCloudSignInError(err) }
        )
    }

    val triggerGoogleSignOut: () -> Unit = {
        signOutCloudSync(
            credentialManager = credentialManager,
            scope = scope,
            onSignOutComplete = {
                viewModel.showNotification("Disconnected Google Cloud Auto-Sync.")
            }
        )
    }

    val openApkDownloadUrl: (String) -> Unit = { rawUrl ->
        val safeUrl = rawUrl.trim().ifBlank { adminSettings.apkDownloadUrl }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModel.showNotification(
                "Unable to open update URL (${safeUrl}): ${e.localizedMessage ?: "No browser found"}",
                isError = true
            )
        }
    }

    var activeUploadMediaType by remember { mutableStateOf<MediaType?>(null) }
    var activeUploadClassId by remember { mutableStateOf<String?>(null) }
    var activeTargetModuleForClass by remember { mutableStateOf<String?>(null) }
    var showPhoneInstallDialog by remember { mutableStateOf(false) }
    var showExamsAssignmentsHub by remember { mutableStateOf(false) }
    var showSplashScreen by remember { mutableStateOf(true) }
    var pendingDownloadMedia by remember { mutableStateOf<LessonMedia?>(null) }
    var pendingDownloadCertificate by remember { mutableStateOf<CertificateRecord?>(null) }
    var pendingUnknownSourcesApkRetry by remember { mutableStateOf(false) }
    var teacherPanelInitialTab by remember { mutableStateOf(0) }
    var quickManageExam by remember { mutableStateOf<ModuleExam?>(null) }
    var quickCreateExamModuleId by remember { mutableStateOf<String?>(null) }
    var quickManageAssignment by remember { mutableStateOf<ModuleAssignment?>(null) }
    var quickCreateAssignmentModuleId by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (pendingUnknownSourcesApkRetry) {
            pendingUnknownSourcesApkRetry = false
            val retryResult = viewModel.installDownloadedApk()
            if (retryResult is ApkInstallLaunchResult.RequiresUnknownSourcesPermission) {
                viewModel.showNotification(
                    "Allow installing apps from this source in Settings to finish updating ISCEB.",
                    isError = true
                )
            }
        }
    }

    val handleInstallDownloadedApk: () -> Unit = {
        when (val launchResult = viewModel.installDownloadedApk()) {
            is ApkInstallLaunchResult.RequiresUnknownSourcesPermission -> {
                pendingUnknownSourcesApkRetry = true
                viewModel.showNotification("Enable 'Allow from this source' permission to install the ISCEB update.")
                unknownSourcesLauncher.launch(launchResult.settingsIntent)
            }
            is ApkInstallLaunchResult.LaunchedInstaller,
            is ApkInstallLaunchResult.Failure -> Unit
        }
    }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val targetMedia = pendingDownloadMedia
        if (uri != null && targetMedia != null) {
            viewModel.exportMediaToUri(targetMedia, uri)
        }
        pendingDownloadMedia = null
    }

    val exportCertificatePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val targetCert = pendingDownloadCertificate
        if (uri != null && targetCert != null) {
            viewModel.exportCertificatePdfToUri(targetCert, uri)
        }
        pendingDownloadCertificate = null
    }

    val startDownloadCertificatePdf: (CertificateRecord) -> Unit = { cert ->
        pendingDownloadCertificate = cert
        val safeName = cert.certificateId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        exportCertificatePdfLauncher.launch("$safeName.pdf")
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = { user -> viewModel.onCloudSignInSuccess(user) },
            onUnauthenticated = {}
        )
        kotlinx.coroutines.delay(3000)
        viewModel.checkForUpdates(silentOnError = true)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.checkUpdatesOnLifecycleResume()
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> viewModel.pauseAllActiveStudySessions()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showSplashScreen) {
        SplashScreen(
            onDismiss = { showSplashScreen = false },
            isInitializing = isInitializing
        )
    } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isDesktopLayout = maxWidth >= 840.dp
            val isExpandedLayout = maxWidth >= 600.dp

            val totalClassesCount = classes.size
            val completedClassesCount = progress.completedClassIds.size
            val workspaceProgressPct = if (totalClassesCount > 0) {
                ((completedClassesCount * 100f) / totalClassesCount).toInt().coerceIn(0, 100)
            } else 0
            val hasIssuedCert = issuedCertificates.any { it.studentId == currentUser.id }
            val workspaceCertLabel = when {
                hasIssuedCert -> "Issued"
                certificateEligibility.isEligible -> "Ready"
                else -> "PGD"
            }
            val workspaceApkLabel = run {
                val apkVer = appUpdateInfo.latestAvailableApkVersionName.ifBlank {
                    publishManifest.latestApkVersionName.ifBlank { "1.6" }
                }
                val sizeMb = if (appUpdateInfo.fileSizeBytes > 0L) {
                    String.format(java.util.Locale.US, "%.1f MB", appUpdateInfo.fileSizeBytes / (1024.0 * 1024.0))
                } else "18.5 MB"
                "v$apkVer ($sizeMb)"
            }
            val workspaceSyncLabel = cloudAuthUser?.email?.let { "Cloud: $it" } ?: "Local + Web Ready"
            val handleOpenAdminGovernance: () -> Unit = {
                when {
                    isAdmin -> viewModel.navigateTo(Screen.AdminPanel)
                    isTeacher -> viewModel.navigateTo(Screen.TeacherPanel)
                    else -> viewModel.showAdminLoginDialog.value = true
                }
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = !isDesktopLayout,
                drawerContent = {
                    if (!isDesktopLayout) {
                        ModalDrawerSheet(drawerContainerColor = NavyDark) {
                            AppDesktopSidebar(
                                currentTab = currentTab,
                                onTabSelected = { tab ->
                                    scope.launch { drawerState.close() }
                                    viewModel.setTab(tab)
                                },
                                completedCount = completedClassesCount,
                                totalCount = totalClassesCount,
                                isAdmin = isAdmin,
                                isTeacher = isTeacher,
                                progressPercent = workspaceProgressPct,
                                moduleCount = modules.size,
                                examCount = moduleExams.size,
                                assignmentCount = moduleAssignments.size,
                                certificateStatusLabel = workspaceCertLabel,
                                membersCount = iscebMembers.size,
                                curriculumVersionTag = publishManifest.versionTag.ifBlank { "v1.6.0" },
                                latestApkLabel = workspaceApkLabel,
                                syncStatusLabel = workspaceSyncLabel,
                                isCloudSynced = cloudAuthUser != null,
                                onOpenExamsAndAssignments = {
                                    scope.launch { drawerState.close() }
                                    showExamsAssignmentsHub = true
                                },
                                onOpenAdminGovernance = {
                                    scope.launch { drawerState.close() }
                                    handleOpenAdminGovernance()
                                },
                                onCheckAppUpdateClick = {
                                    scope.launch { drawerState.close() }
                                    viewModel.checkForUpdates(silentOnError = false)
                                },
                                onOpenApkPortalClick = {
                                    scope.launch { drawerState.close() }
                                    showPhoneInstallDialog = true
                                }
                            )
                        }
                    }
                }
            ) {
            Scaffold(
                topBar = {
                    if (currentScreen !is Screen.CertificateVerification) {
                        AppTopBar(
                            title = "ISCEB",
                            currentUser = currentUser,
                            isAdmin = isAdmin,
                            onAdminClick = {
                                if (isAdmin) {
                                    viewModel.logoutAdmin()
                                } else {
                                    viewModel.showAdminLoginDialog.value = true
                                }
                            },
                            onVerificationClick = {
                                viewModel.navigateTo(Screen.CertificateVerification)
                            },
                            onSettingsClick = {
                                viewModel.showAdminSettingsDialog.value = true
                            },
                            onFirebaseClick = {
                                viewModel.showFirebaseGuideDialog.value = true
                            },
                            onPhoneInstallClick = {
                                showPhoneInstallDialog = true
                            },
                            isTeacher = isTeacher,
                            onWorkspaceMenuClick = if (!isDesktopLayout) {
                                { scope.launch { drawerState.open() } }
                            } else null,
                            modifier = Modifier.statusBarsPadding()
                        )
                    }
                },
                bottomBar = {
                    if (currentScreen is Screen.Main && !isExpandedLayout) {
                        AppBottomBar(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.setTab(it) },
                            completedCount = progress.completedClassIds.size
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (currentScreen is Screen.Main && isDesktopLayout) {
                        AppDesktopSidebar(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.setTab(it) },
                            completedCount = completedClassesCount,
                            totalCount = totalClassesCount,
                            isAdmin = isAdmin,
                            isTeacher = isTeacher,
                            progressPercent = workspaceProgressPct,
                            moduleCount = modules.size,
                            examCount = moduleExams.size,
                            assignmentCount = moduleAssignments.size,
                            certificateStatusLabel = workspaceCertLabel,
                            membersCount = iscebMembers.size,
                            curriculumVersionTag = publishManifest.versionTag.ifBlank { "v1.6.0" },
                            latestApkLabel = workspaceApkLabel,
                            syncStatusLabel = workspaceSyncLabel,
                            isCloudSynced = cloudAuthUser != null,
                            onOpenExamsAndAssignments = { showExamsAssignmentsHub = true },
                            onOpenAdminGovernance = handleOpenAdminGovernance,
                            onCheckAppUpdateClick = { viewModel.checkForUpdates(silentOnError = false) },
                            onOpenApkPortalClick = { showPhoneInstallDialog = true }
                        )
                    } else if (currentScreen is Screen.Main && isExpandedLayout) {
                        AppNavigationRail(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.setTab(it) },
                            completedCount = progress.completedClassIds.size
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = if (isDesktopLayout) 1200.dp else 960.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            when (val screen = currentScreen) {
                is Screen.Main -> {
                    when (currentTab) {
                        NavTab.HOME -> {
                            HomeScreen(
                                modules = modules,
                                classes = classes,
                                progress = progress,
                                isAdmin = isAdmin,
                                searchQuery = searchQuery,
                                selectedFilter = selectedFilter,
                                publishManifest = publishManifest,
                                pendingUpdateManifest = pendingUpdateManifest,
                                syncStatus = syncStatus,
                                appUpdateInfo = appUpdateInfo,
                                cloudUserEmail = cloudAuthUser?.email,
                                lastSyncTimestamp = lastSyncTimestamp,
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onFilterChange = { viewModel.setSelectedFilter(it) },
                                onModuleClick = { mod -> viewModel.navigateTo(Screen.ModuleDetail(mod.id)) },
                                onClassClick = { cls -> viewModel.navigateTo(Screen.ClassDetail(cls.id)) },
                                onEditModule = { mod -> viewModel.editingModule.value = mod },
                                onDeleteModule = { mod ->
                                    viewModel.deleteConfirmation.value = "Module: ${mod.title}" to mod.id
                                },
                                onCreateModuleClick = { viewModel.showCreateModuleDialog.value = true },
                                onSeeAllModules = { viewModel.setTab(NavTab.MODULES) },
                                onSyncClick = { viewModel.syncCurriculumNow() },
                                onCheckUpdatesClick = { viewModel.checkForUpdates(silentOnError = false) },
                                onPhoneInstallClick = { showPhoneInstallDialog = true },
                                onConnectCloudSyncClick = triggerGoogleSignIn,
                                onUpdateApkClick = { _ ->
                                    showPhoneInstallDialog = true
                                    viewModel.downloadLatestApk()
                                },
                                founderProfile = founderProfile,
                                developerProfile = developerProfile,
                                membersCount = iscebMembers.size,
                                onOpenMembers = { viewModel.setTab(NavTab.MEMBERS) },
                                onOpenFounderProfile = { viewModel.navigateTo(Screen.FounderProfileScreenRoute) },
                                onOpenDeveloperProfile = { viewModel.navigateTo(Screen.DeveloperProfileScreenRoute) },
                                isTeacher = isTeacher,
                                examCount = moduleExams.size,
                                assignmentCount = moduleAssignments.size,
                                certificateStatusLabel = workspaceCertLabel,
                                showInlineWorkspaceCard = !isDesktopLayout,
                                onNavigateTab = { tab -> viewModel.setTab(tab) },
                                onOpenExamsAndAssignments = { showExamsAssignmentsHub = true },
                                onOpenAdminGovernance = handleOpenAdminGovernance
                            )
                        }
                        NavTab.MODULES -> {
                            ModulesScreen(
                                modules = modules,
                                classes = classes,
                                isAdmin = isAdmin,
                                onModuleClick = { mod -> viewModel.navigateTo(Screen.ModuleDetail(mod.id)) },
                                onEditModule = { mod -> viewModel.editingModule.value = mod },
                                onDeleteModule = { mod ->
                                    viewModel.deleteConfirmation.value = "Module: ${mod.title}" to mod.id
                                },
                                onCreateModuleClick = { viewModel.showCreateModuleDialog.value = true }
                            )
                        }
                        NavTab.MY_LEARNING -> {
                            val passedExamCnt = adminSettings.requiredExamIds.count { reqId ->
                                examAttempts.any { it.examId == reqId && it.studentId == currentUser.id && it.isPassed }
                            }
                            val submittedAsgCnt = assignmentSubmissions.count { it.studentId == currentUser.id }
                            MyLearningScreen(
                                modules = modules,
                                classes = classes,
                                progress = progress,
                                eligibility = certificateEligibility,
                                issuedCertificates = issuedCertificates.filter { it.studentId == currentUser.id },
                                isAdmin = isAdmin,
                                onClassClick = { cls -> viewModel.navigateTo(Screen.ClassDetail(cls.id)) },
                                onToggleComplete = { cls -> viewModel.adminToggleClassCompletion(cls.id) },
                                onToggleBookmark = { cls -> viewModel.toggleClassBookmarked(cls.id) },
                                onGenerateCertificate = { viewModel.generateCertificate() },
                                onOpenCertificatePdf = { cert -> viewModel.openOrShareCertificate(cert) },
                                onVerifyCertificate = { certId ->
                                    viewModel.searchCertificate(certId)
                                    viewModel.navigateTo(Screen.CertificateVerification)
                                },
                                passedExamCount = passedExamCnt,
                                totalRequiredExamCount = adminSettings.requiredExamIds.size.coerceAtLeast(1),
                                submittedAssignmentCount = submittedAsgCnt,
                                adminSettings = adminSettings,
                                onPreviewDemoCertificate = { viewModel.previewDemoCertificate() },
                                onDownloadCertificatePdf = startDownloadCertificatePdf,
                                onDownloadDemoCertificatePdf = {
                                    startDownloadCertificatePdf(viewModel.createDemoCertificateRecord())
                                }
                            )
                        }
                        NavTab.MEMBERS -> {
                            MembersScreen(
                                members = iscebMembers,
                                founderProfile = founderProfile,
                                developerProfile = developerProfile,
                                isAdmin = isAdmin,
                                onOpenFounderProfile = { viewModel.navigateTo(Screen.FounderProfileScreenRoute) },
                                onOpenDeveloperProfile = { viewModel.navigateTo(Screen.DeveloperProfileScreenRoute) },
                                onMemberClick = { member ->
                                    viewModel.navigateTo(Screen.MemberDetail(member.id))
                                },
                                onAddMemberClick = { viewModel.showAddMemberDialog.value = true },
                                onEditMemberClick = { member -> viewModel.editingMember.value = member },
                                onDeleteMemberClick = { member ->
                                    viewModel.deleteConfirmation.value = "Member: ${member.fullName}" to member.id
                                },
                                onMoveMemberOrder = { memberId, moveUp ->
                                    viewModel.moveIscebMemberOrder(memberId, moveUp)
                                }
                            )
                        }
                        NavTab.DEVELOPER_PROFILE -> {
                            DeveloperProfileScreen(
                                developerProfile = developerProfile,
                                isAdmin = isAdmin,
                                onUpdateProfile = { updated, pickedUri ->
                                    viewModel.updateDeveloperProfile(updated, pickedUri)
                                }
                            )
                        }
                        NavTab.PROFILE -> {
                            ProfileScreen(
                                currentUser = currentUser,
                                availableUsers = viewModel.availableUsers,
                                isAdmin = isAdmin,
                                cloudAuthEmail = cloudAuthUser?.email,
                                appUpdateInfo = appUpdateInfo,
                                apkDownloadState = apkDownloadState,
                                onSwitchUser = { viewModel.switchUser(it) },
                                onAdminLoginClick = { viewModel.showAdminLoginDialog.value = true },
                                onAdminLogoutClick = { viewModel.logoutAdmin() },
                                onOpenVerification = { viewModel.navigateTo(Screen.CertificateVerification) },
                                onOpenSettings = { viewModel.showAdminSettingsDialog.value = true },
                                onOpenFirebaseGuide = { viewModel.showFirebaseGuideDialog.value = true },
                                onResetDemoData = {
                                    viewModel.deleteConfirmation.value = "All curriculum custom data (Reset to Initial 12 Modules & 48 Classes)" to "RESET_DEMO"
                                },
                                onOpenPhoneInstall = { showPhoneInstallDialog = true },
                                onCheckUpdates = { viewModel.checkForUpdates(silentOnError = false) },
                                onDownloadLatestApk = { viewModel.downloadLatestApk() },
                                onCancelApkDownload = { viewModel.cancelApkDownload() },
                                onInstallDownloadedApk = handleInstallDownloadedApk,
                                onOpenWebDownloadPage = openApkDownloadUrl,
                                onOpenDeveloperProfile = { viewModel.navigateTo(Screen.DeveloperProfileScreenRoute) },
                                onOpenFounderProfile = { viewModel.navigateTo(Screen.FounderProfileScreenRoute) },
                                onOpenMembers = { viewModel.setTab(NavTab.MEMBERS) },
                                founderProfile = founderProfile,
                                developerProfile = developerProfile,
                                membersCount = iscebMembers.size,
                                onConnectGoogleCloudClick = triggerGoogleSignIn,
                                onDisconnectGoogleCloudClick = triggerGoogleSignOut,
                                onExportBackupJson = { viewModel.exportWorkspaceBackupJson() },
                                onImportBackupJson = { viewModel.importWorkspaceBackupJson(it) },
                                onBackupMessage = { msg -> viewModel.showNotification(msg) },
                                registeredAccounts = registeredAccounts,
                                isTeacher = isTeacher,
                                modules = modules,
                                classes = classes,
                                studentProgressMap = studentProgressMap,
                                userProgress = progress,
                                moduleExams = moduleExams,
                                examAttempts = examAttempts,
                                moduleAssignments = moduleAssignments,
                                assignmentSubmissions = assignmentSubmissions,
                                eligibility = certificateEligibility,
                                issuedCertificates = issuedCertificates,
                                onRegisterOrSignInStudent = { fullName, email, org, desig, mobile, program, photoUri ->
                                    viewModel.registerOrSignInStudentAccount(fullName, email, org, desig, mobile, program, photoUri)
                                },
                                onUpdateStudentProfile = { fullName, org, desig, mobile, program, photoUri ->
                                    viewModel.updateStudentOwnProfile(fullName, org, desig, mobile, program, photoUri)
                                },
                                onSignOutAccount = {
                                    viewModel.signOutStudentAccount()
                                    triggerGoogleSignOut()
                                },
                                onDeleteStudentAccount = {
                                    viewModel.deleteStudentAccountAndData {
                                        signOutCloudSync(
                                            credentialManager = credentialManager,
                                            scope = scope,
                                            onSignOutComplete = {}
                                        )
                                    }
                                },
                                onActivateTeacher = { userId -> viewModel.activateTeacherAccount(userId) },
                                onOpenTeacherPanel = { viewModel.navigateTo(Screen.TeacherPanel) },
                                onOpenAdminPanel = { viewModel.navigateTo(Screen.AdminPanel) },
                                onModuleClick = { mod -> viewModel.navigateTo(Screen.ModuleDetail(mod.id)) },
                                onClassClick = { cls -> viewModel.navigateTo(Screen.ClassDetail(cls.id)) },
                                onTakeExam = { ex -> viewModel.openExam(ex) },
                                onOpenAssignment = { asg -> viewModel.openAssignment(asg) },
                                onGenerateCertificate = { viewModel.generateCertificate() },
                                onOpenCertificatePdf = { cert -> viewModel.openOrShareCertificate(cert) },
                                onVerifyCertificate = { certId ->
                                    viewModel.searchCertificate(certId)
                                    viewModel.navigateTo(Screen.CertificateVerification)
                                },
                                onPreviewDemoCertificate = { viewModel.previewDemoCertificate() },
                                onDownloadCertificatePdf = startDownloadCertificatePdf,
                                onDownloadDemoCertificatePdf = {
                                    startDownloadCertificatePdf(viewModel.createDemoCertificateRecord())
                                },
                                demoCertificateEnabled = adminSettings.demoCertificateEnabled
                            )
                        }
                    }
                }

                is Screen.ModuleDetail -> {
                    val targetModule = modules.find { it.id == screen.moduleId } ?: modules.firstOrNull()
                    if (targetModule != null) {
                        val canManageMod = viewModel.canManageModule(targetModule.id)
                        val modExam = moduleExams.find { it.moduleId == targetModule.id }
                        val bestAtt = modExam?.let { ex ->
                            examAttempts.filter { it.examId == ex.id && it.studentId == currentUser.id }.maxByOrNull { it.scoreMarks }
                        }
                        val attemptsUsed = modExam?.let { viewModel.getStudentExamAttemptsCount(it.id) } ?: 0
                        val modAsg = moduleAssignments.find { it.moduleId == targetModule.id }
                        val asgSub = modAsg?.let { asg ->
                            assignmentSubmissions.find { it.assignmentId == asg.id && it.studentId == currentUser.id }
                        }

                        ModuleDetailScreen(
                            module = targetModule,
                            classes = classes,
                            exam = modExam,
                            bestAttempt = bestAtt,
                            assignment = modAsg,
                            assignmentSubmission = asgSub,
                            isAdmin = isAdmin,
                            onBack = { viewModel.handleBack() },
                            onClassClick = { cls -> viewModel.navigateTo(Screen.ClassDetail(cls.id)) },
                            onToggleComplete = { cls -> viewModel.adminToggleClassCompletion(cls.id) },
                            onToggleBookmark = { cls -> viewModel.toggleClassBookmarked(cls.id) },
                            onEditModule = { mod -> viewModel.editingModule.value = mod },
                            onDeleteModule = { mod ->
                                viewModel.deleteConfirmation.value = "Module: ${mod.title}" to mod.id
                            },
                            onAddClass = { mod ->
                                activeTargetModuleForClass = mod.id
                                viewModel.showCreateClassDialog.value = true
                            },
                            onEditClass = { cls -> viewModel.editingClass.value = cls },
                            onDeleteClass = { cls ->
                                viewModel.deleteConfirmation.value = "Class: ${cls.title}" to cls.id
                            },
                            onMoveClassUp = { cls -> viewModel.reorderClass(cls.id, true) },
                            onMoveClassDown = { cls -> viewModel.reorderClass(cls.id, false) },
                            onToggleClassPublish = { cls -> viewModel.toggleClassPublished(cls.id) },
                            onTakeExam = { ex -> viewModel.openExam(ex) },
                            onOpenAssignment = { asg -> viewModel.openAssignment(asg) },
                            canManageModule = canManageMod,
                            attemptsUsedCount = attemptsUsed,
                            onCreateOrEditExam = {
                                if (modExam != null) {
                                    quickManageExam = modExam
                                } else {
                                    quickCreateExamModuleId = targetModule.id
                                }
                            },
                            onCreateOrEditAssignment = {
                                if (modAsg != null) {
                                    quickManageAssignment = modAsg
                                } else {
                                    quickCreateAssignmentModuleId = targetModule.id
                                }
                            }
                        )
                    } else {
                        viewModel.handleBack()
                    }
                }

                is Screen.ClassDetail -> {
                    val targetClass = classes.find { it.id == screen.classId } ?: classes.firstOrNull()
                    if (targetClass != null) {
                        val parentModule = modules.find { it.id == targetClass.moduleId }
                        val prog = studentProgressMap[targetClass.id]
                        val canManageCls = viewModel.canManageModule(targetClass.moduleId)
                        ClassDetailScreen(
                            courseClass = targetClass,
                            module = parentModule,
                            allModuleClasses = classes.filter { it.moduleId == targetClass.moduleId },
                            studentProgress = prog,
                            adminSettings = adminSettings,
                            isAdmin = isAdmin,
                            isPlayingAudio = isPlayingAudio,
                            audioProgressMs = audioProgressMs,
                            audioDurationMs = audioDurationMs,
                            audioSpeed = audioSpeed,
                            isPlayingVideo = isPlayingVideo,
                            videoPositionMs = videoPositionMs,
                            videoDurationMs = videoDurationMs,
                            onBack = { viewModel.handleBack() },
                            onToggleBookmark = { viewModel.toggleClassBookmarked(targetClass.id) },
                            onConfirmPdfRead = { viewModel.confirmPdfRead(targetClass.id) },
                            onTakeQuiz = { viewModel.activeQuizClass.value = targetClass },
                            onAdminToggleCompletion = { viewModel.adminToggleClassCompletion(targetClass.id) },
                            onPlayPauseAudio = { viewModel.playPauseAudio(targetClass.id) },
                            onSeekAudio = { viewModel.seekAudio(it) },
                            onSkipAudioSeconds = { viewModel.skipAudioSeconds(it) },
                            onAudioSpeedChange = { viewModel.setAudioSpeed(it) },
                            onPlayPauseVideo = { viewModel.playPauseVideo(targetClass.id) },
                            onSeekVideo = { viewModel.seekVideo(it, targetClass.id) },
                            onOpenUploadDialog = { mediaType ->
                                activeUploadMediaType = mediaType
                                activeUploadClassId = targetClass.id
                            },
                            onDeleteMedia = { mediaType -> viewModel.deleteMedia(targetClass.id, mediaType) },
                            onDeleteAttachment = { mediaId -> viewModel.deleteAttachment(targetClass.id, mediaId) },
                            onOpenAttachment = { media -> viewModel.openMediaFile(media) },
                            onOpenExternalAttachment = { media -> viewModel.openMediaWithExternalViewer(media) },
                            onDownloadAttachment = { media ->
                                pendingDownloadMedia = media
                                exportDocumentLauncher.launch(media.fileName)
                            },
                            onResolveMediaFile = { media -> viewModel.resolveLocalMediaFile(media) },
                            onLoadDemoMedia = { viewModel.loadSampleMedia(targetClass.id) },
                            onNavigateClass = { nextCls ->
                                viewModel.navigateTo(Screen.ClassDetail(nextCls.id))
                            },
                            canManageClass = canManageCls,
                            canDownloadMedia = canManageCls,
                            onPdfPageStudyTick = { curPage, totPages, activeDelta, idleDelta ->
                                viewModel.recordPdfPageStudy(
                                    classId = targetClass.id,
                                    currentPage = curPage,
                                    totalPages = totPages,
                                    secondsReading = activeDelta,
                                    explicitConfirm = false,
                                    idleExcludedSeconds = idleDelta
                                )
                            },
                            onVideoPlaybackProgress = { posMs, durMs, isPlayingNow ->
                                viewModel.updateVideoPlaybackProgress(
                                    classId = targetClass.id,
                                    positionMs = posMs,
                                    durationMs = durMs,
                                    isPlaying = isPlayingNow
                                )
                            }
                        )
                    } else {
                        viewModel.handleBack()
                    }
                }

                is Screen.CertificateVerification -> {
                    CertificateVerificationScreen(
                        searchQuery = verificationSearchQuery,
                        verificationResult = verificationResult,
                        isSearched = isVerificationSearched,
                        isAdmin = isAdmin,
                        onSearchChange = { viewModel.verificationSearchQuery.value = it },
                        onSearchSubmit = { viewModel.searchCertificate(it) },
                        onBack = { viewModel.handleBack() },
                        onRevoke = { cert -> viewModel.revokeTargetCert.value = cert },
                        onReinstate = { cert -> viewModel.reinstateCertificate(cert.certificateId) },
                        onOpenPdf = { cert -> viewModel.openOrShareCertificate(cert) }
                    )
                }

                is Screen.FounderProfileScreenRoute -> {
                    FounderProfileScreen(
                        founderProfile = founderProfile,
                        isAdmin = isAdmin,
                        onBack = { viewModel.handleBack() },
                        onUpdateProfile = { updated, pickedUri ->
                            viewModel.updateFounderProfile(updated, pickedUri)
                        }
                    )
                }

                is Screen.DeveloperProfileScreenRoute -> {
                    DeveloperProfileScreen(
                        developerProfile = developerProfile,
                        isAdmin = isAdmin,
                        onBack = { viewModel.handleBack() },
                        onUpdateProfile = { updated, pickedUri ->
                            viewModel.updateDeveloperProfile(updated, pickedUri)
                        }
                    )
                }

                is Screen.MemberDetail -> {
                    val targetMember = iscebMembers.find { it.id == screen.memberId }
                    if (targetMember != null) {
                        MemberDetailScreen(
                            member = targetMember,
                            isAdmin = isAdmin,
                            onBack = { viewModel.handleBack() },
                            onEditClick = { viewModel.editingMember.value = targetMember },
                            onDeleteClick = {
                                viewModel.deleteConfirmation.value = "Member: ${targetMember.fullName}" to targetMember.id
                            }
                        )
                    } else {
                        viewModel.handleBack()
                    }
                }

                is Screen.TeacherPanel -> {
                    TeacherPanelScreen(
                        currentUser = currentUser,
                        isAdmin = isAdmin,
                        modules = modules,
                        classes = classes,
                        moduleExams = moduleExams,
                        moduleAssignments = moduleAssignments,
                        examAttempts = examAttempts,
                        assignmentSubmissions = assignmentSubmissions,
                        registeredAccounts = registeredAccounts,
                        onBack = { viewModel.handleBack() },
                        onOpenModule = { mod -> viewModel.navigateTo(Screen.ModuleDetail(mod.id)) },
                        onOpenClass = { cls -> viewModel.navigateTo(Screen.ClassDetail(cls.id)) },
                        onAddClass = { modId ->
                            activeTargetModuleForClass = modId
                            viewModel.showCreateClassDialog.value = true
                        },
                        onEditClass = { cls -> viewModel.editingClass.value = cls },
                        onToggleClassPublish = { classId -> viewModel.toggleClassPublished(classId) },
                        onUploadClassMedia = { cls ->
                            activeUploadMediaType = MediaType.PDF
                            activeUploadClassId = cls.id
                        },
                        onSaveExam = { exam -> viewModel.createOrUpdateModuleExam(exam) },
                        onSaveAssignment = { asg -> viewModel.createOrUpdateModuleAssignment(asg) },
                        onOpenAssignmentForGrading = { asg -> viewModel.openAssignment(asg) },
                        onGradeSubmission = { subId, marks, feedback ->
                            viewModel.gradeAssignment(subId, marks, feedback)
                        },
                        initialTab = teacherPanelInitialTab,
                        onDeleteExam = { examId -> viewModel.deleteModuleExam(examId) },
                        onDeleteAssignment = { asgId -> viewModel.deleteModuleAssignment(asgId) }
                    )
                }

                is Screen.AdminPanel -> {
                    AdminPanelScreen(
                        currentUser = currentUser,
                        isAdmin = isAdmin,
                        modules = modules,
                        registeredAccounts = registeredAccounts,
                        issuedCertificates = issuedCertificates,
                        assignmentSubmissions = assignmentSubmissions,
                        auditLogs = auditLogs,
                        adminSettings = adminSettings,
                        onBack = { viewModel.handleBack() },
                        onOpenAdminSettings = { viewModel.showAdminSettingsDialog.value = true },
                        onOpenTeacherPanel = {
                            teacherPanelInitialTab = 0
                            viewModel.navigateTo(Screen.TeacherPanel)
                        },
                        onOpenAssessmentStudio = {
                            teacherPanelInitialTab = 1
                            viewModel.navigateTo(Screen.TeacherPanel)
                        },
                        onOpenVerification = { viewModel.navigateTo(Screen.CertificateVerification) },
                        onCreateOrUpdateTeacher = { fullName, email, desig, org, assignedIds ->
                            viewModel.createOrUpdateTeacherAccount(fullName, email, desig, org, assignedIds)
                        },
                        onUpdateUserAccess = { userId, role, status, verified, assignedIds ->
                            viewModel.adminUpdateUserAccess(userId, role, status, verified, assignedIds)
                        },
                        onActivateTeacherSession = { userId ->
                            viewModel.activateTeacherAccount(userId)
                            viewModel.navigateTo(Screen.TeacherPanel)
                        },
                        onReissueCertificate = { studentId, reason -> viewModel.reissueCertificate(studentId, reason) },
                        onRevokeCertificate = { cert -> viewModel.revokeTargetCert.value = cert },
                        onReinstateCertificate = { certId -> viewModel.reinstateCertificate(certId) },
                        onOpenCertificatePdf = { cert -> viewModel.openOrShareCertificate(cert) },
                        onToggleCertificatesEnabled = { enabled -> viewModel.toggleCertificatesEnabled(enabled) },
                        onToggleDemoCertificateEnabled = { enabled -> viewModel.toggleDemoCertificateEnabled(enabled) },
                        onPreviewDemoCertificate = { viewModel.previewDemoCertificate() },
                        onDownloadDemoCertificatePdf = {
                            startDownloadCertificatePdf(viewModel.createDemoCertificateRecord())
                        },
                        onDownloadCertificatePdf = startDownloadCertificatePdf
                    )
                }
            }
                        }

                        // Floating Notification Toast (inside BoxScope)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = notification != null,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                        ) {
                            notification?.let { notif ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (notif.isError) DangerRed else NavyPrimary)
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = notif.message,
                                        color = if (notif.isError) Color.White else GoldMetallic,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    // ================== DIALOGS ==================

    // Quiz Dialog
    activeQuizClass?.let { qClass ->
        QuizDialog(
            courseClass = qClass,
            passingScorePercent = adminSettings.quizPassingScore,
            onDismiss = { viewModel.activeQuizClass.value = null },
            onSubmitScore = { score -> viewModel.submitQuiz(qClass.id, score) }
        )
    }

    // Certificate Preview Dialog
    previewCert?.let { cert ->
        CertificatePreviewDialog(
            record = cert,
            onDismiss = { viewModel.previewCertificate.value = null },
            onOpenPdf = { viewModel.openOrShareCertificate(cert) },
            onVerifyClick = { certId ->
                viewModel.previewCertificate.value = null
                viewModel.searchCertificate(certId)
                viewModel.navigateTo(Screen.CertificateVerification)
            },
            onDownloadPdf = { startDownloadCertificatePdf(cert) }
        )
    }

    // Admin Governance Settings Dialog
    if (showAdminSettings) {
        AdminSettingsDialog(
            initialSettings = adminSettings,
            allModules = modules,
            currentManifest = publishManifest,
            cloudAuthEmail = cloudAuthUser?.email,
            activeAdminId = currentUser.id,
            adminSummaries = adminSummaries,
            latestInviteNotice = latestCoAdminInviteNotice,
            latestRecoveryKeyNotice = latestRecoveryKeyNotice,
            onGenerateCoAdminInvite = { targetId -> viewModel.generateCoAdminSetupInvite(targetId) },
            onChangeOwnPassword = { curr, next, conf -> viewModel.changeCurrentAdminPassword(curr, next, conf) },
            onDismissSecurityNotices = {
                viewModel.dismissCoAdminInviteNotice()
                viewModel.dismissRecoveryKeyNotice()
            },
            onConnectGoogleAdminClick = triggerGoogleSignIn,
            onDismiss = { viewModel.showAdminSettingsDialog.value = false },
            onSave = { newSettings -> viewModel.updateAdminSettings(newSettings) },
            onFastTrackDemo = {
                viewModel.demoFastTrackCompletion()
                viewModel.showAdminSettingsDialog.value = false
            },
            onPublishToCloud = { tag, notes, announcement ->
                viewModel.adminPublishCurriculum(tag, notes, announcement)
            },
            onPublishToCloudWithApk = { tag, notes, announcement, apkCode, apkName, apkUrl ->
                viewModel.adminPublishCurriculum(
                    newVersionTag = tag,
                    changeNotes = notes,
                    announcement = announcement,
                    latestApkVersionCode = apkCode,
                    latestApkVersionName = apkName,
                    apkDownloadUrl = apkUrl
                )
            },
            onOpenMembersManagement = {
                viewModel.showAdminSettingsDialog.value = false
                viewModel.setTab(NavTab.MEMBERS)
            }
        )
    }

    // Revocation Dialog
    revokeTargetCert?.let { targetCert ->
        RevokeCertificateDialog(
            certificate = targetCert,
            onDismiss = { viewModel.revokeTargetCert.value = null },
            onConfirm = { reason -> viewModel.revokeCertificate(targetCert.certificateId, reason) }
        )
    }

    // Admin Login Dialog
    if (showAdminLogin) {
        AdminLoginDialog(
            adminSummaries = adminSummaries,
            isAnyAdminConfigured = isAnyAdminConfigured,
            cloudAuthEmail = cloudAuthUser?.email,
            recoveryKeyNotice = latestRecoveryKeyNotice,
            onDismiss = { viewModel.showAdminLoginDialog.value = false },
            onSignIn = { adminId, password -> viewModel.authenticateAdmin(adminId, password) },
            onSetupPassword = { adminId, newPw, confirmPw, invite ->
                viewModel.setupInitialAdminPassword(adminId, newPw, confirmPw, invite)
            },
            onResetPassword = { adminId, recoveryCode, newPw, confirmPw ->
                viewModel.resetAdminPasswordWithVerification(adminId, recoveryCode, newPw, confirmPw)
            },
            onTriggerGoogleSignIn = triggerGoogleSignIn,
            onDismissRecoveryNotice = { viewModel.dismissRecoveryKeyNotice() }
        )
    }

    // Create Module Dialog
    if (showCreateModule) {
        CreateEditModuleDialog(
            initialModule = null,
            onDismiss = { viewModel.showCreateModuleDialog.value = false },
            onSave = { title, desc, cat, hours ->
                viewModel.addModule(title, desc, cat, hours)
            }
        )
    }

    // Edit Module Dialog
    editingModule?.let { mod ->
        CreateEditModuleDialog(
            initialModule = mod,
            onDismiss = { viewModel.editingModule.value = null },
            onSave = { title, desc, cat, hours ->
                viewModel.updateModule(mod.id, title, desc, cat, hours)
            }
        )
    }

    // Create Class Dialog
    if (showCreateClass) {
        CreateEditClassDialog(
            initialClass = null,
            moduleId = activeTargetModuleForClass ?: (modules.firstOrNull()?.id ?: "mod-1"),
            onDismiss = { viewModel.showCreateClassDialog.value = false },
            onSave = { title, summary, duration, objectives ->
                viewModel.addClass(
                    moduleId = activeTargetModuleForClass ?: (modules.firstOrNull()?.id ?: "mod-1"),
                    title = title,
                    summary = summary,
                    duration = duration,
                    objectives = objectives
                )
            }
        )
    }

    // Edit Class Dialog
    editingClass?.let { cls ->
        CreateEditClassDialog(
            initialClass = cls,
            moduleId = cls.moduleId,
            onDismiss = { viewModel.editingClass.value = null },
            onSave = { title, summary, duration, objectives ->
                viewModel.updateClass(cls.id, title, summary, duration, objectives)
            }
        )
    }

    // Upload Media Dialog (Multi-file, Multi-format)
    val currentUploadMediaType = activeUploadMediaType
    val currentUploadClassId = activeUploadClassId
    if (currentUploadMediaType != null && currentUploadClassId != null) {
        val targetClass = classes.find { it.id == currentUploadClassId }
        UploadMediaDialog(
            initialMediaType = currentUploadMediaType,
            classTitle = targetClass?.title ?: "Class Lesson",
            uploadProgress = uploadProgress,
            uploadByteStatus = uploadByteStatus,
            uploadError = uploadError,
            onDismiss = {
                viewModel.clearUploadError()
                activeUploadMediaType = null
                activeUploadClassId = null
            },
            onUploadMultipleFiles = { pendingItems ->
                viewModel.uploadMultipleMediaFromPhone(
                    classId = currentUploadClassId,
                    pendingItems = pendingItems
                ) { success ->
                    if (success) {
                        activeUploadMediaType = null
                        activeUploadClassId = null
                    }
                }
            },
            onAttachRemoteUrl = { mediaType, remoteUrl, title, description ->
                viewModel.attachRemoteMediaUrl(
                    classId = currentUploadClassId,
                    mediaType = mediaType,
                    remoteUrl = remoteUrl,
                    title = title,
                    description = description
                ) { success ->
                    if (success) {
                        activeUploadMediaType = null
                        activeUploadClassId = null
                    }
                }
            },
            onUploadPreset = { mediaType, fileName, fileSizeBytes, title, description ->
                viewModel.uploadMedia(
                    classId = currentUploadClassId,
                    mediaType = mediaType,
                    fileName = fileName,
                    fileSizeBytes = fileSizeBytes,
                    title = title,
                    description = description
                )
                activeUploadMediaType = null
                activeUploadClassId = null
            },
            onLoadDemoPreset = {
                viewModel.loadSampleMedia(currentUploadClassId)
                activeUploadMediaType = null
                activeUploadClassId = null
            },
            onRetry = {
                viewModel.clearUploadError()
            }
        )
    }

    // In-App Multi-Page PDF Reader Dialog
    previewPdfMedia?.let { pdfItem ->
        val activePdfClassId = (currentScreen as? Screen.ClassDetail)?.classId
        val activePdfClass = activePdfClassId?.let { id -> classes.find { it.id == id } }
        val activePdfProg = activePdfClassId?.let { id -> studentProgressMap[id] }
        val canDownloadPdf = activePdfClass == null || viewModel.canManageModule(activePdfClass.moduleId)
        PdfPreviewDialog(
            media = pdfItem,
            onResolvePdfFile = { media -> viewModel.resolveLocalMediaFile(media) },
            onOpenExternal = { media -> viewModel.openMediaWithExternalViewer(media) },
            onDownload = { media ->
                pendingDownloadMedia = media
                exportDocumentLauncher.launch(media.fileName)
            },
            onDismiss = { viewModel.previewPdfMedia.value = null },
            initialPage = (activePdfProg?.lastPdfPage ?: 1).coerceAtLeast(1),
            canDownload = canDownloadPdf,
            onPageStudyTick = { curPage, totPages, activeDelta, idleDelta ->
                if (activePdfClassId != null) {
                    viewModel.recordPdfPageStudy(
                        classId = activePdfClassId,
                        currentPage = curPage,
                        totalPages = totPages,
                        secondsReading = activeDelta,
                        explicitConfirm = false,
                        idleExcludedSeconds = idleDelta
                    )
                }
            }
        )
    }

    // In-App Fullscreen Image Viewer Dialog
    previewImageMedia?.let { imgItem ->
        val activeImgClassId = (currentScreen as? Screen.ClassDetail)?.classId
        val activeImgClass = activeImgClassId?.let { id -> classes.find { it.id == id } }
        val canDownloadImg = activeImgClass == null || viewModel.canManageModule(activeImgClass.moduleId)
        ImagePreviewDialog(
            media = imgItem,
            onResolveImageFile = { media -> viewModel.resolveLocalMediaFile(media) },
            onDownload = { media ->
                pendingDownloadMedia = media
                exportDocumentLauncher.launch(media.fileName)
            },
            onDismiss = { viewModel.previewImageMedia.value = null },
            canDownload = canDownloadImg
        )
    }

    // In-App Fullscreen Video Player Dialog
    previewVideoMedia?.let { videoItem ->
        val activeVideoClassId = (currentScreen as? Screen.ClassDetail)?.classId
        val activeVideoClass = activeVideoClassId?.let { id -> classes.find { it.id == id } }
        val canDownloadVideo = activeVideoClass == null || viewModel.canManageModule(activeVideoClass.moduleId)
        VideoPreviewDialog(
            media = videoItem,
            onResolveVideoFile = { media -> viewModel.resolveLocalMediaFile(media) },
            onOpenExternal = { media -> viewModel.openMediaWithExternalViewer(media) },
            onDownload = { media ->
                pendingDownloadMedia = media
                exportDocumentLauncher.launch(media.fileName)
            },
            onDismiss = { viewModel.previewVideoMedia.value = null },
            canDownload = canDownloadVideo,
            onPlaybackProgress = { posMs, durMs, isPlayingNow ->
                if (activeVideoClassId != null) {
                    viewModel.updateVideoPlaybackProgress(
                        classId = activeVideoClassId,
                        positionMs = posMs,
                        durationMs = durMs,
                        isPlaying = isPlayingNow
                    )
                }
            }
        )
    }

    // In-App Document Reader / Viewer Dialog
    previewDocumentMedia?.let { docItem ->
        val activeDocClassId = (currentScreen as? Screen.ClassDetail)?.classId
        val activeDocClass = activeDocClassId?.let { id -> classes.find { it.id == id } }
        val canDownloadDoc = activeDocClass == null || viewModel.canManageModule(activeDocClass.moduleId)
        DocumentPreviewDialog(
            media = docItem,
            onResolveDocumentFile = { media -> viewModel.resolveLocalMediaFile(media) },
            onOpenExternal = { media -> viewModel.openMediaWithExternalViewer(media) },
            onDownload = { media ->
                pendingDownloadMedia = media
                exportDocumentLauncher.launch(media.fileName)
            },
            onDismiss = { viewModel.previewDocumentMedia.value = null },
            canDownload = canDownloadDoc
        )
    }

    // Add / Edit ISCEB Member Dialog
    if (showAddMemberDialog || editingMember != null) {
        AddEditMemberDialog(
            initialMember = editingMember,
            nextDisplayOrder = (iscebMembers.maxOfOrNull { it.displayOrder } ?: 0) + 1,
            onDismiss = {
                viewModel.showAddMemberDialog.value = false
                viewModel.editingMember.value = null
            },
            onSave = { member, pickedUri, clearExisting ->
                viewModel.saveIscebMember(member, pickedUri, clearExisting)
            }
        )
    }

    // Delete Confirmation Dialog
    deleteConfirmation?.let { (description, id) ->
        DeleteConfirmDialog(
            targetDescription = description,
            onDismiss = { viewModel.deleteConfirmation.value = null },
            onConfirm = {
                if (id == "RESET_DEMO") {
                    viewModel.resetDemoData()
                    viewModel.deleteConfirmation.value = null
                } else if (id.startsWith("mod-")) {
                    viewModel.deleteModule(id)
                } else if (id.startsWith("member-")) {
                    viewModel.deleteIscebMember(id)
                } else {
                    viewModel.deleteClass(id)
                }
            }
        )
    }

    // Firebase Architecture Guide Dialog
    if (showFirebaseGuide) {
        FirebaseGuideDialog(
            onDismiss = { viewModel.showFirebaseGuideDialog.value = false }
        )
    }

    // Active Exam Dialog
    activeExam?.let { currentExam ->
        val attemptsUsed = viewModel.getStudentExamAttemptsCount(currentExam.id)
        ExamDialog(
            exam = currentExam,
            timeRemainingSeconds = examTimeRemainingSeconds,
            currentAnswers = examCurrentAnswers,
            attemptResult = activeExamResult,
            isSubmitted = isExamSubmitted,
            onSelectAnswer = { qId, optIdx -> viewModel.selectExamAnswer(qId, optIdx) },
            onSubmit = { viewModel.submitActiveExam(isAutoTimeout = false) },
            onRetake = { viewModel.retakeExam(currentExam) },
            onDismiss = { viewModel.closeExam() },
            attemptsUsed = attemptsUsed,
            canRetake = attemptsUsed < currentExam.maxAttempts
        )
    }

    // Active Assignment Dialog
    activeAssignment?.let { currentAsg ->
        val sub = assignmentSubmissions.find { it.assignmentId == currentAsg.id && it.studentId == currentUser.id }
        val canGradeMod = viewModel.canManageModule(currentAsg.moduleId)
        val staffSubs = if (canGradeMod) {
            assignmentSubmissions.filter { it.assignmentId == currentAsg.id }
        } else {
            emptyList()
        }
        AssignmentDialog(
            assignment = currentAsg,
            currentSubmission = sub,
            isAdmin = isAdmin,
            onSubmit = { text, fileName -> viewModel.submitAssignment(currentAsg.id, text, fileName) },
            onGrade = { subId, marks, feedback -> viewModel.gradeAssignment(subId, marks, feedback) },
            onDismiss = { viewModel.closeAssignment() },
            canGrade = canGradeMod,
            submissionsForStaff = staffSubs
        )
    }

    // Share & Install Dialog (100% English)
    if (showPhoneInstallDialog) {
        PhoneInstallDialog(
            onDismiss = { showPhoneInstallDialog = false },
            onShowNotification = { msg -> viewModel.showNotification(msg) },
            onCheckUpdates = { viewModel.checkForUpdates(silentOnError = false) },
            onSyncCurriculum = { viewModel.syncCurriculumNow() },
            currentCurriculumVersion = publishManifest.versionTag,
            appUpdateInfo = appUpdateInfo,
            apkDownloadState = apkDownloadState,
            cloudAuthEmail = cloudAuthUser?.email,
            onOpenApkDownloadUrl = openApkDownloadUrl,
            onDownloadLatestApk = { viewModel.downloadLatestApk() },
            onCancelApkDownload = { viewModel.cancelApkDownload() },
            onInstallDownloadedApk = handleInstallDownloadedApk,
            onConnectGoogleSync = triggerGoogleSignIn
        )
    }

    if (showExamsAssignmentsHub) {
        ExamsAssignmentsHubDialog(
            modules = modules,
            exams = moduleExams,
            assignments = moduleAssignments,
            examAttempts = examAttempts,
            assignmentSubmissions = assignmentSubmissions,
            currentUserId = currentUser.id,
            onTakeExam = { ex ->
                showExamsAssignmentsHub = false
                viewModel.openExam(ex)
            },
            onOpenAssignment = { asg ->
                showExamsAssignmentsHub = false
                viewModel.openAssignment(asg)
            },
            onDismiss = { showExamsAssignmentsHub = false }
        )
    }

    if (quickManageExam != null || quickCreateExamModuleId != null) {
        val modId = quickManageExam?.moduleId ?: quickCreateExamModuleId ?: "mod-1"
        ExamQuestionBuilderDialog(
            initialExam = quickManageExam,
            modules = modules,
            defaultModuleId = modId,
            authorUid = currentUser.authUid.ifBlank { currentUser.id },
            onDismiss = {
                quickManageExam = null
                quickCreateExamModuleId = null
            },
            onSave = { updated ->
                viewModel.createOrUpdateModuleExam(updated)
                quickManageExam = null
                quickCreateExamModuleId = null
            },
            onDelete = quickManageExam?.let { ex ->
                {
                    viewModel.deleteModuleExam(ex.id)
                    quickManageExam = null
                }
            }
        )
    }

    if (quickManageAssignment != null || quickCreateAssignmentModuleId != null) {
        val modId = quickManageAssignment?.moduleId ?: quickCreateAssignmentModuleId ?: "mod-1"
        AssignmentBuilderDialog(
            initialAssignment = quickManageAssignment,
            modules = modules,
            defaultModuleId = modId,
            authorUid = currentUser.authUid.ifBlank { currentUser.id },
            authorName = currentUser.fullName,
            onDismiss = {
                quickManageAssignment = null
                quickCreateAssignmentModuleId = null
            },
            onSave = { updated ->
                viewModel.createOrUpdateModuleAssignment(updated)
                quickManageAssignment = null
                quickCreateAssignmentModuleId = null
            },
            onDelete = quickManageAssignment?.let { asg ->
                {
                    viewModel.deleteModuleAssignment(asg.id)
                    quickManageAssignment = null
                }
            }
        )
    }
            }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
