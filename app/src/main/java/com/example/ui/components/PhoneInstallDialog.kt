package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.example.ui.icons.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import com.example.ui.icons.CloudSync
import com.example.ui.icons.ContentCopy
import com.example.ui.icons.Download
import com.example.ui.icons.ErrorOutline
import com.example.ui.icons.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import com.example.ui.icons.Security
import androidx.compose.material.icons.filled.Share
import com.example.ui.icons.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ApkDownloadState
import com.example.model.AppUpdateConfig
import com.example.model.AppUpdateInfo
import com.example.ui.theme.BgLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.SuccessGreen
import com.example.util.ApkUpdateManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhoneInstallDialog(
    onDismiss: () -> Unit,
    onShowNotification: (String) -> Unit,
    onCheckUpdates: () -> Unit = {},
    onSyncCurriculum: () -> Unit = {},
    currentCurriculumVersion: String = "v1.4.0",
    appUpdateInfo: AppUpdateInfo = AppUpdateInfo(),
    apkDownloadState: ApkDownloadState = ApkDownloadState.Idle,
    cloudAuthEmail: String? = null,
    onOpenApkDownloadUrl: (String) -> Unit = {},
    onDownloadLatestApk: () -> Unit = {},
    onCancelApkDownload: () -> Unit = {},
    onInstallDownloadedApk: () -> Unit = {},
    onConnectGoogleSync: () -> Unit = {}
) {
    val context = LocalContext.current
    var copiedLink by remember { mutableStateOf(false) }

    val permanentPortalUrl = appUpdateInfo.downloadPageUrl.ifBlank { AppUpdateConfig.DEFAULT_DOWNLOAD_PAGE_URL }

    LaunchedEffect(Unit) {
        val elapsed = System.currentTimeMillis() - appUpdateInfo.lastCheckedTimestamp
        if (elapsed > 10_000L && apkDownloadState !is ApkDownloadState.Downloading) {
            onCheckUpdates()
        }
    }

    val shareMessage = """
        ISCEB Android App — Official Online Download & Auto-Update

        • Permanent Download Link: $permanentPortalUrl
        • Direct APK Link: ${appUpdateInfo.apkDownloadUrl}
        • Latest Version: v${appUpdateInfo.latestAvailableApkVersionName} (Build ${appUpdateInfo.latestAvailableApkVersionCode})

        Tap the link on Android to download ISCEB and install or update in-place without losing your progress or certificates.
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Official ISCEB Logo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyPrimary)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(42.dp)
                                    .width(62.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_isceb_logo),
                                    contentDescription = "ISCEB - Connect. learn. Achieve.",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "ISCEB App Updates & Install",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = "Online APK Updater & Live Cloud Sync",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = GoldSoft,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: In-App APK Update Checker, Downloader & Installer
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_inapp_apk_updater"),
                    colors = CardDefaults.cardColors(containerColor = NavyDark),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(GoldMetallic),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = NavyDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App Binary Updates (APK)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = if (appUpdateInfo.isBinaryUpdateRequired) {
                                        "Update ready: v${appUpdateInfo.latestAvailableApkVersionName} (Build ${appUpdateInfo.latestAvailableApkVersionCode})"
                                    } else {
                                        "Installed: v${appUpdateInfo.currentAppVersionName} (Build ${appUpdateInfo.currentAppVersionCode})"
                                    },
                                    color = GoldSoft,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Version Info Summary Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F2338))
                                .border(1.dp, GoldMetallic.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Installed: v${appUpdateInfo.currentAppVersionName} (${appUpdateInfo.currentAppVersionCode})",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "Latest: v${appUpdateInfo.latestAvailableApkVersionName} (${appUpdateInfo.latestAvailableApkVersionCode})",
                                        fontSize = 10.5.sp,
                                        color = GoldMetallic,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                val metaParts = buildList {
                                    if (appUpdateInfo.releaseDate.isNotBlank()) add("Released: ${appUpdateInfo.releaseDate}")
                                    if (appUpdateInfo.fileSizeBytes > 0L) add(ApkUpdateManager.formatByteSize(appUpdateInfo.fileSizeBytes))
                                    if (appUpdateInfo.lastCheckedTimestamp > 0L) {
                                        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(appUpdateInfo.lastCheckedTimestamp))
                                        add("Checked: $timeStr")
                                    }
                                }
                                if (metaParts.isNotEmpty()) {
                                    Text(
                                        text = metaParts.joinToString(" • "),
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                if (appUpdateInfo.releaseNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Release Notes: ${appUpdateInfo.releaseNotes}",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFE2E8F0),
                                        lineHeight = 14.sp
                                    )
                                }
                                if (appUpdateInfo.sha256.isNotBlank()) {
                                    Text(
                                        text = "SHA-256: ${appUpdateInfo.sha256.take(16)}…${appUpdateInfo.sha256.takeLast(8)}",
                                        fontSize = 9.5.sp,
                                        color = GoldSoft
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic Download & Verification State UI
                        when (val dlState = apkDownloadState) {
                            is ApkDownloadState.Checking -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = GoldMetallic,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Checking update server for latest release...", color = Color.White, fontSize = 11.sp)
                                }
                            }

                            is ApkDownloadState.Downloading -> {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val pctLabel = if (dlState.progressFraction >= 0f) {
                                        "${(dlState.progressFraction * 100).toInt()}%"
                                    } else "Downloading..."
                                    val bytesLabel = if (dlState.totalBytes > 0L) {
                                        "${ApkUpdateManager.formatByteSize(dlState.downloadedBytes)} / ${ApkUpdateManager.formatByteSize(dlState.totalBytes)}"
                                    } else {
                                        ApkUpdateManager.formatByteSize(dlState.downloadedBytes)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Downloading APK...", color = GoldSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("$bytesLabel ($pctLabel)", color = Color.White, fontSize = 10.5.sp)
                                    }
                                    if (dlState.progressFraction >= 0f) {
                                        LinearProgressIndicator(
                                            progress = { dlState.progressFraction },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = GoldMetallic,
                                            trackColor = Color(0xFF1E3A5F)
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = GoldMetallic,
                                            trackColor = Color(0xFF1E3A5F)
                                        )
                                    }
                                    TextButton(
                                        onClick = onCancelApkDownload,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Cancel Download", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                    }
                                }
                            }

                            is ApkDownloadState.Verifying -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = GoldMetallic,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verifying SHA-256 package integrity...", color = GoldSoft, fontSize = 11.sp)
                                }
                            }

                            is ApkDownloadState.ReadyToInstall -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF14532D))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF86EFAC),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Verified v${dlState.versionName} (${ApkUpdateManager.formatByteSize(dlState.fileSizeBytes)})",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "SHA-256: ${dlState.verifiedSha256.take(18)}…",
                                                color = Color(0xFFBBF7D0),
                                                fontSize = 9.5.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onInstallDownloadedApk,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_install_downloaded_apk")
                                ) {
                                    Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Install Update Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            is ApkDownloadState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF7F1D1D))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = Color(0xFFFCA5A5),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = dlState.message,
                                            color = Color.White,
                                            fontSize = 10.5.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                                if (dlState.canRetry) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onDownloadLatestApk,
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("btn_retry_apk_download")
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry Download", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                    }
                                }
                            }

                            is ApkDownloadState.Idle -> {
                                if (appUpdateInfo.lastCheckError != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF78350F))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = appUpdateInfo.lastCheckError,
                                            color = Color(0xFFFEF3C7),
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        if (apkDownloadState !is ApkDownloadState.Downloading &&
                            apkDownloadState !is ApkDownloadState.Verifying &&
                            apkDownloadState !is ApkDownloadState.ReadyToInstall
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCheckUpdates,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_dialog_check_updates")
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Check Updates", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = onDownloadLatestApk,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_download_latest_apk")
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (appUpdateInfo.isBinaryUpdateRequired) "Download APK" else "Download Latest",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: Permanent Web Download Portal & Link Sharing
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NavyPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Permanent Web Download Page",
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = permanentPortalUrl,
                                    color = GoldDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onOpenApkDownloadUrl(permanentPortalUrl) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_open_web_download_page")
                            ) {
                                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Page", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }

                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share ISCEB Download Link"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NavySecondary, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_share_phone_link")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        InstallStepItem(
                            stepNumber = "1",
                            title = "Always the Same Link",
                            desc = "Each new build replaces the APK on the download page—no new link or USB cable needed."
                        )
                        InstallStepItem(
                            stepNumber = "2",
                            title = "Unknown Apps Permission",
                            desc = "When installing directly on Android, enable 'Allow from this source' if prompted."
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { onOpenApkDownloadUrl(AppUpdateConfig.DEFAULT_DESKTOP_PORTAL_URL) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_open_desktop_portal")
                        ) {
                            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open PC / Desktop Version (${AppUpdateConfig.DEFAULT_DESKTOP_PORTAL_URL})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 3: Live Cloud Curriculum Auto-Sync (Separate from APK Updates)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NavySecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cloud Sync (Syllabus & Live Data)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    text = "Curriculum: $currentCurriculumVersion • ${if (cloudAuthEmail != null) cloudAuthEmail else "Offline / Sign in"}",
                                    fontSize = 10.sp,
                                    color = GoldDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Curriculum updates, notices, and profiles sync automatically via Firebase Firestore without reinstalling. UI or feature code updates arrive via the APK Updater above.",
                            fontSize = 10.5.sp,
                            color = Color(0xFF475569),
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (cloudAuthEmail == null) {
                            OutlinedButton(
                                onClick = onConnectGoogleSync,
                                modifier = Modifier.fillMaxWidth().testTag("btn_dialog_connect_google"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sign in with Google for Cloud Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Button(
                            onClick = onSyncCurriculum,
                            modifier = Modifier.fillMaxWidth().testTag("btn_dialog_sync_curriculum"),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Curriculum Data Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // System Specifications
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgLight)
                        .border(1.dp, Color(0xFFDCE4ED), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = GoldDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Signing & Data Preservation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NavyPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Package ID: com.aistudio.supplychain.scm (Play Store compatible)\n• SHA-256 verification before installing\n• Preserves logins, classes, progress, and certificates across every update.",
                            fontSize = 10.sp,
                            color = Color(0xFF475569),
                            lineHeight = 14.5.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("ISCEB Permanent Download URL", permanentPortalUrl)
                    clipboard.setPrimaryClip(clip)
                    copiedLink = true
                    onShowNotification("Download link copied: $permanentPortalUrl")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyPrimary,
                    contentColor = GoldMetallic
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_copy_phone_instructions")
            ) {
                Icon(
                    imageVector = if (copiedLink) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (copiedLink) "Link Copied" else "Copy URL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Close", color = NavyPrimary, fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun InstallStepItem(
    stepNumber: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(GoldMetallic),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = NavyPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = NavyPrimary
            )
            Text(
                text = desc,
                fontSize = 10.sp,
                color = Color(0xFF5A6E85),
                lineHeight = 14.sp
            )
        }
    }
}
