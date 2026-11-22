package com.example.model

import com.example.BuildConfig
import java.util.UUID

enum class CoursePart(val label: String, val levelSubtitle: String) {
    PART_1_BASIC("Part 1: Basic", "Foundation Level – 28 Classes"),
    PART_2_ADVANCE("Part 2: Advance", "Strategic & Analytical Level – 20 Classes")
}

enum class MediaType(val displayLabel: String) {
    PDF("PDF"),
    IMAGE("Image"),
    DOCUMENT("Document"),
    AUDIO("Audio"),
    VIDEO("Video")
}

enum class ClassStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    LOCKED
}

enum class CertificateStatus {
    VALID,
    REVOKED,
    NOT_FOUND
}

enum class UserRole {
    STUDENT,
    TEACHER,
    ADMIN
}

enum class AccountStatus(val displayLabel: String) {
    ACTIVE("Active"),
    PENDING_VERIFICATION("Pending Verification"),
    SUSPENDED("Suspended")
}

enum class SubmissionStatus(val displayLabel: String) {
    UPCOMING("Upcoming"),
    PENDING("Pending"),
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under Review"),
    GRADED("Graded")
}

enum class CertificationEligibilityStatus(val displayLabel: String) {
    NOT_YET_ELIGIBLE("Not Yet Eligible"),
    LEARNING_REQUIREMENTS_INCOMPLETE("Learning Requirements Incomplete"),
    ASSESSMENTS_PENDING("Assessments Pending"),
    ELIGIBLE_FOR_CERTIFICATION("Eligible for Certification"),
    CERTIFICATE_ISSUED("Certificate Issued")
}

data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val role: UserRole = UserRole.STUDENT,
    val organization: String = "ISCEB Enrolled Professional",
    val designation: String = "",
    val additionalRole: String = "",
    val mobileNumber: String = "",
    val studentId: String = "",
    val photoUrl: String = "",
    val enrolledProgram: String = "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)",
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val isVerified: Boolean = false,
    val assignedModuleIds: Set<String> = emptySet(),
    val authUid: String = "",
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
)

data class DeveloperProfile(
    val name: String = "Md. Nafiul Islam Nahin",
    val designation: String = "Developer of ISCEB App",
    val biography: String = "App Developer for ISCEB (Institute of Supply Chain Excellence Bangladesh), architecting the native Android application, digital learning workflows, offline-first synchronization, and certification platform.",
    val educationBackground: String = "Post Graduate Diploma (PGD) in Supply Chain Management • Specialization in Enterprise Logistics, Operations & Mobile Application Engineering",
    val workExperience: String = "Lead Android & EdTech Systems Developer — Designed, engineered, and maintaining the official ISCEB mobile application with interactive curriculum management, assessment tracking, and cloud OTA synchronization.",
    val skillsAndExpertise: List<String> = listOf(
        "Android & Kotlin Architecture",
        "Jetpack Compose UI/UX",
        "Cloud Sync & Firebase",
        "Supply Chain Management Systems",
        "Assessment & Certification Engines",
        "EdTech Platform Design"
    ),
    val projectInformation: String = "ISCEB App is an official Supply Chain Management education platform featuring 12 structured modules, 48 industry-focused classes, multimedia study materials, member profiles, and verifiable digital certification.",
    val contactEmail: String = "www.mdnafiulislam@gmail.com",
    val contactPhone: String = "",
    val contactWebsite: String = "https://www.isceb.org",
    val location: String = "Dhaka, Bangladesh",
    val photoUri: String = ""
)

