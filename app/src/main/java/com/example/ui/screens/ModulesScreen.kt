package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CourseClass
import com.example.model.CourseModule
import com.example.model.CoursePart
import com.example.ui.components.ModuleCard
import com.example.ui.theme.BgLight
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary

@Composable
fun ModulesScreen(
    modules: List<CourseModule>,
    classes: List<CourseClass>,
    isAdmin: Boolean,
    onModuleClick: (CourseModule) -> Unit,
    onEditModule: (CourseModule) -> Unit,
    onDeleteModule: (CourseModule) -> Unit,
    onCreateModuleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPart by remember { mutableStateOf("All") }

    val partTabs = listOf(
        "All",
        "Part 1: Basic (28 Classes)",
        "Part 2: Advance (20 Classes)"
    )

    val visibleClasses = remember(classes, isAdmin) {
        if (isAdmin) classes else classes.filter { it.isPublished }
    }

    val filteredModules = modules.filter { mod ->
        val modClasses = visibleClasses.filter { it.moduleId == mod.id }
        val matchesQuery = searchQuery.isBlank() ||
            mod.title.contains(searchQuery, ignoreCase = true) ||
            mod.description.contains(searchQuery, ignoreCase = true) ||
            modClasses.any { it.title.contains(searchQuery, ignoreCase = true) }

        val matchesPart = when (selectedPart) {
            "Part 1: Basic (28 Classes)" -> mod.part == CoursePart.PART_1_BASIC
            "Part 2: Advance (20 Classes)" -> mod.part == CoursePart.PART_2_ADVANCE
            else -> true
        }
        matchesQuery && matchesPart
    }.sortedBy { it.number }

    val part1Modules = filteredModules.filter { it.part == CoursePart.PART_1_BASIC }
    val part2Modules = filteredModules.filter { it.part == CoursePart.PART_2_ADVANCE }

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = onCreateModuleClick,
                    containerColor = GoldMetallic,
                    contentColor = NavyPrimary,
                    modifier = Modifier.testTag("fab_add_module")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Module")
                }
            }
        },
        containerColor = BgLight
    ) { padding ->
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val useTwoColumns = maxWidth >= 680.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Info
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Curriculum Architecture",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = NavyPrimary,
                                fontSize = 22.sp
                            )
                        )
                        Text(
                            text = "12 Modules • 48 Structured Classes (Part 1 Basic: 28 • Part 2 Advance: 20)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7F96),
                                fontSize = 12.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search modules or classes (e.g. Sourcing, LC, ERP)...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF7A8B9E))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_modules_search"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldMetallic,
                                unfocusedBorderColor = Color(0xFFD6E2F0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(partTabs) { tab ->
                                val isSel = selectedPart == tab
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedPart = tab },
                                    label = { Text(tab, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NavyPrimary,
                                        selectedLabelColor = GoldMetallic,
                                        containerColor = Color.White,
                                        labelColor = NavyPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSel,
                                        borderColor = if (isSel) GoldMetallic else Color(0xFFD6E2F0)
                                    )
                                )
                            }
                        }
                    }
                }

                // Modules count summary
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Modules (${filteredModules.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        )
                        Text(
                            text = "Total ${modules.sumOf { it.estimatedHours }} Hours Study",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GoldDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                if (filteredModules.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_isceb_logo),
                                    contentDescription = "ISCEB - Connect. learn. Achieve.",
                                    modifier = Modifier.width(120.dp).height(64.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No matching ISCEB modules found",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try clearing your search or selecting 'All' parts.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7F96))
                                )
                            }
                        }
                    }
                } else {
                    if (part1Modules.isNotEmpty()) {
                        item {
                            PartSectionHeader(
                                title = "PART 1 / BASIC (FOUNDATION LEVEL) • 28 CLASSES",
                                subtitle = "Core Concepts & Operational Fundamentals • Lectures 01–28",
                                containerColor = NavyPrimary
                            )
                        }

                        if (useTwoColumns) {
                            val pairs = part1Modules.chunked(2)
                            items(pairs) { rowMods ->
                                ModuleGridRow(
                                    rowModules = rowMods,
                                    classes = visibleClasses,
                                    isAdmin = isAdmin,
                                    onModuleClick = onModuleClick,
                                    onEditModule = onEditModule,
                                    onDeleteModule = onDeleteModule
                                )
                            }
                        } else {
                            items(part1Modules, key = { it.id }) { mod ->
                                val modClasses = visibleClasses.filter { it.moduleId == mod.id }
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    ModuleCard(
                                        module = mod,
                                        totalClasses = modClasses.size,
                                        completedClasses = modClasses.count { it.isCompleted },
                                        isAdmin = isAdmin,
                                        onClick = { onModuleClick(mod) },
                                        onEditClick = { onEditModule(mod) },
                                        onDeleteClick = { onDeleteModule(mod) }
                                    )
                                }
                            }
                        }
                    }

                    if (part2Modules.isNotEmpty()) {
                        item {
                            PartSectionHeader(
                                title = "PART 2 / ADVANCE (STRATEGIC & ANALYTICAL LEVEL) • 20 CLASSES",
                                subtitle = "Strategic SCM, Trade, Technology & Sustainability • Lectures 29–48",
                                containerColor = NavySecondary
                            )
                        }

                        if (useTwoColumns) {
                            val pairs = part2Modules.chunked(2)
                            items(pairs) { rowMods ->
                                ModuleGridRow(
                                    rowModules = rowMods,
                                    classes = visibleClasses,
                                    isAdmin = isAdmin,
                                    onModuleClick = onModuleClick,
                                    onEditModule = onEditModule,
                                    onDeleteModule = onDeleteModule
                                )
                            }
                        } else {
                            items(part2Modules, key = { it.id }) { mod ->
                                val modClasses = visibleClasses.filter { it.moduleId == mod.id }
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    ModuleCard(
                                        module = mod,
                                        totalClasses = modClasses.size,
                                        completedClasses = modClasses.count { it.isCompleted },
                                        isAdmin = isAdmin,
                                        onClick = { onModuleClick(mod) },
                                        onEditClick = { onEditModule(mod) },
                                        onDeleteClick = { onDeleteModule(mod) }
                                    )
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
private fun PartSectionHeader(
    title: String,
    subtitle: String,
    containerColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        color = containerColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = title,
                color = GoldMetallic,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.5.sp,
                letterSpacing = 0.4.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFFD6E2F0),
                fontSize = 10.5.sp
            )
        }
    }
}

@Composable
private fun ModuleGridRow(
    rowModules: List<CourseModule>,
    classes: List<CourseClass>,
    isAdmin: Boolean,
    onModuleClick: (CourseModule) -> Unit,
    onEditModule: (CourseModule) -> Unit,
    onDeleteModule: (CourseModule) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rowModules.forEach { mod ->
            val modClasses = classes.filter { it.moduleId == mod.id }
            Box(modifier = Modifier.weight(1f)) {
                ModuleCard(
                    module = mod,
                    totalClasses = modClasses.size,
                    completedClasses = modClasses.count { it.isCompleted },
                    isAdmin = isAdmin,
                    onClick = { onModuleClick(mod) },
                    onEditClick = { onEditModule(mod) },
                    onDeleteClick = { onDeleteModule(mod) }
                )
            }
        }
        if (rowModules.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
