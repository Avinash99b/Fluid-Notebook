package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.RecentNotebookEntity
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.LiquidGlassPillButton
import com.example.ui.components.ThemeModeSegmentedToggle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.RecentFilter
import java.util.Locale

@Composable
fun HomeScreen(
    recentNotebooks: List<RecentNotebookEntity>,
    totalRecentCount: Int,
    searchQuery: String,
    selectedFilter: RecentFilter,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (RecentFilter) -> Unit,
    onSelectFileClick: () -> Unit,
    onCreateNewNotebook: (fileName: String, kernelDisplay: String, language: String) -> Unit,
    onOpenRecentNotebook: (String) -> Unit,
    onTogglePinNotebook: (String, Boolean) -> Unit,
    onRemoveRecentNotebook: (String) -> Unit,
    onClearAllRecentClick: () -> Unit,
    onRestoreSamplesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearAllDialog by rememberSaveable { mutableStateOf(false) }
    var showNewNotebookDialog by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        val isExpanded = maxWidth >= 700.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isExpanded) 24.dp else 16.dp)
        ) {
            // Top Header Row
            HomeTopHeader(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                modifier = Modifier.padding(top = 12.dp, bottom = 14.dp)
            )

            if (isExpanded) {
                // Canonical Two-Pane Layout for Tablets, Foldables & Landscape
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HeroSelectFileBanner(
                            onSelectFileClick = onSelectFileClick,
                            onNewNotebookClick = { showNewNotebookDialog = true }
                        )

                        WorkspaceQuickStatsCard(
                            recentNotebooks = recentNotebooks,
                            totalCount = totalRecentCount
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                    ) {
                        RecentFilesHeaderAndSearch(
                            totalRecentCount = totalRecentCount,
                            searchQuery = searchQuery,
                            selectedFilter = selectedFilter,
                            onSearchQueryChange = onSearchQueryChange,
                            onFilterChange = onFilterChange,
                            onClearAllRequest = { showClearAllDialog = true }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        RecentNotebooksListContent(
                            recentNotebooks = recentNotebooks,
                            totalRecentCount = totalRecentCount,
                            onOpenRecentNotebook = onOpenRecentNotebook,
                            onTogglePinNotebook = onTogglePinNotebook,
                            onRemoveRecentNotebook = onRemoveRecentNotebook,
                            onSelectFileClick = onSelectFileClick,
                            onRestoreSamplesClick = onRestoreSamplesClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // Mobile / Compact Single-Column Fluid Layout
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_lazy_column"),
                    contentPadding = PaddingValues(bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "hero_banner") {
                        HeroSelectFileBanner(
                            onSelectFileClick = onSelectFileClick,
                            onNewNotebookClick = { showNewNotebookDialog = true }
                        )
                    }

                    item(key = "recent_header_and_filters") {
                        RecentFilesHeaderAndSearch(
                            totalRecentCount = totalRecentCount,
                            searchQuery = searchQuery,
                            selectedFilter = selectedFilter,
                            onSearchQueryChange = onSearchQueryChange,
                            onFilterChange = onFilterChange,
                            onClearAllRequest = { showClearAllDialog = true }
                        )
                    }

                    if (recentNotebooks.isEmpty()) {
                        item(key = "empty_recent_state") {
                            EmptyRecentFilesCard(
                                hasTotalItems = totalRecentCount > 0,
                                onSelectFileClick = onSelectFileClick,
                                onRestoreSamplesClick = onRestoreSamplesClick
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = recentNotebooks,
                            key = { _, item -> item.id }
                        ) { index, item ->
                            RecentNotebookRowCard(
                                item = item,
                                index = index,
                                onClick = { onOpenRecentNotebook(item.id) },
                                onTogglePin = { onTogglePinNotebook(item.id, item.isPinned) },
                                onRemove = { onRemoveRecentNotebook(item.id) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            )
                        }
                    }

                    item(key = "bottom_nav_spacer") {
                        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                    }
                }
            }
        }
    }

    if (showClearAllDialog) {
        LiquidGlassDialog(
            title = stringResource(R.string.dialog_clear_all_title),
            subtitle = stringResource(R.string.dialog_clear_all_message),
            onDismiss = { showClearAllDialog = false },
            confirmButtonText = stringResource(R.string.clear_all),
            isDestructive = true,
            onConfirm = {
                showClearAllDialog = false
                onClearAllRecentClick()
            }
        )
    }

    if (showNewNotebookDialog) {
        CreateNewNotebookDialog(
            onDismiss = { showNewNotebookDialog = false },
            onCreate = { fileName, kernelDisplay, lang ->
                showNewNotebookDialog = false
                onCreateNewNotebook(fileName, kernelDisplay, lang)
            }
        )
    }
}

@Composable
private fun HomeTopHeader(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "{J}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        ThemeModeSegmentedToggle(
            currentMode = themeMode,
            onModeSelected = onThemeModeChange
        )
    }
}