data class FounderProfile(
    val name: String = "Md. Amirul Islam",
    val designation: String = "Founder of ISCEB",
    val professionalIdentity: String = "Head of Supply Chain Management at BRB Group",
    val additionalRole: String = "Lead Trainer of ISCEB",
    val currentOrganization: String = "BRB Group",
    val mobileNumber: String = "01715-282905",
    val email: String = "amirul.islam@isceb.org",
    val educationBackground: String = "MBA in Finance; M.Com in Marketing; Post Graduate Diploma in Supply Chain Management (ISCEA, USA); global certifications under TUV SUD South Asia Pvt. Ltd., Business Express, and SELTA Training Associates (risk management, internal auditing, process validation, and international logistics).",
    val professionalExperience: String = "Over two decades of leadership in end-to-end supply chain operations across Bangladesh's top conglomerates. Currently Head of Supply Chain Management at BRB Group (procurement, logistics, warehousing, planning, and supplier relationship management). Former key leadership roles at NIPRO JMI Company Ltd, Paradise Cables Ltd, Bengal Group of Industries, and Masud Steel Design BD Ltd; Lead Trainer at IECB and Founder & Lead Trainer at ISCEB.",
    val biography: String = "Md. Amirul Islam is a seasoned Supply Chain Management professional with over two decades of proven expertise in leading and optimizing end-to-end supply chain operations across some of Bangladesh’s most respected conglomerates. He currently serves as the Head of Supply Chain Management at BRB Group, overseeing procurement, logistics, warehousing, planning, and supplier relationship management.\n\nThroughout his distinguished career, Mr. Islam has held key leadership positions in reputed organizations such as NIPRO JMI Company Ltd, Paradise Cables Ltd, Bengal Group of Industries, and Masud Steel Design BD Ltd. Beyond his corporate accomplishments, he serves as Lead Trainer at IECB and Founder & Lead Trainer of ISCEB with a vision to \"Connect. Learn. Achieve.\"",
    val skillsAndExpertise: List<String> = listOf(
        "Strategic Sourcing & Vendor Management",
        "Bonded Warehouse Operations",
        "International Trade Compliance",
        "Demand Forecasting & Risk Management",
        "Internal Auditing & Process Validation",
        "Executive Training & Mentorship"
    ),
    val location: String = "Bangladesh",
    val photoUri: String = ""
)

data class IscebMember(
    val id: String = UUID.randomUUID().toString(),
    val photoUri: String = "",
    val builtInPhotoKey: String = "",
    val fullName: String = "",
    val designation: String = "",
    val professionalIdentity: String = "",
    val currentOrganization: String = "",
    val educationalQualification: String = "",
    val professionalExperience: String = "",
    val skillsAndExpertise: List<String> = emptyList(),
    val roleInIsceb: String = "",
    val contactInformation: String = "",
    val biography: String = "",
    val displayOrder: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

data class LessonMedia(
    val type: MediaType,
    val fileName: String,
    val fileSizeFormatted: String,
    val fileSizeBytes: Long,
    val url: String = "",
    val durationFormatted: String? = null,
    val uploadTimestamp: Long = System.currentTimeMillis(),
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val mimeType: String = "",
    val cloudFileId: String = "",
    val cloudChunkCount: Int = 0
) {
    val displayTitle: String
        get() = title.ifBlank { fileName }
}

data class PendingUploadItem(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val fileName: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val mimeType: String,
    val mediaType: MediaType,
    val title: String,
    val description: String = "",
    val durationFormatted: String? = null
)

data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

data class ExamQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val marks: Int = 1,
    val explanation: String
)

data class ModuleExam(
    val id: String,
    val moduleId: String,
    val title: String,
    val totalMarks: Int,
    val isSyllabusFixedMarks: Boolean = false,
    val passingPercentage: Int = 60,
    val durationMinutes: Int = 30,
    val questions: List<ExamQuestion> = emptyList(),
    val scheduledWindowNote: String = "Class hours: 9:00 PM to 11:00 PM (Asia/Dhaka)",
    val maxAttempts: Int = 3,
    val openFromMs: Long? = null,
    val openUntilMs: Long? = null,
    val isRequiredForCertification: Boolean = false,
    val createdByUid: String = "system"
)

data class ExamAttempt(
    val attemptId: String,
    val examId: String,
    val studentId: String,
    val studentAnswers: Map<String, Int>,
    val scoreMarks: Int,
    val totalMarks: Int,
    val percentage: Int,
    val isPassed: Boolean,
    val startTimeMs: Long,
    val submittedTimeMs: Long,
    val isAutoSubmittedOnTimeout: Boolean = false,
    val moduleId: String = ""
)

