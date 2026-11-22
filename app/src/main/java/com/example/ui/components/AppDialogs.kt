package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import com.example.ui.icons.AdminPanelSettings
import com.example.ui.icons.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import com.example.ui.icons.Cloud
import com.example.ui.icons.CloudUpload
import androidx.compose.material.icons.filled.Delete
import com.example.ui.icons.Error
import androidx.compose.material.icons.filled.Info
import com.example.ui.icons.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.data.CloudSyncRepository
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.auth.AdminAccountStatus
import com.example.auth.AdminAuthManager
import com.example.auth.AdminAuthResult
import com.example.auth.AdminAuthSummary
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.MediaType
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SuccessGreen
import com.example.util.MediaFileManager
import com.example.util.PickedFileMeta

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.model.PendingUploadItem

private enum class AdminAuthMode { SIGN_IN, SETUP, RECOVER }

private fun defaultFallbackAdminSummaries(): List<AdminAuthSummary> = listOf(
    AdminAuthSummary(
        adminId = AdminAuthManager.ADMIN_AMIRUL_ID,
        fullName = "Md. Amirul Islam",
        designation = "Founder of ISCEB",
        additionalRole = "Lead Trainer of ISCEB",
        organization = "Head of Supply Chain Management at BRB Group",
        email = "amirul.islam@isceb.org",
        mobileNumber = "01715-282905",
        status = AdminAccountStatus.PENDING_SETUP,
        hasPendingSetupInvite = false,
        failedAttempts = 0,
        lockoutRemainingSeconds = 0L
    ),
    AdminAuthSummary(
        adminId = AdminAuthManager.ADMIN_NAFIUL_ID,
        fullName = "Md. Nafiul Islam Nahin",
        designation = "Developer of ISCEB App",
        additionalRole = "",
        organization = "ISCEB App Engineering",
        email = "www.mdnafiulislam@gmail.com",
        mobileNumber = "",
        status = AdminAccountStatus.PENDING_SETUP,
        hasPendingSetupInvite = false,
        failedAttempts = 0,
        lockoutRemainingSeconds = 0L
    )
)

