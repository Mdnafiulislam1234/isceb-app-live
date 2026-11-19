package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.example.ui.icons.AdminPanelSettings
import com.example.ui.icons.Badge
import com.example.ui.icons.ChevronRight
import com.example.ui.icons.Cloud
import com.example.ui.icons.CloudDone
import com.example.ui.icons.CloudOff
import com.example.ui.icons.Download
import com.example.ui.icons.ErrorOutline
import com.example.ui.icons.Groups
import com.example.ui.icons.InstallMobile
import androidx.compose.material.icons.filled.Lock
import com.example.ui.icons.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import com.example.ui.icons.School
import com.example.ui.icons.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import com.example.ui.icons.SwitchAccount
import com.example.ui.icons.SystemUpdate
import com.example.ui.icons.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ApkDownloadState
import com.example.model.AppUpdateConfig
import com.example.model.AppUpdateInfo
import com.example.model.DeveloperProfile
import com.example.model.FounderProfile
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.util.ApkUpdateManager
import java.io.File
import com.example.ui.theme.BgLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SuccessGreen

@Composable
fun ProfileScreen(
    currentUser: UserProfile,
    availableUsers: List<UserProfile>,
    isAdmin: Boolean,
    cloudAuthEmail: String? = null,
    appUpdateInfo: AppUpdateInfo = AppUpdateInfo(),
    apkDownloadState: ApkDownloadState = ApkDownloadState.Idle,
    onSwitchUser: (String) -> Unit,
    onAdminLoginClick: () -> Unit,
    onAdminLogoutClick: () -> Unit,
    onOpenVerification: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFirebaseGuide: () -> Unit,
    onResetDemoData: () -> Unit,
    onOpenPhoneInstall: () -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    onDownloadLatestApk: () -> Unit = {},
    onCancelApkDownload: () -> Unit = {},
    onInstallDownloadedApk: () -> Unit = {},
    onOpenWebDownloadPage: (String) -> Unit = {},
    onOpenDeveloperProfile: () -> Unit = {},
    onOpenFounderProfile: () -> Unit = {},
    onOpenMembers: () -> Unit = {},
    founderProfile: FounderProfile = FounderProfile(),
    developerProfile: DeveloperProfile = DeveloperProfile(),
    membersCount: Int = 1,
    onConnectGoogleCloudClick: () -> Unit = {},
    onDisconnectGoogleCloudClick: () -> Unit = {},
    onExportBackupJson: () -> String = { "{}" },
    onImportBackupJson: (String) -> Result<Int> = { Result.success(0) },
    onBackupMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val payload = onExportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                }
                onBackupMessage("Backup saved! Keep this .json file safe before uninstalling or switching devices.")
            } catch (e: Exception) {
                onBackupMessage("Backup export failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    onImportBackupJson(text)
                } else {
                    onBackupMessage("Selected backup file is empty.")
                }
            } catch (e: Exception) {
                onBackupMessage("Backup restore failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Official ISCEB Brand Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_isceb_logo),
                        contentDescription = "ISCEB - Connect. learn. Achieve.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(86.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ISCEB • Official Academic & Faculty Directorate",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldDark,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        // User Profile Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isAdmin || currentUser.role == UserRole.ADMIN) {
                        val isFounderAccount = currentUser.id == "admin-2"
                        val customFile = (if (isFounderAccount) founderProfile.photoUri else developerProfile.photoUri)
                            .takeIf { it.isNotBlank() }
                            ?.let { File(it) }
                            ?.takeIf { it.exists() }
                        val fallbackRes = if (isFounderAccount) R.drawable.img_admin_amirul else R.drawable.img_admin_nafiul
                        val photoDesc = if (isFounderAccount) {
                            "Md. Amirul Islam - Founder of ISCEB"
                        } else {
                            "Md. Nafiul Islam Nahin - Developer of ISCEB App"
                        }
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(2.dp, GoldMetallic, CircleShape)
                                .background(Color.White)
                                .testTag("img_admin_profile_photo"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customFile != null) {
                                AsyncImage(
                                    model = customFile,
                                    contentDescription = photoDesc,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alignment = BiasAlignment(0f, -0.35f)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = fallbackRes),
                                    contentDescription = photoDesc,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alignment = BiasAlignment(0f, -0.35f)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(GoldMetallic),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User Avatar",
                                tint = NavyPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentUser.fullName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = currentUser.designation.ifBlank { currentUser.organization } +
                            if (currentUser.additionalRole.isNotBlank()) " • ${currentUser.additionalRole}" else "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = GoldSoft,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = buildString {
                            append(currentUser.email)
                            if (currentUser.mobileNumber.isNotBlank()) {
                                append(" • Mobile: ${currentUser.mobileNumber}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.5.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavySurface)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isAdmin) "⚡ ROLE: ADMINISTRATOR (GOVERNANCE & UPLOADS)" else "👤 ROLE: VERIFIED SCM STUDENT",
                            color = if (isAdmin) GoldMetallic else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Google Cloud Auto-Sync Account Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("card_profile_cloud_sync"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (cloudAuthEmail != null) Color(0xFFDCFCE7) else Color(0xFFEFF3F8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (cloudAuthEmail != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (cloudAuthEmail != null) SuccessGreen else NavyPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google Cloud Auto-Sync",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                )
                                Text(
                                    text = if (cloudAuthEmail != null) {
                                        "Connected: $cloudAuthEmail • App v${appUpdateInfo.currentAppVersionName} (${appUpdateInfo.currentAppVersionCode})"
                                    } else {
                                        "Sign in with Google for automatic live syllabus & progress sync across phones"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7F96), fontSize = 10.5.sp)
                                )
                            }
                        }

                        if (cloudAuthEmail != null) {
                            OutlinedButton(
                                onClick = onDisconnectGoogleCloudClick,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_profile_signout_cloud")
                            ) {
                                Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                            }
                        } else {
                            Button(
                                onClick = onConnectGoogleCloudClick,
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_profile_connect_google")
                            ) {
                                Text("Sign in with Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Dedicated In-App APK Update Center Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("card_profile_app_update"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (appUpdateInfo.isBinaryUpdateRequired) GoldMetallic else NavyPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = if (appUpdateInfo.isBinaryUpdateRequired) NavyDark else GoldMetallic,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "App Updates & Online APK",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                )
                                Text(
                                    text = "Installed: v${appUpdateInfo.currentAppVersionName} (${appUpdateInfo.currentAppVersionCode}) • Latest: v${appUpdateInfo.latestAvailableApkVersionName} (${appUpdateInfo.latestAvailableApkVersionCode})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (appUpdateInfo.isBinaryUpdateRequired) GoldDark else Color(0xFF6B7F96),
                                        fontSize = 10.5.sp,
                                        fontWeight = if (appUpdateInfo.isBinaryUpdateRequired) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                val portalUrl = appUpdateInfo.downloadPageUrl.ifBlank { AppUpdateConfig.DEFAULT_DOWNLOAD_PAGE_URL }
                                onOpenWebDownloadPage(portalUrl)
                            },
                            modifier = Modifier.testTag("btn_profile_open_download_page")
                        ) {
                            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Web Page", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }

                    if (appUpdateInfo.releaseNotes.isNotBlank()) {
                        Text(
                            text = "Release Notes: ${appUpdateInfo.releaseNotes}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 15.sp
                        )
                    }

                    when (val dlState = apkDownloadState) {
                        is ApkDownloadState.Checking -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NavyPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking update server...", fontSize = 11.sp, color = NavyPrimary)
                            }
                        }
                        is ApkDownloadState.Downloading -> {
                            val pct = if (dlState.progressFraction >= 0f) "${(dlState.progressFraction * 100).toInt()}%" else "..."
                            val bytesStr = if (dlState.totalBytes > 0L) {
                                "${ApkUpdateManager.formatByteSize(dlState.downloadedBytes)} / ${ApkUpdateManager.formatByteSize(dlState.totalBytes)}"
                            } else {
                                ApkUpdateManager.formatByteSize(dlState.downloadedBytes)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Downloading APK...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                Text("$bytesStr ($pct)", fontSize = 10.5.sp, color = GoldDark, fontWeight = FontWeight.Bold)
                            }
                            if (dlState.progressFraction >= 0f) {
                                LinearProgressIndicator(
                                    progress = { dlState.progressFraction },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = GoldMetallic,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = GoldMetallic,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }
                            TextButton(onClick = onCancelApkDownload, modifier = Modifier.align(Alignment.End)) {
                                Text("Cancel", color = DangerRed, fontSize = 11.sp)
                            }
                        }
                        is ApkDownloadState.Verifying -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = GoldDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying APK SHA-256 checksum...", fontSize = 11.sp, color = NavyPrimary)
                            }
                        }
                        is ApkDownloadState.ReadyToInstall -> {
                            Button(
                                onClick = onInstallDownloadedApk,
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_profile_install_apk")
                            ) {
                                Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Install Verified v${dlState.versionName}", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                        }
                        is ApkDownloadState.Error -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dlState.message, fontSize = 10.5.sp, color = DangerRed)
                            }
                            if (dlState.canRetry) {
                                Button(
                                    onClick = onDownloadLatestApk,
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_profile_retry_apk")
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        is ApkDownloadState.Idle -> {
                            if (appUpdateInfo.lastCheckError != null) {
                                Text(
                                    text = appUpdateInfo.lastCheckError,
                                    fontSize = 10.sp,
                                    color = DangerRed
                                )
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
                                modifier = Modifier.weight(1f).testTag("btn_profile_check_updates")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check Updates", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                            Button(
                                onClick = onDownloadLatestApk,
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_profile_download_apk")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download Latest", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportBackupLauncher.launch("ISCEB-backup-v${appUpdateInfo.currentAppVersionName}.json") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_profile_export_backup")
                        ) {
                            Text("Export Backup", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                        OutlinedButton(
                            onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_profile_import_backup")
                        ) {
                            Text("Restore Backup", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GoldDark)
                        }
                    }
                }
            }
        }

        // Student Account Switcher (For testing multi-student progress isolation)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SwitchAccount, contentDescription = null, tint = GoldDark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Switch Student Profile",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Switching student profiles isolates individual class completion, active study times, and issued certificates in persistent storage.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B), fontSize = 11.sp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableUsers.filter { it.role == UserRole.STUDENT }) { user ->
                            val isSel = user.id == currentUser.id
                            FilterChip(
                                selected = isSel,
                                onClick = { onSwitchUser(user.id) },
                                label = {
                                    Text(
                                        text = "${user.fullName} (Student)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyPrimary,
                                    selectedLabelColor = GoldMetallic,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = NavyPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Administrator Mode Toggle Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAdmin) GoldMetallic else Color(0xFFEFF3F8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isAdmin) NavyPrimary else Color(0xFF6B7F96),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Administrator Management Mode",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                )
                                Text(
                                    text = if (isAdmin) "Active: Edit modules, manage rules & revoke certs" else "Protected with per-admin cryptographic authentication",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7F96), fontSize = 11.sp)
                                )
                            }
                        }

                        if (isAdmin) {
                            Button(
                                onClick = onAdminLogoutClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = DangerRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Logout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onAdminLoginClick,
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_admin_login_profile")
                            ) {
                                Text("Login", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // System Settings & Actions
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Institutional Registry & Settings",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                )

                // ISCEB Leadership & Administration Panel Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_isceb_administration"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NavyPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ISCEB Leadership & Admin Panel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = "Full Administrator Privileges & Curriculum Governance",
                                    fontSize = 10.5.sp,
                                    color = GoldDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Founder Profile Card: Md. Amirul Islam
                        val founderPhotoFile = founderProfile.photoUri.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgLight)
                                .border(1.dp, Color(0xFFD6E2F0), RoundedCornerShape(10.dp))
                                .clickable { onOpenFounderProfile() }
                                .padding(12.dp)
                                .testTag("card_profile_founder"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, GoldMetallic, CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (founderPhotoFile != null) {
                                    AsyncImage(
                                        model = founderPhotoFile,
                                        contentDescription = founderProfile.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, -0.35f)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_admin_amirul),
                                        contentDescription = "${founderProfile.name} - ${founderProfile.designation}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, -0.35f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = founderProfile.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = "${founderProfile.designation} • ${founderProfile.additionalRole}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp,
                                    color = GoldDark
                                )
                                Text(
                                    text = founderProfile.professionalIdentity,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.5.sp,
                                    color = NavySecondary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = GoldDark, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Mobile: ${founderProfile.mobileNumber} • Tap for Founder Profile",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "View Founder Profile", tint = GoldDark)
                        }

                        // Developer Profile Card: Md. Nafiul Islam Nahin
                        val devPhotoFile = developerProfile.photoUri.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgLight)
                                .border(1.dp, Color(0xFFD6E2F0), RoundedCornerShape(10.dp))
                                .clickable { onOpenDeveloperProfile() }
                                .padding(12.dp)
                                .testTag("card_profile_developer"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, GoldMetallic, CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (devPhotoFile != null) {
                                    AsyncImage(
                                        model = devPhotoFile,
                                        contentDescription = developerProfile.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, -0.35f)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_admin_nafiul),
                                        contentDescription = "${developerProfile.name} - ${developerProfile.designation}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, -0.35f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = developerProfile.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = developerProfile.designation,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = GoldDark
                                )
                                Text(
                                    text = "Tap to view full Developer Profile",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "View Developer Profile", tint = GoldDark)
                        }

                        // ISCEB Members Directory Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NavyPrimary)
                                .clickable { onOpenMembers() }
                                .padding(12.dp)
                                .testTag("card_profile_members"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(GoldMetallic),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "ISCEB Members Directory",
                                    tint = NavyDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ISCEB Members Directory ($membersCount)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAdmin) {
                                        "View members • Add, edit, upload photos & reorder"
                                    } else {
                                        "Browse ISCEB board leadership & member profiles"
                                    },
                                    fontSize = 10.sp,
                                    color = GoldSoft
                                )
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open ISCEB Members", tint = GoldMetallic)
                        }
                    }
                }

                // ISCEB Academic Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NavyPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.School, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ISCEB",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = "Connect. learn. Achieve. • www.isceb.org",
                                    fontSize = 11.sp,
                                    color = GoldDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Share & Install Card (100% English)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavyPrimary)
                                .clickable { onOpenPhoneInstall() }
                                .padding(10.dp)
                                .testTag("btn_profile_phone_install")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GoldMetallic),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Share, Install & Cloud Sync (APK & OTA)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Wireless phone install guide & live syllabus sync",
                                        fontSize = 10.sp,
                                        color = GoldSoft
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = GoldMetallic,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgLight, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "• Program: Post Graduate Diploma (PGD) in Supply Chain Management",
                                    fontSize = 11.sp,
                                    color = NavyPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "• Structure: 12 Modules • 48 Classes • 24-Week Program",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "• Class Time: 9:00 PM to 11:00 PM (Asia/Dhaka default)",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "• Curricula: Part 1 Basic (28 Classes) + Part 2 Advance (20 Classes)",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "• Exams: Mod 1 (20M), Mod 2 (25M), Mod 3 (30M), Mod 6 (25M), Final Exam (100M)",
                                    fontSize = 11.sp,
                                    color = GoldDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Certificate Verification Registry
                ActionSettingItem(
                    icon = Icons.Default.Verified,
                    title = "Certificate Verification Registry",
                    subtitle = "Verify diploma authenticity by unique identifier, inspect status, or audit revocations",
                    onClick = onOpenVerification
                )

                // Curriculum & Completion Governance Settings (Admin Only)
                if (isAdmin) {
                    ActionSettingItem(
                        icon = Icons.Default.Settings,
                        title = "Curriculum Governance & Completion Rules",
                        subtitle = "Configure min learning time, playback %, mandatory quizzes, and certificate signers",
                        onClick = onOpenSettings
                    )
                }

                // Cloud Services & Production Architecture
                ActionSettingItem(
                    icon = Icons.Default.Cloud,
                    title = "Firebase Cloud Setup & Storage Rules",
                    subtitle = "Firestore security rules, storage zero-trust policies, and environment config",
                    onClick = onOpenFirebaseGuide
                )

                // Security info
                ActionSettingItem(
                    icon = Icons.Default.Security,
                    title = "Security & Upload Limits",
                    subtitle = "50MB file validation per item, multi-type study attachments, input sanitization",
                    onClick = onOpenFirebaseGuide
                )

                // Native Android Delivery
                ActionSettingItem(
                    icon = Icons.Default.InstallMobile,
                    title = "Native Android & Cloud Auto-Sync",
                    subtitle = "In-place APK updates & live Firestore curriculum sync",
                    onClick = onOpenFirebaseGuide
                )

                // Reset Demo Data
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResetDemoData() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7F7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Reset Curriculum to Initial Demo State",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = DangerRed)
                                )
                                Text(
                                    text = "Restores 12 original modules, 48 classes, and clears test certificates",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF888888), fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF0F5FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = NavyPrimary))
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7F96), fontSize = 11.sp))
                }
            }

            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFB0C0D0), modifier = Modifier.size(18.dp))
        }
    }
}