data class ModuleAssignment(
    val id: String,
    val moduleId: String,
    val title: String,
    val topicDescription: String,
    val instructions: String,
    val maxMarks: Int = 20,
    val isFinalProject: Boolean = false,
    val isRequiredForCertification: Boolean = isFinalProject,
    val allowFileAttachment: Boolean = true,
    val dueDateMs: Long? = null,
    val createdByUid: String = "system",
    val createdByName: String = "ISCEB Faculty"
)

data class AssignmentSubmission(
    val submissionId: String,
    val assignmentId: String,
    val studentId: String,
    val studentName: String,
    val submissionText: String,
    val attachedFileName: String? = null,
    val submissionTimestamp: Long = System.currentTimeMillis(),
    val marksAwarded: Int? = null,
    val feedback: String? = null,
    val isGraded: Boolean = false,
    val moduleId: String = "",
    val status: SubmissionStatus = if (isGraded) SubmissionStatus.GRADED else SubmissionStatus.SUBMITTED,
    val gradedByUid: String? = null,
    val gradedByName: String? = null,
    val gradedTimestamp: Long? = null
)

data class StudentClassProgress(
    val classId: String,
    val studentId: String,
    val status: ClassStatus = ClassStatus.NOT_STARTED,
    val startTimeMs: Long = 0L,
    val lastActiveTimeMs: Long = 0L,
    val activeLearningSeconds: Long = 0L,
    val maxPlaybackPercent: Float = 0f,
    val isPdfConfirmed: Boolean = false,
    val pdfReadingSeconds: Long = 0L,
    val quizScore: Int? = null,
    val quizPassed: Boolean = false,
    val completionTimestamp: Long? = null,
    val lastPlaybackPositionMs: Long = 0L,
    val lastPdfPage: Int = 0,
    val totalPdfPages: Int = 0,
    val visitedPdfPages: Set<Int> = emptySet(),
    val idleExcludedSeconds: Long = 0L
) {
    val isCompleted: Boolean
        get() = status == ClassStatus.COMPLETED
}

data class CourseScheduleConfig(
    val courseDurationWeeks: Int = 24,
    val courseDurationMonths: Int = 6,
    val classesPerWeek: Int = 2,
    val classTimeWindow: String = "9:00 PM to 11:00 PM",
    val timeZoneId: String = "Asia/Dhaka",
    val classDays: String = "Friday & Saturday"
)

data class AdminCompletionSettings(
    val minLearningTimeSeconds: Long = 60L,
    val minPlaybackPercent: Float = 80f,
    val minPdfReadingSeconds: Long = 45L,
    val isQuizRequired: Boolean = true,
    val quizPassingScore: Int = 70,
    val requiredModuleIds: Set<String> = (1..12).map { "mod-$it" }.toSet(),
    val institutionName: String = "ISCEB",
    val officialWebsite: String = "www.isceb.org",
    val institutionAccreditation: String = "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)",
    val leftSignerName: String = "Hasina Khan",
    val leftSignerTitle: String = "",
    val authorizedSignerName: String = "Amirul Islam",
    val authorizedSignerTitle: String = "Founder of ISCEB",
    val certificateTitle: String = "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)",
    val certificatesEnabled: Boolean = true,
    val demoCertificateEnabled: Boolean = true,
    val scheduleConfig: CourseScheduleConfig = CourseScheduleConfig(),
    val remoteSyncManifestUrl: String = AppUpdateConfig.DEFAULT_VERSION_JSON_URL,
    val apkDownloadUrl: String = AppUpdateConfig.DEFAULT_APK_DIRECT_URL,
    val autoApplyContentUpdates: Boolean = true,
    val requiredExamIds: Set<String> = setOf("exam-mod-1", "exam-mod-2", "exam-mod-3", "exam-mod-6", "exam-final"),
    val requiredAssignmentIds: Set<String> = setOf("asg-12-final-project"),
    val requireGradedFinalProject: Boolean = false,
    val requirePdfAllPagesVisited: Boolean = false
)

