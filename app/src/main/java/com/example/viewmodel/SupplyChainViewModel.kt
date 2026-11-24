package com.example.viewmodel

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.auth.AdminAccountStatus
import com.example.auth.AdminAuthResult
import com.example.auth.AdminAuthSummary
import com.example.auth.authStateFlow
import com.example.data.CloudCatalogSnapshot
import com.example.data.CloudSyncRepository
import com.example.data.OperationType
import com.example.data.SupplyChainRepository
import com.example.data.handleFirestoreError
import com.example.model.AccountStatus
import com.example.model.AdminCompletionSettings
import com.example.model.ApkDownloadState
import com.example.model.AppNotification
import com.example.model.AppUpdateInfo
import com.example.model.AssignmentSubmission
import com.example.model.AuditLogEntry
import com.example.model.CertificateEligibility
import com.example.model.CertificateRecord
import com.example.model.CertificationEligibilityStatus
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.CoursePart
import com.example.model.CurriculumPublishManifest
import com.example.model.DeveloperProfile
import com.example.model.ExamAttempt
import com.example.model.FounderProfile
import com.example.model.IscebMember
import com.example.model.LessonMedia
import com.example.model.MediaType
import com.example.model.ModuleAssignment
import com.example.model.ModuleExam
import com.example.model.PendingUploadItem
import com.example.model.StudentClassProgress
import com.example.model.SubmissionStatus
import com.example.model.SyncStatus
import com.example.model.UserProfile
import com.example.model.UserProgress
import com.example.model.UserRole
import com.example.util.ApkDownloadOutcome
import com.example.util.ApkInstallLaunchResult
import com.example.util.ApkUpdateManager
import com.example.util.CertificatePdfGenerator
import com.example.util.MediaFileManager
import kotlinx.coroutines.withContext
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class Screen {
    object Main : Screen()
    data class ModuleDetail(val moduleId: String) : Screen()
    data class ClassDetail(val classId: String) : Screen()
    object CertificateVerification : Screen()
    object FounderProfileScreenRoute : Screen()
    object DeveloperProfileScreenRoute : Screen()
    data class MemberDetail(val memberId: String) : Screen()
    object TeacherPanel : Screen()
    object AdminPanel : Screen()
}

enum class NavTab {
    HOME,
    MODULES,
    MY_LEARNING,
    MEMBERS,
    DEVELOPER_PROFILE,
    PROFILE
}

class SupplyChainViewModel(application: Application) : AndroidViewModel(application) {