@Composable
private fun HeroSelectFileBanner(
    onSelectFileClick: () -> Unit,
    onNewNotebookClick: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_select_file_card"),
        cornerRadius = 26.dp,
        elevated = true,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle Liquid Glass Hero Artwork Backdrop
            Image(
                painter = painterResource(id = R.drawable.img_liquid_hero_1794272812882),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(26.dp)),
                alpha = if (glass.isDark) 0.34f else 0.16f
            )

            // Gradient Overlay for high-contrast typography
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                glass.glassElevatedTop.copy(alpha = 0.72f),
                                glass.glassElevatedBottom.copy(alpha = 0.94f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "NBFORMAT v4 · LIQUID GLASS ENGINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Open, Inspect & Edit Jupyter Notebooks",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Select any .ipynb file from your device storage to view formatted code, inline plots, and markdown—or edit, rename, and save changes effortlessly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassPillButton(
                        text = stringResource(R.string.select_file),
                        icon = Icons.Filled.FolderOpen,
                        onClick = onSelectFileClick,
                        isPrimary = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_file_button")
                    )

                    LiquidGlassPillButton(
                        text = stringResource(R.string.new_notebook),
                        icon = Icons.Filled.Add,
                        onClick = onNewNotebookClick,
                        isPrimary = false,
                        modifier = Modifier.testTag("new_notebook_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceQuickStatsCard(
    recentNotebooks: List<RecentNotebookEntity>,
    totalCount: Int
) {
    val totalCells = remember(recentNotebooks) { recentNotebooks.sumOf { it.cellCount } }
    val totalCode = remember(recentNotebooks) { recentNotebooks.sumOf { it.codeCellCount } }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatColumn(value = totalCount.toString(), label = "Notebooks")
            StatColumn(value = totalCells.toString(), label = "Total Cells")
            StatColumn(value = totalCode.toString(), label = "Code Blocks")
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecentFilesHeaderAndSearch(
    totalRecentCount: Int,
    searchQuery: String,
    selectedFilter: RecentFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (RecentFilter) -> Unit,
    onClearAllRequest: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.recent_files),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = totalRecentCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(
                visible = totalRecentCount > 0,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut()
            ) {
                LiquidGlassPillButton(
                    text = stringResource(R.string.clear_all),
                    icon = Icons.Outlined.DeleteSweep,
                    onClick = onClearAllRequest,
                    isPrimary = false,
                    accentColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("clear_all_button")
                )
            }
        }

        // Search Input + Filter Pills
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_recent_input"),
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_recent_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search recent notebooks",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear search query"
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = glass.glassBorderMid,
                focusedContainerColor = glass.glassSurfaceTop,
                unfocusedContainerColor = glass.glassSurfaceBottom
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecentFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                glass.glassSurfaceTop
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                glass.glassBorderMid
                            },
                            shape = CircleShape
                        )
                        .clickable { onFilterChange(filter) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentNotebooksListContent(
    recentNotebooks: List<RecentNotebookEntity>,
    totalRecentCount: Int,
    onOpenRecentNotebook: (String) -> Unit,
    onTogglePinNotebook: (String, Boolean) -> Unit,
    onRemoveRecentNotebook: (String) -> Unit,
    onSelectFileClick: () -> Unit,
    onRestoreSamplesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (recentNotebooks.isEmpty()) {
        EmptyRecentFilesCard(
            hasTotalItems = totalRecentCount > 0,
            onSelectFileClick = onSelectFileClick,
            onRestoreSamplesClick = onRestoreSamplesClick
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = recentNotebooks,
                key = { _, item -> item.id }
            ) { index, item ->
                RecentNotebookRowCard(
                    item = item,
                    index = index,
                    onClick = { onOpenRecentNotebook(item.id) },
                    onTogglePin = { onTogglePinNotebook(item.id, item.isPinned) },
                    onRemove = { onRemoveRecentNotebook(item.id) }
                )
            }
        }
    }
}