data class CertificateRecord(
    val certificateId: String,
    val studentId: String,
    val studentName: String,
    val courseTitle: String,
    val issueDate: String,
    val issueTimestamp: Long,
    val institutionName: String,
    val officialWebsite: String = "www.isceb.org",
    val authorizedSigner: String,
    val status: CertificateStatus = CertificateStatus.VALID,
    val revocationReason: String? = null,
    val revocationTimestamp: Long? = null,
    val completedClassesCount: Int,
    val totalRequiredClasses: Int,
    val filePath: String? = null,
    val studentCode: String = "",
    val verificationUrl: String = "https://www.isceb.org/verify/$certificateId"
)

data class CertificateEligibility(
    val isEligible: Boolean,
    val completedRequiredCount: Int,
    val totalRequiredCount: Int,
    val missingClasses: List<String> = emptyList(),
    val allQuizzesPassed: Boolean = true,
    val mandatoryExamsPassed: Boolean = true,
    val finalProjectSubmitted: Boolean = true,
    val existingCertificate: CertificateRecord? = null,
    val status: CertificationEligibilityStatus = when {
        existingCertificate != null -> CertificationEligibilityStatus.CERTIFICATE_ISSUED
        isEligible -> CertificationEligibilityStatus.ELIGIBLE_FOR_CERTIFICATION
        completedRequiredCount == 0 && allQuizzesPassed -> CertificationEligibilityStatus.NOT_YET_ELIGIBLE
        completedRequiredCount < totalRequiredCount -> CertificationEligibilityStatus.LEARNING_REQUIREMENTS_INCOMPLETE
        else -> CertificationEligibilityStatus.ASSESSMENTS_PENDING
    },
    val remainingRequirements: List<String> = emptyList()
)

