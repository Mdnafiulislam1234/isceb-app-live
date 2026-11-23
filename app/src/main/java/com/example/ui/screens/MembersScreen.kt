package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import com.example.ui.icons.ArrowDownward
import com.example.ui.icons.ArrowUpward
import com.example.ui.icons.Badge
import com.example.ui.icons.Build
import com.example.ui.icons.Business
import com.example.ui.icons.ChevronRight
import com.example.ui.icons.ContactMail
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.example.ui.icons.Groups
import androidx.compose.material.icons.filled.Person
import com.example.ui.icons.PhotoCamera
import com.example.ui.icons.School
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.DeveloperProfile
import com.example.model.FounderProfile
import com.example.model.IscebMember
import com.example.ui.theme.BgLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import java.io.File
import java.util.UUID

@Composable
fun MembersScreen(
    members: List<IscebMember>,
    founderProfile: FounderProfile,
    developerProfile: DeveloperProfile,
    isAdmin: Boolean,
    onOpenFounderProfile: () -> Unit,
    onOpenDeveloperProfile: () -> Unit,
    onMemberClick: (IscebMember) -> Unit,
    onAddMemberClick: () -> Unit,
    onEditMemberClick: (IscebMember) -> Unit,
    onDeleteMemberClick: (IscebMember) -> Unit,
    onMoveMemberOrder: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val orderedMembers = remember(members, searchQuery) {
        val sorted = members.sortedBy { it.displayOrder }
        if (searchQuery.isBlank()) {
            sorted
        } else {
            val q = searchQuery.trim()
            sorted.filter { m ->
                m.fullName.contains(q, ignoreCase = true) ||
                    m.designation.contains(q, ignoreCase = true) ||
                    m.professionalIdentity.contains(q, ignoreCase = true) ||
                    m.currentOrganization.contains(q, ignoreCase = true) ||
                    m.roleInIsceb.contains(q, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight)
            .testTag("screen_isceb_members"),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(NavyDark, NavyPrimary, NavySecondary)
                            )
                        )
                        .padding(16.dp)
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GoldMetallic),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "ISCEB Members",
                                    tint = NavyDark,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ISCEB Members & Leadership",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                )
                                Text(
                                    text = "Executive leadership, board faculty & registered ISCEB members",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = GoldSoft,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        if (isAdmin) {
                            Button(
                                onClick = onAddMemberClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldMetallic,
                                    contentColor = NavyDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_add_isceb_member")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Member",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Member", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // Search Members Directory
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ISCEB Members Directory (${members.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    )
                    if (isAdmin) {
                        Text(
                            text = "Admin Edit & Order Enabled",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GoldDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search member name, role, organization...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Members",
                            tint = Color(0xFF7A8B9E)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_members"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldMetallic,
                        unfocusedBorderColor = Color(0xFFD6E2F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
        }

        // Responsive Member Cards Grid
        if (orderedMembers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching members found" else "No additional members registered yet",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = NavyPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAdmin) {
                                "Tap 'Add Member' to add real ISCEB members with verified details and photos."
                            } else {
                                "Admins can add and verify ISCEB member profiles from Members Management."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        } else {
            item {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val columns = when {
                        maxWidth >= 900.dp -> 3
                        maxWidth >= 600.dp -> 2
                        else -> 1
                    }
                    val rows = orderedMembers.chunked(columns)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEach { rowMembers ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowMembers.forEach { member ->
                                    val globalIdx = members.sortedBy { it.displayOrder }.indexOfFirst { it.id == member.id }
                                    MemberDirectoryCard(
                                        member = member,
                                        isAdmin = isAdmin,
                                        canMoveUp = globalIdx > 0,
                                        canMoveDown = globalIdx in 0 until (members.size - 1),
                                        onClick = { onMemberClick(member) },
                                        onEditClick = { onEditMemberClick(member) },
                                        onDeleteClick = { onDeleteMemberClick(member) },
                                        onMoveUp = { onMoveMemberOrder(member.id, true) },
                                        onMoveDown = { onMoveMemberOrder(member.id, false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowMembers.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FounderSummaryCard(
    founderProfile: FounderProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("card_founder_profile"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldMetallic.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatarBox(
                photoUri = founderProfile.photoUri,
                builtInPhotoKey = "amirul",
                fullName = founderProfile.name,
                size = 54.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = founderProfile.designation.uppercase(),
                    color = GoldDark,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp
                )
                Text(
                    text = founderProfile.name,
                    color = NavyPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = founderProfile.professionalIdentity,
                    color = Color(0xFF334155),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${founderProfile.additionalRole} • ${founderProfile.mobileNumber}",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Founder Profile",
                tint = GoldDark
            )
        }
    }
}

@Composable
fun DeveloperSummaryCard(
    developerProfile: DeveloperProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("card_developer_profile"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6E2F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatarBox(
                photoUri = developerProfile.photoUri,
                builtInPhotoKey = "nafiul",
                fullName = developerProfile.name,
                size = 54.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = developerProfile.designation.uppercase(),
                    color = NavySecondary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp
                )
                Text(
                    text = developerProfile.name,
                    color = NavyPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ISCEB App Engineering & Cloud Sync",
                    color = Color(0xFF334155),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = developerProfile.contactEmail,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Developer Profile",
                tint = NavySecondary
            )
        }
    }
}

@Composable
private fun MemberDirectoryCard(
    member: IscebMember,
    isAdmin: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("card_member_${member.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemberAvatarBox(
                    photoUri = member.photoUri,
                    builtInPhotoKey = member.builtInPhotoKey,
                    fullName = member.fullName,
                    size = 56.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            fontSize = 14.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (member.designation.isNotBlank()) {
                        Text(
                            text = member.designation,
                            color = GoldDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (member.currentOrganization.isNotBlank() || member.professionalIdentity.isNotBlank()) {
                        Text(
                            text = member.professionalIdentity.ifBlank { member.currentOrganization },
                            color = Color(0xFF475569),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (member.roleInIsceb.isNotBlank()) {
                        Text(
                            text = member.roleInIsceb,
                            color = NavySecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open Member Details",
                    tint = GoldDark
                )
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Position ${member.displayOrder}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp,
                            modifier = Modifier.size(40.dp).testTag("btn_move_member_up_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Move Member Up",
                                tint = if (canMoveUp) NavyPrimary else Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown,
                            modifier = Modifier.size(40.dp).testTag("btn_move_member_down_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Move Member Down",
                                tint = if (canMoveDown) NavyPrimary else Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(40.dp).testTag("btn_edit_member_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Member",
                                tint = GoldDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(40.dp).testTag("btn_delete_member_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Member",
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberDetailScreen(
    member: IscebMember,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onEditClick: (IscebMember) -> Unit,
    onDeleteClick: (IscebMember) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight)
            .testTag("screen_member_detail_${member.id}"),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_member_detail_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Members",
                            tint = NavyPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ISCEB Member Profile",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    )
                }

                if (isAdmin) {
                    Row {
                        IconButton(
                            onClick = { onEditClick(member) },
                            modifier = Modifier.testTag("btn_detail_edit_member")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Member",
                                tint = GoldDark
                            )
                        }
                        IconButton(
                            onClick = { onDeleteClick(member) },
                            modifier = Modifier.testTag("btn_detail_delete_member")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Member",
                                tint = DangerRed
                            )
                        }
                    }
                }
            }
        }

        // Field 1, 2, 3, 4: Photo, Full Name, Designation, Professional Identity
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MemberAvatarBox(
                        photoUri = member.photoUri,
                        builtInPhotoKey = member.builtInPhotoKey,
                        fullName = member.fullName,
                        size = 104.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    )
                    if (member.designation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = GoldMetallic.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldMetallic.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = GoldMetallic,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = member.designation,
                                    color = GoldSoft,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    if (member.professionalIdentity.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = member.professionalIdentity,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    if (member.roleInIsceb.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = member.roleInIsceb,
                            color = GoldMetallic,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Fields 5 & 9: Organization & Role in ISCEB
        item {
            MemberDetailCard(icon = Icons.Default.Business, title = "Organization & Role in ISCEB") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MemberFieldLine(label = "Professional Identity", value = member.professionalIdentity)
                    MemberFieldLine(label = "Current Organization / Workplace", value = member.currentOrganization)
                    MemberFieldLine(label = "Role in ISCEB", value = member.roleInIsceb)
                }
            }
        }

        // Field 6: Educational Qualification
        item {
            MemberDetailCard(icon = Icons.Default.School, title = "Educational Qualification") {
                MemberOptionalText(member.educationalQualification)
            }
        }

        // Field 7: Professional Experience
        item {
            MemberDetailCard(icon = Icons.Default.WorkOutline, title = "Professional Experience") {
                MemberOptionalText(member.professionalExperience)
            }
        }

        // Field 8: Skills & Expertise
        item {
            MemberDetailCard(icon = Icons.Default.Build, title = "Skills & Expertise") {
                if (member.skillsAndExpertise.isEmpty()) {
                    MemberOptionalText("")
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        member.skillsAndExpertise.forEach { skill ->
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
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFF1F5F9)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6E2F0))
                            )
                        }
                    }
                }
            }
        }

        // Field 11: Short Personal & Professional Biography
        item {
            MemberDetailCard(icon = Icons.Default.Person, title = "Personal & Professional Biography") {
                MemberOptionalText(member.biography)
            }
        }

        // Field 10: Contact Information
        item {
            MemberDetailCard(icon = Icons.Default.ContactMail, title = "Contact Information") {
                MemberOptionalText(member.contactInformation)
            }
        }
    }
}

@Composable
private fun MemberDetailCard(
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
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavyPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GoldMetallic,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun MemberFieldLine(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF64748B)
        )
        Text(
            text = value.ifBlank { "Information not provided yet — pending admin update" },
            fontSize = 12.5.sp,
            fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
            fontStyle = if (value.isBlank()) FontStyle.Italic else FontStyle.Normal,
            color = if (value.isBlank()) Color(0xFF64748B) else NavyPrimary
        )
    }
}

@Composable
private fun MemberOptionalText(text: String) {
    Text(
        text = text.ifBlank { "Information not provided yet — Admins can collect and add this later." },
        style = MaterialTheme.typography.bodyMedium.copy(
            color = if (text.isBlank()) Color(0xFF64748B) else Color(0xFF334155),
            fontStyle = if (text.isBlank()) FontStyle.Italic else FontStyle.Normal,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    )
}

@Composable
fun MemberAvatarBox(
    photoUri: String,
    builtInPhotoKey: String,
    fullName: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val localPhotoFile = photoUri
        .takeIf { it.isNotBlank() }
        ?.let { File(it) }
        ?.takeIf { it.exists() }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.8.dp, GoldMetallic, CircleShape)
            .background(NavyPrimary),
        contentAlignment = Alignment.Center
    ) {
        when {
            localPhotoFile != null -> {
                AsyncImage(
                    model = localPhotoFile,
                    contentDescription = fullName,
                    modifier = Modifier.fillMaxSize(),
                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                    contentScale = ContentScale.Crop
                )
            }
            builtInPhotoKey == "amirul" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_admin_amirul),
                    contentDescription = fullName,
                    modifier = Modifier.fillMaxSize(),
                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                    contentScale = ContentScale.Crop
                )
            }
            builtInPhotoKey == "hasina" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_member_hasina),
                    contentDescription = fullName,
                    modifier = Modifier.fillMaxSize(),
                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                    contentScale = ContentScale.Crop
                )
            }
            builtInPhotoKey == "masudur" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_member_masudur),
                    contentDescription = fullName,
                    modifier = Modifier.fillMaxSize(),
                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                    contentScale = ContentScale.Crop
                )
            }
            builtInPhotoKey == "asif" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_member_asif),
                    contentDescription = fullName,
                    modifier = Modifier.fillMaxSize(),
                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                    contentScale = ContentScale.Crop
                )
            }
            builtInPhotoKey == "mashiur" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_member_mashiur),
                    contentDescription = fullName,
                    modifier = Modifier.fillMaxSize(),
                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                    contentScale = ContentScale.Crop
                )
            }
            builtInPhotoKey == "shajibul" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_member_shajibul),
                    contentDescription = fullName,
                    modifier = Modifier.fillMaxSize(),
                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                    contentScale = ContentScale.Crop
                )
            }
            builtInPhotoKey == "tonmoy" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_member_tonmoy),
                    contentDescription = fullName,
                    modifier = Modifier.fillMaxSize(),
                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                    contentScale = ContentScale.Crop
                )
            }
            builtInPhotoKey == "nafiul" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_admin_nafiul),
                    contentDescription = fullName,
                    modifier = Modifier.fillMaxSize(),
                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                val initials = fullName
                    .split(" ")
                    .filter { it.isNotBlank() && !it.equals("Md.", ignoreCase = true) }
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                    .ifBlank { "IM" }
                Text(
                    text = initials,
                    color = GoldMetallic,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.34f).sp
                )
            }
        }
    }
}

