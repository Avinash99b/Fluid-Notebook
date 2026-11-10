package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CellOutput
import com.example.model.CellType
import com.example.model.NotebookCell
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.util.SyntaxHighlighter

@Composable
fun CellItemCard(
    cell: NotebookCell,
    cellIndex: Int,
    totalCells: Int,
    isEditing: Boolean,
    searchQuery: String,
    onToggleEdit: () -> Unit,
    onSourceChange: (String) -> Unit,
    onChangeCellType: (CellType) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicateCell: () -> Unit,
    onDeleteCell: () -> Unit,
    onToggleCollapse: () -> Unit,
    onInsertBelow: (CellType) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassColors.current
    val accentColor = when (cell.cellType) {
        CellType.CODE -> glass.badgeCode
        CellType.MARKDOWN -> glass.badgeMarkdown
        CellType.RAW -> MaterialTheme.colorScheme.tertiary
    }

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .testTag("notebook_cell_card_$cellIndex"),
        cornerRadius = 20.dp,
        elevated = isEditing,
        accentGlowColor = if (isEditing) accentColor else null,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cell Top Control Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor.copy(alpha = if (glass.isDark) 0.12f else 0.08f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left badge: In [x] / Markdown + Type Switcher
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.20f))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                val nextType = when (cell.cellType) {
                                    CellType.CODE -> CellType.MARKDOWN
                                    CellType.MARKDOWN -> CellType.CODE
                                    CellType.RAW -> CellType.CODE
                                }
                                onChangeCellType(nextType)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val badgeLabel = when (cell.cellType) {
                            CellType.CODE -> "In [${cell.executionCount?.toString() ?: " "}]:"
                            CellType.MARKDOWN -> "MD · #${cellIndex + 1}"
                            CellType.RAW -> "RAW · #${cellIndex + 1}"
                        }
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = accentColor
                        )
                    }

                    Text(
                        text = cell.cellType.displayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Right Cell Action Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_cell_button_$cellIndex")
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Filled.Check else Icons.Outlined.Edit,
                            contentDescription = if (isEditing) "Finish editing cell" else "Edit cell",
                            tint = if (isEditing) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (cellIndex > 0) {
                        IconButton(
                            onClick = onMoveUp,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = "Move cell up",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    if (cellIndex < totalCells - 1) {
                        IconButton(
                            onClick = onMoveDown,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = "Move cell down",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDuplicateCell,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Duplicate cell",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleCollapse,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (cell.isCollapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                            contentDescription = if (cell.isCollapsed) "Expand cell" else "Collapse cell",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteCell,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("delete_cell_button_$cellIndex")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete cell",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Collapsible Cell Body
            AnimatedVisibility(
                visible = !cell.isCollapsed,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedContent(
                        targetState = isEditing,
                        transitionSpec = {
                            fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                        },
                        label = "cellEditModeTransition"
                    ) { editing ->
                        if (editing) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = cell.source,
                                    onValueChange = onSourceChange,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 100.dp)
                                        .testTag("cell_source_input_$cellIndex"),
                                    textStyle = TextStyle(
                                        fontFamily = if (cell.cellType == CellType.CODE) {
                                            JetBrainsMonoFontFamily
                                        } else {
                                            MaterialTheme.typography.bodyLarge.fontFamily
                                        },
                                        fontSize = 13.5.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    placeholder = {
                                        Text(
                                            text = if (cell.cellType == CellType.CODE) {
                                                "# Enter Python or R code..."
                                            } else {
                                                "# Enter Markdown notes..."
                                            },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = glass.glassBorderMid,
                                        focusedContainerColor = glass.codeBackground,
                                        unfocusedContainerColor = glass.codeBackground
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CellTypeChip(
                                            label = "Code",
                                            selected = cell.cellType == CellType.CODE,
                                            onClick = { onChangeCellType(CellType.CODE) }
                                        )
                                        CellTypeChip(
                                            label = "Markdown",
                                            selected = cell.cellType == CellType.MARKDOWN,
                                            onClick = { onChangeCellType(CellType.MARKDOWN) }
                                        )
                                    }

                                    LiquidGlassPillButton(
                                        text = "Done",
                                        icon = Icons.Filled.Check,
                                        onClick = onToggleEdit,
                                        isPrimary = true
                                    )
                                }
                            }
                        } else {
                            // View Mode
                            when (cell.cellType) {
                                CellType.CODE -> {
                                    CodeCellViewContent(
                                        code = cell.source,
                                        searchQuery = searchQuery,
                                        onClickToEdit = onToggleEdit
                                    )
                                }
                                CellType.MARKDOWN -> {
                                    MarkdownCellViewContent(
                                        markdown = cell.source,
                                        searchQuery = searchQuery,
                                        onClickToEdit = onToggleEdit
                                    )
                                }
                                CellType.RAW -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggleEdit() }
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = cell.source.ifBlank { "(Empty raw cell — tap to edit)" },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Code Outputs Section
                    if (cell.cellType == CellType.CODE && cell.outputs.isNotEmpty()) {
                        HorizontalDivider(
                            color = glass.glassBorderSubtle,
                            thickness = 1.dp
                        )
                        CellOutputsSection(
                            outputs = cell.outputs,
                            executionCount = cell.executionCount
                        )
                    }

                    // Subtle Bottom Quick-Insert Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onInsertBelow(CellType.CODE) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = glass.badgeCode,
                                modifier = Modifier.size(13.dp)
                            )
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = "Insert code cell below",
                                tint = glass.badgeCode,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "+Code",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.badgeCode
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onInsertBelow(CellType.MARKDOWN) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = glass.badgeMarkdown,
                                modifier = Modifier.size(13.dp)
                            )
                            Icon(
                                imageVector = Icons.Outlined.Notes,
                                contentDescription = "Insert markdown cell below",
                                tint = glass.badgeMarkdown,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "+Markdown",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.badgeMarkdown
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CellTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg
        )
    }
}

@Composable
private fun CodeCellViewContent(
    code: String,
    searchQuery: String,
    onClickToEdit: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val baseColor = MaterialTheme.colorScheme.onSurface
    val highlighted = remember(code, glass, baseColor, searchQuery) {
        SyntaxHighlighter.highlightPython(
            code = code.ifBlank { "# Empty code cell (tap to edit)" },
            glassColors = glass,
            baseColor = baseColor,
            searchQuery = searchQuery
        )
    }
    val lines = remember(code) {
        val count = code.lines().size.coerceAtLeast(1)
        (1..count).joinToString("\n")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(glass.codeBackground)
            .clickable { onClickToEdit() }
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        // Line numbers gutter
        Text(
            text = lines,
            style = TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.5.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(end = 12.dp)
        )

        // Vertically aligned, horizontally scrollable syntax-highlighted code
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = highlighted,
                style = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            )
        }
    }
}