    private val cloudSyncRepository: CloudSyncRepository? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        try {
            CloudSyncRepository(application.applicationContext)
        } catch (_: Exception) {
            null
        }
    }

    private val repository = SupplyChainRepository(
        context = application.applicationContext,
        cloudSyncRepository = cloudSyncRepository,
        autoLoad = false
    )

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    val currentUser: StateFlow<UserProfile> = repository.currentUser
    val availableUsers: List<UserProfile> = repository.availableUsers
    val adminUsers: List<UserProfile> = repository.availableUsers.filter { it.role == UserRole.ADMIN }
    val registeredAccounts: StateFlow<List<UserProfile>> = repository.registeredAccounts
    val hasSignedInAccount: StateFlow<Boolean> = repository.hasSignedInAccount
    val auditLogs: StateFlow<List<AuditLogEntry>> = repository.auditLogs
    val developerProfile: StateFlow<DeveloperProfile> = repository.developerProfile
    val founderProfile: StateFlow<FounderProfile> = repository.founderProfile
    val iscebMembers: StateFlow<List<IscebMember>> = repository.iscebMembers
    val modules: StateFlow<List<CourseModule>> = repository.modules
    val classes: StateFlow<List<CourseClass>> = repository.classes
    val progress: StateFlow<UserProgress> = repository.progress
    val studentProgressMap: StateFlow<Map<String, StudentClassProgress>> = repository.studentProgressMap
    val adminSettings: StateFlow<AdminCompletionSettings> = repository.adminSettings
    val issuedCertificates: StateFlow<List<CertificateRecord>> = repository.issuedCertificates
    val isAdminLoggedIn: StateFlow<Boolean> = repository.isAdminLoggedIn

    val isTeacher: StateFlow<Boolean> = combine(currentUser, isAdminLoggedIn) { user, admin ->
        !admin && user.role == UserRole.TEACHER && user.accountStatus == AccountStatus.ACTIVE
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isStaff: StateFlow<Boolean> = combine(isAdminLoggedIn, isTeacher) { admin, teacher ->
        admin || teacher
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun canManageModule(moduleId: String): Boolean {
        if (isAdminLoggedIn.value) return true
        val user = currentUser.value
        return user.role == UserRole.TEACHER &&
            user.accountStatus == AccountStatus.ACTIVE &&
            user.assignedModuleIds.contains(moduleId)
    }

    // Assessments
    val moduleExams: StateFlow<List<ModuleExam>> = repository.moduleExams
    val examAttempts: StateFlow<List<ExamAttempt>> = repository.examAttempts
    val moduleAssignments: StateFlow<List<ModuleAssignment>> = repository.moduleAssignments
    val assignmentSubmissions: StateFlow<List<AssignmentSubmission>> = repository.assignmentSubmissions

    // Cloud Curriculum Synchronization & Versioning
    val publishManifest: StateFlow<CurriculumPublishManifest> = repository.publishManifest
    val pendingUpdateManifest: StateFlow<CurriculumPublishManifest?> = repository.pendingUpdateManifest
    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus
    val lastSyncTimestamp: StateFlow<Long> = repository.lastSyncTimestamp
    val appUpdateInfo: StateFlow<AppUpdateInfo> = repository.appUpdateInfo

    private val apkUpdateManager = ApkUpdateManager(application.applicationContext)
    private val _apkDownloadState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)
    val apkDownloadState: StateFlow<ApkDownloadState> = _apkDownloadState.asStateFlow()
    private var apkDownloadJob: Job? = null

    // Firebase Google Auth state (gates Firestore snapshot listeners & writes)
    private val _cloudAuthUser = MutableStateFlow<FirebaseUser?>(null)
    val cloudAuthUser: StateFlow<FirebaseUser?> = _cloudAuthUser.asStateFlow()

    private val _cloudSyncError = MutableStateFlow<String?>(null)
    val cloudSyncError: StateFlow<String?> = _cloudSyncError.asStateFlow()

    private var cloudListenerJob: Job? = null
    private var staffListenerJob: Job? = null
    private var lastAutoResumeCheckMs: Long = System.currentTimeMillis()
    private var lastObservedManifest: CurriculumPublishManifest? = null
    private var lastObservedCatalog: CloudCatalogSnapshot? = null

    // Active Exam Session
    val activeExam = MutableStateFlow<ModuleExam?>(null)
    val examTimeRemainingSeconds = MutableStateFlow(0)
    val examCurrentAnswers = MutableStateFlow<Map<String, Int>>(emptyMap())
    val activeExamResult = MutableStateFlow<ExamAttempt?>(null)
    val isExamSubmitted = MutableStateFlow(false)
    private var examTimerJob: Job? = null

    // Active Assignment Dialog
    val activeAssignment = MutableStateFlow<ModuleAssignment?>(null)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Main)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _navigationStack = mutableListOf<Screen>()

    private val _currentTab = MutableStateFlow(NavTab.HOME)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _notification = MutableStateFlow<AppNotification?>(null)
    val notification: StateFlow<AppNotification?> = _notification.asStateFlow()

    // Audio Playback State (backed by real android.media.MediaPlayer)
    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    private val _audioCurrentClassId = MutableStateFlow<String?>(null)
    val audioCurrentClassId: StateFlow<String?> = _audioCurrentClassId.asStateFlow()

    private val _audioProgressMs = MutableStateFlow(0L)
    val audioProgressMs: StateFlow<Long> = _audioProgressMs.asStateFlow()

    private val _audioDurationMs = MutableStateFlow(18 * 60 * 1000L)
    val audioDurationMs: StateFlow<Long> = _audioDurationMs.asStateFlow()

    private val _audioSpeed = MutableStateFlow(1.0f)
    val audioSpeed: StateFlow<Float> = _audioSpeed.asStateFlow()

    private var audioTimerJob: Job? = null
    private var audioLoadJob: Job? = null
    private var audioMediaPlayer: MediaPlayer? = null
    private var audioPreparedClassId: String? = null
    private var audioPreparedSourceKey: String? = null

    // Video Playback State (synced with real VideoView playback)
    private val _isPlayingVideo = MutableStateFlow(false)
    val isPlayingVideo: StateFlow<Boolean> = _isPlayingVideo.asStateFlow()

    private val _videoPositionMs = MutableStateFlow(0L)
    val videoPositionMs: StateFlow<Long> = _videoPositionMs.asStateFlow()

    private val _videoDurationMs = MutableStateFlow(12 * 60 * 1000L + 30 * 1000L)
    val videoDurationMs: StateFlow<Long> = _videoDurationMs.asStateFlow()

    private var videoTimerJob: Job? = null
    private var lastVideoActiveRecordedSecMs: Long = 0L

    // Upload state
    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _uploadingTarget = MutableStateFlow<String?>(null)
    val uploadingTarget: StateFlow<String?> = _uploadingTarget.asStateFlow()

    private val _uploadByteStatus = MutableStateFlow<String?>(null)
    val uploadByteStatus: StateFlow<String?> = _uploadByteStatus.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    // Dialogs & Modals
    val showAdminLoginDialog = MutableStateFlow(false)
    val showCreateModuleDialog = MutableStateFlow(false)
    val editingModule = MutableStateFlow<CourseModule?>(null)
    val showCreateClassDialog = MutableStateFlow(false)
    val editingClass = MutableStateFlow<CourseClass?>(null)
    val deleteConfirmation = MutableStateFlow<Pair<String, String>?>(null)
    val showFirebaseGuideDialog = MutableStateFlow(false)
    val showAdminSettingsDialog = MutableStateFlow(false)
    val activeQuizClass = MutableStateFlow<CourseClass?>(null)
    val previewCertificate = MutableStateFlow<CertificateRecord?>(null)
    val revokeTargetCert = MutableStateFlow<CertificateRecord?>(null)
    val showAddMemberDialog = MutableStateFlow(false)
    val editingMember = MutableStateFlow<IscebMember?>(null)
    val previewPdfMedia = MutableStateFlow<LessonMedia?>(null)
    val previewImageMedia = MutableStateFlow<LessonMedia?>(null)
    val previewVideoMedia = MutableStateFlow<LessonMedia?>(null)
    val previewDocumentMedia = MutableStateFlow<LessonMedia?>(null)

    // Cryptographic Dual-Admin Auth State (Md. Amirul Islam first, Md. Nafiul Islam Nahin second)
    private val _adminSummaries = MutableStateFlow<List<AdminAuthSummary>>(emptyList())
    val adminSummaries: StateFlow<List<AdminAuthSummary>> = _adminSummaries.asStateFlow()

    private val _isAnyAdminConfigured = MutableStateFlow(false)
    val isAnyAdminConfigured: StateFlow<Boolean> = _isAnyAdminConfigured.asStateFlow()

    private val _latestRecoveryKeyNotice = MutableStateFlow<String?>(null)
    val latestRecoveryKeyNotice: StateFlow<String?> = _latestRecoveryKeyNotice.asStateFlow()

    private val _latestCoAdminInviteNotice = MutableStateFlow<String?>(null)
    val latestCoAdminInviteNotice: StateFlow<String?> = _latestCoAdminInviteNotice.asStateFlow()

    // Verification search state
    val verificationSearchQuery = MutableStateFlow("")
    val verificationResult = MutableStateFlow<CertificateRecord?>(null)
    val isVerificationSearched = MutableStateFlow(false)

    // Certificate Eligibility (lightweight initial state so UI renders instantly before IO init completes)
    private val emptyEligibility = CertificateEligibility(
        isEligible = false,
        completedRequiredCount = 0,
        totalRequiredCount = 0
    )
    val certificateEligibility: StateFlow<CertificateEligibility> = combine(
        classes,
        studentProgressMap,
        currentUser,
        examAttempts,
        assignmentSubmissions
    ) { clsList, _, user, _, _ ->
        if (clsList.isEmpty()) emptyEligibility
        else repository.evaluateCertificateEligibility(user.id)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyEligibility
    )

    // Filtered classes
    val filteredClasses: StateFlow<List<CourseClass>> = combine(
        classes,
        _searchQuery,
        _selectedFilter
    ) { allCls, query, filter ->
        allCls.filter { cls ->
            val matchesQuery = query.isBlank() ||
                cls.title.contains(query, ignoreCase = true) ||
                cls.summary.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "Part 1: Basic" -> {
                    val mod = modules.value.find { it.id == cls.moduleId }
                    mod?.part == CoursePart.PART_1_BASIC
                }
                "Part 2: Advance" -> {
                    val mod = modules.value.find { it.id == cls.moduleId }
                    mod?.part == CoursePart.PART_2_ADVANCE
                }
                "Completed" -> cls.isCompleted
                "In Progress" -> !cls.isCompleted && cls.hasAnyMedia
                "With Media" -> cls.hasAnyMedia
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall Progress Ratio
    val overallProgressRatio: StateFlow<Float> = combine(classes, progress) { clsList, prog ->
        if (clsList.isEmpty()) 0f else prog.completedClassIds.size.toFloat() / clsList.size.toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.initializeIfNeeded()
                refreshAdminAuthState()
            } finally {
                _isInitializing.value = false
            }

            try {
                Firebase.auth.authStateFlow().collect { user ->
                    _cloudAuthUser.value = user
                    cloudListenerJob?.cancel()
                    cloudListenerJob = null
                    _cloudSyncError.value = null
                    if (user != null) {
                        startAuthenticatedCloudListeners(user.uid)
                    }
                }
            } catch (_: Exception) {
                // Keep app running offline-first if FirebaseAuth is unavailable in tests
            }
        }
    }

    private fun startAuthenticatedCloudListeners(uid: String) {
        val cloudRepo = cloudSyncRepository ?: return
        cloudListenerJob = viewModelScope.launch {
            launch {
                cloudRepo.observeLiveManifest()
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        handleFirestoreError(rootError, OperationType.GET, "curriculum_manifests/live")
                        _cloudSyncError.value = rootError.message ?: "Failed to observe live manifest"
                    }
                    .collect { remoteManifest ->
                        if (remoteManifest != null) {
                            lastObservedManifest = remoteManifest
                            val applied = repository.applyRemoteManifestAndCatalog(
                                manifest = remoteManifest,
                                catalog = lastObservedCatalog,
                                isWorkInProgress = isWorkInProgress(),
                                sourceLabel = "Firebase Cloud Firestore"
                            )
                            if (applied && !isWorkInProgress() && adminSettings.value.autoApplyContentUpdates) {
                                showNotification("🔄 Live Cloud Update synced (${remoteManifest.versionTag})")
                            } else if (applied && pendingUpdateManifest.value != null) {
                                showNotification("🔔 New ISCEB Update (${remoteManifest.versionTag}) ready — tap Sync Now when finished.")
                            }
                        }
                    }
            }

            launch {
                cloudRepo.observeLiveCatalog()
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        handleFirestoreError(rootError, OperationType.GET, "curriculum_catalogs/live")
                        _cloudSyncError.value = rootError.message ?: "Failed to observe live catalog"
                    }
                    .collect { catalog ->
                        if (catalog != null) {
                            lastObservedCatalog = catalog
                            val manifestToApply = lastObservedManifest ?: publishManifest.value.copy(
                                versionCode = catalog.versionCode
                            )
                            repository.applyRemoteManifestAndCatalog(
                                manifest = manifestToApply,
                                catalog = catalog,
                                isWorkInProgress = isWorkInProgress(),
                                sourceLabel = "Firebase Cloud Firestore"
                            )
                        }
                    }
            }

            launch {
                cloudRepo.observeUserAccount(uid)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                    }
                    .collect { remoteAccount ->
                        if (remoteAccount != null) {
                            repository.mergeCloudUserAccount(remoteAccount)
                            if (remoteAccount.role == UserRole.TEACHER || remoteAccount.role == UserRole.ADMIN) {
                                ensureStaffCloudListeners(includeAdminLogs = remoteAccount.role == UserRole.ADMIN || isAdminLoggedIn.value)
                            }
                        }
                    }
            }

            launch {
                cloudRepo.observeUserProgress(uid)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        handleFirestoreError(rootError, OperationType.LIST, "users/$uid/progress")
                        _cloudSyncError.value = rootError.message ?: "Failed to sync student progress"
                    }
                    .collect { cloudProgress ->
                        repository.mergeCloudStudentProgress(cloudProgress)
                    }
            }

            launch {
                cloudRepo.observeAssignments()
                    .catch { if (it is CancellationException) throw it }
                    .collect { remote -> if (remote.isNotEmpty()) repository.mergeCloudAssignments(remote) }
            }

            launch {
                cloudRepo.observeExams()
                    .catch { if (it is CancellationException) throw it }
                    .collect { remote -> if (remote.isNotEmpty()) repository.mergeCloudExams(remote) }
            }

            launch {
                cloudRepo.observeUserSubmissions(uid)
                    .catch { if (it is CancellationException) throw it }
                    .collect { remote -> if (remote.isNotEmpty()) repository.mergeCloudSubmissions(remote) }
            }

            launch {
                cloudRepo.observeUserExamAttempts(uid)
                    .catch { if (it is CancellationException) throw it }
                    .collect { remote -> if (remote.isNotEmpty()) repository.mergeCloudExamAttempts(remote) }
            }

            launch {
                cloudRepo.observeUserCertificates(uid)
                    .catch { if (it is CancellationException) throw it }
                    .collect { remote -> if (remote.isNotEmpty()) repository.mergeCloudCertificates(remote) }
            }

            if (isAdminLoggedIn.value || currentUser.value.role == UserRole.TEACHER) {
                ensureStaffCloudListeners(includeAdminLogs = isAdminLoggedIn.value)
            }
        }
    }

    private fun ensureStaffCloudListeners(includeAdminLogs: Boolean) {
        val cloudRepo = cloudSyncRepository ?: return
        if (cloudAuthUser.value == null) return
        if (staffListenerJob?.isActive == true && !includeAdminLogs) return
        staffListenerJob?.cancel()
        staffListenerJob = viewModelScope.launch {
            launch {
                cloudRepo.observeAllUserAccounts()
                    .catch { if (it is CancellationException) throw it }
                    .collect { users -> repository.mergeCloudAllUsers(users) }
            }
            launch {
                cloudRepo.observeAllSubmissions()
                    .catch { if (it is CancellationException) throw it }
                    .collect { subs -> repository.mergeCloudSubmissions(subs) }
            }
            launch {
                cloudRepo.observeAllExamAttempts()
                    .catch { if (it is CancellationException) throw it }
                    .collect { attempts -> repository.mergeCloudExamAttempts(attempts) }
            }
            launch {
                cloudRepo.observeAllCertificates()
                    .catch { if (it is CancellationException) throw it }
                    .collect { certs -> repository.mergeCloudCertificates(certs) }
            }
            if (includeAdminLogs) {
                launch {
                    cloudRepo.observeAuditLogs()
                        .catch { if (it is CancellationException) throw it }
                        .collect { logs -> repository.mergeCloudAuditLogs(logs) }
                }
            }
        }
    }

    fun isWorkInProgress(): Boolean {
        val inActiveExam = activeExam.value != null && !isExamSubmitted.value
        val isUploadingMedia = _uploadProgress.value != null
        return inActiveExam || isUploadingMedia
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            _navigationStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun handleBack(): Boolean {
        return if (_navigationStack.isNotEmpty()) {
            _currentScreen.value = _navigationStack.removeAt(_navigationStack.lastIndex)
            true
        } else if (_currentScreen.value !is Screen.Main) {
            _currentScreen.value = Screen.Main
            true
        } else {
            false
        }
    }

    fun setTab(tab: NavTab) {
        _currentTab.value = tab
        if (_currentScreen.value !is Screen.Main) {
            _currentScreen.value = Screen.Main
            _navigationStack.clear()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun refreshAdminAuthState() {
        _adminSummaries.value = repository.adminAuthManager.getAdminSummaries()
        _isAnyAdminConfigured.value = repository.adminAuthManager.isAnyAdminConfigured()
    }

    fun dismissRecoveryKeyNotice() {
        _latestRecoveryKeyNotice.value = null
    }

    fun dismissCoAdminInviteNotice() {
        _latestCoAdminInviteNotice.value = null
    }

    suspend fun resolveLocalMediaFile(media: LessonMedia): File? {
        return MediaFileManager.ensureLocalMediaFile(getApplication(), media, cloudSyncRepository)
    }

    // Account & Role Management (Genuine Student, Teacher & Admin accounts)
    fun registerOrSignInStudentAccount(
        fullName: String,
        email: String,
        organization: String = "ISCEB Enrolled Professional",
        designation: String = "PGD Candidate",
        mobileNumber: String = "",
        enrolledProgram: String = "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)",
        pickedPhotoUri: Uri? = null
    ) {
        if (fullName.isBlank() || email.isBlank() || !email.contains("@")) {
            showNotification("Enter your full name and a valid email address.", isError = true)
            return
        }
        stopAudioPlayback()
        stopVideoPlayback()
        closeExam()
        viewModelScope.launch(Dispatchers.IO) {
            var photoPath = ""
            if (pickedPhotoUri != null) {
                photoPath = MediaFileManager.copyProfilePhotoToInternalStorage(
                    getApplication(),
                    pickedPhotoUri,
                    prefix = "student_avatar"
                )?.absolutePath.orEmpty()
            }
            val account = repository.signInOrRegisterStudentAccount(
                fullName = fullName,
                email = email,
                authUid = cloudAuthUser.value?.uid.orEmpty(),
                organization = organization,
                designation = designation,
                mobileNumber = mobileNumber,
                enrolledProgram = enrolledProgram,
                photoUrl = photoPath
            )
            if (cloudAuthUser.value != null) {
                cloudSyncRepository?.upsertUserAccount(account, allowRoleAndPrivileges = false)
            }
            refreshAdminAuthState()
            showNotification("Welcome, ${account.fullName}! Student ID: ${account.studentId}")
        }
    }

    fun updateStudentOwnProfile(
        fullName: String,
        organization: String,
        designation: String,
        mobileNumber: String,
        enrolledProgram: String,
        pickedPhotoUri: Uri? = null
    ) {
        if (currentUser.value.id == "guest-unauthenticated") {
            showNotification("Please sign in with your student account first.", isError = true)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            var newPhotoPath: String? = null
            if (pickedPhotoUri != null) {
                newPhotoPath = MediaFileManager.copyProfilePhotoToInternalStorage(
                    getApplication(),
                    pickedPhotoUri,
                    prefix = "student_${currentUser.value.id.take(10)}"
                )?.absolutePath
            }
            val updated = repository.updateStudentOwnProfile(
                fullName = fullName,
                organization = organization,
                designation = designation,
                mobileNumber = mobileNumber,
                enrolledProgram = enrolledProgram,
                photoUrl = newPhotoPath
            )
            if (updated != null) {
                if (cloudAuthUser.value != null) {
                    cloudSyncRepository?.upsertUserAccount(updated, allowRoleAndPrivileges = false)
                }
                showNotification("Student profile updated.")
            } else {
                showNotification("Profile editing is unavailable for this account.", isError = true)
            }
        }
    }

    fun signOutStudentAccount() {
        stopAudioPlayback()
        stopVideoPlayback()
        closeExam()
        staffListenerJob?.cancel()
        staffListenerJob = null
        repository.signOutAccount()
        refreshAdminAuthState()
        try {
            Firebase.auth.signOut()
        } catch (_: Exception) {
        }
        if (_currentScreen.value is Screen.AdminPanel || _currentScreen.value is Screen.TeacherPanel) {
            _currentScreen.value = Screen.Main
            _navigationStack.clear()
        }
        showNotification("Signed out safely.")
    }

    fun activateTeacherAccount(userId: String) {
        stopAudioPlayback()
        stopVideoPlayback()
        closeExam()
        val ok = repository.activateTeacherSession(userId)
        refreshAdminAuthState()
        if (ok) {
            ensureStaffCloudListeners(includeAdminLogs = false)
            showNotification("Teacher mode active: ${repository.currentUser.value.fullName}")
        } else {
            showNotification("Unable to activate Teacher account.", isError = true)
        }
    }

    fun createOrUpdateTeacherAccount(
        fullName: String,
        email: String,
        designation: String,
        organization: String,
        assignedModuleIds: Set<String>
    ) {
        if (!isAdminLoggedIn.value) {
            showNotification("Only Administrators can create or assign Teacher accounts.", isError = true)
            return
        }
        if (fullName.isBlank() || email.isBlank()) {
            showNotification("Teacher name and email are required.", isError = true)
            return
        }
        val teacher = repository.createOrUpdateTeacherAccount(
            fullName = fullName,
            email = email,
            designation = designation,
            organization = organization,
            assignedModuleIds = assignedModuleIds
        )
        val entry = repository.recordAuditLog(
            actionType = "TEACHER_ROLE_ASSIGNED",
            targetId = teacher.id,
            summary = "Assigned ${assignedModuleIds.size} modules to Teacher ${teacher.fullName} (${teacher.email})"
        )
        viewModelScope.launch {
            if (cloudAuthUser.value != null) {
                cloudSyncRepository?.upsertUserAccount(teacher, allowRoleAndPrivileges = true)
                cloudSyncRepository?.appendAuditLog(entry)
            }
        }
        showNotification("Saved Teacher account: ${teacher.fullName} (${assignedModuleIds.size} modules)")
    }

    fun adminUpdateUserAccess(
        userId: String,
        role: UserRole,
        accountStatus: AccountStatus,
        isVerified: Boolean,
        assignedModuleIds: Set<String>
    ) {
        if (!isAdminLoggedIn.value) {
            showNotification("Admin authentication required to update user roles or status.", isError = true)
            return
        }
        val updated = repository.adminUpdateUserAccess(userId, role, accountStatus, isVerified, assignedModuleIds)
        if (updated == null) {
            showNotification("User update failed or restricted.", isError = true)
            return
        }
        val entry = repository.recordAuditLog(
            actionType = "USER_ACCESS_UPDATED",
            targetId = updated.id,
            summary = "Updated ${updated.fullName}: role=${updated.role.name}, status=${updated.accountStatus.name}, verified=${updated.isVerified}"
        )
        viewModelScope.launch {
            if (cloudAuthUser.value != null) {
                cloudSyncRepository?.upsertUserAccount(updated, allowRoleAndPrivileges = true)
                cloudSyncRepository?.appendAuditLog(entry)
            }
        }
        showNotification("Updated access for ${updated.fullName}.")
    }

    // Account Switcher (Restricted to teacher/student switching when authorized; admins use AdminLoginDialog)
    fun switchUser(userId: String) {
        stopAudioPlayback()
        stopVideoPlayback()
        closeExam()
        val switched = repository.switchUser(userId)
        refreshAdminAuthState()
        if (switched) {
            val user = repository.currentUser.value
            showNotification("Active account: ${user.fullName} (${user.role.name})")
        } else {
            showNotification("Administrator mode requires password authentication.", isError = true)
        }
    }

    // Real Audio Playback with resume position (MediaPlayer + cloud chunk resolution)
    fun playPauseAudio(classId: String) {
        val targetClass = classes.value.find { it.id == classId }
        val audioMedia = targetClass?.audioMedia
        if (audioMedia == null) {
            showNotification("No audio lecture attached to this class.", isError = true)
            return
        }
        val sourceKey = "${audioMedia.id}|${audioMedia.url}|${audioMedia.cloudFileId}"
        val existingPlayer = audioMediaPlayer
        if (existingPlayer != null && audioPreparedClassId == classId && audioPreparedSourceKey == sourceKey) {
            if (_isPlayingAudio.value) {
                runCatching { existingPlayer.pause() }
                _isPlayingAudio.value = false
                audioTimerJob?.cancel()
                val dur = _audioDurationMs.value.coerceAtLeast(1L)
                val pos = runCatching { existingPlayer.currentPosition.toLong() }.getOrDefault(_audioProgressMs.value)
                _audioProgressMs.value = pos.coerceIn(0L, dur)
                val percent = (_audioProgressMs.value.toFloat() / dur.toFloat()) * 100f
                repository.recordActiveLearningTime(classId, 0L, percent, playbackPositionMs = _audioProgressMs.value)
                pushClassProgressToCloud(classId)
            } else {
                applyAudioSpeed(existingPlayer, _audioSpeed.value)
                runCatching { existingPlayer.start() }
                _isPlayingAudio.value = true
                startAudioTicker(classId)
            }
            return
        }

        // Initialize and prepare real MediaPlayer for this class audio
        releaseAudioPlayer()
        audioLoadJob?.cancel()
        audioTimerJob?.cancel()
        _isPlayingAudio.value = false
        _audioCurrentClassId.value = classId

        audioLoadJob = viewModelScope.launch {
            showNotification("Loading audio lecture: ${audioMedia.displayTitle}...")
            val localFile = withContext(Dispatchers.IO) { resolveLocalMediaFile(audioMedia) }
            val trimmedUrl = audioMedia.url.trim()
            val canStreamHttp = trimmedUrl.startsWith("https://", ignoreCase = true) ||
                trimmedUrl.startsWith("http://", ignoreCase = true)

            if (localFile == null && !canStreamHttp) {
                val errMsg = if (audioMedia.cloudFileId.isNotBlank()) {
                    "Unable to download audio lecture. Connect to internet & Google Sync."
                } else {
                    "Audio file is only stored on the uploader's device. Ask Admin to re-upload with Google Cloud Sync or an HTTPS link."
                }
                showNotification(errMsg, isError = true)
                return@launch
            }

            val preparedPlayer = withContext(Dispatchers.IO) {
                try {
                    val mp = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        if (localFile != null && localFile.exists()) {
                            setDataSource(localFile.absolutePath)
                        } else {
                            setDataSource(trimmedUrl)
                        }
                        prepare()
                    }
                    Result.success(mp)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure<MediaPlayer>(e)
                }
            }

            val mp = preparedPlayer.getOrNull()
            if (mp == null) {
                val reason = preparedPlayer.exceptionOrNull()?.localizedMessage ?: "Unsupported audio format"
                showNotification("Cannot play '${audioMedia.displayTitle}': $reason", isError = true)
                return@launch
            }

            val actualDurMs = runCatching { mp.duration.toLong() }.getOrDefault(0L).coerceAtLeast(1000L)
            _audioDurationMs.value = actualDurMs
            val savedResume = studentProgressMap.value[classId]?.lastPlaybackPositionMs
                ?.takeIf { it in 1L until (actualDurMs - 1000L) } ?: 0L
            if (savedResume > 0L) {
                runCatching { mp.seekTo(savedResume.toInt()) }
            }
            _audioProgressMs.value = savedResume

            mp.setOnCompletionListener {
                _isPlayingAudio.value = false
                audioTimerJob?.cancel()
                _audioProgressMs.value = _audioDurationMs.value
                repository.recordActiveLearningTime(
                    classId = classId,
                    secondsToAdd = 0L,
                    playbackPercent = 100f,
                    playbackPositionMs = _audioDurationMs.value
                )
                pushClassProgressToCloud(classId)
            }
            mp.setOnErrorListener { _, what, extra ->
                _isPlayingAudio.value = false
                audioTimerJob?.cancel()
                showNotification("Audio playback error ($what/$extra).", isError = true)
                true
            }

            audioMediaPlayer = mp
            audioPreparedClassId = classId
            audioPreparedSourceKey = sourceKey
            applyAudioSpeed(mp, _audioSpeed.value)
            runCatching { mp.start() }
            _isPlayingAudio.value = true
            startAudioTicker(classId)
        }
    }

    private fun applyAudioSpeed(mp: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val params = PlaybackParams().apply { setSpeed(speed.coerceIn(0.5f, 2.5f)) }
                mp.playbackParams = params
            }
        }
    }

    private fun releaseAudioPlayer() {
        audioPreparedClassId = null
        audioPreparedSourceKey = null
        val mp = audioMediaPlayer
        audioMediaPlayer = null
        if (mp != null) {
            runCatching {
                if (mp.isPlaying) mp.stop()
            }
            runCatching { mp.release() }
        }
    }

    fun seekAudio(targetMs: Long) {
        val dur = _audioDurationMs.value.coerceAtLeast(1L)
        val clamped = targetMs.coerceIn(0L, dur)
        _audioProgressMs.value = clamped
        runCatching { audioMediaPlayer?.seekTo(clamped.toInt()) }
        _audioCurrentClassId.value?.let { classId ->
            val percent = (clamped.toFloat() / dur.toFloat()) * 100f
            repository.recordActiveLearningTime(classId, 0L, percent, playbackPositionMs = clamped)
        }
    }

    fun skipAudioSeconds(seconds: Int) {
        val next = _audioProgressMs.value + (seconds * 1000L)
        seekAudio(next)
    }

    fun setAudioSpeed(speed: Float) {
        _audioSpeed.value = speed
        audioMediaPlayer?.let { mp ->
            val wasPlaying = runCatching { mp.isPlaying }.getOrDefault(false)
            applyAudioSpeed(mp, speed)
            if (!wasPlaying && _isPlayingAudio.value.not()) {
                runCatching { if (mp.isPlaying) mp.pause() }
            }
        }
    }

    private fun startAudioTicker(classId: String) {
        audioTimerJob?.cancel()
        audioTimerJob = viewModelScope.launch {
            var tickCount = 0
            while (_isPlayingAudio.value) {
                delay(500)
                if (!_isPlayingAudio.value) break
                tickCount++
                val dur = _audioDurationMs.value.coerceAtLeast(1L)
                val playerPos = audioMediaPlayer?.let { mp ->
                    runCatching { mp.currentPosition.toLong() }.getOrNull()
                }
                val pos = (playerPos ?: (_audioProgressMs.value + (500 * _audioSpeed.value).toLong())).coerceIn(0L, dur)
                _audioProgressMs.value = pos

                if (tickCount % 2 == 0) {
                    val percent = (pos.toFloat() / dur.toFloat()) * 100f
                    repository.recordActiveLearningTime(
                        classId = classId,
                        secondsToAdd = 1L,
                        playbackPercent = percent,
                        playbackPositionMs = pos
                    )
                }

                val stillPlaying = audioMediaPlayer?.let { mp ->
                    runCatching { mp.isPlaying }.getOrDefault(true)
                } ?: true
                if (!stillPlaying && pos >= dur - 250L) {
                    _isPlayingAudio.value = false
                    pushClassProgressToCloud(classId)
                    break
                }
            }
        }
    }

    private fun stopAudioPlayback() {
        _audioCurrentClassId.value?.let { classId ->
            val dur = _audioDurationMs.value.coerceAtLeast(1L)
            val percent = (_audioProgressMs.value.toFloat() / dur.toFloat()) * 100f
            repository.recordActiveLearningTime(classId, 0L, percent, playbackPositionMs = _audioProgressMs.value)
            pushClassProgressToCloud(classId)
        }
        _isPlayingAudio.value = false
        audioLoadJob?.cancel()
        audioTimerJob?.cancel()
        releaseAudioPlayer()
    }

    // Real Video Playback state synchronized with inline / fullscreen VideoView
    fun playPauseVideo(classId: String) {
        _isPlayingVideo.value = !_isPlayingVideo.value
        if (_isPlayingVideo.value) {
            if (_videoPositionMs.value == 0L) {
                val savedPos = studentProgressMap.value[classId]?.lastPlaybackPositionMs
                    ?.takeIf { it in 1L until (_videoDurationMs.value - 1000L) } ?: 0L
                _videoPositionMs.value = savedPos
            }
            lastVideoActiveRecordedSecMs = System.currentTimeMillis()
        } else {
            videoTimerJob?.cancel()
            val dur = _videoDurationMs.value.coerceAtLeast(1L)
            val percent = (_videoPositionMs.value.toFloat() / dur.toFloat()) * 100f
            repository.recordActiveLearningTime(classId, 0L, percent, playbackPositionMs = _videoPositionMs.value)
            pushClassProgressToCloud(classId)
        }
    }

    fun updateVideoPlaybackProgress(
        classId: String,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean
    ) {
        val safeDur = durationMs.coerceAtLeast(1000L)
        val safePos = positionMs.coerceIn(0L, safeDur)
        _videoDurationMs.value = safeDur
        _videoPositionMs.value = safePos
        if (_isPlayingVideo.value != isPlaying) {
            _isPlayingVideo.value = isPlaying
        }

        val now = System.currentTimeMillis()
        val elapsedSec = if (isPlaying && lastVideoActiveRecordedSecMs > 0L) {
            ((now - lastVideoActiveRecordedSecMs) / 1000L).coerceIn(0L, 5L)
        } else {
            0L
        }
        if (elapsedSec > 0L || !isPlaying) {
            if (isPlaying) {
                lastVideoActiveRecordedSecMs = now
            }
            val percent = (safePos.toFloat() / safeDur.toFloat()) * 100f
            repository.recordActiveLearningTime(
                classId = classId,
                secondsToAdd = elapsedSec,
                playbackPercent = percent,
                playbackPositionMs = safePos
            )
            if (!isPlaying || (safePos > 0L && (safePos / 1000L) % 10L == 0L)) {
                pushClassProgressToCloud(classId)
            }
        }
    }

    fun seekVideo(targetMs: Long, classId: String) {
        val dur = _videoDurationMs.value.coerceAtLeast(1L)
        val clamped = targetMs.coerceIn(0L, dur)
        _videoPositionMs.value = clamped
        val percent = (clamped.toFloat() / dur.toFloat()) * 100f
        repository.recordActiveLearningTime(classId, 0L, percent, playbackPositionMs = clamped)
    }

    private fun stopVideoPlayback() {
        _isPlayingVideo.value = false
        videoTimerJob?.cancel()
        lastVideoActiveRecordedSecMs = 0L
    }

    fun pauseAllActiveStudySessions() {
        stopAudioPlayback()
        if (_isPlayingVideo.value) {
            stopVideoPlayback()
            val activeClassId = (_currentScreen.value as? Screen.ClassDetail)?.classId
            activeClassId?.let { pushClassProgressToCloud(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioLoadJob?.cancel()
        audioTimerJob?.cancel()
        videoTimerJob?.cancel()
        releaseAudioPlayer()
    }

    // PDF Reading Tracking & Confirmation (tracks actual pages & active seconds, excludes idle time)
    fun recordPdfPageStudy(
        classId: String,
        currentPage: Int,
        totalPages: Int,
        secondsReading: Long,
        explicitConfirm: Boolean = false,
        idleExcludedSeconds: Long = 0L
    ) {
        val prevPage = studentProgressMap.value[classId]?.lastPdfPage ?: 0
        repository.recordPdfPageStudy(
            classId = classId,
            currentPage = currentPage,
            totalPages = totalPages,
            secondsReading = secondsReading,
            explicitConfirm = explicitConfirm,
            idleExcludedSecondsToAdd = idleExcludedSeconds
        )
        val updatedSec = studentProgressMap.value[classId]?.pdfReadingSeconds ?: 0L
        if (explicitConfirm || currentPage != prevPage || (updatedSec > 0L && updatedSec % 5L == 0L)) {
            pushClassProgressToCloud(classId)
        }
    }

    fun confirmPdfRead(classId: String) {
        val prog = studentProgressMap.value[classId]
        repository.recordPdfPageStudy(
            classId = classId,
            currentPage = (prog?.lastPdfPage ?: 1).coerceAtLeast(1),
            totalPages = (prog?.totalPdfPages ?: 1).coerceAtLeast(1),
            secondsReading = 0L,
            explicitConfirm = true
        )
        pushClassProgressToCloud(classId)
        showNotification("Confirmed: PDF syllabus guide reviewed.")
    }

    // Class Quick Quiz Submission
    fun submitQuiz(classId: String, scorePercent: Int) {
        val passed = repository.submitQuiz(classId, scorePercent)
        pushClassProgressToCloud(classId)
        if (passed) {
            val issued = repository.issuedCertificates.value.find { it.studentId == currentUser.value.id && it.status == com.example.model.CertificateStatus.VALID }
            if (issued != null && cloudAuthUser.value != null) {
                viewModelScope.launch { cloudSyncRepository?.issueCertificateInCloud(issued) }
            }
            showNotification("Quiz Passed! ($scorePercent%) — Verified for Class Completion! 🎉")
            activeQuizClass.value = null
        } else {
            showNotification("Score: $scorePercent%. Passing score is ${adminSettings.value.quizPassingScore}%. Try again!", isError = true)
        }
    }

    private fun pushClassProgressToCloud(classId: String) {
        val cloudRepo = cloudSyncRepository ?: return
        if (cloudAuthUser.value == null) return
        val progress = studentProgressMap.value[classId] ?: return
        viewModelScope.launch {
            cloudRepo.syncStudentClassProgress(progress)
        }
    }

    // ================= EXAM ASSESSMENT ACTIONS =================

    fun getStudentExamAttemptsCount(examId: String): Int =
        repository.getStudentExamAttemptsCount(examId, currentUser.value.id)

    fun openExam(exam: ModuleExam) {
        val previousAttempt = repository.getStudentBestExamAttempt(exam.id)
        if (previousAttempt == null) {
            val (canStart, reason) = repository.canStudentStartExam(exam.id)
            if (!canStart) {
                showNotification(reason ?: "Cannot start exam.", isError = true)
                return
            }
        }
        activeExam.value = exam
        activeExamResult.value = previousAttempt
        isExamSubmitted.value = previousAttempt != null
        examCurrentAnswers.value = previousAttempt?.studentAnswers ?: emptyMap()
        val cutoffSec = exam.openUntilMs?.let { ((it - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(1) } ?: Int.MAX_VALUE
        examTimeRemainingSeconds.value = minOf((exam.durationMinutes.coerceAtLeast(1)) * 60, cutoffSec)

        if (previousAttempt == null) {
            startExamCountdown()
        }
    }

    fun retakeExam(exam: ModuleExam) {
        val (canStart, reason) = repository.canStudentStartExam(exam.id)
        if (!canStart) {
            showNotification(reason ?: "Maximum attempts reached or exam window closed.", isError = true)
            return
        }
        activeExam.value = exam
        activeExamResult.value = null
        isExamSubmitted.value = false
        examCurrentAnswers.value = emptyMap()
        val cutoffSec = exam.openUntilMs?.let { ((it - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(1) } ?: Int.MAX_VALUE
        examTimeRemainingSeconds.value = minOf((exam.durationMinutes.coerceAtLeast(1)) * 60, cutoffSec)
        startExamCountdown()
    }

    fun selectExamAnswer(questionId: String, optionIndex: Int) {
        if (isExamSubmitted.value) return
        val current = examCurrentAnswers.value.toMutableMap()
        current[questionId] = optionIndex
        examCurrentAnswers.value = current
    }

    fun submitActiveExam(isAutoTimeout: Boolean = false) {
        val exam = activeExam.value ?: return
        examTimerJob?.cancel()

        val attempt = repository.submitExamAttempt(
            examId = exam.id,
            studentAnswers = examCurrentAnswers.value,
            isAutoSubmittedOnTimeout = isAutoTimeout
        )

        activeExamResult.value = attempt
        isExamSubmitted.value = true

        viewModelScope.launch {
            if (cloudAuthUser.value != null) {
                cloudSyncRepository?.recordExamAttemptInCloud(attempt, exam.moduleId)
                val issued = issuedCertificates.value.find {
                    it.studentId == attempt.studentId && it.status == com.example.model.CertificateStatus.VALID
                }
                if (issued != null) {
                    cloudSyncRepository?.issueCertificateInCloud(issued)
                }
            }
        }

        val baseResultMsg = if (attempt.isPassed) {
            "Exam Passed! Score: ${attempt.scoreMarks}/${attempt.totalMarks} (${attempt.percentage}%) 🎉"
        } else {
            "Score: ${attempt.scoreMarks}/${attempt.totalMarks} (${attempt.percentage}%). Required: ${exam.passingPercentage}%."
        }
        val resultMsg = if (isAutoTimeout) "Time Up! Auto-submitted — $baseResultMsg" else baseResultMsg
        showNotification(resultMsg, isError = !attempt.isPassed || isAutoTimeout)
    }

    fun createOrUpdateModuleExam(exam: ModuleExam) {
        if (!canManageModule(exam.moduleId)) {
            showNotification("Permission denied: Not assigned to manage this module.", isError = true)
            return
        }
        val saved = repository.createOrUpdateExam(exam)
        val log = repository.recordAuditLog(
            actionType = "EXAM_SAVED",
            targetId = saved.id,
            summary = "Saved exam '${saved.title}' (${saved.questions.size} Qs, ${saved.durationMinutes}m) for module ${saved.moduleId}"
        )
        viewModelScope.launch {
            if (cloudAuthUser.value != null) {
                cloudSyncRepository?.upsertExam(saved)
                cloudSyncRepository?.appendAuditLog(log)
            }
        }
        autoPublishCatalogIfCloudAdmin("Updated exam: ${saved.title}")
        showNotification("Exam saved: ${saved.title}")
    }

    fun deleteModuleExam(examId: String) {
        val existing = moduleExams.value.find { it.id == examId } ?: return
        if (!canManageModule(existing.moduleId)) {
            showNotification("Permission denied: Not assigned to manage this module.", isError = true)
            return
        }
        if (repository.deleteExam(examId)) {
            val log = repository.recordAuditLog(
                actionType = "EXAM_DELETED",
                targetId = examId,
                summary = "Deleted exam '${existing.title}' from module ${existing.moduleId}"
            )
            viewModelScope.launch {
                if (cloudAuthUser.value != null) {
                    cloudSyncRepository?.appendAuditLog(log)
                }
            }
            autoPublishCatalogIfCloudAdmin("Removed exam: ${existing.title}")
            showNotification("Deleted exam: ${existing.title}")
        }
    }

    fun closeExam() {
        examTimerJob?.cancel()
        activeExam.value = null
        activeExamResult.value = null
        isExamSubmitted.value = false
        examCurrentAnswers.value = emptyMap()
    }

    private fun startExamCountdown() {
        examTimerJob?.cancel()
        examTimerJob = viewModelScope.launch {
            while (examTimeRemainingSeconds.value > 0 && !isExamSubmitted.value) {
                delay(1000)
                examTimeRemainingSeconds.value = examTimeRemainingSeconds.value - 1
                if (examTimeRemainingSeconds.value <= 0) {
                    showNotification("Exam time expired! Auto-submitting answers...", isError = true)
                    submitActiveExam(isAutoTimeout = true)
                    break
                }
            }
        }
    }

    // ================= ASSIGNMENT & PROJECT ACTIONS =================

    fun openAssignment(assignment: ModuleAssignment) {
        activeAssignment.value = assignment
    }

    fun closeAssignment() {
        activeAssignment.value = null
    }

    fun createOrUpdateModuleAssignment(assignment: ModuleAssignment) {
        if (!canManageModule(assignment.moduleId)) {
            showNotification("Permission denied: Not assigned to manage this module.", isError = true)
            return
        }
        val saved = repository.createOrUpdateAssignment(assignment)
        val log = repository.recordAuditLog(
            actionType = "ASSIGNMENT_SAVED",
            targetId = saved.id,
            summary = "Saved assignment '${saved.title}' for module ${saved.moduleId} (${saved.maxMarks} marks)"
        )
        viewModelScope.launch {
            if (cloudAuthUser.value != null) {
                cloudSyncRepository?.upsertAssignment(saved)
                cloudSyncRepository?.appendAuditLog(log)
            }
        }
        autoPublishCatalogIfCloudAdmin("Updated assignment: ${saved.title}")
        showNotification("Assignment saved: ${saved.title}")
    }

    fun deleteModuleAssignment(assignmentId: String) {
        val existing = moduleAssignments.value.find { it.id == assignmentId } ?: return
        if (!canManageModule(existing.moduleId)) {
            showNotification("Permission denied: Not assigned to manage this module.", isError = true)
            return
        }
        if (repository.deleteAssignment(assignmentId)) {
            val log = repository.recordAuditLog(
                actionType = "ASSIGNMENT_DELETED",
                targetId = assignmentId,
                summary = "Deleted assignment '${existing.title}' from module ${existing.moduleId}"
            )
            viewModelScope.launch {
                if (cloudAuthUser.value != null) {
                    cloudSyncRepository?.appendAuditLog(log)
                }
            }
            autoPublishCatalogIfCloudAdmin("Removed assignment: ${existing.title}")
            showNotification("Deleted assignment: ${existing.title}")
        }
    }

    fun canStudentSubmitAssignment(assignmentId: String): Pair<Boolean, String?> =
        repository.canStudentSubmitAssignment(assignmentId)

    fun submitAssignment(assignmentId: String, text: String, fileName: String? = null) {
        val (allowed, reason) = repository.canStudentSubmitAssignment(assignmentId)
        if (!allowed) {
            showNotification(reason ?: "Submission deadline expired (Time Up).", isError = true)
            return
        }
        if (text.isBlank()) {
            showNotification("Please provide assignment text or report summary.", isError = true)
            return
        }
        val existing = repository.getStudentAssignmentSubmission(assignmentId)
        if (existing?.isGraded == true) {
            showNotification("This graded submission is locked and cannot be modified.", isError = true)
            return
        }
        val sub = repository.submitAssignment(assignmentId, text, fileName)
        viewModelScope.launch {
            if (cloudAuthUser.value != null) {
                val targetModuleId = activeAssignment.value?.moduleId
                    ?: moduleAssignments.value.find { it.id == assignmentId }?.moduleId.orEmpty()
                cloudSyncRepository?.submitAssignmentToCloud(sub, targetModuleId)
                val issued = issuedCertificates.value.find {
                    it.studentId == sub.studentId && it.status == com.example.model.CertificateStatus.VALID
                }
                if (issued != null) {
                    cloudSyncRepository?.issueCertificateInCloud(issued)
                }
            }
        }
        showNotification("Assignment submitted successfully for review!")
        activeAssignment.value = null
    }

    fun gradeAssignment(submissionId: String, marks: Int, feedback: String) {
        val target = assignmentSubmissions.value.find { it.submissionId == submissionId }
        val modId = target?.moduleId ?: moduleAssignments.value.find { it.id == target?.assignmentId }?.moduleId.orEmpty()
        if (modId.isNotBlank() && !canManageModule(modId)) {
            showNotification("Permission denied: Only assigned faculty or Admin can grade this module.", isError = true)
            return
        }
        repository.gradeAssignment(
            submissionId = submissionId,
            marksAwarded = marks,
            feedback = feedback,
            graderUid = currentUser.value.authUid.ifBlank { currentUser.value.id },
            graderName = currentUser.value.fullName
        )
        val updatedSub = assignmentSubmissions.value.find { it.submissionId == submissionId }
        val log = repository.recordAuditLog(
            actionType = "ASSIGNMENT_GRADED",
            targetId = submissionId,
            summary = "Graded ${target?.studentName ?: submissionId} with $marks marks"
        )
        viewModelScope.launch {
            if (cloudAuthUser.value != null) {
                cloudSyncRepository?.gradeSubmissionInCloud(
                    submissionId = submissionId,
                    marksAwarded = marks,
                    feedback = feedback,
                    graderName = currentUser.value.fullName
                )
                cloudSyncRepository?.appendAuditLog(log)
                if (updatedSub != null) {
                    val issued = issuedCertificates.value.find {
                        it.studentId == updatedSub.studentId && it.status == com.example.model.CertificateStatus.VALID
                    }
                    if (issued != null) {
                        cloudSyncRepository?.issueCertificateInCloud(issued)
                    }
                }
            }
        }
        showNotification("Assignment graded: $marks marks awarded.")
    }

    fun getStudentAssignmentSubmission(assignmentId: String): AssignmentSubmission? {
        return repository.getStudentAssignmentSubmission(assignmentId)
    }

    // ================= CERTIFICATE ACTIONS =================

    fun generateCertificate(): CertificateRecord? {
        if (!adminSettings.value.certificatesEnabled) {
            showNotification("Official certificate issuance is currently disabled by Administrator.", isError = true)
            return null
        }
        val cert = repository.generateCertificateForStudent()
        return if (cert != null) {
            previewCertificate.value = cert
            viewModelScope.launch {
                if (cloudAuthUser.value != null) {
                    cloudSyncRepository?.issueCertificateInCloud(cert)
                }
            }
            showNotification("Official ISCEB Diploma Issued! (ID: ${cert.certificateId})")
            cert
        } else {
            showNotification("Ineligible: Complete all required lessons, exams & assignments.", isError = true)
            null
        }
    }

    fun previewDemoCertificate(): CertificateRecord? {
        val settings = adminSettings.value
        if (!isAdminLoggedIn.value && !settings.demoCertificateEnabled) {
            showNotification("Demo certificate preview is currently disabled by Administrator.", isError = true)
            return null
        }
        val demoCert = repository.createDemoCertificateRecord()
        previewCertificate.value = demoCert
        return demoCert
    }

    fun createDemoCertificateRecord(): CertificateRecord {
        return repository.createDemoCertificateRecord()
    }

    fun exportCertificatePdfToUri(record: CertificateRecord, targetUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = CertificatePdfGenerator.exportCertificatePdfToUri(getApplication(), record, targetUri)
            withContext(Dispatchers.Main) {
                res.onSuccess { bytes ->
                    showNotification("Certificate PDF saved (${bytes / 1024} KB).")
                }.onFailure { err ->
                    showNotification("Unable to save PDF: ${err.localizedMessage ?: "Export failed"}", isError = true)
                }
            }
        }
    }

    fun toggleCertificatesEnabled(enabled: Boolean) {
        if (!isAdminLoggedIn.value) {
            showNotification("Only Administrators can toggle certificate settings.", isError = true)
            return
        }
        val updated = adminSettings.value.copy(certificatesEnabled = enabled)
        repository.updateAdminSettings(updated)
        autoPublishCatalogIfCloudAdmin(if (enabled) "Enabled official certificates" else "Paused official certificates")
        showNotification(if (enabled) "Official certificates enabled." else "Official certificates paused.")
    }

    fun toggleDemoCertificateEnabled(enabled: Boolean) {
        if (!isAdminLoggedIn.value) {
            showNotification("Only Administrators can toggle demo certificate settings.", isError = true)
            return
        }
        val updated = adminSettings.value.copy(demoCertificateEnabled = enabled)
        repository.updateAdminSettings(updated)
        autoPublishCatalogIfCloudAdmin(if (enabled) "Enabled Demo Certificate preview" else "Restricted Demo Certificate preview to Admin")
        showNotification(if (enabled) "Demo Certificate enabled for preview." else "Demo Certificate restricted to Admin.")
    }

    fun reissueCertificate(studentId: String, reason: String = "Administrative Re-issuance"): CertificateRecord? {
        if (!isAdminLoggedIn.value) {
            showNotification("Only Administrators can re-issue certificates.", isError = true)
            return null
        }
        val cert = repository.reissueCertificateForStudent(studentId, reason)
        if (cert != null) {
            val log = repository.recordAuditLog(
                actionType = "CERTIFICATE_REISSUED",
                targetId = cert.certificateId,
                summary = "Re-issued certificate ${cert.certificateId} for ${cert.studentName}: $reason"
            )
            viewModelScope.launch {
                if (cloudAuthUser.value != null) {
                    cloudSyncRepository?.issueCertificateInCloud(cert)
                    cloudSyncRepository?.appendAuditLog(log)
                }
            }
            showNotification("Re-issued certificate ${cert.certificateId} for ${cert.studentName}.")
        } else {
            showNotification("Could not re-issue certificate for this student.", isError = true)
        }
        return cert
    }

    fun openOrShareCertificate(record: CertificateRecord) {
        val file = CertificatePdfGenerator.generateCertificatePdf(getApplication(), record)
        CertificatePdfGenerator.openOrShareCertificate(getApplication(), file)
    }

    fun searchCertificate(certId: String) {
        isVerificationSearched.value = true
        verificationSearchQuery.value = certId
        val localRes = repository.verifyCertificateById(certId)
        verificationResult.value = localRes
        if (localRes != null) {
            showNotification("Verified Certificate: ${localRes.studentName} (${localRes.status})")
        } else if (cloudAuthUser.value != null && cloudSyncRepository != null) {
            viewModelScope.launch {
                val remoteRes = cloudSyncRepository?.verifyCertificateOnline(certId)?.getOrNull()
                if (remoteRes != null) {
                    repository.mergeCloudCertificates(listOf(remoteRes))
                    verificationResult.value = remoteRes
                    showNotification("Verified Online: ${remoteRes.studentName} (${remoteRes.status})")
                } else {
                    showNotification("No certificate found with ID: $certId", isError = true)
                }
            }
        } else {
            showNotification("No certificate found with ID: $certId", isError = true)
        }
    }

    fun revokeCertificate(certId: String, reason: String) {
        if (!isAdminLoggedIn.value) {
            showNotification("Only Administrators can revoke certificates.", isError = true)
            return
        }
        val targetCert = issuedCertificates.value.find { it.certificateId == certId }
        val ok = repository.revokeCertificate(certId, reason)
        if (ok) {
            val log = repository.recordAuditLog(
                actionType = "CERTIFICATE_REVOKED",
                targetId = certId,
                summary = "Revoked certificate $certId: $reason"
            )
            viewModelScope.launch {
                if (cloudAuthUser.value != null && targetCert != null) {
                    cloudSyncRepository?.updateCertificateStatusInCloud(
                        certId = certId,
                        studentUid = targetCert.studentId,
                        status = com.example.model.CertificateStatus.REVOKED,
                        reason = reason
                    )
                    cloudSyncRepository?.appendAuditLog(log)
                }
            }
            showNotification("Certificate $certId revoked.")
            revokeTargetCert.value = null
            if (verificationResult.value?.certificateId == certId) {
                verificationResult.value = repository.verifyCertificateById(certId)
            }
        } else {
            showNotification("Failed to revoke certificate.", isError = true)
        }
    }

    fun reinstateCertificate(certId: String) {
        if (!isAdminLoggedIn.value) {
            showNotification("Only Administrators can reinstate certificates.", isError = true)
            return
        }
        val targetCert = issuedCertificates.value.find { it.certificateId == certId }
        val ok = repository.reinstateCertificate(certId)
        if (ok) {
            val log = repository.recordAuditLog(
                actionType = "CERTIFICATE_REINSTATED",
                targetId = certId,
                summary = "Reinstated certificate $certId as VALID"
            )
            viewModelScope.launch {
                if (cloudAuthUser.value != null && targetCert != null) {
                    cloudSyncRepository?.updateCertificateStatusInCloud(
                        certId = certId,
                        studentUid = targetCert.studentId,
                        status = com.example.model.CertificateStatus.VALID,
                        reason = null
                    )
                    cloudSyncRepository?.appendAuditLog(log)
                }
            }
            showNotification("Certificate $certId reinstated as VALID.")
            if (verificationResult.value?.certificateId == certId) {
                verificationResult.value = repository.verifyCertificateById(certId)
            }
        } else {
            showNotification("Failed to reinstate certificate.", isError = true)
        }
    }

    fun updateAdminSettings(settings: AdminCompletionSettings) {
        if (!isAdminLoggedIn.value) {
            showNotification("Only Administrators can update institutional rules.", isError = true)
            return
        }
        repository.updateAdminSettings(settings)
        val log = repository.recordAuditLog(
            actionType = "SETTINGS_UPDATED",
            targetId = "admin_completion_settings",
            summary = "Updated completion rules: minStudy=${settings.minLearningTimeSeconds}s, quizPass=${settings.quizPassingScore}%"
        )
        viewModelScope.launch {
            if (cloudAuthUser.value != null) cloudSyncRepository?.appendAuditLog(log)
        }
        autoPublishCatalogIfCloudAdmin("Updated institutional & completion rules")
        showNotification("Updated completion rules and institutional settings.")
        showAdminSettingsDialog.value = false
    }

    fun demoFastTrackCompletion() {
        if (!isAdminLoggedIn.value) {
            showNotification("Restricted: Admin mode required.", isError = true)
            return
        }
        repository.fastTrackCompleteAllClasses()
        showNotification("Fast-Track Activated: Completed all 48 classes and exams for ${currentUser.value.fullName}!")
    }

    fun adminToggleClassCompletion(classId: String) {
        repository.toggleComplete(classId)
        pushClassProgressToCloud(classId)
        val isNowCompleted = classes.value.find { it.id == classId }?.isCompleted == true
        val msg = if (isNowCompleted) "Marked as completed" else "Marked as incomplete"
        showNotification(msg)
    }

    fun toggleClassBookmarked(classId: String) {
        repository.toggleBookmark(classId)
        val isBookmarked = classes.value.find { it.id == classId }?.isBookmarked == true
        val msg = if (isBookmarked) "Saved to Bookmarks" else "Removed from Bookmarks"
        showNotification(msg)
    }

    // Admin Operations, Founder/Developer Profiles & ISCEB Members
    fun updateDeveloperProfile(updatedProfile: DeveloperProfile, pickedPhotoUri: Uri? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            var finalProfile = updatedProfile
            if (pickedPhotoUri != null) {
                val savedPhoto = MediaFileManager.copyProfilePhotoToInternalStorage(
                    getApplication(),
                    pickedPhotoUri,
                    prefix = "developer_avatar"
                )
                if (savedPhoto != null) {
                    finalProfile = updatedProfile.copy(photoUri = savedPhoto.absolutePath)
                }
            }
            repository.updateDeveloperProfile(finalProfile)
            autoPublishCatalogIfCloudAdmin("Updated Developer Profile")
            showNotification("Developer Profile updated & saved cleanly.")
        }
    }

    fun updateFounderProfile(updatedProfile: FounderProfile, pickedPhotoUri: Uri? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            var finalProfile = updatedProfile
            if (pickedPhotoUri != null) {
                val savedPhoto = MediaFileManager.copyProfilePhotoToInternalStorage(
                    getApplication(),
                    pickedPhotoUri,
                    prefix = "founder_avatar"
                )
                if (savedPhoto != null) {
                    finalProfile = updatedProfile.copy(photoUri = savedPhoto.absolutePath)
                }
            }
            repository.updateFounderProfile(finalProfile)
            autoPublishCatalogIfCloudAdmin("Updated Founder Profile")
            showNotification("Founder Profile updated & saved cleanly.")
        }
    }

    fun saveIscebMember(member: IscebMember, pickedPhotoUri: Uri? = null, clearCustomPhoto: Boolean = false) {
        if (member.fullName.isBlank()) {
            showNotification("Member full name is required.", isError = true)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            var finalMember = if (clearCustomPhoto) {
                member.copy(photoUri = "")
            } else {
                member
            }
            if (pickedPhotoUri != null) {
                val savedPhoto = MediaFileManager.copyProfilePhotoToInternalStorage(
                    getApplication(),
                    pickedPhotoUri,
                    prefix = "member_${member.id.take(12)}"
                )
                if (savedPhoto != null) {
                    finalMember = finalMember.copy(photoUri = savedPhoto.absolutePath)
                }
            }
            repository.addOrUpdateMember(finalMember)
            autoPublishCatalogIfCloudAdmin("Updated ISCEB Members Directory")
            showAddMemberDialog.value = false
            editingMember.value = null
            showNotification("Saved ISCEB Member: ${finalMember.fullName}")
        }
    }

    fun deleteIscebMember(memberId: String) {
        val targetName = iscebMembers.value.find { it.id == memberId }?.fullName ?: "Member"
        repository.deleteMember(memberId)
        autoPublishCatalogIfCloudAdmin("Removed member from ISCEB Directory")
        deleteConfirmation.value = null
        val activeScreen = _currentScreen.value
        if (activeScreen is Screen.MemberDetail && activeScreen.memberId == memberId) {
            handleBack()
        }
        showNotification("Removed member: $targetName")
    }

    fun moveIscebMemberOrder(memberId: String, moveUp: Boolean) {
        repository.moveMemberOrder(memberId, moveUp)
        autoPublishCatalogIfCloudAdmin("Reordered ISCEB Members Directory")
        showNotification(if (moveUp) "Moved member up in directory." else "Moved member down in directory.")
    }

    fun authenticateAdmin(adminId: String, password: String): AdminAuthResult {
        val result = repository.adminAuthManager.authenticate(adminId, password)
        refreshAdminAuthState()
        when (result) {
            is AdminAuthResult.Authenticated -> {
                repository.activateVerifiedAdminSession(result.adminId)
                ensureStaffCloudListeners(includeAdminLogs = true)
                showNotification("Authenticated: ${result.fullName}")
                showAdminLoginDialog.value = false
            }
            is AdminAuthResult.Failure -> {
                showNotification(result.errorMessage, isError = true)
            }
            else -> Unit
        }
        return result
    }

    fun setupInitialAdminPassword(
        adminId: String,
        newPassword: String,
        confirmPassword: String,
        inviteToken: String = ""
    ): AdminAuthResult {
        val result = repository.adminAuthManager.setupInitialPassword(
            adminId = adminId,
            newPassword = newPassword,
            confirmPassword = confirmPassword,
            setupInviteToken = inviteToken
        )
        refreshAdminAuthState()
        when (result) {
            is AdminAuthResult.Authenticated -> {
                repository.activateVerifiedAdminSession(result.adminId)
                ensureStaffCloudListeners(includeAdminLogs = true)
                result.oneTimeRecoveryCode?.let { code ->
                    _latestRecoveryKeyNotice.value = "Save one-time recovery key for ${result.fullName}: $code"
                }
                showNotification(result.message)
                showAdminLoginDialog.value = false
            }
            is AdminAuthResult.Failure -> {
                showNotification(result.errorMessage, isError = true)
            }
            else -> Unit
        }
        return result
    }

    fun generateCoAdminSetupInvite(targetAdminId: String): AdminAuthResult {
        val result = repository.adminAuthManager.generateSecondAdminSetupInvite(targetAdminId)
        refreshAdminAuthState()
        when (result) {
            is AdminAuthResult.SetupInviteGenerated -> {
                val msg = "Single-use invite for ${result.targetAdminName}: ${result.setupToken} (expires in 24h)"
                _latestCoAdminInviteNotice.value = msg
                showNotification(msg)
            }
            is AdminAuthResult.Failure -> {
                showNotification(result.errorMessage, isError = true)
            }
            else -> Unit
        }
        return result
    }

    fun changeCurrentAdminPassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): AdminAuthResult {
        val activeAdminId = currentUser.value.id
        val result = repository.adminAuthManager.changeOwnPassword(
            adminId = activeAdminId,
            currentPassword = currentPassword,
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )
        refreshAdminAuthState()
        when (result) {
            is AdminAuthResult.Success -> {
                result.oneTimeRecoveryCode?.let { code ->
                    _latestRecoveryKeyNotice.value = "New one-time recovery key for ${currentUser.value.fullName}: $code"
                }
                showNotification(result.message)
            }
            is AdminAuthResult.Failure -> {
                showNotification(result.errorMessage, isError = true)
            }
            else -> Unit
        }
        return result
    }

    fun resetAdminPasswordWithVerification(
        adminId: String,
        recoveryCode: String,
        newPassword: String,
        confirmPassword: String
    ): AdminAuthResult {
        val result = repository.adminAuthManager.resetPasswordWithVerification(
            adminId = adminId,
            verifiedGoogleEmail = cloudAuthUser.value?.email,
            recoveryCode = recoveryCode,
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )
        refreshAdminAuthState()
        when (result) {
            is AdminAuthResult.Authenticated -> {
                repository.activateVerifiedAdminSession(result.adminId)
                ensureStaffCloudListeners(includeAdminLogs = true)
                result.oneTimeRecoveryCode?.let { code ->
                    _latestRecoveryKeyNotice.value = "Updated one-time recovery key for ${result.fullName}: $code"
                }
                showNotification(result.message)
                showAdminLoginDialog.value = false
            }
            is AdminAuthResult.Failure -> {
                showNotification(result.errorMessage, isError = true)
            }
            else -> Unit
        }
        return result
    }

    fun logoutAdmin() {
        staffListenerJob?.cancel()
        staffListenerJob = null
        repository.deactivateAdminSession()
        refreshAdminAuthState()
        if (_currentScreen.value is Screen.AdminPanel) {
            _currentScreen.value = Screen.Main
            _navigationStack.clear()
        }
        showNotification("Exited Administrator Mode.")
    }

    fun addModule(title: String, description: String, category: String, hours: Int) {
        if (title.isBlank()) {
            showNotification("Module title cannot be empty", isError = true)
            return
        }
        val nextNumber = (modules.value.maxOfOrNull { it.number } ?: 0) + 1
        val mod = CourseModule(
            id = "mod-${System.currentTimeMillis()}",
            number = nextNumber,
            title = title,
            description = description,
            part = if (nextNumber <= 6) CoursePart.PART_1_BASIC else CoursePart.PART_2_ADVANCE,
            category = category,
            estimatedHours = hours
        )
        repository.addModule(mod)
        autoPublishCatalogIfCloudAdmin("Created Module #${mod.number}: ${mod.title}")
        showNotification("Created Module #${mod.number}: ${mod.title}")
        showCreateModuleDialog.value = false
    }

    fun updateModule(moduleId: String, title: String, description: String, category: String, hours: Int) {
        if (title.isBlank()) {
            showNotification("Module title cannot be empty", isError = true)
            return
        }
        val existing = modules.value.find { it.id == moduleId } ?: return
        val updated = existing.copy(
            title = title,
            description = description,
            category = category,
            estimatedHours = hours
        )
        repository.updateModule(updated)
        autoPublishCatalogIfCloudAdmin("Updated Module #${updated.number}: ${updated.title}")
        showNotification("Updated module details successfully.")
        editingModule.value = null
    }

    fun deleteModule(moduleId: String) {
        repository.deleteModule(moduleId)
        autoPublishCatalogIfCloudAdmin("Deleted module")
        showNotification("Module deleted.")
        deleteConfirmation.value = null
        if (_currentScreen.value is Screen.ModuleDetail) {
            handleBack()
        }
    }

    fun addClass(moduleId: String, title: String, summary: String, duration: Int, objectives: List<String>) {
        if (title.isBlank()) {
            showNotification("Class title cannot be empty", isError = true)
            return
        }
        val moduleClasses = classes.value.filter { it.moduleId == moduleId }
        val nextIndex = moduleClasses.size + 1
        val cls = CourseClass(
            id = "class-${System.currentTimeMillis()}",
            moduleId = moduleId,
            orderIndex = nextIndex,
            title = title,
            summary = summary,
            durationMinutes = duration,
            isPublished = true,
            learningObjectives = objectives
        )
        repository.addClass(cls)
        autoPublishCatalogIfCloudAdmin("Added published class: ${cls.title}")
        showNotification("Added class: ${cls.title}")
        showCreateClassDialog.value = false
    }

    fun updateClass(classId: String, title: String, summary: String, duration: Int, objectives: List<String>) {
        if (title.isBlank()) {
            showNotification("Class title cannot be empty", isError = true)
            return
        }
        val existing = classes.value.find { it.id == classId } ?: return
        val updated = existing.copy(
            title = title,
            summary = summary,
            durationMinutes = duration,
            learningObjectives = objectives
        )
        repository.updateClass(updated)
        autoPublishCatalogIfCloudAdmin("Updated class: ${updated.title}")
        showNotification("Class updated successfully.")
        editingClass.value = null
    }

    fun toggleClassPublished(classId: String) {
        val target = classes.value.find { it.id == classId } ?: return
        val updated = target.copy(isPublished = !target.isPublished)
        repository.updateClass(updated)
        autoPublishCatalogIfCloudAdmin(if (updated.isPublished) "Published class: ${updated.title}" else "Unpublished class: ${updated.title}")
        val msg = if (updated.isPublished) "Class published to learners" else "Class unpublished"
        showNotification(msg)
    }

    fun reorderClass(classId: String, moveUp: Boolean) {
        val target = classes.value.find { it.id == classId } ?: return
        val moduleClasses = classes.value.filter { it.moduleId == target.moduleId }.sortedBy { it.orderIndex }.toMutableList()
        val index = moduleClasses.indexOfFirst { it.id == classId }
        if (index == -1) return

        val newIndex = if (moveUp) index - 1 else index + 1
        if (newIndex in 0 until moduleClasses.size) {
            val item = moduleClasses.removeAt(index)
            moduleClasses.add(newIndex, item)
            repository.reorderClasses(target.moduleId, moduleClasses)
            autoPublishCatalogIfCloudAdmin("Reordered classes")
            showNotification(if (moveUp) "Moved class up" else "Moved class down")
        }
    }

    fun deleteClass(classId: String) {
        repository.deleteClass(classId)
        autoPublishCatalogIfCloudAdmin("Deleted class")
        showNotification("Class deleted.")
        deleteConfirmation.value = null
        if (_currentScreen.value is Screen.ClassDetail) {
            handleBack()
        }
    }

    // Media Upload / Cloud Chunk Sync / Remote URL / Replace / Delete
    fun uploadRealMediaFromPhone(
        classId: String,
        mediaType: MediaType,
        uri: Uri,
        customName: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val targetKey = "$classId-${mediaType.name}"
        _uploadingTarget.value = targetKey
        _uploadProgress.value = 0.05f
        _uploadError.value = null
        _uploadByteStatus.value = "Preparing file..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedFile = MediaFileManager.copyUriToInternalStorage(
                    context = getApplication(),
                    uri = uri,
                    classId = classId,
                    mediaType = mediaType,
                    customFileName = customName
                ) { progress, copied, total ->
                    _uploadProgress.value = progress * 0.7f
                    _uploadByteStatus.value = "${MediaFileManager.formatFileSize(copied)} / ${MediaFileManager.formatFileSize(total)}"
                }

                var cloudFileId = ""
                var cloudChunkCount = 0
                var cloudFailureMsg: String? = null
                val cloudRepo = cloudSyncRepository
                if (cloudRepo != null && cloudAuthUser.value != null && savedFile.length() <= CloudSyncRepository.MAX_CLOUD_FILE_BYTES) {
                    _uploadByteStatus.value = "Syncing to Cloud Firestore..."
                    _uploadProgress.value = 0.85f
                    val candidateFileId = "file_${classId}_${System.currentTimeMillis()}".replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    val uploadRes = cloudRepo.uploadLessonFileChunks(candidateFileId, savedFile.readBytes())
                    if (uploadRes.isSuccess) {
                        cloudFileId = candidateFileId
                        cloudChunkCount = uploadRes.getOrDefault(0)
                    } else {
                        cloudFailureMsg = uploadRes.exceptionOrNull()?.localizedMessage ?: "Firestore chunk upload failed"
                    }
                }

                val duration = when (mediaType) {
                    MediaType.AUDIO, MediaType.VIDEO -> MediaFileManager.extractFormattedDuration(savedFile)
                    MediaType.PDF, MediaType.IMAGE, MediaType.DOCUMENT -> null
                }

                val media = LessonMedia(
                    type = mediaType,
                    fileName = savedFile.name,
                    fileSizeFormatted = MediaFileManager.formatFileSize(savedFile.length()),
                    fileSizeBytes = savedFile.length(),
                    url = savedFile.absolutePath,
                    durationFormatted = duration,
                    uploadTimestamp = System.currentTimeMillis(),
                    title = customName?.takeIf { it.isNotBlank() } ?: savedFile.name,
                    mimeType = MediaFileManager.inferMimeType(
                        LessonMedia(
                            type = mediaType,
                            fileName = savedFile.name,
                            fileSizeFormatted = "",
                            fileSizeBytes = 0L
                        )
                    ),
                    cloudFileId = cloudFileId,
                    cloudChunkCount = cloudChunkCount
                )

                repository.attachFilesToClass(classId, listOf(media))
                val publishRes = autoPublishCatalogNow("Uploaded ${media.displayTitle}")
                _uploadProgress.value = null
                _uploadingTarget.value = null
                _uploadByteStatus.value = null
                _uploadError.value = cloudFailureMsg
                when {
                    cloudFailureMsg != null -> showNotification(
                        "Saved locally, but Cloud Sync failed ($cloudFailureMsg). Sign in as Admin Gmail.",
                        isError = true
                    )
                    publishRes.isFailure && cloudAuthUser.value != null -> showNotification(
                        "File uploaded, but publishing live catalog failed: ${publishRes.exceptionOrNull()?.localizedMessage ?: "Permission denied"}",
                        isError = true
                    )
                    cloudAuthUser.value == null -> showNotification(
                        "Saved locally (${media.fileSizeFormatted}). Connect Admin Google Cloud Sync to make visible to Student phones."
                    )
                    savedFile.length() > CloudSyncRepository.MAX_CLOUD_FILE_BYTES -> showNotification(
                        "Saved locally (${media.fileSizeFormatted}). Files >18 MB need an HTTPS URL for Student cross-device access."
                    )
                    else -> showNotification(
                        "Uploaded & cloud-synced ${mediaType.displayLabel}: ${media.displayTitle} (${media.fileSizeFormatted})!"
                    )
                }
                onComplete?.invoke(true)
            } catch (e: Exception) {
                _uploadProgress.value = null
                _uploadingTarget.value = null
                _uploadByteStatus.value = null
                _uploadError.value = "Upload error: ${e.message ?: "Failed to read file from phone storage"}"
                showNotification("Upload failed: ${e.message ?: "Permission or file access issue"}", isError = true)
                onComplete?.invoke(false)
            }
        }
    }

    fun uploadMultipleMediaFromPhone(
        classId: String,
        pendingItems: List<PendingUploadItem>,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (pendingItems.isEmpty()) {
            _uploadError.value = "Select at least one file to upload."
            onComplete?.invoke(false)
            return
        }

        val oversized = pendingItems.firstOrNull { it.sizeBytes > MediaFileManager.MAX_UPLOAD_BYTES }
        if (oversized != null) {
            val msg = "File '${oversized.fileName}' (${oversized.sizeFormatted}) exceeds the 50 MB upload limit."
            _uploadError.value = msg
            showNotification(msg, isError = true)
            onComplete?.invoke(false)
            return
        }

        _uploadingTarget.value = "$classId-MULTI"
        _uploadProgress.value = 0.04f
        _uploadError.value = null
        _uploadByteStatus.value = "Preparing ${pendingItems.size} file(s)..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uploadedMedia = mutableListOf<LessonMedia>()
                val totalCount = pendingItems.size
                val cloudRepo = cloudSyncRepository
                var cloudSyncedFiles = 0
                var largeLocalOnlyFiles = 0
                var firstCloudError: String? = null

                pendingItems.forEachIndexed { index, item ->
                    val uri = Uri.parse(item.uriString)
                    val savedFile = MediaFileManager.copyUriToInternalStorage(
                        context = getApplication(),
                        uri = uri,
                        classId = classId,
                        mediaType = item.mediaType,
                        customFileName = item.fileName
                    ) { fileProgress, copied, total ->
                        val overall = ((index.toFloat() + fileProgress * 0.7f) / totalCount.toFloat()).coerceIn(0.02f, 0.95f)
                        _uploadProgress.value = overall
                        _uploadByteStatus.value = "File ${index + 1}/$totalCount • ${MediaFileManager.formatFileSize(copied)} / ${MediaFileManager.formatFileSize(total)}"
                    }

                    var cloudFileId = ""
                    var cloudChunkCount = 0
                    if (cloudRepo != null && cloudAuthUser.value != null) {
                        if (savedFile.length() <= CloudSyncRepository.MAX_CLOUD_FILE_BYTES) {
                            _uploadByteStatus.value = "Syncing file ${index + 1}/$totalCount to Firestore..."
                            val candidateFileId = "file_${classId}_${index}_${System.currentTimeMillis()}".replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val uploadRes = cloudRepo.uploadLessonFileChunks(candidateFileId, savedFile.readBytes())
                            if (uploadRes.isSuccess) {
                                cloudFileId = candidateFileId
                                cloudChunkCount = uploadRes.getOrDefault(0)
                                cloudSyncedFiles++
                            } else if (firstCloudError == null) {
                                firstCloudError = uploadRes.exceptionOrNull()?.localizedMessage ?: "Firestore upload denied"
                            }
                        } else {
                            largeLocalOnlyFiles++
                        }
                    }

                    val duration = when (item.mediaType) {
                        MediaType.AUDIO, MediaType.VIDEO ->
                            item.durationFormatted ?: MediaFileManager.extractFormattedDuration(savedFile)
                        MediaType.PDF, MediaType.IMAGE, MediaType.DOCUMENT -> null
                    }

                    uploadedMedia.add(
                        LessonMedia(
                            type = item.mediaType,
                            fileName = savedFile.name,
                            fileSizeFormatted = MediaFileManager.formatFileSize(savedFile.length()),
                            fileSizeBytes = savedFile.length(),
                            url = savedFile.absolutePath,
                            durationFormatted = duration,
                            uploadTimestamp = System.currentTimeMillis(),
                            title = item.title.trim().ifBlank { item.fileName },
                            description = item.description.trim(),
                            mimeType = item.mimeType,
                            cloudFileId = cloudFileId,
                            cloudChunkCount = cloudChunkCount
                        )
                    )
                }

                repository.attachFilesToClass(classId, uploadedMedia)
                val publishRes = autoPublishCatalogNow("Uploaded ${uploadedMedia.size} class study file(s)")
                _uploadProgress.value = null
                _uploadingTarget.value = null
                _uploadByteStatus.value = null
                _uploadError.value = firstCloudError
                val summaryMsg = when {
                    firstCloudError != null ->
                        "Saved locally, but Cloud Sync failed ($firstCloudError). Sign in with Admin Gmail (www.mdnafiulislam@gmail.com)."
                    publishRes.isFailure && cloudAuthUser.value != null ->
                        "Uploaded chunks, but live catalog publish failed: ${publishRes.exceptionOrNull()?.localizedMessage ?: "Permission denied"}"
                    largeLocalOnlyFiles > 0 ->
                        "Uploaded ${uploadedMedia.size} file(s). Files >18 MB need an HTTPS URL for cross-device student access."
                    cloudSyncedFiles > 0 ->
                        "Uploaded & cloud-synced ${uploadedMedia.size} study file(s) to student phones!"
                    else ->
                        "Saved ${uploadedMedia.size} study file(s) locally. Sign in with Admin Google Cloud Sync to push to students."
                }
                showNotification(summaryMsg, isError = firstCloudError != null || (publishRes.isFailure && cloudAuthUser.value != null))
                onComplete?.invoke(true)
            } catch (e: Exception) {
                _uploadProgress.value = null
                _uploadingTarget.value = null
                _uploadByteStatus.value = null
                _uploadError.value = "Upload error: ${e.message ?: "Unable to copy selected file(s)."}"
                showNotification("Upload failed: ${e.message ?: "File read error"}", isError = true)
                onComplete?.invoke(false)
            }
        }
    }

    fun attachRemoteMediaUrl(
        classId: String,
        mediaType: MediaType,
        remoteUrl: String,
        title: String,
        description: String = "",
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val cleanUrl = remoteUrl.trim()
        if (!cleanUrl.startsWith("https://", ignoreCase = true) && !cleanUrl.startsWith("http://", ignoreCase = true)) {
            _uploadError.value = "Enter a valid HTTPS file URL (e.g. https://...)."
            onComplete?.invoke(false)
            return
        }
        val derivedName = MediaFileManager.sanitizeFileName(
            cleanUrl.substringBefore('?').substringAfterLast('/')
                .ifBlank { "${title.ifBlank { "material" }}.${mediaType.name.lowercase()}" }
        )
        val media = LessonMedia(
            type = mediaType,
            fileName = derivedName,
            fileSizeFormatted = "Cloud Link",
            fileSizeBytes = 0L,
            url = cleanUrl,
            title = title.trim().ifBlank { derivedName },
            description = description.trim(),
            mimeType = MediaFileManager.inferMimeType(
                LessonMedia(type = mediaType, fileName = derivedName, fileSizeFormatted = "", fileSizeBytes = 0L)
            )
        )
        repository.attachFilesToClass(classId, listOf(media))
        autoPublishCatalogIfCloudAdmin("Attached HTTPS ${mediaType.displayLabel}: ${media.displayTitle}")
        _uploadError.value = null
        showNotification("Attached HTTPS ${mediaType.displayLabel}: ${media.displayTitle}")
        onComplete?.invoke(true)
    }

    fun openMediaFile(media: LessonMedia) {
        when (media.type) {
            MediaType.PDF -> {
                previewPdfMedia.value = media
                return
            }
            MediaType.IMAGE -> {
                previewImageMedia.value = media
                return
            }
            MediaType.VIDEO -> {
                previewVideoMedia.value = media
                return
            }
            MediaType.DOCUMENT -> {
                previewDocumentMedia.value = media
                return
            }
            MediaType.AUDIO -> {
                val activeClass = (_currentScreen.value as? Screen.ClassDetail)?.classId
                    ?.let { cid -> classes.value.find { it.id == cid } }
                if (activeClass != null && activeClass.audioMedia?.id == media.id) {
                    playPauseAudio(activeClass.id)
                    return
                }
            }
        }
        viewModelScope.launch {
            val localFile = resolveLocalMediaFile(media)
            val resolvedMedia = if (localFile != null) media.copy(url = localFile.absolutePath) else media
            val opened = MediaFileManager.openMediaWithSystemViewer(getApplication(), resolvedMedia)
            if (!opened) {
                showNotification(
                    if (localFile != null) "Tap Download to save '${media.displayTitle}' to your Files app."
                    else "File '${media.displayTitle}' is not cached offline yet. Connect to internet & Google Sync.",
                    isError = true
                )
            }
        }
    }

    fun openMediaWithExternalViewer(media: LessonMedia) {
        viewModelScope.launch {
            val localFile = resolveLocalMediaFile(media)
            val resolvedMedia = if (localFile != null) media.copy(url = localFile.absolutePath) else media
            val opened = MediaFileManager.openMediaWithSystemViewer(getApplication(), resolvedMedia)
            if (!opened) {
                showNotification(
                    if (localFile != null) "No external app found for '${media.displayTitle}'. Use Download to save to Files."
                    else "File '${media.displayTitle}' could not be downloaded. Check internet & Google Sync.",
                    isError = true
                )
            }
        }
    }

    fun exportMediaToUri(media: LessonMedia, destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedFile = resolveLocalMediaFile(media)
            val resolvedMedia = if (resolvedFile != null) media.copy(url = resolvedFile.absolutePath) else media
            val saved = MediaFileManager.exportMediaToUri(getApplication(), resolvedMedia, destinationUri)
            if (saved) {
                showNotification("Downloaded '${media.displayTitle}' to your selected folder!")
            } else {
                showNotification("Could not download '${media.displayTitle}'. Check internet connection or re-sync class materials.", isError = true)
            }
        }
    }

    fun clearUploadError() {
        _uploadError.value = null
    }

    fun uploadMedia(
        classId: String,
        mediaType: MediaType,
        fileName: String,
        fileSizeBytes: Long,
        title: String = "",
        description: String = ""
    ) {
        val maxSizeBytes = MediaFileManager.MAX_UPLOAD_BYTES
        if (fileSizeBytes > maxSizeBytes) {
            showNotification("File exceeds 50MB upload limit (${formatFileSize(fileSizeBytes)})", isError = true)
            return
        }

        val targetKey = "$classId-${mediaType.name}"
        _uploadingTarget.value = targetKey
        _uploadProgress.value = 0f

        viewModelScope.launch {
            for (step in 1..10) {
                delay(100)
                _uploadProgress.value = step / 10f
            }

            val sanitized = sanitizeFileName(fileName)
            val media = LessonMedia(
                type = mediaType,
                fileName = sanitized,
                fileSizeFormatted = formatFileSize(fileSizeBytes),
                fileSizeBytes = fileSizeBytes,
                url = "storage://$classId/${mediaType.name.lowercase()}_$sanitized",
                durationFormatted = if (mediaType == MediaType.AUDIO) "15:40" else if (mediaType == MediaType.VIDEO) "10:20" else null,
                title = title.trim().ifBlank { fileName },
                description = description.trim()
            )

            repository.attachFilesToClass(classId, listOf(media))
            autoPublishCatalogIfCloudAdmin("Updated ${media.displayTitle}")
            _uploadingTarget.value = null
            _uploadProgress.value = null
            showNotification("${mediaType.displayLabel} attached successfully!")
        }
    }

    fun deleteMedia(classId: String, mediaType: MediaType) {
        repository.removeMediaFromClass(classId, mediaType)
        autoPublishCatalogIfCloudAdmin("Removed ${mediaType.displayLabel}")
        showNotification("${mediaType.displayLabel} removed.")
    }

    fun deleteAttachment(classId: String, mediaId: String) {
        repository.removeAttachmentFromClass(classId, mediaId)
        autoPublishCatalogIfCloudAdmin("Removed class attachment")
        showNotification("Attachment removed from class.")
    }

    fun loadSampleMedia(classId: String) {
        repository.seedDemoMediaForClass(classId)
        showNotification("Loaded demo lecture materials (PDF, Audio, Video)")
    }

    fun resetDemoData() {
        repository.resetToDemoData()
        showNotification("Reset all 12 modules, 48 classes, and certificates to default.")
    }

    private fun autoPublishCatalogIfCloudAdmin(changeSummary: String) {
        if (!isAdminLoggedIn.value || cloudSyncRepository == null || cloudAuthUser.value == null) return
        viewModelScope.launch {
            val res = autoPublishCatalogNow(changeSummary)
            if (res.isFailure) {
                showNotification(
                    "Saved locally, but Cloud Publish failed: ${res.exceptionOrNull()?.localizedMessage ?: "Admin Google account required"}",
                    isError = true
                )
            }
        }
    }

    private suspend fun autoPublishCatalogNow(changeSummary: String): Result<Unit> {
        val cloudRepo = cloudSyncRepository ?: return Result.failure(IllegalStateException("Cloud repository unavailable"))
        if (!isAdminLoggedIn.value || cloudAuthUser.value == null) {
            return Result.failure(IllegalStateException("Admin Google Cloud Sync not connected"))
        }
        val nextNum = (publishManifest.value.versionCode - 100).coerceAtLeast(1) + 1
        val manifest = repository.adminPublishCurriculumUpdate(
            newVersionTag = "v1.$nextNum.0",
            changeNotes = changeSummary,
            announcement = publishManifest.value.announcement ?: "ISCEB Classes Friday & Saturday 9:00 PM - 11:00 PM (Asia/Dhaka)",
            latestApkVersionCode = appUpdateInfo.value.latestAvailableApkVersionCode.coerceAtLeast(BuildConfig.VERSION_CODE),
            latestApkVersionName = appUpdateInfo.value.latestAvailableApkVersionName.ifBlank { BuildConfig.VERSION_NAME },
            apkDownloadUrl = adminSettings.value.apkDownloadUrl
        )
        return cloudRepo.publishLiveCurriculum(
            manifest = manifest,
            modules = modules.value,
            classes = classes.value,
            settings = adminSettings.value,
            founderProfileJson = repository.getSerializedFounderProfile(),
            developerProfileJson = repository.getSerializedDeveloperProfile(),
            membersJson = repository.getSerializedMembers(),
            examsJson = repository.getSerializedExams(),
            assignmentsJson = repository.getSerializedAssignments()
        )
    }

    // Cloud Curriculum Synchronization & OTA Publishing
    fun checkUpdatesOnLifecycleResume() {
        val now = System.currentTimeMillis()
        if (now - lastAutoResumeCheckMs < 15_000L) return
        lastAutoResumeCheckMs = now
        viewModelScope.launch {
            val hasUpdate = repository.checkForRemoteUpdates(isWorkInProgress = isWorkInProgress())
            val apkUpdate = appUpdateInfo.value
            if (apkUpdate.isBinaryUpdateRequired) {
                showNotification("📲 New ISCEB APK v${apkUpdate.latestAvailableApkVersionName} available for download!")
            } else if (hasUpdate && pendingUpdateManifest.value != null) {
                showNotification("🔔 New ISCEB update ${pendingUpdateManifest.value?.versionTag ?: ""} ready. Tap Sync Now!")
            }
        }
    }

    fun checkForUpdates(silentOnError: Boolean = false) {
        viewModelScope.launch {
            if (_apkDownloadState.value !is ApkDownloadState.Downloading &&
                _apkDownloadState.value !is ApkDownloadState.Verifying &&
                _apkDownloadState.value !is ApkDownloadState.ReadyToInstall
            ) {
                _apkDownloadState.value = ApkDownloadState.Checking
            }
            val hasUpdate = repository.checkForRemoteUpdates(isWorkInProgress = isWorkInProgress())
            val apkUpdate = appUpdateInfo.value
            val err = apkUpdate.lastCheckError
            if (_apkDownloadState.value is ApkDownloadState.Checking) {
                _apkDownloadState.value = if (err != null && !apkUpdate.isBinaryUpdateRequired) {
                    ApkDownloadState.Error(err, canRetry = true)
                } else {
                    ApkDownloadState.Idle
                }
            }
            when {
                apkUpdate.isBinaryUpdateRequired -> {
                    showNotification("📲 New APK v${apkUpdate.latestAvailableApkVersionName} available! Tap Download to update.")
                }
                hasUpdate -> {
                    showNotification("🔔 New ISCEB update ${pendingUpdateManifest.value?.versionTag ?: ""} detected & ready!")
                }
                err != null -> {
                    if (!silentOnError) {
                        showNotification("⚠️ Update server check failed: $err", isError = true)
                    }
                }
                else -> {
                    if (!silentOnError) {
                        showNotification("✅ App is up to date (v${BuildConfig.VERSION_NAME}, Build ${BuildConfig.VERSION_CODE}).")
                    }
                }
            }
        }
    }

    fun downloadLatestApk() {
        val info = appUpdateInfo.value
        val targetUrl = info.apkDownloadUrl.trim()
        if (targetUrl.isBlank()) {
            _apkDownloadState.value = ApkDownloadState.Error("APK download URL is not configured.", canRetry = false)
            return
        }

        apkDownloadJob?.cancel()
        _apkDownloadState.value = ApkDownloadState.Downloading(
            downloadedBytes = 0L,
            totalBytes = info.fileSizeBytes,
            progressFraction = if (info.fileSizeBytes > 0L) 0f else -1f
        )

        apkDownloadJob = viewModelScope.launch {
            val outcome = apkUpdateManager.downloadAndVerifyApk(
                apkUrl = targetUrl,
                versionName = info.latestAvailableApkVersionName,
                expectedSha256 = info.sha256,
                expectedSizeBytes = info.fileSizeBytes,
                onProgress = { downloaded, total, fraction ->
                    _apkDownloadState.value = ApkDownloadState.Downloading(
                        downloadedBytes = downloaded,
                        totalBytes = total,
                        progressFraction = fraction
                    )
                },
                onVerifying = {
                    _apkDownloadState.value = ApkDownloadState.Verifying("ISCEB-v${info.latestAvailableApkVersionName}.apk")
                }
            )

            when (outcome) {
                is ApkDownloadOutcome.Success -> {
                    _apkDownloadState.value = ApkDownloadState.ReadyToInstall(
                        apkFilePath = outcome.apkFile.absolutePath,
                        versionName = info.latestAvailableApkVersionName,
                        versionCode = info.latestAvailableApkVersionCode,
                        verifiedSha256 = outcome.verifiedSha256,
                        fileSizeBytes = outcome.fileSizeBytes
                    )
                    showNotification("✅ Verified APK downloaded (${ApkUpdateManager.formatByteSize(outcome.fileSizeBytes)}). Ready to install!")
                }
                is ApkDownloadOutcome.Failure -> {
                    _apkDownloadState.value = ApkDownloadState.Error(outcome.errorMessage, canRetry = true)
                    showNotification("⚠️ ${outcome.errorMessage}", isError = true)
                }
            }
        }
    }

    fun cancelApkDownload() {
        apkDownloadJob?.cancel()
        apkDownloadJob = null
        _apkDownloadState.value = ApkDownloadState.Idle
        showNotification("APK download cancelled.")
    }

    fun installDownloadedApk(): ApkInstallLaunchResult {
        val state = _apkDownloadState.value
        if (state !is ApkDownloadState.ReadyToInstall) {
            return ApkInstallLaunchResult.Failure("Download the APK first before installing.")
        }
        val result = apkUpdateManager.launchAndroidPackageInstaller(state.apkFilePath)
        if (result is ApkInstallLaunchResult.Failure) {
            showNotification(result.message, isError = true)
        }
        return result
    }

    fun resetApkDownloadState() {
        if (_apkDownloadState.value !is ApkDownloadState.Downloading) {
            _apkDownloadState.value = ApkDownloadState.Idle
        }
    }

    fun exportWorkspaceBackupJson(): String = repository.exportWorkspaceBackupJson()

    fun importWorkspaceBackupJson(rawJson: String): Result<Int> {
        val result = repository.importWorkspaceBackupJson(rawJson)
        result.onSuccess { count ->
            showNotification("✅ Restored $count saved records (progress, exams, certificates & settings).")
        }.onFailure { err ->
            showNotification("⚠️ Backup restore failed: ${err.localizedMessage ?: "Invalid file"}", isError = true)
        }
        return result
    }

    fun syncCurriculumNow() {
        if (isWorkInProgress()) {
            showNotification("Finish your active exam or upload first; update is queued safely.", isError = true)
            return
        }
        viewModelScope.launch {
            val success = repository.syncCurriculumFromCloudOrHttps(isWorkInProgress = false)
            if (success) {
                showNotification("✅ Successfully synchronized (${publishManifest.value.versionTag})!")
            }
        }
    }

    fun adminPublishCurriculum(
        newVersionTag: String,
        changeNotes: String,
        announcement: String,
        latestApkVersionCode: Int = appUpdateInfo.value.latestAvailableApkVersionCode.coerceAtLeast(BuildConfig.VERSION_CODE),
        latestApkVersionName: String = appUpdateInfo.value.latestAvailableApkVersionName.ifBlank { BuildConfig.VERSION_NAME },
        apkDownloadUrl: String = adminSettings.value.apkDownloadUrl,
        apkReleaseNotes: String = ""
    ) {
        viewModelScope.launch {
            val manifest = repository.adminPublishCurriculumUpdate(
                newVersionTag = newVersionTag,
                changeNotes = changeNotes,
                announcement = announcement,
                latestApkVersionCode = latestApkVersionCode,
                latestApkVersionName = latestApkVersionName,
                apkDownloadUrl = apkDownloadUrl,
                apkReleaseNotes = apkReleaseNotes
            )

            val cloudRepo = cloudSyncRepository
            val user = cloudAuthUser.value
            if (cloudRepo != null && user != null) {
                val res = cloudRepo.publishLiveCurriculum(
                    manifest = manifest,
                    modules = modules.value,
                    classes = classes.value,
                    settings = adminSettings.value,
                    founderProfileJson = repository.getSerializedFounderProfile(),
                    developerProfileJson = repository.getSerializedDeveloperProfile(),
                    membersJson = repository.getSerializedMembers(),
                    examsJson = repository.getSerializedExams(),
                    assignmentsJson = repository.getSerializedAssignments()
                )
                if (res.isSuccess) {
                    _cloudSyncError.value = null
                    showNotification("🚀 ${manifest.versionTag} published LIVE to all devices via Cloud Firestore!")
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Permission denied"
                    _cloudSyncError.value = err
                    showNotification("⚠️ Saved locally; Cloud publish needs admin Google login (www.mdnafiulislam@gmail.com): $err", isError = true)
                }
            } else {
                showNotification("Saved locally (${manifest.versionTag}). Sign in with Google (www.mdnafiulislam@gmail.com) to broadcast live to phones!", isError = false)
            }
        }
    }

    fun onCloudSignInSuccess(user: FirebaseUser) {
        _cloudSyncError.value = null
        viewModelScope.launch {
            val cloudRepo = cloudSyncRepository
            val existingRemote = try {
                cloudRepo?.getUserAccountOnce(user.uid)?.getOrNull()
            } catch (_: Exception) {
                null
            }
            val account = if (existingRemote != null) {
                repository.mergeCloudUserAccount(existingRemote)
                val synced = repository.signInOrRegisterStudentAccount(
                    fullName = existingRemote.fullName.ifBlank { user.displayName ?: "ISCEB Student" },
                    email = existingRemote.email.ifBlank { user.email ?: "" },
                    authUid = user.uid,
                    organization = existingRemote.organization,
                    designation = existingRemote.designation,
                    mobileNumber = existingRemote.mobileNumber,
                    enrolledProgram = existingRemote.enrolledProgram,
                    photoUrl = existingRemote.photoUrl.ifBlank { user.photoUrl?.toString().orEmpty() }
                )
                if (existingRemote.role == UserRole.TEACHER) {
                    repository.activateTeacherSession(synced.id)
                    ensureStaffCloudListeners(includeAdminLogs = false)
                }
                repository.currentUser.value
            } else {
                val created = repository.signInOrRegisterStudentAccount(
                    fullName = user.displayName ?: (user.email?.substringBefore("@") ?: "ISCEB Student"),
                    email = user.email ?: "",
                    authUid = user.uid,
                    photoUrl = user.photoUrl?.toString().orEmpty()
                )
                cloudRepo?.upsertUserAccount(created, allowRoleAndPrivileges = false)
                created
            }
            showNotification("☁️ Signed in & synced: ${account.fullName} (${account.studentId})")
            checkForUpdates()
        }
    }

    fun onCloudSignInError(message: String) {
        _cloudSyncError.value = message
        showNotification("Google Sign-In error: $message", isError = true)
    }

    fun clearCloudSyncError() {
        _cloudSyncError.value = null
    }

    fun showNotification(message: String, isError: Boolean = false) {
        _notification.value = AppNotification(
            id = System.currentTimeMillis().toString(),
            message = message,
            isError = isError
        )
        viewModelScope.launch {
            delay(3500)
            if (_notification.value?.message == message) {
                _notification.value = null
            }
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
