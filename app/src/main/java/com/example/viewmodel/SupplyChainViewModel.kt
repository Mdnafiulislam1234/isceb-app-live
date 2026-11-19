package com.example.viewmodel

import android.app.Application
import android.net.Uri
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
import com.example.model.AdminCompletionSettings
import com.example.model.ApkDownloadState
import com.example.model.AppNotification
import com.example.model.AppUpdateInfo
import com.example.model.AssignmentSubmission
import com.example.model.CertificateEligibility
import com.example.model.CertificateRecord
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
import com.example.model.SyncStatus
import com.example.model.UserProfile
import com.example.model.UserProgress
import com.example.model.UserRole
import com.example.util.ApkDownloadOutcome
import com.example.util.ApkInstallLaunchResult
import com.example.util.ApkUpdateManager
import com.example.util.CertificatePdfGenerator
import com.example.util.MediaFileManager
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

    // Audio Playback Simulation State
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

    // Video Playback Simulation State
    private val _isPlayingVideo = MutableStateFlow(false)
    val isPlayingVideo: StateFlow<Boolean> = _isPlayingVideo.asStateFlow()

    private val _videoPositionMs = MutableStateFlow(0L)
    val videoPositionMs: StateFlow<Long> = _videoPositionMs.asStateFlow()

    private val _videoDurationMs = MutableStateFlow(12 * 60 * 1000L + 30 * 1000L)
    val videoDurationMs: StateFlow<Long> = _videoDurationMs.asStateFlow()

    private var videoTimerJob: Job? = null

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
        examAttempts
    ) { clsList, _, user, _ ->
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

    // Student-only profile switcher (Admins must use cryptographic password auth)
    fun switchUser(userId: String) {
        stopAudioPlayback()
        stopVideoPlayback()
        closeExam()
        val switched = repository.switchUser(userId)
        refreshAdminAuthState()
        if (switched) {
            val user = repository.currentUser.value
            showNotification("Switched account to: ${user.fullName} (Student)")
        } else {
            showNotification("Administrator mode requires password authentication.", isError = true)
        }
    }

    // Audio Playback
    fun playPauseAudio(classId: String) {
        if (_audioCurrentClassId.value != classId) {
            _audioCurrentClassId.value = classId
            _audioProgressMs.value = 0L
            _isPlayingAudio.value = true
            startAudioTicker(classId)
        } else {
            _isPlayingAudio.value = !_isPlayingAudio.value
            if (_isPlayingAudio.value) startAudioTicker(classId) else audioTimerJob?.cancel()
        }
    }

    fun seekAudio(targetMs: Long) {
        _audioProgressMs.value = targetMs.coerceIn(0L, _audioDurationMs.value)
        _audioCurrentClassId.value?.let { classId ->
            val percent = (_audioProgressMs.value.toFloat() / _audioDurationMs.value.toFloat()) * 100f
            repository.recordActiveLearningTime(classId, 0L, percent)
        }
    }

    fun skipAudioSeconds(seconds: Int) {
        val next = _audioProgressMs.value + (seconds * 1000L)
        seekAudio(next)
    }

    fun setAudioSpeed(speed: Float) {
        _audioSpeed.value = speed
    }

    private fun startAudioTicker(classId: String) {
        audioTimerJob?.cancel()
        audioTimerJob = viewModelScope.launch {
            while (_isPlayingAudio.value) {
                delay(1000)
                val step = (1000 * _audioSpeed.value).toLong()
                val next = (_audioProgressMs.value + step).coerceAtMost(_audioDurationMs.value)
                _audioProgressMs.value = next

                val percent = (next.toFloat() / _audioDurationMs.value.toFloat()) * 100f
                repository.recordActiveLearningTime(classId, (1 * _audioSpeed.value).toLong(), percent)

                if (next >= _audioDurationMs.value) {
                    _isPlayingAudio.value = false
                    pushClassProgressToCloud(classId)
                    break
                }
            }
        }
    }

    private fun stopAudioPlayback() {
        _audioCurrentClassId.value?.let { pushClassProgressToCloud(it) }
        _isPlayingAudio.value = false
        audioTimerJob?.cancel()
    }

    // Video Playback
    fun playPauseVideo(classId: String) {
        _isPlayingVideo.value = !_isPlayingVideo.value
        if (_isPlayingVideo.value) {
            startVideoTicker(classId)
        } else {
            videoTimerJob?.cancel()
            pushClassProgressToCloud(classId)
        }
    }

    private fun startVideoTicker(classId: String) {
        videoTimerJob?.cancel()
        videoTimerJob = viewModelScope.launch {
            while (_isPlayingVideo.value) {
                delay(1000)
                val next = (_videoPositionMs.value + 1000L).coerceAtMost(_videoDurationMs.value)
                _videoPositionMs.value = next

                val percent = (next.toFloat() / _videoDurationMs.value.toFloat()) * 100f
                repository.recordActiveLearningTime(classId, 1L, percent)

                if (next >= _videoDurationMs.value) {
                    _isPlayingVideo.value = false
                    pushClassProgressToCloud(classId)
                    break
                }
            }
        }
    }

    fun seekVideo(targetMs: Long, classId: String) {
        _videoPositionMs.value = targetMs.coerceIn(0L, _videoDurationMs.value)
        val percent = (_videoPositionMs.value.toFloat() / _videoDurationMs.value.toFloat()) * 100f
        repository.recordActiveLearningTime(classId, 0L, percent)
    }

    private fun stopVideoPlayback() {
        _isPlayingVideo.value = false
        videoTimerJob?.cancel()
    }

    // PDF Reading Tracking & Confirmation
    fun confirmPdfRead(classId: String) {
        repository.recordPdfReading(classId, 30L, explicitConfirm = true)
        pushClassProgressToCloud(classId)
        showNotification("Confirmed: PDF syllabus guide thoroughly reviewed.")
    }

    // Class Quick Quiz Submission
    fun submitQuiz(classId: String, scorePercent: Int) {
        val passed = repository.submitQuiz(classId, scorePercent)
        pushClassProgressToCloud(classId)
        if (passed) {
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

    fun openExam(exam: ModuleExam) {
        // If student already has an attempt, load it
        val previousAttempt = repository.getStudentBestExamAttempt(exam.id)
        activeExam.value = exam
        activeExamResult.value = previousAttempt
        isExamSubmitted.value = previousAttempt != null
        examCurrentAnswers.value = previousAttempt?.studentAnswers ?: emptyMap()
        examTimeRemainingSeconds.value = exam.durationMinutes * 60

        if (previousAttempt == null) {
            startExamCountdown()
        }
    }

    fun retakeExam(exam: ModuleExam) {
        activeExam.value = exam
        activeExamResult.value = null
        isExamSubmitted.value = false
        examCurrentAnswers.value = emptyMap()
        examTimeRemainingSeconds.value = exam.durationMinutes * 60
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

        val resultMsg = if (attempt.isPassed) {
            "Exam Passed! Score: ${attempt.scoreMarks}/${attempt.totalMarks} (${attempt.percentage}%) 🎉"
        } else {
            "Score: ${attempt.scoreMarks}/${attempt.totalMarks} (${attempt.percentage}%). Required: ${exam.passingPercentage}%."
        }
        showNotification(resultMsg, isError = !attempt.isPassed)
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

    fun submitAssignment(assignmentId: String, text: String, fileName: String? = null) {
        if (text.isBlank()) {
            showNotification("Please provide assignment text or report summary.", isError = true)
            return
        }
        repository.submitAssignment(assignmentId, text, fileName)
        showNotification("Assignment submitted successfully for review!")
        activeAssignment.value = null
    }

    fun gradeAssignment(submissionId: String, marks: Int, feedback: String) {
        repository.gradeAssignment(submissionId, marks, feedback)
        showNotification("Assignment graded: $marks marks awarded.")
    }

    fun getStudentAssignmentSubmission(assignmentId: String): AssignmentSubmission? {
        return repository.getStudentAssignmentSubmission(assignmentId)
    }

    // ================= CERTIFICATE ACTIONS =================

    fun generateCertificate(): CertificateRecord? {
        val cert = repository.generateCertificateForStudent()
        return if (cert != null) {
            previewCertificate.value = cert
            showNotification("Official ISCEB Diploma Issued! (ID: ${cert.certificateId}) 🎉")
            cert
        } else {
            showNotification("Ineligible: Complete all required classes, exams & capstone.", isError = true)
            null
        }
    }

    fun openOrShareCertificate(record: CertificateRecord) {
        val file = record.filePath?.let { File(it) }
            ?: CertificatePdfGenerator.generateCertificatePdf(getApplication(), record)
        CertificatePdfGenerator.openOrShareCertificate(getApplication(), file)
    }

    fun searchCertificate(certId: String) {
        isVerificationSearched.value = true
        verificationSearchQuery.value = certId
        val res = repository.verifyCertificateById(certId)
        verificationResult.value = res
        if (res != null) {
            showNotification("Verified Certificate: ${res.studentName} (${res.status})")
        } else {
            showNotification("No certificate found with ID: $certId", isError = true)
        }
    }

    fun revokeCertificate(certId: String, reason: String) {
        val ok = repository.revokeCertificate(certId, reason)
        if (ok) {
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
        val ok = repository.reinstateCertificate(certId)
        if (ok) {
            showNotification("Certificate $certId reinstated as VALID.")
            if (verificationResult.value?.certificateId == certId) {
                verificationResult.value = repository.verifyCertificateById(certId)
            }
        } else {
            showNotification("Failed to reinstate certificate.", isError = true)
        }
    }

    fun updateAdminSettings(settings: AdminCompletionSettings) {
        repository.updateAdminSettings(settings)
        autoPublishCatalogIfCloudAdmin("Updated institutional & completion rules")
        showNotification("Updated completion rules and institutional settings.")
        showAdminSettingsDialog.value = false
    }

    fun demoFastTrackCompletion() {
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
        repository.deactivateAdminSession()
        refreshAdminAuthState()
        showNotification("Switched to Learner Mode.")
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
                val cloudRepo = cloudSyncRepository
                if (cloudRepo != null && cloudAuthUser.value != null && savedFile.length() <= CloudSyncRepository.MAX_CLOUD_FILE_BYTES) {
                    _uploadByteStatus.value = "Syncing to Cloud Firestore..."
                    _uploadProgress.value = 0.85f
                    val candidateFileId = "file_${classId}_${System.currentTimeMillis()}".replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    val uploadRes = cloudRepo.uploadLessonFileChunks(candidateFileId, savedFile.readBytes())
                    if (uploadRes.isSuccess) {
                        cloudFileId = candidateFileId
                        cloudChunkCount = uploadRes.getOrDefault(0)
                    }
                }

                val duration = when (mediaType) {
                    MediaType.AUDIO -> "18:24"
                    MediaType.VIDEO -> "12:30"
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
                autoPublishCatalogNow("Uploaded ${media.displayTitle}")
                _uploadProgress.value = null
                _uploadingTarget.value = null
                _uploadByteStatus.value = null
                _uploadError.value = null
                val cloudNote = if (cloudChunkCount > 0) " • Synced to students" else if (savedFile.length() > CloudSyncRepository.MAX_CLOUD_FILE_BYTES) " • Large file saved locally (use HTTPS link for student streaming)" else ""
                showNotification("Uploaded ${mediaType.displayLabel}: ${media.displayTitle} (${media.fileSizeFormatted})$cloudNote")
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
                            }
                        } else {
                            largeLocalOnlyFiles++
                        }
                    }

                    val duration = when (item.mediaType) {
                        MediaType.AUDIO -> item.durationFormatted ?: "18:24"
                        MediaType.VIDEO -> item.durationFormatted ?: "12:30"
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
                autoPublishCatalogNow("Uploaded ${uploadedMedia.size} class study file(s)")
                _uploadProgress.value = null
                _uploadingTarget.value = null
                _uploadByteStatus.value = null
                _uploadError.value = null
                val summaryMsg = when {
                    largeLocalOnlyFiles > 0 -> "Uploaded ${uploadedMedia.size} file(s). Files >3.5 MB need an HTTPS URL for cross-device student access."
                    cloudSyncedFiles > 0 -> "Uploaded & cloud-synced ${uploadedMedia.size} study file(s) to student phones!"
                    else -> "Saved ${uploadedMedia.size} study file(s) locally. Connect Admin Google Sync to push live."
                }
                showNotification(summaryMsg)
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
        if (media.type == MediaType.PDF) {
            previewPdfMedia.value = media
            return
        }
        if (media.type == MediaType.IMAGE) {
            previewImageMedia.value = media
            return
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
                showNotification("Viewing '${media.displayTitle}' inside ISCEB reader (no external viewer installed).")
            }
        }
    }

    fun exportMediaToUri(media: LessonMedia, destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            resolveLocalMediaFile(media)
            val saved = MediaFileManager.exportMediaToUri(getApplication(), media, destinationUri)
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
            autoPublishCatalogNow(changeSummary)
        }
    }

    private suspend fun autoPublishCatalogNow(changeSummary: String) {
        val cloudRepo = cloudSyncRepository ?: return
        if (!isAdminLoggedIn.value || cloudAuthUser.value == null) return
        val nextNum = (publishManifest.value.versionCode - 100).coerceAtLeast(1) + 1
        val manifest = repository.adminPublishCurriculumUpdate(
            newVersionTag = "v1.$nextNum.0",
            changeNotes = changeSummary,
            announcement = publishManifest.value.announcement ?: "ISCEB Classes Friday & Saturday 9:00 PM - 11:00 PM (Asia/Dhaka)",
            latestApkVersionCode = appUpdateInfo.value.latestAvailableApkVersionCode.coerceAtLeast(BuildConfig.VERSION_CODE),
            latestApkVersionName = appUpdateInfo.value.latestAvailableApkVersionName.ifBlank { BuildConfig.VERSION_NAME },
            apkDownloadUrl = adminSettings.value.apkDownloadUrl
        )
        cloudRepo.publishLiveCurriculum(
            manifest = manifest,
            modules = modules.value,
            classes = classes.value,
            settings = adminSettings.value,
            founderProfileJson = repository.getSerializedFounderProfile(),
            developerProfileJson = repository.getSerializedDeveloperProfile(),
            membersJson = repository.getSerializedMembers()
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
                    membersJson = repository.getSerializedMembers()
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
        showNotification("☁️ Google Cloud Auto-Sync connected: ${user.email ?: "Signed In"}")
        checkForUpdates()
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