data class AuditLogEntry(
    val logId: String = "log-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(4)}",
    val actorUid: String,
    val actorName: String,
    val actorRole: UserRole,
    val actionType: String,
    val targetId: String,
    val summary: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class CourseClass(
    val id: String,
    val moduleId: String,
    val orderIndex: Int,
    val title: String,
    val summary: String,
    val durationMinutes: Int = 30,
    val isPublished: Boolean = true,
    val learningObjectives: List<String> = emptyList(),
    val pdfMedia: LessonMedia? = null,
    val audioMedia: LessonMedia? = null,
    val videoMedia: LessonMedia? = null,
    val isCompleted: Boolean = false,
    val isBookmarked: Boolean = false,
    val status: ClassStatus = ClassStatus.NOT_STARTED,
    val activeLearningSeconds: Long = 0L,
    val quizPassed: Boolean = false,
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val attachments: List<LessonMedia> = emptyList(),
    val lectureCode: String = "",
    val minStudySecondsOverride: Long? = null,
    val allowMediaDownload: Boolean = true
) {
    val allAttachments: List<LessonMedia>
        get() {
            val combined = mutableListOf<LessonMedia>()
            val seenKeys = mutableSetOf<String>()
            fun addUnique(item: LessonMedia?) {
                if (item == null) return
                val key = item.url.ifBlank { item.id }.ifBlank { "${item.type.name}_${item.fileName}" }
                if (seenKeys.add(key)) {
                    combined.add(item)
                }
            }
            attachments.forEach { addUnique(it) }
            addUnique(pdfMedia)
            addUnique(audioMedia)
            addUnique(videoMedia)
            return combined
        }

    val hasAnyMedia: Boolean
        get() = pdfMedia != null || audioMedia != null || videoMedia != null || attachments.isNotEmpty()
}

data class CourseModule(
    val id: String,
    val number: Int,
    val title: String,
    val description: String,
    val part: CoursePart = if (number <= 6) CoursePart.PART_1_BASIC else CoursePart.PART_2_ADVANCE,
    val category: String = "Core SCM",
    val estimatedHours: Int = 4,
    val iconName: String = "inventory",
    val isRequired: Boolean = true
)

data class UserProgress(
    val completedClassIds: Set<String> = emptySet(),
    val bookmarkedClassIds: Set<String> = emptySet(),
    val currentModuleId: String = "mod-1",
    val currentClassId: String = "class-1",
    val studyStreakDays: Int = 0,
    val totalActiveLearningSeconds: Long = 0L
)

data class AppNotification(
    val id: String,
    val message: String,
    val isError: Boolean = false
)

enum class SyncStatus {
    IDLE,
    CHECKING,
    UPDATE_AVAILABLE,
    SYNCING,
    SYNCED_SUCCESS,
    OFFLINE_CACHED,
    ERROR
}

object AppUpdateConfig {
    val HOSTING_BASE: String
        get() = BuildConfig.HOSTING_BASE_URL.trimEnd('/')
    val DEFAULT_DOWNLOAD_PAGE_URL: String
        get() = "$HOSTING_BASE/"
    val DEFAULT_VERSION_JSON_URL: String
        get() = "$HOSTING_BASE/version.json"
    val DEFAULT_APK_DIRECT_URL: String
        get() = "$HOSTING_BASE/downloads/ISCEB-latest.apk"
    val DEFAULT_DESKTOP_PORTAL_URL: String
        get() = "$HOSTING_BASE/desktop/"
}

data class CurriculumPublishManifest(
    val versionCode: Int = 102,
    val versionTag: String = "v1.3.0",
    val publishedTimestamp: Long = System.currentTimeMillis(),
    val publishedBy: String = "Md. Nafiul Islam Nahin (Developer of ISCEB App)",
    val releaseNotes: String = "ISCEB v1.3: Founder Profile, Developer of ISCEB App Profile, and ISCEB Members Directory with full admin management.",
    val announcement: String? = "Welcome to ISCEB — Connect. learn. Achieve. Classes are scheduled Friday & Saturday 9:00 PM - 11:00 PM (Asia/Dhaka).",
    val minSupportedAppVersion: Int = 1,
    val latestApkVersionCode: Int = 5,
    val latestApkVersionName: String = "1.4",
    val apkDownloadUrl: String = AppUpdateConfig.DEFAULT_APK_DIRECT_URL,
    val apkReleaseNotes: String = "",
    val apkSha256: String = "",
    val apkFileSizeBytes: Long = 0L,
    val apkReleaseDate: String = "",
    val downloadPageUrl: String = AppUpdateConfig.DEFAULT_DOWNLOAD_PAGE_URL
)

data class AppUpdateInfo(
    val currentAppVersionCode: Int = 5,
    val currentAppVersionName: String = "1.4",
    val latestAvailableApkVersionCode: Int = 5,
    val latestAvailableApkVersionName: String = "1.4",
    val apkDownloadUrl: String = AppUpdateConfig.DEFAULT_APK_DIRECT_URL,
    val downloadPageUrl: String = AppUpdateConfig.DEFAULT_DOWNLOAD_PAGE_URL,
    val releaseNotes: String = "ISCEB v1.4: Online APK auto-update checker, verified in-app APK downloader, package installer integration, and updated Admin Login ordering.",
    val sha256: String = "",
    val fileSizeBytes: Long = 0L,
    val releaseDate: String = "",
    val minSupportedVersionCode: Int = 1,
    val isBinaryUpdateRequired: Boolean = false,
    val lastCheckedTimestamp: Long = 0L,
    val updateSource: String = "ISCEB Cloud Sync",
    val lastCheckError: String? = null
)

sealed class ApkDownloadState {
    object Idle : ApkDownloadState()
    object Checking : ApkDownloadState()
    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progressFraction: Float
    ) : ApkDownloadState()
    data class Verifying(val fileName: String) : ApkDownloadState()
    data class ReadyToInstall(
        val apkFilePath: String,
        val versionName: String,
        val versionCode: Int,
        val verifiedSha256: String,
        val fileSizeBytes: Long
    ) : ApkDownloadState()
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : ApkDownloadState()
}
