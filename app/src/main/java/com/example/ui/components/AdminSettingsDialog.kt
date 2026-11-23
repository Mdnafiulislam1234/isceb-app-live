package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.example.ui.icons.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import com.example.ui.icons.Campaign
import com.example.ui.icons.CloudSync
import com.example.ui.icons.CloudUpload
import androidx.compose.material.icons.filled.Settings
import com.example.ui.icons.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.AdminAccountStatus
import com.example.auth.AdminAuthManager
import com.example.auth.AdminAuthResult
import com.example.auth.AdminAuthSummary
import com.example.model.AdminCompletionSettings
import com.example.model.CourseModule
import com.example.model.CurriculumPublishManifest
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.SuccessGreen

@Composable
fun AdminSettingsDialog(
    initialSettings: AdminCompletionSettings,
    allModules: List<CourseModule>,
    currentManifest: CurriculumPublishManifest = CurriculumPublishManifest(),
    cloudAuthEmail: String? = null,
    activeAdminId: String = AdminAuthManager.ADMIN_AMIRUL_ID,
    adminSummaries: List<AdminAuthSummary> = emptyList(),
    latestInviteNotice: String? = null,
    latestRecoveryKeyNotice: String? = null,
    onGenerateCoAdminInvite: (String) -> Unit = {},
    onChangeOwnPassword: (String, String, String) -> AdminAuthResult = { _, _, _ -> AdminAuthResult.Failure("Not configured") },
    onDismissSecurityNotices: () -> Unit = {},
    onConnectGoogleAdminClick: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (AdminCompletionSettings) -> Unit,
    onFastTrackDemo: () -> Unit,
    onPublishToCloud: (versionTag: String, notes: String, announcement: String) -> Unit = { _, _, _ -> },
    onPublishToCloudWithApk: ((versionTag: String, notes: String, announcement: String, latestApkVersionCode: Int, latestApkVersionName: String, apkDownloadUrl: String) -> Unit)? = null,
    onOpenMembersManagement: () -> Unit = {}
) {
    var minTimeSec by remember { mutableLongStateOf(initialSettings.minLearningTimeSeconds) }
    var minPlayback by remember { mutableFloatStateOf(initialSettings.minPlaybackPercent) }
    var minPdfSec by remember { mutableLongStateOf(initialSettings.minPdfReadingSeconds) }
    var isQuizReq by remember { mutableStateOf(initialSettings.isQuizRequired) }
    var quizPassScore by remember { mutableStateOf(initialSettings.quizPassingScore.toString()) }

    var instName by remember { mutableStateOf(initialSettings.institutionName) }
    var officialWebsite by remember { mutableStateOf(initialSettings.officialWebsite) }
    var instAccred by remember { mutableStateOf(initialSettings.institutionAccreditation) }
    var leftSignerName by remember { mutableStateOf(initialSettings.leftSignerName) }
    var leftSignerTitle by remember { mutableStateOf(initialSettings.leftSignerTitle) }
    var signerName by remember { mutableStateOf(initialSettings.authorizedSignerName) }
    var signerTitle by remember { mutableStateOf(initialSettings.authorizedSignerTitle) }
    var certTitle by remember { mutableStateOf(initialSettings.certificateTitle) }
    var certificatesEnabled by remember { mutableStateOf(initialSettings.certificatesEnabled) }
    var demoCertificateEnabled by remember { mutableStateOf(initialSettings.demoCertificateEnabled) }
    var classTimeWindow by remember { mutableStateOf(initialSettings.scheduleConfig.classTimeWindow) }
    var timeZoneId by remember { mutableStateOf(initialSettings.scheduleConfig.timeZoneId) }
    var remoteSyncUrl by remember { mutableStateOf(initialSettings.remoteSyncManifestUrl) }
    var apkDownloadUrl by remember { mutableStateOf(initialSettings.apkDownloadUrl) }
    var autoApplyUpdates by remember { mutableStateOf(initialSettings.autoApplyContentUpdates) }

    var requiredModules by remember { mutableStateOf(initialSettings.requiredModuleIds.toMutableSet()) }
    var requiredExams by remember { mutableStateOf(initialSettings.requiredExamIds.toMutableSet()) }
    var requireFinalProjectGraded by remember { mutableStateOf(initialSettings.requireGradedFinalProject) }
    var requireAllPdfPages by remember { mutableStateOf(initialSettings.requirePdfAllPagesVisited) }

    var nextVersionTag by remember { mutableStateOf("v1.${(currentManifest.versionCode - 100) + 1}.0") }
    var changeNotes by remember { mutableStateOf("Updated lecture objectives, readings, and exam window timings.") }
    var broadcastAnnouncement by remember { mutableStateOf(currentManifest.announcement ?: "Classes Friday & Saturday 9:00 PM - 11:00 PM (Asia/Dhaka).") }
    var apkVersionCodeInput by remember { mutableStateOf(currentManifest.latestApkVersionCode.toString()) }
    var apkVersionNameInput by remember { mutableStateOf(currentManifest.latestApkVersionName) }
    var isPublishingSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
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
                            .background(NavyPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = GoldMetallic, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Curriculum Governance & Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live Cloud OTA Publishing Card
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_admin_cloud_publishing"),
                    colors = CardDefaults.cardColors(containerColor = NavyDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = GoldMetallic,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Live Cloud OTA Publishing",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GoldMetallic)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Live: ${currentManifest.versionTag}",
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Ordered ISCEB Admin Panel Leadership: 1st Md. Amirul Islam, 2nd Md. Nafiul Islam Nahin directly below
                        Column(
                            modifier = Modifier.fillMaxWidth().testTag("admin_leadership_order_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F2338))
                                    .border(1.dp, GoldMetallic.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.5.dp, GoldMetallic, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_admin_amirul),
                                        contentDescription = "Md. Amirul Islam",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, -0.35f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Md. Amirul Islam",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Founder of ISCEB • Lead Trainer of ISCEB",
                                        color = GoldSoft,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F2338))
                                    .border(1.dp, GoldMetallic.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.5.dp, GoldMetallic, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_admin_nafiul),
                                        contentDescription = "Md. Nafiul Islam Nahin",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, -0.35f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Md. Nafiul Islam Nahin",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Developer of ISCEB App",
                                        color = GoldSoft,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Publishing pushes updated modules, classes, settings & APK release metadata instantly to connected student devices via Firebase Cloud Firestore.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )

                        // Google Cloud Admin Auth Gate Status
                        val isAuthorizedAdminGoogle = cloudAuthEmail.equals("www.mdnafiulislam@gmail.com", ignoreCase = true) ||
                            cloudAuthEmail.equals("amirul.islam@isceb.org", ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isAuthorizedAdminGoogle -> Color(0xFF14532D)
                                        cloudAuthEmail != null -> Color(0xFF78350F)
                                        else -> Color(0xFF1E293B)
                                    }
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isAuthorizedAdminGoogle -> "Live Cloud Admin Connected ($cloudAuthEmail)"
                                        cloudAuthEmail != null -> "Signed in as $cloudAuthEmail"
                                        else -> "Google Sign-In required for Live Firestore Broadcast"
                                    },
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!isAuthorizedAdminGoogle) {
                                    Text(
                                        text = "Sign in with Google account www.mdnafiulislam@gmail.com",
                                        color = GoldSoft,
                                        fontSize = 9.5.sp
                                    )
                                }
                            }
                            if (!isAuthorizedAdminGoogle) {
                                TextButton(
                                    onClick = onConnectGoogleAdminClick,
                                    modifier = Modifier.testTag("btn_connect_google_admin")
                                ) {
                                    Text(
                                        text = "Google Sign-In",
                                        color = GoldMetallic,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = nextVersionTag,
                            onValueChange = { nextVersionTag = it },
                            label = { Text("Publish Version Tag", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_publish_version_tag"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldMetallic,
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = GoldMetallic,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )

                        OutlinedTextField(
                            value = changeNotes,
                            onValueChange = { changeNotes = it },
                            label = { Text("Curriculum Change Notes", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("input_publish_change_notes"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldMetallic,
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = GoldMetallic,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )

                        OutlinedTextField(
                            value = broadcastAnnouncement,
                            onValueChange = { broadcastAnnouncement = it },
                            label = { Text("Broadcast Student Announcement", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("input_publish_announcement"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldMetallic,
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = GoldMetallic,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = apkVersionCodeInput,
                                onValueChange = { apkVersionCodeInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("APK Build #", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("input_publish_apk_code"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldMetallic,
                                    unfocusedBorderColor = Color(0xFF475569),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = GoldMetallic,
                                    unfocusedLabelColor = Color(0xFF94A3B8)
                                )
                            )
                            OutlinedTextField(
                                value = apkVersionNameInput,
                                onValueChange = { apkVersionNameInput = it },
                                label = { Text("APK Version", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("input_publish_apk_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldMetallic,
                                    unfocusedBorderColor = Color(0xFF475569),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = GoldMetallic,
                                    unfocusedLabelColor = Color(0xFF94A3B8)
                                )
                            )
                        }

                        OutlinedTextField(
                            value = apkDownloadUrl,
                            onValueChange = { apkDownloadUrl = it },
                            label = { Text("APK Download / Release URL", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_apk_download_url"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldMetallic,
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = GoldMetallic,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )

                        Button(
                            onClick = {
                                val parsedCode = apkVersionCodeInput.toIntOrNull() ?: currentManifest.latestApkVersionCode
                                val resolvedName = apkVersionNameInput.trim().ifBlank { currentManifest.latestApkVersionName }
                                if (onPublishToCloudWithApk != null) {
                                    onPublishToCloudWithApk(
                                        nextVersionTag,
                                        changeNotes,
                                        broadcastAnnouncement,
                                        parsedCode,
                                        resolvedName,
                                        apkDownloadUrl.trim()
                                    )
                                } else {
                                    onPublishToCloud(nextVersionTag, changeNotes, broadcastAnnouncement)
                                }
                                isPublishingSuccess = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic, contentColor = NavyDark),
                            modifier = Modifier.fillMaxWidth().testTag("btn_publish_curriculum_cloud")
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Publish Curriculum & Update (OTA)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (isPublishingSuccess) {
                            Text(
                                text = if (isAuthorizedAdminGoogle) {
                                    "✅ Submitted to live Cloud Firestore & local manifest."
                                } else {
                                    "ℹ️ Saved locally. Sign in with www.mdnafiulislam@gmail.com via Google Sign-In to push live to other devices."
                                },
                                color = if (isAuthorizedAdminGoogle) SuccessGreen else GoldSoft,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Admin Security, Co-Admin Invite & Password Rotation Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_admin_security_panel"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6E2F0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val activeAdmin = adminSummaries.find { it.adminId == activeAdminId }
                    val coAdmin = adminSummaries.firstOrNull { it.adminId != activeAdminId }
                    var currentPw by remember { mutableStateOf("") }
                    var newPw by remember { mutableStateOf("") }
                    var confirmPw by remember { mutableStateOf("") }
                    var pwFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Admin Security & Co-Admin Onboarding",
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Signed in as: ${activeAdmin?.fullName ?: "Administrator"}",
                            fontSize = 11.sp,
                            color = GoldDark,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (coAdmin != null && coAdmin.status == AdminAccountStatus.PENDING_SETUP) {
                            Button(
                                onClick = { onGenerateCoAdminInvite(coAdmin.adminId) },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_generate_coadmin_invite")
                            ) {
                                Text("Generate Single-Use Invite for ${coAdmin.fullName}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!latestInviteNotice.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF9C3))
                                    .padding(8.dp)
                            ) {
                                Text(latestInviteNotice, fontSize = 10.5.sp, color = NavyDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!latestRecoveryKeyNotice.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFECFDF5))
                                    .padding(8.dp)
                            ) {
                                Text(latestRecoveryKeyNotice, fontSize = 10.5.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!latestInviteNotice.isNullOrBlank() || !latestRecoveryKeyNotice.isNullOrBlank()) {
                            TextButton(onClick = onDismissSecurityNotices) {
                                Text("Clear Security Notice", fontSize = 10.sp)
                            }
                        }

                        Text(
                            text = "Rotate My Admin Password",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = NavyPrimary
                        )
                        OutlinedTextField(
                            value = currentPw,
                            onValueChange = { currentPw = it; pwFeedback = null },
                            label = { Text("Current Password", fontSize = 10.5.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth().testTag("input_change_current_pw"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                        )
                        OutlinedTextField(
                            value = newPw,
                            onValueChange = { newPw = it; pwFeedback = null },
                            label = { Text("New Strong Password", fontSize = 10.5.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth().testTag("input_change_new_pw"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                        )
                        OutlinedTextField(
                            value = confirmPw,
                            onValueChange = { confirmPw = it; pwFeedback = null },
                            label = { Text("Confirm New Password", fontSize = 10.5.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth().testTag("input_change_confirm_pw"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                        )
                        Button(
                            onClick = {
                                when (val res = onChangeOwnPassword(currentPw, newPw, confirmPw)) {
                                    is AdminAuthResult.Success -> {
                                        currentPw = ""
                                        newPw = ""
                                        confirmPw = ""
                                        pwFeedback = true to res.message
                                    }
                                    is AdminAuthResult.Failure -> {
                                        pwFeedback = false to res.errorMessage
                                    }
                                    else -> Unit
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavySecondary, contentColor = GoldMetallic),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_save_changed_password")
                        ) {
                            Text("Update Admin Password", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        pwFeedback?.let { (ok, msg) ->
                            Text(
                                text = msg,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (ok) SuccessGreen else DangerRed
                            )
                        }
                    }
                }

                // Quick Tester Helper
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF9C3))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reviewer Testing Tool", fontWeight = FontWeight.Bold, color = Color(0xFF854D0E), fontSize = 11.sp)
                            Text("Fast-track student to 100% completion & quiz pass to instantly test certificate generation.", fontSize = 10.sp, color = Color(0xFF713F12))
                        }
                        Button(
                            onClick = onFastTrackDemo,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF854D0E), contentColor = Color.White),
                            modifier = Modifier.testTag("btn_fast_track_completion")
                        ) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fast Track", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quick ISCEB Members Directory Management shortcut
                Button(
                    onClick = onOpenMembersManagement,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_admin_manage_members")
                ) {
                    Text("Manage ISCEB Members & Profiles", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Text(
                    text = "1. Class Completion Criteria Rules",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                )

                // Minimum active learning time
                Column {
                    Text(
                        text = "Minimum Required Active Learning Time: $minTimeSec seconds",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                    )
                    Slider(
                        value = minTimeSec.toFloat(),
                        onValueChange = { minTimeSec = it.toLong() },
                        valueRange = 10f..300f,
                        steps = 28,
                        colors = SliderDefaults.colors(thumbColor = GoldMetallic, activeTrackColor = GoldMetallic)
                    )
                    Text("Paused or backgrounded time is strictly excluded.", fontSize = 10.sp, color = Color.Gray)
                }

                // Minimum playback percentage
                Column {
                    Text(
                        text = "Minimum Audio/Video Playback: ${minPlayback.toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                    )
                    Slider(
                        value = minPlayback,
                        onValueChange = { minPlayback = it },
                        valueRange = 50f..100f,
                        steps = 9,
                        colors = SliderDefaults.colors(thumbColor = GoldMetallic, activeTrackColor = GoldMetallic)
                    )
                }

                // Minimum PDF reading time
                Column {
                    Text(
                        text = "Minimum PDF Reading Time: $minPdfSec seconds",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                    )
                    Slider(
                        value = minPdfSec.toFloat(),
                        onValueChange = { minPdfSec = it.toLong() },
                        valueRange = 10f..180f,
                        steps = 16,
                        colors = SliderDefaults.colors(thumbColor = GoldMetallic, activeTrackColor = GoldMetallic)
                    )
                }

                // Mandatory Quiz Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mandatory Quiz Requirement", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NavyPrimary)
                        Text("Students must pass class assessment to mark complete", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = isQuizReq,
                        onCheckedChange = { isQuizReq = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldMetallic, checkedTrackColor = NavyPrimary)
                    )
                }

                if (isQuizReq) {
                    OutlinedTextField(
                        value = quizPassScore,
                        onValueChange = { quizPassScore = it },
                        label = { Text("Passing Score Threshold (%)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "2. Certificate & Institutional Template",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Official Certificates", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NavyPrimary)
                        Text("Toggle PGD diploma generation across students", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = certificatesEnabled,
                        onCheckedChange = { certificatesEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldMetallic, checkedTrackColor = NavyPrimary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Demo Certificate Preview", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NavyPrimary)
                        Text("Allow previewing and downloading sample PGD PDF certificate", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = demoCertificateEnabled,
                        onCheckedChange = { demoCertificateEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldMetallic, checkedTrackColor = NavyPrimary)
                    )
                }

                OutlinedTextField(
                    value = certTitle,
                    onValueChange = { certTitle = it },
                    label = { Text("Certificate Diploma Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = instName,
                    onValueChange = { instName = it },
                    label = { Text("Institution Code (Displayed as Institute of Supply Chain Excellence Bangladesh)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = officialWebsite,
                    onValueChange = { officialWebsite = it },
                    label = { Text("Official Institution Website") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = classTimeWindow,
                    onValueChange = { classTimeWindow = it },
                    label = { Text("Class Schedule Hours") },
                    placeholder = { Text("e.g. 9:00 PM to 11:00 PM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = timeZoneId,
                    onValueChange = { timeZoneId = it },
                    label = { Text("Class Timezone") },
                    placeholder = { Text("e.g. Asia/Dhaka") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = instAccred,
                    onValueChange = { instAccred = it },
                    label = { Text("Accreditation / Standard Statement") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = leftSignerName,
                    onValueChange = { leftSignerName = it },
                    label = { Text("Left Signatory Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = leftSignerTitle,
                    onValueChange = { leftSignerTitle = it },
                    label = { Text("Left Signatory Designation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = signerName,
                    onValueChange = { signerName = it },
                    label = { Text("Right Signatory Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = signerTitle,
                    onValueChange = { signerTitle = it },
                    label = { Text("Right Signatory Designation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "3. Live Sync & Remote Update Delivery",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                )

                OutlinedTextField(
                    value = remoteSyncUrl,
                    onValueChange = { remoteSyncUrl = it },
                    label = { Text("HTTPS Update Manifest (version.json / GitHub JSON)") },
                    placeholder = { Text("https://mdnafiulislam1234.github.io/isceb-app-live/version.json") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_remote_sync_manifest_url"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Apply Cloud Content Updates", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NavyPrimary)
                        Text("Automatically synchronize modules & notices unless an exam or upload is active", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = autoApplyUpdates,
                        onCheckedChange = { autoApplyUpdates = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldMetallic, checkedTrackColor = NavyPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "4. Required Modules for Diploma (${requiredModules.size}/${allModules.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                )

                allModules.forEach { mod ->
                    val isChecked = requiredModules.contains(mod.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) requiredModules.remove(mod.id) else requiredModules.add(mod.id)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) requiredModules.add(mod.id) else requiredModules.remove(mod.id)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = NavyPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Module ${mod.number}: ${mod.title}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                color = NavyPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "5. Certification Exam & Project Rules",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require Graded Capstone Project", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NavyPrimary)
                        Text("Diploma requires final project graded, not only submitted", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = requireFinalProjectGraded,
                        onCheckedChange = { requireFinalProjectGraded = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldMetallic, checkedTrackColor = NavyPrimary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require All PDF Pages Visited", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NavyPrimary)
                        Text("Students must flip through every page before confirming PDF read", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = requireAllPdfPages,
                        onCheckedChange = { requireAllPdfPages = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldMetallic, checkedTrackColor = NavyPrimary)
                    )
                }

                listOf(
                    "exam-mod-1" to "Module 1 Exam (20 Marks)",
                    "exam-mod-2" to "Module 2 Exam (25 Marks)",
                    "exam-mod-3" to "Module 3 Exam (30 Marks)",
                    "exam-mod-6" to "Module 6 Exam (25 Marks)",
                    "exam-final" to "Final Comprehensive Exam (100 Marks)"
                ).forEach { (examId, label) ->
                    val checkedExam = requiredExams.contains(examId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                requiredExams = requiredExams.toMutableSet().apply {
                                    if (checkedExam) remove(examId) else add(examId)
                                }
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkedExam,
                            onCheckedChange = { on ->
                                requiredExams = requiredExams.toMutableSet().apply {
                                    if (on) add(examId) else remove(examId)
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = NavyPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, fontSize = 12.sp, color = NavyPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val passInt = quizPassScore.toIntOrNull() ?: 70
                    val newSettings = initialSettings.copy(
                        minLearningTimeSeconds = minTimeSec,
                        minPlaybackPercent = minPlayback,
                        minPdfReadingSeconds = minPdfSec,
                        isQuizRequired = isQuizReq,
                        quizPassingScore = passInt,
                        requiredModuleIds = requiredModules.toSet(),
                        requiredExamIds = requiredExams.toSet(),
                        requiredAssignmentIds = initialSettings.requiredAssignmentIds.ifEmpty { setOf("asg-12-final-project") },
                        requireGradedFinalProject = requireFinalProjectGraded,
                        requirePdfAllPagesVisited = requireAllPdfPages,
                        institutionName = instName.trim().ifEmpty { initialSettings.institutionName },
                        officialWebsite = officialWebsite.trim().ifEmpty { initialSettings.officialWebsite },
                        institutionAccreditation = instAccred.trim().ifEmpty { initialSettings.institutionAccreditation },
                        leftSignerName = leftSignerName.trim().ifEmpty { "Hasina Khan" },
                        leftSignerTitle = leftSignerTitle.trim().ifEmpty { "Chairman & CEO" },
                        authorizedSignerName = signerName.trim().ifEmpty { "Amirul Islam" },
                        authorizedSignerTitle = signerTitle.trim().ifEmpty { "Founder of ISCEB" },
                        certificateTitle = certTitle.trim().ifEmpty { initialSettings.certificateTitle },
                        certificatesEnabled = certificatesEnabled,
                        demoCertificateEnabled = demoCertificateEnabled,
                        scheduleConfig = initialSettings.scheduleConfig.copy(
                            classTimeWindow = classTimeWindow.trim().ifEmpty { "9:00 PM to 11:00 PM" },
                            timeZoneId = timeZoneId.trim().ifEmpty { "Asia/Dhaka" }
                        ),
                        remoteSyncManifestUrl = remoteSyncUrl.trim(),
                        apkDownloadUrl = apkDownloadUrl.trim().ifEmpty { initialSettings.apkDownloadUrl },
                        autoApplyContentUpdates = autoApplyUpdates
                    )
                    onSave(newSettings)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                modifier = Modifier.testTag("btn_save_admin_settings")
            ) {
                Text("Save Governance Rules", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