@Composable
fun AddEditMemberDialog(
    initialMember: IscebMember?,
    nextDisplayOrder: Int,
    onDismiss: () -> Unit,
    onSave: (IscebMember, Uri?, Boolean) -> Unit
) {
    val isEdit = initialMember != null
    var fullName by remember { mutableStateOf(initialMember?.fullName ?: "") }
    var designation by remember { mutableStateOf(initialMember?.designation ?: "") }
    var professionalIdentity by remember { mutableStateOf(initialMember?.professionalIdentity ?: "") }
    var currentOrganization by remember { mutableStateOf(initialMember?.currentOrganization ?: "") }
    var educationalQualification by remember { mutableStateOf(initialMember?.educationalQualification ?: "") }
    var professionalExperience by remember { mutableStateOf(initialMember?.professionalExperience ?: "") }
    var skillsText by remember { mutableStateOf(initialMember?.skillsAndExpertise?.joinToString(", ") ?: "") }
    var roleInIsceb by remember { mutableStateOf(initialMember?.roleInIsceb ?: "") }
    var contactInformation by remember { mutableStateOf(initialMember?.contactInformation ?: "") }
    var biography by remember { mutableStateOf(initialMember?.biography ?: "") }
    var displayOrderText by remember { mutableStateOf((initialMember?.displayOrder ?: nextDisplayOrder).toString()) }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var clearExistingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri
            clearExistingPhoto = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = GoldDark,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEdit) "Edit ISCEB Member" else "Add New ISCEB Member",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_pick_member_photo"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (pickedPhotoUri != null) "Photo Selected" else "Upload Photo",
                            color = NavyPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    }
                    if (!initialMember?.photoUri.isNullOrBlank() && pickedPhotoUri == null) {
                        TextButton(onClick = { clearExistingPhoto = true }) {
                            Text("Remove", color = DangerRed, fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_name"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_designation"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = professionalIdentity,
                    onValueChange = { professionalIdentity = it },
                    label = { Text("Professional Identity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_identity"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = currentOrganization,
                    onValueChange = { currentOrganization = it },
                    label = { Text("Current Organization / Workplace") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_org"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = roleInIsceb,
                    onValueChange = { roleInIsceb = it },
                    label = { Text("Role in ISCEB") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_role"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = educationalQualification,
                    onValueChange = { educationalQualification = it },
                    label = { Text("Educational Qualification") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_education"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = professionalExperience,
                    onValueChange = { professionalExperience = it },
                    label = { Text("Professional Experience") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_experience"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = skillsText,
                    onValueChange = { skillsText = it },
                    label = { Text("Skills & Expertise (comma-separated)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_skills"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = contactInformation,
                    onValueChange = { contactInformation = it },
                    label = { Text("Contact Info (Phone / Email / LinkedIn)") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_contact"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = biography,
                    onValueChange = { biography = it },
                    label = { Text("Personal & Professional Biography") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_bio"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
                OutlinedTextField(
                    value = displayOrderText,
                    onValueChange = { displayOrderText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Display Order #") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_member_order"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMetallic)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedSkills = skillsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val orderNum = displayOrderText.toIntOrNull()?.coerceAtLeast(1) ?: nextDisplayOrder
                    val member = IscebMember(
                        id = initialMember?.id ?: "member-${UUID.randomUUID()}",
                        photoUri = initialMember?.photoUri ?: "",
                        builtInPhotoKey = initialMember?.builtInPhotoKey ?: "",
                        fullName = fullName.trim(),
                        designation = designation.trim(),
                        professionalIdentity = professionalIdentity.trim(),
                        currentOrganization = currentOrganization.trim(),
                        educationalQualification = educationalQualification.trim(),
                        professionalExperience = professionalExperience.trim(),
                        skillsAndExpertise = parsedSkills,
                        roleInIsceb = roleInIsceb.trim(),
                        contactInformation = contactInformation.trim(),
                        biography = biography.trim(),
                        displayOrder = orderNum
                    )
                    onSave(member, pickedPhotoUri, clearExistingPhoto)
                },
                enabled = fullName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = GoldMetallic),
                modifier = Modifier.testTag("btn_save_member")
            ) {
                Text(if (isEdit) "Save Changes" else "Add Member", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
