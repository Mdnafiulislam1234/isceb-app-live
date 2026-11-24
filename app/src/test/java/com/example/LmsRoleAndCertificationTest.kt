package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SupplyChainRepository
import com.example.model.AccountStatus
import com.example.model.CertificateStatus
import com.example.model.CertificationEligibilityStatus
import com.example.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LmsRoleAndCertificationTest {

    private lateinit var context: Context
    private lateinit var repository: SupplyChainRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("supply_chain_masterclass_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        repository = SupplyChainRepository(context)
    }

    @Test
    fun `demo student profiles removed and initial unauthenticated state defaults to guest`() {
        val allNames = repository.availableUsers.map { it.fullName }
        assertFalse(allNames.contains("Sarah Jenkins"))
        assertFalse(allNames.contains("Alex Rivera"))
        assertFalse(allNames.contains("Tanvir Ahmed"))
        assertTrue(repository.availableUsers.all { it.role == UserRole.ADMIN })

        assertEquals("guest-unauthenticated", repository.currentUser.value.id)
        assertFalse(repository.hasSignedInAccount.value)
        assertEquals(0, repository.registeredAccounts.value.size)
    }

    @Test
    fun `student registration generates unique student ID persists profile and supports sign out`() {
        val registered = repository.signInOrRegisterStudentAccount(
            fullName = "Mahmudul Hasan",
            email = "mahmudul@isceb-student.org",
            organization = "Apex Supply Chain Ltd.",
            designation = "Procurement Specialist",
            mobileNumber = "01711-000111"
        )
        assertEquals(UserRole.STUDENT, registered.role)
        assertTrue(registered.studentId.startsWith("ISCEB-PGD-"))
        assertEquals("Mahmudul Hasan", repository.currentUser.value.fullName)
        assertTrue(repository.hasSignedInAccount.value)

        val edited = repository.updateStudentOwnProfile(
            fullName = "Mahmudul Hasan Chowdhury",
            organization = "BRB Cables Supply Chain",
            designation = "Senior Procurement Lead",
            mobileNumber = "01711-000222",
            enrolledProgram = "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)"
        )
        assertNotNull(edited)
        assertEquals("Mahmudul Hasan Chowdhury", edited!!.fullName)
        assertEquals("BRB Cables Supply Chain", repository.currentUser.value.organization)

        // Persist across reload
        val reloaded = SupplyChainRepository(context)
        assertEquals(registered.id, reloaded.currentUser.value.id)
        assertEquals("Mahmudul Hasan Chowdhury", reloaded.currentUser.value.fullName)

        reloaded.signOutAccount()
        assertEquals("guest-unauthenticated", reloaded.currentUser.value.id)
        assertFalse(reloaded.hasSignedInAccount.value)
    }

    @Test
    fun `teacher creation role scoping and suspension behavior`() {
        val teacher = repository.createOrUpdateTeacherAccount(
            fullName = "Prof. Tariqul Islam",
            email = "tariqul@isceb.org",
            designation = "Senior Logistics Instructor",
            organization = "ISCEB Faculty",
            assignedModuleIds = setOf("mod-1", "mod-2")
        )
        assertEquals(UserRole.TEACHER, teacher.role)
        assertEquals(AccountStatus.ACTIVE, teacher.accountStatus)
        assertTrue(teacher.assignedModuleIds.contains("mod-1"))
        assertFalse(teacher.assignedModuleIds.contains("mod-4"))

        assertTrue(repository.activateTeacherSession(teacher.id))
        assertEquals(teacher.id, repository.currentUser.value.id)

        // Suspending currently active user automatically signs out session
        val suspended = repository.adminUpdateUserAccess(
            userId = teacher.id,
            role = UserRole.TEACHER,
            accountStatus = AccountStatus.SUSPENDED,
            isVerified = true,
            assignedModuleIds = teacher.assignedModuleIds
        )
        assertNotNull(suspended)
        assertEquals(AccountStatus.SUSPENDED, suspended!!.accountStatus)
        assertEquals("guest-unauthenticated", repository.currentUser.value.id)
        assertFalse(repository.activateTeacherSession(teacher.id))
    }

    @Test
    fun `pdf page telemetry separates active reading from idle time and enforces study gate`() {
        repository.signInOrRegisterStudentAccount("Tasnia Rahman", "tasnia@example.com")
        repository.recordPdfPageStudy("class-1", currentPage = 1, totalPages = 5, secondsReading = 20L, idleExcludedSecondsToAdd = 15L)
        repository.recordPdfPageStudy("class-1", currentPage = 3, totalPages = 5, secondsReading = 30L, explicitConfirm = true)

        val progress = repository.studentProgressMap.value["class-1"]
        assertNotNull(progress)
        assertEquals(3, progress!!.lastPdfPage)
        assertEquals(5, progress.totalPdfPages)
        assertTrue(progress.visitedPdfPages.containsAll(setOf(1, 3)))
        assertEquals(50L, progress.pdfReadingSeconds)
        assertEquals(15L, progress.idleExcludedSeconds)
        assertTrue(progress.isPdfConfirmed)
        assertTrue(progress.activeLearningSeconds >= 50L)
    }

    @Test
    fun `exam max attempts enforcement and graded assignment modification lock`() {
        repository.signInOrRegisterStudentAccount("Rafiqul Islam", "rafiqul@example.com")
        val exam = repository.moduleExams.value.first { it.id == "exam-mod-1" }
        val updatedExam = repository.createOrUpdateExam(exam.copy(maxAttempts = 2))

        val (canFirst, _) = repository.canStudentStartExam(updatedExam.id)
        assertTrue(canFirst)

        val partialAnswers = updatedExam.questions.take(1).associate { it.id to it.correctOptionIndex }
        repository.submitExamAttempt(updatedExam.id, partialAnswers, false)
        assertEquals(1, repository.getStudentExamAttemptsCount(updatedExam.id))
        assertTrue(repository.canStudentStartExam(updatedExam.id).first)

        repository.submitExamAttempt(updatedExam.id, partialAnswers, false)
        assertEquals(2, repository.getStudentExamAttemptsCount(updatedExam.id))
        val (canThird, reason) = repository.canStudentStartExam(updatedExam.id)
        assertFalse("Third attempt must be blocked after maxAttempts=2", canThird)
        assertNotNull(reason)

        // Assignment graded lock check
        val sub = repository.submitAssignment("asg-1", "Initial supply chain report draft", "draft.pdf")
        repository.gradeAssignment(sub.submissionId, 18, "Well structured", graderUid = "tch-1", graderName = "Prof. Tariqul")
        val attemptOverwrite = repository.submitAssignment("asg-1", "Trying to overwrite after grading", "hacked.pdf")
        assertEquals("Initial supply chain report draft", attemptOverwrite.submissionText)
        assertTrue(attemptOverwrite.isGraded)
    }

    @Test
    fun `admin certificate reissue revoke reinstate and audit log trail`() {
        val student = repository.signInOrRegisterStudentAccount("Farhan Sadik", "farhan@example.com")
        assertEquals(CertificationEligibilityStatus.NOT_YET_ELIGIBLE, repository.evaluateCertificateEligibility().status)

        repository.fastTrackCompleteAllClasses()
        val eligible = repository.evaluateCertificateEligibility()
        assertTrue(eligible.isEligible)

        val issued = repository.generateCertificateForStudent()
        assertNotNull(issued)
        assertEquals(CertificationEligibilityStatus.CERTIFICATE_ISSUED, repository.evaluateCertificateEligibility().status)

        // Revoke -> reinstate -> reissue
        assertTrue(repository.revokeCertificate(issued!!.certificateId, "Audit review hold"))
        assertEquals(CertificateStatus.REVOKED, repository.verifyCertificateById(issued.certificateId)?.status)

        assertTrue(repository.reinstateCertificate(issued.certificateId))
        assertEquals(CertificateStatus.VALID, repository.verifyCertificateById(issued.certificateId)?.status)

        val reissued = repository.reissueCertificateForStudent(student.id, "Name spelling correction")
        assertNotNull(reissued)
        assertTrue(reissued!!.certificateId.startsWith("ISCEB-SCM-"))

        val log = repository.recordAuditLog("CERTIFICATE_REISSUED", reissued.certificateId, "Re-issued certificate")
        assertTrue(repository.auditLogs.value.any { it.logId == log.logId })
    }

    @Test
    fun `admin creates timed MCQ exam and deadlined assignment with time-up enforcement and catalog sync`() {
        val customQuestions = listOf(
            com.example.model.ExamQuestion(
                id = "q-cust-1",
                question = "Which metric measures on-time in-full supplier delivery?",
                options = listOf("OTIF", "EOQ", "LIFO", "ROP"),
                correctOptionIndex = 0,
                marks = 10,
                explanation = "OTIF stands for On-Time In-Full delivery."
            ),
            com.example.model.ExamQuestion(
                id = "q-cust-2",
                question = "Safety stock protects supply chains primarily against:",
                options = listOf("Fixed rent", "Demand & lead-time variability", "Office lighting", "Brand logos"),
                correctOptionIndex = 1,
                marks = 10,
                explanation = "Safety stock buffers uncertainty in demand and lead time."
            )
        )
        val futureCutoff = System.currentTimeMillis() + 3_600_000L
        val pastCutoff = System.currentTimeMillis() - 60_000L

        val customExam = repository.createOrUpdateExam(
            com.example.model.ModuleExam(
                id = "exam-custom-timed",
                moduleId = "mod-4",
                title = "Module 4 Custom Timed Exam",
                durationMinutes = 15,
                passingPercentage = 60,
                totalMarks = 20,
                questions = customQuestions,
                isSyllabusFixedMarks = false,
                openUntilMs = pastCutoff
            )
        )
        assertEquals(20, customExam.totalMarks)
        assertEquals(2, customExam.questions.size)

        repository.signInOrRegisterStudentAccount("Nabila Chowdhury", "nabila@example.com")
        val (canStartExpiredExam, examReason) = repository.canStudentStartExam(customExam.id)
        assertFalse("Expired exam cutoff must block starting exam", canStartExpiredExam)
        assertNotNull(examReason)

        // Extend exam window into the future -> student can start & timeout auto-submit records score
        repository.createOrUpdateExam(customExam.copy(openUntilMs = futureCutoff))
        assertTrue(repository.canStudentStartExam(customExam.id).first)
        val timeoutAttempt = repository.submitExamAttempt(
            examId = customExam.id,
            studentAnswers = mapOf("q-cust-1" to 0),
            isAutoSubmittedOnTimeout = true
        )
        assertTrue(timeoutAttempt.isAutoSubmittedOnTimeout)
        assertEquals(10, timeoutAttempt.scoreMarks)

        // Assignment deadline enforcement
        val timedAssignment = repository.createOrUpdateAssignment(
            com.example.model.ModuleAssignment(
                id = "asg-timed-1",
                moduleId = "mod-4",
                title = "Bonded Warehouse Audit Report",
                topicDescription = "Prepare customs compliance checklist.",
                instructions = "Upload summary before deadline.",
                maxMarks = 25,
                dueDateMs = pastCutoff
            )
        )
        val (canSubmitExpired, asgReason) = repository.canStudentSubmitAssignment(timedAssignment.id)
        assertFalse("Expired assignment deadline must block submissions", canSubmitExpired)
        assertTrue(asgReason?.contains("Time Up", ignoreCase = true) == true)

        // Verify cloud catalog serialization round-trip preserves custom exam & deadlined assignment
        val examsJson = repository.getSerializedExams()
        val assignmentsJson = repository.getSerializedAssignments()
        assertTrue(examsJson.contains("exam-custom-timed"))
        assertTrue(assignmentsJson.contains("asg-timed-1"))

        assertTrue(repository.deleteExam(customExam.id))
        assertTrue(repository.deleteAssignment(timedAssignment.id))
    }
}

