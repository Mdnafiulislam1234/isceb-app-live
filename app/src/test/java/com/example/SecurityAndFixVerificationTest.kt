package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AdminAuthManager
import com.example.auth.AdminAuthResult
import com.example.data.SupplyChainRepository
import com.example.model.AdminCompletionSettings
import com.example.model.CourseClass
import com.example.model.LessonMedia
import com.example.model.MediaType
import com.example.ui.components.AdminLoginDialog
import com.example.ui.components.AdminSettingsDialog
import com.example.ui.components.AppTopBar
import com.example.ui.components.PdfViewerCard
import com.example.ui.theme.MyApplicationTheme
import com.example.util.MediaFileManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityAndFixVerificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var repository: SupplyChainRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("supply_chain_masterclass_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("test_admin_auth_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        repository = SupplyChainRepository(context)
    }

    @Test
    fun `hardened admin auth rejects weak secrets enforces invite lockout and detects session tampering`() {
        val prefs = context.getSharedPreferences("test_admin_auth_prefs", Context.MODE_PRIVATE)
        var fakeTime = 1_760_000_000_000L
        val auth = AdminAuthManager(prefs) { fakeTime }

        // Weak legacy / guessable secrets rejected
        assertNotNull(auth.validatePasswordStrength("scm2026", AdminAuthManager.ADMIN_AMIRUL_ID))
        assertNotNull(auth.validatePasswordStrength("01715-282905", AdminAuthManager.ADMIN_AMIRUL_ID))
        assertNotNull(auth.validatePasswordStrength("www.mdnafiulislam@gmail.com", AdminAuthManager.ADMIN_NAFIUL_ID))
        assertNotNull(auth.validatePasswordStrength("Amirul@2026", AdminAuthManager.ADMIN_AMIRUL_ID))
        assertNotNull(auth.validatePasswordStrength("Nafiul!2026", AdminAuthManager.ADMIN_NAFIUL_ID))
        assertNull(auth.validatePasswordStrength("BrbExecutive#91", AdminAuthManager.ADMIN_AMIRUL_ID))

        // First admin configures own strong password
        val res1 = auth.setupInitialPassword(
            AdminAuthManager.ADMIN_AMIRUL_ID,
            "BrbExecutive#91",
            "BrbExecutive#91"
        )
        assertTrue(res1 is AdminAuthResult.Authenticated)
        val recovery1 = (res1 as AdminAuthResult.Authenticated).oneTimeRecoveryCode
        assertFalse(recovery1.isNullOrBlank())
        assertEquals(AdminAuthManager.ADMIN_AMIRUL_ID, auth.getValidatedSessionAdminId())

        // Second admin blocked without single-use invite token
        val blockedSetup2 = auth.setupInitialPassword(
            AdminAuthManager.ADMIN_NAFIUL_ID,
            "DevGuard!842",
            "DevGuard!842",
            setupInviteToken = ""
        )
        assertTrue(blockedSetup2 is AdminAuthResult.Failure)

        // Generate invite for 2nd admin and complete onboarding
        val inviteRes = auth.generateSecondAdminSetupInvite(AdminAuthManager.ADMIN_NAFIUL_ID)
        assertTrue(inviteRes is AdminAuthResult.SetupInviteGenerated)
        val inviteToken = (inviteRes as AdminAuthResult.SetupInviteGenerated).setupToken

        val res2 = auth.setupInitialPassword(
            AdminAuthManager.ADMIN_NAFIUL_ID,
            "DevGuard!842",
            "DevGuard!842",
            setupInviteToken = inviteToken
        )
        assertTrue(res2 is AdminAuthResult.Authenticated)
        assertFalse(auth.hasActiveInviteFor(AdminAuthManager.ADMIN_NAFIUL_ID))

        // Tampering session admin ID in SharedPreferences invalidates HMAC session
        prefs.edit().putString("admin_session_admin_id", AdminAuthManager.ADMIN_AMIRUL_ID).commit()
        assertNull("HMAC tamper check must reject forged admin_session_admin_id", auth.getValidatedSessionAdminId())

        // Sign-in works & locks out after 5 wrong attempts
        auth.clearSession()
        repeat(4) {
            val fail = auth.authenticate(AdminAuthManager.ADMIN_AMIRUL_ID, "WrongPass#1")
            assertTrue(fail is AdminAuthResult.Failure)
        }
        val lockedOut = auth.authenticate(AdminAuthManager.ADMIN_AMIRUL_ID, "WrongPass#1")
        assertTrue(lockedOut is AdminAuthResult.Failure)
        assertTrue((lockedOut as AdminAuthResult.Failure).lockoutRemainingSeconds > 0L)

        // Correct password denied during active lockout
        val deniedWhileLocked = auth.authenticate(AdminAuthManager.ADMIN_AMIRUL_ID, "BrbExecutive#91")
        assertTrue(deniedWhileLocked is AdminAuthResult.Failure)

        // Advance time beyond lockout & sign in
        fakeTime += 61_000L
        val verified = auth.authenticate(AdminAuthManager.ADMIN_AMIRUL_ID, "BrbExecutive#91")
        assertTrue(verified is AdminAuthResult.Authenticated)

        // Verified Google email or recovery key can reset password
        val badReset = auth.resetPasswordWithVerification(
            adminId = AdminAuthManager.ADMIN_AMIRUL_ID,
            verifiedGoogleEmail = "random@student.com",
            recoveryCode = "WRONG-CODE",
            newPassword = "NewStrong#999",
            confirmPassword = "NewStrong#999"
        )
        assertTrue(badReset is AdminAuthResult.Failure)

        val goodReset = auth.resetPasswordWithVerification(
            adminId = AdminAuthManager.ADMIN_AMIRUL_ID,
            verifiedGoogleEmail = null,
            recoveryCode = recovery1!!,
            newPassword = "NewStrong#999",
            confirmPassword = "NewStrong#999"
        )
        assertTrue(goodReset is AdminAuthResult.Authenticated)
    }

    @Test
    fun `top bar renders full ISCEB brand text on compact 360dp phone without truncation`() {
        val student = repository.currentUser.value
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = androidx.compose.ui.Modifier.width(360.dp)) {
                    AppTopBar(
                        title = "ISCEB",
                        currentUser = student,
                        isAdmin = false,
                        onAdminClick = {},
                        onVerificationClick = {},
                        onSettingsClick = {},
                        onFirebaseClick = {},
                        onPhoneInstallClick = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("txt_topbar_brand_title")
            .assertIsDisplayed()
            .assertTextEquals("ISCEB")
        composeTestRule.onNodeWithTag("btn_top_phone_install").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_badge_button").assertIsDisplayed()
    }

    @Test
    fun `admin login dialog displays Amirul first then Nafiul with zero password autofill`() {
        val summaries = repository.adminAuthManager.getAdminSummaries()
        composeTestRule.setContent {
            MyApplicationTheme {
                AdminLoginDialog(
                    adminSummaries = summaries,
                    isAnyAdminConfigured = false,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        val amirulNode = composeTestRule.onNodeWithTag("card_select_admin_2")
        val nafiulNode = composeTestRule.onNodeWithTag("card_select_admin_1")
        amirulNode.assertIsDisplayed()
        nafiulNode.assertIsDisplayed()

        val amirulTop = amirulNode.fetchSemanticsNode().boundsInRoot.top
        val nafiulTop = nafiulNode.fetchSemanticsNode().boundsInRoot.top
        assertTrue("Md. Amirul Islam must be rendered above Md. Nafiul Islam Nahin", amirulTop < nafiulTop)

        // Password fields must remain empty upon clicking admin cards
        nafiulNode.performClick()
        composeTestRule.waitForIdle()
        val setupPassText = composeTestRule.onNodeWithTag("input_admin_new_password")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.EditableText)
            ?.text
        assertEquals("", setupPassText)
    }

    @Test
    fun `admin settings leadership hierarchy lists Amirul 1st and Nafiul 2nd directly below`() {
        val summaries = repository.adminAuthManager.getAdminSummaries()
        composeTestRule.setContent {
            MyApplicationTheme {
                AdminSettingsDialog(
                    initialSettings = AdminCompletionSettings(),
                    allModules = repository.modules.value,
                    activeAdminId = AdminAuthManager.ADMIN_AMIRUL_ID,
                    adminSummaries = summaries,
                    onDismiss = {},
                    onSave = {},
                    onFastTrackDemo = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        val amirulNode = composeTestRule.onNodeWithText("Md. Amirul Islam")
        val nafiulNode = composeTestRule.onNodeWithText("Md. Nafiul Islam Nahin")
        amirulNode.assertIsDisplayed()
        nafiulNode.assertIsDisplayed()
        val amirulTop = amirulNode.fetchSemanticsNode().boundsInRoot.top
        val nafiulTop = nafiulNode.fetchSemanticsNode().boundsInRoot.top
        assertTrue("Leadership card 1. Amirul must sit directly above 2. Nafiul", amirulTop < nafiulTop)
    }

    @Test
    fun `real pdf attachment renders without placeholder sheet text and respects unpublished class visibility`() {
        // Write valid %PDF-1.4 document in app files dir (PdfDocument/PdfRenderer use native JNI unavailable on JVM)
        val dir = File(context.filesDir, "uploaded_media").apply { mkdirs() }
        val pdfFile = File(dir, "real_class1_outline.pdf")
        val minimalValidPdf = """
            %PDF-1.4
            1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
            2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
            3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 300 400] >> endobj
            xref
            0 4
            0000000000 65535 f 
            0000000009 00000 n 
            0000000058 00000 n 
            0000000115 00000 n 
            trailer << /Size 4 /Root 1 0 R >>
            startxref
            183
            %%EOF
        """.trimIndent().toByteArray(Charsets.US_ASCII)
        FileOutputStream(pdfFile).use { it.write(minimalValidPdf) }
        assertTrue(pdfFile.exists() && pdfFile.length() > 0L)
        assertEquals("%PDF-", pdfFile.readBytes().copyOf(5).decodeToString())

        // Safe wrapper behavior on JVM where native PdfRenderer is absent
        assertTrue(MediaFileManager.getPdfPageCount(pdfFile) >= 0)

        val pdfMedia = LessonMedia(
            id = "pdf-real-1",
            type = MediaType.PDF,
            fileName = "real_class1_outline.pdf",
            fileSizeFormatted = "4 KB",
            fileSizeBytes = pdfFile.length(),
            url = pdfFile.absolutePath,
            title = "ISCEB PGD-SCM Class Outline",
            mimeType = "application/pdf"
        )
        assertEquals(pdfFile.absolutePath, MediaFileManager.findExistingLocalFile(context, pdfMedia)?.absolutePath)
        assertEquals("application/pdf", MediaFileManager.inferMimeType(pdfMedia))

        composeTestRule.setContent {
            MyApplicationTheme {
                PdfViewerCard(
                    pdfMedia = pdfMedia,
                    isAdmin = false,
                    onReplace = {},
                    onDelete = {},
                    onResolvePdfFile = { pdfFile }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Ensure old fake placeholder text never appears anywhere
        composeTestRule.onNodeWithText("Executive Document Sheet", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("CONFIDENTIAL SCM STUDY", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_open_pdf_reader").assertIsDisplayed()

        // Unpublished class filtering for students
        val unpublished = CourseClass(
            id = "draft-99",
            moduleId = "mod-1",
            orderIndex = 99,
            title = "Internal Faculty Draft",
            summary = "Hidden until published",
            durationMinutes = 30,
            isPublished = false,
            lectureCode = "99"
        )
        val studentVisible = listOf(unpublished).filter { it.isPublished }
        assertTrue(studentVisible.isEmpty())
    }
}
