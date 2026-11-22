package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import com.example.ui.icons.Campaign
import com.example.ui.icons.CloudDone
import com.example.ui.icons.CloudOff
import com.example.ui.icons.CloudSync
import com.example.ui.icons.Download
import com.example.ui.icons.FilterList
import com.example.ui.icons.Groups
import com.example.ui.icons.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import com.example.ui.icons.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AppUpdateInfo
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.CurriculumPublishManifest
import com.example.model.DeveloperProfile
import com.example.model.FounderProfile
import com.example.model.SyncStatus
import com.example.model.UserProgress
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.components.HeroSection
import com.example.ui.components.IscebWorkspaceCard
import com.example.ui.components.ModuleCard
import com.example.ui.theme.BgLight
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.NavTab

@Composable
fun HomeScreen(
    modules: List<CourseModule>,
    classes: List<CourseClass>,
    progress: UserProgress,
    isAdmin: Boolean,
    searchQuery: String,
    selectedFilter: String,
    publishManifest: CurriculumPublishManifest = CurriculumPublishManifest(),
    pendingUpdateManifest: CurriculumPublishManifest? = null,
    syncStatus: SyncStatus = SyncStatus.SYNCED_SUCCESS,
    appUpdateInfo: AppUpdateInfo = AppUpdateInfo(),
    cloudUserEmail: String? = null,
    lastSyncTimestamp: Long = 0L,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onModuleClick: (CourseModule) -> Unit,
    onClassClick: (CourseClass) -> Unit,
    onEditModule: (CourseModule) -> Unit,
    onDeleteModule: (CourseModule) -> Unit,
    onCreateModuleClick: () -> Unit,
    onSeeAllModules: () -> Unit,
    onSyncClick: () -> Unit = {},
    onCheckUpdatesClick: () -> Unit = {},
    onPhoneInstallClick: () -> Unit = {},
    onConnectCloudSyncClick: () -> Unit = {},
    onUpdateApkClick: (String) -> Unit = {},
    founderProfile: FounderProfile = FounderProfile(),
    developerProfile: DeveloperProfile = DeveloperProfile(),
    membersCount: Int = 1,
    onOpenMembers: () -> Unit = {},
    onOpenFounderProfile: () -> Unit = {},
    onOpenDeveloperProfile: () -> Unit = {},
    isTeacher: Boolean = false,
    examCount: Int = 5,
    assignmentCount: Int = 4,
    certificateStatusLabel: String = "PGD",
    showInlineWorkspaceCard: Boolean = true,
    onNavigateTab: (NavTab) -> Unit = {},
    onOpenExamsAndAssignments: () -> Unit = {},
    onOpenAdminGovernance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val visibleClasses = remember(classes, isAdmin) {
        if (isAdmin) classes else classes.filter { it.isPublished }
    }
    val nextClass = remember(visibleClasses) {
        visibleClasses.firstOrNull { !it.isCompleted } ?: visibleClasses.firstOrNull()
    }
    val founderFile = remember(founderProfile.photoUri) {
        founderProfile.photoUri.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
    }
    val devFile = remember(developerProfile.photoUri) {
        developerProfile.photoUri.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
    }
    val classesByModule = remember(visibleClasses) {
        visibleClasses.groupBy { it.moduleId }
    }
    val displayedModules = remember(modules, classesByModule, searchQuery, selectedFilter) {
        val filtered = modules.filter { mod ->
            val modClasses = classesByModule[mod.id].orEmpty()
            val matchesQuery = searchQuery.isBlank() ||
                mod.title.contains(searchQuery, ignoreCase = true) ||
                mod.description.contains(searchQuery, ignoreCase = true) ||
                modClasses.any { it.title.contains(searchQuery, ignoreCase = true) }

            val matchesFilter = when (selectedFilter) {
                "Part 1: Basic" -> mod.part == com.example.model.CoursePart.PART_1_BASIC
                "Part 2: Advance" -> mod.part == com.example.model.CoursePart.PART_2_ADVANCE
                "Completed" -> modClasses.isNotEmpty() && modClasses.all { it.isCompleted }
                "With Media" -> modClasses.any { it.hasAnyMedia }
                else -> true
            }
            matchesQuery && matchesFilter
        }
        if (searchQuery.isBlank() && selectedFilter == "All") filtered.take(6) else filtered
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Complete Official ISCEB Brand Banner
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
                    // Complete uncropped ISCEB logo, retaining exact aspect ratio and tagline
                    Image(
                        painter = painterResource(id = R.drawable.img_isceb_logo),
                        contentDescription = "ISCEB - Connect. learn. Achieve.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(94.dp)
                            .testTag("img_dashboard_brand_logo"),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ISCEB • POST GRADUATE DIPLOMA (PGD) IN SUPPLY CHAIN MANAGEMENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldDark,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }

        // Unified ISCEB Workspace Hub (matches Live Web/PWA Workspace)
        if (showInlineWorkspaceCard) {
            item {
                val completedCount = progress.completedClassIds.size
                val totalClassesCount = classes.size.coerceAtLeast(1)
                val progressPct = ((completedCount * 100f) / totalClassesCount).toInt().coerceIn(0, 100)
                val apkLabel = run {
                    val apkVer = appUpdateInfo.latestAvailableApkVersionName.ifBlank {
                        publishManifest.latestApkVersionName.ifBlank { "1.6" }
                    }
                    val sizeMb = if (appUpdateInfo.fileSizeBytes > 0L) {
                        String.format(Locale.US, "%.1f MB", appUpdateInfo.fileSizeBytes / (1024.0 * 1024.0))
                    } else "18.5 MB"
                    "v$apkVer ($sizeMb)"
                }
                val syncLabel = if (!cloudUserEmail.isNullOrBlank()) "Cloud: $cloudUserEmail" else "Local + Web Ready"

                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    IscebWorkspaceCard(
                        currentTab = NavTab.HOME,
                        onTabSelected = { tab ->
                            if (tab == NavTab.MODULES) onSeeAllModules() else onNavigateTab(tab)
                        },
                        progressPercent = progressPct,
                        moduleCount = modules.size,
                        totalClassesCount = classes.size,
                        examCount = examCount,
                        assignmentCount = assignmentCount,
                        certificateStatusLabel = certificateStatusLabel,
                        membersCount = membersCount,
                        isAdmin = isAdmin,
                        isTeacher = isTeacher,
                        curriculumVersionTag = publishManifest.versionTag.ifBlank { "v1.6.0" },
                        latestApkLabel = apkLabel,
                        syncStatusLabel = syncLabel,
                        isCloudSynced = !cloudUserEmail.isNullOrBlank(),
                        onOpenExamsAndAssignments = onOpenExamsAndAssignments,
                        onOpenAdminGovernance = onOpenAdminGovernance,
                        onCheckAppUpdateClick = onCheckUpdatesClick,
                        onOpenApkPortalClick = onPhoneInstallClick
                    )
                }
            }
        }

        // Part 1 & Part 2 Executive Curriculum Summary Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = { onFilterChange("Part 1: Basic") },
                    shape = RoundedCornerShape(12.dp),
                    color = NavyPrimary,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "PART 1 / BASIC",
                            color = GoldMetallic,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Foundation Level • 28 Classes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Modules 01–06 (Lectures 01–28)",
                            color = Color(0xFFB0C2D6),
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    onClick = { onFilterChange("Part 2: Advance") },
                    shape = RoundedCornerShape(12.dp),
                    color = NavySecondary,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "PART 2 / ADVANCE",
                            color = GoldMetallic,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Strategic Level • 20 Classes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Modules 07–12 (Lectures 29–48)",
                            color = Color(0xFFB0C2D6),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // ISCEB Leadership & Members Quick-Access Card
        item {
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
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ISCEB Leadership & Members",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        )
                        TextButton(
                            onClick = onOpenMembers,
                            modifier = Modifier.height(30.dp).testTag("btn_home_view_members")
                        ) {
                            Text(
                                text = "Directory ($membersCount)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldDark
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Founder Card
                        Surface(
                            onClick = onOpenFounderProfile,
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6E2F0)),
                            modifier = Modifier.weight(1f).testTag("btn_home_founder_profile")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, GoldMetallic, CircleShape)
                                ) {
                                    if (founderFile != null) {
                                        AsyncImage(
                                            model = founderFile,
                                            contentDescription = founderProfile.name,
                                            modifier = Modifier.fillMaxSize(),
                                            alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_admin_amirul),
                                            contentDescription = founderProfile.name,
                                            modifier = Modifier.fillMaxSize(),
                                            alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = "Founder",
                                    fontWeight = FontWeight.Bold,
                                    color = GoldDark,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = founderProfile.name,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyPrimary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Members Directory Card
                        Surface(
                            onClick = onOpenMembers,
                            shape = RoundedCornerShape(10.dp),
                            color = NavyPrimary,
                            modifier = Modifier.weight(1f).testTag("btn_home_isceb_members")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(GoldMetallic),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = "ISCEB Members",
                                        tint = NavyDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = "ISCEB Members",
                                    fontWeight = FontWeight.Bold,
                                    color = GoldMetallic,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "$membersCount Listed",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // Developer Card
                        Surface(
                            onClick = onOpenDeveloperProfile,
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6E2F0)),
                            modifier = Modifier.weight(1f).testTag("btn_home_developer_profile")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, GoldMetallic, CircleShape)
                                ) {
                                    if (devFile != null) {
                                        AsyncImage(
                                            model = devFile,
                                            contentDescription = developerProfile.name,
                                            modifier = Modifier.fillMaxSize(),
                                            alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_admin_nafiul),
                                            contentDescription = developerProfile.name,
                                            modifier = Modifier.fillMaxSize(),
                                            alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = "App Developer",
                                    fontWeight = FontWeight.Bold,
                                    color = GoldDark,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = developerProfile.name,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyPrimary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Cloud Curriculum Sync & Update Center
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pending Update Available Card
                if (pendingUpdateManifest != null || syncStatus == SyncStatus.UPDATE_AVAILABLE) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_curriculum_update_available"),
                        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NavySecondary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = GoldMetallic,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "New ISCEB Curriculum Available",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${pendingUpdateManifest?.versionTag ?: "Latest Version"} • Published by Md. Nafiul Islam Nahin",
                                            color = GoldSoft,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = onSyncClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldMetallic,
                                        contentColor = NavyDark
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_sync_now_home")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sync Now",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (!pendingUpdateManifest?.releaseNotes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Notes: ${pendingUpdateManifest?.releaseNotes}",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Official Academic Notice Announcement
                if (!publishManifest.announcement.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_academic_announcement"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = GoldDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Official Academic Notice",
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = publishManifest.announcement,
                                    color = Color(0xFF334155),
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.5.sp
                                )
                            }
                        }
                    }
                }

                // APK Binary Update Required Banner
                if (appUpdateInfo.isBinaryUpdateRequired) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_apk_binary_update"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7C2D12)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(GoldMetallic),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = "App Update Available",
                                            tint = NavyDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "New App Version Available (APK v${appUpdateInfo.latestAvailableApkVersionName})",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 12.5.sp
                                        )
                                        Text(
                                            text = "Installed: v${appUpdateInfo.currentAppVersionName} (${appUpdateInfo.currentAppVersionCode}) → Latest: Build ${appUpdateInfo.latestAvailableApkVersionCode}",
                                            color = GoldSoft,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onUpdateApkClick(appUpdateInfo.apkDownloadUrl) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldMetallic,
                                        contentColor = NavyDark
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_update_apk_home")
                                ) {
                                    Text(
                                        text = "Download & Install",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (appUpdateInfo.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = buildString {
                                        if (appUpdateInfo.releaseDate.isNotBlank()) {
                                            append("${appUpdateInfo.releaseDate} • ")
                                        }
                                        append(appUpdateInfo.releaseNotes)
                                    },
                                    color = Color(0xFFFFEDD5),
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                // Cloud Status, Google Sign-In Auto-Sync & Quick Check Action
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (pendingUpdateManifest != null) GoldDark else SuccessGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val lastTimeLabel = if (lastSyncTimestamp > 0L) {
                                    SimpleDateFormat("hh:mm a", Locale.US).format(Date(lastSyncTimestamp))
                                } else {
                                    "Ready"
                                }
                                Text(
                                    text = "ISCEB Cloud: ${publishManifest.versionTag} • Build ${appUpdateInfo.currentAppVersionCode} • Sync: $lastTimeLabel",
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            TextButton(
                                onClick = onCheckUpdatesClick,
                                modifier = Modifier.height(30.dp).testTag("btn_check_curriculum_updates")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Check Updates",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (cloudUserEmail != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (cloudUserEmail != null) SuccessGreen else GoldDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (cloudUserEmail != null) {
                                        "Auto-Sync Active: $cloudUserEmail"
                                    } else {
                                        "Sign in with Google to enable automatic Cloud Sync"
                                    },
                                    fontSize = 10.sp,
                                    color = if (cloudUserEmail != null) Color(0xFF166534) else NavyPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (cloudUserEmail == null) {
                                OutlinedButton(
                                    onClick = onConnectCloudSyncClick,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("btn_connect_google_sync_home")
                                ) {
                                    Text(
                                        text = "Connect Google",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hero Section
        item {
            HeroSection(
                totalModules = modules.size,
                totalClasses = visibleClasses.size,
                completedClasses = progress.completedClassIds.size,
                nextClass = nextClass,
                onContinueClick = { onClassClick(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Search & Filter
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search by topic, e.g. Incoterms, EOQ, ERP...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF7A8B9E)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldMetallic,
                        unfocusedBorderColor = Color(0xFFD6E2F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf("All", "Part 1: Basic", "Part 2: Advance", "Completed", "With Media")
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterChange(filter) },
                            label = {
                                Text(
                                    text = filter,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = GoldMetallic,
                                containerColor = Color.White,
                                labelColor = NavyPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) GoldMetallic else Color(0xFFD6E2F0)
                            )
                        )
                    }
                }
            }
        }

        // Admin Create Action Banner
        if (isAdmin) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Curriculum Administration",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    )

                    Button(
                        onClick = onCreateModuleClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldMetallic,
                            contentColor = NavyPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_create_module_home")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Module", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Featured Modules Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Core Curriculum Modules",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        fontSize = 18.sp
                    )
                )

                TextButton(onClick = onSeeAllModules) {
                    Text("See All (${modules.size})", color = GoldDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = GoldDark, modifier = Modifier.size(14.dp))
                }
            }
        }

        items(displayedModules, key = { it.id }) { mod ->
            val moduleClasses = classesByModule[mod.id].orEmpty()
            val completedCount = moduleClasses.count { it.isCompleted }

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ModuleCard(
                    module = mod,
                    totalClasses = moduleClasses.size,
                    completedClasses = completedCount,
                    isAdmin = isAdmin,
                    onClick = { onModuleClick(mod) },
                    onEditClick = { onEditModule(mod) },
                    onDeleteClick = { onDeleteModule(mod) }
                )
            }
        }
    }
}
