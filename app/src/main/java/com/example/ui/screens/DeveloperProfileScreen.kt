package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.example.ui.icons.Build
import com.example.ui.icons.ContactMail
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import com.example.ui.icons.Language
import com.example.ui.icons.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import com.example.ui.icons.PhotoCamera
import com.example.ui.icons.School
import com.example.ui.icons.Verified
import com.example.ui.icons.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.DeveloperProfile
import com.example.ui.theme.BgLight
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperProfileScreen(
    developerProfile: DeveloperProfile,
    isAdmin: Boolean,
    onUpdateProfile: (DeveloperProfile, Uri?) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }
    var showEditDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight)
            .testTag("screen_developer_profile"),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (onBack != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_developer_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NavyPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Developer of ISCEB App",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        )
                        Text(
                            text = "App Engineering & System Architecture",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
        // Hero Executive Portrait Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(NavyDark, NavyPrimary, NavySecondary)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, GoldMetallic, CircleShape)
                                .background(Color.White)
                                .testTag("img_developer_profile_photo"),
                            contentAlignment = Alignment.Center
                        ) {
                            val localPhotoFile = developerProfile.photoUri
                                .takeIf { it.isNotBlank() }
                                ?.let { File(it) }
                                ?.takeIf { it.exists() }

                            if (localPhotoFile != null) {
                                AsyncImage(
                                    model = localPhotoFile,
                                    contentDescription = developerProfile.name,
                                    modifier = Modifier.fillMaxSize(),
                                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_admin_nafiul),
                                    contentDescription = "${developerProfile.name} - ${developerProfile.designation}",
                                    modifier = Modifier.fillMaxSize(),
                                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = developerProfile.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = GoldMetallic.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldMetallic.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = GoldMetallic,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = developerProfile.designation,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = GoldSoft,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showEditDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldMetallic,
                                    contentColor = NavyDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_edit_developer_profile")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Edit Developer Profile",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Short Biography
        item {
            DeveloperSectionCard(
                icon = Icons.Default.Person,
                title = "Short Biography"
            ) {
                Text(
                    text = developerProfile.biography,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF334155),
                        lineHeight = 21.sp,
                        fontSize = 13.sp
                    )
                )
            }
        }

        // Educational Background
        item {
            DeveloperSectionCard(
                icon = Icons.Default.School,
                title = "Educational Background"
            ) {
                Text(
                    text = developerProfile.educationBackground,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF334155),
                        lineHeight = 20.sp,
                        fontSize = 13.sp
                    )
                )
            }
        }

        // Skills & Expertise
        item {
            DeveloperSectionCard(
                icon = Icons.Default.Build,
                title = "Skills & Expertise"
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    developerProfile.skillsAndExpertise.forEach { skill ->
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = skill,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyPrimary
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFF1F5F9)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6E2F0))
                        )
                    }
                }
            }
        }

        // Work & Project Experience
        item {
            DeveloperSectionCard(
                icon = Icons.Default.WorkOutline,
                title = "Work & Project Experience"
            ) {
                Text(
                    text = developerProfile.workExperience,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF334155),
                        lineHeight = 20.sp,
                        fontSize = 13.sp
                    )
                )
            }
        }

        // Project Information
        item {
            DeveloperSectionCard(
                icon = Icons.Default.WorkOutline,
                title = "Project Information — ISCEB App"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_isceb_logo),
                        contentDescription = "ISCEB Logo",
                        modifier = Modifier
                            .width(68.dp)
                            .height(44.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = developerProfile.projectInformation,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF334155),
                            lineHeight = 18.sp,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Contact Information
        item {
            DeveloperSectionCard(
                icon = Icons.Default.ContactMail,
                title = "Contact Information"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ContactInfoRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = developerProfile.contactEmail
                    )
                    if (developerProfile.contactPhone.isNotBlank()) {
                        ContactInfoRow(
                            icon = Icons.Default.Phone,
                            label = "Phone",
                            value = developerProfile.contactPhone
                        )
                    }
                    ContactInfoRow(
                        icon = Icons.Default.Language,
                        label = "Official Platform",
                        value = developerProfile.contactWebsite
                    )
                    ContactInfoRow(
                        icon = Icons.Default.LocationOn,
                        label = "Location",
                        value = developerProfile.location
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        EditDeveloperProfileDialog(
            initialProfile = developerProfile,
            onDismiss = { showEditDialog = false },
            onSave = { updated, uri ->
                onUpdateProfile(updated, uri)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun DeveloperSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavyPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GoldMetallic,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        fontSize = 14.5.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ContactInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = GoldDark,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = NavyPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                )
            )
        }
    }
}

@Composable
private fun EditDeveloperProfileDialog(
    initialProfile: DeveloperProfile,
    onDismiss: () -> Unit,
    onSave: (DeveloperProfile, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(initialProfile.name) }
    var designation by remember { mutableStateOf(initialProfile.designation) }
    var biography by remember { mutableStateOf(initialProfile.biography) }
    var education by remember { mutableStateOf(initialProfile.educationBackground) }
    var workExperience by remember { mutableStateOf(initialProfile.workExperience) }
    var skillsText by remember { mutableStateOf(initialProfile.skillsAndExpertise.joinToString(", ")) }
    var projectInfo by remember { mutableStateOf(initialProfile.projectInformation) }
    var email by remember { mutableStateOf(initialProfile.contactEmail) }
    var phone by remember { mutableStateOf(initialProfile.contactPhone) }
    var website by remember { mutableStateOf(initialProfile.contactWebsite) }
    var location by remember { mutableStateOf(initialProfile.location) }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Developer Profile",
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
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_pick_developer_photo"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (pickedPhotoUri != null) "Photo Selected (Ready to Save)" else "Change Profile Photo",
                        color = NavyPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_dev_name"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_dev_designation"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = biography,
                    onValueChange = { biography = it },
                    label = { Text("Short Biography") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("input_dev_bio"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = education,
                    onValueChange = { education = it },
                    label = { Text("Educational Background") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = workExperience,
                    onValueChange = { workExperience = it },
                    label = { Text("Work & Project Experience") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = skillsText,
                    onValueChange = { skillsText = it },
                    label = { Text("Skills & Expertise (comma-separated)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = projectInfo,
                    onValueChange = { projectInfo = it },
                    label = { Text("Project Information") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Contact Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedSkills = skillsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(
                        initialProfile.copy(
                            name = name.trim().ifBlank { initialProfile.name },
                            designation = designation.trim().ifBlank { initialProfile.designation },
                            biography = biography.trim().ifBlank { initialProfile.biography },
                            educationBackground = education.trim(),
                            workExperience = workExperience.trim(),
                            skillsAndExpertise = if (parsedSkills.isNotEmpty()) parsedSkills else initialProfile.skillsAndExpertise,
                            projectInformation = projectInfo.trim(),
                            contactEmail = email.trim(),
                            contactPhone = phone.trim(),
                            contactWebsite = website.trim(),
                            location = location.trim()
                        ),
                        pickedPhotoUri
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                modifier = Modifier.testTag("btn_save_developer_profile")
            ) {
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
