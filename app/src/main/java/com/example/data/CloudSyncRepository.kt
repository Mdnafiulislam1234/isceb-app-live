package com.example.data

import android.content.Context
import com.example.BuildConfig
import com.example.R
import com.example.model.AdminCompletionSettings
import com.example.model.AppUpdateConfig
import com.example.model.ClassStatus
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.CoursePart
import com.example.model.CourseScheduleConfig
import com.example.model.CurriculumPublishManifest
import com.example.model.LessonMedia
import com.example.model.MediaType
import com.example.model.StudentClassProgress
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
    val membersJson: String = ""
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
        membersJson: String = ""
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
                membersJson = membersJson
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
        val docRef = db.collection("users").document(uid).collection("progress").document(progress.classId)
        return try {
            val snap = docRef.get().await()
            val mutableFields = buildMap<String, Any> {
                put("status", progress.status.name)
                put("activeLearningSeconds", progress.activeLearningSeconds)
                put("maxPlaybackPercent", progress.maxPlaybackPercent.toDouble())
                put("isPdfConfirmed", progress.isPdfConfirmed)
                put("pdfReadingSeconds", progress.pdfReadingSeconds)
                progress.quizScore?.let { put("quizScore", it) }
                put("quizPassed", progress.quizPassed)
                progress.completionTimestamp?.let { put("completionTimestamp", it) }
                put("updatedAt", FieldValue.serverTimestamp())
            }

            if (!snap.exists()) {
                val createPayload = mutableMapOf<String, Any>().apply {
                    putAll(mutableFields)
                    put("userId", uid)
                    put("classId", progress.classId)
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
        membersJson: String = ""
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
                cls.pdfMedia?.let { put("pdfMedia", mediaToMap(it)) }
                cls.audioMedia?.let { put("audioMedia", mediaToMap(it)) }
                cls.videoMedia?.let { put("videoMedia", mediaToMap(it)) }
                if (cls.attachments.isNotEmpty()) {
                    put("attachments", cls.attachments.map { mediaToMap(it) })
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
            if (founderProfileJson.isNotBlank()) put("founderProfileJson", founderProfileJson)
            if (developerProfileJson.isNotBlank()) put("developerProfileJson", developerProfileJson)
            if (membersJson.isNotBlank()) put("membersJson", membersJson)
        }
        return mapOf(
            "versionCode" to versionCode.toLong(),
            "modules" to moduleMaps,
            "classes" to classMaps,
            "settings" to settingsMap
        )
    }

    private fun mediaToMap(media: LessonMedia): Map<String, Any> {
        return buildMap {
            put("id", media.id)
            put("type", media.type.name)
            put("fileName", media.fileName)
            put("fileSizeFormatted", media.fileSizeFormatted)
            put("fileSizeBytes", media.fileSizeBytes)
            put("url", media.url)
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
                lectureCode = c["lectureCode"] as? String ?: ""
            )
        }

        val reqIds = (rawSettings["requiredModuleIds"] as? List<*>)?.mapNotNull { it as? String }?.toSet()
            ?: (1..12).map { "mod-$it" }.toSet()

        val settings = AdminCompletionSettings(
            minLearningTimeSeconds = (rawSettings["minLearningTimeSeconds"] as? Number)?.toLong() ?: 60L,
            minPlaybackPercent = (rawSettings["minPlaybackPercent"] as? Number)?.toFloat() ?: 80f,
            minPdfReadingSeconds = (rawSettings["minPdfReadingSeconds"] as? Number)?.toLong() ?: 45L,
            isQuizRequired = rawSettings["isQuizRequired"] as? Boolean ?: true,
            quizPassingScore = (rawSettings["quizPassingScore"] as? Number)?.toInt() ?: 70,
            requiredModuleIds = reqIds,
            institutionName = rawSettings["institutionName"] as? String ?: "ISCEB",
            officialWebsite = rawSettings["officialWebsite"] as? String ?: "www.isceb.org",
            institutionAccreditation = rawSettings["institutionAccreditation"] as? String
                ?: "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)",
            authorizedSignerName = (rawSettings["authorizedSignerName"] as? String)
                ?.replace("Academic Detector", "Developer of ISCEB App")
                ?.replace("Founder & Developer of ISCEB", "Developer of ISCEB App")
                ?.replace("MD. Nafiul", "Md. Nafiul")
                ?: "Md. Amirul Islam, Founder of ISCEB",
            authorizedSignerTitle = rawSettings["authorizedSignerTitle"] as? String
                ?: "Lead Trainer of ISCEB • Head of SCM, BRB Group",
            certificateTitle = rawSettings["certificateTitle"] as? String
                ?: "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)",
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
            membersJson = rawSettings["membersJson"] as? String ?: ""
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
        return StudentClassProgress(
            classId = classId,
            studentId = userId,
            status = status,
            activeLearningSeconds = doc.getLong("activeLearningSeconds") ?: 0L,
            maxPlaybackPercent = (doc.getDouble("maxPlaybackPercent") ?: 0.0).toFloat(),
            isPdfConfirmed = doc.getBoolean("isPdfConfirmed") ?: false,
            pdfReadingSeconds = doc.getLong("pdfReadingSeconds") ?: 0L,
            quizScore = doc.getLong("quizScore")?.toInt(),
            quizPassed = doc.getBoolean("quizPassed") ?: false,
            completionTimestamp = doc.getLong("completionTimestamp")
        )
    }

    companion object {
        const val COLLECTION_MANIFESTS = "curriculum_manifests"
        const val COLLECTION_CATALOGS = "curriculum_catalogs"
        const val COLLECTION_LESSON_FILES = "lesson_files"
        const val LIVE_DOC_ID = "live"
        const val MAX_CLOUD_FILE_BYTES = 3_500_000L
        const val CHUNK_RAW_BYTES = 340_000

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
