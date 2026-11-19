package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.data.CloudSyncRepository
import com.example.model.LessonMedia
import com.example.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PickedFileMeta(
    val fileName: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val mimeType: String?
)

object MediaFileManager {

    const val MAX_UPLOAD_BYTES: Long = 50L * 1024L * 1024L // 50 MB limit per file

    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun inferMediaType(mimeType: String?, fileName: String): MediaType {
        val lowerMime = mimeType?.lowercase(Locale.ROOT).orEmpty()
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when {
            lowerMime == "application/pdf" || ext == "pdf" -> MediaType.PDF
            lowerMime.startsWith("image/") || ext in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "heic") -> MediaType.IMAGE
            lowerMime.startsWith("audio/") || ext in setOf("mp3", "m4a", "wav", "aac", "ogg", "flac") -> MediaType.AUDIO
            lowerMime.startsWith("video/") || ext in setOf("mp4", "mkv", "webm", "mov", "3gp", "avi") -> MediaType.VIDEO
            else -> MediaType.DOCUMENT
        }
    }

    fun inferMimeType(media: LessonMedia): String {
        if (media.mimeType.isNotBlank()) return media.mimeType
        val ext = media.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (media.type) {
            MediaType.PDF -> "application/pdf"
            MediaType.IMAGE -> when (ext) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                else -> "image/*"
            }
            MediaType.DOCUMENT -> when (ext) {
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "xls" -> "application/vnd.ms-excel"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "ppt" -> "application/vnd.ms-powerpoint"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "txt" -> "text/plain"
                "csv" -> "text/csv"
                else -> "*/*"
            }
            MediaType.AUDIO -> "audio/*"
            MediaType.VIDEO -> "video/*"
        }
    }

    fun getFileMetaFromUri(context: Context, uri: Uri): PickedFileMeta {
        var displayName = "uploaded_file"
        var sizeBytes = 0L
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            displayName = name
                        }
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
            displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "uploaded_file"
        }

        if (sizeBytes <= 0L) {
            try {
                contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    if (afd.length > 0) sizeBytes = afd.length
                }
            } catch (_: Exception) {
                // Unknown stream size; actual copy will count exact bytes
            }
        }

        val sizeFormatted = if (sizeBytes > 0L) formatFileSize(sizeBytes) else "Ready"
        return PickedFileMeta(
            fileName = sanitizeFileName(displayName),
            sizeBytes = sizeBytes,
            sizeFormatted = sizeFormatted,
            mimeType = mimeType
        )
    }

    suspend fun copyUriToInternalStorage(
        context: Context,
        uri: Uri,
        classId: String,
        mediaType: MediaType,
        customFileName: String? = null,
        onProgress: (progress: Float, copiedBytes: Long, totalBytes: Long) -> Unit
    ): File {
        val meta = getFileMetaFromUri(context, uri)
        if (meta.sizeBytes > MAX_UPLOAD_BYTES) {
            throw IllegalArgumentException("File '${meta.fileName}' (${meta.sizeFormatted}) exceeds the 50 MB upload limit.")
        }

        val originalName = customFileName?.ifBlank { null } ?: meta.fileName
        val cleanName = sanitizeFileName(originalName)

        val mediaDir = File(context.filesDir, "uploaded_media")
        if (!mediaDir.exists() && !mediaDir.mkdirs()) {
            throw IllegalStateException("Unable to create internal storage directory for class materials.")
        }

        val hasExtension = cleanName.contains('.') && cleanName.substringAfterLast('.').length in 2..5
        val fallbackExt = when (mediaType) {
            MediaType.PDF -> ".pdf"
            MediaType.IMAGE -> ".jpg"
            MediaType.DOCUMENT -> ".docx"
            MediaType.AUDIO -> ".mp3"
            MediaType.VIDEO -> ".mp4"
        }
        val finalName = if (hasExtension) cleanName else "$cleanName$fallbackExt"

        val destinationFile = File(
            mediaDir,
            "${classId}_${mediaType.name.lowercase(Locale.ROOT)}_${System.currentTimeMillis()}_$finalName"
        )

        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not read selected file '${meta.fileName}'. Please pick the file again.")

        val totalExpectedBytes = if (meta.sizeBytes > 0L) meta.sizeBytes else 512L * 1024L

        try {
            FileOutputStream(destinationFile).use { outputStream ->
                val buffer = ByteArray(16 * 1024)
                var bytesCopied = 0L
                var read: Int

                inputStream.use { stream ->
                    while (stream.read(buffer).also { read = it } != -1) {
                        bytesCopied += read
                        if (bytesCopied > MAX_UPLOAD_BYTES) {
                            throw IllegalArgumentException("File exceeds the 50 MB upload limit.")
                        }
                        outputStream.write(buffer, 0, read)
                        val currentProgress = (bytesCopied.toFloat() / totalExpectedBytes.toFloat()).coerceIn(0.02f, 0.99f)
                        onProgress(currentProgress, bytesCopied, maxOf(totalExpectedBytes, bytesCopied))
                    }
                }
                outputStream.flush()
            }
        } catch (e: Exception) {
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            throw e
        }

        if (!destinationFile.exists() || destinationFile.length() == 0L) {
            if (destinationFile.exists()) destinationFile.delete()
            throw IllegalStateException("Uploaded file '${meta.fileName}' was empty or failed to save.")
        }

        onProgress(1.0f, destinationFile.length(), destinationFile.length())
        return destinationFile
    }

    fun copyProfilePhotoToInternalStorage(
        context: Context,
        uri: Uri,
        prefix: String = "developer_avatar"
    ): File? {
        return try {
            val profileDir = File(context.filesDir, "profile_photos")
            if (!profileDir.exists()) profileDir.mkdirs()
            val cleanPrefix = sanitizeFileName(prefix)
            val destinationFile = File(profileDir, "${cleanPrefix}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: return null
            if (destinationFile.exists() && destinationFile.length() > 0L) destinationFile else null
        } catch (_: Exception) {
            null
        }
    }

    fun findExistingLocalFile(context: Context, media: LessonMedia): File? {
        if (media.url.isNotBlank() && !media.url.startsWith("http://") && !media.url.startsWith("https://") && !media.url.startsWith("storage://")) {
            val direct = File(media.url)
            if (direct.exists() && direct.length() > 0L) return direct
        }
        val cached = getCachedCloudFile(context, media)
        if (cached.exists() && cached.length() > 0L) return cached
        return null
    }

    fun getCachedCloudFile(context: Context, media: LessonMedia): File {
        val dir = File(context.filesDir, "cached_cloud_media").apply { if (!exists()) mkdirs() }
        val key = sanitizeFileName(media.cloudFileId.ifBlank { media.id })
        val cleanName = sanitizeFileName(media.fileName.ifBlank { "material.bin" })
        return File(dir, "${key}_$cleanName")
    }

    suspend fun ensureLocalMediaFile(
        context: Context,
        media: LessonMedia,
        cloudSyncRepository: CloudSyncRepository? = null
    ): File? = withContext(Dispatchers.IO) {
        findExistingLocalFile(context, media)?.let { return@withContext it }

        val targetFile = getCachedCloudFile(context, media)
        if (media.url.startsWith("http://") || media.url.startsWith("https://")) {
            try {
                val request = Request.Builder().url(media.url.trim()).build()
                downloadClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStream = response.body?.byteStream()
                        if (bodyStream != null) {
                            FileOutputStream(targetFile).use { out ->
                                bodyStream.copyTo(out)
                                out.flush()
                            }
                            if (targetFile.exists() && targetFile.length() > 0L) {
                                return@withContext targetFile
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                if (targetFile.exists()) targetFile.delete()
            }
        }

        if (media.cloudFileId.isNotBlank() && media.cloudChunkCount > 0 && cloudSyncRepository != null) {
            try {
                val result = cloudSyncRepository.downloadLessonFileBytes(media.cloudFileId, media.cloudChunkCount)
                val bytes = result.getOrNull()
                if (bytes != null && bytes.isNotEmpty()) {
                    FileOutputStream(targetFile).use { out ->
                        out.write(bytes)
                        out.flush()
                    }
                    if (targetFile.exists() && targetFile.length() > 0L) {
                        return@withContext targetFile
                    }
                }
            } catch (_: Exception) {
                if (targetFile.exists()) targetFile.delete()
            }
        }

        null
    }

    fun getPdfPageCount(file: File): Int {
        if (!file.exists() || file.length() == 0L) return 0
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (_: Throwable) {
            0
        }
    }

    fun renderPdfPageBitmap(file: File, pageIndex: Int, targetWidthPx: Int = 1080): Bitmap? {
        if (!file.exists() || file.length() == 0L) return null
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount <= 0) return null
                    val safeIndex = pageIndex.coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(safeIndex).use { page ->
                        val width = targetWidthPx.coerceIn(320, 1800)
                        val ratio = page.height.toFloat() / page.width.coerceAtLeast(1).toFloat()
                        val height = (width * ratio).toInt().coerceIn(240, 2600)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun exportMediaToUri(context: Context, media: LessonMedia, destinationUri: Uri): Boolean {
        return try {
            val sourceFile = findExistingLocalFile(context, media) ?: return false
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openMediaWithSystemViewer(context: Context, media: LessonMedia): Boolean {
        return try {
            val localFile = findExistingLocalFile(context, media)
            if (localFile != null && localFile.exists()) {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    localFile
                )
                val mimeType = inferMimeType(media)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(intent, "Open class material").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooser)
                return true
            }

            val url = media.url.trim()
            if (url.startsWith("http://") || url.startsWith("https://")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    fun sanitizeFileName(name: String): String {
        val trimmed = name.trim().ifBlank { "file" }
        return trimmed.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(80)
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            bytes > 0 -> "$bytes B"
            else -> "0 B"
        }
    }
}