@Composable
private fun RecentNotebookRowCard(
    item: RecentNotebookEntity,
    index: Int,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassColors.current
    val relativeTime = remember(item.lastOpenedAt) {
        DateUtils.getRelativeTimeSpanString(
            item.lastOpenedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
    val formattedSize = remember(item.fileSizeBytes) {
        if (item.fileSizeBytes < 1024) {
            "${item.fileSizeBytes} B"
        } else {
            String.format(Locale.US, "%.1f KB", item.fileSizeBytes / 1024.0)
        }
    }

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_notebook_card_$index"),
        cornerRadius = 20.dp,
        elevated = item.isPinned,
        accentGlowColor = if (item.isPinned) MaterialTheme.colorScheme.primary else null,
        onClick = onClick,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // File Emblem
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${item.kernelName} · $formattedSize · $relativeTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("pin_recent_button_$index")
                    ) {
                        Icon(
                            imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (item.isPinned) "Unpin notebook" else "Pin notebook",
                            tint = if (item.isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("remove_recent_button_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove from recently opened",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cell Composition Bar & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallBadgePill(
                        text = "${item.codeCellCount} Code",
                        color = glass.badgeCode
                    )
                    SmallBadgePill(
                        text = "${item.markdownCellCount} Markdown",
                        color = glass.badgeMarkdown
                    )
                }

                val codeFraction = if (item.cellCount > 0) {
                    (item.codeCellCount.toFloat() / item.cellCount.toFloat()).coerceIn(0.15f, 0.85f)
                } else 0.5f

                Row(
                    modifier = Modifier
                        .width(96.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(glass.badgeMarkdown.copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(codeFraction)
                            .fillMaxHeight()
                            .background(glass.badgeCode)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallBadgePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun EmptyRecentFilesCard(
    hasTotalItems: Boolean,
    onSelectFileClick: () -> Unit,
    onRestoreSamplesClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_recent_files_card"),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (hasTotalItems) "No Matching Notebooks" else stringResource(R.string.empty_recent_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.empty_recent_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassPillButton(
                    text = stringResource(R.string.select_file),
                    icon = Icons.Filled.FolderOpen,
                    onClick = onSelectFileClick,
                    isPrimary = true
                )

                LiquidGlassPillButton(
                    text = stringResource(R.string.load_samples),
                    icon = Icons.Outlined.Refresh,
                    onClick = onRestoreSamplesClick,
                    isPrimary = false,
                    modifier = Modifier.testTag("restore_samples_button")
                )
            }
        }
    }
}

@Composable
private fun CreateNewNotebookDialog(
    onDismiss: () -> Unit,
    onCreate: (fileName: String, kernelDisplay: String, language: String) -> Unit
) {
    var fileName by rememberSaveable { mutableStateOf("experiment_analysis.ipynb") }
    val kernels = listOf(
        Triple("Python 3 (ipykernel)", "Python", "python3"),
        Triple("PyTorch 2.4 CUDA", "Python", "pytorch"),
        Triple("Julia 1.10", "Julia", "julia"),
        Triple("R 4.3 Statistical", "R", "ir")
    )
    var selectedKernelIdx by rememberSaveable { mutableStateOf(0) }

    LiquidGlassDialog(
        title = stringResource(R.string.dialog_new_notebook_title),
        subtitle = "Configure the filename and kernel metadata for your new .ipynb notebook.",
        onDismiss = onDismiss,
        confirmButtonText = stringResource(R.string.btn_create),
        onConfirm = {
            val chosen = kernels[selectedKernelIdx]
            onCreate(fileName, chosen.first, chosen.second)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("Notebook Filename (.ipynb)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_notebook_name_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Text(
                text = "Select Kernel Runtime",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                kernels.forEachIndexed { idx, (displayName, lang, _) ->
                    val selected = idx == selectedKernelIdx
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedKernelIdx = idx }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = lang,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
