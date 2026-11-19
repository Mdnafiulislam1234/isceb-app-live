package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

sealed class ApkDownloadOutcome {
    data class Success(
        val apkFile: File,
        val verifiedSha256: String,
        val fileSizeBytes: Long
    ) : ApkDownloadOutcome()

    data class Failure(val errorMessage: String) : ApkDownloadOutcome()
}

sealed class ApkInstallLaunchResult {
    object LaunchedInstaller : ApkInstallLaunchResult()
    data class RequiresUnknownSourcesPermission(val settingsIntent: Intent) : ApkInstallLaunchResult()
    data class Failure(val message: String) : ApkInstallLaunchResult()
}

class ApkUpdateManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient = defaultDownloadClient()
) {

    fun isSafeDownloadUrl(rawUrl: String): Boolean {
        val parsed = rawUrl.trim().toHttpUrlOrNull() ?: return false
        if (parsed.isHttps) return true
        // Permit localhost HTTP only for automated local unit/Robolectric tests
        val host = parsed.host.lowercase()
        return parsed.scheme == "http" && (host == "127.0.0.1" || host == "localhost")
    }

    suspend fun downloadAndVerifyApk(
        apkUrl: String,
        versionName: String,
        expectedSha256: String = "",
        expectedSizeBytes: Long = 0L,
        onProgress: (downloadedBytes: Long, totalBytes: Long, progressFraction: Float) -> Unit,
        onVerifying: () -> Unit = {}
    ): ApkDownloadOutcome = withContext(Dispatchers.IO) {
        val trimmedUrl = apkUrl.trim()
        if (!isSafeDownloadUrl(trimmedUrl)) {
            return@withContext ApkDownloadOutcome.Failure(
                "Invalid or insecure APK URL. HTTPS download link is required."
            )
        }
        val parsedUrl = trimmedUrl.toHttpUrlOrNull()
            ?: return@withContext ApkDownloadOutcome.Failure("Malformed APK download URL.")

        val updatesDir = File(context.cacheDir, APK_UPDATES_DIR).apply { mkdirs() }
        // Clean stale update files from earlier attempts
        updatesDir.listFiles()?.forEach { oldFile ->
            if (oldFile.isFile) oldFile.delete()
        }

        val safeVer = versionName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "latest" }
        val partFile = File(updatesDir, "ISCEB-v$safeVer.apk.part")
        val finalFile = File(updatesDir, "ISCEB-v$safeVer.apk")

        val request = Request.Builder()
            .url(parsedUrl)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Cache-Control", "no-cache, no-store")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ApkDownloadOutcome.Failure(
                        "Update server returned HTTP ${response.code}. Please confirm the APK release is uploaded."
                    )
                }

                val body = response.body
                    ?: return@withContext ApkDownloadOutcome.Failure("Update server returned an empty download stream.")

                val headerTotal = body.contentLength().takeIf { it > 0L } ?: expectedSizeBytes.coerceAtLeast(0L)
                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L
                val buffer = ByteArray(16 * 1024)

                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloaded += read
                            val fraction = if (headerTotal > 0L) {
                                (downloaded.toFloat() / headerTotal.toFloat()).coerceIn(0f, 1f)
                            } else {
                                -1f
                            }
                            onProgress(downloaded, headerTotal, fraction)
                        }
                        output.flush()
                    }
                }

                if (downloaded <= 0L) {
                    partFile.delete()
                    return@withContext ApkDownloadOutcome.Failure("Downloaded APK file is empty (0 bytes).")
                }

                onVerifying()

                if (expectedSizeBytes > 0L && downloaded != expectedSizeBytes) {
                    partFile.delete()
                    return@withContext ApkDownloadOutcome.Failure(
                        "Downloaded size ($downloaded bytes) does not match expected package size ($expectedSizeBytes bytes)."
                    )
                }

                if (!hasValidZipMagicHeader(partFile)) {
                    partFile.delete()
                    return@withContext ApkDownloadOutcome.Failure(
                        "Downloaded file is not a valid Android APK package (invalid binary header)."
                    )
                }

                val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                val normalizedExpectedSha = expectedSha256.trim().lowercase()
                if (normalizedExpectedSha.isNotEmpty() && actualSha256 != normalizedExpectedSha) {
                    partFile.delete()
                    return@withContext ApkDownloadOutcome.Failure(
                        "APK security verification failed (SHA-256 mismatch). Corrupted or tampered download discarded."
                    )
                }

                if (finalFile.exists()) finalFile.delete()
                if (!partFile.renameTo(finalFile)) {
                    partFile.copyTo(finalFile, overwrite = true)
                    partFile.delete()
                }

                ApkDownloadOutcome.Success(
                    apkFile = finalFile,
                    verifiedSha256 = actualSha256,
                    fileSizeBytes = finalFile.length()
                )
            }
        } catch (ce: CancellationException) {
            partFile.delete()
            throw ce
        } catch (e: Exception) {
            partFile.delete()
            ApkDownloadOutcome.Failure(
                e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Network error while downloading update package."
            )
        }
    }

    private fun hasValidZipMagicHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 4L) return false
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                val read = fis.read(header)
                read == 4 &&
                    header[0] == 0x50.toByte() && // 'P'
                    header[1] == 0x4B.toByte() && // 'K'
                    header[2] == 0x03.toByte() &&
                    header[3] == 0x04.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun buildUnknownSourcesSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun launchAndroidPackageInstaller(apkFilePath: String): ApkInstallLaunchResult {
        val file = File(apkFilePath)
        if (!file.exists() || !file.isFile || file.length() <= 0L) {
            return ApkInstallLaunchResult.Failure("Downloaded APK file is no longer in cache. Please retry download.")
        }

        if (!canRequestPackageInstalls()) {
            return ApkInstallLaunchResult.RequiresUnknownSourcesPermission(buildUnknownSourcesSettingsIntent())
        }

        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            ApkInstallLaunchResult.LaunchedInstaller
        } catch (e: Exception) {
            ApkInstallLaunchResult.Failure(
                "Unable to open Android Package Installer: ${e.localizedMessage ?: "Unsupported installer intent"}"
            )
        }
    }

    companion object {
        const val APK_UPDATES_DIR = "apk_updates"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"

        private fun defaultDownloadClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .cache(null)
                .build()
        }

        fun formatByteSize(bytes: Long): String {
            if (bytes <= 0L) return "Unknown size"
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) String.format(java.util.Locale.US, "%.1f MB", mb)
            else String.format(java.util.Locale.US, "%.0f KB", bytes / 1024.0)
        }
    }
}