@Composable
private fun MarkdownCellViewContent(
    markdown: String,
    searchQuery: String,
    onClickToEdit: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val lines = remember(markdown) { markdown.lines() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickToEdit() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (markdown.isBlank()) {
            Text(
                text = "Tap to add Markdown documentation...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            var inCodeBlock = false
            val codeBlockBuffer = StringBuilder()

            for (rawLine in lines) {
                val trimmed = rawLine.trim()
                if (trimmed.startsWith("```")) {
                    if (inCodeBlock) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(glass.codeBackground)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = codeBlockBuffer.toString().trimEnd(),
                                style = TextStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        codeBlockBuffer.clear()
                        inCodeBlock = false
                    } else {
                        inCodeBlock = true
                    }
                    continue
                }

                if (inCodeBlock) {
                    codeBlockBuffer.appendLine(rawLine)
                    continue
                }

                when {
                    trimmed.startsWith("# ") -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = parseInlineMarkdown(
                                    trimmed.removePrefix("# "),
                                    glass.codeBackground,
                                    searchQuery
                                ),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    trimmed.startsWith("## ") -> {
                        Text(
                            text = parseInlineMarkdown(
                                trimmed.removePrefix("## "),
                                glass.codeBackground,
                                searchQuery
                            ),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    trimmed.startsWith("### ") -> {
                        Text(
                            text = parseInlineMarkdown(
                                trimmed.removePrefix("### "),
                                glass.codeBackground,
                                searchQuery
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    trimmed.startsWith("> ") -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(20.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = parseInlineMarkdown(
                                    trimmed.removePrefix("> "),
                                    glass.codeBackground,
                                    searchQuery
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                        Row(
                            modifier = Modifier.padding(start = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = parseInlineMarkdown(
                                    trimmed.substring(2),
                                    glass.codeBackground,
                                    searchQuery
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    trimmed.isNotEmpty() -> {
                        Text(
                            text = parseInlineMarkdown(trimmed, glass.codeBackground, searchQuery),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun parseInlineMarkdown(
    text: String,
    codeBg: Color,
    searchQuery: String
): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val close = text.indexOf("**", i + 2)
                if (close > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, close))
                    }
                    i = close + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("`", i) -> {
                val close = text.indexOf("`", i + 1)
                if (close > i + 1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            background = codeBg,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(" ${text.substring(i + 1, close)} ")
                    }
                    i = close + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }

    if (searchQuery.isNotBlank()) {
        val plain = toAnnotatedString().text
        var idx = plain.indexOf(searchQuery, ignoreCase = true)
        while (idx >= 0) {
            addStyle(
                SpanStyle(
                    background = Color(0x66FBBF24),
                    fontWeight = FontWeight.Bold
                ),
                idx,
                (idx + searchQuery.length).coerceAtMost(plain.length)
            )
            idx = plain.indexOf(searchQuery, startIndex = idx + searchQuery.length, ignoreCase = true)
        }
    }
}

@Composable
private fun CellOutputsSection(
    outputs: List<CellOutput>,
    executionCount: Int?
) {
    val glass = LocalLiquidGlassColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(glass.codeOutputBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        outputs.forEach { output ->
            when (output) {
                is CellOutput.StreamOutput -> {
                    val isErr = output.name.equals("stderr", ignoreCase = true)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (isErr) "stderr" else "stdout",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isErr) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(end = 10.dp, top = 2.dp)
                                .width(48.dp)
                        )
                        Text(
                            text = output.text.trimEnd(),
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = if (isErr) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is CellOutput.ExecuteResultOutput -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "Out[${output.executionCount ?: executionCount ?: 1}]:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = glass.syntaxNumber,
                            modifier = Modifier
                                .padding(end = 10.dp, top = 2.dp)
                                .width(54.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            if (!output.plainText.isNullOrBlank()) {
                                Text(
                                    text = output.plainText.trimEnd(),
                                    style = TextStyle(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            if (!output.imagePngBase64.isNullOrBlank()) {
                                Base64PngPlotView(base64Png = output.imagePngBase64)
                            }
                        }
                    }
                }
                is CellOutput.DisplayDataOutput -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!output.imagePngBase64.isNullOrBlank()) {
                            Base64PngPlotView(base64Png = output.imagePngBase64)
                        } else if (!output.plainText.isNullOrBlank()) {
                            Text(
                                text = output.plainText.trimEnd(),
                                style = TextStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
                is CellOutput.ErrorOutput -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "${output.ename}: ${output.evalue}",
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                        if (output.traceback.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = output.traceback.take(6).joinToString("\n"),
                                style = TextStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Base64PngPlotView(base64Png: String) {
    val imageBitmap = remember(base64Png) {
        runCatching {
            val bytes = Base64.decode(base64Png, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Notebook inline figure output",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp)
                )
        )
    }
}
