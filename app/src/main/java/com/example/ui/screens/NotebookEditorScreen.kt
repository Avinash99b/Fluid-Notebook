package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.SaveAs
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Toc
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CellType
import com.example.model.NotebookCell
import com.example.ui.components.CellItemCard
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.LiquidGlassPillButton
import com.example.ui.components.ThemeModeSegmentedToggle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.ActiveNotebookState
import kotlinx.coroutines.launch

@Composable
fun NotebookEditorScreen(
    state: ActiveNotebookState,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNavigateBack: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveAndExit: () -> Unit,
    onSaveAsDeviceFileClick: (suggestedName: String) -> Unit,
    onSaveAsWorkspaceCopy: (newFileName: String) -> Unit,
    onRenameNotebook: (newName: String) -> Unit,
    onOpenAnotherFileClick: () -> Unit,
    onSetEditingCell: (String?) -> Unit,
    onUpdateCellSource: (cellId: String, newSource: String) -> Unit,
    onChangeCellType: (cellId: String, newType: CellType) -> Unit,
    onRunCell: (cellId: String) -> Unit,
    onRunAllCells: () -> Unit,
    onAddCellAt: (index: Int, cellType: CellType) -> Unit,
    onDuplicateCell: (cellId: String) -> Unit,
    onMoveCell: (cellId: String, delta: Int) -> Unit,
    onDeleteCell: (cellId: String) -> Unit,
    onToggleCellCollapse: (cellId: String) -> Unit,
    onClearAllOutputs: () -> Unit,
    onToggleRawJsonMode: () -> Unit,
    onUpdateRawJsonDraft: (String) -> Unit,
    onToggleSearchBar: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassColors.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showSaveAsDialog by rememberSaveable { mutableStateOf(false) }
    var showUnsavedExitDialog by rememberSaveable { mutableStateOf(false) }
    var showOutlineDrawer by rememberSaveable { mutableStateOf(false) }

    val handleBackNavigation = {
        if (state.isDirty) {
            showUnsavedExitDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Mandatory BackHandler for secondary screen
    BackHandler(onBack = handleBackNavigation)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        val isWideScreen = maxWidth >= 760.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Liquid Glass Command Bar
            EditorTopGlassBar(
                state = state,
                themeMode = themeMode,
                isWideScreen = isWideScreen,
                onBackClick = handleBackNavigation,
                onSaveClick = onSaveClick,
                onSaveAsClick = { showSaveAsDialog = true },
                onRenameClick = { showRenameDialog = true },
                onOpenAnotherFileClick = onOpenAnotherFileClick,
                onClearAllOutputs = onClearAllOutputs,
                onToggleRawJsonMode = onToggleRawJsonMode,
                onToggleSearchBar = onToggleSearchBar,
                onToggleOutline = { showOutlineDrawer = !showOutlineDrawer },
                onThemeModeChange = onThemeModeChange,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            // Expandable Search Bar
            AnimatedVisibility(
                visible = state.isSearchBarVisible,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_search_input"),
                        singleLine = true,
                        placeholder = { Text("Search code or markdown in ${state.displayName}…") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = onToggleSearchBar) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Close search"
                                )
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
                }
            }

            // Collapsible Outline Strip on Compact Screens
            AnimatedVisibility(
                visible = showOutlineDrawer && !isWideScreen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                NotebookOutlineCard(
                    cells = state.notebook.cells,
                    onJumpToCell = { index ->
                        showOutlineDrawer = false
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Main Workspace Area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = if (isWideScreen) 20.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isWideScreen) {
                    // Persistent Left Outline & File Inspector Pane on Tablets/Foldables
                    Column(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NotebookOutlineCard(
                            cells = state.notebook.cells,
                            onJumpToCell = { idx ->
                                coroutineScope.launch {
                                    listState.animateScrollToItem(idx)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Right / Main Notebook Content or Raw JSON Editor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (state.isRawJsonMode) {
                        RawJsonEditorPane(
                            jsonDraft = state.rawJsonDraft,
                            jsonError = state.rawJsonError,
                            onJsonChange = onUpdateRawJsonDraft,
                            onExitRawMode = onToggleRawJsonMode
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("notebook_cells_list"),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 110.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(
                                items = state.notebook.cells,
                                key = { _, cell -> cell.id }
                            ) { index, cell ->
                                val isEditing = state.activeEditingCellId == cell.id
                                CellItemCard(
                                    cell = cell,
                                    cellIndex = index,
                                    totalCells = state.notebook.cells.size,
                                    isEditing = isEditing,
                                    searchQuery = state.searchQuery,
                                    onToggleEdit = {
                                        onSetEditingCell(if (isEditing) null else cell.id)
                                    },
                                    onSourceChange = { newText ->
                                        onUpdateCellSource(cell.id, newText)
                                    },
                                    onChangeCellType = { newType ->
                                        onChangeCellType(cell.id, newType)
                                    },
                                    onRunCell = { onRunCell(cell.id) },
                                    onMoveUp = { onMoveCell(cell.id, -1) },
                                    onMoveDown = { onMoveCell(cell.id, 1) },
                                    onDuplicateCell = { onDuplicateCell(cell.id) },
                                    onDeleteCell = { onDeleteCell(cell.id) },
                                    onToggleCollapse = { onToggleCellCollapse(cell.id) },
                                    onInsertBelow = { type ->
                                        onAddCellAt(index + 1, type)
                                    },
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
                    }

                    // Floating Liquid Glass Bottom Action Bar
                    FloatingEditorBottomDock(
                        isDirty = state.isDirty,
                        onAddCodeCell = {
                            onAddCellAt(state.notebook.cells.size, CellType.CODE)
                            coroutineScope.launch {
                                listState.animateScrollToItem(state.notebook.cells.size)
                            }
                        },
                        onAddMarkdownCell = {
                            onAddCellAt(state.notebook.cells.size, CellType.MARKDOWN)
                            coroutineScope.launch {
                                listState.animateScrollToItem(state.notebook.cells.size)
                            }
                        },
                        onRunAllCells = onRunAllCells,
                        onSaveClick = onSaveClick,
                        onSaveAsClick = { showSaveAsDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 12.dp)
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        RenameNotebookDialog(
            currentName = state.displayName,
            onDismiss = { showRenameDialog = false },
            onConfirmRename = { newName ->
                showRenameDialog = false
                onRenameNotebook(newName)
            }
        )
    }

    if (showSaveAsDialog) {
        SaveAsNotebookDialog(
            currentName = state.displayName,
            onDismiss = { showSaveAsDialog = false },
            onSaveCopyInWorkspace = { newName ->
                showSaveAsDialog = false
                onSaveAsWorkspaceCopy(newName)
            },
            onExportToDeviceStorage = { suggestedName ->
                showSaveAsDialog = false
                onSaveAsDeviceFileClick(suggestedName)
            }
        )
    }

    if (showUnsavedExitDialog) {
        LiquidGlassDialog(
            title = stringResource(R.string.dialog_unsaved_title),
            subtitle = stringResource(R.string.dialog_unsaved_message),
            onDismiss = { showUnsavedExitDialog = false },
            confirmButtonText = stringResource(R.string.btn_save_and_exit),
            dismissButtonText = stringResource(R.string.btn_discard),
            onConfirm = {
                showUnsavedExitDialog = false
                onSaveAndExit()
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Or tap outside to keep editing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditorTopGlassBar(
    state: ActiveNotebookState,
    themeMode: ThemeMode,
    isWideScreen: Boolean,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveAsClick: () -> Unit,
    onRenameClick: () -> Unit,
    onOpenAnotherFileClick: () -> Unit,
    onClearAllOutputs: () -> Unit,
    onToggleRawJsonMode: () -> Unit,
    onToggleSearchBar: () -> Unit,
    onToggleOutline: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassColors.current
    var showFileMenu by remember { mutableStateOf(false) }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevated = true,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button + File Title (tap to rename)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("back_to_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onRenameClick() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("rename_file_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.DriveFileRenameOutline,
                                contentDescription = "Rename file",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val statusColor = if (state.isDirty) glass.badgeDirty else glass.badgeSaved
                            val statusText = if (state.isDirty) {
                                stringResource(R.string.unsaved_changes)
                            } else {
                                stringResource(R.string.saved_clean)
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = "$statusText · ${state.notebook.metadata.kernelDisplayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Right Quick Icons: Search, Outline, Raw JSON, Theme
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onToggleSearchBar,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("toggle_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search inside notebook",
                            tint = if (state.isSearchBarVisible) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (!isWideScreen) {
                        IconButton(
                            onClick = onToggleOutline,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("toggle_outline_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Toc,
                                contentDescription = "Notebook outline",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleRawJsonMode,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("toggle_raw_json_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = "Inspect Raw .ipynb JSON",
                            tint = if (state.isRawJsonMode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (isWideScreen) {
                        ThemeModeSegmentedToggle(
                            currentMode = themeMode,
                            onModeSelected = onThemeModeChange
                        )
                    }
                }
            }

            // Second Row: Explicit File Operations Strip (Save, Save As, Rename, Open, Clear Outputs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileActionChip(
                    label = stringResource(R.string.action_save),
                    icon = Icons.Filled.Save,
                    isHighlighted = state.isDirty,
                    onClick = onSaveClick,
                    testTag = "save_file_button"
                )

                FileActionChip(
                    label = stringResource(R.string.action_save_as),
                    icon = Icons.Outlined.SaveAs,
                    isHighlighted = false,
                    onClick = onSaveAsClick,
                    testTag = "save_as_button"
                )

                FileActionChip(
                    label = stringResource(R.string.action_rename),
                    icon = Icons.Outlined.DriveFileRenameOutline,
                    isHighlighted = false,
                    onClick = onRenameClick,
                    testTag = "rename_action_button"
                )

                FileActionChip(
                    label = "Open File",
                    icon = Icons.Outlined.FolderOpen,
                    isHighlighted = false,
                    onClick = onOpenAnotherFileClick,
                    testTag = "editor_open_file_button"
                )

                Box {
                    FileActionChip(
                        label = stringResource(R.string.action_clear_outputs),
                        icon = Icons.Outlined.CleaningServices,
                        isHighlighted = false,
                        onClick = onClearAllOutputs,
                        testTag = "clear_outputs_button"
                    )
                    DropdownMenu(
                        expanded = showFileMenu,
                        onDismissRequest = { showFileMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear All Outputs") },
                            onClick = {
                                showFileMenu = false
                                onClearAllOutputs()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val glass = LocalLiquidGlassColors.current
    val bgColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        glass.glassSurfaceTop
    }
    val fgColor = if (isHighlighted) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else glass.glassBorderMid,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isHighlighted) fgColor else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = fgColor
        )
    }
}

@Composable
private fun NotebookOutlineCard(
    cells: List<NotebookCell>,
    onJumpToCell: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassColors.current

    LiquidGlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Cell Navigator (${cells.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(cells) { index, cell ->
                    val firstLine = cell.source.lineSequence()
                        .firstOrNull { it.isNotBlank() }
                        ?.removePrefix("#")
                        ?.trim()
                        ?: "(Empty ${cell.cellType.displayLabel} cell)"

                    val badgeColor = if (cell.cellType == CellType.CODE) {
                        glass.badgeCode
                    } else {
                        glass.badgeMarkdown
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeColor.copy(alpha = 0.08f))
                            .clickable { onJumpToCell(index) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeColor,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = firstLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RawJsonEditorPane(
    jsonDraft: String,
    jsonError: String?,
    onJsonChange: (String) -> Unit,
    onExitRawMode: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 86.dp),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Raw .ipynb JSON Source",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = jsonError ?: "Valid nbformat v4 JSON — changes sync directly to cells",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (jsonError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            glass.badgeSaved
                        }
                    )
                }

                LiquidGlassPillButton(
                    text = "Back to Cells",
                    onClick = onExitRawMode,
                    isPrimary = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = jsonDraft,
                onValueChange = onJsonChange,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("raw_json_editor_input"),
                textStyle = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = glass.glassBorderMid,
                    focusedContainerColor = glass.codeBackground,
                    unfocusedContainerColor = glass.codeBackground
                )
            )
        }
    }
}

@Composable
private fun FloatingEditorBottomDock(
    isDirty: Boolean,
    onAddCodeCell: () -> Unit,
    onAddMarkdownCell: () -> Unit,
    onRunAllCells: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveAsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassColors.current

    LiquidGlassCard(
        modifier = modifier,
        cornerRadius = 28.dp,
        elevated = true,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassPillButton(
                text = "+ Code",
                icon = Icons.Filled.Add,
                onClick = onAddCodeCell,
                isPrimary = false,
                accentColor = glass.badgeCode,
                modifier = Modifier.testTag("dock_add_code_button")
            )

            LiquidGlassPillButton(
                text = "+ Markdown",
                icon = Icons.Outlined.Notes,
                onClick = onAddMarkdownCell,
                isPrimary = false,
                accentColor = glass.badgeMarkdown,
                modifier = Modifier.testTag("dock_add_markdown_button")
            )

            LiquidGlassPillButton(
                text = "Run All",
                icon = Icons.Filled.PlayArrow,
                onClick = onRunAllCells,
                isPrimary = false,
                accentColor = glass.badgeSaved,
                modifier = Modifier.testTag("dock_run_all_button")
            )

            LiquidGlassPillButton(
                text = if (isDirty) "Save*" else "Save",
                icon = Icons.Filled.Save,
                onClick = onSaveClick,
                isPrimary = isDirty,
                modifier = Modifier.testTag("dock_save_button")
            )

            LiquidGlassPillButton(
                text = "Save As…",
                icon = Icons.Outlined.SaveAs,
                onClick = onSaveAsClick,
                isPrimary = false,
                modifier = Modifier.testTag("dock_save_as_button")
            )
        }
    }
}

@Composable
private fun RenameNotebookDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirmRename: (String) -> Unit
) {
    var nameInput by rememberSaveable(currentName) {
        mutableStateOf(currentName.removeSuffix(".ipynb"))
    }

    LiquidGlassDialog(
        title = stringResource(R.string.dialog_rename_title),
        subtitle = "Enter a new filename. The .ipynb extension will be preserved automatically.",
        onDismiss = onDismiss,
        confirmButtonText = stringResource(R.string.action_rename),
        onConfirm = {
            if (nameInput.isNotBlank()) {
                onConfirmRename("$nameInput.ipynb")
            }
        }
    ) {
        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Notebook name") },
            suffix = { Text(".ipynb", fontFamily = JetBrainsMonoFontFamily) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rename_notebook_input"),
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun SaveAsNotebookDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSaveCopyInWorkspace: (String) -> Unit,
    onExportToDeviceStorage: (String) -> Unit
) {
    val defaultCopyName = remember(currentName) {
        val base = currentName.removeSuffix(".ipynb")
        "${base}_copy.ipynb"
    }
    var fileNameInput by rememberSaveable(defaultCopyName) { mutableStateOf(defaultCopyName) }

    LiquidGlassDialog(
        title = "Save Notebook As…",
        subtitle = "Save a new copy directly to your Lumina workspace or export the .ipynb file to any folder on your Android device.",
        onDismiss = onDismiss,
        confirmButtonText = "Save in Workspace",
        onConfirm = {
            if (fileNameInput.isNotBlank()) {
                onSaveCopyInWorkspace(fileNameInput)
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = fileNameInput,
                onValueChange = { fileNameInput = it },
                label = { Text("New filename (.ipynb)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_as_filename_input"),
                shape = RoundedCornerShape(14.dp)
            )

            LiquidGlassPillButton(
                text = "Export to Device Storage…",
                icon = Icons.Outlined.FileCopy,
                onClick = {
                    val target = fileNameInput.ifBlank { defaultCopyName }
                    onExportToDeviceStorage(
                        if (target.endsWith(".ipynb", ignoreCase = true)) target else "$target.ipynb"
                    )
                },
                isPrimary = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_device_storage_button")
            )
        }
    }
}
