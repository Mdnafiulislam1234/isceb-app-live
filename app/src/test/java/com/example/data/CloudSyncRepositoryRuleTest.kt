package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.AdminCompletionSettings
import com.example.model.ClassStatus
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.CoursePart
import com.example.model.CurriculumPublishManifest
import com.example.model.StudentClassProgress
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun seedAdminRegistryViaOwnerRest(uid: String, email: String) {
        val urlStr = "http://$EMULATOR_HOST:$FIRESTORE_PORT/v1/projects/$PROJECT_ID/databases/$databaseId/documents/admins/$uid"
        val bodyJson = """
            {
              "fields": {
                "uid": {"stringValue": "$uid"},
                "email": {"stringValue": "$email"},
                "role": {"stringValue": "ADMIN"}
              }
            }
        """.trimIndent()
        val request = Request.Builder()
            .url(urlStr)
            .addHeader("Authorization", "Bearer owner")
            .patch(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        OkHttpClient().newCall(request).execute().use { response ->
            assertTrue("Expected 2xx seeding admin registry, got ${response.code}", response.isSuccessful)
        }
    }

    @Test
    fun syncAndObserveStudentProgress_authenticatedOwner_succeedsAndPreservesData() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@test.com")
        val repository = CloudSyncRepository(firestore, auth)

        val progress = StudentClassProgress(
            classId = "class-1",
            studentId = aliceUid,
            status = ClassStatus.COMPLETED,
            activeLearningSeconds = 120L,
            maxPlaybackPercent = 95f,
            isPdfConfirmed = true,
            pdfReadingSeconds = 60L,
            quizScore = 100,
            quizPassed = true,
            completionTimestamp = System.currentTimeMillis()
        )

        val writeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncStudentClassProgress(progress)
        }
        assertTrue("Authenticated student progress sync must succeed", writeResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProgress(aliceUid).first { it.containsKey("class-1") }
        }
        assertEquals(ClassStatus.COMPLETED, observed["class-1"]?.status)
        assertEquals(120L, observed["class-1"]?.activeLearningSeconds)
        assertEquals(100, observed["class-1"]?.quizScore)
    }

    @Test
    fun observeUserProgress_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = CloudSyncRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserProgress("some_other_uid").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated user")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
        }
    }

    @Test
    fun studentCannotPublishLiveManifest_failsWithPermissionDenied() = runBlocking {
        signInTestUser("student_${UUID.randomUUID().toString().take(6)}@test.com")
        val repository = CloudSyncRepository(firestore, auth)

        val manifest = CurriculumPublishManifest(
            versionCode = 105,
            versionTag = "v1.5.0",
            latestApkVersionCode = 2,
            latestApkVersionName = "1.1"
        )
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.publishLiveCurriculum(
                manifest = manifest,
                modules = listOf(CourseModule("mod-1", 1, "Module 1", "Desc", CoursePart.PART_1_BASIC)),
                classes = listOf(CourseClass("class-1", "mod-1", 1, "Class 1", "Summary")),
                settings = AdminCompletionSettings()
            )
        }
        assertTrue("Non-admin must be rejected by Firestore rules", result.isFailure)
        val firestoreException = result.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun authorizedAdminPublishesAndStudentReceivesLiveManifestAndCatalog() = runBlocking {
        val adminEmail = "admin_${UUID.randomUUID().toString().take(6)}@isceb.org"
        val adminUid = signInTestUser(adminEmail)
        withContext(Dispatchers.IO) {
            seedAdminRegistryViaOwnerRest(adminUid, adminEmail)
        }

        val adminRepo = CloudSyncRepository(firestore, auth)
        val uniqueTag = "v1.${(10..99).random()}.0"
        val manifest = CurriculumPublishManifest(
            versionCode = 110,
            versionTag = uniqueTag,
            releaseNotes = "Live cross-device syllabus sync verification",
            announcement = "Synchronized automatically via Cloud Firestore",
            latestApkVersionCode = 3,
            latestApkVersionName = "1.2",
            apkDownloadUrl = "https://www.isceb.org/app/releases/latest"
        )
        val modules = listOf(CourseModule("mod-1", 1, "Concept of SCM", "Live Module", CoursePart.PART_1_BASIC))
        val classes = listOf(CourseClass("class-1", "mod-1", 1, "Lecture 01", "Live Class Summary"))
        val settings = AdminCompletionSettings(institutionName = "ISCEB", quizPassingScore = 75)

        val publishRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.publishLiveCurriculum(manifest, modules, classes, settings)
        }
        assertTrue("Admin publish failed: ${publishRes.exceptionOrNull()}", publishRes.isSuccess)

        // Student signs in on second session and observes live manifest + catalog
        signInTestUser("learner_${UUID.randomUUID().toString().take(6)}@test.com")
        val studentRepo = CloudSyncRepository(firestore, auth)

        val liveManifest = withTimeout(FLOW_TIMEOUT_MS) {
            studentRepo.observeLiveManifest().first { it?.versionTag == uniqueTag }
        }
        assertNotNull(liveManifest)
        assertEquals(3, liveManifest!!.latestApkVersionCode)
        assertEquals("1.2", liveManifest.latestApkVersionName)

        val liveCatalog = withTimeout(FLOW_TIMEOUT_MS) {
            studentRepo.observeLiveCatalog().first { it?.versionCode == 110 }
        }
        assertNotNull(liveCatalog)
        assertEquals(75, liveCatalog!!.settings.quizPassingScore)
        assertEquals("Live Class Summary", liveCatalog.classes.first().summary)
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
