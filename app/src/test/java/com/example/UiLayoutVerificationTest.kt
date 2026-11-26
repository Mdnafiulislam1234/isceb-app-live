package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.SupplyChainRepository
import com.example.model.AdminCompletionSettings
import com.example.model.CertificateEligibility
import com.example.model.CertificateRecord
import com.example.model.CertificateStatus
import com.example.model.CertificationEligibilityStatus
import com.example.model.UserProgress
import com.example.ui.components.CertificatePreviewCard
import com.example.ui.screens.ClassDetailScreen
import com.example.ui.screens.ModuleDetailScreen
import com.example.ui.screens.MyLearningScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CertificatePdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class UiLayoutVerificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var repository: SupplyChainRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("supply_chain_masterclass_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        repository = SupplyChainRepository(context)
    }

    private fun assertSingleLineNoClipping(tag: String) {
        val node = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        val action = node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action
        assertNotNull("Expected TextLayoutResult action on node $tag", action)
        action!!.invoke(results)
        assertTrue("Expected non-empty TextLayoutResult for $tag", results.isNotEmpty())
        val layout = results.first()
        assertEquals("Expected single line for $tag", 1, layout.lineCount)
        assertFalse("Expected no vertical overflow for $tag", layout.didOverflowHeight)
        assertTrue("Expected positive bounds for $tag", node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f)
    }

    @Test
    fun `learning section keeps Certificate Eligibility badge and In Progress tab on single lines at 360dp`() {
        val modules = repository.modules.value
        val classes = repository.classes.value
        val progress = UserProgress()
        val eligibility = CertificateEligibility(
            isEligible = false,
            completedRequiredCount = 0,
            totalRequiredCount = 48,
            status = CertificationEligibilityStatus.NOT_YET_ELIGIBLE
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    MyLearningScreen(
                        modules = modules,
                        classes = classes,
                        progress = progress,
                        eligibility = eligibility,
                        issuedCertificates = emptyList(),
                        isAdmin = false,
                        onClassClick = {},
                        onToggleComplete = {},
                        onToggleBookmark = {},
                        onGenerateCertificate = {},
                        onOpenCertificatePdf = {},
                        onVerifyCertificate = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("badge_learning_cert_status", useUnmergedTree = true).assertIsDisplayed()
        assertSingleLineNoClipping("badge_learning_cert_status")

        composeTestRule.onNodeWithText("In Progress (0)", useUnmergedTree = true).assertIsDisplayed()
        val tabNode = composeTestRule.onNodeWithText("In Progress (0)", useUnmergedTree = true).fetchSemanticsNode()
        val tabLayouts = mutableListOf<TextLayoutResult>()
        tabNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(tabLayouts)
        assertEquals("In Progress tab must stay on 1 line", 1, tabLayouts.first().lineCount)
    }

    @Test
    fun `module detail assignment 20 Marks badge renders on single line without wrapping at 360dp`() {
        val mod1 = repository.modules.value.first { it.id == "mod-1" }
        val mod1Classes = repository.classes.value.filter { it.moduleId == mod1.id }
        val mod1Exam = repository.moduleExams.value.first { it.moduleId == mod1.id }
        val mod1Assignment = repository.moduleAssignments.value.first { it.moduleId == mod1.id }.copy(maxMarks = 20)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    ModuleDetailScreen(
                        module = mod1,
                        classes = mod1Classes,
                        exam = mod1Exam,
                        bestAttempt = null,
                        assignment = mod1Assignment,
                        assignmentSubmission = null,
                        isAdmin = false,
                        onBack = {},
                        onClassClick = {},
                        onToggleComplete = {},
                        onToggleBookmark = {},
                        onEditModule = {},
                        onDeleteModule = {},
                        onAddClass = {},
                        onEditClass = {},
                        onDeleteClass = {},
                        onMoveClassUp = {},
                        onMoveClassDown = {},
                        onToggleClassPublish = {},
                        onTakeExam = {},
                        onOpenAssignment = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("badge_assignment_marks_mod-1"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("badge_exam_marks_mod-1", useUnmergedTree = true).assertIsDisplayed()
        assertSingleLineNoClipping("badge_exam_marks_mod-1")

        composeTestRule.onNodeWithTag("badge_assignment_marks_mod-1", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("20 Marks")
        assertSingleLineNoClipping("badge_assignment_marks_mod-1")
    }

    @Test
    fun `admin media upload center displays full banner text and single-line parallel upload options at 360dp`() {
        val mod1 = repository.modules.value.first { it.id == "mod-1" }
        val mod1Classes = repository.classes.value.filter { it.moduleId == mod1.id }
        val cls1 = mod1Classes.first()

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    ClassDetailScreen(
                        courseClass = cls1,
                        module = mod1,
                        allModuleClasses = mod1Classes,
                        studentProgress = null,
                        adminSettings = AdminCompletionSettings(),
                        isAdmin = true,
                        isPlayingAudio = false,
                        audioProgressMs = 0L,
                        audioDurationMs = 0L,
                        audioSpeed = 1f,
                        isPlayingVideo = false,
                        videoPositionMs = 0L,
                        videoDurationMs = 0L,
                        onBack = {},
                        onToggleBookmark = {},
                        onConfirmPdfRead = {},
                        onTakeQuiz = {},
                        onAdminToggleCompletion = {},
                        onPlayPauseAudio = {},
                        onSeekAudio = {},
                        onSkipAudioSeconds = {},
                        onAudioSpeedChange = {},
                        onPlayPauseVideo = {},
                        onSeekVideo = {},
                        onOpenUploadDialog = {},
                        onDeleteMedia = {},
                        onLoadDemoMedia = {},
                        onNavigateClass = {},
                        canManageClass = true
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("btn_upload_opt_video"))
        composeTestRule.waitForIdle()

        val bannerNode = composeTestRule.onNodeWithTag("btn_admin_multi_upload_center", useUnmergedTree = true)
        bannerNode.assertIsDisplayed()
        val bannerContainerBounds = bannerNode.fetchSemanticsNode().boundsInRoot
        val bannerTextNode = composeTestRule.onNodeWithText(
            "Upload multiple files, PDF, image, document, audio, video",
            useUnmergedTree = true
        ).fetchSemanticsNode()
        val bannerTextBounds = bannerTextNode.boundsInRoot
        val bannerLayouts = mutableListOf<TextLayoutResult>()
        bannerTextNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(bannerLayouts)
        assertTrue(bannerLayouts.isNotEmpty())
        assertFalse("Upload banner text must not vertically overflow", bannerLayouts.first().didOverflowHeight)
        assertTrue(
            "Upload banner text must fit inside button without vertical clipping",
            bannerTextBounds.top >= bannerContainerBounds.top - 1f &&
                bannerTextBounds.bottom <= bannerContainerBounds.bottom + 1f
        )

        val expectedOptions = listOf(
            "btn_upload_opt_image" to "Image",
            "btn_upload_opt_document" to "Document",
            "btn_upload_opt_audio" to "Audio",
            "btn_upload_opt_video" to "Video"
        )
        expectedOptions.forEach { (tag, label) ->
            val btnInteraction = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)
            btnInteraction.assertIsDisplayed()
            val btnBounds = btnInteraction.fetchSemanticsNode().boundsInRoot
            val textNode = composeTestRule.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode()
            val textBounds = textNode.boundsInRoot
            val layouts = mutableListOf<TextLayoutResult>()
            textNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            assertEquals("$label button must not wrap vertically across lines", 1, layouts.first().lineCount)
            assertFalse("$label button text must not vertically overflow", layouts.first().didOverflowHeight)
            assertTrue(
                "$label text must sit cleanly inside its button bounds",
                textBounds.left >= btnBounds.left - 1f &&
                    textBounds.right <= btnBounds.right + 1f &&
                    textBounds.top >= btnBounds.top - 1f &&
                    textBounds.bottom <= btnBounds.bottom + 1f
            )
        }

        val imgBounds = composeTestRule.onNodeWithTag("btn_upload_opt_image").fetchSemanticsNode().boundsInRoot
        val docBounds = composeTestRule.onNodeWithTag("btn_upload_opt_document").fetchSemanticsNode().boundsInRoot
        val audioBounds = composeTestRule.onNodeWithTag("btn_upload_opt_audio").fetchSemanticsNode().boundsInRoot
        val videoBounds = composeTestRule.onNodeWithTag("btn_upload_opt_video").fetchSemanticsNode().boundsInRoot
        assertEquals("Image and Document must align on the same horizontal row", imgBounds.top, docBounds.top, 2f)
        assertEquals("Audio and Video must align on the same horizontal row", audioBounds.top, videoBounds.top, 2f)
        assertTrue("Second row must sit cleanly below first row", audioBounds.top > imgBounds.bottom)
    }

    @Test
    fun `learning section renders Demo Certificate controls on narrow 360dp viewport`() {
        val modules = repository.modules.value
        val classes = repository.classes.value
        val eligibility = CertificateEligibility(
            isEligible = false,
            completedRequiredCount = 2,
            totalRequiredCount = 48,
            status = CertificationEligibilityStatus.NOT_YET_ELIGIBLE
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    MyLearningScreen(
                        modules = modules,
                        classes = classes,
                        progress = UserProgress(),
                        eligibility = eligibility,
                        issuedCertificates = emptyList(),
                        isAdmin = true,
                        adminSettings = AdminCompletionSettings(
                            certificatesEnabled = true,
                            demoCertificateEnabled = true
                        ),
                        onClassClick = {},
                        onToggleComplete = {},
                        onToggleBookmark = {},
                        onGenerateCertificate = {},
                        onOpenCertificatePdf = {},
                        onVerifyCertificate = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodes(hasScrollAction())[0]
            .performScrollToNode(hasTestTag("card_demo_certificate"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("btn_preview_demo_certificate", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_download_demo_certificate", useUnmergedTree = true).assertIsDisplayed()
        val demoPreviewText = composeTestRule.onNodeWithText("Demo Preview", useUnmergedTree = true).fetchSemanticsNode()
        val demoPreviewBtn = composeTestRule.onNodeWithTag("btn_preview_demo_certificate", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(
            "Demo Preview text must fit cleanly inside button bounds",
            demoPreviewText.boundsInRoot.left >= demoPreviewBtn.boundsInRoot.left - 1f &&
                demoPreviewText.boundsInRoot.right <= demoPreviewBtn.boundsInRoot.right + 1f
        )
    }

    @Test
    fun `certificate preview card uses official ISCEB full name and Hasina Akter Khan left Md Amirul Islam right signatories`() {
        val demoRecord = CertificateRecord(
            certificateId = CertificatePdfGenerator.DEMO_CERTIFICATE_ID,
            studentId = "student-demo-1",
            studentName = "Tanvir Ahmed",
            courseTitle = "Post Graduate Diploma in Supply Chain Management (PGD in SCM)",
            issueDate = "November 21, 2026",
            issueTimestamp = 1795200000000L,
            institutionName = "ISCEB",
            authorizedSigner = "Md. Amirul Islam, Founder of ISCEB",
            status = CertificateStatus.VALID,
            completedClassesCount = 48,
            totalRequiredClasses = 48,
            studentCode = "ISCEB-DEMO-01"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    CertificatePreviewCard(record = demoRecord)
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("DEMO CERTIFICATE PREVIEW", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_cert_institution_name", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("INSTITUTE OF SUPPLY CHAIN EXCELLENCE BANGLADESH (ISCEB)")

        val leftNode = composeTestRule.onNodeWithTag("cert_left_signatory", useUnmergedTree = true)
        val rightNode = composeTestRule.onNodeWithTag("cert_right_signatory", useUnmergedTree = true)
        leftNode.assertIsDisplayed()
        rightNode.assertIsDisplayed()

        composeTestRule.onNodeWithText("Hasina Akter Khan", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Chairman & CEO", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Md. Amirul Islam", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Founder of ISCEB", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Nafiul", substring = true, useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("International Supply Chain Education Board", substring = true, useUnmergedTree = true)
            .assertDoesNotExist()

        val leftBounds = leftNode.fetchSemanticsNode().boundsInRoot
        val rightBounds = rightNode.fetchSemanticsNode().boundsInRoot
        assertTrue("Left signatory must sit to the left of right signatory without overlap", leftBounds.right <= rightBounds.left)
    }

    @Test
    fun `demo certificate pdf generation creates non-empty PDF with official name and signatories`() {
        val demoRecord = repository.createDemoCertificateRecord()
        assertEquals(CertificatePdfGenerator.DEMO_CERTIFICATE_ID, demoRecord.certificateId)
        val leftSig = CertificatePdfGenerator.resolveLeftSignatory(repository.adminSettings.value)
        assertEquals("Hasina Akter Khan", leftSig.name)
        assertEquals("Chairman & CEO", leftSig.title)
        assertEquals("Md. Amirul Islam", CertificatePdfGenerator.resolveRightSignatory(demoRecord, repository.adminSettings.value).name)
        assertEquals("Founder of ISCEB", CertificatePdfGenerator.resolveRightSignatory(demoRecord, repository.adminSettings.value).title)

        val pdfFile = CertificatePdfGenerator.generateCertificatePdf(context, demoRecord)
        assertTrue("Generated demo PDF must exist", pdfFile.exists())
        assertTrue("Generated demo PDF must be non-empty", pdfFile.length() > 40L)
        val bytes = pdfFile.readBytes()
        val header = String(bytes.copyOfRange(0, minOf(5, bytes.size)))
        assertEquals("%PDF-", header)
    }

    @Test
    fun `unified ISCEB Workspace card and compact TopBar menu button render cleanly at 360dp`() {
        val student = repository.currentUser.value
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    androidx.compose.foundation.layout.Column {
                        com.example.ui.components.AppTopBar(
                            title = "ISCEB",
                            currentUser = student,
                            isAdmin = false,
                            onAdminClick = {},
                            onVerificationClick = {},
                            onSettingsClick = {},
                            onFirebaseClick = {},
                            onPhoneInstallClick = {},
                            onWorkspaceMenuClick = {}
                        )
                        com.example.ui.components.IscebWorkspaceCard(
                            currentTab = com.example.viewmodel.NavTab.HOME,
                            onTabSelected = {},
                            progressPercent = 0,
                            moduleCount = 12,
                            totalClassesCount = 48,
                            examCount = 5,
                            assignmentCount = 4,
                            certificateStatusLabel = "Issued",
                            membersCount = 8,
                            isAdmin = false,
                            isTeacher = false,
                            curriculumVersionTag = "v1.6.0",
                            latestApkLabel = "v1.6 (18.5 MB)",
                            syncStatusLabel = "Local + Web Ready",
                            isCloudSynced = false,
                            onOpenExamsAndAssignments = {},
                            onOpenAdminGovernance = {},
                            onCheckAppUpdateClick = {},
                            onOpenApkPortalClick = {}
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("btn_top_workspace_drawer", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("txt_topbar_brand_title", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_isceb_workspace_hub", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Courses & Lectures", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Exams & Assignments", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("My Learning & Certificate", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("ISCEB Directory & Team", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Admin Governance", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_workspace_check_update", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_workspace_apk_portal", useUnmergedTree = true).assertIsDisplayed()
    }
}
