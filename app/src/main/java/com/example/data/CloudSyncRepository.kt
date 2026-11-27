package com.example.data

import android.content.Context
import com.example.BuildConfig
import com.example.R
import com.example.model.AccountStatus
import com.example.model.AdminCompletionSettings
import com.example.model.AppUpdateConfig
import com.example.model.AssignmentSubmission
import com.example.model.AuditLogEntry
import com.example.model.CertificateRecord
import com.example.model.CertificateStatus
import com.example.model.ClassStatus
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.CoursePart
import com.example.model.CourseScheduleConfig
import com.example.model.CurriculumPublishManifest
import com.example.model.ExamAttempt
import com.example.model.ExamQuestion
import com.example.model.LessonMedia
import com.example.model.MediaType
import com.example.model.ModuleAssignment
import com.example.model.ModuleExam
import com.example.model.StudentClassProgress
import com.example.model.SubmissionStatus
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CloudCatalogSnapshot(
    val versionCode: Int,
    val modules: List<CourseModule>,
    val classes: List<CourseClass>,
    val settings: AdminCompletionSettings,
    val founderProfileJson: String = "",
    val developerProfileJson: String = "",
    val membersJson: String = "",
    val examsJson: String = "",
    val assignmentsJson: String = ""
)

sealed class RemoteManifestFetchResult {
    data class Success(val manifest: CurriculumPublishManifest) : RemoteManifestFetchResult()
    data class Failure(val errorMessage: String, val httpCode: Int? = null) : RemoteManifestFetchResult()
}

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val okHttpClient: OkHttpClient = defaultNoCacheClient()
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun currentAuthenticatedUid(): String? = auth.currentUser?.uid

    // First-party snapshots() observation for shared live manifest (caller MUST be authenticated)
    fun observeLiveManifest(): Flow<CurriculumPublishManifest?> {
        requireUserId()
        val docRef = db.collection(COLLECTION_MANIFESTS).document(LIVE_DOC_ID)
        return docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) snapshotToManifest(snapshot) else null
        }
    }

    // First-party snapshots() observation for shared live curriculum catalog (caller MUST be authenticated)
    fun observeLiveCatalog(): Flow<CloudCatalogSnapshot?> {
        requireUserId()
        val docRef = db.collection(COLLECTION_CATALOGS).document(LIVE_DOC_ID)
        return docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) snapshotToCatalog(snapshot) else null
        }
    }

    // Per-user progress real-time observation scoped to UID
    fun observeUserProgress(userId: String): Flow<Map<String, StudentClassProgress>> {
        val path = "users/$userId/progress"
        return db.collection("users").document(userId).collection("progress")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    docToStudentProgress(doc)
                }.associateBy { it.classId }
            }
    }

    suspend fun getLiveManifestOnce(): Result<CurriculumPublishManifest?> {
        requireUserId()
        val docRef = db.collection(COLLECTION_MANIFESTS).document(LIVE_DOC_ID)
        return try {
            val snap = docRef.get().await()
            Result.success(if (snap.exists()) snapshotToManifest(snap) else null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getLiveCatalogOnce(): Result<CloudCatalogSnapshot?> {
        requireUserId()
        val docRef = db.collection(COLLECTION_CATALOGS).document(LIVE_DOC_ID)
        return try {
            val snap = docRef.get().await()
            Result.success(if (snap.exists()) snapshotToCatalog(snap) else null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun publishLiveCurriculum(
        manifest: CurriculumPublishManifest,
        modules: List<CourseModule>,
        classes: List<CourseClass>,
        settings: AdminCompletionSettings,
        founderProfileJson: String = "",
        developerProfileJson: String = "",
        membersJson: String = "",
        examsJson: String = "",
        assignmentsJson: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val manifestRef = db.collection(COLLECTION_MANIFESTS).document(LIVE_DOC_ID)
        val catalogRef = db.collection(COLLECTION_CATALOGS).document(LIVE_DOC_ID)

        return try {
            val manifestSnap = manifestRef.get().await()
            val mutableManifestFields = buildMutableManifestMap(manifest)
            if (!manifestSnap.exists()) {
                val createPayload = mutableMapOf<String, Any>().apply {
                    putAll(mutableManifestFields)
                    put("authorUid", uid)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                manifestRef.set(createPayload).await()
            } else {
                val updatePayload = mutableMapOf<String, Any>().apply {
                    putAll(mutableManifestFields)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                manifestRef.update(updatePayload).await()
            }

            val catalogSnap = catalogRef.get().await()
            val mutableCatalogFields = buildMutableCatalogMap(
                versionCode = manifest.versionCode,
                modules = modules,
                classes = classes,
                settings = settings,
                founderProfileJson = founderProfileJson,
                developerProfileJson = developerProfileJson,
                membersJson = membersJson,
                examsJson = examsJson,
                assignmentsJson = assignmentsJson
            )
            if (!catalogSnap.exists()) {
                val createPayload = mutableMapOf<String, Any>().apply {
                    putAll(mutableCatalogFields)
                    put("authorUid", uid)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                catalogRef.set(createPayload).await()
            } else {
                val updatePayload = mutableMapOf<String, Any>().apply {
                    putAll(mutableCatalogFields)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                catalogRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, manifestRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncStudentClassProgress(progress: StudentClassProgress): Result<Unit> {
        val uid = requireUserId()
        val safeClassId = sanitizeFirestoreId(progress.classId)
        val docRef = db.collection(COLLECTION_USERS).document(uid).collection("progress").document(safeClassId)
        return try {
            val snap = docRef.get().await()
            val mutableFields = buildMap<String, Any> {
                put("status", progress.status.name)
                if (progress.startTimeMs > 0L) put("startTimeMs", progress.startTimeMs)
                if (progress.lastActiveTimeMs > 0L) put("lastActiveTimeMs", progress.lastActiveTimeMs)
                put("activeLearningSeconds", progress.activeLearningSeconds.coerceAtLeast(0L))
                put("idleExcludedSeconds", progress.idleExcludedSeconds.coerceAtLeast(0L))
                put("maxPlaybackPercent", progress.maxPlaybackPercent.toDouble().coerceIn(0.0, 100.0))
                put("lastPlaybackPositionMs", progress.lastPlaybackPositionMs.coerceAtLeast(0L))
                put("isPdfConfirmed", progress.isPdfConfirmed)
                put("pdfReadingSeconds", progress.pdfReadingSeconds.coerceAtLeast(0L))
                put("lastPdfPage", progress.lastPdfPage.coerceAtLeast(0).toLong())
                put("totalPdfPages", progress.totalPdfPages.coerceAtLeast(0).toLong())
                put("visitedPdfPages", progress.visitedPdfPages.sorted().take(500).map { it.toLong() })
                progress.quizScore?.let { put("quizScore", it.toLong()) }
                put("quizPassed", progress.quizPassed)
                progress.completionTimestamp?.let { put("completionTimestamp", it) }
                put("updatedAt", FieldValue.serverTimestamp())
            }

            if (!snap.exists()) {
                val createPayload = mutableMapOf<String, Any>().apply {
                    putAll(mutableFields)
                    put("userId", uid)
                    put("classId", safeClassId)
                    put("createdAt", FieldValue.serverTimestamp())
                }
                docRef.set(createPayload).await()
            } else {
                docRef.update(mutableFields).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // ================= LMS ACCOUNTS, ASSESSMENTS, CERTIFICATES & AUDIT LOGS =================

    fun observeUserAccount(uid: String): Flow<UserProfile?> {
        val safeUid = sanitizeFirestoreId(uid)
        return db.collection(COLLECTION_USERS).document(safeUid)
            .snapshots()
            .map { snap -> if (snap.exists()) docToUserProfile(snap) else null }
    }

    fun observeAllUserAccounts(): Flow<List<UserProfile>> {
        return db.collection(COLLECTION_USERS)
            .snapshots()
            .map { qs -> qs.documents.mapNotNull { docToUserProfile(it) } }
    }

    suspend fun getUserAccountOnce(uid: String): Result<UserProfile?> {
        val safeUid = sanitizeFirestoreId(uid)
        val docRef = db.collection(COLLECTION_USERS).document(safeUid)
        return try {
            val snap = docRef.get().await()
            Result.success(if (snap.exists()) docToUserProfile(snap) else null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertUserAccount(profile: UserProfile, allowRoleAndPrivileges: Boolean = false): Result<Unit> {
        val callerUid = requireUserId()
        val targetUid = sanitizeFirestoreId(profile.authUid.ifBlank { callerUid })
        val docRef = db.collection(COLLECTION_USERS).document(targetUid)
        return try {
            val snap = docRef.get().await()
            val safePhotoUrl = profile.photoUrl.trim().takeIf {
                it.length <= 95000 && (
                    it.startsWith("https://", ignoreCase = true) ||
                        it.startsWith("http://", ignoreCase = true) ||
                        it.startsWith("data:image/", ignoreCase = true)
                    )
            }
            if (!snap.exists()) {
                val payload = buildMap<String, Any> {
                    put("uid", targetUid)
                    put("studentId", profile.studentId.ifBlank { "ISCEB-PGD-2026-0001" })
                    put("fullName", profile.fullName.ifBlank { "ISCEB Learner" }.take(160))
                    put("email", profile.email.ifBlank { "${targetUid}@isceb.org" }.take(200))
                    if (!safePhotoUrl.isNullOrBlank()) put("photoUrl", safePhotoUrl)
                    put("role", if (allowRoleAndPrivileges) profile.role.name else UserRole.STUDENT.name)
                    if (profile.organization.isNotBlank()) put("organization", profile.organization.take(200))
                    if (profile.designation.isNotBlank()) put("designation", profile.designation.take(200))
                    if (profile.mobileNumber.isNotBlank()) put("mobileNumber", profile.mobileNumber.take(64))
                    put("enrolledProgram", profile.enrolledProgram.ifBlank { "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)" }.take(200))
                    put("accountStatus", if (allowRoleAndPrivileges) profile.accountStatus.name else AccountStatus.ACTIVE.name)
                    put("isVerified", if (allowRoleAndPrivileges) profile.isVerified else false)
                    put("assignedModuleIds", if (allowRoleAndPrivileges) profile.assignedModuleIds.toList().take(50) else emptyList<String>())
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                val existingRemotePhoto = snap.getString("photoUrl")?.takeIf { it.isNotBlank() && it.length <= 95000 }
                val effectivePhotoUrl = safePhotoUrl ?: existingRemotePhoto ?: ""
                val updatePayload = buildMap<String, Any> {
                    put("fullName", profile.fullName.ifBlank { "ISCEB Learner" }.take(160))
                    put("photoUrl", effectivePhotoUrl)
                    put("organization", profile.organization.take(200))
                    put("designation", profile.designation.take(200))
                    put("mobileNumber", profile.mobileNumber.take(64))
                    put("enrolledProgram", profile.enrolledProgram.ifBlank { "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)" }.take(200))
                    if (allowRoleAndPrivileges) {
                        put("studentId", profile.studentId)
                        put("email", profile.email)
                        put("role", profile.role.name)
                        put("accountStatus", profile.accountStatus.name)
                        put("isVerified", profile.isVerified)
                        put("assignedModuleIds", profile.assignedModuleIds.toList().take(50))
                    }
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteOwnAccountData(uid: String): Result<Unit> {
        val callerUid = requireUserId()
        val targetUid = sanitizeFirestoreId(uid.ifBlank { callerUid })
        if (callerUid != targetUid) {
            return Result.failure(SecurityException("Only the signed-in account owner can delete their cloud account data."))
        }
        return try {
            val userDocRef = db.collection(COLLECTION_USERS).document(targetUid)

            // 1. Delete per-user progress docs
            val progressDocs = userDocRef.collection("progress").get().await()
            for (doc in progressDocs.documents) {
                doc.reference.delete().await()
            }

            // 2. Delete per-user certificate subcollection docs
            val userCertDocs = userDocRef.collection("certificates").get().await()
            for (doc in userCertDocs.documents) {
                doc.reference.delete().await()
            }

            // 3. Delete student's own assignment submissions
            val submissionDocs = db.collection(COLLECTION_SUBMISSIONS)
                .whereEqualTo("studentId", targetUid)
                .get()
                .await()
            for (doc in submissionDocs.documents) {
                doc.reference.delete().await()
            }

            // 4. Delete user account profile document
            userDocRef.delete().await()

            // 5. Delete Firebase Auth account if session is recent enough, then sign out cleanly
            try {
                auth.currentUser?.delete()?.await()
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                // If recent-login is required, Firestore personal records are already wiped; sign out cleanly.
                auth.signOut()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$targetUid")
            Result.failure(e)
        }
    }

    fun observeAssignments(): Flow<List<ModuleAssignment>> {
        requireUserId()
        return db.collection(COLLECTION_ASSIGNMENTS)
            .snapshots()
            .map { qs -> qs.documents.mapNotNull { docToAssignment(it) } }
    }

    suspend fun upsertAssignment(assignment: ModuleAssignment): Result<Unit> {
        val uid = requireUserId()
        val safeId = sanitizeFirestoreId(assignment.id)
        val docRef = db.collection(COLLECTION_ASSIGNMENTS).document(safeId)
        return try {
            val snap = docRef.get().await()
            val common = buildMap<String, Any> {
                put("title", assignment.title.take(300))
                put("topicDescription", assignment.topicDescription.take(4000))
                put("instructions", assignment.instructions.take(4000))
                put("maxMarks", assignment.maxMarks.coerceIn(1, 1000).toLong())
                put("isFinalProject", assignment.isFinalProject)
                put("isRequired", assignment.isRequiredForCertification)
                put("allowFileAttachment", assignment.allowFileAttachment)
                assignment.dueDateMs?.let { put("dueDateMs", it) }
                if (assignment.createdByName.isNotBlank()) put("createdByName", assignment.createdByName.take(160))
                put("updatedAt", FieldValue.serverTimestamp())
            }
            if (!snap.exists()) {
                val createPayload = mutableMapOf<String, Any>().apply {
                    putAll(common)
                    put("id", safeId)
                    put("moduleId", assignment.moduleId)
                    put("createdByUid", uid)
                    put("createdAt", FieldValue.serverTimestamp())
                }
                docRef.set(createPayload).await()
            } else {
                docRef.update(common).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserSubmissions(userId: String): Flow<List<AssignmentSubmission>> {
        requireUserId()
        return db.collection(COLLECTION_SUBMISSIONS)
            .whereEqualTo("studentId", userId)
            .snapshots()
            .map { qs -> qs.documents.mapNotNull { docToSubmission(it) } }
    }

    fun observeAllSubmissions(): Flow<List<AssignmentSubmission>> {
        requireUserId()
        return db.collection(COLLECTION_SUBMISSIONS)
            .snapshots()
            .map { qs -> qs.documents.mapNotNull { docToSubmission(it) } }
    }

    suspend fun submitAssignmentToCloud(submission: AssignmentSubmission, moduleId: String): Result<Unit> {
        val uid = requireUserId()
        val safeId = sanitizeFirestoreId(submission.submissionId)
        val docRef = db.collection(COLLECTION_SUBMISSIONS).document(safeId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = buildMap<String, Any> {
                    put("submissionId", safeId)
                    put("assignmentId", submission.assignmentId)
                    put("moduleId", moduleId.ifBlank { "mod-1" })
                    put("studentId", uid)
                    put("studentName", submission.studentName.ifBlank { "ISCEB Learner" }.take(160))
                    put("submissionText", submission.submissionText.take(15000))
                    submission.attachedFileName?.takeIf { it.isNotBlank() }?.let { put("attachedFileName", it.take(500)) }
                    put("submissionTimestamp", submission.submissionTimestamp)
                    put("isGraded", false)
                    put("status", SubmissionStatus.SUBMITTED.name)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                val updatePayload = buildMap<String, Any> {
                    put("studentName", submission.studentName.ifBlank { "ISCEB Learner" }.take(160))
                    put("submissionText", submission.submissionText.take(15000))
                    submission.attachedFileName?.takeIf { it.isNotBlank() }?.let { put("attachedFileName", it.take(500)) }
                    put("submissionTimestamp", submission.submissionTimestamp)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun gradeSubmissionInCloud(
        submissionId: String,
        marksAwarded: Int,
        feedback: String,
        graderName: String
    ): Result<Unit> {
        val uid = requireUserId()
        val safeId = sanitizeFirestoreId(submissionId)
        val docRef = db.collection(COLLECTION_SUBMISSIONS).document(safeId)
        return try {
            val payload = mapOf<String, Any>(
                "marksAwarded" to marksAwarded.coerceIn(0, 1000).toLong(),
                "feedback" to feedback.take(4000),
                "isGraded" to true,
                "status" to SubmissionStatus.GRADED.name,
                "gradedByUid" to uid,
                "gradedByName" to graderName.take(160),
                "gradedTimestamp" to System.currentTimeMillis(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeExams(): Flow<List<ModuleExam>> {
        requireUserId()
        return db.collection(COLLECTION_EXAMS)
            .snapshots()
            .map { qs -> qs.documents.mapNotNull { docToExam(it) } }
    }

    suspend fun upsertExam(exam: ModuleExam): Result<Unit> {
        val uid = requireUserId()
        val safeId = sanitizeFirestoreId(exam.id)
        val docRef = db.collection(COLLECTION_EXAMS).document(safeId)
        return try {
            val snap = docRef.get().await()
            val qMaps = exam.questions.map { q ->
                mapOf(
                    "id" to q.id,
                    "question" to q.question,
                    "options" to q.options,
                    "correctOptionIndex" to q.correctOptionIndex.toLong(),
                    "marks" to q.marks.toLong(),
                    "explanation" to q.explanation
                )
            }
            val common = buildMap<String, Any> {
                put("title", exam.title.take(300))
                put("totalMarks", exam.totalMarks.coerceIn(1, 1000).toLong())
                put("isSyllabusFixedMarks", exam.isSyllabusFixedMarks)
                put("passingPercentage", exam.passingPercentage.coerceIn(1, 100).toLong())
                put("durationMinutes", exam.durationMinutes.coerceIn(1, 600).toLong())
                put("maxAttempts", exam.maxAttempts.coerceIn(1, 100).toLong())
                exam.openFromMs?.let { put("openFromMs", it) }
                exam.openUntilMs?.let { put("openUntilMs", it) }
                put("isRequired", exam.isRequiredForCertification)
                put("scheduledWindowNote", exam.scheduledWindowNote.take(500))
                put("questions", qMaps)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            if (!snap.exists()) {
                val createPayload = mutableMapOf<String, Any>().apply {
                    putAll(common)
                    put("id", safeId)
                    put("moduleId", exam.moduleId)
                    put("createdByUid", uid)
                    put("createdAt", FieldValue.serverTimestamp())
                }
                docRef.set(createPayload).await()
            } else {
                docRef.update(common).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserExamAttempts(userId: String): Flow<List<ExamAttempt>> {
        requireUserId()
        return db.collection(COLLECTION_EXAM_ATTEMPTS)
            .whereEqualTo("studentId", userId)
            .snapshots()
            .map { qs -> qs.documents.mapNotNull { docToExamAttempt(it) } }
    }

    fun observeAllExamAttempts(): Flow<List<ExamAttempt>> {
        requireUserId()
        return db.collection(COLLECTION_EXAM_ATTEMPTS)
            .snapshots()
            .map { qs -> qs.documents.mapNotNull { docToExamAttempt(it) } }
    }

    suspend fun recordExamAttemptInCloud(attempt: ExamAttempt, moduleId: String): Result<Unit> {
        val uid = requireUserId()
        val safeId = sanitizeFirestoreId(attempt.attemptId)
        val docRef = db.collection(COLLECTION_EXAM_ATTEMPTS).document(safeId)
        return try {
            val payload = mapOf<String, Any>(
                "attemptId" to safeId,
                "examId" to attempt.examId,
                "moduleId" to moduleId.ifBlank { attempt.moduleId }.ifBlank { "mod-1" },
                "studentId" to uid,
                "studentAnswers" to attempt.studentAnswers.mapValues { (_, v) -> v.toLong() },
                "scoreMarks" to attempt.scoreMarks.coerceIn(0, 1000).toLong(),
                "totalMarks" to attempt.totalMarks.coerceIn(1, 1000).toLong(),
                "percentage" to attempt.percentage.coerceIn(0, 100).toLong(),
                "isPassed" to attempt.isPassed,
                "startTimeMs" to attempt.startTimeMs,
                "submittedTimeMs" to attempt.submittedTimeMs,
                "isAutoSubmittedOnTimeout" to attempt.isAutoSubmittedOnTimeout,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserCertificates(userId: String): Flow<List<CertificateRecord>> {
        requireUserId()
        return db.collection(COLLECTION_USERS).document(sanitizeFirestoreId(userId))
            .collection(COLLECTION_CERTIFICATES)
            .snapshots()
            .map { qs -> qs.documents.mapNotNull { docToCertificate(it) } }
    }

    fun observeAllCertificates(): Flow<List<CertificateRecord>> {
        requireUserId()
        return db.collection(COLLECTION_CERTIFICATES)
            .snapshots()
            .map { qs -> qs.documents.mapNotNull { docToCertificate(it) } }
    }

    suspend fun issueCertificateInCloud(record: CertificateRecord): Result<Unit> {
        val uid = requireUserId()
        val safeCertId = sanitizeFirestoreId(record.certificateId)
        val targetUserUid = sanitizeFirestoreId(record.studentId.ifBlank { uid })
        val globalRef = db.collection(COLLECTION_CERTIFICATES).document(safeCertId)
        val userCertRef = db.collection(COLLECTION_USERS).document(targetUserUid).collection(COLLECTION_CERTIFICATES).document(safeCertId)
        return try {
            val payload = buildMap<String, Any> {
                put("certificateId", safeCertId)
                put("studentId", targetUserUid)
                put("studentCode", record.studentCode)
                put("studentName", record.studentName.take(160))
                put("courseTitle", record.courseTitle.take(300))
                put("issueDate", record.issueDate.take(100))
                put("issueTimestamp", record.issueTimestamp)
                put("institutionName", record.institutionName.take(200))
                put("officialWebsite", record.officialWebsite.take(200))
                put("authorizedSigner", record.authorizedSigner.take(300))
                put("status", CertificateStatus.VALID.name)
                put("completedClassesCount", record.completedClassesCount.coerceAtLeast(0).toLong())
                put("totalRequiredClasses", record.totalRequiredClasses.coerceAtLeast(1).toLong())
                put("verificationUrl", record.verificationUrl.take(500))
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            if (!globalRef.get().await().exists()) {
                globalRef.set(payload).await()
            }
            if (!userCertRef.get().await().exists()) {
                userCertRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, globalRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCertificateStatusInCloud(
        certId: String,
        studentUid: String,
        status: CertificateStatus,
        reason: String?
    ): Result<Unit> {
        requireUserId()
        val safeCertId = sanitizeFirestoreId(certId)
        val safeStudentUid = sanitizeFirestoreId(studentUid)
        val globalRef = db.collection(COLLECTION_CERTIFICATES).document(safeCertId)
        val userCertRef = db.collection(COLLECTION_USERS).document(safeStudentUid).collection(COLLECTION_CERTIFICATES).document(safeCertId)
        return try {
            val updateMap = mutableMapOf<String, Any?>(
                "status" to status.name,
                "revocationReason" to reason,
                "revocationTimestamp" to if (status == CertificateStatus.REVOKED) System.currentTimeMillis() else null,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            globalRef.update(updateMap).await()
            if (userCertRef.get().await().exists()) {
                userCertRef.update(updateMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, globalRef.path)
            Result.failure(e)
        }
    }

    suspend fun verifyCertificateOnline(certId: String): Result<CertificateRecord?> {
        requireUserId()
        val safeCertId = sanitizeFirestoreId(certId.trim())
        val docRef = db.collection(COLLECTION_CERTIFICATES).document(safeCertId)
        return try {
            val snap = docRef.get().await()
            Result.success(if (snap.exists()) docToCertificate(snap) else null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeAuditLogs(): Flow<List<AuditLogEntry>> {
        requireUserId()
        return db.collection(COLLECTION_AUDIT_LOGS)
            .orderBy("timestampMs", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { qs -> qs.documents.mapNotNull { docToAuditLog(it) } }
    }

    suspend fun appendAuditLog(entry: AuditLogEntry): Result<Unit> {
        val uid = requireUserId()
        val safeId = sanitizeFirestoreId(entry.logId)
        val docRef = db.collection(COLLECTION_AUDIT_LOGS).document(safeId)
        return try {
            val payload = mapOf<String, Any>(
                "logId" to safeId,
                "actorUid" to uid,
                "actorName" to entry.actorName.ifBlank { "ISCEB Staff" }.take(160),
                "actorRole" to if (entry.actorRole == UserRole.TEACHER) UserRole.TEACHER.name else UserRole.ADMIN.name,
                "actionType" to entry.actionType.take(100),
                "targetId" to entry.targetId.take(128),
                "summary" to entry.summary.take(1000),
                "timestampMs" to entry.timestampMs,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun uploadLessonFileChunks(
        fileId: String,
        bytes: ByteArray
    ): Result<Int> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        if (bytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Cannot upload an empty file."))
        }
        if (bytes.size > MAX_CLOUD_FILE_BYTES) {
            return@withContext Result.failure(
                IllegalArgumentException("File exceeds ${MAX_CLOUD_FILE_BYTES / (1024 * 1024)} MB direct Firestore sync limit.")
            )
        }
        val totalChunks = (bytes.size + CHUNK_RAW_BYTES - 1) / CHUNK_RAW_BYTES
        return@withContext try {
            for (chunkIndex in 0 until totalChunks) {
                val start = chunkIndex * CHUNK_RAW_BYTES
                val end = minOf(bytes.size, start + CHUNK_RAW_BYTES)
                val slice = bytes.copyOfRange(start, end)
                val b64 = android.util.Base64.encodeToString(slice, android.util.Base64.NO_WRAP)
                val docId = "${fileId}_$chunkIndex"
                val docRef = db.collection(COLLECTION_LESSON_FILES).document(docId)
                val existingSnap = docRef.get().await()
                if (!existingSnap.exists()) {
                    val createPayload = mapOf<String, Any>(
                        "fileId" to fileId,
                        "chunkIndex" to chunkIndex.toLong(),
                        "totalChunks" to totalChunks.toLong(),
                        "dataBase64" to b64,
                        "authorUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(createPayload).await()
                } else {
                    val updatePayload = mapOf<String, Any>(
                        "fileId" to fileId,
                        "chunkIndex" to chunkIndex.toLong(),
                        "totalChunks" to totalChunks.toLong(),
                        "dataBase64" to b64,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updatePayload).await()
                }
            }
            Result.success(totalChunks)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$COLLECTION_LESSON_FILES/$fileId")
            Result.failure(e)
        }
    }

    suspend fun downloadLessonFileBytes(
        fileId: String,
        chunkCount: Int
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        requireUserId()
        if (fileId.isBlank() || chunkCount <= 0) {
            return@withContext Result.failure(IllegalArgumentException("No cloud file chunks referenced."))
        }
        return@withContext try {
            val buffer = java.io.ByteArrayOutputStream()
            for (chunkIndex in 0 until chunkCount) {
                val docId = "${fileId}_$chunkIndex"
                val snap = db.collection(COLLECTION_LESSON_FILES).document(docId).get().await()
                if (!snap.exists()) {
                    return@withContext Result.failure(IllegalStateException("Missing cloud chunk $chunkIndex for $fileId"))
                }
                val b64 = snap.getString("dataBase64").orEmpty()
                if (b64.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Empty cloud chunk $chunkIndex for $fileId"))
                }
                val decoded = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                buffer.write(decoded)
            }
            Result.success(buffer.toByteArray())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "$COLLECTION_LESSON_FILES/$fileId")
            Result.failure(e)
        }
    }

    /**
     * Cache-busted HTTPS manifest / version.json / GitHub Release check using OkHttp.
     * Bypasses HTTP, proxy, and CDN caches via CacheControl.FORCE_NETWORK,
     * no-cache/no-store headers, and timestamp query parameter (?t=...).
     */
    suspend fun fetchRemoteManifestDetailed(endpointUrl: String): RemoteManifestFetchResult =
        withContext(Dispatchers.IO) {
            if (endpointUrl.isBlank()) {
                return@withContext RemoteManifestFetchResult.Failure("Update manifest URL is not configured.")
            }
            val parsedUrl = endpointUrl.trim().toHttpUrlOrNull()
                ?: return@withContext RemoteManifestFetchResult.Failure("Malformed update manifest URL: $endpointUrl")
            val cacheBustedUrl = parsedUrl.newBuilder()
                .setQueryParameter("t", System.currentTimeMillis().toString())
                .build()

            val request = Request.Builder()
                .url(cacheBustedUrl)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .build()

            try {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val hint = if (response.code == 404) {
                            "Update manifest not published yet on server (HTTP 404)."
                        } else {
                            "Update server returned HTTP ${response.code}."
                        }
                        return@withContext RemoteManifestFetchResult.Failure(hint, httpCode = response.code)
                    }
                    val body = response.body?.string()
                        ?: return@withContext RemoteManifestFetchResult.Failure("Update server returned an empty response.")
                    val parsed = parseRemoteManifestJson(body)
                        ?: return@withContext RemoteManifestFetchResult.Failure("Invalid update manifest format received from server.")
                    RemoteManifestFetchResult.Success(parsed)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                RemoteManifestFetchResult.Failure(
                    "Unable to reach update server: ${e.localizedMessage ?: "Network unavailable"}"
                )
            }
        }

    suspend fun fetchRemoteManifestCacheBusted(endpointUrl: String): CurriculumPublishManifest? {
        return when (val result = fetchRemoteManifestDetailed(endpointUrl)) {
            is RemoteManifestFetchResult.Success -> result.manifest
            is RemoteManifestFetchResult.Failure -> null
        }
    }

    internal fun parseRemoteManifestJson(jsonBody: String): CurriculumPublishManifest? {
        return try {
            val obj = JSONObject(jsonBody)
            // Support GitHub Releases JSON format, version.json format, or legacy ISCEB manifest JSON
            if (obj.has("tag_name") && !obj.has("versionCode")) {
                val tagName = obj.optString("tag_name", "v1.0.0")
                val assets = obj.optJSONArray("assets")
                var apkAssetUrl = obj.optString("html_url", AppUpdateConfig.DEFAULT_APK_DIRECT_URL)
                var apkSize = 0L
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkAssetUrl = asset.optString("browser_download_url", apkAssetUrl)
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }
                val extractedVersion = extractVersionCodeFromTag(tagName)
                CurriculumPublishManifest(
                    versionCode = extractedVersion + 100,
                    versionTag = tagName,
                    publishedTimestamp = System.currentTimeMillis(),
                    publishedBy = "ISCEB Release Server",
                    releaseNotes = obj.optString("body", "Updated release available."),
                    announcement = null,
                    minSupportedAppVersion = 1,
                    latestApkVersionCode = extractedVersion.coerceAtLeast(BuildConfig.VERSION_CODE),
                    latestApkVersionName = tagName.removePrefix("v"),
                    apkDownloadUrl = apkAssetUrl,
                    apkReleaseNotes = obj.optString("body", ""),
                    apkFileSizeBytes = apkSize,
                    apkReleaseDate = obj.optString("published_at", "").take(10),
                    downloadPageUrl = obj.optString("html_url", AppUpdateConfig.DEFAULT_DOWNLOAD_PAGE_URL)
                )
            } else {
                val isVersionJsonShape = obj.has("versionName") || obj.has("apkUrl")
                val resolvedApkCode = when {
                    obj.has("latestApkVersionCode") -> obj.optInt("latestApkVersionCode", BuildConfig.VERSION_CODE)
                    isVersionJsonShape && obj.has("versionCode") -> obj.optInt("versionCode", BuildConfig.VERSION_CODE)
                    else -> BuildConfig.VERSION_CODE
                }
                val resolvedApkName = when {
                    obj.has("latestApkVersionName") -> obj.optString("latestApkVersionName", BuildConfig.VERSION_NAME)
                    obj.has("versionName") -> obj.optString("versionName", BuildConfig.VERSION_NAME)
                    else -> BuildConfig.VERSION_NAME
                }
                val resolvedCurriculumCode = when {
                    obj.has("curriculumVersionCode") -> obj.optInt("curriculumVersionCode", 102)
                    obj.has("latestApkVersionCode") -> obj.optInt("versionCode", 102)
                    isVersionJsonShape -> 102
                    else -> obj.optInt("versionCode", 102)
                }
                val resolvedApkUrl = obj.optString("apkUrl", "").trim()
                    .ifBlank { obj.optString("apkDownloadUrl", AppUpdateConfig.DEFAULT_APK_DIRECT_URL).trim() }
                    .ifBlank { AppUpdateConfig.DEFAULT_APK_DIRECT_URL }
                val resolvedNotes = obj.optString("apkReleaseNotes", "").trim()
                    .ifBlank { obj.optString("releaseNotes", "").trim() }

                CurriculumPublishManifest(
                    versionCode = resolvedCurriculumCode,
                    versionTag = obj.optString("versionTag", "v$resolvedApkName"),
                    publishedTimestamp = obj.optLong("publishedTimestamp", System.currentTimeMillis()),
                    publishedBy = obj.optString("publishedBy", "Md. Nafiul Islam Nahin (Developer of ISCEB App)")
                        .replace("Founder & Developer of ISCEB", "Developer of ISCEB App")
                        .replace("Academic Detector", "Developer of ISCEB App"),
                    releaseNotes = obj.optString("releaseNotes", resolvedNotes),
                    announcement = obj.optString("announcement", "").takeIf { it.isNotBlank() },
                    minSupportedAppVersion = obj.optInt(
                        "minSupportedVersionCode",
                        obj.optInt("minSupportedAppVersion", 1)
                    ),
                    latestApkVersionCode = resolvedApkCode,
                    latestApkVersionName = resolvedApkName,
                    apkDownloadUrl = resolvedApkUrl,
                    apkReleaseNotes = resolvedNotes,
                    apkSha256 = obj.optString("sha256", "").trim().lowercase(),
                    apkFileSizeBytes = obj.optLong("fileSizeBytes", 0L),
                    apkReleaseDate = obj.optString("releaseDate", "").trim(),
                    downloadPageUrl = obj.optString("downloadPageUrl", AppUpdateConfig.DEFAULT_DOWNLOAD_PAGE_URL)
                        .trim()
                        .ifBlank { AppUpdateConfig.DEFAULT_DOWNLOAD_PAGE_URL }
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractVersionCodeFromTag(tag: String): Int {
        val digits = tag.replace(Regex("[^0-9]"), "")
        return digits.toIntOrNull()?.coerceAtLeast(1) ?: 1
    }

    private fun buildMutableManifestMap(manifest: CurriculumPublishManifest): Map<String, Any> {
        return buildMap {
            put("versionCode", manifest.versionCode.toLong())
            put("versionTag", manifest.versionTag)
            put("publishedTimestamp", manifest.publishedTimestamp)
            put("publishedBy", manifest.publishedBy)
            put("releaseNotes", manifest.releaseNotes)
            manifest.announcement?.let { put("announcement", it) }
            put("minSupportedAppVersion", manifest.minSupportedAppVersion.toLong())
            put("latestApkVersionCode", manifest.latestApkVersionCode.toLong())
            put("latestApkVersionName", manifest.latestApkVersionName)
            put("apkDownloadUrl", manifest.apkDownloadUrl)
            if (manifest.apkReleaseNotes.isNotBlank()) {
                put("apkReleaseNotes", manifest.apkReleaseNotes)
            }
        }
    }

    private fun buildMutableCatalogMap(
        versionCode: Int,
        modules: List<CourseModule>,
        classes: List<CourseClass>,
        settings: AdminCompletionSettings,
        founderProfileJson: String = "",
        developerProfileJson: String = "",
        membersJson: String = "",
        examsJson: String = "",
        assignmentsJson: String = ""
    ): Map<String, Any> {
        val moduleMaps = modules.map { mod ->
            mapOf(
                "id" to mod.id,
                "number" to mod.number.toLong(),
                "title" to mod.title,
                "description" to mod.description,
                "part" to mod.part.name,
                "category" to mod.category,
                "estimatedHours" to mod.estimatedHours.toLong(),
                "iconName" to mod.iconName,
                "isRequired" to mod.isRequired
            )
        }
        val classMaps = classes.map { cls ->
            buildMap {
                put("id", cls.id)
                put("moduleId", cls.moduleId)
                put("orderIndex", cls.orderIndex.toLong())
                put("lectureCode", cls.lectureCode)
                put("title", cls.title)
                put("summary", cls.summary)
                put("durationMinutes", cls.durationMinutes.toLong())
                put("isPublished", cls.isPublished)
                put("learningObjectives", cls.learningObjectives)
                cls.minStudySecondsOverride?.let { put("minStudySecondsOverride", it) }
                put("allowMediaDownload", cls.allowMediaDownload)
                cls.pdfMedia?.takeIf { isCloudSyncedMedia(it) }?.let { put("pdfMedia", mediaToMap(it)) }
                cls.audioMedia?.takeIf { isCloudSyncedMedia(it) }?.let { put("audioMedia", mediaToMap(it)) }
                cls.videoMedia?.takeIf { isCloudSyncedMedia(it) }?.let { put("videoMedia", mediaToMap(it)) }
                val syncedAttachments = cls.attachments.filter { isCloudSyncedMedia(it) }
                if (syncedAttachments.isNotEmpty()) {
                    put("attachments", syncedAttachments.map { mediaToMap(it) })
                }
            }
        }
        val settingsMap = buildMap<String, Any> {
            put("minLearningTimeSeconds", settings.minLearningTimeSeconds)
            put("minPlaybackPercent", settings.minPlaybackPercent.toDouble())
            put("minPdfReadingSeconds", settings.minPdfReadingSeconds)
            put("isQuizRequired", settings.isQuizRequired)
            put("quizPassingScore", settings.quizPassingScore.toLong())
            put("requiredModuleIds", settings.requiredModuleIds.toList())
            put("requiredExamIds", settings.requiredExamIds.toList())
            put("requiredAssignmentIds", settings.requiredAssignmentIds.toList())
            put("requireGradedFinalProject", settings.requireGradedFinalProject)
            put("requirePdfAllPagesVisited", settings.requirePdfAllPagesVisited)
            put("institutionName", settings.institutionName)
            put("officialWebsite", settings.officialWebsite)
            put("institutionAccreditation", settings.institutionAccreditation)
            put("leftSignerName", settings.leftSignerName)
            put("leftSignerTitle", settings.leftSignerTitle)
            put("authorizedSignerName", settings.authorizedSignerName)
            put("authorizedSignerTitle", settings.authorizedSignerTitle)
            put("certificateTitle", settings.certificateTitle)
            put("certificatesEnabled", settings.certificatesEnabled)
            put("demoCertificateEnabled", settings.demoCertificateEnabled)
            put("classTimeWindow", settings.scheduleConfig.classTimeWindow)
            put("timeZoneId", settings.scheduleConfig.timeZoneId)
            put("remoteSyncManifestUrl", settings.remoteSyncManifestUrl)
            put("apkDownloadUrl", settings.apkDownloadUrl)
            put("autoApplyContentUpdates", settings.autoApplyContentUpdates)
            if (founderProfileJson.isNotBlank()) put("founderProfileJson", founderProfileJson)
            if (developerProfileJson.isNotBlank()) put("developerProfileJson", developerProfileJson)
            if (membersJson.isNotBlank()) put("membersJson", membersJson)
            if (examsJson.isNotBlank()) put("examsJson", examsJson)
            if (assignmentsJson.isNotBlank()) put("assignmentsJson", assignmentsJson)
        }
        return mapOf(
            "versionCode" to versionCode.toLong(),
            "modules" to moduleMaps,
            "classes" to classMaps,
            "settings" to settingsMap
        )
    }

    private fun isCloudSyncedMedia(media: LessonMedia): Boolean {
        val trimmedUrl = media.url.trim()
        val hasHttp = trimmedUrl.startsWith("http://", ignoreCase = true) || trimmedUrl.startsWith("https://", ignoreCase = true)
        return hasHttp || (media.cloudFileId.isNotBlank() && media.cloudChunkCount > 0)
    }

    private fun mediaToMap(media: LessonMedia): Map<String, Any> {
        val trimmedUrl = media.url.trim()
        val safeCloudUrl = if (trimmedUrl.startsWith("http://", ignoreCase = true) || trimmedUrl.startsWith("https://", ignoreCase = true)) {
            trimmedUrl
        } else {
            ""
        }
        return buildMap {
            put("id", media.id)
            put("type", media.type.name)
            put("fileName", media.fileName)
            put("fileSizeFormatted", media.fileSizeFormatted)
            put("fileSizeBytes", media.fileSizeBytes)
            put("url", safeCloudUrl)
            put("title", media.title)
            put("description", media.description)
            put("mimeType", media.mimeType)
            if (media.cloudFileId.isNotBlank()) put("cloudFileId", media.cloudFileId)
            if (media.cloudChunkCount > 0) put("cloudChunkCount", media.cloudChunkCount.toLong())
            media.durationFormatted?.let { put("durationFormatted", it) }
            put("uploadTimestamp", media.uploadTimestamp)
        }
    }

    private fun snapshotToManifest(snapshot: DocumentSnapshot): CurriculumPublishManifest {
        return CurriculumPublishManifest(
            versionCode = (snapshot.getLong("versionCode") ?: 102L).toInt(),
            versionTag = snapshot.getString("versionTag") ?: "v1.3.0",
            publishedTimestamp = snapshot.getLong("publishedTimestamp") ?: System.currentTimeMillis(),
            publishedBy = (snapshot.getString("publishedBy") ?: "Md. Nafiul Islam Nahin (Developer of ISCEB App)")
                .replace("Founder & Developer of ISCEB", "Developer of ISCEB App")
                .replace("Academic Detector", "Developer of ISCEB App"),
            releaseNotes = snapshot.getString("releaseNotes") ?: "",
            announcement = snapshot.getString("announcement"),
            minSupportedAppVersion = (snapshot.getLong("minSupportedAppVersion") ?: 1L).toInt(),
            latestApkVersionCode = (snapshot.getLong("latestApkVersionCode") ?: BuildConfig.VERSION_CODE.toLong()).toInt(),
            latestApkVersionName = snapshot.getString("latestApkVersionName") ?: BuildConfig.VERSION_NAME,
            apkDownloadUrl = snapshot.getString("apkDownloadUrl") ?: AppUpdateConfig.DEFAULT_APK_DIRECT_URL,
            apkReleaseNotes = snapshot.getString("apkReleaseNotes") ?: ""
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun snapshotToCatalog(snapshot: DocumentSnapshot): CloudCatalogSnapshot? {
        val versionCode = (snapshot.getLong("versionCode") ?: 100L).toInt()
        val rawModules = snapshot.get("modules") as? List<Map<String, Any?>> ?: emptyList()
        val rawClasses = snapshot.get("classes") as? List<Map<String, Any?>> ?: emptyList()
        val rawSettings = snapshot.get("settings") as? Map<String, Any?> ?: emptyMap()

        val modules = rawModules.mapNotNull { m ->
            val id = m["id"] as? String ?: return@mapNotNull null
            val number = (m["number"] as? Number)?.toInt() ?: 1
            val partName = m["part"] as? String
            val part = try {
                if (partName != null) CoursePart.valueOf(partName)
                else if (number <= 6) CoursePart.PART_1_BASIC else CoursePart.PART_2_ADVANCE
            } catch (_: Exception) {
                if (number <= 6) CoursePart.PART_1_BASIC else CoursePart.PART_2_ADVANCE
            }
            CourseModule(
                id = id,
                number = number,
                title = m["title"] as? String ?: "",
                description = m["description"] as? String ?: "",
                part = part,
                category = m["category"] as? String ?: "Core SCM",
                estimatedHours = (m["estimatedHours"] as? Number)?.toInt() ?: 4,
                iconName = m["iconName"] as? String ?: "inventory",
                isRequired = m["isRequired"] as? Boolean ?: true
            )
        }

        val classes = rawClasses.mapNotNull { c ->
            val id = c["id"] as? String ?: return@mapNotNull null
            val objList = (c["learningObjectives"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val attachmentsList = (c["attachments"] as? List<*>)?.mapNotNull { item ->
                mapToMedia(item as? Map<String, Any?>)
            } ?: emptyList()
            CourseClass(
                id = id,
                moduleId = c["moduleId"] as? String ?: "mod-1",
                orderIndex = (c["orderIndex"] as? Number)?.toInt() ?: 1,
                title = c["title"] as? String ?: "",
                summary = c["summary"] as? String ?: "",
                durationMinutes = (c["durationMinutes"] as? Number)?.toInt() ?: 30,
                isPublished = c["isPublished"] as? Boolean ?: true,
                learningObjectives = objList,
                pdfMedia = mapToMedia(c["pdfMedia"] as? Map<String, Any?>),
                audioMedia = mapToMedia(c["audioMedia"] as? Map<String, Any?>),
                videoMedia = mapToMedia(c["videoMedia"] as? Map<String, Any?>),
                attachments = attachmentsList,
                lectureCode = c["lectureCode"] as? String ?: "",
                minStudySecondsOverride = (c["minStudySecondsOverride"] as? Number)?.toLong(),
                allowMediaDownload = c["allowMediaDownload"] as? Boolean ?: false
            )
        }

        val reqIds = (rawSettings["requiredModuleIds"] as? List<*>)?.mapNotNull { it as? String }?.toSet()
            ?: (1..12).map { "mod-$it" }.toSet()
        val reqExamIds = (rawSettings["requiredExamIds"] as? List<*>)?.mapNotNull { it as? String }?.toSet()
            ?: setOf("exam-mod-1", "exam-mod-2", "exam-mod-3", "exam-mod-6", "exam-final")
        val reqAsgIds = (rawSettings["requiredAssignmentIds"] as? List<*>)?.mapNotNull { it as? String }?.toSet()
            ?: setOf("asg-12-final-project")

        val settings = AdminCompletionSettings(
            minLearningTimeSeconds = (rawSettings["minLearningTimeSeconds"] as? Number)?.toLong() ?: 60L,
            minPlaybackPercent = (rawSettings["minPlaybackPercent"] as? Number)?.toFloat() ?: 80f,
            minPdfReadingSeconds = (rawSettings["minPdfReadingSeconds"] as? Number)?.toLong() ?: 45L,
            isQuizRequired = rawSettings["isQuizRequired"] as? Boolean ?: true,
            quizPassingScore = (rawSettings["quizPassingScore"] as? Number)?.toInt() ?: 70,
            requiredModuleIds = reqIds,
            requiredExamIds = reqExamIds,
            requiredAssignmentIds = reqAsgIds,
            requireGradedFinalProject = rawSettings["requireGradedFinalProject"] as? Boolean ?: false,
            requirePdfAllPagesVisited = rawSettings["requirePdfAllPagesVisited"] as? Boolean ?: false,
            institutionName = (rawSettings["institutionName"] as? String)
                ?.let { if (it.contains("International Supply Chain", ignoreCase = true)) "ISCEB" else it }
                ?: "ISCEB",
            officialWebsite = rawSettings["officialWebsite"] as? String ?: "www.isceb.org",
            institutionAccreditation = rawSettings["institutionAccreditation"] as? String
                ?: "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)",
            leftSignerName = (rawSettings["leftSignerName"] as? String)
                ?.takeIf { it.isNotBlank() && !it.contains("Nafiul", ignoreCase = true) && !it.equals("Hasina Khan", ignoreCase = true) }
                ?: "Hasina Akter Khan",
            leftSignerTitle = (rawSettings["leftSignerTitle"] as? String)
                ?.takeIf { it.isNotBlank() && !it.contains("Developer", ignoreCase = true) }
                ?: "Chairman & CEO",
            authorizedSignerName = (rawSettings["authorizedSignerName"] as? String)
                ?.let {
                    if (it.isBlank() || it.contains("Nafiul", ignoreCase = true) || it.contains("Developer", ignoreCase = true) || it.contains("Academic", ignoreCase = true) || it.contains("Hasina", ignoreCase = true)) {
                        "Md. Amirul Islam"
                    } else if (it.equals("Amirul Islam", ignoreCase = true)) {
                        "Md. Amirul Islam"
                    } else {
                        it.substringBefore(",").trim()
                    }
                }
                ?: "Md. Amirul Islam",
            authorizedSignerTitle = (rawSettings["authorizedSignerTitle"] as? String)
                ?.takeIf { it.isNotBlank() && !it.contains("Developer", ignoreCase = true) && !it.contains("Academic Directorate", ignoreCase = true) }
                ?: "Founder of ISCEB",
            certificateTitle = rawSettings["certificateTitle"] as? String
                ?: "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)",
            certificatesEnabled = rawSettings["certificatesEnabled"] as? Boolean ?: true,
            demoCertificateEnabled = rawSettings["demoCertificateEnabled"] as? Boolean ?: true,
            scheduleConfig = CourseScheduleConfig(
                classTimeWindow = rawSettings["classTimeWindow"] as? String ?: "9:00 PM to 11:00 PM",
                timeZoneId = rawSettings["timeZoneId"] as? String ?: "Asia/Dhaka"
            ),
            remoteSyncManifestUrl = (rawSettings["remoteSyncManifestUrl"] as? String)
                ?.ifBlank { AppUpdateConfig.DEFAULT_VERSION_JSON_URL }
                ?: AppUpdateConfig.DEFAULT_VERSION_JSON_URL,
            apkDownloadUrl = (rawSettings["apkDownloadUrl"] as? String)
                ?.takeIf { it.isNotBlank() && it != "https://www.isceb.org/app/releases/latest" }
                ?: AppUpdateConfig.DEFAULT_APK_DIRECT_URL,
            autoApplyContentUpdates = rawSettings["autoApplyContentUpdates"] as? Boolean ?: true
        )

        return CloudCatalogSnapshot(
            versionCode = versionCode,
            modules = modules,
            classes = classes,
            settings = settings,
            founderProfileJson = rawSettings["founderProfileJson"] as? String ?: "",
            developerProfileJson = rawSettings["developerProfileJson"] as? String ?: "",
            membersJson = rawSettings["membersJson"] as? String ?: "",
            examsJson = rawSettings["examsJson"] as? String ?: "",
            assignmentsJson = rawSettings["assignmentsJson"] as? String ?: ""
        )
    }

    private fun mapToMedia(map: Map<String, Any?>?): LessonMedia? {
        if (map == null) return null
        val typeStr = map["type"] as? String ?: return null
        val mediaType = try {
            MediaType.valueOf(typeStr)
        } catch (_: Exception) {
            return null
        }
        return LessonMedia(
            type = mediaType,
            fileName = map["fileName"] as? String ?: "",
            fileSizeFormatted = map["fileSizeFormatted"] as? String ?: "0 B",
            fileSizeBytes = (map["fileSizeBytes"] as? Number)?.toLong() ?: 0L,
            url = map["url"] as? String ?: "",
            durationFormatted = map["durationFormatted"] as? String,
            uploadTimestamp = (map["uploadTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            id = map["id"] as? String ?: java.util.UUID.randomUUID().toString(),
            title = map["title"] as? String ?: "",
            description = map["description"] as? String ?: "",
            mimeType = map["mimeType"] as? String ?: "",
            cloudFileId = map["cloudFileId"] as? String ?: "",
            cloudChunkCount = (map["cloudChunkCount"] as? Number)?.toInt() ?: 0
        )
    }

    private fun docToStudentProgress(doc: DocumentSnapshot): StudentClassProgress? {
        val classId = doc.getString("classId") ?: doc.id
        val userId = doc.getString("userId") ?: return null
        val statusName = doc.getString("status") ?: ClassStatus.NOT_STARTED.name
        val status = try {
            ClassStatus.valueOf(statusName)
        } catch (_: Exception) {
            ClassStatus.NOT_STARTED
        }
        val visited = (doc.get("visitedPdfPages") as? List<*>)
            ?.mapNotNull { (it as? Number)?.toInt() }
            ?.toSet()
            ?: emptySet()
        return StudentClassProgress(
            classId = classId,
            studentId = userId,
            status = status,
            startTimeMs = doc.getLong("startTimeMs") ?: 0L,
            lastActiveTimeMs = doc.getLong("lastActiveTimeMs") ?: 0L,
            activeLearningSeconds = doc.getLong("activeLearningSeconds") ?: 0L,
            maxPlaybackPercent = (doc.getDouble("maxPlaybackPercent") ?: 0.0).toFloat(),
            isPdfConfirmed = doc.getBoolean("isPdfConfirmed") ?: false,
            pdfReadingSeconds = doc.getLong("pdfReadingSeconds") ?: 0L,
            quizScore = doc.getLong("quizScore")?.toInt(),
            quizPassed = doc.getBoolean("quizPassed") ?: false,
            completionTimestamp = doc.getLong("completionTimestamp"),
            lastPlaybackPositionMs = doc.getLong("lastPlaybackPositionMs") ?: 0L,
            lastPdfPage = (doc.getLong("lastPdfPage") ?: 0L).toInt(),
            totalPdfPages = (doc.getLong("totalPdfPages") ?: 0L).toInt(),
            visitedPdfPages = visited,
            idleExcludedSeconds = doc.getLong("idleExcludedSeconds") ?: 0L
        )
    }

    private fun docToUserProfile(doc: DocumentSnapshot): UserProfile? {
        val uid = doc.getString("uid") ?: doc.id
        val role = try {
            UserRole.valueOf(doc.getString("role") ?: UserRole.STUDENT.name)
        } catch (_: Exception) {
            UserRole.STUDENT
        }
        val accStatus = try {
            AccountStatus.valueOf(doc.getString("accountStatus") ?: AccountStatus.ACTIVE.name)
        } catch (_: Exception) {
            AccountStatus.ACTIVE
        }
        val assigned = (doc.get("assignedModuleIds") as? List<*>)?.mapNotNull { it as? String }?.toSet() ?: emptySet()
        return UserProfile(
            id = uid,
            fullName = doc.getString("fullName") ?: return null,
            email = doc.getString("email") ?: "",
            role = role,
            organization = doc.getString("organization") ?: "ISCEB Enrolled Professional",
            designation = doc.getString("designation") ?: "",
            mobileNumber = doc.getString("mobileNumber") ?: "",
            studentId = doc.getString("studentId") ?: "",
            photoUrl = doc.getString("photoUrl") ?: "",
            enrolledProgram = doc.getString("enrolledProgram") ?: "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)",
            accountStatus = accStatus,
            isVerified = doc.getBoolean("isVerified") ?: false,
            assignedModuleIds = assigned,
            authUid = uid,
            createdAtMs = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
            updatedAtMs = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
        )
    }

    private fun docToAssignment(doc: DocumentSnapshot): ModuleAssignment? {
        val id = doc.getString("id") ?: doc.id
        return ModuleAssignment(
            id = id,
            moduleId = doc.getString("moduleId") ?: return null,
            title = doc.getString("title") ?: return null,
            topicDescription = doc.getString("topicDescription") ?: "",
            instructions = doc.getString("instructions") ?: "",
            maxMarks = (doc.getLong("maxMarks") ?: 20L).toInt(),
            isFinalProject = doc.getBoolean("isFinalProject") ?: false,
            isRequiredForCertification = doc.getBoolean("isRequired") ?: (doc.getBoolean("isFinalProject") ?: false),
            allowFileAttachment = doc.getBoolean("allowFileAttachment") ?: true,
            dueDateMs = doc.getLong("dueDateMs"),
            createdByUid = doc.getString("createdByUid") ?: "system",
            createdByName = doc.getString("createdByName") ?: "ISCEB Faculty"
        )
    }

    private fun docToSubmission(doc: DocumentSnapshot): AssignmentSubmission? {
        val subId = doc.getString("submissionId") ?: doc.id
        val isGraded = doc.getBoolean("isGraded") ?: false
        val status = try {
            SubmissionStatus.valueOf(doc.getString("status") ?: if (isGraded) SubmissionStatus.GRADED.name else SubmissionStatus.SUBMITTED.name)
        } catch (_: Exception) {
            if (isGraded) SubmissionStatus.GRADED else SubmissionStatus.SUBMITTED
        }
        return AssignmentSubmission(
            submissionId = subId,
            assignmentId = doc.getString("assignmentId") ?: return null,
            studentId = doc.getString("studentId") ?: return null,
            studentName = doc.getString("studentName") ?: "ISCEB Student",
            submissionText = doc.getString("submissionText") ?: "",
            attachedFileName = doc.getString("attachedFileName"),
            submissionTimestamp = doc.getLong("submissionTimestamp") ?: 0L,
            marksAwarded = doc.getLong("marksAwarded")?.toInt(),
            feedback = doc.getString("feedback"),
            isGraded = isGraded,
            moduleId = doc.getString("moduleId") ?: "",
            status = status,
            gradedByUid = doc.getString("gradedByUid"),
            gradedByName = doc.getString("gradedByName"),
            gradedTimestamp = doc.getLong("gradedTimestamp")
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun docToExam(doc: DocumentSnapshot): ModuleExam? {
        val id = doc.getString("id") ?: doc.id
        val rawQuestions = doc.get("questions") as? List<Map<String, Any?>> ?: emptyList()
        val parsedQuestions = rawQuestions.mapNotNull { q ->
            val qId = q["id"] as? String ?: return@mapNotNull null
            val qText = q["question"] as? String ?: return@mapNotNull null
            val opts = (q["options"] as? List<*>)?.mapNotNull { it as? String } ?: return@mapNotNull null
            ExamQuestion(
                id = qId,
                question = qText,
                options = opts,
                correctOptionIndex = (q["correctOptionIndex"] as? Number)?.toInt() ?: 0,
                marks = (q["marks"] as? Number)?.toInt() ?: 1,
                explanation = q["explanation"] as? String ?: ""
            )
        }
        return ModuleExam(
            id = id,
            moduleId = doc.getString("moduleId") ?: return null,
            title = doc.getString("title") ?: return null,
            totalMarks = (doc.getLong("totalMarks") ?: 20L).toInt(),
            isSyllabusFixedMarks = doc.getBoolean("isSyllabusFixedMarks") ?: false,
            passingPercentage = (doc.getLong("passingPercentage") ?: 60L).toInt(),
            durationMinutes = (doc.getLong("durationMinutes") ?: 30L).toInt(),
            questions = parsedQuestions,
            scheduledWindowNote = doc.getString("scheduledWindowNote") ?: "Class hours: 9:00 PM to 11:00 PM (Asia/Dhaka)",
            maxAttempts = (doc.getLong("maxAttempts") ?: 3L).toInt(),
            openFromMs = doc.getLong("openFromMs"),
            openUntilMs = doc.getLong("openUntilMs"),
            isRequiredForCertification = doc.getBoolean("isRequired") ?: false,
            createdByUid = doc.getString("createdByUid") ?: "system"
        )
    }

    private fun docToExamAttempt(doc: DocumentSnapshot): ExamAttempt? {
        val rawAnswers = doc.get("studentAnswers") as? Map<*, *> ?: emptyMap<Any, Any>()
        val answersMap = rawAnswers.entries.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val value = (v as? Number)?.toInt() ?: return@mapNotNull null
            key to value
        }.toMap()
        return ExamAttempt(
            attemptId = doc.getString("attemptId") ?: doc.id,
            examId = doc.getString("examId") ?: return null,
            studentId = doc.getString("studentId") ?: return null,
            studentAnswers = answersMap,
            scoreMarks = (doc.getLong("scoreMarks") ?: 0L).toInt(),
            totalMarks = (doc.getLong("totalMarks") ?: 1L).toInt(),
            percentage = (doc.getLong("percentage") ?: 0L).toInt(),
            isPassed = doc.getBoolean("isPassed") ?: false,
            startTimeMs = doc.getLong("startTimeMs") ?: 0L,
            submittedTimeMs = doc.getLong("submittedTimeMs") ?: 0L,
            isAutoSubmittedOnTimeout = doc.getBoolean("isAutoSubmittedOnTimeout") ?: false,
            moduleId = doc.getString("moduleId") ?: ""
        )
    }

    private fun docToCertificate(doc: DocumentSnapshot): CertificateRecord? {
        val certId = doc.getString("certificateId") ?: doc.id
        val status = try {
            CertificateStatus.valueOf(doc.getString("status") ?: CertificateStatus.VALID.name)
        } catch (_: Exception) {
            CertificateStatus.VALID
        }
        return CertificateRecord(
            certificateId = certId,
            studentId = doc.getString("studentId") ?: return null,
            studentName = doc.getString("studentName") ?: return null,
            courseTitle = doc.getString("courseTitle") ?: return null,
            issueDate = doc.getString("issueDate") ?: "",
            issueTimestamp = doc.getLong("issueTimestamp") ?: 0L,
            institutionName = doc.getString("institutionName") ?: "ISCEB",
            officialWebsite = doc.getString("officialWebsite") ?: "www.isceb.org",
            authorizedSigner = doc.getString("authorizedSigner") ?: "",
            status = status,
            revocationReason = doc.getString("revocationReason"),
            revocationTimestamp = doc.getLong("revocationTimestamp"),
            completedClassesCount = (doc.getLong("completedClassesCount") ?: 0L).toInt(),
            totalRequiredClasses = (doc.getLong("totalRequiredClasses") ?: 48L).toInt(),
            studentCode = doc.getString("studentCode") ?: "",
            verificationUrl = doc.getString("verificationUrl") ?: "https://www.isceb.org/verify/$certId"
        )
    }

    private fun docToAuditLog(doc: DocumentSnapshot): AuditLogEntry? {
        val role = try {
            UserRole.valueOf(doc.getString("actorRole") ?: UserRole.ADMIN.name)
        } catch (_: Exception) {
            UserRole.ADMIN
        }
        return AuditLogEntry(
            logId = doc.getString("logId") ?: doc.id,
            actorUid = doc.getString("actorUid") ?: return null,
            actorName = doc.getString("actorName") ?: "ISCEB Staff",
            actorRole = role,
            actionType = doc.getString("actionType") ?: return null,
            targetId = doc.getString("targetId") ?: "",
            summary = doc.getString("summary") ?: "",
            timestampMs = doc.getLong("timestampMs") ?: 0L
        )
    }

    private fun sanitizeFirestoreId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return cleaned.ifBlank { "id_${System.currentTimeMillis()}" }.take(128)
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_ASSIGNMENTS = "assignments"
        const val COLLECTION_SUBMISSIONS = "submissions"
        const val COLLECTION_EXAMS = "exams"
        const val COLLECTION_EXAM_ATTEMPTS = "exam_attempts"
        const val COLLECTION_CERTIFICATES = "certificates"
        const val COLLECTION_AUDIT_LOGS = "audit_logs"
        const val COLLECTION_MANIFESTS = "curriculum_manifests"
        const val COLLECTION_CATALOGS = "curriculum_catalogs"
        const val COLLECTION_LESSON_FILES = "lesson_files"
        const val LIVE_DOC_ID = "live"
        const val MAX_CLOUD_FILE_BYTES = 18L * 1024L * 1024L // 18 MB (48 chunks * 400KB raw <= 50 chunks & 600KB Base64 limit)
        const val CHUNK_RAW_BYTES = 400_000

        private fun defaultNoCacheClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .cache(null)
                .build()
        }
    }
}
