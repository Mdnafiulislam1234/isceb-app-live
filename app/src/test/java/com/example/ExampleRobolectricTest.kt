package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SupplyChainRepository
import com.example.model.CertificateStatus
import com.example.model.ClassStatus
import com.example.model.CoursePart
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

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
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("ISCEB", appName)
    }

    @Test
    fun `verify exact 12 modules and 48 lectures partitioned into Part 1 (28) and Part 2 (20)`() {
        val modules = repository.modules.value
        val classes = repository.classes.value

        assertEquals("Must have exactly 12 modules", 12, modules.size)
        assertEquals("Must have exactly 48 lectures", 48, classes.size)

        // Part 1: Basic (Foundation Level) – 28 Classes (Modules 1–6)
        val part1Modules = modules.filter { it.part == CoursePart.PART_1_BASIC }
        assertEquals(6, part1Modules.size)
        val part1Classes = classes.filter { cls -> part1Modules.any { it.id == cls.moduleId } }
        assertEquals("Part 1 must contain exactly 28 classes", 28, part1Classes.size)

        // Module 1 (5 classes: 01–05)
        val mod1Classes = classes.filter { it.moduleId == "mod-1" }
        assertEquals(5, mod1Classes.size)
        assertEquals("Concept of SCM and Demand Forecasting & Planning", modules[0].title)
        assertEquals("Orientation and Concept of SCM", mod1Classes[0].title)
        assertEquals("01", mod1Classes[0].lectureCode)
        assertEquals("Exam (20 Marks)", mod1Classes[4].title)
        assertEquals("05", mod1Classes[4].lectureCode)

        // Module 2 (5 classes: 06–10)
        val mod2Classes = classes.filter { it.moduleId == "mod-2" }
        assertEquals(5, mod2Classes.size)
        assertEquals("Sourcing & Vendor Management", modules[1].title)
        assertEquals("Assignment & Exam (25 Marks)", mod2Classes[4].title)

        // Module 3 (9 classes: 11–19)
        val mod3Classes = classes.filter { it.moduleId == "mod-3" }
        assertEquals("Module 3 must have exactly 9 classes", 9, mod3Classes.size)
        assertEquals("Procurement (Local & International)", modules[2].title)
        assertEquals("Basic of Procurement", mod3Classes[0].title)
        assertEquals("Assignment & Exam (30 Marks)", mod3Classes[8].title)

        // Module 4 (5 classes: 20–24)
        val mod4Classes = classes.filter { it.moduleId == "mod-4" }
        assertEquals("Module 4 must have exactly 5 classes", 5, mod4Classes.size)
        assertEquals("Inventory, Warehouse, Distribution & Logistics", modules[3].title)

        // Module 5 (2 classes: 25–26)
        val mod5Classes = classes.filter { it.moduleId == "mod-5" }
        assertEquals("Module 5 must have exactly 2 classes", 2, mod5Classes.size)
        assertEquals("Billing Management & CRM", modules[4].title)

        // Module 6 (2 classes: 27–28)
        val mod6Classes = classes.filter { it.moduleId == "mod-6" }
        assertEquals("Module 6 must have exactly 2 classes", 2, mod6Classes.size)
        assertEquals("SOP & QMS", modules[5].title)
        assertEquals("SOP & QMS", mod6Classes[0].title)
        assertEquals("Exam (25 Marks)", mod6Classes[1].title)
        assertEquals("28", mod6Classes[1].lectureCode)

        // Part 2: Advance (Strategic & Analytical Level) – 20 Classes (Modules 7–12)
        val part2Modules = modules.filter { it.part == CoursePart.PART_2_ADVANCE }
        assertEquals(6, part2Modules.size)
        val part2Classes = classes.filter { cls -> part2Modules.any { it.id == cls.moduleId } }
        assertEquals("Part 2 must contain exactly 20 classes", 20, part2Classes.size)

        assertEquals("Introduction to Supply Chain Management", modules[6].title)
        assertEquals("Procurement & Sourcing Management", modules[7].title)
        assertEquals("Inventory, Warehouse & Operations Management", modules[8].title)
        assertEquals("International Trade & Supply Chain Finance", modules[9].title)
        assertEquals("Supply Chain Planning, Strategy & Technology", modules[10].title)
        assertEquals("Sustainable Supply Chain & Project Work", modules[11].title)

        assertEquals(3, classes.filter { it.moduleId == "mod-7" }.size)
        assertEquals(3, classes.filter { it.moduleId == "mod-8" }.size)
        assertEquals(3, classes.filter { it.moduleId == "mod-9" }.size)
        assertEquals(6, classes.filter { it.moduleId == "mod-10" }.size)
        assertEquals(2, classes.filter { it.moduleId == "mod-11" }.size)
        val mod12Classes = classes.filter { it.moduleId == "mod-12" }
        assertEquals(3, mod12Classes.size)
        assertEquals("Final Exam (100 Marks)", mod12Classes[2].title)
        assertEquals("48", mod12Classes[2].lectureCode)
    }

    @Test
    fun `verify explicit syllabus assessment marks and schedules`() {
        val exams = repository.moduleExams.value
        assertNotNull(exams)

        // Module 1 exam: 20 marks explicit in syllabus
        val mod1Exam = exams.find { it.id == "exam-mod-1" }
        assertNotNull(mod1Exam)
        assertEquals(20, mod1Exam!!.totalMarks)
        assertTrue(mod1Exam.isSyllabusFixedMarks)

        // Module 2 exam: 25 marks explicit in syllabus
        val mod2Exam = exams.find { it.id == "exam-mod-2" }
        assertNotNull(mod2Exam)
        assertEquals(25, mod2Exam!!.totalMarks)
        assertTrue(mod2Exam.isSyllabusFixedMarks)

        // Module 3 exam: 30 marks explicit in syllabus
        val mod3Exam = exams.find { it.id == "exam-mod-3" }
        assertNotNull(mod3Exam)
        assertEquals(30, mod3Exam!!.totalMarks)
        assertTrue(mod3Exam.isSyllabusFixedMarks)

        // Module 6 exam: 25 marks explicit in syllabus
        val mod6Exam = exams.find { it.id == "exam-mod-6" }
        assertNotNull(mod6Exam)
        assertEquals(25, mod6Exam!!.totalMarks)
        assertTrue(mod6Exam.isSyllabusFixedMarks)

        // Final Comprehensive Exam: 100 marks explicit in syllabus
        val finalExam = exams.find { it.id == "exam-final" }
        assertNotNull(finalExam)
        assertEquals(100, finalExam!!.totalMarks)
        assertTrue(finalExam.isSyllabusFixedMarks)

        // Configurable module (e.g. Module 4)
        val mod4Exam = exams.find { it.id == "exam-mod-4" }
        assertNotNull(mod4Exam)
        assertFalse(mod4Exam!!.isSyllabusFixedMarks)

        // Institution & Schedule settings
        val settings = repository.adminSettings.value
        assertEquals("ISCEB", settings.institutionName)
        assertEquals("www.isceb.org", settings.officialWebsite)
        assertEquals("9:00 PM to 11:00 PM", settings.scheduleConfig.classTimeWindow)
        assertEquals("Asia/Dhaka", settings.scheduleConfig.timeZoneId)
        assertEquals(24, settings.scheduleConfig.courseDurationWeeks)
        assertEquals(6, settings.scheduleConfig.courseDurationMonths)
        assertEquals(2, settings.scheduleConfig.classesPerWeek)
    }

    @Test
    fun `test interactive exam scoring and auto-submit timeout`() {
        val mod2Exam = repository.moduleExams.value.find { it.id == "exam-mod-2" }!!
        val answers = mod2Exam.questions.associate { it.id to it.correctOptionIndex }

        // Perfect score attempt
        val attempt = repository.submitExamAttempt(mod2Exam.id, answers, isAutoSubmittedOnTimeout = false)
        assertEquals(25, attempt.scoreMarks)
        assertEquals(25, attempt.totalMarks)
        assertEquals(100, attempt.percentage)
        assertTrue(attempt.isPassed)
        assertFalse(attempt.isAutoSubmittedOnTimeout)

        // Timeout auto-submit attempt
        val timeoutAnswers = mod2Exam.questions.take(1).associate { it.id to it.correctOptionIndex }
        val timeoutAttempt = repository.submitExamAttempt(mod2Exam.id, timeoutAnswers, isAutoSubmittedOnTimeout = true)
        assertTrue(timeoutAttempt.isAutoSubmittedOnTimeout)
        assertEquals(5, timeoutAttempt.scoreMarks)
    }

    @Test
    fun `test assignment submission and grading workflow`() {
        val assignment = repository.moduleAssignments.value.find { it.id == "asg-1" }!!
        val submission = repository.submitAssignment(
            assignmentId = assignment.id,
            submissionText = "3-month Moving average forecast: Month 4 demand = 4,200 units. PR generated for 4,500 units.",
            attachedFileName = "FMCG_Demand_Report.pdf"
        )

        assertNotNull(submission)
        assertEquals("student-1", submission.studentId)
        assertFalse(submission.isGraded)

        // Faculty grading
        repository.gradeAssignment(submission.submissionId, 19, "Excellent quantitative analysis and clear PR conversion.")
        val updatedSub = repository.getStudentAssignmentSubmission(assignment.id)
        assertNotNull(updatedSub)
        assertTrue(updatedSub!!.isGraded)
        assertEquals(19, updatedSub.marksAwarded)
    }

    @Test
    fun `test active learning time and paused playback logic`() {
        val classId = "class-1"
        assertEquals(0L, repository.studentProgressMap.value[classId]?.activeLearningSeconds ?: 0L)

        // Paused playback (0 seconds) does not advance learning time
        repository.recordActiveLearningTime(classId, 0L, playbackPercent = 0f)
        assertEquals(0L, repository.studentProgressMap.value[classId]?.activeLearningSeconds ?: 0L)

        // Active playback (30s)
        repository.recordActiveLearningTime(classId, 30L, playbackPercent = 40f)
        val progressAfterPlay = repository.studentProgressMap.value[classId]
        assertNotNull(progressAfterPlay)
        assertEquals(30L, progressAfterPlay!!.activeLearningSeconds)
        assertEquals(40f, progressAfterPlay.maxPlaybackPercent, 0.01f)
        assertEquals(ClassStatus.IN_PROGRESS, progressAfterPlay.status)
    }

    @Test
    fun `test authoritative completion rules enforcement`() {
        val classId = "class-1"
        val settings = repository.adminSettings.value
        assertEquals(60L, settings.minLearningTimeSeconds)
        assertTrue(settings.isQuizRequired)

        // Active time only 30s (< 60s) -> should NOT be completed
        repository.recordActiveLearningTime(classId, 30L, 85f)
        var isCompleted = repository.evaluateAndRecordClassCompletion(classId)
        assertFalse(isCompleted)

        // Reaching 65s, but quiz not passed yet -> should NOT be completed
        repository.recordActiveLearningTime(classId, 35L, 85f)
        isCompleted = repository.evaluateAndRecordClassCompletion(classId)
        assertFalse(isCompleted)

        // Failed quiz (50%) -> should NOT be completed
        repository.submitQuiz(classId, 50)
        isCompleted = repository.evaluateAndRecordClassCompletion(classId)
        assertFalse(isCompleted)

        // Passed quiz (100%) -> should BE COMPLETED!
        val passed = repository.submitQuiz(classId, 100)
        assertTrue(passed)

        val progress = repository.studentProgressMap.value[classId]
        assertNotNull(progress)
        assertEquals(ClassStatus.COMPLETED, progress!!.status)
        assertNotNull(progress.completionTimestamp)
        assertTrue(repository.classes.value.find { it.id == classId }!!.isCompleted)
    }

    @Test
    fun `test multi-user progress isolation across logout and switch`() {
        assertEquals("student-1", repository.currentUser.value.id)
        repository.recordActiveLearningTime("class-1", 70L, 100f)
        repository.submitQuiz("class-1", 100)
        assertTrue(repository.classes.value.find { it.id == "class-1" }!!.isCompleted)

        // Switch to User 2
        repository.switchUser("student-2")
        assertEquals("student-2", repository.currentUser.value.id)

        val alexClass1 = repository.classes.value.find { it.id == "class-1" }!!
        assertFalse("Alex's class should not be completed", alexClass1.isCompleted)
        assertEquals(ClassStatus.NOT_STARTED, alexClass1.status)

        // Switch back to User 1: progress restored
        repository.switchUser("student-1")
        assertEquals("student-1", repository.currentUser.value.id)
        val sarahClass1 = repository.classes.value.find { it.id == "class-1" }!!
        assertTrue("Sarah's completed class must be restored", sarahClass1.isCompleted)
        assertEquals(ClassStatus.COMPLETED, sarahClass1.status)
    }

    @Test
    fun `test certificate generation for SCM Global PGD and verification`() {
        // Initially ineligible
        val eligibilityInitial = repository.evaluateCertificateEligibility()
        assertFalse("Incomplete student should not be eligible", eligibilityInitial.isEligible)

        // Fast-track student to 100% completion (all 48 classes, mandatory exams, and capstone project)
        repository.fastTrackCompleteAllClasses()
        val eligibilityComplete = repository.evaluateCertificateEligibility()
        assertTrue("Fully completed student must be eligible", eligibilityComplete.isEligible)
        assertEquals(48, eligibilityComplete.completedRequiredCount)

        // Generate Certificate
        val cert = repository.generateCertificateForStudent()
        assertNotNull(cert)
        assertTrue(cert!!.certificateId.startsWith("ISCEB-SCM-"))
        assertEquals("ISCEB", cert.institutionName)
        assertEquals("www.isceb.org", cert.officialWebsite)
        assertEquals(CertificateStatus.VALID, cert.status)
        assertNotNull(cert.filePath)
        assertTrue(File(cert.filePath!!).exists())

        // Verify by Certificate ID
        val verifiedCert = repository.verifyCertificateById(cert.certificateId)
        assertNotNull(verifiedCert)
        assertEquals(CertificateStatus.VALID, verifiedCert!!.status)
        assertEquals(cert.certificateId, verifiedCert.certificateId)

        // Revocation by Admin
        val revoked = repository.revokeCertificate(cert.certificateId, "Audited non-compliance finding")
        assertTrue(revoked)

        val verifiedRevoked = repository.verifyCertificateById(cert.certificateId)
        assertNotNull(verifiedRevoked)
        assertEquals(CertificateStatus.REVOKED, verifiedRevoked!!.status)
        assertEquals("Audited non-compliance finding", verifiedRevoked.revocationReason)
    }

    @Test
    fun `verify dual administrators Md Nafiul Islam Nahin and Md Amirul Islam`() {
        val admin1 = repository.availableUsers.find { it.id == "admin-1" }
        assertNotNull(admin1)
        assertEquals("Md. Nafiul Islam Nahin", admin1!!.fullName)
        assertEquals("www.mdnafiulislam@gmail.com", admin1.email)
        assertEquals("Developer of ISCEB App", admin1.designation)

        val admin2 = repository.availableUsers.find { it.id == "admin-2" }
        assertNotNull(admin2)
        assertEquals("Md. Amirul Islam", admin2!!.fullName)
        assertEquals(com.example.model.UserRole.ADMIN, admin2.role)
        assertEquals("Founder of ISCEB", admin2.designation)
        assertEquals("Head of Supply Chain Management at BRB Group", admin2.organization)
        assertEquals("Lead Trainer of ISCEB", admin2.additionalRole)
        assertEquals("01715-282905", admin2.mobileNumber)

        // Hardened security: switchUser MUST strictly reject admin account switching
        assertFalse(repository.switchUser("admin-1"))
        assertFalse(repository.switchUser("admin-2"))
        assertFalse(repository.isAdminLoggedIn.value)
        assertEquals("student-1", repository.currentUser.value.id)

        // Proper per-admin cryptographic setup & verification: Amirul 1st, then single-use invite for Nafiul
        val setupAmirul = repository.adminAuthManager.setupInitialPassword(
            adminId = "admin-2",
            newPassword = "BrbExecutive#91",
            confirmPassword = "BrbExecutive#91"
        )
        assertTrue(setupAmirul is com.example.auth.AdminAuthResult.Authenticated)
        assertNotNull(repository.activateVerifiedAdminSession("admin-2"))
        assertEquals("Md. Amirul Islam", repository.currentUser.value.fullName)
        assertTrue(repository.isAdminLoggedIn.value)

        // Generate invite for co-admin Nafiul
        val inviteResult = repository.adminAuthManager.generateSecondAdminSetupInvite("admin-1")
        assertTrue(inviteResult is com.example.auth.AdminAuthResult.SetupInviteGenerated)
        val token = (inviteResult as com.example.auth.AdminAuthResult.SetupInviteGenerated).setupToken

        val setupNafiul = repository.adminAuthManager.setupInitialPassword(
            adminId = "admin-1",
            newPassword = "DevGuard!842",
            confirmPassword = "DevGuard!842",
            setupInviteToken = token
        )
        assertTrue(setupNafiul is com.example.auth.AdminAuthResult.Authenticated)
        assertNotNull(repository.activateVerifiedAdminSession("admin-1"))
        assertEquals("Md. Nafiul Islam Nahin", repository.currentUser.value.fullName)
        assertTrue(repository.isAdminLoggedIn.value)
    }

    @Test
    fun `verify multi-format class media attachments and persistence across repository reload`() {
        val testPdf = com.example.model.LessonMedia(
            id = "att-pdf-1",
            type = com.example.model.MediaType.PDF,
            fileName = "Demand_Forecasting_Framework.pdf",
            fileSizeFormatted = "3.5 MB",
            fileSizeBytes = 3670016L,
            url = "/data/user/0/com.example/files/uploaded_media/class-1_pdf_test.pdf",
            title = "Demand Forecasting Guide",
            description = "Core PDF handbook"
        )
        val testImage = com.example.model.LessonMedia(
            id = "att-img-1",
            type = com.example.model.MediaType.IMAGE,
            fileName = "SCOR_Framework_Diagram.png",
            fileSizeFormatted = "1.1 MB",
            fileSizeBytes = 1153433L,
            url = "/data/user/0/com.example/files/uploaded_media/class-1_img_test.png",
            title = "SCOR Architecture Diagram",
            description = "Visual chart"
        )
        val testDoc = com.example.model.LessonMedia(
            id = "att-doc-1",
            type = com.example.model.MediaType.DOCUMENT,
            fileName = "PR_SR_Template.xlsx",
            fileSizeFormatted = "540 KB",
            fileSizeBytes = 552960L,
            url = "/data/user/0/com.example/files/uploaded_media/class-1_doc_test.xlsx",
            title = "PR & SR Worksheet",
            description = "Editable Excel template"
        )
        val testAudio = com.example.model.LessonMedia(
            id = "att-aud-1",
            type = com.example.model.MediaType.AUDIO,
            fileName = "Demand_Forecasting_Lecture.mp3",
            fileSizeFormatted = "14.2 MB",
            fileSizeBytes = 14889779L,
            url = "/data/user/0/com.example/files/uploaded_media/class-1_audio_test.mp3",
            durationFormatted = "18:24"
        )

        repository.attachFilesToClass("class-1", listOf(testPdf, testImage, testDoc, testAudio))

        val updatedClass = repository.classes.value.find { it.id == "class-1" }
        assertNotNull(updatedClass)
        assertEquals(4, updatedClass!!.allAttachments.size)
        assertEquals("Demand_Forecasting_Framework.pdf", updatedClass.pdfMedia?.fileName)
        assertEquals("Demand_Forecasting_Lecture.mp3", updatedClass.audioMedia?.fileName)

        // Reload repository from SharedPreferences
        val reloadedRepository = SupplyChainRepository(context)
        val reloadedClass = reloadedRepository.classes.value.find { it.id == "class-1" }
        assertNotNull(reloadedClass)
        assertEquals(4, reloadedClass!!.allAttachments.size)
        assertTrue(reloadedClass.allAttachments.any { it.type == com.example.model.MediaType.IMAGE && it.title == "SCOR Architecture Diagram" })
        assertTrue(reloadedClass.allAttachments.any { it.type == com.example.model.MediaType.DOCUMENT && it.fileName == "PR_SR_Template.xlsx" })
    }

    @Test
    fun `verify developer profile defaults strict role separation and persistence`() {
        val defaultProfile = repository.developerProfile.value
        assertEquals("Md. Nafiul Islam Nahin", defaultProfile.name)
        assertEquals("Developer of ISCEB App", defaultProfile.designation)
        assertFalse(defaultProfile.designation.contains("Founder", ignoreCase = true))
        assertFalse(defaultProfile.biography.contains("Founder", ignoreCase = true))
        assertEquals("www.mdnafiulislam@gmail.com", defaultProfile.contactEmail)

        val edited = defaultProfile.copy(
            designation = "Founder & Developer of ISCEB",
            biography = "Updated Mobile Systems Engineer & Developer of ISCEB App.",
            workExperience = "Built full offline-first Android app & Firestore sync.",
            contactPhone = "+880 1700-000000"
        )
        repository.updateDeveloperProfile(edited)

        val reloadedRepository = SupplyChainRepository(context)
        assertEquals("Developer of ISCEB App", reloadedRepository.developerProfile.value.designation)
        assertEquals("Updated Mobile Systems Engineer & Developer of ISCEB App.", reloadedRepository.developerProfile.value.biography)
        assertEquals("Built full offline-first Android app & Firestore sync.", reloadedRepository.developerProfile.value.workExperience)
        assertEquals("+880 1700-000000", reloadedRepository.developerProfile.value.contactPhone)
    }

    @Test
    fun `verify founder profile defaults and persistence for Md Amirul Islam`() {
        val founder = repository.founderProfile.value
        assertEquals("Md. Amirul Islam", founder.name)
        assertEquals("Founder of ISCEB", founder.designation)
        assertEquals("Head of Supply Chain Management at BRB Group", founder.professionalIdentity)
        assertEquals("Lead Trainer of ISCEB", founder.additionalRole)
        assertEquals("01715-282905", founder.mobileNumber)

        val updatedFounder = founder.copy(
            educationBackground = "Supply Chain Leadership Certification",
            biography = "Developing ethical and strategic Supply Chain leaders."
        )
        repository.updateFounderProfile(updatedFounder)

        val reloadedRepository = SupplyChainRepository(context)
        assertEquals("Supply Chain Leadership Certification", reloadedRepository.founderProfile.value.educationBackground)
        assertEquals("Developing ethical and strategic Supply Chain leaders.", reloadedRepository.founderProfile.value.biography)
    }

    @Test
    fun `verify ISCEB members directory zero fabrication initial state and full CRUD with reorder`() {
        val initialMembers = repository.iscebMembers.value
        assertEquals(1, initialMembers.size)
        val founderEntry = initialMembers.first()
        assertEquals("Md. Amirul Islam", founderEntry.fullName)
        assertEquals("Founder of ISCEB", founderEntry.designation)
        assertEquals("Head of Supply Chain Management at BRB Group", founderEntry.professionalIdentity)
        assertEquals("BRB Group", founderEntry.currentOrganization)
        assertEquals("", founderEntry.educationalQualification)
        assertEquals("amirul", founderEntry.builtInPhotoKey)

        // Admin adds a new member with complete 11-field record
        val newMember = com.example.model.IscebMember(
            id = "member-custom-1",
            fullName = "Md. Tariqul Islam",
            designation = "Senior Trainer",
            professionalIdentity = "Supply Chain Logistics Lead",
            currentOrganization = "Apex Logistics Ltd.",
            educationalQualification = "MBA in Supply Chain Management",
            professionalExperience = "12 years in Port & Freight Operations",
            skillsAndExpertise = listOf("Logistics", "Incoterms", "Customs"),
            roleInIsceb = "Faculty Member",
            contactInformation = "tariqul@example.org",
            biography = "Seasoned international trade and port logistics practitioner.",
            displayOrder = 2
        )
        repository.addOrUpdateMember(newMember)
        assertEquals(2, repository.iscebMembers.value.size)

        // Move custom member up
        repository.moveMemberOrder("member-custom-1", moveUp = true)
        val reordered = repository.iscebMembers.value.sortedBy { it.displayOrder }
        assertEquals("member-custom-1", reordered.first().id)

        // Persist across reload
        val reloaded = SupplyChainRepository(context)
        assertEquals(2, reloaded.iscebMembers.value.size)
        assertEquals("Md. Tariqul Islam", reloaded.iscebMembers.value.first().fullName)

        // Delete member
        reloaded.deleteMember("member-custom-1")
        assertEquals(1, reloaded.iscebMembers.value.size)
        assertEquals("Md. Amirul Islam", reloaded.iscebMembers.value.first().fullName)
    }

    @Test
    fun `verify official ISCEB logo and launcher resources exist and are valid`() {
        // Verify logo drawable is available in resources
        val logoDrawable = context.resources.getDrawable(R.drawable.img_isceb_logo, null)
        assertNotNull("ISCEB logo drawable must be loadable", logoDrawable)
        assertTrue("ISCEB logo must have valid intrinsic dimensions", logoDrawable.intrinsicWidth > 0 && logoDrawable.intrinsicHeight > 0)

        // Verify aspect ratio is approximately 4:3 (1200x896)
        val aspect = logoDrawable.intrinsicWidth.toFloat() / logoDrawable.intrinsicHeight.toFloat()
        assertEquals(1200f / 896f, aspect, 0.05f)

        // Verify launcher icon drawable
        val launcherDrawable = context.resources.getDrawable(R.mipmap.ic_launcher, null)
        assertNotNull("Launcher icon must be loadable", launcherDrawable)
    }

    @Test
    fun `verify admin and founder portrait resources exist and designations contain no typo`() {
        val adminUsers = repository.availableUsers.filter { it.role == com.example.model.UserRole.ADMIN }
        assertEquals(2, adminUsers.size)
        assertFalse(adminUsers.any { it.designation.contains("Detector", ignoreCase = true) })
        assertFalse(adminUsers.any { it.id == "admin-1" && it.designation.contains("Founder", ignoreCase = true) })

        val nafiulPortrait = context.resources.getDrawable(R.drawable.img_admin_nafiul, null)
        assertNotNull("Developer portrait photo must be loadable", nafiulPortrait)
        assertTrue("Developer photo must have valid dimensions", nafiulPortrait.intrinsicWidth > 0 && nafiulPortrait.intrinsicHeight > 0)

        val amirulPortrait = context.resources.getDrawable(R.drawable.img_admin_amirul, null)
        assertNotNull("Founder portrait photo must be loadable", amirulPortrait)
        assertTrue("Founder photo must have valid dimensions", amirulPortrait.intrinsicWidth > 0 && amirulPortrait.intrinsicHeight > 0)
    }

    @Test
    fun `verify cloud OTA curriculum publishing and live sync workflow`() {
        // Initial baseline manifest
        val initialManifest = repository.publishManifest.value
        assertEquals("v1.3.0", initialManifest.versionTag)
        assertEquals("Md. Nafiul Islam Nahin (Developer of ISCEB App)", initialManifest.publishedBy)

        // Admin publishes updated curriculum version
        val updatedManifest = repository.adminPublishCurriculumUpdate(
            newVersionTag = "v1.4.0",
            changeNotes = "Added new Case Study and updated exam timings for Friday classes",
            announcement = "Next Exam scheduled Friday 9:00 PM."
        )

        assertEquals("v1.4.0", updatedManifest.versionTag)
        assertEquals(103, updatedManifest.versionCode)
        assertEquals(com.example.model.SyncStatus.SYNCED_SUCCESS, repository.syncStatus.value)

        // Verify sync retains student records
        repository.recordActiveLearningTime("class-1", 120L)
        val studentProgressBefore = repository.studentProgressMap.value["class-1"]
        assertNotNull(studentProgressBefore)

        // Trigger sync
        val syncResult = repository.syncCurriculumNow()
        assertTrue(syncResult)

        val studentProgressAfter = repository.studentProgressMap.value["class-1"]
        assertNotNull(studentProgressAfter)
        assertEquals(studentProgressBefore?.activeLearningSeconds, studentProgressAfter?.activeLearningSeconds)
    }
}


