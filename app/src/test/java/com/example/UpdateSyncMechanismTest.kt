package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CloudCatalogSnapshot
import com.example.data.CloudSyncRepository
import com.example.data.RemoteManifestFetchResult
import com.example.data.SupplyChainRepository
import com.example.model.AdminCompletionSettings
import com.example.model.ClassStatus
import com.example.model.CurriculumPublishManifest
import com.example.model.StudentClassProgress
import com.example.model.SyncStatus
import com.example.model.UserRole
import com.example.util.ApkDownloadOutcome
import com.example.util.ApkUpdateManager
import com.google.firebase.FirebaseApp
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
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
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UpdateSyncMechanismTest {

    private lateinit var context: Context
    private lateinit var repository: SupplyChainRepository
    private lateinit var cloudSyncRepository: CloudSyncRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        context.getSharedPreferences("supply_chain_masterclass_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        cloudSyncRepository = CloudSyncRepository(context)
        repository = SupplyChainRepository(context, cloudSyncRepository)
    }

    @Test
    fun `fetchRemoteManifestCacheBusted sends no-cache headers and timestamp param and parses ISCEB manifest`() = runBlocking {
        val capturedQuery = AtomicReference<String?>()
        val capturedCacheControl = AtomicReference<String?>()
        val capturedPragma = AtomicReference<String?>()

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/manifest.json") { exchange ->
            capturedQuery.set(exchange.requestURI.query)
            capturedCacheControl.set(exchange.requestHeaders.getFirst("Cache-Control"))
            capturedPragma.set(exchange.requestHeaders.getFirst("Pragma"))

            val json = """
                {
                  "versionCode": 105,
                  "versionTag": "v1.5.0",
                  "publishedTimestamp": 1763300000000,
                  "publishedBy": "Md. Nafiul Islam Nahin (Developer of ISCEB App)",
                  "releaseNotes": "Live OTA curriculum + binary check",
                  "announcement": "Friday 9 PM Live Class",
                  "minSupportedAppVersion": 1,
                  "latestApkVersionCode": 5,
                  "latestApkVersionName": "1.5",
                  "apkDownloadUrl": "https://www.isceb.org/app/ISCEB-v1.5.apk",
                  "apkReleaseNotes": "Auto sync improvements"
                }
            """.trimIndent().toByteArray()

            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, json.size.toLong())
            exchange.responseBody.use { it.write(json) }
        }
        server.start()

        try {
            val port = server.address.port
            val manifest = cloudSyncRepository.fetchRemoteManifestCacheBusted("http://127.0.0.1:$port/manifest.json")
            assertNotNull(manifest)
            assertEquals(105, manifest!!.versionCode)
            assertEquals("v1.5.0", manifest.versionTag)
            assertEquals(5, manifest.latestApkVersionCode)
            assertEquals("1.5", manifest.latestApkVersionName)
            assertEquals("https://www.isceb.org/app/ISCEB-v1.5.apk", manifest.apkDownloadUrl)

            val query = capturedQuery.get().orEmpty()
            assertTrue("Expected cache-busting query param 't=' in '$query'", query.contains("t="))
            val cacheHeader = capturedCacheControl.get().orEmpty()
            assertTrue("Expected no-cache header in '$cacheHeader'", cacheHeader.contains("no-cache"))
            assertEquals("no-cache", capturedPragma.get())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchRemoteManifestCacheBusted parses GitHub Releases JSON and extracts apk download url`() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/releases/latest") { exchange ->
            val json = """
                {
                  "tag_name": "v2.0",
                  "html_url": "https://github.com/isceb/app/releases/tag/v2.0",
                  "body": "Major update release",
                  "assets": [
                    {
                      "name": "ISCEB.apk",
                      "browser_download_url": "https://github.com/isceb/app/releases/download/v2.0/ISCEB.apk"
                    }
                  ]
                }
            """.trimIndent().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, json.size.toLong())
            exchange.responseBody.use { it.write(json) }
        }
        server.start()

        try {
            val port = server.address.port
            val manifest = cloudSyncRepository.fetchRemoteManifestCacheBusted("http://127.0.0.1:$port/releases/latest")
            assertNotNull(manifest)
            assertEquals("v2.0", manifest!!.versionTag)
            assertEquals("https://github.com/isceb/app/releases/download/v2.0/ISCEB.apk", manifest.apkDownloadUrl)
            assertTrue(manifest.latestApkVersionCode >= BuildConfig.VERSION_CODE)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `applyRemoteManifestAndCatalog flags binary APK update when remote versionCode exceeds installed`() {
        val newerBinaryManifest = CurriculumPublishManifest(
            versionCode = repository.publishManifest.value.versionCode + 1,
            versionTag = "v1.4.0",
            latestApkVersionCode = BuildConfig.VERSION_CODE + 1,
            latestApkVersionName = "2.0.0",
            apkDownloadUrl = "https://www.isceb.org/app/ISCEB-v2.apk",
            apkReleaseNotes = "New native UI & certificate engine"
        )

        val applied = repository.applyRemoteManifestAndCatalog(
            manifest = newerBinaryManifest,
            catalog = null,
            isWorkInProgress = false,
            sourceLabel = "Test Cloud Sync"
        )

        assertTrue(applied)
        val updateInfo = repository.appUpdateInfo.value
        assertTrue("Binary APK update flag should trigger", updateInfo.isBinaryUpdateRequired)
        assertEquals(BuildConfig.VERSION_CODE + 1, updateInfo.latestAvailableApkVersionCode)
        assertEquals("2.0.0", updateInfo.latestAvailableApkVersionName)
        assertEquals("https://www.isceb.org/app/ISCEB-v2.apk", updateInfo.apkDownloadUrl)
    }

    @Test
    fun `applyRemoteManifestAndCatalog defers curriculum apply while student exam or upload is active`() {
        val initialVersionCode = repository.publishManifest.value.versionCode
        val incomingManifest = CurriculumPublishManifest(
            versionCode = initialVersionCode + 5,
            versionTag = "v1.9.0",
            releaseNotes = "Updated procurement slides",
            latestApkVersionCode = BuildConfig.VERSION_CODE,
            latestApkVersionName = BuildConfig.VERSION_NAME
        )

        val detected = repository.applyRemoteManifestAndCatalog(
            manifest = incomingManifest,
            catalog = null,
            isWorkInProgress = true,
            sourceLabel = "Test Cloud Sync"
        )

        assertTrue(detected)
        assertEquals(initialVersionCode, repository.publishManifest.value.versionCode)
        assertEquals(incomingManifest, repository.pendingUpdateManifest.value)
        assertEquals(SyncStatus.UPDATE_AVAILABLE, repository.syncStatus.value)
        assertFalse(repository.appUpdateInfo.value.isBinaryUpdateRequired)

        // Now student completes exam and clicks Sync Now
        val synced = repository.syncCurriculumNow()
        assertTrue(synced)
        assertEquals(incomingManifest.versionCode, repository.publishManifest.value.versionCode)
        assertNull(repository.pendingUpdateManifest.value)
        assertEquals(SyncStatus.SYNCED_SUCCESS, repository.syncStatus.value)
    }

    @Test
    fun `mergeCloudStudentProgress preserves highest progress and merges completed classes safely`() {
        val studentId = repository.currentUser.value.id
        // Local student has 240 active seconds on class-1 and passed quiz
        repository.mergeCloudStudentProgress(
            mapOf(
                "class-1" to StudentClassProgress(
                    classId = "class-1",
                    studentId = studentId,
                    status = ClassStatus.COMPLETED,
                    activeLearningSeconds = 240L,
                    maxPlaybackPercent = 98f,
                    isPdfConfirmed = true,
                    pdfReadingSeconds = 90L,
                    quizScore = 92,
                    quizPassed = true,
                    completionTimestamp = 1700000000000L
                )
            )
        )

        // Simulate stale/partial remote sync with lower numbers on class-1 and new progress on class-2
        repository.mergeCloudStudentProgress(
            mapOf(
                "class-1" to StudentClassProgress(
                    classId = "class-1",
                    studentId = studentId,
                    status = ClassStatus.IN_PROGRESS,
                    activeLearningSeconds = 60L,
                    maxPlaybackPercent = 40f,
                    isPdfConfirmed = false,
                    quizScore = 70,
                    quizPassed = false
                ),
                "class-2" to StudentClassProgress(
                    classId = "class-2",
                    studentId = studentId,
                    status = ClassStatus.COMPLETED,
                    activeLearningSeconds = 180L,
                    isPdfConfirmed = true,
                    quizScore = 88,
                    quizPassed = true,
                    completionTimestamp = 1700000100000L
                )
            )
        )

        val mergedMap = repository.studentProgressMap.value
        val cls1 = mergedMap["class-1"]!!
        assertEquals(ClassStatus.COMPLETED, cls1.status)
        assertEquals(240L, cls1.activeLearningSeconds)
        assertEquals(92, cls1.quizScore)
        assertTrue(cls1.isPdfConfirmed)

        val cls2 = mergedMap["class-2"]!!
        assertEquals(ClassStatus.COMPLETED, cls2.status)
        assertTrue(repository.progress.value.completedClassIds.containsAll(setOf("class-1", "class-2")))
    }

    @Test
    fun `catalog OTA update preserves existing student completion while updating class content`() {
        repository.toggleComplete("class-1")
        assertTrue(repository.classes.value.first { it.id == "class-1" }.isCompleted)

        val updatedClasses = repository.classes.value.map { cls ->
            if (cls.id == "class-2") {
                cls.copy(title = "Lecture 02: Updated Live Syllabus Topic", isCompleted = false)
            } else {
                cls.copy(isCompleted = false)
            }
        }
        val manifest = CurriculumPublishManifest(
            versionCode = 110,
            versionTag = "v2.1.0",
            releaseNotes = "Updated Lecture 02 title"
        )
        val snapshot = CloudCatalogSnapshot(
            versionCode = 110,
            modules = repository.modules.value,
            classes = updatedClasses,
            settings = AdminCompletionSettings()
        )

        repository.applyRemoteManifestAndCatalog(
            manifest = manifest,
            catalog = snapshot,
            isWorkInProgress = false
        )

        val currentClasses = repository.classes.value
        assertTrue("class-1 must remain completed after catalog sync", currentClasses.first { it.id == "class-1" }.isCompleted)
        assertEquals("Lecture 02: Updated Live Syllabus Topic", currentClasses.first { it.id == "class-2" }.title)
    }

    @Test
    fun `fetchRemoteManifestDetailed parses version json schema without downgrading curriculum versionCode`() = runBlocking {
        val initialCurriculumCode = repository.publishManifest.value.versionCode
        val shaSample = "a".repeat(64)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/version.json") { exchange ->
            val json = """
                {
                  "versionName": "1.6",
                  "versionCode": 9,
                  "apkUrl": "https://gen-lang-client-0901479759.web.app/downloads/ISCEB-latest.apk",
                  "downloadPageUrl": "https://gen-lang-client-0901479759.web.app/",
                  "sha256": "$shaSample",
                  "fileSizeBytes": 24117248,
                  "releaseDate": "2026-11-18",
                  "releaseNotes": "In-app updater and online APK release",
                  "minSupportedVersionCode": 2
                }
            """.trimIndent().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, json.size.toLong())
            exchange.responseBody.use { it.write(json) }
        }
        server.start()

        try {
            val port = server.address.port
            val result = cloudSyncRepository.fetchRemoteManifestDetailed("http://127.0.0.1:$port/version.json")
            assertTrue(result is RemoteManifestFetchResult.Success)
            val manifest = (result as RemoteManifestFetchResult.Success).manifest
            assertEquals(9, manifest.latestApkVersionCode)
            assertEquals("1.6", manifest.latestApkVersionName)
            assertEquals("https://gen-lang-client-0901479759.web.app/downloads/ISCEB-latest.apk", manifest.apkDownloadUrl)
            assertEquals(shaSample, manifest.apkSha256)
            assertEquals(24117248L, manifest.apkFileSizeBytes)
            assertEquals("2026-11-18", manifest.apkReleaseDate)
            assertEquals(2, manifest.minSupportedAppVersion)

            repository.applyRemoteManifestAndCatalog(
                manifest = manifest,
                catalog = null,
                isWorkInProgress = false,
                sourceLabel = "version.json"
            )
            assertEquals(initialCurriculumCode, repository.publishManifest.value.versionCode)
            assertTrue(repository.appUpdateInfo.value.isBinaryUpdateRequired)
            assertEquals(shaSample, repository.appUpdateInfo.value.sha256)
            assertEquals(24117248L, repository.appUpdateInfo.value.fileSizeBytes)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchRemoteManifestDetailed reports failure on HTTP 404`() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/version.json") { exchange ->
            val msg = "Not Found".toByteArray()
            exchange.sendResponseHeaders(404, msg.size.toLong())
            exchange.responseBody.use { it.write(msg) }
        }
        server.start()

        try {
            val port = server.address.port
            val result = cloudSyncRepository.fetchRemoteManifestDetailed("http://127.0.0.1:$port/version.json")
            assertTrue(result is RemoteManifestFetchResult.Failure)
            assertTrue((result as RemoteManifestFetchResult.Failure).errorMessage.contains("404"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `apkUpdateManager downloads verifies valid ZIP header and SHA-256 and rejects corrupt payloads`() = runBlocking {
        val validApkPayload = byteArrayOf(0x50, 0x4B, 0x03, 0x04) + "ISCEB_VALID_APK_TEST_PAYLOAD".toByteArray()
        val validSha = MessageDigest.getInstance("SHA-256")
            .digest(validApkPayload)
            .joinToString("") { "%02x".format(it) }
        val htmlPayload = "<!DOCTYPE html><html><body>404 Fallback</body></html>".toByteArray()

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/valid.apk") { exchange ->
            exchange.responseHeaders.add("Content-Type", "application/vnd.android.package-archive")
            exchange.sendResponseHeaders(200, validApkPayload.size.toLong())
            exchange.responseBody.use { it.write(validApkPayload) }
        }
        server.createContext("/fake.apk") { exchange ->
            exchange.responseHeaders.add("Content-Type", "text/html")
            exchange.sendResponseHeaders(200, htmlPayload.size.toLong())
            exchange.responseBody.use { it.write(htmlPayload) }
        }
        server.start()

        val manager = ApkUpdateManager(context)
        try {
            val port = server.address.port
            var lastProgress = -1f

            // 1. Valid APK + matching SHA-256
            val okOutcome = manager.downloadAndVerifyApk(
                apkUrl = "http://127.0.0.1:$port/valid.apk",
                versionName = "1.4",
                expectedSha256 = validSha,
                expectedSizeBytes = validApkPayload.size.toLong(),
                onProgress = { _, _, fraction -> lastProgress = fraction }
            )
            assertTrue("Expected valid APK download to succeed", okOutcome is ApkDownloadOutcome.Success)
            val ok = okOutcome as ApkDownloadOutcome.Success
            assertEquals(validSha, ok.verifiedSha256)
            assertEquals(validApkPayload.size.toLong(), ok.fileSizeBytes)
            assertTrue(ok.apkFile.exists())
            assertEquals(1.0f, lastProgress, 0.001f)

            // 2. SHA-256 mismatch rejects and deletes artifact
            val badShaOutcome = manager.downloadAndVerifyApk(
                apkUrl = "http://127.0.0.1:$port/valid.apk",
                versionName = "1.4",
                expectedSha256 = "0".repeat(64),
                expectedSizeBytes = validApkPayload.size.toLong(),
                onProgress = { _, _, _ -> }
            )
            assertTrue(badShaOutcome is ApkDownloadOutcome.Failure)
            assertTrue((badShaOutcome as ApkDownloadOutcome.Failure).errorMessage.contains("SHA-256"))

            // 3. Invalid non-ZIP/HTML content rejected
            val htmlOutcome = manager.downloadAndVerifyApk(
                apkUrl = "http://127.0.0.1:$port/fake.apk",
                versionName = "1.4",
                onProgress = { _, _, _ -> }
            )
            assertTrue(htmlOutcome is ApkDownloadOutcome.Failure)
            assertTrue((htmlOutcome as ApkDownloadOutcome.Failure).errorMessage.contains("valid Android APK"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `admin users list puts Md Amirul Islam first and Md Nafiul Islam Nahin second`() {
        val admins = repository.availableUsers.filter { it.role == UserRole.ADMIN }
        assertEquals(2, admins.size)
        assertEquals("admin-2", admins[0].id)
        assertEquals("Md. Amirul Islam", admins[0].fullName)
        assertEquals("admin-1", admins[1].id)
        assertEquals("Md. Nafiul Islam Nahin", admins[1].fullName)
    }

    @Test
    fun `deferred autoLoad=false repository stays empty until initializeIfNeeded runs idempotently on IO`() = runBlocking {
        val lazyRepo = SupplyChainRepository(context, cloudSyncRepository, autoLoad = false)
        assertFalse(lazyRepo.isReady.value)
        assertTrue(lazyRepo.modules.value.isEmpty())
        assertTrue(lazyRepo.classes.value.isEmpty())

        lazyRepo.initializeIfNeeded()
        lazyRepo.initializeIfNeeded() // second call must be safe no-op

        assertTrue(lazyRepo.isReady.value)
        assertEquals(12, lazyRepo.modules.value.size)
        assertEquals(48, lazyRepo.classes.value.size)
    }

    @Test
    fun `workspace backup export and import restores completed progress and custom settings cleanly`() {
        repository.toggleComplete("class-1")
        repository.recordActiveLearningTime("class-2", 95L)
        assertTrue(repository.classes.value.first { it.id == "class-1" }.isCompleted)

        val backupJson = repository.exportWorkspaceBackupJson()
        assertTrue(backupJson.contains("com.aistudio.supplychain.scm"))

        // Clear all SharedPreferences to simulate clean reinstall on new signature/device
        context.getSharedPreferences("supply_chain_masterclass_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        val freshRepo = SupplyChainRepository(context, cloudSyncRepository)
        assertFalse(freshRepo.classes.value.first { it.id == "class-1" }.isCompleted)

        val importResult = freshRepo.importWorkspaceBackupJson(backupJson)
        assertTrue(importResult.isSuccess)
        assertTrue((importResult.getOrNull() ?: 0) > 0)
        assertTrue(freshRepo.classes.value.first { it.id == "class-1" }.isCompleted)
        assertEquals(95L, freshRepo.studentProgressMap.value["class-2"]?.activeLearningSeconds)
    }
}
