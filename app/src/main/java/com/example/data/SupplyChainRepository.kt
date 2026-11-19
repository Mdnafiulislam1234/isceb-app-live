package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.auth.AdminAuthManager
import com.example.model.AdminCompletionSettings
import com.example.model.AppUpdateConfig
import com.example.model.AppUpdateInfo
import com.example.model.AssignmentSubmission
import com.example.model.CertificateEligibility
import com.example.model.CertificateRecord
import com.example.model.CertificateStatus
import com.example.model.ClassStatus
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.CoursePart
import com.example.model.CourseScheduleConfig
import com.example.model.CurriculumPublishManifest
import com.example.model.DeveloperProfile
import com.example.model.ExamAttempt
import com.example.model.ExamQuestion
import com.example.model.FounderProfile
import com.example.model.IscebMember
import com.example.model.LessonMedia
import com.example.model.MediaType
import com.example.model.ModuleAssignment
import com.example.model.ModuleExam
import com.example.model.QuizQuestion
import com.example.model.StudentClassProgress
import com.example.model.SyncStatus
import com.example.model.UserProfile
import com.example.model.UserProgress
import com.example.model.UserRole
import com.example.util.CertificatePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SupplyChainRepository(
    private val context: Context,
    private val cloudSyncRepository: CloudSyncRepository? = null,
    autoLoad: Boolean = true
) {

    private val initMutex = Mutex()
    private val _isReady = MutableStateFlow(autoLoad)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val prefs: SharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        context.applicationContext.getSharedPreferences("supply_chain_masterclass_prefs", Context.MODE_PRIVATE)
    }

    val adminAuthManager: AdminAuthManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AdminAuthManager(prefs)
    }

    private val CURRENT_SYLLABUS_VERSION = "v8_isceb_exact_syllabus_48"

    // User Management
    val availableUsers = listOf(
        UserProfile("student-1", "Sarah Jenkins", "sarah.jenkins@isceb.org", UserRole.STUDENT, "ISCEB PGD Candidate"),
        UserProfile("student-2", "Alex Rivera", "alex.rivera@freightcorp.com", UserRole.STUDENT, "Pacific Maritime Forwarding"),
        UserProfile("student-3", "Tanvir Ahmed", "tanvir.ahmed@dhakalogistics.com", UserRole.STUDENT, "Apex SCM Bangladesh"),
        UserProfile(
            id = "admin-2",
            fullName = "Md. Amirul Islam",
            email = "amirul.islam@isceb.org",
            role = UserRole.ADMIN,
            organization = "Head of Supply Chain Management at BRB Group",
            designation = "Founder of ISCEB",
            additionalRole = "Lead Trainer of ISCEB",
            mobileNumber = "01715-282905"
        ),
        UserProfile(
            id = "admin-1",
            fullName = "Md. Nafiul Islam Nahin",
            email = "www.mdnafiulislam@gmail.com",
            role = UserRole.ADMIN,
            organization = "ISCEB App Engineering",
            designation = "Developer of ISCEB App"
        )
    )

    private val _currentUser = MutableStateFlow(availableUsers[0])
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _developerProfile = MutableStateFlow(DeveloperProfile())
    val developerProfile: StateFlow<DeveloperProfile> = _developerProfile.asStateFlow()

    private val _founderProfile = MutableStateFlow(FounderProfile())
    val founderProfile: StateFlow<FounderProfile> = _founderProfile.asStateFlow()

    private val _iscebMembers = MutableStateFlow<List<IscebMember>>(emptyList())
    val iscebMembers: StateFlow<List<IscebMember>> = _iscebMembers.asStateFlow()

    private val _modules = MutableStateFlow<List<CourseModule>>(emptyList())
    val modules: StateFlow<List<CourseModule>> = _modules.asStateFlow()

    private val _classes = MutableStateFlow<List<CourseClass>>(emptyList())
    val classes: StateFlow<List<CourseClass>> = _classes.asStateFlow()

    private val _progress = MutableStateFlow(UserProgress())
    val progress: StateFlow<UserProgress> = _progress.asStateFlow()

    private val _studentProgressMap = MutableStateFlow<Map<String, StudentClassProgress>>(emptyMap())
    val studentProgressMap: StateFlow<Map<String, StudentClassProgress>> = _studentProgressMap.asStateFlow()

    private val _adminSettings = MutableStateFlow(AdminCompletionSettings())
    val adminSettings: StateFlow<AdminCompletionSettings> = _adminSettings.asStateFlow()

    private val _issuedCertificates = MutableStateFlow<List<CertificateRecord>>(emptyList())
    val issuedCertificates: StateFlow<List<CertificateRecord>> = _issuedCertificates.asStateFlow()

    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    // Assessments: Exams and Assignments
    private val _moduleExams = MutableStateFlow<List<ModuleExam>>(emptyList())
    val moduleExams: StateFlow<List<ModuleExam>> = _moduleExams.asStateFlow()

    private val _examAttempts = MutableStateFlow<List<ExamAttempt>>(emptyList())
    val examAttempts: StateFlow<List<ExamAttempt>> = _examAttempts.asStateFlow()

    private val _moduleAssignments = MutableStateFlow<List<ModuleAssignment>>(emptyList())
    val moduleAssignments: StateFlow<List<ModuleAssignment>> = _moduleAssignments.asStateFlow()

    private val _assignmentSubmissions = MutableStateFlow<List<AssignmentSubmission>>(emptyList())
    val assignmentSubmissions: StateFlow<List<AssignmentSubmission>> = _assignmentSubmissions.asStateFlow()

    // Cloud Curriculum Synchronization & Live Publishing
    private val _publishManifest = MutableStateFlow(
        CurriculumPublishManifest(
            versionCode = 102,
            versionTag = "v1.3.0",
            publishedTimestamp = 1735689600000L,
            publishedBy = "Md. Nafiul Islam Nahin (Developer of ISCEB App)",
            releaseNotes = "ISCEB v1.3: Founder Profile, Developer of ISCEB App Profile, and ISCEB Members Directory.",
            announcement = "ISCEB • Connect. learn. Achieve. Classes Friday & Saturday 9:00 PM - 11:00 PM (Asia/Dhaka)."
        )
    )
    val publishManifest: StateFlow<CurriculumPublishManifest> = _publishManifest.asStateFlow()

    private val _pendingUpdateManifest = MutableStateFlow<CurriculumPublishManifest?>(null)
    val pendingUpdateManifest: StateFlow<CurriculumPublishManifest?> = _pendingUpdateManifest.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _appUpdateInfo = MutableStateFlow(
        AppUpdateInfo(
            currentAppVersionCode = BuildConfig.VERSION_CODE,
            currentAppVersionName = BuildConfig.VERSION_NAME,
            latestAvailableApkVersionCode = BuildConfig.VERSION_CODE,
            latestAvailableApkVersionName = BuildConfig.VERSION_NAME,
            apkDownloadUrl = AppUpdateConfig.DEFAULT_APK_DIRECT_URL,
            downloadPageUrl = AppUpdateConfig.DEFAULT_DOWNLOAD_PAGE_URL,
            releaseNotes = "ISCEB v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}): Direct online APK downloader, SHA-256 verification, and cloud sync.",
            isBinaryUpdateRequired = false,
            lastCheckedTimestamp = if (autoLoad) prefs.getLong("last_sync_timestamp", 0L) else 0L,
            updateSource = "ISCEB Cloud Sync"
        )
    )
    val appUpdateInfo: StateFlow<AppUpdateInfo> = _appUpdateInfo.asStateFlow()

    init {
        if (autoLoad) {
            loadData()
            _isReady.value = true
        }
    }

    suspend fun initializeIfNeeded() {
        if (_isReady.value) return
        initMutex.withLock {
            if (_isReady.value) return
            withContext(Dispatchers.IO) {
                loadData()
            }
            _isReady.value = true
        }
    }

    private fun loadData() {
        val savedVersion = prefs.getString("schema_version", null)
        val isFirstOrMismatch = savedVersion != CURRENT_SYLLABUS_VERSION

        // Load previously saved settings safely for non-destructive migration
        val savedSettingsJson = prefs.getString("admin_completion_settings", null)
        val existingSettings = if (!savedSettingsJson.isNullOrBlank()) {
            try {
                deserializeSettings(savedSettingsJson)
            } catch (_: Exception) {
                AdminCompletionSettings()
            }
        } else {
            AdminCompletionSettings()
        }
        val migratedSettings = existingSettings.copy(
            institutionName = if (existingSettings.institutionName.contains("SCM Global", ignoreCase = true) || existingSettings.institutionName.isBlank()) "ISCEB" else existingSettings.institutionName,
            officialWebsite = existingSettings.officialWebsite.ifBlank { "www.isceb.org" },
            authorizedSignerName = existingSettings.authorizedSignerName
                .replace("Academic Detector", "Developer of ISCEB App")
                .replace("Academic Director", "Developer of ISCEB App")
                .replace("Founder & Developer of ISCEB", "Developer of ISCEB App")
                .replace("MD. Nafiul", "Md. Nafiul"),
            authorizedSignerTitle = if (existingSettings.authorizedSignerTitle.contains("SCM Global", ignoreCase = true) || existingSettings.authorizedSignerTitle.isBlank()) {
                "ISCEB Academic Directorate"
            } else {
                existingSettings.authorizedSignerTitle
            },
            remoteSyncManifestUrl = existingSettings.remoteSyncManifestUrl.ifBlank { AppUpdateConfig.DEFAULT_VERSION_JSON_URL },
            apkDownloadUrl = existingSettings.apkDownloadUrl
                .takeIf { it.isNotBlank() && it != "https://www.isceb.org/app/releases/latest" }
                ?: AppUpdateConfig.DEFAULT_APK_DIRECT_URL
        )
        _adminSettings.value = migratedSettings
        if (savedSettingsJson.isNullOrBlank() || migratedSettings != existingSettings) {
            persistSettings(migratedSettings)
        }

        if (isFirstOrMismatch) {
            // Non-destructive upgrade to exact ISCEB 12 modules & 48 lectures:
            // preserve any previously uploaded media attached to classes by classId.
            val prevClassesById = try {
                val prevJson = prefs.getString("classes_json", null)
                if (!prevJson.isNullOrBlank()) deserializeClasses(prevJson).associateBy { it.id } else emptyMap()
            } catch (_: Exception) {
                emptyMap()
            }

            val initialMods = getInitialModules()
            val initialCls = getInitialClasses().map { fresh ->
                val prev = prevClassesById[fresh.id]
                if (prev != null && prev.hasAnyMedia) {
                    fresh.copy(
                        pdfMedia = prev.pdfMedia,
                        audioMedia = prev.audioMedia,
                        videoMedia = prev.videoMedia,
                        attachments = prev.attachments
                    )
                } else fresh
            }
            val initialExams = getInitialModuleExams()
            val initialAssignments = getInitialModuleAssignments()

            _modules.value = initialMods
            _classes.value = initialCls
            _moduleExams.value = initialExams
            _moduleAssignments.value = initialAssignments

            persistModules(initialMods)
            persistClasses(initialCls)
            persistExams(initialExams)
            persistAssignments(initialAssignments)

            prefs.edit().putString("schema_version", CURRENT_SYLLABUS_VERSION).apply()
        } else {
            // Load Modules & Classes
            val savedModulesJson = prefs.getString("modules_json", null)
            val savedClassesJson = prefs.getString("classes_json", null)
            val rawModules = if (!savedModulesJson.isNullOrBlank()) deserializeModules(savedModulesJson) else getInitialModules()
            val initialTitles = getInitialModules().associate { it.id to it.title }
            val syncedModules = rawModules.map { mod ->
                val exactTitle = initialTitles[mod.id]
                if (exactTitle != null && exactTitle != mod.title) mod.copy(title = exactTitle) else mod
            }
            _modules.value = syncedModules
            _classes.value = if (!savedClassesJson.isNullOrBlank()) deserializeClasses(savedClassesJson) else getInitialClasses()

            // Load Exams & Assignments
            val savedExamsJson = prefs.getString("exams_json", null)
            _moduleExams.value = if (!savedExamsJson.isNullOrBlank()) deserializeExams(savedExamsJson) else getInitialModuleExams()

            val savedAssignmentsJson = prefs.getString("assignments_json", null)
            _moduleAssignments.value = if (!savedAssignmentsJson.isNullOrBlank()) deserializeAssignments(savedAssignmentsJson) else getInitialModuleAssignments()
        }

        // Load Developer Profile (and sanitize legacy Founder phrase for Nahin)
        val devProfileJson = prefs.getString("developer_profile_json", null)
        val loadedDev = try {
            if (!devProfileJson.isNullOrBlank()) deserializeDeveloperProfile(devProfileJson) else DeveloperProfile()
        } catch (_: Exception) {
            DeveloperProfile()
        }
        val sanitizedDev = sanitizeDeveloperProfile(loadedDev)
        _developerProfile.value = sanitizedDev
        if (devProfileJson.isNullOrBlank() || sanitizedDev != loadedDev) {
            prefs.edit().putString("developer_profile_json", serializeDeveloperProfile(sanitizedDev)).apply()
        }

        // Load Founder Profile
        val founderProfileJson = prefs.getString("founder_profile_json", null)
        _founderProfile.value = try {
            if (!founderProfileJson.isNullOrBlank()) deserializeFounderProfile(founderProfileJson) else FounderProfile()
        } catch (_: Exception) {
            FounderProfile()
        }

        // Load ISCEB Members Directory
        val membersJson = prefs.getString("isceb_members_json", null)
        _iscebMembers.value = try {
            if (!membersJson.isNullOrBlank()) {
                deserializeMembers(membersJson).sortedBy { it.displayOrder }
            } else {
                val initialMembers = getInitialMembers()
                persistMembers(initialMembers)
                initialMembers
            }
        } catch (_: Exception) {
            getInitialMembers()
        }

        // Load Active User with strict cryptographic session validation for admin mode
        val validatedSessionAdminId = adminAuthManager.getValidatedSessionAdminId()
        val savedUserId = prefs.getString("current_user_id", availableUsers[0].id)
        val defaultStudent = availableUsers.first { it.role == UserRole.STUDENT }
        val activeUser = if (validatedSessionAdminId != null) {
            availableUsers.find { it.id == validatedSessionAdminId && it.role == UserRole.ADMIN }
                ?: defaultStudent
        } else {
            availableUsers.find { it.id == savedUserId && it.role == UserRole.STUDENT }
                ?: defaultStudent
        }
        _currentUser.value = activeUser
        if (activeUser.role == UserRole.ADMIN && validatedSessionAdminId != null) {
            setAdminLoggedIn(true)
        } else {
            adminAuthManager.clearSession()
            prefs.edit().putString("current_user_id", activeUser.id).apply()
            setAdminLoggedIn(false)
        }

        // Load Certificates Registry
        val certsJson = prefs.getString("issued_certificates_json", null)
        _issuedCertificates.value = try {
            if (!certsJson.isNullOrBlank()) deserializeCertificates(certsJson) else emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        // Load Exam Attempts & Assignment Submissions
        val attemptsJson = prefs.getString("exam_attempts_json", null)
        _examAttempts.value = try {
            if (!attemptsJson.isNullOrBlank()) deserializeExamAttempts(attemptsJson) else emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val submissionsJson = prefs.getString("assignment_submissions_json", null)
        _assignmentSubmissions.value = try {
            if (!submissionsJson.isNullOrBlank()) deserializeAssignmentSubmissions(submissionsJson) else emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        // Load student-specific progress
        loadStudentProgress(activeUser.id)

        // Load Cloud Manifest & Sync State
        val localManifestJson = prefs.getString("local_manifest_json", null)
        if (!localManifestJson.isNullOrBlank()) {
            _publishManifest.value = deserializeManifest(localManifestJson)
        }
        val cloudLiveManifestJson = prefs.getString("cloud_manifest_live", null)
        if (!cloudLiveManifestJson.isNullOrBlank()) {
            val remote = deserializeManifest(cloudLiveManifestJson)
            refreshAppUpdateInfoFromManifest(remote, source = "Cached Cloud Manifest")
            if (remote.versionCode > _publishManifest.value.versionCode) {
                _pendingUpdateManifest.value = remote
                _syncStatus.value = SyncStatus.UPDATE_AVAILABLE
            } else {
                _syncStatus.value = SyncStatus.SYNCED_SUCCESS
            }
        } else {
            val defaultManifest = _publishManifest.value
            prefs.edit()
                .putString("cloud_manifest_live", serializeManifest(defaultManifest))
                .putString("local_manifest_json", serializeManifest(defaultManifest))
                .apply()
            refreshAppUpdateInfoFromManifest(defaultManifest, source = "ISCEB Cloud Sync")
            _syncStatus.value = SyncStatus.SYNCED_SUCCESS
        }
        _lastSyncTimestamp.value = prefs.getLong("last_sync_timestamp", System.currentTimeMillis())
    }

    fun switchUser(userId: String): Boolean {
        val user = availableUsers.find { it.id == userId && it.role == UserRole.STUDENT } ?: return false
        adminAuthManager.clearSession()
        _currentUser.value = user
        prefs.edit().putString("current_user_id", user.id).apply()
        setAdminLoggedIn(false)
        loadStudentProgress(user.id)
        return true
    }

    fun activateVerifiedAdminSession(adminId: String): UserProfile? {
        val validatedId = adminAuthManager.getValidatedSessionAdminId() ?: return null
        if (validatedId != adminId) return null
        val adminUser = availableUsers.find { it.id == adminId && it.role == UserRole.ADMIN } ?: return null
        _currentUser.value = adminUser
        prefs.edit().putString("current_user_id", adminUser.id).apply()
        setAdminLoggedIn(true)
        loadStudentProgress(adminUser.id)
        return adminUser
    }

    fun deactivateAdminSession() {
        adminAuthManager.clearSession()
        val defaultStudent = availableUsers.first { it.role == UserRole.STUDENT }
        _currentUser.value = defaultStudent
        prefs.edit().putString("current_user_id", defaultStudent.id).apply()
        setAdminLoggedIn(false)
        loadStudentProgress(defaultStudent.id)
    }

    private fun loadStudentProgress(studentId: String) {
        val json = prefs.getString("student_progress_$studentId", null)
        val progressMap = mutableMapOf<String, StudentClassProgress>()

        if (!json.isNullOrBlank()) {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val prog = deserializeStudentProgress(obj)
                progressMap[prog.classId] = prog
            }
        }

        _studentProgressMap.value = progressMap

        // Update classes view with this student's status
        val completedIds = mutableSetOf<String>()
        val savedBookmarks = prefs.getStringSet("bookmarked_classes_$studentId", emptySet()) ?: emptySet()
        var totalActiveSec = 0L

        val updatedClasses = _classes.value.map { cls ->
            val prog = progressMap[cls.id]
            val isComp = prog?.status == ClassStatus.COMPLETED
            if (isComp) completedIds.add(cls.id)
            totalActiveSec += (prog?.activeLearningSeconds ?: 0L)

            cls.copy(
                isCompleted = isComp,
                isBookmarked = savedBookmarks.contains(cls.id),
                status = prog?.status ?: ClassStatus.NOT_STARTED,
                activeLearningSeconds = prog?.activeLearningSeconds ?: 0L,
                quizPassed = prog?.quizPassed ?: false
            )
        }

        _classes.value = updatedClasses
        _progress.value = UserProgress(
            completedClassIds = completedIds,
            bookmarkedClassIds = savedBookmarks,
            currentModuleId = _modules.value.firstOrNull()?.id ?: "mod-1",
            currentClassId = updatedClasses.firstOrNull()?.id ?: "class-1",
            totalActiveLearningSeconds = totalActiveSec
        )
    }

    // Record Active Learning Time (paused or background does not advance)
    fun recordActiveLearningTime(classId: String, secondsToAdd: Long, playbackPercent: Float? = null) {
        val studentId = _currentUser.value.id
        val currentProg = _studentProgressMap.value[classId] ?: StudentClassProgress(
            classId = classId,
            studentId = studentId,
            status = ClassStatus.IN_PROGRESS,
            startTimeMs = System.currentTimeMillis()
        )

        val newActiveSec = currentProg.activeLearningSeconds + secondsToAdd
        val newMaxPlayback = playbackPercent?.let { maxOf(currentProg.maxPlaybackPercent, it) } ?: currentProg.maxPlaybackPercent
        val newStatus = if (currentProg.status == ClassStatus.COMPLETED) ClassStatus.COMPLETED else ClassStatus.IN_PROGRESS

        val updatedProg = currentProg.copy(
            status = newStatus,
            lastActiveTimeMs = System.currentTimeMillis(),
            activeLearningSeconds = newActiveSec,
            maxPlaybackPercent = newMaxPlayback
        )

        val updatedMap = _studentProgressMap.value.toMutableMap()
        updatedMap[classId] = updatedProg
        _studentProgressMap.value = updatedMap

        persistStudentProgress(studentId, updatedMap.values.toList())
        evaluateAndRecordClassCompletion(classId)
    }

    // Record PDF reading confirmation
    fun recordPdfReading(classId: String, secondsReading: Long, explicitConfirm: Boolean = false) {
        val studentId = _currentUser.value.id
        val currentProg = _studentProgressMap.value[classId] ?: StudentClassProgress(
            classId = classId,
            studentId = studentId,
            status = ClassStatus.IN_PROGRESS,
            startTimeMs = System.currentTimeMillis()
        )

        val updatedProg = currentProg.copy(
            pdfReadingSeconds = currentProg.pdfReadingSeconds + secondsReading,
            activeLearningSeconds = currentProg.activeLearningSeconds + secondsReading,
            isPdfConfirmed = explicitConfirm || currentProg.isPdfConfirmed,
            lastActiveTimeMs = System.currentTimeMillis()
        )

        val updatedMap = _studentProgressMap.value.toMutableMap()
        updatedMap[classId] = updatedProg
        _studentProgressMap.value = updatedMap

        persistStudentProgress(studentId, updatedMap.values.toList())
        evaluateAndRecordClassCompletion(classId)
    }

    // Submit Quiz for a Class
    fun submitQuiz(classId: String, scorePercent: Int): Boolean {
        val studentId = _currentUser.value.id
        val settings = _adminSettings.value
        val passed = scorePercent >= settings.quizPassingScore

        val currentProg = _studentProgressMap.value[classId] ?: StudentClassProgress(
            classId = classId,
            studentId = studentId,
            status = ClassStatus.IN_PROGRESS,
            startTimeMs = System.currentTimeMillis()
        )

        val updatedProg = currentProg.copy(
            quizScore = scorePercent,
            quizPassed = passed,
            lastActiveTimeMs = System.currentTimeMillis()
        )

        val updatedMap = _studentProgressMap.value.toMutableMap()
        updatedMap[classId] = updatedProg
        _studentProgressMap.value = updatedMap

        persistStudentProgress(studentId, updatedMap.values.toList())
        evaluateAndRecordClassCompletion(classId)
        return passed
    }

    // Authoritative Completion Evaluation
    fun evaluateAndRecordClassCompletion(classId: String): Boolean {
        val studentId = _currentUser.value.id
        val targetClass = _classes.value.find { it.id == classId } ?: return false
        val prog = _studentProgressMap.value[classId] ?: return false
        val settings = _adminSettings.value

        if (prog.status == ClassStatus.COMPLETED) return true

        val timeMet = prog.activeLearningSeconds >= settings.minLearningTimeSeconds
        val hasPdfOrDoc = targetClass.pdfMedia != null || targetClass.attachments.any {
            it.type == MediaType.PDF || it.type == MediaType.DOCUMENT
        }
        val playbackMet = when {
            targetClass.videoMedia != null -> prog.maxPlaybackPercent >= settings.minPlaybackPercent
            targetClass.audioMedia != null -> prog.maxPlaybackPercent >= settings.minPlaybackPercent
            hasPdfOrDoc -> prog.isPdfConfirmed || (prog.pdfReadingSeconds >= settings.minPdfReadingSeconds)
            else -> true
        }

        val quizMet = if (settings.isQuizRequired) prog.quizPassed else true

        if (timeMet && playbackMet && quizMet) {
            val completionTimestamp = System.currentTimeMillis()
            val completedProg = prog.copy(
                status = ClassStatus.COMPLETED,
                completionTimestamp = completionTimestamp
            )

            val updatedMap = _studentProgressMap.value.toMutableMap()
            updatedMap[classId] = completedProg
            _studentProgressMap.value = updatedMap

            persistStudentProgress(studentId, updatedMap.values.toList())

            val newCompleted = _progress.value.completedClassIds + classId
            _progress.value = _progress.value.copy(completedClassIds = newCompleted)

            val updatedClasses = _classes.value.map { cls ->
                if (cls.id == classId) cls.copy(isCompleted = true, status = ClassStatus.COMPLETED) else cls
            }
            _classes.value = updatedClasses

            return true
        }
        return false
    }

    // Toggle Bookmark
    fun toggleBookmark(classId: String) {
        val studentId = _currentUser.value.id
        val currentBookmarks = _progress.value.bookmarkedClassIds.toMutableSet()
        if (currentBookmarks.contains(classId)) {
            currentBookmarks.remove(classId)
        } else {
            currentBookmarks.add(classId)
        }
        _progress.value = _progress.value.copy(bookmarkedClassIds = currentBookmarks)
        prefs.edit().putStringSet("bookmarked_classes_$studentId", currentBookmarks).apply()

        val updatedClasses = _classes.value.map { cls ->
            if (cls.id == classId) cls.copy(isBookmarked = currentBookmarks.contains(classId)) else cls
        }
        _classes.value = updatedClasses
    }

    // Toggle Complete (Admin override or testing)
    fun toggleComplete(classId: String) {
        val studentId = _currentUser.value.id
        val prog = _studentProgressMap.value[classId] ?: StudentClassProgress(
            classId = classId,
            studentId = studentId,
            startTimeMs = System.currentTimeMillis()
        )

        val newStatus = if (prog.status == ClassStatus.COMPLETED) ClassStatus.IN_PROGRESS else ClassStatus.COMPLETED
        val updatedProg = prog.copy(
            status = newStatus,
            completionTimestamp = if (newStatus == ClassStatus.COMPLETED) System.currentTimeMillis() else null,
            quizPassed = if (newStatus == ClassStatus.COMPLETED) true else prog.quizPassed
        )

        val updatedMap = _studentProgressMap.value.toMutableMap()
        updatedMap[classId] = updatedProg
        _studentProgressMap.value = updatedMap
        persistStudentProgress(studentId, updatedMap.values.toList())

        val isComp = newStatus == ClassStatus.COMPLETED
        val newCompleted = if (isComp) _progress.value.completedClassIds + classId else _progress.value.completedClassIds - classId
        _progress.value = _progress.value.copy(completedClassIds = newCompleted)

        val updatedClasses = _classes.value.map { cls ->
            if (cls.id == classId) cls.copy(isCompleted = isComp, status = newStatus) else cls
        }
        _classes.value = updatedClasses
    }

    // ================= EXAM ASSESSMENT SYSTEM =================

    fun submitExamAttempt(
        examId: String,
        studentAnswers: Map<String, Int>,
        isAutoSubmittedOnTimeout: Boolean = false
    ): ExamAttempt {
        val studentId = _currentUser.value.id
        val exam = _moduleExams.value.find { it.id == examId } ?: error("Exam not found: $examId")

        var earnedMarks = 0
        exam.questions.forEach { q ->
            val selected = studentAnswers[q.id]
            if (selected != null && selected == q.correctOptionIndex) {
                earnedMarks += q.marks
            }
        }

        val percentage = if (exam.totalMarks > 0) (earnedMarks * 100) / exam.totalMarks else 0
        val isPassed = percentage >= exam.passingPercentage

        val attempt = ExamAttempt(
            attemptId = "attempt-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(4)}",
            examId = examId,
            studentId = studentId,
            studentAnswers = studentAnswers,
            scoreMarks = earnedMarks,
            totalMarks = exam.totalMarks,
            percentage = percentage,
            isPassed = isPassed,
            startTimeMs = System.currentTimeMillis() - 15 * 60 * 1000L,
            submittedTimeMs = System.currentTimeMillis(),
            isAutoSubmittedOnTimeout = isAutoSubmittedOnTimeout
        )

        val updatedList = _examAttempts.value + attempt
        _examAttempts.value = updatedList
        persistExamAttempts(updatedList)
        return attempt
    }

    fun getStudentBestExamAttempt(examId: String, studentId: String = _currentUser.value.id): ExamAttempt? {
        return _examAttempts.value
            .filter { it.examId == examId && it.studentId == studentId }
            .maxByOrNull { it.scoreMarks }
    }

    // ================= ASSIGNMENT & PROJECT SYSTEM =================

    fun submitAssignment(
        assignmentId: String,
        submissionText: String,
        attachedFileName: String? = null
    ): AssignmentSubmission {
        val student = _currentUser.value
        val submission = AssignmentSubmission(
            submissionId = "sub-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(4)}",
            assignmentId = assignmentId,
            studentId = student.id,
            studentName = student.fullName,
            submissionText = submissionText,
            attachedFileName = attachedFileName,
            submissionTimestamp = System.currentTimeMillis()
        )

        val updatedList = _assignmentSubmissions.value.filterNot { it.assignmentId == assignmentId && it.studentId == student.id } + submission
        _assignmentSubmissions.value = updatedList
        persistAssignmentSubmissions(updatedList)
        return submission
    }

    fun gradeAssignment(submissionId: String, marksAwarded: Int, feedback: String) {
        val updatedList = _assignmentSubmissions.value.map { sub ->
            if (sub.submissionId == submissionId) {
                sub.copy(
                    marksAwarded = marksAwarded,
                    feedback = feedback,
                    isGraded = true
                )
            } else sub
        }
        _assignmentSubmissions.value = updatedList
        persistAssignmentSubmissions(updatedList)
    }

    fun getStudentAssignmentSubmission(assignmentId: String, studentId: String = _currentUser.value.id): AssignmentSubmission? {
        return _assignmentSubmissions.value.find { it.assignmentId == assignmentId && it.studentId == studentId }
    }

    // ================= CERTIFICATE & ELIGIBILITY =================

    fun evaluateCertificateEligibility(studentId: String = _currentUser.value.id): CertificateEligibility {
        val settings = _adminSettings.value
        val reqModules = settings.requiredModuleIds
        val requiredClasses = _classes.value.filter { reqModules.contains(it.moduleId) }

        val studentProgress = _studentProgressMap.value
        val completedClasses = requiredClasses.filter {
            studentProgress[it.id]?.status == ClassStatus.COMPLETED
        }

        val missing = requiredClasses.filter {
            studentProgress[it.id]?.status != ClassStatus.COMPLETED
        }.map { it.title }

        val allQuizzesPassed = requiredClasses.all {
            val prog = studentProgress[it.id]
            if (settings.isQuizRequired) prog?.quizPassed == true else true
        }

        // Syllabus assessment rules check:
        // Mandatory exams: Mod 1 (20m), Mod 2 (25m), Mod 3 (30m), Mod 6 (25m), and Final Exam (100m)
        val mod1Passed = getStudentBestExamAttempt("exam-mod-1", studentId)?.isPassed ?: false
        val mod2Passed = getStudentBestExamAttempt("exam-mod-2", studentId)?.isPassed ?: false
        val mod3Passed = getStudentBestExamAttempt("exam-mod-3", studentId)?.isPassed ?: false
        val mod6Passed = getStudentBestExamAttempt("exam-mod-6", studentId)?.isPassed ?: false
        val finalExamPassed = getStudentBestExamAttempt("exam-final", studentId)?.isPassed ?: false

        val mandatoryExamsPassed = mod1Passed && mod2Passed && mod3Passed && mod6Passed && finalExamPassed

        // Final Project submission check:
        val finalProject = _moduleAssignments.value.find { it.isFinalProject }
        val finalProjectSub = finalProject?.let { getStudentAssignmentSubmission(it.id, studentId) }
        val finalProjectSubmitted = finalProjectSub != null

        val isEligible = completedClasses.size == requiredClasses.size && allQuizzesPassed && mandatoryExamsPassed && finalProjectSubmitted
        val existing = _issuedCertificates.value.find { it.studentId == studentId && it.status == CertificateStatus.VALID }

        return CertificateEligibility(
            isEligible = isEligible,
            completedRequiredCount = completedClasses.size,
            totalRequiredCount = requiredClasses.size,
            missingClasses = missing,
            allQuizzesPassed = allQuizzesPassed,
            mandatoryExamsPassed = mandatoryExamsPassed,
            finalProjectSubmitted = finalProjectSubmitted,
            existingCertificate = existing
        )
    }

    fun generateCertificateForStudent(studentId: String = _currentUser.value.id): CertificateRecord? {
        val eligibility = evaluateCertificateEligibility(studentId)
        if (!eligibility.isEligible && eligibility.existingCertificate == null) {
            return null
        }
        if (eligibility.existingCertificate != null) {
            return eligibility.existingCertificate
        }

        val student = availableUsers.find { it.id == studentId } ?: _currentUser.value
        val settings = _adminSettings.value
        val certId = "ISCEB-SCM-${SimpleDateFormat("yyyyMM", Locale.US).format(Date())}-${UUID.randomUUID().toString().take(6).uppercase()}"

        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
        val record = CertificateRecord(
            certificateId = certId,
            studentId = student.id,
            studentName = student.fullName,
            courseTitle = settings.certificateTitle,
            issueDate = dateFormat.format(Date()),
            issueTimestamp = System.currentTimeMillis(),
            institutionName = settings.institutionName,
            officialWebsite = settings.officialWebsite,
            authorizedSigner = "${settings.authorizedSignerName}, ${settings.authorizedSignerTitle}",
            status = CertificateStatus.VALID,
            completedClassesCount = eligibility.completedRequiredCount,
            totalRequiredClasses = eligibility.totalRequiredCount
        )

        val file = CertificatePdfGenerator.generateCertificatePdf(context, record)
        val recordWithFile = record.copy(filePath = file.absolutePath)

        val updatedList = _issuedCertificates.value + recordWithFile
        _issuedCertificates.value = updatedList
        persistCertificates(updatedList)

        return recordWithFile
    }

    fun verifyCertificateById(certId: String): CertificateRecord? {
        val cleanId = certId.trim()
        return _issuedCertificates.value.find {
            it.certificateId.equals(cleanId, ignoreCase = true)
        }
    }

    fun revokeCertificate(certId: String, reason: String): Boolean {
        val found = _issuedCertificates.value.find { it.certificateId == certId } ?: return false
        val updated = found.copy(
            status = CertificateStatus.REVOKED,
            revocationReason = reason,
            revocationTimestamp = System.currentTimeMillis()
        )
        val updatedList = _issuedCertificates.value.map { if (it.certificateId == certId) updated else it }
        _issuedCertificates.value = updatedList
        persistCertificates(updatedList)
        return true
    }

    fun reinstateCertificate(certId: String): Boolean {
        val found = _issuedCertificates.value.find { it.certificateId == certId } ?: return false
        val updated = found.copy(
            status = CertificateStatus.VALID,
            revocationReason = null,
            revocationTimestamp = null
        )
        val updatedList = _issuedCertificates.value.map { if (it.certificateId == certId) updated else it }
        _issuedCertificates.value = updatedList
        persistCertificates(updatedList)
        return true
    }

    // ================= ADMIN SETTINGS & MANAGEMENT =================

    fun updateAdminSettings(settings: AdminCompletionSettings) {
        _adminSettings.value = settings
        persistSettings(settings)
    }

    fun setAdminLoggedIn(isAdmin: Boolean) {
        _isAdminLoggedIn.value = isAdmin
        prefs.edit().putBoolean("is_admin_logged_in", isAdmin).apply()
    }

    fun addModule(module: CourseModule) {
        val updated = _modules.value + module
        _modules.value = updated
        persistModules(updated)
    }

    fun updateModule(module: CourseModule) {
        val updated = _modules.value.map { if (it.id == module.id) module else it }
        _modules.value = updated
        persistModules(updated)
    }

    fun deleteModule(moduleId: String) {
        val updated = _modules.value.filter { it.id != moduleId }
        _modules.value = updated
        persistModules(updated)

        val remainingClasses = _classes.value.filter { it.moduleId != moduleId }
        _classes.value = remainingClasses
        persistClasses(remainingClasses)
    }

    fun addClass(courseClass: CourseClass) {
        val updated = _classes.value + courseClass
        _classes.value = updated
        persistClasses(updated)
    }

    fun updateClass(courseClass: CourseClass) {
        val updated = _classes.value.map { if (it.id == courseClass.id) courseClass else it }
        _classes.value = updated
        persistClasses(updated)
    }

    fun deleteClass(classId: String) {
        val updated = _classes.value.filter { it.id != classId }
        _classes.value = updated
        persistClasses(updated)
    }

    fun reorderClasses(moduleId: String, reorderedClasses: List<CourseClass>) {
        val otherClasses = _classes.value.filter { it.moduleId != moduleId }
        val updatedWithIndices = reorderedClasses.mapIndexed { index, cls ->
            cls.copy(orderIndex = index + 1)
        }
        val allUpdated = otherClasses + updatedWithIndices
        _classes.value = allUpdated
        persistClasses(allUpdated)
    }

    fun updateDeveloperProfile(profile: DeveloperProfile) {
        val sanitized = sanitizeDeveloperProfile(profile)
        _developerProfile.value = sanitized
        prefs.edit().putString("developer_profile_json", serializeDeveloperProfile(sanitized)).apply()
    }

    fun updateFounderProfile(profile: FounderProfile) {
        _founderProfile.value = profile
        prefs.edit().putString("founder_profile_json", serializeFounderProfile(profile)).apply()
    }

    fun addOrUpdateMember(member: IscebMember) {
        val currentList = _iscebMembers.value.toMutableList()
        val existingIdx = currentList.indexOfFirst { it.id == member.id }
        val normalizedMember = member.copy(
            displayOrder = if (member.displayOrder > 0) member.displayOrder else (currentList.size + 1),
            updatedAt = System.currentTimeMillis()
        )
        if (existingIdx >= 0) {
            currentList[existingIdx] = normalizedMember
        } else {
            currentList.add(normalizedMember)
        }
        val sorted = reindexMembers(currentList.sortedBy { it.displayOrder })
        _iscebMembers.value = sorted
        persistMembers(sorted)
    }

    fun deleteMember(memberId: String) {
        val filtered = _iscebMembers.value.filterNot { it.id == memberId }
        val reindexed = reindexMembers(filtered.sortedBy { it.displayOrder })
        _iscebMembers.value = reindexed
        persistMembers(reindexed)
    }

    fun moveMemberOrder(memberId: String, moveUp: Boolean) {
        val current = _iscebMembers.value.sortedBy { it.displayOrder }.toMutableList()
        val index = current.indexOfFirst { it.id == memberId }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex in current.indices) {
            val item = current.removeAt(index)
            current.add(targetIndex, item)
            val reindexed = reindexMembers(current)
            _iscebMembers.value = reindexed
            persistMembers(reindexed)
        }
    }

    private fun reindexMembers(list: List<IscebMember>): List<IscebMember> {
        return list.mapIndexed { idx, m -> m.copy(displayOrder = idx + 1) }
    }

    fun getSerializedFounderProfile(): String = serializeFounderProfile(_founderProfile.value)
    fun getSerializedDeveloperProfile(): String = serializeDeveloperProfile(_developerProfile.value)
    fun getSerializedMembers(): String = serializeMembers(_iscebMembers.value)

    private fun sanitizeDeveloperProfile(profile: DeveloperProfile): DeveloperProfile {
        val safeDesignation = if (profile.designation.contains("Founder", ignoreCase = true) ||
            profile.designation.contains("Detector", ignoreCase = true) ||
            profile.designation.isBlank()
        ) {
            "Developer of ISCEB App"
        } else {
            profile.designation
        }
        val safeBio = profile.biography
            .replace("Founder & Developer of ISCEB (International Supply Chain Education Board)", "App Developer for ISCEB (International Supply Chain Education Board)")
            .replace("Founder & Developer of ISCEB", "Developer of ISCEB App")
        return profile.copy(
            designation = safeDesignation,
            biography = safeBio
        )
    }

    private fun getInitialMembers(): List<IscebMember> {
        return listOf(
            IscebMember(
                id = "member-founder-amirul",
                photoUri = "",
                builtInPhotoKey = "amirul",
                fullName = "Md. Amirul Islam",
                designation = "Founder of ISCEB",
                professionalIdentity = "Head of Supply Chain Management at BRB Group",
                currentOrganization = "BRB Group",
                educationalQualification = "",
                professionalExperience = "Head of Supply Chain Management at BRB Group; Founder of ISCEB & Lead Trainer of ISCEB.",
                skillsAndExpertise = listOf(
                    "Supply Chain Leadership",
                    "Strategic Procurement & Sourcing",
                    "International Trade & Logistics",
                    "Executive Training & Mentorship"
                ),
                roleInIsceb = "Founder of ISCEB • Lead Trainer of ISCEB",
                contactInformation = "Mobile: 01715-282905 • Email: amirul.islam@isceb.org",
                biography = "Founder of ISCEB and Lead Trainer guiding supply chain management professionals while serving as Head of Supply Chain Management at BRB Group.",
                displayOrder = 1
            )
        )
    }

    fun attachMediaToClass(classId: String, media: LessonMedia) {
        attachFilesToClass(classId, listOf(media))
    }

    fun attachFilesToClass(classId: String, mediaList: List<LessonMedia>) {
        if (mediaList.isEmpty()) return
        val updated = _classes.value.map { cls ->
            if (cls.id == classId) {
                var newPdf = cls.pdfMedia
                var newAudio = cls.audioMedia
                var newVideo = cls.videoMedia
                val updatedAttachments = cls.attachments.toMutableList()
                mediaList.forEach { media ->
                    when (media.type) {
                        MediaType.PDF -> newPdf = media
                        MediaType.AUDIO -> newAudio = media
                        MediaType.VIDEO -> newVideo = media
                        MediaType.IMAGE, MediaType.DOCUMENT -> Unit
                    }
                    updatedAttachments.removeAll { it.id == media.id }
                    updatedAttachments.add(media)
                }
                cls.copy(
                    pdfMedia = newPdf,
                    audioMedia = newAudio,
                    videoMedia = newVideo,
                    attachments = updatedAttachments
                )
            } else cls
        }
        _classes.value = updated
        persistClasses(updated)
    }

    fun removeAttachmentFromClass(classId: String, mediaId: String) {
        val updated = _classes.value.map { cls ->
            if (cls.id == classId) {
                val target = cls.allAttachments.find { it.id == mediaId }
                val remaining = cls.attachments.filterNot { it.id == mediaId }
                val newPdf = if (cls.pdfMedia?.id == mediaId || (target != null && cls.pdfMedia?.url == target.url && target.type == MediaType.PDF)) {
                    remaining.lastOrNull { it.type == MediaType.PDF }
                } else cls.pdfMedia
                val newAudio = if (cls.audioMedia?.id == mediaId || (target != null && cls.audioMedia?.url == target.url && target.type == MediaType.AUDIO)) {
                    remaining.lastOrNull { it.type == MediaType.AUDIO }
                } else cls.audioMedia
                val newVideo = if (cls.videoMedia?.id == mediaId || (target != null && cls.videoMedia?.url == target.url && target.type == MediaType.VIDEO)) {
                    remaining.lastOrNull { it.type == MediaType.VIDEO }
                } else cls.videoMedia

                cls.copy(
                    pdfMedia = newPdf,
                    audioMedia = newAudio,
                    videoMedia = newVideo,
                    attachments = remaining
                )
            } else cls
        }
        _classes.value = updated
        persistClasses(updated)
    }

    fun removeMediaFromClass(classId: String, mediaType: MediaType) {
        val updated = _classes.value.map { cls ->
            if (cls.id == classId) {
                val filteredAttachments = cls.attachments.filterNot { it.type == mediaType }
                when (mediaType) {
                    MediaType.PDF -> cls.copy(pdfMedia = null, attachments = filteredAttachments)
                    MediaType.AUDIO -> cls.copy(audioMedia = null, attachments = filteredAttachments)
                    MediaType.VIDEO -> cls.copy(videoMedia = null, attachments = filteredAttachments)
                    MediaType.IMAGE, MediaType.DOCUMENT -> cls.copy(attachments = filteredAttachments)
                }
            } else cls
        }
        _classes.value = updated
        persistClasses(updated)
    }

    // Fast-track Demo Completion
    fun fastTrackCompleteAllClasses() {
        val studentId = _currentUser.value.id
        val updatedMap = _studentProgressMap.value.toMutableMap()
        val allIds = _classes.value.map { it.id }.toSet()

        _classes.value.forEach { cls ->
            updatedMap[cls.id] = StudentClassProgress(
                classId = cls.id,
                studentId = studentId,
                status = ClassStatus.COMPLETED,
                activeLearningSeconds = 120L,
                maxPlaybackPercent = 100f,
                isPdfConfirmed = true,
                quizScore = 100,
                quizPassed = true,
                completionTimestamp = System.currentTimeMillis()
            )
        }

        _studentProgressMap.value = updatedMap
        persistStudentProgress(studentId, updatedMap.values.toList())

        // Also complete mandatory exams
        _moduleExams.value.forEach { exam ->
            val mockAnswers = exam.questions.associate { it.id to it.correctOptionIndex }
            submitExamAttempt(exam.id, mockAnswers, isAutoSubmittedOnTimeout = false)
        }

        // Submit final project
        val finalProject = _moduleAssignments.value.find { it.isFinalProject }
        if (finalProject != null) {
            submitAssignment(
                finalProject.id,
                "ISCEB Capstone Project: End-to-End Multimodal SCM Optimization & Port Congestion Mitigation Strategy for Chattogram Port.",
                "ISCEB_Capstone_Report_Final.pdf"
            )
        }

        _progress.value = _progress.value.copy(completedClassIds = allIds)
        _classes.value = _classes.value.map { it.copy(isCompleted = true, status = ClassStatus.COMPLETED, quizPassed = true) }
    }

    fun seedDemoMediaForClass(classId: String) {
        val target = _classes.value.find { it.id == classId } ?: return
        val samplePdf = LessonMedia(
            type = MediaType.PDF,
            fileName = "${target.title.take(24).replace(" ", "_")}_Syllabus_Guide.pdf",
            fileSizeFormatted = "4.2 MB",
            fileSizeBytes = 4404019L,
            url = "demo://sample_guide.pdf",
            title = "Syllabus Study Guide PDF",
            description = "Comprehensive lecture slides and reference handbook."
        )
        val sampleAudio = LessonMedia(
            type = MediaType.AUDIO,
            fileName = "${target.title.take(24).replace(" ", "_")}_Lecture_Audio.mp3",
            fileSizeFormatted = "18.5 MB",
            fileSizeBytes = 19398656L,
            url = "demo://sample_audio_lecture.mp3",
            durationFormatted = "18:24",
            title = "Class Audio Briefing",
            description = "Recorded faculty commentary and case insights."
        )
        val sampleVideo = LessonMedia(
            type = MediaType.VIDEO,
            fileName = "${target.title.take(24).replace(" ", "_")}_Executive_Video.mp4",
            fileSizeFormatted = "42.1 MB",
            fileSizeBytes = 44145000L,
            url = "demo://sample_video_lecture.mp4",
            durationFormatted = "12:30",
            title = "Executive Session Recording",
            description = "Recorded lecture session and walkthrough."
        )

        val updated = _classes.value.map { cls ->
            if (cls.id == classId) {
                cls.copy(
                    pdfMedia = samplePdf,
                    audioMedia = sampleAudio,
                    videoMedia = sampleVideo,
                    attachments = (cls.attachments + listOf(samplePdf, sampleAudio, sampleVideo))
                )
            } else cls
        }
        _classes.value = updated
        persistClasses(updated)
    }

    fun resetToDemoData() {
        val initialMods = getInitialModules()
        val initialCls = getInitialClasses()
        val initialExams = getInitialModuleExams()
        val initialAssignments = getInitialModuleAssignments()
        val initialMembers = getInitialMembers()

        _modules.value = initialMods
        _classes.value = initialCls
        _moduleExams.value = initialExams
        _moduleAssignments.value = initialAssignments
        _studentProgressMap.value = emptyMap()
        _issuedCertificates.value = emptyList()
        _examAttempts.value = emptyList()
        _assignmentSubmissions.value = emptyList()
        _adminSettings.value = AdminCompletionSettings()
        _developerProfile.value = DeveloperProfile()
        _founderProfile.value = FounderProfile()
        _iscebMembers.value = initialMembers

        prefs.edit().clear().apply()
        prefs.edit().putString("schema_version", CURRENT_SYLLABUS_VERSION).apply()

        persistModules(initialMods)
        persistClasses(initialCls)
        persistSettings(_adminSettings.value)
        persistExams(initialExams)
        persistAssignments(initialAssignments)
        prefs.edit()
            .putString("developer_profile_json", serializeDeveloperProfile(_developerProfile.value))
            .putString("founder_profile_json", serializeFounderProfile(_founderProfile.value))
            .apply()
        persistMembers(initialMembers)
        loadStudentProgress(_currentUser.value.id)
    }

    // ================= Persistence Helpers =================

    private fun persistStudentProgress(studentId: String, list: List<StudentClassProgress>) {
        val arr = JSONArray()
        list.forEach { prog ->
            val obj = JSONObject().apply {
                put("classId", prog.classId)
                put("studentId", prog.studentId)
                put("status", prog.status.name)
                put("startTimeMs", prog.startTimeMs)
                put("lastActiveTimeMs", prog.lastActiveTimeMs)
                put("activeLearningSeconds", prog.activeLearningSeconds)
                put("maxPlaybackPercent", prog.maxPlaybackPercent.toDouble())
                put("isPdfConfirmed", prog.isPdfConfirmed)
                put("pdfReadingSeconds", prog.pdfReadingSeconds)
                prog.quizScore?.let { put("quizScore", it) }
                put("quizPassed", prog.quizPassed)
                prog.completionTimestamp?.let { put("completionTimestamp", it) }
            }
            arr.put(obj)
        }
        prefs.edit().putString("student_progress_$studentId", arr.toString()).apply()
    }

    private fun deserializeStudentProgress(obj: JSONObject): StudentClassProgress {
        return StudentClassProgress(
            classId = obj.getString("classId"),
            studentId = obj.getString("studentId"),
            status = ClassStatus.valueOf(obj.optString("status", ClassStatus.NOT_STARTED.name)),
            startTimeMs = obj.optLong("startTimeMs", 0L),
            lastActiveTimeMs = obj.optLong("lastActiveTimeMs", 0L),
            activeLearningSeconds = obj.optLong("activeLearningSeconds", 0L),
            maxPlaybackPercent = obj.optDouble("maxPlaybackPercent", 0.0).toFloat(),
            isPdfConfirmed = obj.optBoolean("isPdfConfirmed", false),
            pdfReadingSeconds = obj.optLong("pdfReadingSeconds", 0L),
            quizScore = if (obj.has("quizScore")) obj.getInt("quizScore") else null,
            quizPassed = obj.optBoolean("quizPassed", false),
            completionTimestamp = if (obj.has("completionTimestamp")) obj.getLong("completionTimestamp") else null
        )
    }

    private fun persistSettings(settings: AdminCompletionSettings) {
        val obj = JSONObject().apply {
            put("minLearningTimeSeconds", settings.minLearningTimeSeconds)
            put("minPlaybackPercent", settings.minPlaybackPercent.toDouble())
            put("minPdfReadingSeconds", settings.minPdfReadingSeconds)
            put("isQuizRequired", settings.isQuizRequired)
            put("quizPassingScore", settings.quizPassingScore)
            put("requiredModuleIds", JSONArray(settings.requiredModuleIds))
            put("institutionName", settings.institutionName)
            put("officialWebsite", settings.officialWebsite)
            put("institutionAccreditation", settings.institutionAccreditation)
            put("authorizedSignerName", settings.authorizedSignerName)
            put("authorizedSignerTitle", settings.authorizedSignerTitle)
            put("certificateTitle", settings.certificateTitle)
            put("classTimeWindow", settings.scheduleConfig.classTimeWindow)
            put("timeZoneId", settings.scheduleConfig.timeZoneId)
            put("remoteSyncManifestUrl", settings.remoteSyncManifestUrl)
            put("apkDownloadUrl", settings.apkDownloadUrl)
            put("autoApplyContentUpdates", settings.autoApplyContentUpdates)
        }
        prefs.edit().putString("admin_completion_settings", obj.toString()).apply()
    }

    private fun deserializeSettings(json: String): AdminCompletionSettings {
        val obj = JSONObject(json)
        val reqArr = obj.optJSONArray("requiredModuleIds")
        val reqSet = mutableSetOf<String>()
        if (reqArr != null) {
            for (i in 0 until reqArr.length()) reqSet.add(reqArr.getString(i))
        } else {
            reqSet.addAll((1..12).map { "mod-$it" })
        }

        return AdminCompletionSettings(
            minLearningTimeSeconds = obj.optLong("minLearningTimeSeconds", 60L),
            minPlaybackPercent = obj.optDouble("minPlaybackPercent", 80.0).toFloat(),
            minPdfReadingSeconds = obj.optLong("minPdfReadingSeconds", 45L),
            isQuizRequired = obj.optBoolean("isQuizRequired", true),
            quizPassingScore = obj.optInt("quizPassingScore", 70),
            requiredModuleIds = reqSet,
            institutionName = obj.optString("institutionName", "ISCEB"),
            officialWebsite = obj.optString("officialWebsite", "www.isceb.org"),
            institutionAccreditation = obj.optString("institutionAccreditation", "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)"),
            authorizedSignerName = obj.optString("authorizedSignerName", "Md. Amirul Islam, Founder of ISCEB")
                .replace("Academic Detector", "Developer of ISCEB App")
                .replace("Founder & Developer of ISCEB", "Developer of ISCEB App")
                .replace("MD. Nafiul", "Md. Nafiul"),
            authorizedSignerTitle = obj.optString("authorizedSignerTitle", "ISCEB Academic Directorate"),
            certificateTitle = obj.optString("certificateTitle", "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)"),
            scheduleConfig = CourseScheduleConfig(
                classTimeWindow = obj.optString("classTimeWindow", "9:00 PM to 11:00 PM"),
                timeZoneId = obj.optString("timeZoneId", "Asia/Dhaka")
            ),
            remoteSyncManifestUrl = obj.optString("remoteSyncManifestUrl", AppUpdateConfig.DEFAULT_VERSION_JSON_URL)
                .ifBlank { AppUpdateConfig.DEFAULT_VERSION_JSON_URL },
            apkDownloadUrl = obj.optString("apkDownloadUrl", AppUpdateConfig.DEFAULT_APK_DIRECT_URL)
                .takeIf { it.isNotBlank() && it != "https://www.isceb.org/app/releases/latest" }
                ?: AppUpdateConfig.DEFAULT_APK_DIRECT_URL,
            autoApplyContentUpdates = obj.optBoolean("autoApplyContentUpdates", true)
        )
    }

    private fun persistCertificates(list: List<CertificateRecord>) {
        val arr = JSONArray()
        list.forEach { cert ->
            val obj = JSONObject().apply {
                put("certificateId", cert.certificateId)
                put("studentId", cert.studentId)
                put("studentName", cert.studentName)
                put("courseTitle", cert.courseTitle)
                put("issueDate", cert.issueDate)
                put("issueTimestamp", cert.issueTimestamp)
                put("institutionName", cert.institutionName)
                put("officialWebsite", cert.officialWebsite)
                put("authorizedSigner", cert.authorizedSigner)
                put("status", cert.status.name)
                cert.revocationReason?.let { put("revocationReason", it) }
                cert.revocationTimestamp?.let { put("revocationTimestamp", it) }
                put("completedClassesCount", cert.completedClassesCount)
                put("totalRequiredClasses", cert.totalRequiredClasses)
                cert.filePath?.let { put("filePath", it) }
            }
            arr.put(obj)
        }
        prefs.edit().putString("issued_certificates_json", arr.toString()).apply()
    }

    private fun deserializeCertificates(json: String): List<CertificateRecord> {
        val list = mutableListOf<CertificateRecord>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                CertificateRecord(
                    certificateId = obj.getString("certificateId"),
                    studentId = obj.getString("studentId"),
                    studentName = obj.getString("studentName"),
                    courseTitle = obj.getString("courseTitle"),
                    issueDate = obj.getString("issueDate"),
                    issueTimestamp = obj.getLong("issueTimestamp"),
                    institutionName = obj.getString("institutionName"),
                    officialWebsite = obj.optString("officialWebsite", "www.isceb.com"),
                    authorizedSigner = obj.getString("authorizedSigner"),
                    status = CertificateStatus.valueOf(obj.optString("status", CertificateStatus.VALID.name)),
                    revocationReason = if (obj.has("revocationReason")) obj.getString("revocationReason") else null,
                    revocationTimestamp = if (obj.has("revocationTimestamp")) obj.getLong("revocationTimestamp") else null,
                    completedClassesCount = obj.optInt("completedClassesCount", 48),
                    totalRequiredClasses = obj.optInt("totalRequiredClasses", 48),
                    filePath = if (obj.has("filePath")) obj.getString("filePath") else null
                )
            )
        }
        return list
    }

    private fun persistModules(list: List<CourseModule>) {
        val arr = JSONArray()
        list.forEach { mod ->
            val obj = JSONObject().apply {
                put("id", mod.id)
                put("number", mod.number)
                put("title", mod.title)
                put("description", mod.description)
                put("part", mod.part.name)
                put("category", mod.category)
                put("estimatedHours", mod.estimatedHours)
                put("iconName", mod.iconName)
                put("isRequired", mod.isRequired)
            }
            arr.put(obj)
        }
        prefs.edit().putString("modules_json", arr.toString()).apply()
    }

    private fun deserializeModules(json: String): List<CourseModule> {
        val result = mutableListOf<CourseModule>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val partName = obj.optString("part", if (obj.getInt("number") <= 6) CoursePart.PART_1_BASIC.name else CoursePart.PART_2_ADVANCE.name)
            result.add(
                CourseModule(
                    id = obj.getString("id"),
                    number = obj.getInt("number"),
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    part = CoursePart.valueOf(partName),
                    category = obj.optString("category", "Core SCM"),
                    estimatedHours = obj.optInt("estimatedHours", 4),
                    iconName = obj.optString("iconName", "inventory"),
                    isRequired = obj.optBoolean("isRequired", true)
                )
            )
        }
        return result
    }

    private fun persistClasses(list: List<CourseClass>) {
        val arr = JSONArray()
        list.forEach { cls ->
            val obj = JSONObject().apply {
                put("id", cls.id)
                put("moduleId", cls.moduleId)
                put("orderIndex", cls.orderIndex)
                put("lectureCode", cls.lectureCode)
                put("title", cls.title)
                put("summary", cls.summary)
                put("durationMinutes", cls.durationMinutes)
                put("isPublished", cls.isPublished)
                put("learningObjectives", JSONArray(cls.learningObjectives))

                cls.pdfMedia?.let { put("pdfMedia", serializeMedia(it)) }
                cls.audioMedia?.let { put("audioMedia", serializeMedia(it)) }
                cls.videoMedia?.let { put("videoMedia", serializeMedia(it)) }
                val attArr = JSONArray()
                cls.attachments.forEach { attArr.put(serializeMedia(it)) }
                put("attachments", attArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString("classes_json", arr.toString()).apply()
    }

    private fun deserializeClasses(json: String): List<CourseClass> {
        val result = mutableListOf<CourseClass>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val objectivesJson = obj.optJSONArray("learningObjectives")
            val objectives = mutableListOf<String>()
            if (objectivesJson != null) {
                for (j in 0 until objectivesJson.length()) {
                    objectives.add(objectivesJson.getString(j))
                }
            }

            val pdfObj = obj.optJSONObject("pdfMedia")
            val audioObj = obj.optJSONObject("audioMedia")
            val videoObj = obj.optJSONObject("videoMedia")
            val attachmentsArr = obj.optJSONArray("attachments")
            val attachments = mutableListOf<LessonMedia>()
            if (attachmentsArr != null) {
                for (j in 0 until attachmentsArr.length()) {
                    val mObj = attachmentsArr.optJSONObject(j)
                    if (mObj != null) {
                        attachments.add(deserializeMedia(mObj))
                    }
                }
            }
            val title = obj.getString("title")

            result.add(
                CourseClass(
                    id = obj.getString("id"),
                    moduleId = obj.getString("moduleId"),
                    orderIndex = obj.getInt("orderIndex"),
                    title = title,
                    summary = obj.getString("summary"),
                    durationMinutes = obj.optInt("durationMinutes", 30),
                    isPublished = obj.optBoolean("isPublished", true),
                    learningObjectives = objectives,
                    pdfMedia = pdfObj?.let { deserializeMedia(it) },
                    audioMedia = audioObj?.let { deserializeMedia(it) },
                    videoMedia = videoObj?.let { deserializeMedia(it) },
                    quizQuestions = generateClassQuizzes(title),
                    attachments = attachments,
                    lectureCode = obj.optString("lectureCode", "")
                )
            )
        }
        return result
    }

    private fun serializeMedia(media: LessonMedia): JSONObject {
        return JSONObject().apply {
            put("id", media.id)
            put("type", media.type.name)
            put("fileName", media.fileName)
            put("fileSizeFormatted", media.fileSizeFormatted)
            put("fileSizeBytes", media.fileSizeBytes)
            put("url", media.url)
            put("title", media.title)
            put("description", media.description)
            put("mimeType", media.mimeType)
            put("cloudFileId", media.cloudFileId)
            put("cloudChunkCount", media.cloudChunkCount)
            put("durationFormatted", media.durationFormatted ?: "")
            put("uploadTimestamp", media.uploadTimestamp)
        }
    }

    private fun deserializeMedia(obj: JSONObject): LessonMedia {
        val rawType = obj.optString("type", MediaType.DOCUMENT.name)
        val parsedType = try {
            MediaType.valueOf(rawType)
        } catch (_: Exception) {
            MediaType.DOCUMENT
        }
        return LessonMedia(
            type = parsedType,
            fileName = obj.getString("fileName"),
            fileSizeFormatted = obj.optString("fileSizeFormatted", "0 B"),
            fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
            url = obj.optString("url", ""),
            durationFormatted = obj.optString("durationFormatted", "").ifEmpty { null },
            uploadTimestamp = obj.optLong("uploadTimestamp", System.currentTimeMillis()),
            id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() },
            title = obj.optString("title", ""),
            description = obj.optString("description", ""),
            mimeType = obj.optString("mimeType", ""),
            cloudFileId = obj.optString("cloudFileId", ""),
            cloudChunkCount = obj.optInt("cloudChunkCount", 0)
        )
    }

    private fun serializeDeveloperProfile(profile: DeveloperProfile): String {
        val obj = JSONObject().apply {
            put("name", profile.name)
            put("designation", profile.designation)
            put("biography", profile.biography)
            put("educationBackground", profile.educationBackground)
            put("workExperience", profile.workExperience)
            put("skillsAndExpertise", JSONArray(profile.skillsAndExpertise))
            put("projectInformation", profile.projectInformation)
            put("contactEmail", profile.contactEmail)
            put("contactPhone", profile.contactPhone)
            put("contactWebsite", profile.contactWebsite)
            put("location", profile.location)
            put("photoUri", profile.photoUri)
        }
        return obj.toString()
    }

    private fun deserializeDeveloperProfile(json: String): DeveloperProfile {
        return try {
            val obj = JSONObject(json)
            val skillsArr = obj.optJSONArray("skillsAndExpertise")
            val skills = mutableListOf<String>()
            if (skillsArr != null) {
                for (i in 0 until skillsArr.length()) {
                    skills.add(skillsArr.getString(i))
                }
            }
            val defaults = DeveloperProfile()
            sanitizeDeveloperProfile(
                DeveloperProfile(
                    name = obj.optString("name", defaults.name),
                    designation = obj.optString("designation", defaults.designation),
                    biography = obj.optString("biography", defaults.biography),
                    educationBackground = obj.optString("educationBackground", defaults.educationBackground),
                    workExperience = obj.optString("workExperience", defaults.workExperience),
                    skillsAndExpertise = if (skills.isEmpty()) defaults.skillsAndExpertise else skills,
                    projectInformation = obj.optString("projectInformation", defaults.projectInformation),
                    contactEmail = obj.optString("contactEmail", defaults.contactEmail),
                    contactPhone = obj.optString("contactPhone", defaults.contactPhone),
                    contactWebsite = obj.optString("contactWebsite", defaults.contactWebsite),
                    location = obj.optString("location", defaults.location),
                    photoUri = obj.optString("photoUri", "")
                )
            )
        } catch (_: Exception) {
            DeveloperProfile()
        }
    }

    private fun serializeFounderProfile(profile: FounderProfile): String {
        val obj = JSONObject().apply {
            put("name", profile.name)
            put("designation", profile.designation)
            put("professionalIdentity", profile.professionalIdentity)
            put("additionalRole", profile.additionalRole)
            put("currentOrganization", profile.currentOrganization)
            put("mobileNumber", profile.mobileNumber)
            put("email", profile.email)
            put("educationBackground", profile.educationBackground)
            put("professionalExperience", profile.professionalExperience)
            put("biography", profile.biography)
            put("skillsAndExpertise", JSONArray(profile.skillsAndExpertise))
            put("location", profile.location)
            put("photoUri", profile.photoUri)
        }
        return obj.toString()
    }

    private fun deserializeFounderProfile(json: String): FounderProfile {
        return try {
            val obj = JSONObject(json)
            val skillsArr = obj.optJSONArray("skillsAndExpertise")
            val skills = mutableListOf<String>()
            if (skillsArr != null) {
                for (i in 0 until skillsArr.length()) {
                    skills.add(skillsArr.getString(i))
                }
            }
            val defaults = FounderProfile()
            FounderProfile(
                name = obj.optString("name", defaults.name),
                designation = obj.optString("designation", defaults.designation),
                professionalIdentity = obj.optString("professionalIdentity", defaults.professionalIdentity),
                additionalRole = obj.optString("additionalRole", defaults.additionalRole),
                currentOrganization = obj.optString("currentOrganization", defaults.currentOrganization),
                mobileNumber = obj.optString("mobileNumber", defaults.mobileNumber),
                email = obj.optString("email", defaults.email),
                educationBackground = obj.optString("educationBackground", defaults.educationBackground),
                professionalExperience = obj.optString("professionalExperience", defaults.professionalExperience),
                biography = obj.optString("biography", defaults.biography),
                skillsAndExpertise = if (skills.isEmpty()) defaults.skillsAndExpertise else skills,
                location = obj.optString("location", defaults.location),
                photoUri = obj.optString("photoUri", "")
            )
        } catch (_: Exception) {
            FounderProfile()
        }
    }

    private fun persistMembers(list: List<IscebMember>) {
        prefs.edit().putString("isceb_members_json", serializeMembers(list)).apply()
    }

    private fun serializeMembers(list: List<IscebMember>): String {
        val arr = JSONArray()
        list.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("photoUri", m.photoUri)
                put("builtInPhotoKey", m.builtInPhotoKey)
                put("fullName", m.fullName)
                put("designation", m.designation)
                put("professionalIdentity", m.professionalIdentity)
                put("currentOrganization", m.currentOrganization)
                put("educationalQualification", m.educationalQualification)
                put("professionalExperience", m.professionalExperience)
                put("skillsAndExpertise", JSONArray(m.skillsAndExpertise))
                put("roleInIsceb", m.roleInIsceb)
                put("contactInformation", m.contactInformation)
                put("biography", m.biography)
                put("displayOrder", m.displayOrder)
                put("updatedAt", m.updatedAt)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeMembers(json: String): List<IscebMember> {
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<IscebMember>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val skillsArr = obj.optJSONArray("skillsAndExpertise")
                val skills = mutableListOf<String>()
                if (skillsArr != null) {
                    for (j in 0 until skillsArr.length()) {
                        val s = skillsArr.optString(j, "").trim()
                        if (s.isNotBlank()) skills.add(s)
                    }
                }
                result.add(
                    IscebMember(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        photoUri = obj.optString("photoUri", ""),
                        builtInPhotoKey = obj.optString("builtInPhotoKey", ""),
                        fullName = obj.optString("fullName", ""),
                        designation = obj.optString("designation", ""),
                        professionalIdentity = obj.optString("professionalIdentity", ""),
                        currentOrganization = obj.optString("currentOrganization", ""),
                        educationalQualification = obj.optString("educationalQualification", ""),
                        professionalExperience = obj.optString("professionalExperience", ""),
                        skillsAndExpertise = skills,
                        roleInIsceb = obj.optString("roleInIsceb", ""),
                        contactInformation = obj.optString("contactInformation", ""),
                        biography = obj.optString("biography", ""),
                        displayOrder = obj.optInt("displayOrder", i + 1),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistExams(list: List<ModuleExam>) {
        val arr = JSONArray()
        list.forEach { exam ->
            val obj = JSONObject().apply {
                put("id", exam.id)
                put("moduleId", exam.moduleId)
                put("title", exam.title)
                put("totalMarks", exam.totalMarks)
                put("isSyllabusFixedMarks", exam.isSyllabusFixedMarks)
                put("passingPercentage", exam.passingPercentage)
                put("durationMinutes", exam.durationMinutes)
                put("scheduledWindowNote", exam.scheduledWindowNote)
            }
            arr.put(obj)
        }
        prefs.edit().putString("exams_json", arr.toString()).apply()
    }

    private fun deserializeExams(json: String): List<ModuleExam> {
        val defaults = getInitialModuleExams().associateBy { it.id }
        val result = mutableListOf<ModuleExam>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.getString("id")
            val defaultExam = defaults[id]
            result.add(
                ModuleExam(
                    id = id,
                    moduleId = obj.getString("moduleId"),
                    title = obj.getString("title"),
                    totalMarks = obj.getInt("totalMarks"),
                    isSyllabusFixedMarks = obj.optBoolean("isSyllabusFixedMarks", false),
                    passingPercentage = obj.optInt("passingPercentage", 60),
                    durationMinutes = obj.optInt("durationMinutes", 30),
                    questions = defaultExam?.questions ?: emptyList(),
                    scheduledWindowNote = obj.optString("scheduledWindowNote", "Class hours: 9:00 PM to 11:00 PM (Asia/Dhaka)")
                )
            )
        }
        return if (result.isEmpty()) getInitialModuleExams() else result
    }

    private fun persistExamAttempts(list: List<ExamAttempt>) {
        val arr = JSONArray()
        list.forEach { att ->
            val obj = JSONObject().apply {
                put("attemptId", att.attemptId)
                put("examId", att.examId)
                put("studentId", att.studentId)
                put("scoreMarks", att.scoreMarks)
                put("totalMarks", att.totalMarks)
                put("percentage", att.percentage)
                put("isPassed", att.isPassed)
                put("startTimeMs", att.startTimeMs)
                put("submittedTimeMs", att.submittedTimeMs)
                put("isAutoSubmittedOnTimeout", att.isAutoSubmittedOnTimeout)
                val answersObj = JSONObject()
                att.studentAnswers.forEach { (q, a) -> answersObj.put(q, a) }
                put("studentAnswers", answersObj)
            }
            arr.put(obj)
        }
        prefs.edit().putString("exam_attempts_json", arr.toString()).apply()
    }

    private fun deserializeExamAttempts(json: String): List<ExamAttempt> {
        val list = mutableListOf<ExamAttempt>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val answersObj = obj.optJSONObject("studentAnswers")
            val answersMap = mutableMapOf<String, Int>()
            answersObj?.keys()?.forEach { k ->
                answersMap[k] = answersObj.getInt(k)
            }
            list.add(
                ExamAttempt(
                    attemptId = obj.getString("attemptId"),
                    examId = obj.getString("examId"),
                    studentId = obj.getString("studentId"),
                    studentAnswers = answersMap,
                    scoreMarks = obj.getInt("scoreMarks"),
                    totalMarks = obj.getInt("totalMarks"),
                    percentage = obj.getInt("percentage"),
                    isPassed = obj.getBoolean("isPassed"),
                    startTimeMs = obj.getLong("startTimeMs"),
                    submittedTimeMs = obj.getLong("submittedTimeMs"),
                    isAutoSubmittedOnTimeout = obj.optBoolean("isAutoSubmittedOnTimeout", false)
                )
            )
        }
        return list
    }

    private fun persistAssignments(list: List<ModuleAssignment>) {
        val arr = JSONArray()
        list.forEach { asg ->
            val obj = JSONObject().apply {
                put("id", asg.id)
                put("moduleId", asg.moduleId)
                put("title", asg.title)
                put("topicDescription", asg.topicDescription)
                put("instructions", asg.instructions)
                put("maxMarks", asg.maxMarks)
                put("isFinalProject", asg.isFinalProject)
            }
            arr.put(obj)
        }
        prefs.edit().putString("assignments_json", arr.toString()).apply()
    }

    private fun deserializeAssignments(json: String): List<ModuleAssignment> {
        val list = mutableListOf<ModuleAssignment>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                ModuleAssignment(
                    id = obj.getString("id"),
                    moduleId = obj.getString("moduleId"),
                    title = obj.getString("title"),
                    topicDescription = obj.getString("topicDescription"),
                    instructions = obj.getString("instructions"),
                    maxMarks = obj.optInt("maxMarks", 20),
                    isFinalProject = obj.optBoolean("isFinalProject", false)
                )
            )
        }
        return if (list.isEmpty()) getInitialModuleAssignments() else list
    }

    private fun persistAssignmentSubmissions(list: List<AssignmentSubmission>) {
        val arr = JSONArray()
        list.forEach { sub ->
            val obj = JSONObject().apply {
                put("submissionId", sub.submissionId)
                put("assignmentId", sub.assignmentId)
                put("studentId", sub.studentId)
                put("studentName", sub.studentName)
                put("submissionText", sub.submissionText)
                sub.attachedFileName?.let { put("attachedFileName", it) }
                put("submissionTimestamp", sub.submissionTimestamp)
                sub.marksAwarded?.let { put("marksAwarded", it) }
                sub.feedback?.let { put("feedback", it) }
                put("isGraded", sub.isGraded)
            }
            arr.put(obj)
        }
        prefs.edit().putString("assignment_submissions_json", arr.toString()).apply()
    }

    private fun deserializeAssignmentSubmissions(json: String): List<AssignmentSubmission> {
        val list = mutableListOf<AssignmentSubmission>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                AssignmentSubmission(
                    submissionId = obj.getString("submissionId"),
                    assignmentId = obj.getString("assignmentId"),
                    studentId = obj.getString("studentId"),
                    studentName = obj.getString("studentName"),
                    submissionText = obj.getString("submissionText"),
                    attachedFileName = if (obj.has("attachedFileName")) obj.getString("attachedFileName") else null,
                    submissionTimestamp = obj.getLong("submissionTimestamp"),
                    marksAwarded = if (obj.has("marksAwarded")) obj.getInt("marksAwarded") else null,
                    feedback = if (obj.has("feedback")) obj.getString("feedback") else null,
                    isGraded = obj.optBoolean("isGraded", false)
                )
            )
        }
        return list
    }

    // ================= INITIAL DATA: EXACT SYLLABUS =================

    private fun getInitialModules(): List<CourseModule> = listOf(
        // PART 1 / BASIC (FOUNDATION LEVEL) • 28 CLASSES
        CourseModule(
            id = "mod-1",
            number = 1,
            title = "Concept of SCM and Demand Forecasting & Planning",
            description = "Orientation, core definitions, cross-functional flow, demand planning/forecasting principles, and Module 01 Exam (20 Marks).",
            part = CoursePart.PART_1_BASIC,
            category = "Foundation",
            estimatedHours = 5,
            iconName = "hub",
            isRequired = true
        ),
        CourseModule(
            id = "mod-2",
            number = 2,
            title = "Sourcing & Vendor Management",
            description = "Strategic sourcing, negotiations, vendor management & selection, enlistment process, and Assignment & Exam (25 Marks).",
            part = CoursePart.PART_1_BASIC,
            category = "Sourcing",
            estimatedHours = 5,
            iconName = "shopping_cart",
            isRequired = true
        ),
        CourseModule(
            id = "mod-3",
            number = 3,
            title = "Procurement (Local & International)",
            description = "Local procurement, PO/WO, purchase register & bill checklist, complete import workflow, LC types, PI/Incoterms/HS Code, export process, and Assignment & Exam (30 Marks).",
            part = CoursePart.PART_1_BASIC,
            category = "Procurement",
            estimatedHours = 9,
            iconName = "description",
            isRequired = true
        ),
        CourseModule(
            id = "mod-4",
            number = 4,
            title = "Inventory, Warehouse, Distribution & Logistics",
            description = "Inventory management systems, warehouse management systems, manufacturing & production, delivery & distribution, and transportation & logistics.",
            part = CoursePart.PART_1_BASIC,
            category = "Operations",
            estimatedHours = 5,
            iconName = "warehouse",
            isRequired = true
        ),
        CourseModule(
            id = "mod-5",
            number = 5,
            title = "Billing Management & CRM",
            description = "Bill process, bond & VAT-Tax compliance, and Customer Relationship Management (CRM) in supply chains.",
            part = CoursePart.PART_1_BASIC,
            category = "Finance & CRM",
            estimatedHours = 2,
            iconName = "account_balance",
            isRequired = true
        ),
        CourseModule(
            id = "mod-6",
            number = 6,
            title = "SOP & QMS",
            description = "Standard Operating Procedures (SOP), Quality Management Systems (QMS), and Module 06 Exam (25 Marks).",
            part = CoursePart.PART_1_BASIC,
            category = "Quality & Standards",
            estimatedHours = 2,
            iconName = "verified",
            isRequired = true
        ),

        // PART 2 / ADVANCE (STRATEGIC & ANALYTICAL LEVEL) • 20 CLASSES
        CourseModule(
            id = "mod-7",
            number = 7,
            title = "Introduction to Supply Chain Management",
            description = "Strategic SCM overview, integration & global trends, SCM vs Logistics, and 3PL/4PL, reverse logistics & last-mile delivery.",
            part = CoursePart.PART_2_ADVANCE,
            category = "Strategy",
            estimatedHours = 3,
            iconName = "trending_up",
            isRequired = true
        ),
        CourseModule(
            id = "mod-8",
            number = 8,
            title = "Procurement & Sourcing Management",
            description = "Procurement policies, strategic sourcing, digital procurement, supplier evaluation, make-or-buy decisions, and cost management.",
            part = CoursePart.PART_2_ADVANCE,
            category = "Advanced Sourcing",
            estimatedHours = 3,
            iconName = "local_shipping",
            isRequired = true
        ),
        CourseModule(
            id = "mod-9",
            number = 9,
            title = "Inventory, Warehouse & Operations Management",
            description = "Inventory fundamentals, control models, warehouse design & space optimization, production/capacity planning, Lean, JIT & TQM.",
            part = CoursePart.PART_2_ADVANCE,
            category = "Advanced Operations",
            estimatedHours = 3,
            iconName = "inventory_2",
            isRequired = true
        ),
        CourseModule(
            id = "mod-10",
            number = 10,
            title = "International Trade & Supply Chain Finance",
            description = "Letter of Credit (L/C), banking procedures, Incoterms, customs clearance, freight forwarder selection & landed cost, case study, and working capital.",
            part = CoursePart.PART_2_ADVANCE,
            category = "Global Finance",
            estimatedHours = 6,
            iconName = "monetization_on",
            isRequired = true
        ),
        CourseModule(
            id = "mod-11",
            number = 11,
            title = "Supply Chain Planning, Strategy & Technology",
            description = "Demand planning, MRP & DRP systems, and digital supply chain integration across ERP, SAP, IoT, AI, Big Data, Analytics & Blockchain.",
            part = CoursePart.PART_2_ADVANCE,
            category = "Digital Tech & ERP",
            estimatedHours = 2,
            iconName = "memory",
            isRequired = true
        ),
        CourseModule(
            id = "mod-12",
            number = 12,
            title = "Sustainable Supply Chain & Project Work",
            description = "Waste reduction, Green SCM, CSR, Green Procurement & Eco-Design, Group Discussion & Course Review, and Final Exam (100 Marks).",
            part = CoursePart.PART_2_ADVANCE,
            category = "Capstone & Sustainability",
            estimatedHours = 3,
            iconName = "eco",
            isRequired = true
        )
    )

    private fun getInitialClasses(): List<CourseClass> = listOf(
        // ================= PART 1 / BASIC (FOUNDATION LEVEL) • 28 CLASSES =================
        // MODULE 01 | CONCEPT OF SCM AND DEMAND FORECASTING & PLANNING (01-05)
        CourseClass(
            id = "class-1",
            moduleId = "mod-1",
            orderIndex = 1,
            lectureCode = "01",
            title = "Orientation and Concept of SCM",
            summary = "Course roadmap, ISCEB standards, defining modern supply chains, and three fundamental flows (material, information, financial).",
            durationMinutes = 30,
            learningObjectives = listOf("ISCEB PGD course expectations", "Definitions and scope of SCM", "3 foundational flows")
        ),
        CourseClass(
            id = "class-2",
            moduleId = "mod-1",
            orderIndex = 2,
            lectureCode = "02",
            title = "Basic of SCM",
            summary = "The 5 pillars: Plan, Source, Make, Deliver, Return. Functional silos vs integrated end-to-end supply chains.",
            durationMinutes = 30,
            learningObjectives = listOf("SCOR framework overview", "Breaking down functional silos", "Supply chain drivers")
        ),
        CourseClass(
            id = "class-3",
            moduleId = "mod-1",
            orderIndex = 3,
            lectureCode = "03",
            title = "Demand Forecasting & Planning - Basic",
            summary = "Foundational principles of demand planning, qualitative vs quantitative forecasting, and seasonal demand factors.",
            durationMinutes = 30,
            learningObjectives = listOf("Principles of demand forecasting", "Moving average & exponential smoothing", "Managing forecast volatility")
        ),
        CourseClass(
            id = "class-4",
            moduleId = "mod-1",
            orderIndex = 4,
            lectureCode = "04",
            title = "Demand Forecasting & Planning - PR, SR & Forecasting",
            summary = "Translating forecasts into Purchase Requisition (PR), Store Requisition (SR), and master planning schedules.",
            durationMinutes = 30,
            learningObjectives = listOf("Purchase Requisition generation", "Store Requisition workflows", "Demand aggregation")
        ),
        CourseClass(
            id = "class-5",
            moduleId = "mod-1",
            orderIndex = 5,
            lectureCode = "05",
            title = "Exam (20 Marks)",
            summary = "Module 01 formal assessment evaluating Concept of SCM and Demand Forecasting & Planning (20 Marks).",
            durationMinutes = 30,
            learningObjectives = listOf("SCM concept mastery check", "Demand planning application", "PR & SR operational assessment")
        ),

        // MODULE 02 | SOURCING & VENDOR MANAGEMENT (06-10)
        CourseClass(
            id = "class-6",
            moduleId = "mod-2",
            orderIndex = 1,
            lectureCode = "06",
            title = "Sourcing",
            summary = "Strategic sourcing methodology, supplier discovery, market benchmarking, and total cost of ownership (TCO).",
            durationMinutes = 30,
            learningObjectives = listOf("7-step sourcing process", "Supplier market research", "TCO calculations")
        ),
        CourseClass(
            id = "class-7",
            moduleId = "mod-2",
            orderIndex = 2,
            lectureCode = "07",
            title = "Negotiations",
            summary = "Principles of commercial negotiation, BATNA preparation, bargaining zones, and win-win supplier partnerships.",
            durationMinutes = 30,
            learningObjectives = listOf("BATNA preparation", "Negotiation strategies in procurement", "Contract closure techniques")
        ),
        CourseClass(
            id = "class-8",
            moduleId = "mod-2",
            orderIndex = 3,
            lectureCode = "08",
            title = "Vendor Management & Selection",
            summary = "Vendor lifecycle management, scorecards, selection matrices, relationship tiers, and SLA monitoring mechanisms.",
            durationMinutes = 30,
            learningObjectives = listOf("Supplier KPI scorecards", "Vendor selection criteria", "Mitigating supplier concentration risks")
        ),
        CourseClass(
            id = "class-9",
            moduleId = "mod-2",
            orderIndex = 4,
            lectureCode = "09",
            title = "Vendor Enlistment Process",
            summary = "Standard vendor enlistment documentation, legal compliance checks, financial vetting, and audit trails.",
            durationMinutes = 30,
            learningObjectives = listOf("Enlistment document verification", "Due diligence and statutory compliance", "Vendor audit workflows")
        ),
        CourseClass(
            id = "class-10",
            moduleId = "mod-2",
            orderIndex = 5,
            lectureCode = "10",
            title = "Assignment & Exam (25 Marks)",
            summary = "Module 02 practical assignment and formal examination on Sourcing & Vendor Management (25 Marks).",
            durationMinutes = 40,
            learningObjectives = listOf("Strategic sourcing application", "Vendor scorecard & negotiation evaluation", "Module 02 exam readiness")
        ),

        // MODULE 03 | PROCUREMENT (LOCAL & INTERNATIONAL) (11-19)
        CourseClass(
            id = "class-11",
            moduleId = "mod-3",
            orderIndex = 1,
            lectureCode = "11",
            title = "Basic of Procurement",
            summary = "Commercial procurement cycles, requisition review, compliance checks, and authorization limits.",
            durationMinutes = 30,
            learningObjectives = listOf("Procure-to-Pay (P2P) cycle", "Procurement ethics", "Authorization matrices")
        ),
        CourseClass(
            id = "class-12",
            moduleId = "mod-3",
            orderIndex = 2,
            lectureCode = "12",
            title = "Local Procurement - PO, WO, Purchase Register & Bill Check List",
            summary = "Drafting Purchase Orders (PO) and Work Orders (WO), maintaining compliant Purchase Registers, and executing Bill Check Lists.",
            durationMinutes = 30,
            learningObjectives = listOf("Drafting legally binding POs/WOs", "Purchase register maintenance", "Bill check list & 3-way matching")
        ),
        CourseClass(
            id = "class-13",
            moduleId = "mod-3",
            orderIndex = 3,
            lectureCode = "13",
            title = "Import - Basic",
            summary = "Fundamentals of international purchasing, regulatory frameworks, foreign exchange regulations, and country import policies.",
            durationMinutes = 30,
            learningObjectives = listOf("Import policy orders", "Foreign exchange regulations", "Import permits & IRC requirements")
        ),
        CourseClass(
            id = "class-14",
            moduleId = "mod-3",
            orderIndex = 4,
            lectureCode = "14",
            title = "Import - Flow Chart",
            summary = "End-to-end import visual flow chart from indenting and opening LC to port discharge and final warehouse delivery.",
            durationMinutes = 30,
            learningObjectives = listOf("Visualizing import milestones", "Banking & customs coordination", "Lead time buffer control")
        ),
        CourseClass(
            id = "class-15",
            moduleId = "mod-3",
            orderIndex = 5,
            lectureCode = "15",
            title = "Import - LC Types",
            summary = "Comprehensive breakdown of Letters of Credit: Sight, Usance/Deferred, Irrevocable, Back-to-Back, Revolving, and Standby LCs.",
            durationMinutes = 30,
            learningObjectives = listOf("UCP 600 rules", "Irrevocable Sight vs Usance LCs", "Back-to-Back LC mechanisms")
        ),
        CourseClass(
            id = "class-16",
            moduleId = "mod-3",
            orderIndex = 6,
            lectureCode = "16",
            title = "Import - Proforma Invoice, Incoterms & H.S Code",
            summary = "Scrutiny of Proforma Invoices (PI), Incoterms 2020 obligations (FOB, CFR, CIF, DDP), and Harmonized System (HS) code classification.",
            durationMinutes = 30,
            learningObjectives = listOf("Incoterms 2020 risk transfer points", "HS Code tariff determination", "PI clause validation")
        ),
        CourseClass(
            id = "class-17",
            moduleId = "mod-3",
            orderIndex = 7,
            lectureCode = "17",
            title = "Import - All Documents",
            summary = "Detailed analysis of Commercial Invoice, Packing List, Bill of Lading (B/L) / Air Waybill (AWB), Certificate of Origin, and Inspection Certificates.",
            durationMinutes = 30,
            learningObjectives = listOf("B/L vs AWB legal distinctions", "Certificate of Origin rules", "Discrepancy prevention")
        ),
        CourseClass(
            id = "class-18",
            moduleId = "mod-3",
            orderIndex = 8,
            lectureCode = "18",
            title = "Export Process",
            summary = "Export processing workflows, EXP issuance, customs bonded warehouse licensing, export documentation, and duty drawback facilities.",
            durationMinutes = 30,
            learningObjectives = listOf("Export documentation workflow", "Bonded warehouse facilities", "Duty drawback utilization")
        ),
        CourseClass(
            id = "class-19",
            moduleId = "mod-3",
            orderIndex = 9,
            lectureCode = "19",
            title = "Assignment & Exam (30 Marks)",
            summary = "Module 03 practical assignment and comprehensive examination on Local & International Procurement (30 Marks).",
            durationMinutes = 45,
            learningObjectives = listOf("Local PO/WO & import compliance check", "LC & Incoterms case assessment", "Export-import synthesis")
        ),

        // MODULE 04 | INVENTORY, WAREHOUSE, DISTRIBUTION & LOGISTICS (20-24)
        CourseClass(
            id = "class-20",
            moduleId = "mod-4",
            orderIndex = 1,
            lectureCode = "20",
            title = "Inventory Management System",
            summary = "Inventory ledger maintenance, stock valuation (FIFO, LIFO, Weighted Average), and physical stock reconciliation.",
            durationMinutes = 30,
            learningObjectives = listOf("Inventory valuation models", "Cycle counting vs annual audit", "Shrinkage management")
        ),
        CourseClass(
            id = "class-21",
            moduleId = "mod-4",
            orderIndex = 2,
            lectureCode = "21",
            title = "Warehouse Management System",
            summary = "WMS architecture, warehouse zones, material handling equipment, slotting strategies, and barcode/RFID tracking.",
            durationMinutes = 30,
            learningObjectives = listOf("Warehouse zone optimization", "Cross-docking and slotting", "WMS software architecture")
        ),
        CourseClass(
            id = "class-22",
            moduleId = "mod-4",
            orderIndex = 3,
            lectureCode = "22",
            title = "Manufacturing & Production",
            summary = "Production scheduling, bill of materials (BOM), work-in-progress (WIP) tracking, and capacity bottleneck management.",
            durationMinutes = 30,
            learningObjectives = listOf("Bill of Materials structures", "WIP tracking", "Capacity requirement planning")
        ),
        CourseClass(
            id = "class-23",
            moduleId = "mod-4",
            orderIndex = 4,
            lectureCode = "23",
            title = "Delivery & Distribution",
            summary = "Fulfillment workflows, order picking systems (wave, batch, zone), staging, dispatch, and proof-of-delivery (POD).",
            durationMinutes = 30,
            learningObjectives = listOf("Picking strategies", "Order staging and dispatch", "Electronic POD (e-POD)")
        ),
        CourseClass(
            id = "class-24",
            moduleId = "mod-4",
            orderIndex = 5,
            lectureCode = "24",
            title = "Transportation & Logistics",
            summary = "Freight modal selection (road, rail, ocean, air), carrier contracts, freight consolidation, and container load optimization.",
            durationMinutes = 30,
            learningObjectives = listOf("Modal economics and transit times", "FCL vs LCL consolidation", "Fleet routing tactics")
        ),

        // MODULE 05 | BILLING MANAGEMENT & CRM (25-26)
        CourseClass(
            id = "class-25",
            moduleId = "mod-5",
            orderIndex = 1,
            lectureCode = "25",
            title = "Bill Process, Bond & VAT-Tax",
            summary = "Supplier bill processing, customs bond reconciliation, statutory VAT deduction at source (VDS), withholding tax regulations, and audit compliance.",
            durationMinutes = 30,
            learningObjectives = listOf("Bill verification procedures", "Bond reconciliation & VAT Deduction at Source", "Tax compliance in procurement")
        ),
        CourseClass(
            id = "class-26",
            moduleId = "mod-5",
            orderIndex = 2,
            lectureCode = "26",
            title = "Customer Relationship Management",
            summary = "Customer order fulfillment cycles, On-Time In-Full (OTIF) metrics, customer dispute resolution, and retention strategies.",
            durationMinutes = 30,
            learningObjectives = listOf("OTIF calculation", "Customer service level agreements", "Collaborative forecasting with clients")
        ),

        // MODULE 06 | SOP & QMS (27-28)
        CourseClass(
            id = "class-27",
            moduleId = "mod-6",
            orderIndex = 1,
            lectureCode = "27",
            title = "SOP & QMS",
            summary = "Designing bulletproof Standard Operating Procedures (SOP) for logistics, ISO 9001 Quality Management System architecture, and continuous improvement (Kaizen/PDCA).",
            durationMinutes = 30,
            learningObjectives = listOf("Standard Operating Procedure drafting", "ISO 9001 Quality Management integration", "Root-cause analysis (5 Whys)")
        ),
        CourseClass(
            id = "class-28",
            moduleId = "mod-6",
            orderIndex = 2,
            lectureCode = "28",
            title = "Exam (25 Marks)",
            summary = "Module 06 formal examination covering SOP & QMS operational governance (25 Marks).",
            durationMinutes = 35,
            learningObjectives = listOf("SOP & QMS mastery verification", "ISO 9001 compliance checks", "Continuous improvement assessment")
        ),

        // ================= PART 2 / ADVANCE (STRATEGIC & ANALYTICAL LEVEL) • 20 CLASSES =================
        // MODULE 07 | INTRODUCTION TO SUPPLY CHAIN MANAGEMENT (29-31)
        CourseClass(
            id = "class-29",
            moduleId = "mod-7",
            orderIndex = 1,
            lectureCode = "29",
            title = "SCM - Overview, Key Concepts, Components, Integration & Global Trends",
            summary = "Strategic SCM overview, key components, upstream-downstream integration, geopolitical disruption responses, and global trends.",
            durationMinutes = 30,
            learningObjectives = listOf("Evolution of supply chain paradigms", "Enterprise supply chain integration", "Global trends & resilience")
        ),
        CourseClass(
            id = "class-30",
            moduleId = "mod-7",
            orderIndex = 2,
            lectureCode = "30",
            title = "SCM vs Logistics: Scope, Transportation & Distribution Systems",
            summary = "Differentiating SCM strategy from tactical logistics, distribution network modeling, and hub-and-spoke vs direct delivery.",
            durationMinutes = 30,
            learningObjectives = listOf("Logistics vs SCM conceptual boundaries", "Hub-and-spoke distribution networks", "Total logistics cost curve")
        ),
        CourseClass(
            id = "class-31",
            moduleId = "mod-7",
            orderIndex = 3,
            lectureCode = "31",
            title = "3PL/4PL, Reverse Logistics & Last-Mile Delivery",
            summary = "Contract logistics (3PL/4PL) governance, circular reverse logistics for returns, and urban last-mile delivery economics.",
            durationMinutes = 30,
            learningObjectives = listOf("Selecting 3PL vs 4PL providers", "Reverse logistics flow optimization", "Last-mile unit economics")
        ),

        // MODULE 08 | PROCUREMENT & SOURCING MANAGEMENT (32-34)
        CourseClass(
            id = "class-32",
            moduleId = "mod-8",
            orderIndex = 1,
            lectureCode = "32",
            title = "Procurement Management, Policies, Strategic Sourcing & Digital Procurement",
            summary = "Corporate procurement policy design, category management strategies, e-procurement reverse auctions, and spend analysis frameworks.",
            durationMinutes = 30,
            learningObjectives = listOf("Category management methodology", "Digital e-procurement workflows", "Procurement governance models")
        ),
        CourseClass(
            id = "class-33",
            moduleId = "mod-8",
            orderIndex = 2,
            lectureCode = "33",
            title = "Supplier Evaluation, Vendor Selection & Make-or-Buy Decisions",
            summary = "Kraljic portfolio matrix, multi-criteria vendor decision models (AHP), and quantitative make-or-buy financial models.",
            durationMinutes = 30,
            learningObjectives = listOf("Kraljic portfolio matrix", "Analytical Hierarchy Process (AHP)", "Make-or-buy cost models")
        ),
        CourseClass(
            id = "class-34",
            moduleId = "mod-8",
            orderIndex = 3,
            lectureCode = "34",
            title = "Supplier Relationship & Cost Management",
            summary = "Measuring procurement ROI, supplier development programs, target costing, and value engineering (VAVE).",
            durationMinutes = 30,
            learningObjectives = listOf("Procurement ROI & savings metrics", "Value Analysis / Value Engineering", "Supplier development alliances")
        ),

        // MODULE 09 | INVENTORY, WAREHOUSE & OPERATIONS MANAGEMENT (35-37)
        CourseClass(
            id = "class-35",
            moduleId = "mod-9",
            orderIndex = 1,
            lectureCode = "35",
            title = "Inventory Fundamentals, Classification & Control Models",
            summary = "Economic Order Quantity (EOQ), ABC-FSN-VED classification, statistical safety stock, and Re-Order Point (ROP) optimization.",
            durationMinutes = 30,
            learningObjectives = listOf("EOQ with quantity discounts", "Safety stock & ROP formulas", "Multi-criteria ABC/VED matrix")
        ),
        CourseClass(
            id = "class-36",
            moduleId = "mod-9",
            orderIndex = 2,
            lectureCode = "36",
            title = "Warehouse Design, Layout Planning & Space Optimization",
            summary = "Warehouse capacity planning, aisle width calculation, racking systems (VNA, drive-in), and heat map layout modeling.",
            durationMinutes = 30,
            learningObjectives = listOf("Warehouse cubic volume utilization", "Racking configuration trade-offs", "Picking travel time reduction")
        ),
        CourseClass(
            id = "class-37",
            moduleId = "mod-9",
            orderIndex = 3,
            lectureCode = "37",
            title = "Production Planning, Capacity Planning, Lean, JIT & TQM",
            summary = "Master Production Scheduling (MPS), Rough-Cut Capacity Planning (RCCP), Kanban pull systems, and Total Quality Management.",
            durationMinutes = 30,
            learningObjectives = listOf("MPS and RCCP coordination", "Kanban pull mechanism implementation", "Lean waste elimination (Muda)")
        ),

        // MODULE 10 | INTERNATIONAL TRADE & SUPPLY CHAIN FINANCE (38-43)
        CourseClass(
            id = "class-38",
            moduleId = "mod-10",
            orderIndex = 1,
            lectureCode = "38",
            title = "Letter of Credit (L/C), Banking Procedures & Trade Documentation",
            summary = "Detailed UCP 600 articles, ISBP 745 documentation standards, discrepancy resolution, and trade factoring.",
            durationMinutes = 30,
            learningObjectives = listOf("UCP 600 key articles & ISBP 745", "Bank confirmation & negotiation", "Preventing documentary rejections")
        ),
        CourseClass(
            id = "class-39",
            moduleId = "mod-10",
            orderIndex = 2,
            lectureCode = "39",
            title = "Incoterms, Global Trade Rules & Export-Import",
            summary = "Advanced Incoterms 2020 legal nuances, marine insurance obligations, customs jurisdiction, and multilateral trade treaties.",
            durationMinutes = 30,
            learningObjectives = listOf("Incoterms 2020 legal liabilities", "Marine insurance Institute Cargo Clauses (A/B/C)", "Trade compliance audits")
        ),
        CourseClass(
            id = "class-40",
            moduleId = "mod-10",
            orderIndex = 3,
            lectureCode = "40",
            title = "Customs Clearance, Port Operations & Shipping Management",
            summary = "Customs assessment notice, Bill of Entry (B/E) filing, port demurrage & detention avoidance, and shipping agent roles.",
            durationMinutes = 30,
            learningObjectives = listOf("Bill of Entry submission process", "Demurrage vs detention penalty avoidance", "C&F agent management")
        ),
        CourseClass(
            id = "class-41",
            moduleId = "mod-10",
            orderIndex = 4,
            lectureCode = "41",
            title = "Freight Forwarder Selection & Landed Cost",
            summary = "Evaluating international freight forwarders, carrier consolidation benchmarks, and Total Landed Cost calculation models.",
            durationMinutes = 30,
            learningObjectives = listOf("Freight forwarder SLA vetting", "Air vs ocean landed cost modeling", "Customs duties, surcharges & buffer math")
        ),
        CourseClass(
            id = "class-42",
            moduleId = "mod-10",
            orderIndex = 5,
            lectureCode = "42",
            title = "Case Study",
            summary = "Applied industry case study analyzing end-to-end international trade documentation, LC discrepancies, and port clearance bottlenecks.",
            durationMinutes = 30,
            learningObjectives = listOf("Real-world import discrepancy diagnosis", "Landed cost sensitivity simulation", "Executive trade resolution")
        ),
        CourseClass(
            id = "class-43",
            moduleId = "mod-10",
            orderIndex = 6,
            lectureCode = "43",
            title = "Financial Flows, Working Capital, Cost Control & Budgeting",
            summary = "Cash-to-Cash (C2C) cycle calculation, supply chain financing (reverse factoring), and logistics budgeting methods.",
            durationMinutes = 30,
            learningObjectives = listOf("Cash-to-Cash cycle optimization", "Supply Chain Finance & dynamic discounting", "Zero-based logistics budgeting")
        ),

        // MODULE 11 | SUPPLY CHAIN PLANNING, STRATEGY & TECHNOLOGY (44-45)
        CourseClass(
            id = "class-44",
            moduleId = "mod-11",
            orderIndex = 1,
            lectureCode = "44",
            title = "Demand Planning, Forecasting, MRP & DRP Systems",
            summary = "Sales & Operations Planning (S&OP) monthly cycle, Material Requirements Planning (MRP I & II), and Distribution Requirements Planning (DRP).",
            durationMinutes = 30,
            learningObjectives = listOf("Executive S&OP consensus process", "MRP explosion and netting logic", "DRP network calculation")
        ),
        CourseClass(
            id = "class-45",
            moduleId = "mod-11",
            orderIndex = 2,
            lectureCode = "45",
            title = "ERP, SAP, IoT, AI, Big Data, Analytics & Blockchain",
            summary = "Enterprise Resource Planning (ERP/SAP) integration, IoT real-time cold-chain tracking, AI predictive demand, Big Data analytics, and Blockchain traceability.",
            durationMinutes = 30,
            learningObjectives = listOf("SAP SCM module workflows", "IoT & AI predictive supply analytics", "Blockchain provenance tracking")
        ),

        // MODULE 12 | SUSTAINABLE SUPPLY CHAIN & PROJECT WORK (46-48)
        CourseClass(
            id = "class-46",
            moduleId = "mod-12",
            orderIndex = 1,
            lectureCode = "46",
            title = "Waste Reduction, Green SCM, CSR, Green Procurement & Eco-Design",
            summary = "Environmental compliance, waste reduction, circular supply chains, CSR, green procurement criteria, and Scope 1/2/3 carbon accounting.",
            durationMinutes = 30,
            learningObjectives = listOf("Scope 1, 2, 3 carbon accounting", "Green procurement criteria", "Waste reduction & eco-design")
        ),
        CourseClass(
            id = "class-47",
            moduleId = "mod-12",
            orderIndex = 2,
            lectureCode = "47",
            title = "Group Discussion & Course Review",
            summary = "Executive group discussions, capstone synthesis across all 12 modules, and comprehensive pre-final course review.",
            durationMinutes = 30,
            learningObjectives = listOf("Collaborative capstone defense", "Cross-module strategic review", "Addressing stakeholder objections")
        ),
        CourseClass(
            id = "class-48",
            moduleId = "mod-12",
            orderIndex = 3,
            lectureCode = "48",
            title = "Final Exam (100 Marks)",
            summary = "Official ISCEB PGD Final Comprehensive Examination synthesizing Foundation and Strategic Supply Chain Management (100 Marks).",
            durationMinutes = 60,
            learningObjectives = listOf("End-to-end SCM mastery verification", "Quantitative & strategic synthesis", "PGD certification qualification")
        )
    )

    // ================= EXAM SEED DATA (EXACT SYLLABUS MARKS) =================
    // Explicit marks: Mod 1 = 20 marks, Mod 2 = 25 marks, Mod 3 = 30 marks, Mod 6 = 25 marks, Final Exam = 100 marks.

    private fun getInitialModuleExams(): List<ModuleExam> = listOf(
        // Module 1 Exam (EXPLICIT SYLLABUS: 20 MARKS)
        ModuleExam(
            id = "exam-mod-1",
            moduleId = "mod-1",
            title = "Module 1 Examination: Concept of SCM & Demand Planning (20 Marks)",
            totalMarks = 20,
            isSyllabusFixedMarks = true,
            passingPercentage = 60,
            durationMinutes = 20,
            questions = listOf(
                ExamQuestion("q1-1", "Which of the following describes the three foundational flows in a supply chain?", listOf("Material, Information, and Financial flows", "Production, Marketing, and Sales flows", "Import, Export, and Duty flows", "Warehousing, Shipping, and Billing flows"), 0, 5, "SCM synchronizes materials, data/information, and monetary/cash flows."),
                ExamQuestion("q1-2", "What is the primary consequence of uncoordinated demand signals along the supply tier?", listOf("Lower holding costs", "The Bullwhip Effect", "Immediate replenishment", "Zero lead time"), 1, 5, "Unsynchronized orders distort demand variance upstream, causing the Bullwhip Effect."),
                ExamQuestion("q1-3", "In the SCOR framework, which process coordinates aggregate demand and supply?", listOf("Source", "Plan", "Make", "Deliver"), 1, 5, "The Plan process balances resources and demand."),
                ExamQuestion("q1-4", "Which document converts customer sales forecast into internal raw material purchasing needs?", listOf("Purchase Requisition (PR)", "Bill of Lading", "Commercial Invoice", "Work Order"), 0, 5, "PR communicates required quantities from planners to procurement.")
            )
        ),

        // Module 2 Exam (EXPLICIT SYLLABUS: 25 MARKS)
        ModuleExam(
            id = "exam-mod-2",
            moduleId = "mod-2",
            title = "Module 2 Examination: Sourcing & Vendor Management (25 Marks)",
            totalMarks = 25,
            isSyllabusFixedMarks = true,
            passingPercentage = 60,
            durationMinutes = 30,
            questions = listOf(
                ExamQuestion("q2-1", "In strategic negotiation, what does the acronym BATNA represent?", listOf("Best Alternative to a Negotiated Agreement", "Basic Agreement Terms for National Agents", "Benchmark Analysis of Total Network Assets", "Bargaining Advantage Through Net Assets"), 0, 5, "BATNA defines the negotiator's strongest fallback alternative if negotiations collapse."),
                ExamQuestion("q2-2", "Which evaluation metric assesses supplier performance based on price, quality, delivery, and service?", listOf("Vendor Balanced Scorecard", "Gantt Chart", "Break-Even Graph", "Pareto Analysis"), 0, 5, "A vendor balanced scorecard systematically grades multi-criteria supplier performance."),
                ExamQuestion("q2-3", "What is the primary purpose of a formal Vendor Enlistment Process?", listOf("To eliminate all foreign suppliers", "To verify statutory compliance, financial viability, and operational capability", "To increase purchase order lead times", "To minimize inventory turnover"), 1, 5, "Vendor enlistment establishes due diligence, legal verification, and risk mitigation."),
                ExamQuestion("q2-4", "Total Cost of Ownership (TCO) in strategic sourcing includes:", listOf("Only the invoiced purchase price", "Purchase price plus acquisition, maintenance, operating, and disposal costs", "Only customs duty and freight charges", "Only warehousing storage rent"), 1, 5, "TCO captures direct, indirect, lifecycle, and end-of-life supplier costs."),
                ExamQuestion("q2-5", "In supplier relationship management, which tactic is best suited for strategic partnership items?", listOf("Aggressive price spot bidding", "Long-term collaboration, risk sharing, and joint value creation", "Frequent vendor termination", "Informal verbal contracts"), 1, 5, "Strategic items require collaborative, high-trust alliances.")
            )
        ),

        // Module 3 Exam (EXPLICIT SYLLABUS: 30 MARKS)
        ModuleExam(
            id = "exam-mod-3",
            moduleId = "mod-3",
            title = "Module 3 Examination: Procurement Local & International (30 Marks)",
            totalMarks = 30,
            isSyllabusFixedMarks = true,
            passingPercentage = 60,
            durationMinutes = 35,
            questions = listOf(
                ExamQuestion("q3-1", "Under Incoterms 2020 rules, under which term does the seller pay for carriage AND marine insurance to destination?", listOf("FOB (Free on Board)", "EXW (Ex Works)", "CIF (Cost, Insurance and Freight)", "CFR (Cost and Freight)"), 2, 6, "Under CIF, the seller pays freight and provides minimum marine cargo insurance coverage."),
                ExamQuestion("q3-2", "Which type of Letter of Credit cannot be cancelled or amended without the agreement of all parties?", listOf("Revocable LC", "Irrevocable LC", "Red Clause LC", "Transferable LC"), 1, 6, "Under UCP 600, all documentary credits are irrevocably binding unless stipulated otherwise."),
                ExamQuestion("q3-3", "What is the primary legal function of a clean Bill of Lading (B/L)?", listOf("Only a packing declaration", "Document of title, receipt of cargo by carrier, and evidence of contract of carriage", "A government export tax invoice", "A bank loan guarantee"), 1, 6, "A Bill of Lading is a title document, cargo receipt, and freight transport contract."),
                ExamQuestion("q3-4", "What does an 8-digit Harmonized System (HS) code primarily determine in customs clearance?", listOf("Vessel sailing speed", "Applicable customs duties, regulatory duties, and VAT rates", "The buyer's bank credit score", "The number of container seals"), 1, 6, "HS codes classify commodities globally for statutory tariff assessment."),
                ExamQuestion("q3-5", "What facility allows export-oriented manufacturing firms in Bangladesh to import raw materials duty-free?", listOf("Commercial Invoice Voucher", "Customs Bonded Warehouse / Bond License", "Sight Draft Endorsement", "Local Overdraft Facility"), 1, 6, "Bonded warehouse privileges grant duty-free raw material imports for 100% export-oriented goods.")
            )
        ),

        // Module 4 Exam (Configurable, default 20 marks)
        ModuleExam(
            id = "exam-mod-4",
            moduleId = "mod-4",
            title = "Module 4 Assessment: Inventory, Warehouse & Logistics",
            totalMarks = 20,
            isSyllabusFixedMarks = false,
            passingPercentage = 60,
            durationMinutes = 20,
            questions = listOf(
                ExamQuestion("q4-1", "What is the primary benefit of cross-docking in a modern distribution center?", listOf("Eliminating or minimizing intermediate storage and handling", "Increasing long-term warehouse storage capacity", "Multiplying pick path distances", "Doubling order cycle times"), 0, 5, "Cross-docking moves goods directly from inbound to outbound transports with minimal dwell time."),
                ExamQuestion("q4-2", "Which picking methodology involves a worker picking orders for multiple customers simultaneously?", listOf("Single order picking", "Batch picking", "Zone picking", "Bulk pallet picking"), 1, 5, "Batch picking consolidates line items across several orders into a single picking pass."),
                ExamQuestion("q4-3", "What inventory valuation method assumes that the oldest inventory items are sold first?", listOf("LIFO", "FIFO", "Weighted Average", "Standard Cost"), 1, 5, "FIFO (First-In, First-Out) reflects realistic physical flows for perishable/expiring goods."),
                ExamQuestion("q4-4", "Which document verifies that freight was delivered in good condition to the consignee?", listOf("Proof of Delivery (POD)", "Purchase Requisition", "Proforma Invoice", "Work Order"), 0, 5, "Signed POD or e-POD legally confirms fulfillment completion.")
            )
        ),

        // Module 5 Exam (Configurable, default 20 marks)
        ModuleExam(
            id = "exam-mod-5",
            moduleId = "mod-5",
            title = "Module 5 Assessment: Billing Management, Bond, VAT-Tax & CRM",
            totalMarks = 20,
            isSyllabusFixedMarks = false,
            passingPercentage = 60,
            durationMinutes = 20,
            questions = listOf(
                ExamQuestion("q5-1", "In accounts payable matching, what 3 documents are compared before vendor bill payment?", listOf("PO, Goods Received Note (GRN), and Vendor Invoice", "Quotation, Passport, and Bill of Entry", "Credit Card, Bank Statement, and Resume", "Work Order, Sales Brochure, and Audit Report"), 0, 10, "The 3-way match compares the Purchase Order, Receiving Note (GRN), and Vendor Invoice."),
                ExamQuestion("q5-2", "What does the SCM metric OTIF evaluate?", listOf("On-Time In-Full order fulfillment percentage", "Over-Time In Freight costs", "Online Total Inventory Factoring", "Optimal Tariff Import Fee"), 0, 10, "OTIF measures deliveries that arrive precisely on schedule with complete item quantities.")
            )
        ),

        // Module 6 Exam (EXPLICIT SYLLABUS: 25 MARKS)
        ModuleExam(
            id = "exam-mod-6",
            moduleId = "mod-6",
            title = "Module 6 Examination: SOP & QMS (25 Marks)",
            totalMarks = 25,
            isSyllabusFixedMarks = true,
            passingPercentage = 60,
            durationMinutes = 30,
            questions = listOf(
                ExamQuestion("q6-1", "What is the primary role of a Standard Operating Procedure (SOP) in warehouse logistics?", listOf("To provide step-by-step instructions ensuring consistency, safety, and compliance", "To serve solely as marketing material", "To replace all training programs permanently", "To eliminate the need for supervisory staff"), 0, 5, "SOPs establish consistent, measurable operational standards across teams."),
                ExamQuestion("q6-2", "Which standard specifies requirements for a certified Quality Management System (QMS)?", listOf("ISO 14001", "ISO 9001", "ISO 45001", "ISO 22000"), 1, 5, "ISO 9001 is the international benchmark for quality management systems."),
                ExamQuestion("q6-3", "In lean continuous improvement, what does the PDCA cycle stand for?", listOf("Plan, Do, Check, Act", "Purchase, Deliver, Calculate, Audit", "Prepare, Distribute, Count, Authorize", "Plan, Delay, Cancel, Assess"), 0, 5, "PDCA (Plan-Do-Check-Act) is Deming's cycle for iterative quality improvement."),
                ExamQuestion("q6-4", "When investigating an operational defect or shipping error, which root cause technique asks 'Why' iteratively?", listOf("The 5 Whys Analysis", "SWOT Analysis", "Gantt Breakdown", "Break-Even Chart"), 0, 5, "The 5 Whys method drills down past superficial symptoms to the fundamental process failure."),
                ExamQuestion("q6-5", "What is the required consequence when an incoming raw material shipment fails QMS inspection?", listOf("Immediate payment of invoice", "Issuance of Non-Conformance Report (NCR) and material quarantine", "Overlooking minor discrepancies without records", "Discarding all receiving records"), 1, 5, "Failed goods must be quarantined and documented with an NCR pending vendor disposition.")
            )
        ),

        // Module 7 Exam (Configurable, default 20 marks)
        ModuleExam(
            id = "exam-mod-7",
            moduleId = "mod-7",
            title = "Module 7 Assessment: Strategic SCM & Distribution Networks",
            totalMarks = 20,
            isSyllabusFixedMarks = false,
            passingPercentage = 60,
            durationMinutes = 20,
            questions = listOf(
                ExamQuestion("q7-1", "How does a 4PL service provider differ fundamentally from a traditional 3PL provider?", listOf("A 4PL acts as an overarching integrator managing multiple 3PLs and overall supply chain strategy", "A 4PL only owns physical trucks", "A 4PL operates exclusively in air freight", "There is no functional distinction"), 0, 10, "4PL acts as a non-asset-based lead logistics manager coordinating whole supply networks."),
                ExamQuestion("q7-2", "Which flow represents products moving from end-users back to retailers or manufacturers for recycling/refund?", listOf("Outbound distribution", "Reverse logistics", "Forward consolidation", "Intermodal dispatch"), 1, 10, "Reverse logistics handles product returns, repairs, recycling, and remanufacturing.")
            )
        ),

        // Module 8 Exam (Configurable, default 20 marks)
        ModuleExam(
            id = "exam-mod-8",
            moduleId = "mod-8",
            title = "Module 8 Assessment: Strategic Sourcing & Kraljic Matrix",
            totalMarks = 20,
            isSyllabusFixedMarks = false,
            passingPercentage = 60,
            durationMinutes = 20,
            questions = listOf(
                ExamQuestion("q8-1", "In the Kraljic Portfolio Matrix, items with High Profit Impact and High Supply Risk are classified as:", listOf("Strategic Items", "Bottleneck Items", "Leverage Items", "Non-Critical Items"), 0, 10, "Strategic items are critical to profitability and have scarce supply markets."),
                ExamQuestion("q8-2", "In an electronic reverse auction, how do suppliers submit pricing?", listOf("Suppliers submit sequentially decreasing bids in real-time competition", "Suppliers submit increasing bids", "Only one fixed price quote is accepted", "Prices are negotiated via telegram"), 0, 10, "Reverse auctions drive downward pricing through dynamic transparent supplier competition.")
            )
        ),

        // Module 9 Exam (Configurable, default 20 marks)
        ModuleExam(
            id = "exam-mod-9",
            moduleId = "mod-9",
            title = "Module 9 Assessment: Quantitative Inventory & Warehouse Optimization",
            totalMarks = 20,
            isSyllabusFixedMarks = false,
            passingPercentage = 60,
            durationMinutes = 20,
            questions = listOf(
                ExamQuestion("q9-1", "What two competing costs does the Economic Order Quantity (EOQ) formula balance?", listOf("Annual Ordering Cost and Annual Holding/Carrying Cost", "Customs duty and insurance premium", "Employee salary and truck fuel", "Advertising expense and office rent"), 0, 10, "EOQ minimizes total cost where annual order setup costs equal inventory carrying costs."),
                ExamQuestion("q9-2", "What is the primary role of safety stock in a continuous review inventory system?", listOf("To buffer against demand variability and lead time fluctuations", "To eliminate the need for purchase orders", "To increase obsolete inventory write-offs", "To eliminate warehouse space requirements"), 0, 10, "Safety stock prevents stockouts during unanticipated demand spikes or vendor delays.")
            )
        ),

        // Module 10 Exam (Configurable, default 20 marks)
        ModuleExam(
            id = "exam-mod-10",
            moduleId = "mod-10",
            title = "Module 10 Assessment: Trade Finance, Customs & Cash Cycle",
            totalMarks = 20,
            isSyllabusFixedMarks = false,
            passingPercentage = 60,
            durationMinutes = 20,
            questions = listOf(
                ExamQuestion("q10-1", "How is the Cash-to-Cash (C2C) operating cycle calculated?", listOf("Days Inventory Outstanding + Days Sales Outstanding - Days Payables Outstanding", "Days in transit plus customs inspection days", "Total profit minus bank interest", "Annual sales divided by total employees"), 0, 10, "C2C = DIO + DSO - DPO, measuring the elapsed time from paying cash to collecting cash."),
                ExamQuestion("q10-2", "What penalty is incurred when import cargo exceeds free dwell days on port container yards?", listOf("Demurrage", "Rebate", "Salvage", "Freight prepaid"), 0, 10, "Demurrage is charged by shipping lines/ports for containers detained inside the terminal past free time.")
            )
        ),

        // Module 11 Exam (Configurable, default 20 marks)
        ModuleExam(
            id = "exam-mod-11",
            moduleId = "mod-11",
            title = "Module 11 Assessment: S&OP, ERP Systems & Supply Chain Tech",
            totalMarks = 20,
            isSyllabusFixedMarks = false,
            passingPercentage = 60,
            durationMinutes = 20,
            questions = listOf(
                ExamQuestion("q11-1", "What is the primary objective of monthly Sales and Operations Planning (S&OP)?", listOf("To create executive consensus balancing demand forecasts with manufacturing and supply capabilities", "To generate annual financial tax filings only", "To replace enterprise ERP systems", "To outsource all production abroad"), 0, 10, "S&OP aligns commercial sales plans with operational supply chain capacity."),
                ExamQuestion("q11-2", "How do IoT sensor devices enhance supply chain cold chains?", listOf("By providing real-time ambient temperature and location telemetry during transit", "By replacing physical container locks", "By calculating import tariffs automatically", "By printing physical paper shipping labels"), 0, 10, "IoT telemetry alerts managers to temperature excursions before perishable goods spoil.")
            )
        ),

        // Module 12 Exam (Configurable, default 20 marks)
        ModuleExam(
            id = "exam-mod-12",
            moduleId = "mod-12",
            title = "Module 12 Assessment: Sustainable SCM & Capstone Readiness",
            totalMarks = 20,
            isSyllabusFixedMarks = false,
            passingPercentage = 60,
            durationMinutes = 20,
            questions = listOf(
                ExamQuestion("q12-1", "In greenhouse gas (GHG) carbon accounting, what do Scope 3 emissions encompass?", listOf("All indirect emissions throughout the upstream and downstream supply chain", "Direct emissions from company owned boilers", "Emissions from purchased electricity only", "Personal employee home utility usage"), 0, 10, "Scope 3 covers value chain emissions from suppliers, shipping, and product use."),
                ExamQuestion("q12-2", "What is a circular supply chain designed to achieve?", listOf("Recovering and repurposing materials to regenerate value and eliminate landfill waste", "Exporting waste to overseas landfills", "Using only single-use plastics", "Increasing freight distances"), 0, 10, "Circular supply chains keep resources in use for as long as possible via closed loops.")
            )
        ),

        // FINAL COMPREHENSIVE EXAMINATION (EXPLICIT SYLLABUS: 100 MARKS)
        ModuleExam(
            id = "exam-final",
            moduleId = "mod-final",
            title = "Final Comprehensive Examination: PGD in SCM (Syllabus: 100 Marks)",
            totalMarks = 100,
            isSyllabusFixedMarks = true,
            passingPercentage = 60,
            durationMinutes = 60,
            questions = listOf(
                ExamQuestion("qf-1", "Which mathematical formula defines the classical Economic Order Quantity (EOQ)?", listOf("Sqrt((2 * Annual Demand * Order Cost) / Holding Cost)", "(Demand * Lead Time) + Safety Stock", "Annual Demand / 365 * Unit Price", "Holding Cost / (2 * Order Cost)"), 0, 10, "EOQ = Sqrt((2DS)/H), determining the optimal replenishment lot size."),
                ExamQuestion("qf-2", "Under Incoterms 2020, at which precise point does risk transfer from seller to buyer in an FOB shipment?", listOf("When the goods are placed on board the vessel nominated by the buyer at the named port of shipment", "At the seller's factory gate", "Upon arrival at the destination discharge port", "When the buyer pays the bank Letter of Credit"), 0, 10, "FOB transfers risk once goods are physically on board the named vessel at the loading port."),
                ExamQuestion("qf-3", "In Letter of Credit operations, which international banking publication governs uniform documentary examination rules?", listOf("UCP 600 (Uniform Customs and Practice for Documentary Credits)", "Incoterms 2020", "WTO Trade Agreement", "ISO 9001"), 0, 10, "ICC's UCP 600 governs international Letters of Credit."),
                ExamQuestion("qf-4", "Which document is legally mandatory for customs clearance to certify the tariff classification and customs duty assessment in Bangladesh?", listOf("Bill of Entry (B/E)", "Certificate of Incorporation", "Bank Passbook", "Employee Identity Card"), 0, 10, "A Bill of Entry is the statutory customs declaration filed for imported cargo."),
                ExamQuestion("qf-5", "What is the primary objective of safety stock calculation using normal distribution statistics?", listOf("To maintain a defined customer service level probability despite lead-time and demand variance", "To maximize physical inventory depreciation", "To avoid all supplier contract negotiations", "To fill empty warehouse aisles"), 0, 10, "Safety stock buffers against standard deviations of demand during replenishment lead times."),
                ExamQuestion("qf-6", "What is the key advantage of cross-docking compared to traditional warehousing?", listOf("Direct transfer from receiving to shipping with under 24h staging, minimizing holding costs", "Providing 90 days free bonded storage", "Doubling warehouse building square footage", "Multiplying pallet touches"), 0, 10, "Cross-docking speeds throughput and slashes inventory carrying expenses."),
                ExamQuestion("qf-7", "What does a 3-way match in accounts payable verify prior to supplier disbursement?", listOf("The PO, the receiving GRN, and the vendor Invoice match in quantity and unit price", "The buyer's passport, bank balance, and signature", "Three supplier quotes submitted during bidding", "The bill of lading, air waybill, and packing list"), 0, 10, "The 3-way match ensures the business pays only for authorized, verified goods received."),
                ExamQuestion("qf-8", "In the Kraljic Matrix, what strategy is recommended for 'Leverage Items' (High Profit Impact, Low Supply Risk)?", listOf("Competitive bidding and volume pooling across vendors to minimize unit purchase costs", "Exclusive sole-sourcing contracts", "Accepting any vendor price increase", "Holding 12 months safety stock"), 0, 10, "Abundant supply and high spend justify aggressive competitive bidding and volume aggregation."),
                ExamQuestion("qf-9", "How does Enterprise Resource Planning (ERP/SAP) coordinate supply chain processes?", listOf("By providing a unified single source of real-time data across procurement, inventory, finance, and logistics", "By replacing all human warehouse employees", "By operating solely as an email client", "By eliminating all transportation requirements"), 0, 10, "ERP synchronizes transactions seamlessly across business functions."),
                ExamQuestion("qf-10", "What constitutes a circular closed-loop supply chain strategy?", listOf("Systematically recovering end-of-life products for remanufacturing, refurbishment, and raw material recycling", "Disposing of obsolete materials in landfills", "Exporting defective goods with zero recovery", "Purchasing exclusively virgin single-use inputs"), 0, 10, "Closed loops eliminate waste by recirculating valuable components back into production.")
            )
        )
    )

    // ================= INITIAL ASSIGNMENTS & FINAL PROJECT =================

    private fun getInitialModuleAssignments(): List<ModuleAssignment> = listOf(
        ModuleAssignment(
            id = "asg-1",
            moduleId = "mod-1",
            title = "Module 1 Assignment: Demand Planning & Forecast Error Calculation",
            topicDescription = "Analyze historical sales demand for an FMCG distributor in Bangladesh. Calculate 3-month moving average, calculate MAD (Mean Absolute Deviation), and generate the resulting Purchase Requisition.",
            instructions = "Submit a clear breakdown (300-500 words) showing forecast variance calculations and recommendations to avoid bullwhip distortion.",
            maxMarks = 20
        ),
        ModuleAssignment(
            id = "asg-2",
            moduleId = "mod-2",
            title = "Module 2 Assignment: Strategic Vendor Scorecard & Negotiation Plan",
            topicDescription = "Formulate a multi-criteria vendor evaluation scorecard (Quality 35%, Delivery 30%, Cost 25%, ESG 10%) and develop a BATNA negotiation strategy for key raw material procurement.",
            instructions = "Submit your weighted scorecard template and negotiation contingency plan.",
            maxMarks = 25
        ),
        ModuleAssignment(
            id = "asg-3",
            moduleId = "mod-3",
            title = "Module 3 Assignment: Import LC Discrepancy & Incoterms Risk Analysis",
            topicDescription = "Review a sample Proforma Invoice and Commercial Invoice discrepancy under UCP 600 rules. Recommend the appropriate Incoterm 2020 (FOB vs CFR vs CIF) for containerized cargo shipped through Chattogram port.",
            instructions = "Submit an analysis identifying three document discrepancies and draft the corrective amendment request.",
            maxMarks = 30
        ),
        ModuleAssignment(
            id = "asg-4",
            moduleId = "mod-4",
            title = "Module 4 Assignment: Warehouse Layout & Order Picking Optimization",
            topicDescription = "Design a layout plan for a 20,000 sq ft central distribution center. Compare wave picking vs batch picking for 500 daily retail orders to minimize travel distance.",
            instructions = "Submit a warehouse slotting recommendation and pick-path efficiency estimate.",
            maxMarks = 20
        ),
        ModuleAssignment(
            id = "asg-5",
            moduleId = "mod-5",
            title = "Module 5 Assignment: Statutory VAT Compliance & OTIF Dashboard",
            topicDescription = "Calculate VAT Deduction at Source (VDS) on raw material procurement under current National Board of Revenue (NBR) rules, and design an OTIF customer service scorecard.",
            instructions = "Provide the deduction journal entry and target OTIF remediation steps.",
            maxMarks = 20
        ),
        ModuleAssignment(
            id = "asg-6",
            moduleId = "mod-6",
            title = "Module 6 Assignment: SOP Formulation for Cross-Border Dispatch",
            topicDescription = "Author a complete Standard Operating Procedure (SOP) conforming to ISO 9001 guidelines for container inspection, seal verification, and customs dispatch documentation.",
            instructions = "Submit an organized SOP with Purpose, Scope, Responsibilities, Step-by-Step Flow, and Non-Conformance actions.",
            maxMarks = 20
        ),
        ModuleAssignment(
            id = "asg-7",
            moduleId = "mod-7",
            title = "Module 7 Assignment: 3PL vs 4PL Cost-Benefit Case Study",
            topicDescription = "Evaluate outsourcing corporate distribution to a 3PL dedicated fleet versus contracting a 4PL digital orchestrator for nationwide cold chain pharmaceuticals.",
            instructions = "Submit financial comparison, risk matrix, and governance recommendation.",
            maxMarks = 20
        ),
        ModuleAssignment(
            id = "asg-8",
            moduleId = "mod-8",
            title = "Module 8 Assignment: Kraljic Matrix Portfolio Sourcing Blueprint",
            topicDescription = "Map 10 procurement categories into the four Kraljic Matrix quadrants (Strategic, Bottleneck, Leverage, Non-Critical). Outline mitigation strategies for bottleneck single-source components.",
            instructions = "Submit the quadrant chart and supplier relationship management action plans.",
            maxMarks = 20
        ),
        ModuleAssignment(
            id = "asg-9",
            moduleId = "mod-9",
            title = "Module 9 Assignment: EOQ and Safety Stock Optimization",
            topicDescription = "Given annual demand of 24,000 units, ordering cost of $150, holding cost of $5/unit/year, and lead time standard deviation of 4 days, compute optimal EOQ and 95% service-level safety stock.",
            instructions = "Submit step-by-step mathematical calculations and reorder point determination.",
            maxMarks = 20
        ),
        ModuleAssignment(
            id = "asg-10",
            moduleId = "mod-10",
            title = "Module 10 Assignment: Cash-to-Cash Cycle & Demurrage Mitigation",
            topicDescription = "Analyze an importer with DIO of 55 days, DSO of 40 days, and DPO of 30 days. Recommend supply chain finance strategies to compress the 65-day C2C cycle by 20 days and avoid port demurrage.",
            instructions = "Submit the C2C compression plan and port clearance timeline workflow.",
            maxMarks = 20
        ),
        ModuleAssignment(
            id = "asg-11",
            moduleId = "mod-11",
            title = "Module 11 Assignment: Digital Transformation & ERP Roadmap",
            topicDescription = "Develop a phased implementation architecture for deploying SAP SCM with IoT real-time cold chain trackers for an international perishable export business.",
            instructions = "Submit the implementation gantt timeline, change management plan, and expected KPI improvements.",
            maxMarks = 20
        ),

        // MODULE 12 CAPSTONE: FINAL PROJECT
        ModuleAssignment(
            id = "asg-12-final-project",
            moduleId = "mod-12",
            title = "Final Project: End-to-End SCM Transformation Capstone Study",
            topicDescription = "A comprehensive capstone project integrating real-world data collection, root cause diagnosis, and strategic intervention across an entire industrial supply chain ecosystem in Bangladesh or regional trade.",
            instructions = "Select an industry sector (RMG & Textiles, Pharmaceuticals, FMCG, Agriculture/Cold Chain, or Port Logistics). Submit a formal project report covering: (1) Executive Summary & Problem Formulation, (2) Empirical Field Data & Workflow Maps, (3) Quantitative Analysis (EOQ, S&OP, or Bottlenecks), (4) Implementation Plan with Budget, ROI & Risk Controls, (5) Sustainability & Circular SCM Considerations. Minimum 1,500 words or attached executive presentation slide deck.",
            maxMarks = 50,
            isFinalProject = true
        )
    )

    private fun generateClassQuizzes(title: String): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                id = "q-${title.hashCode()}-1",
                question = "What is the core operational principle emphasized in $title?",
                options = listOf(
                    "Standardized compliance, cross-functional flow, and risk mitigation",
                    "Eliminating all external supplier partnerships",
                    "Maximizing physical inventory buffers unconditionally",
                    "Disregarding regulatory and statutory mandates"
                ),
                correctOptionIndex = 0,
                explanation = "Modern supply chain excellence requires standardized workflows, cross-functional coordination, and proactive risk control."
            ),
            QuizQuestion(
                id = "q-${title.hashCode()}-2",
                question = "How does this lesson contribute to overall ISCEB PGD core competency?",
                options = listOf(
                    "Building real-world execution capacity aligned with global standards",
                    "Providing isolated theoretical concepts without practical application",
                    "Increasing administrative delays",
                    "Focusing solely on local domestic trade without compliance"
                ),
                correctOptionIndex = 0,
                explanation = "The ISCEB curriculum connects academic frameworks directly to enterprise execution and measurable business impact."
            )
        )
    }

    // ==========================================
    // CLOUD SYNCHRONIZATION & OTA PUBLISHING API
    // ==========================================

    fun checkForCurriculumUpdates(): Boolean {
        _syncStatus.value = SyncStatus.CHECKING
        val cloudLiveJson = prefs.getString("cloud_manifest_live", null)
        if (!cloudLiveJson.isNullOrBlank()) {
            val remoteManifest = deserializeManifest(cloudLiveJson)
            refreshAppUpdateInfoFromManifest(remoteManifest, source = "Cached Cloud Manifest")
            if (remoteManifest.versionCode > _publishManifest.value.versionCode) {
                _pendingUpdateManifest.value = remoteManifest
                _syncStatus.value = SyncStatus.UPDATE_AVAILABLE
                return true
            }
        }
        _syncStatus.value = SyncStatus.SYNCED_SUCCESS
        _pendingUpdateManifest.value = null
        return false
    }

    suspend fun checkForRemoteUpdates(
        remoteUrlOverride: String? = null,
        isWorkInProgress: Boolean = false
    ): Boolean {
        _syncStatus.value = SyncStatus.CHECKING
        val cloudRepo = cloudSyncRepository
        var bestManifest: CurriculumPublishManifest? = null
        var bestCatalog: CloudCatalogSnapshot? = null
        var sourceLabel = "Local Cache"
        var remoteFetchError: String? = null

        // 1. Check Cloud Firestore (ONLY if user is authenticated with Google)
        if (cloudRepo != null && cloudRepo.currentAuthenticatedUid() != null) {
            val manifestResult = cloudRepo.getLiveManifestOnce()
            val remoteFirestoreManifest = manifestResult.getOrNull()
            if (remoteFirestoreManifest != null) {
                bestManifest = remoteFirestoreManifest
                sourceLabel = "Firebase Cloud Firestore"
                if (remoteFirestoreManifest.versionCode > _publishManifest.value.versionCode) {
                    bestCatalog = cloudRepo.getLiveCatalogOnce().getOrNull()
                }
            }
        }

        // 2. Check Cache-Busted HTTPS Manifest (version.json or GitHub Releases endpoint)
        val targetHttpsUrl = (remoteUrlOverride ?: _adminSettings.value.remoteSyncManifestUrl)
            .trim()
            .ifBlank { AppUpdateConfig.DEFAULT_VERSION_JSON_URL }
        if (cloudRepo != null && targetHttpsUrl.isNotBlank()) {
            when (val fetchResult = cloudRepo.fetchRemoteManifestDetailed(targetHttpsUrl)) {
                is RemoteManifestFetchResult.Success -> {
                    val httpsManifest = fetchResult.manifest
                    if (bestManifest == null ||
                        httpsManifest.versionCode >= bestManifest.versionCode ||
                        httpsManifest.latestApkVersionCode > bestManifest.latestApkVersionCode
                    ) {
                        bestManifest = httpsManifest
                        sourceLabel = "Live Update Server ($targetHttpsUrl)"
                    } else if (bestManifest.apkSha256.isBlank() && httpsManifest.apkSha256.isNotBlank()) {
                        bestManifest = bestManifest.copy(
                            apkSha256 = httpsManifest.apkSha256,
                            apkFileSizeBytes = httpsManifest.apkFileSizeBytes,
                            apkReleaseDate = httpsManifest.apkReleaseDate,
                            downloadPageUrl = httpsManifest.downloadPageUrl
                        )
                    }
                }
                is RemoteManifestFetchResult.Failure -> {
                    remoteFetchError = fetchResult.errorMessage
                }
            }
        }

        val nowMs = System.currentTimeMillis()
        if (bestManifest == null) {
            val localUpdate = checkForCurriculumUpdates()
            _lastSyncTimestamp.value = nowMs
            prefs.edit().putLong("last_sync_timestamp", nowMs).apply()
            _appUpdateInfo.value = _appUpdateInfo.value.copy(
                lastCheckedTimestamp = nowMs,
                lastCheckError = remoteFetchError
            )
            if (remoteFetchError != null && !localUpdate) {
                _syncStatus.value = SyncStatus.ERROR
            }
            return localUpdate
        }

        _lastSyncTimestamp.value = nowMs
        prefs.edit().putLong("last_sync_timestamp", nowMs).apply()
        val applied = applyRemoteManifestAndCatalog(
            manifest = bestManifest,
            catalog = bestCatalog,
            isWorkInProgress = isWorkInProgress,
            sourceLabel = sourceLabel
        )
        _appUpdateInfo.value = _appUpdateInfo.value.copy(
            lastCheckedTimestamp = nowMs,
            lastCheckError = null
        )
        return applied
    }

    suspend fun syncCurriculumFromCloudOrHttps(isWorkInProgress: Boolean = false): Boolean {
        _syncStatus.value = SyncStatus.SYNCING
        val cloudRepo = cloudSyncRepository
        if (cloudRepo != null && cloudRepo.currentAuthenticatedUid() != null) {
            val remoteManifest = cloudRepo.getLiveManifestOnce().getOrNull()
            val remoteCatalog = cloudRepo.getLiveCatalogOnce().getOrNull()
            if (remoteManifest != null) {
                applyRemoteManifestAndCatalog(
                    manifest = remoteManifest,
                    catalog = remoteCatalog,
                    isWorkInProgress = isWorkInProgress,
                    sourceLabel = "Firebase Cloud Firestore",
                    forceApplyNow = true
                )
                return true
            }
        }

        val remoteUrl = _adminSettings.value.remoteSyncManifestUrl.trim()
        if (cloudRepo != null && remoteUrl.isNotBlank()) {
            val httpsManifest = cloudRepo.fetchRemoteManifestCacheBusted(remoteUrl)
            if (httpsManifest != null) {
                applyRemoteManifestAndCatalog(
                    manifest = httpsManifest,
                    catalog = null,
                    isWorkInProgress = isWorkInProgress,
                    sourceLabel = "Live HTTPS Endpoint (No-Cache)",
                    forceApplyNow = true
                )
                return true
            }
        }

        return syncCurriculumNow()
    }

    fun applyRemoteManifestAndCatalog(
        manifest: CurriculumPublishManifest,
        catalog: CloudCatalogSnapshot? = null,
        isWorkInProgress: Boolean = false,
        sourceLabel: String = "Firebase Cloud Firestore",
        forceApplyNow: Boolean = false
    ): Boolean {
        refreshAppUpdateInfoFromManifest(manifest, source = sourceLabel)
        val serializedManifest = serializeManifest(manifest)
        prefs.edit().putString("cloud_manifest_live", serializedManifest).apply()

        if (catalog != null) {
            prefs.edit()
                .putString("cloud_modules_json", serializeModules(catalog.modules))
                .putString("cloud_classes_json", serializeClasses(catalog.classes))
                .putString("cloud_settings_json", serializeSettings(catalog.settings))
                .apply()
        }

        val hasNewerCurriculum = manifest.versionCode > _publishManifest.value.versionCode
        val hasNewerCatalog = catalog != null && catalog.versionCode >= _publishManifest.value.versionCode

        if (!hasNewerCurriculum && !forceApplyNow && !hasNewerCatalog) {
            _syncStatus.value = SyncStatus.SYNCED_SUCCESS
            _pendingUpdateManifest.value = null
            return false
        }

        // Preserve active student work (e.g., running exam or active file upload)
        if (isWorkInProgress && !forceApplyNow) {
            _pendingUpdateManifest.value = manifest
            _syncStatus.value = SyncStatus.UPDATE_AVAILABLE
            return true
        }

        if (_adminSettings.value.autoApplyContentUpdates || forceApplyNow) {
            applyManifestAndCatalogLocally(manifest, catalog)
            return true
        } else {
            _pendingUpdateManifest.value = manifest
            _syncStatus.value = SyncStatus.UPDATE_AVAILABLE
            return true
        }
    }

    private fun applyManifestAndCatalogLocally(
        targetManifest: CurriculumPublishManifest,
        catalog: CloudCatalogSnapshot?
    ) {
        _publishManifest.value = targetManifest
        _pendingUpdateManifest.value = null
        _lastSyncTimestamp.value = System.currentTimeMillis()

        val serialized = serializeManifest(targetManifest)
        prefs.edit()
            .putString("cloud_manifest_live", serialized)
            .putString("local_manifest_json", serialized)
            .putLong("last_sync_timestamp", _lastSyncTimestamp.value)
            .apply()

        if (catalog != null) {
            if (catalog.modules.isNotEmpty()) {
                _modules.value = catalog.modules
                persistModules(catalog.modules)
            }
            if (catalog.classes.isNotEmpty()) {
                mergeRemoteClassesPreservingProgress(catalog.classes)
            }
            _adminSettings.value = catalog.settings
            persistSettings(catalog.settings)

            if (catalog.founderProfileJson.isNotBlank()) {
                val remoteFounder = deserializeFounderProfile(catalog.founderProfileJson)
                _founderProfile.value = remoteFounder
                prefs.edit().putString("founder_profile_json", serializeFounderProfile(remoteFounder)).apply()
            }
            if (catalog.developerProfileJson.isNotBlank()) {
                val remoteDev = deserializeDeveloperProfile(catalog.developerProfileJson)
                _developerProfile.value = remoteDev
                prefs.edit().putString("developer_profile_json", serializeDeveloperProfile(remoteDev)).apply()
            }
            if (catalog.membersJson.isNotBlank()) {
                val remoteMembers = deserializeMembers(catalog.membersJson).sortedBy { it.displayOrder }
                if (remoteMembers.isNotEmpty()) {
                    _iscebMembers.value = remoteMembers
                    persistMembers(remoteMembers)
                }
            }
        } else {
            val cloudModulesJson = prefs.getString("cloud_modules_json", null)
            if (!cloudModulesJson.isNullOrBlank()) {
                val remoteModules = deserializeModules(cloudModulesJson)
                if (remoteModules.isNotEmpty()) {
                    _modules.value = remoteModules
                    persistModules(remoteModules)
                }
            }

            val cloudClassesJson = prefs.getString("cloud_classes_json", null)
            if (!cloudClassesJson.isNullOrBlank()) {
                val remoteClasses = deserializeClasses(cloudClassesJson)
                if (remoteClasses.isNotEmpty()) {
                    mergeRemoteClassesPreservingProgress(remoteClasses)
                }
            }

            val cloudSettingsJson = prefs.getString("cloud_settings_json", null)
            if (!cloudSettingsJson.isNullOrBlank()) {
                val remoteSettings = deserializeSettings(cloudSettingsJson)
                _adminSettings.value = remoteSettings
                persistSettings(remoteSettings)
            }
        }

        refreshAppUpdateInfoFromManifest(targetManifest, source = _appUpdateInfo.value.updateSource)
        _syncStatus.value = SyncStatus.SYNCED_SUCCESS
    }

    private fun mergeRemoteClassesPreservingProgress(remoteClasses: List<CourseClass>) {
        val studentProg = _studentProgressMap.value
        val existingById = _classes.value.associateBy { it.id }
        val savedBookmarks = _progress.value.bookmarkedClassIds

        val mergedClasses = remoteClasses.map { cls ->
            val existing = existingById[cls.id]
            val prog = studentProg[cls.id]
            cls.copy(
                isCompleted = prog?.status == ClassStatus.COMPLETED,
                isBookmarked = savedBookmarks.contains(cls.id),
                status = prog?.status ?: ClassStatus.NOT_STARTED,
                activeLearningSeconds = prog?.activeLearningSeconds ?: 0L,
                quizPassed = prog?.quizPassed ?: false,
                pdfMedia = cls.pdfMedia ?: existing?.pdfMedia,
                audioMedia = cls.audioMedia ?: existing?.audioMedia,
                videoMedia = cls.videoMedia ?: existing?.videoMedia,
                attachments = if (cls.attachments.isNotEmpty()) cls.attachments else existing?.attachments ?: emptyList(),
                lectureCode = cls.lectureCode.ifBlank { existing?.lectureCode ?: "" },
                quizQuestions = if (cls.quizQuestions.isNotEmpty()) cls.quizQuestions else generateClassQuizzes(cls.title)
            )
        }
        _classes.value = mergedClasses
        persistClasses(mergedClasses)
    }

    fun mergeCloudStudentProgress(cloudProgress: Map<String, StudentClassProgress>) {
        if (cloudProgress.isEmpty()) return
        val studentId = _currentUser.value.id
        val mergedMap = _studentProgressMap.value.toMutableMap()

        cloudProgress.forEach { (classId, remote) ->
            val local = mergedMap[classId]
            if (local == null) {
                mergedMap[classId] = remote.copy(studentId = studentId)
            } else {
                val isCompleted = local.status == ClassStatus.COMPLETED || remote.status == ClassStatus.COMPLETED
                val mergedStatus = when {
                    isCompleted -> ClassStatus.COMPLETED
                    local.status == ClassStatus.IN_PROGRESS || remote.status == ClassStatus.IN_PROGRESS -> ClassStatus.IN_PROGRESS
                    else -> ClassStatus.NOT_STARTED
                }
                mergedMap[classId] = local.copy(
                    status = mergedStatus,
                    activeLearningSeconds = maxOf(local.activeLearningSeconds, remote.activeLearningSeconds),
                    maxPlaybackPercent = maxOf(local.maxPlaybackPercent, remote.maxPlaybackPercent),
                    isPdfConfirmed = local.isPdfConfirmed || remote.isPdfConfirmed,
                    pdfReadingSeconds = maxOf(local.pdfReadingSeconds, remote.pdfReadingSeconds),
                    quizScore = listOfNotNull(local.quizScore, remote.quizScore).maxOrNull(),
                    quizPassed = local.quizPassed || remote.quizPassed,
                    completionTimestamp = local.completionTimestamp ?: remote.completionTimestamp
                )
            }
        }

        _studentProgressMap.value = mergedMap
        persistStudentProgress(studentId, mergedMap.values.toList())

        val completedIds = mergedMap.filterValues { it.status == ClassStatus.COMPLETED }.keys
        val totalActiveSec = mergedMap.values.sumOf { it.activeLearningSeconds }
        _classes.value = _classes.value.map { cls ->
            val prog = mergedMap[cls.id]
            cls.copy(
                isCompleted = prog?.status == ClassStatus.COMPLETED,
                status = prog?.status ?: cls.status,
                activeLearningSeconds = prog?.activeLearningSeconds ?: cls.activeLearningSeconds,
                quizPassed = prog?.quizPassed ?: cls.quizPassed
            )
        }
        _progress.value = _progress.value.copy(
            completedClassIds = completedIds,
            totalActiveLearningSeconds = totalActiveSec
        )
    }

    fun refreshAppUpdateInfoFromManifest(
        manifest: CurriculumPublishManifest,
        source: String = "ISCEB Cloud Sync"
    ) {
        val currentCode = BuildConfig.VERSION_CODE
        val currentName = BuildConfig.VERSION_NAME
        val resolvedUrl = manifest.apkDownloadUrl
            .takeIf { it.isNotBlank() && it != "https://www.isceb.org/app/releases/latest" }
            ?: _adminSettings.value.apkDownloadUrl
                .takeIf { it.isNotBlank() && it != "https://www.isceb.org/app/releases/latest" }
            ?: AppUpdateConfig.DEFAULT_APK_DIRECT_URL
        val latestCode = maxOf(manifest.latestApkVersionCode, 1)
        val isRequired = latestCode > currentCode

        _appUpdateInfo.value = AppUpdateInfo(
            currentAppVersionCode = currentCode,
            currentAppVersionName = currentName,
            latestAvailableApkVersionCode = maxOf(latestCode, currentCode),
            latestAvailableApkVersionName = manifest.latestApkVersionName.ifBlank { currentName },
            apkDownloadUrl = resolvedUrl,
            downloadPageUrl = manifest.downloadPageUrl.ifBlank { AppUpdateConfig.DEFAULT_DOWNLOAD_PAGE_URL },
            releaseNotes = manifest.apkReleaseNotes.ifBlank { manifest.releaseNotes },
            sha256 = manifest.apkSha256,
            fileSizeBytes = manifest.apkFileSizeBytes,
            releaseDate = manifest.apkReleaseDate,
            minSupportedVersionCode = manifest.minSupportedAppVersion.coerceAtLeast(1),
            isBinaryUpdateRequired = isRequired,
            lastCheckedTimestamp = System.currentTimeMillis(),
            updateSource = source,
            lastCheckError = null
        )
    }

    fun syncCurriculumNow(): Boolean {
        _syncStatus.value = SyncStatus.SYNCING
        val cloudLiveJson = prefs.getString("cloud_manifest_live", null)
        val targetManifest = if (!cloudLiveJson.isNullOrBlank()) {
            deserializeManifest(cloudLiveJson)
        } else {
            _pendingUpdateManifest.value ?: _publishManifest.value
        }

        applyManifestAndCatalogLocally(targetManifest, catalog = null)
        return true
    }

    fun adminPublishCurriculumUpdate(
        newVersionTag: String,
        changeNotes: String,
        announcement: String,
        latestApkVersionCode: Int = _appUpdateInfo.value.latestAvailableApkVersionCode.coerceAtLeast(BuildConfig.VERSION_CODE),
        latestApkVersionName: String = _appUpdateInfo.value.latestAvailableApkVersionName.ifBlank { BuildConfig.VERSION_NAME },
        apkDownloadUrl: String = _adminSettings.value.apkDownloadUrl,
        apkReleaseNotes: String = ""
    ): CurriculumPublishManifest {
        val nextVersionCode = maxOf(_publishManifest.value.versionCode + 1, 103)
        val resolvedApkUrl = apkDownloadUrl.trim().ifBlank { AppUpdateConfig.DEFAULT_APK_DIRECT_URL }
        val manifest = CurriculumPublishManifest(
            versionCode = nextVersionCode,
            versionTag = if (newVersionTag.isNotBlank()) newVersionTag else "v1.${nextVersionCode - 100}.0",
            publishedTimestamp = System.currentTimeMillis(),
            publishedBy = "Md. Nafiul Islam Nahin (Developer of ISCEB App)",
            releaseNotes = changeNotes.ifBlank { "Curriculum enhancements, member updates, and syllabus refinements published by Developer of ISCEB App." },
            announcement = announcement.ifBlank { "Classes scheduled Friday & Saturday 9:00 PM - 11:00 PM (Asia/Dhaka)." },
            minSupportedAppVersion = 1,
            latestApkVersionCode = latestApkVersionCode.coerceAtLeast(1),
            latestApkVersionName = latestApkVersionName.ifBlank { BuildConfig.VERSION_NAME },
            apkDownloadUrl = resolvedApkUrl,
            apkReleaseNotes = apkReleaseNotes.ifBlank { changeNotes }
        )

        // Persist updated apkDownloadUrl in settings if modified
        if (_adminSettings.value.apkDownloadUrl != resolvedApkUrl) {
            updateAdminSettings(_adminSettings.value.copy(apkDownloadUrl = resolvedApkUrl))
        }

        // Save into cloud manifest cache and local storage
        val serializedManifest = serializeManifest(manifest)
        prefs.edit()
            .putString("cloud_manifest_live", serializedManifest)
            .putString("local_manifest_json", serializedManifest)
            .putString("cloud_modules_json", serializeModules(_modules.value))
            .putString("cloud_classes_json", serializeClasses(_classes.value))
            .putString("cloud_settings_json", serializeSettings(_adminSettings.value))
            .putLong("last_sync_timestamp", manifest.publishedTimestamp)
            .apply()

        _publishManifest.value = manifest
        _pendingUpdateManifest.value = null
        _lastSyncTimestamp.value = manifest.publishedTimestamp
        refreshAppUpdateInfoFromManifest(manifest, source = "Published Curriculum")
        _syncStatus.value = SyncStatus.SYNCED_SUCCESS

        return manifest
    }

    private fun serializeManifest(m: CurriculumPublishManifest): String {
        val obj = JSONObject()
        obj.put("versionCode", m.versionCode)
        obj.put("versionTag", m.versionTag)
        obj.put("publishedTimestamp", m.publishedTimestamp)
        obj.put("publishedBy", m.publishedBy)
        obj.put("releaseNotes", m.releaseNotes)
        obj.put("announcement", m.announcement ?: "")
        obj.put("minSupportedAppVersion", m.minSupportedAppVersion)
        obj.put("latestApkVersionCode", m.latestApkVersionCode)
        obj.put("latestApkVersionName", m.latestApkVersionName)
        obj.put("apkDownloadUrl", m.apkDownloadUrl)
        obj.put("apkReleaseNotes", m.apkReleaseNotes)
        obj.put("apkSha256", m.apkSha256)
        obj.put("apkFileSizeBytes", m.apkFileSizeBytes)
        obj.put("apkReleaseDate", m.apkReleaseDate)
        obj.put("downloadPageUrl", m.downloadPageUrl)
        return obj.toString()
    }

    private fun deserializeManifest(json: String): CurriculumPublishManifest {
        return try {
            val obj = JSONObject(json)
            CurriculumPublishManifest(
                versionCode = obj.optInt("versionCode", 102),
                versionTag = obj.optString("versionTag", "v1.3.0"),
                publishedTimestamp = obj.optLong("publishedTimestamp", System.currentTimeMillis()),
                publishedBy = obj.optString("publishedBy", "Md. Nafiul Islam Nahin (Developer of ISCEB App)")
                    .replace("Founder & Developer of ISCEB", "Developer of ISCEB App")
                    .replace("Academic Detector", "Developer of ISCEB App")
                    .replace("MD. Nafiul", "Md. Nafiul"),
                releaseNotes = obj.optString("releaseNotes", "Official ISCEB PGD Curriculum"),
                announcement = obj.optString("announcement", "Classes Friday & Saturday 9:00 PM - 11:00 PM (Asia/Dhaka)"),
                minSupportedAppVersion = obj.optInt("minSupportedAppVersion", 1),
                latestApkVersionCode = obj.optInt("latestApkVersionCode", BuildConfig.VERSION_CODE),
                latestApkVersionName = obj.optString("latestApkVersionName", BuildConfig.VERSION_NAME),
                apkDownloadUrl = obj.optString("apkDownloadUrl", AppUpdateConfig.DEFAULT_APK_DIRECT_URL)
                    .takeIf { it.isNotBlank() && it != "https://www.isceb.org/app/releases/latest" }
                    ?: AppUpdateConfig.DEFAULT_APK_DIRECT_URL,
                apkReleaseNotes = obj.optString("apkReleaseNotes", ""),
                apkSha256 = obj.optString("apkSha256", ""),
                apkFileSizeBytes = obj.optLong("apkFileSizeBytes", 0L),
                apkReleaseDate = obj.optString("apkReleaseDate", ""),
                downloadPageUrl = obj.optString("downloadPageUrl", AppUpdateConfig.DEFAULT_DOWNLOAD_PAGE_URL)
                    .ifBlank { AppUpdateConfig.DEFAULT_DOWNLOAD_PAGE_URL }
            )
        } catch (e: Exception) {
            CurriculumPublishManifest()
        }
    }

    private fun serializeModules(list: List<CourseModule>): String {
        val arr = JSONArray()
        list.forEach { mod ->
            val obj = JSONObject().apply {
                put("id", mod.id)
                put("number", mod.number)
                put("title", mod.title)
                put("description", mod.description)
                put("part", mod.part.name)
                put("category", mod.category)
                put("estimatedHours", mod.estimatedHours)
                put("iconName", mod.iconName)
                put("isRequired", mod.isRequired)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun serializeClasses(list: List<CourseClass>): String {
        val arr = JSONArray()
        list.forEach { cls ->
            val obj = JSONObject().apply {
                put("id", cls.id)
                put("moduleId", cls.moduleId)
                put("orderIndex", cls.orderIndex)
                put("lectureCode", cls.lectureCode)
                put("title", cls.title)
                put("summary", cls.summary)
                put("durationMinutes", cls.durationMinutes)
                put("isPublished", cls.isPublished)
                put("learningObjectives", JSONArray(cls.learningObjectives))

                cls.pdfMedia?.let { put("pdfMedia", serializeMedia(it)) }
                cls.audioMedia?.let { put("audioMedia", serializeMedia(it)) }
                cls.videoMedia?.let { put("videoMedia", serializeMedia(it)) }
                val attArr = JSONArray()
                cls.attachments.forEach { attArr.put(serializeMedia(it)) }
                put("attachments", attArr)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun serializeSettings(settings: AdminCompletionSettings): String {
        val obj = JSONObject().apply {
            put("minLearningTimeSeconds", settings.minLearningTimeSeconds)
            put("minPlaybackPercent", settings.minPlaybackPercent.toDouble())
            put("minPdfReadingSeconds", settings.minPdfReadingSeconds)
            put("isQuizRequired", settings.isQuizRequired)
            put("quizPassingScore", settings.quizPassingScore)
            put("requiredModuleIds", JSONArray(settings.requiredModuleIds))
            put("institutionName", settings.institutionName)
            put("officialWebsite", settings.officialWebsite)
            put("institutionAccreditation", settings.institutionAccreditation)
            put("authorizedSignerName", settings.authorizedSignerName)
            put("authorizedSignerTitle", settings.authorizedSignerTitle)
            put("certificateTitle", settings.certificateTitle)
            put("classTimeWindow", settings.scheduleConfig.classTimeWindow)
            put("timeZoneId", settings.scheduleConfig.timeZoneId)
            put("remoteSyncManifestUrl", settings.remoteSyncManifestUrl)
            put("apkDownloadUrl", settings.apkDownloadUrl)
            put("autoApplyContentUpdates", settings.autoApplyContentUpdates)
        }
        return obj.toString()
    }

    /**
     * Exports all persistent user progress, exams, assignments, certificates, profiles,
     * and curriculum customizations to a portable JSON string for safe pre-update backup.
     */
    fun exportWorkspaceBackupJson(): String {
        val root = JSONObject()
        root.put("backupFormatVersion", 1)
        root.put("appId", BuildConfig.APPLICATION_ID)
        root.put("versionName", BuildConfig.VERSION_NAME)
        root.put("versionCode", BuildConfig.VERSION_CODE)
        root.put("exportedAtMillis", System.currentTimeMillis())

        val prefsObj = JSONObject()
        prefs.all.forEach { (key, value) ->
            when (value) {
                is String -> prefsObj.put(key, JSONObject().put("t", "s").put("v", value))
                is Int -> prefsObj.put(key, JSONObject().put("t", "i").put("v", value))
                is Long -> prefsObj.put(key, JSONObject().put("t", "l").put("v", value))
                is Float -> prefsObj.put(key, JSONObject().put("t", "f").put("v", value.toDouble()))
                is Boolean -> prefsObj.put(key, JSONObject().put("t", "b").put("v", value))
                is Set<*> -> {
                    val arr = JSONArray()
                    value.filterIsInstance<String>().forEach { arr.put(it) }
                    prefsObj.put(key, JSONObject().put("t", "set").put("v", arr))
                }
            }
        }
        root.put("prefs", prefsObj)
        return root.toString(2)
    }

    /**
     * Restores a workspace backup JSON created by [exportWorkspaceBackupJson] and reloads live StateFlows.
     */
    fun importWorkspaceBackupJson(rawJson: String): Result<Int> {
        return try {
            val root = JSONObject(rawJson)
            val prefsObj = root.optJSONObject("prefs")
                ?: return Result.failure(IllegalArgumentException("Invalid ISCEB backup file (missing 'prefs' object)."))
            val editor = prefs.edit()
            var importedKeys = 0
            val keys = prefsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val entry = prefsObj.optJSONObject(key) ?: continue
                when (entry.optString("t")) {
                    "s" -> { editor.putString(key, entry.optString("v")); importedKeys++ }
                    "i" -> { editor.putInt(key, entry.optInt("v")); importedKeys++ }
                    "l" -> { editor.putLong(key, entry.optLong("v")); importedKeys++ }
                    "f" -> { editor.putFloat(key, entry.optDouble("v").toFloat()); importedKeys++ }
                    "b" -> { editor.putBoolean(key, entry.optBoolean("v")); importedKeys++ }
                    "set" -> {
                        val arr = entry.optJSONArray("v") ?: JSONArray()
                        val strSet = mutableSetOf<String>()
                        for (i in 0 until arr.length()) strSet.add(arr.optString(i))
                        editor.putStringSet(key, strSet)
                        importedKeys++
                    }
                }
            }
            editor.commit()
            loadData()
            _isReady.value = true
            Result.success(importedKeys)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