// ======================= ADMIN LOGIN & ONBOARDING DIALOG =======================
@Composable
fun AdminLoginDialog(
    adminSummaries: List<AdminAuthSummary> = defaultFallbackAdminSummaries(),
    isAnyAdminConfigured: Boolean = false,
    cloudAuthEmail: String? = null,
    recoveryKeyNotice: String? = null,
    onDismiss: () -> Unit,
    onSignIn: (adminId: String, password: String) -> AdminAuthResult = { _, _ -> AdminAuthResult.Failure("Not configured") },
    onSetupPassword: (adminId: String, newPassword: String, confirmPassword: String, inviteToken: String) -> AdminAuthResult = { _, _, _, _ -> AdminAuthResult.Failure("Not configured") },
    onResetPassword: (adminId: String, recoveryCode: String, newPassword: String, confirmPassword: String) -> AdminAuthResult = { _, _, _, _ -> AdminAuthResult.Failure("Not configured") },
    onTriggerGoogleSignIn: () -> Unit = {},
    onDismissRecoveryNotice: () -> Unit = {}
) {
    val orderedAdmins = remember(adminSummaries) {
        val base = if (adminSummaries.isNotEmpty()) adminSummaries else defaultFallbackAdminSummaries()
        listOfNotNull(
            base.find { it.adminId == AdminAuthManager.ADMIN_AMIRUL_ID },
            base.find { it.adminId == AdminAuthManager.ADMIN_NAFIUL_ID }
        ).ifEmpty { base }
    }

    var selectedAdminId by remember { mutableStateOf(AdminAuthManager.ADMIN_AMIRUL_ID) }
    val selectedSummary = orderedAdmins.find { it.adminId == selectedAdminId } ?: orderedAdmins.first()

    var mode by remember(selectedAdminId, selectedSummary.status) {
        mutableStateOf(
            if (selectedSummary.status == AdminAccountStatus.ACTIVE) AdminAuthMode.SIGN_IN else AdminAuthMode.SETUP
        )
    }

    // Security guarantee: never prefill passwords or autofill upon selecting an admin card.
    var password by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var inviteToken by remember { mutableStateOf("") }
    var recoveryCode by remember { mutableStateOf("") }
    var authError by remember(selectedAdminId, mode) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = GoldMetallic,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Admin Authentication",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_isceb_logo),
                        contentDescription = "ISCEB - Connect. learn. Achieve.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .testTag("img_admin_dialog_logo"),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Select Administrator Account",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                orderedAdmins.forEach { admin ->
                    val isSelected = selectedAdminId == admin.adminId
                    val isFounder = admin.adminId == AdminAuthManager.ADMIN_AMIRUL_ID
                    val drawableRes = if (isFounder) R.drawable.img_admin_amirul else R.drawable.img_admin_nafiul
                    val testTagName = if (isFounder) "card_select_admin_2" else "card_select_admin_1"
                    val isConfigured = admin.status == AdminAccountStatus.ACTIVE

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) GoldMetallic else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedAdminId = admin.adminId
                                password = ""
                                newPassword = ""
                                confirmPassword = ""
                                inviteToken = ""
                                recoveryCode = ""
                                authError = null
                            }
                            .padding(8.dp)
                            .testTag(testTagName),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.5.dp, GoldMetallic, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = drawableRes),
                                contentDescription = "${admin.fullName} - ${admin.designation}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alignment = BiasAlignment(0f, -0.35f)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = admin.fullName,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    fontSize = 12.5.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isConfigured) Color(0xFFDCFCE7) else Color(0xFFFEF3C7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isConfigured) "ACTIVE" else "SETUP",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isConfigured) SuccessGreen else GoldDark,
                                        fontSize = 8.5.sp
                                    )
                                }
                            }
                            Text(
                                text = buildString {
                                    append(admin.designation)
                                    if (admin.additionalRole.isNotBlank()) append(" • ${admin.additionalRole}")
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = GoldDark,
                                fontSize = 10.sp
                            )
                            Text(
                                text = admin.organization,
                                color = Color(0xFF475569),
                                fontSize = 9.5.sp
                            )
                        }
                    }
                }

                if (!recoveryKeyNotice.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF9C3))
                            .border(1.dp, GoldMetallic, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = recoveryKeyNotice,
                                color = NavyDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                            TextButton(onClick = onDismissRecoveryNotice) {
                                Text("Dismiss", fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                when (mode) {
                    AdminAuthMode.SIGN_IN -> {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                authError = null
                            },
                            label = { Text("Administrator Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = GoldDark)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_admin_passcode"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldMetallic,
                                unfocusedBorderColor = Color(0xFFCFD9E5)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    mode = AdminAuthMode.RECOVER
                                    authError = null
                                },
                                modifier = Modifier.testTag("btn_admin_forgot_password")
                            ) {
                                Text("Forgot / Reset Password?", fontSize = 11.sp, color = GoldDark, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    AdminAuthMode.SETUP -> {
                        val needsInvite = isAnyAdminConfigured
                        Text(
                            text = if (needsInvite) {
                                "Second Admin Onboarding: Enter the single-use invite code generated by the active admin plus your private strong password."
                            } else {
                                "First-Time Admin Setup: Create a strong private password (min 8 chars with uppercase, lowercase, digit & special symbol)."
                            },
                            color = Color(0xFF334155),
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (needsInvite) {
                            OutlinedTextField(
                                value = inviteToken,
                                onValueChange = {
                                    inviteToken = it
                                    authError = null
                                },
                                label = { Text("Single-Use Setup Invite Code") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_admin_invite_token"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                authError = null
                            },
                            label = { Text("New Strong Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = GoldDark)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_admin_new_password"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                authError = null
                            },
                            label = { Text("Confirm Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_admin_confirm_password"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                        )
                    }

                    AdminAuthMode.RECOVER -> {
                        Text(
                            text = "Verified Password Reset: Sign in with ${selectedSummary.email} via Google OR enter your one-time recovery key.",
                            color = Color(0xFF334155),
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onTriggerGoogleSignIn,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (cloudAuthEmail.equals(selectedSummary.email, ignoreCase = true)) {
                                    "Google Verified: $cloudAuthEmail"
                                } else {
                                    "Verify via Google Sign-In"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = recoveryCode,
                            onValueChange = {
                                recoveryCode = it
                                authError = null
                            },
                            label = { Text("One-Time Recovery Key (if not using Google)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_admin_recovery_code"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                authError = null
                            },
                            label = { Text("New Strong Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_admin_reset_new_password"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                authError = null
                            },
                            label = { Text("Confirm New Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_admin_reset_confirm_password"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                        )
                        TextButton(onClick = { mode = AdminAuthMode.SIGN_IN; authError = null }) {
                            Text("Back to Password Sign In", fontSize = 11.sp, color = NavyPrimary)
                        }
                    }
                }

                if (!authError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, DangerRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .testTag("text_admin_auth_error")
                    ) {
                        Text(
                            text = authError.orEmpty(),
                            color = DangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = when (mode) {
                        AdminAuthMode.SIGN_IN -> onSignIn(selectedAdminId, password)
                        AdminAuthMode.SETUP -> onSetupPassword(selectedAdminId, newPassword, confirmPassword, inviteToken)
                        AdminAuthMode.RECOVER -> onResetPassword(selectedAdminId, recoveryCode, newPassword, confirmPassword)
                    }
                    if (result is AdminAuthResult.Failure) {
                        authError = result.errorMessage
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyPrimary,
                    contentColor = GoldMetallic
                ),
                modifier = Modifier.testTag("btn_confirm_admin_login")
            ) {
                Text(
                    text = when (mode) {
                        AdminAuthMode.SIGN_IN -> "Login as Admin"
                        AdminAuthMode.SETUP -> "Create Admin Password"
                        AdminAuthMode.RECOVER -> "Reset & Login"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF6B7F96))
            }
        }
    )
}

// ======================= CREATE / EDIT MODULE DIALOG =======================
@Composable
fun CreateEditModuleDialog(
    initialModule: CourseModule?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, category: String, hours: Int) -> Unit
) {
    var title by remember { mutableStateOf(initialModule?.title ?: "") }
    var description by remember { mutableStateOf(initialModule?.description ?: "") }
    var category by remember { mutableStateOf(initialModule?.category ?: "Core Operations") }
    var hours by remember { mutableIntStateOf(initialModule?.estimatedHours ?: 4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialModule == null) "Create New Module" else "Edit Module #${initialModule.number}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Module Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_module_title"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Executive Description") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("input_module_description"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Strategic, Operations, Trade)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Estimated Study Hours: $hours h",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NavyPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Row {
                        OutlinedButton(
                            onClick = { if (hours > 1) hours-- },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("-")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = { if (hours < 20) hours++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("+")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, description, category, hours) },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                modifier = Modifier.testTag("btn_save_module")
            ) {
                Text(if (initialModule == null) "Create Module" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ======================= CREATE / EDIT CLASS DIALOG =======================
@Composable
fun CreateEditClassDialog(
    initialClass: CourseClass?,
    moduleId: String,
    onDismiss: () -> Unit,
    onSave: (title: String, summary: String, duration: Int, objectives: List<String>) -> Unit
) {
    var title by remember { mutableStateOf(initialClass?.title ?: "") }
    var summary by remember { mutableStateOf(initialClass?.summary ?: "") }
    var duration by remember { mutableIntStateOf(initialClass?.durationMinutes ?: 25) }
    var objectivesText by remember {
        mutableStateOf(initialClass?.learningObjectives?.joinToString("\n") ?: "Understand key operational formulas\nExamine global logistics case studies\nApply best industry governance standards")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialClass == null) "Add Class to Module" else "Edit Class",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Class Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_class_title"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Class Summary") },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("input_class_summary"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                OutlinedTextField(
                    value = objectivesText,
                    onValueChange = { objectivesText = it },
                    label = { Text("Learning Objectives (one per line)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Duration: $duration minutes",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NavyPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Row {
                        OutlinedButton(
                            onClick = { if (duration > 5) duration -= 5 },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("-")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = { if (duration < 120) duration += 5 },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("+")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val objectives = objectivesText.lines().map { it.trim() }.filter { it.isNotBlank() }
                    onSave(title, summary, duration, objectives)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                modifier = Modifier.testTag("btn_save_class")
            ) {
                Text(if (initialClass == null) "Add Class" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ======================= MULTI-FILE UPLOAD MEDIA DIALOG =======================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadMediaDialog(
    initialMediaType: MediaType,
    classTitle: String,
    uploadProgress: Float?,
    uploadByteStatus: String? = null,
    uploadError: String? = null,
    onDismiss: () -> Unit,
    onUploadMultipleFiles: (List<PendingUploadItem>) -> Unit,
    onAttachRemoteUrl: ((MediaType, url: String, title: String, description: String) -> Unit)? = null,
    onUploadPreset: (MediaType, fileName: String, fileSizeBytes: Long, title: String, description: String) -> Unit = { _, _, _, _, _ -> },
    onLoadDemoPreset: () -> Unit,
    onRetry: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(initialMediaType) }
    var useHttpsUrlMode by remember { mutableStateOf(false) }
    var pendingItems by remember { mutableStateOf<List<PendingUploadItem>>(emptyList()) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    var remoteUrlInput by remember { mutableStateOf("") }
    var presetTitle by remember { mutableStateOf("${classTitle.take(24)} Study Material") }
    var presetDescription by remember { mutableStateOf("") }

    val mimeFiltersForCategory: Array<String> = when (selectedCategory) {
        MediaType.PDF -> arrayOf("application/pdf")
        MediaType.IMAGE -> arrayOf("image/*")
        MediaType.DOCUMENT -> arrayOf(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "application/pdf"
        )
        MediaType.AUDIO -> arrayOf("audio/*")
        MediaType.VIDEO -> arrayOf("video/*")
    }

    val multiFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            localValidationError = null
            val oversizedNames = mutableListOf<String>()
            val newItems = uris.mapNotNull { uri ->
                val meta = MediaFileManager.getFileMetaFromUri(context, uri)
                if (meta.sizeBytes > MediaFileManager.MAX_UPLOAD_BYTES) {
                    oversizedNames.add("${meta.fileName} (${meta.sizeFormatted})")
                    null
                } else {
                    val detectedType = MediaFileManager.inferMediaType(meta.mimeType, meta.fileName)
                    val cleanTitle = meta.fileName.substringBeforeLast('.').replace('_', ' ').ifBlank { meta.fileName }
                    PendingUploadItem(
                        uriString = uri.toString(),
                        fileName = meta.fileName,
                        sizeBytes = meta.sizeBytes,
                        sizeFormatted = meta.sizeFormatted,
                        mimeType = meta.mimeType ?: "",
                        mediaType = detectedType,
                        title = cleanTitle,
                        description = ""
                    )
                }
            }
            if (oversizedNames.isNotEmpty()) {
                localValidationError = "Skipped files exceeding 50 MB limit: ${oversizedNames.joinToString(", ")}"
            }
            if (newItems.isNotEmpty()) {
                pendingItems = pendingItems + newItems
            }
        }
    }

    val isUploading = uploadProgress != null
    val effectiveError = uploadError ?: localValidationError

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = GoldMetallic,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upload Class Files & Media",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Class: $classTitle",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NavyPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Files up to 18 MB auto-sync directly to every student phone via Firestore. For larger videos/files (>18 MB), attach a shared HTTPS URL.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7F96),
                        fontSize = 11.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !useHttpsUrlMode,
                        onClick = { if (!isUploading) useHttpsUrlMode = false },
                        label = { Text("Device Files", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyPrimary,
                            selectedLabelColor = GoldMetallic
                        )
                    )
                    if (onAttachRemoteUrl != null) {
                        FilterChip(
                            selected = useHttpsUrlMode,
                            onClick = { if (!isUploading) useHttpsUrlMode = true },
                            label = { Text("HTTPS Link", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = GoldMetallic
                            ),
                            modifier = Modifier.testTag("chip_upload_https_mode")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "File Category",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MediaType.entries.forEach { category ->
                        val selected = selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { if (!isUploading) selectedCategory = category },
                            label = { Text(category.displayLabel, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = GoldMetallic
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (useHttpsUrlMode) {
                    OutlinedTextField(
                        value = remoteUrlInput,
                        onValueChange = {
                            remoteUrlInput = it
                            localValidationError = null
                        },
                        label = { Text("Direct HTTPS File URL") },
                        placeholder = { Text("https://example.com/lecture-notes.pdf") },
                        singleLine = true,
                        enabled = !isUploading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_remote_media_url"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = presetTitle,
                        onValueChange = { presetTitle = it },
                        label = { Text("Material Title") },
                        singleLine = true,
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth().testTag("input_media_filename"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = presetDescription,
                        onValueChange = { presetDescription = it },
                        label = { Text("Short Description (optional)") },
                        maxLines = 2,
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { multiFilePickerLauncher.launch(mimeFiltersForCategory) },
                            enabled = !isUploading,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_select_phone_file"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NavyPrimary,
                                contentColor = GoldMetallic
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pick ${selectedCategory.displayLabel} Files",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { multiFilePickerLauncher.launch(arrayOf("*/*")) },
                            enabled = !isUploading,
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("btn_pick_any_files"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("All Types", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }

                    if (pendingItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Selected Files (${pendingItems.size}) — Add Title & Description",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        pendingItems.forEachIndexed { idx, item ->
                            val willCloudSync = item.sizeBytes <= CloudSyncRepository.MAX_CLOUD_FILE_BYTES
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFD6E2F0), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${item.mediaType.displayLabel} • ${item.fileName}",
                                            fontWeight = FontWeight.Bold,
                                            color = NavyPrimary,
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = if (willCloudSync) {
                                                "Size: ${item.sizeFormatted} • Syncs automatically to student phones"
                                            } else {
                                                "Size: ${item.sizeFormatted} (>18 MB) • Use HTTPS Link tab for cross-device access"
                                            },
                                            color = if (willCloudSync) Color(0xFF047857) else GoldDark,
                                            fontSize = 10.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            pendingItems = pendingItems.filterNot { it.id == item.id }
                                        },
                                        enabled = !isUploading,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove file",
                                            tint = DangerRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = item.title,
                                    onValueChange = { newTitle ->
                                        pendingItems = pendingItems.mapIndexed { i, existing ->
                                            if (i == idx) existing.copy(title = newTitle) else existing
                                        }
                                    },
                                    label = { Text("File Title", fontSize = 11.sp) },
                                    singleLine = true,
                                    enabled = !isUploading,
                                    modifier = Modifier.fillMaxWidth().testTag("input_media_filename_$idx"),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = item.description,
                                    onValueChange = { newDesc ->
                                        pendingItems = pendingItems.mapIndexed { i, existing ->
                                            if (i == idx) existing.copy(description = newDesc) else existing
                                        }
                                    },
                                    label = { Text("Short Description (optional)", fontSize = 11.sp) },
                                    singleLine = false,
                                    maxLines = 2,
                                    enabled = !isUploading,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = presetTitle,
                            onValueChange = { presetTitle = it },
                            label = { Text("Title (for quick attachment or preset)") },
                            singleLine = true,
                            enabled = !isUploading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_media_filename"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = presetDescription,
                            onValueChange = { presetDescription = it },
                            label = { Text("Short Description (optional)") },
                            maxLines = 2,
                            enabled = !isUploading,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                        )
                    }
                }

                if (effectiveError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, DangerRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = effectiveError,
                                    color = DangerRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    localValidationError = null
                                    onRetry()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Clear Error",
                                    tint = DangerRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (isUploading) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Uploading... ${((uploadProgress ?: 0f) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NavyPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (!uploadByteStatus.isNullOrBlank()) {
                            Text(
                                text = uploadByteStatus,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GoldDark,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { uploadProgress ?: 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GoldMetallic,
                        trackColor = Color(0xFFD6E2F0)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable(enabled = !isUploading) { onLoadDemoPreset() }
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = GoldDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Or tap here to attach sample lecture materials (PDF + Audio + Video).",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = NavyPrimary,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (useHttpsUrlMode && onAttachRemoteUrl != null) {
                        val trimmed = remoteUrlInput.trim()
                        if (!trimmed.startsWith("https://", ignoreCase = true) && !trimmed.startsWith("http://", ignoreCase = true)) {
                            localValidationError = "Enter a valid HTTPS URL starting with https://"
                        } else {
                            onAttachRemoteUrl(selectedCategory, trimmed, presetTitle, presetDescription)
                        }
                    } else if (pendingItems.isNotEmpty()) {
                        onUploadMultipleFiles(pendingItems)
                    } else {
                        localValidationError = "Select at least one real file via 'Pick ${selectedCategory.displayLabel} Files' or 'All Types', or switch to 'HTTPS Link'."
                    }
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyPrimary,
                    contentColor = GoldMetallic
                ),
                modifier = Modifier.testTag("btn_confirm_upload")
            ) {
                Text(
                    text = when {
                        useHttpsUrlMode -> "Attach HTTPS Link"
                        pendingItems.isNotEmpty() -> "Upload ${pendingItems.size} File(s)"
                        else -> "Select File(s) First"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (!isUploading) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

// ======================= DELETE CONFIRMATION DIALOG =======================
@Composable
fun DeleteConfirmDialog(
    targetDescription: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm Deletion",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DangerRed
                    )
                )
            }
        },
        text = {
            Text(
                text = "Are you sure you want to permanently delete $targetDescription? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium.copy(color = NavyPrimary)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                modifier = Modifier.testTag("btn_confirm_delete")
            ) {
                Text("Delete Permanently")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ======================= FIREBASE & PRODUCTION GUIDE DIALOG =======================
@Composable
fun FirebaseGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = GoldMetallic,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Cloud Architecture & Setup Guide",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ISCEB runs offline-first with live Firebase Cloud Firestore synchronization and Credential Manager Google Sign-In.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF334155))
                )

                SectionGuideCard(
                    title = "1. Live Zero-Trust Firestore Security Rules",
                    code = "• /curriculum_manifests/live: Signed-in read (`get`); Admin-only create/update (`www.mdnafiulislam@gmail.com`)\n• /curriculum_catalogs/live: Signed-in read (`get`); Admin-only create/update\n• /users/{userId}/progress/{classId}: Strict owner-only read/write (`request.auth.uid == userId`)"
                )

                SectionGuideCard(
                    title = "2. Live Sync & Cache-Busted Check Pipeline",
                    code = "• Real-time Firestore `snapshots()` listeners (strictly Auth-gated)\n• Cache-busted HTTPS manifest polling (`Cache-Control: no-cache`, `?t=timestamp`)\n• Non-destructive student progress & local media preservation"
                )

                SectionGuideCard(
                    title = "3. Native Android APK In-Place Update Delivery",
                    code = "• Framework: 100% Native Android Kotlin & Jetpack Compose\n• Package ID: com.aistudio.supplychain.scm (Fixed across releases)\n• OTA Dynamic Content: Curriculum, notices & schedules sync automatically\n• Native Binary Updates: Verified BuildConfig.VERSION_CODE comparison opens the official APK release link via Play-compliant Intent.ACTION_VIEW for in-place upgrade without uninstalling."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic)
            ) {
                Text("Close Guide")
            }
        }
    )
}

@Composable
private fun SectionGuideCard(title: String, code: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F5F9))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = NavyPrimary,
                fontSize = 11.sp
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFF1E293B),
                lineHeight = 14.sp
            )
        )
    }
}
