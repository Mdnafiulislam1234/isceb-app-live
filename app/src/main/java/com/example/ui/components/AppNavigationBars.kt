package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.example.ui.icons.AdminPanelSettings
import com.example.ui.icons.Bookmark
import com.example.ui.icons.Cloud
import com.example.ui.icons.GridView
import com.example.ui.icons.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import com.example.ui.icons.Verified
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.viewmodel.NavTab

@Composable
fun AppTopBar(
    title: String,
    currentUser: UserProfile,
    isAdmin: Boolean,
    onAdminClick: () -> Unit,
    onVerificationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFirebaseClick: () -> Unit,
    onPhoneInstallClick: () -> Unit = {},
    isTeacher: Boolean = false,
    modifier: Modifier = Modifier
) {
    val activeAdminRole = currentUser.designation.ifBlank {
        if (currentUser.id == "admin-2") "Founder of ISCEB" else "Developer of ISCEB App"
    }
    val nonAdminSubtitle = when {
        currentUser.id == "guest-unauthenticated" -> "Guest Preview • Sign in on Profile"
        isTeacher || currentUser.role == UserRole.TEACHER -> "${currentUser.fullName} • Faculty"
        else -> "${currentUser.fullName} • ${currentUser.studentId.ifBlank { "PGD Candidate" }}"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NavyPrimary,
        shadowElevation = 6.dp
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWideTopBar = maxWidth >= 440.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isWideTopBar) 12.dp else 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isAdmin) {
                        val photoRes = if (currentUser.id == "admin-2") {
                            R.drawable.img_admin_amirul
                        } else {
                            R.drawable.img_admin_nafiul
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, GoldMetallic, CircleShape)
                                .background(Color.White)
                                .testTag("img_admin_topbar_photo"),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = photoRes),
                                contentDescription = "${currentUser.fullName} - $activeAdminRole",
                                modifier = Modifier.fillMaxSize(),
                                alignment = androidx.compose.ui.BiasAlignment(0f, -0.35f),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .width(if (isWideTopBar) 50.dp else 44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(horizontal = 3.dp, vertical = 2.dp)
                                .testTag("img_student_topbar_logo"),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_isceb_logo),
                                contentDescription = "ISCEB - Connect. learn. Achieve.",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 68.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isAdmin) {
                            Text(
                                text = currentUser.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.2.sp,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = activeAdminRole,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = GoldSoft,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.1.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 0.3.sp,
                                    fontSize = 15.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible,
                                modifier = Modifier.testTag("txt_topbar_brand_title")
                            )
                            Text(
                                text = nonAdminSubtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFB0C2D6),
                                    fontSize = 10.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isWideTopBar) 3.dp else 2.dp)
                ) {
                    Surface(
                        onClick = onPhoneInstallClick,
                        shape = RoundedCornerShape(16.dp),
                        color = GoldMetallic,
                        contentColor = NavyDark,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_top_phone_install")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = if (isWideTopBar) 9.dp else 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share & Install ISCEB",
                                tint = NavyDark,
                                modifier = Modifier.size(15.dp)
                            )
                            if (isWideTopBar) {
                                Text(
                                    text = "Share & Install",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark,
                                        fontSize = 10.5.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onVerificationClick,
                        modifier = Modifier.size(32.dp).testTag("btn_top_verify")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verify Certificate Registry",
                            tint = GoldSoft,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(32.dp).testTag("btn_top_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Governance Settings",
                                tint = GoldMetallic,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onFirebaseClick,
                        modifier = Modifier.size(32.dp).testTag("btn_firebase_info")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Cloud Architecture & Guide",
                            tint = Color(0xFFC0D2E5),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isAdmin) GoldMetallic else NavySecondary)
                            .clickable { onAdminClick() }
                            .padding(horizontal = 8.dp)
                            .testTag("admin_badge_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                                contentDescription = if (isAdmin) "Admin Active" else "Admin Login",
                                tint = if (isAdmin) NavyPrimary else GoldMetallic,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isAdmin) "ADMIN" else "LOGIN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAdmin) NavyPrimary else Color.White,
                                    fontSize = 9.5.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppBottomBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    completedCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = NavyDark,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "Home",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                isSelected = currentTab == NavTab.HOME,
                testTag = "nav_home",
                onClick = { onTabSelected(NavTab.HOME) }
            )

            BottomNavItem(
                label = "Modules",
                selectedIcon = Icons.Filled.GridView,
                unselectedIcon = Icons.Outlined.GridView,
                isSelected = currentTab == NavTab.MODULES,
                testTag = "nav_modules",
                onClick = { onTabSelected(NavTab.MODULES) }
            )

            BottomNavItem(
                label = "Learning",
                selectedIcon = Icons.Filled.Bookmark,
                unselectedIcon = Icons.Outlined.Bookmark,
                isSelected = currentTab == NavTab.MY_LEARNING,
                badgeCount = completedCount,
                testTag = "nav_my_learning",
                onClick = { onTabSelected(NavTab.MY_LEARNING) }
            )

            BottomNavItem(
                label = "Members",
                selectedIcon = Icons.Filled.Groups,
                unselectedIcon = Icons.Outlined.Groups,
                isSelected = currentTab == NavTab.MEMBERS,
                testTag = "nav_members",
                onClick = { onTabSelected(NavTab.MEMBERS) }
            )

            BottomNavItem(
                label = "Profile",
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                isSelected = currentTab == NavTab.PROFILE,
                testTag = "nav_profile",
                onClick = { onTabSelected(NavTab.PROFILE) }
            )
        }
    }
}

@Composable
fun AppNavigationRail(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    completedCount: Int,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = NavyDark,
        contentColor = Color.White
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        val items = listOf(
            Triple(NavTab.HOME, "Home", Pair(Icons.Filled.Home, Icons.Outlined.Home)),
            Triple(NavTab.MODULES, "Modules", Pair(Icons.Filled.GridView, Icons.Outlined.GridView)),
            Triple(NavTab.MY_LEARNING, "Learning", Pair(Icons.Filled.Bookmark, Icons.Outlined.Bookmark)),
            Triple(NavTab.MEMBERS, "Members", Pair(Icons.Filled.Groups, Icons.Outlined.Groups)),
            Triple(NavTab.PROFILE, "Profile", Pair(Icons.Filled.Person, Icons.Outlined.Person))
        )

        items.forEach { (tab, label, icons) ->
            val selected = currentTab == tab
            NavigationRailItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    if (tab == NavTab.MY_LEARNING && completedCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = GoldMetallic, contentColor = NavyPrimary) {
                                    Text(completedCount.toString(), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        ) {
                            Icon(if (selected) icons.first else icons.second, contentDescription = label)
                        }
                    } else {
                        Icon(if (selected) icons.first else icons.second, contentDescription = label)
                    }
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = GoldMetallic,
                    selectedTextColor = GoldMetallic,
                    indicatorColor = NavySecondary,
                    unselectedIconColor = Color(0xFF8FA1B7),
                    unselectedTextColor = Color(0xFF8FA1B7)
                )
            )
        }
    }
}

@Composable
fun AppDesktopSidebar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    completedCount: Int,
    totalCount: Int,
    isAdmin: Boolean,
    isTeacher: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(236.dp)
            .fillMaxHeight()
            .testTag("desktop_sidebar"),
        color = NavyDark,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_isceb_logo),
                        contentDescription = "ISCEB Desktop Portal",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ISCEB WORKSPACE",
                    color = Color(0xFF8FA1B7),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                val items = listOf(
                    Triple(NavTab.HOME, "Dashboard", Pair(Icons.Filled.Home, Icons.Outlined.Home)),
                    Triple(NavTab.MODULES, "All 12 Modules", Pair(Icons.Filled.GridView, Icons.Outlined.GridView)),
                    Triple(NavTab.MY_LEARNING, "My Learning", Pair(Icons.Filled.Bookmark, Icons.Outlined.Bookmark)),
                    Triple(NavTab.MEMBERS, "ISCEB Directory", Pair(Icons.Filled.Groups, Icons.Outlined.Groups)),
                    Triple(NavTab.PROFILE, "Account & Sync", Pair(Icons.Filled.Person, Icons.Outlined.Person))
                )

                items.forEach { (tab, label, icons) ->
                    val selected = currentTab == tab
                    Surface(
                        onClick = { onTabSelected(tab) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) NavySecondary else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("desktop_nav_${tab.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (selected) icons.first else icons.second,
                                    contentDescription = label,
                                    tint = if (selected) GoldMetallic else Color(0xFFA0B3C8),
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = label,
                                    color = if (selected) GoldMetallic else Color(0xFFE2E8F0),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                            if (tab == NavTab.MY_LEARNING && totalCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) GoldMetallic else NavyPrimary
                                ) {
                                    Text(
                                        text = "$completedCount/$totalCount",
                                        color = if (selected) NavyDark else GoldSoft,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = NavyPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = when {
                            isAdmin -> "ADMIN PORTAL ACTIVE"
                            isTeacher -> "TEACHER PORTAL ACTIVE"
                            else -> "DESKTOP / TABLET MODE"
                        },
                        color = GoldMetallic,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Synced with ISCEB Live Cloud",
                        color = Color(0xFFC0D2E5),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    badgeCount: Int? = null,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (badgeCount != null && badgeCount > 0) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = GoldMetallic,
                        contentColor = NavyPrimary
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = if (isSelected) GoldMetallic else Color(0xFF8FA1B7),
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Icon(
                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = if (isSelected) GoldMetallic else Color(0xFF8FA1B7),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) GoldMetallic else Color(0xFF8FA1B7),
                fontSize = 10.5.sp
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}
