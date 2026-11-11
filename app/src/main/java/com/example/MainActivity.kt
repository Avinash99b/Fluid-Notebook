package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AnimatedLiquidBackground
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotebookEditorScreen
import com.example.ui.theme.LuminaNotebookTheme
import com.example.viewmodel.NotebookViewModel
import com.example.viewmodel.ScreenDestination

class MainActivity : ComponentActivity() {

    private var pendingIncomingUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            pendingIncomingUri = extractNotebookUriFromIntent(intent)
        }

        setContent {
            val notebookViewModel: NotebookViewModel = viewModel()
            val themeMode by notebookViewModel.themeMode.collectAsStateWithLifecycle()

            LaunchedEffect(pendingIncomingUri) {
                val targetUri = pendingIncomingUri
                if (targetUri != null) {
                    notebookViewModel.openExternalFileUri(targetUri)
                    pendingIncomingUri = null
                }
            }

            LuminaNotebookTheme(themeMode = themeMode) {
                LuminaNotebookApp(viewModel = notebookViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uri = extractNotebookUriFromIntent(intent)
        if (uri != null) {
            pendingIncomingUri = uri
        }
    }

    private fun extractNotebookUriFromIntent(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data ?: intent.clipData?.getItemAt(0)?.uri
            Intent.ACTION_SEND -> {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?: intent.clipData?.getItemAt(0)?.uri
                    ?: intent.data
            }
            else -> null
        }
    }
}

@Composable
fun LuminaNotebookApp(viewModel: NotebookViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val recentNotebooks by viewModel.recentNotebooks.collectAsStateWithLifecycle()
    val totalRecentCount by viewModel.totalRecentCount.collectAsStateWithLifecycle()
    val homeSearchQuery by viewModel.homeSearchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val activeNotebook by viewModel.activeNotebook.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isLoadingFile by viewModel.isLoadingFile.collectAsStateWithLifecycle()
    val fileError by viewModel.fileError.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.openExternalFileUri(uri)
        }
    }

    val saveAsDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-ipynb+json")
    ) { uri ->
        if (uri != null) {
            viewModel.saveNotebookAsExternalUri(uri)
        }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    AnimatedLiquidBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val isOpeningEditor = targetState is ScreenDestination.Editor
                    val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(
                        durationMillis = 280,
                        easing = FastOutSlowInEasing
                    )
                    val fadeSpec = tween<Float>(
                        durationMillis = 240,
                        easing = FastOutSlowInEasing
                    )
                    if (isOpeningEditor) {
                        (slideInHorizontally(
                            animationSpec = slideSpec,
                            initialOffsetX = { fullWidth -> fullWidth / 5 }
                        ) + fadeIn(fadeSpec)) togetherWith
                            (slideOutHorizontally(
                                animationSpec = slideSpec,
                                targetOffsetX = { fullWidth -> -fullWidth / 7 }
                            ) + fadeOut(fadeSpec))
                    } else {
                        (slideInHorizontally(
                            animationSpec = slideSpec,
                            initialOffsetX = { fullWidth -> -fullWidth / 5 }
                        ) + fadeIn(fadeSpec)) togetherWith
                            (slideOutHorizontally(
                                animationSpec = slideSpec,
                                targetOffsetX = { fullWidth -> fullWidth / 5 }
                            ) + fadeOut(fadeSpec))
                    }
                },
                label = "screenMotionTransition"
            ) { destination ->
                when (destination) {
                    is ScreenDestination.Home -> {
                        HomeScreen(
                            recentNotebooks = recentNotebooks,
                            totalRecentCount = totalRecentCount,
                            searchQuery = homeSearchQuery,
                            selectedFilter = selectedFilter,
                            themeMode = themeMode,
                            isLoadingFile = isLoadingFile,
                            fileError = fileError,
                            onDismissFileError = viewModel::dismissFileError,
                            onThemeModeChange = viewModel::setThemeMode,
                            onSearchQueryChange = viewModel::setHomeSearchQuery,
                            onFilterChange = viewModel::setSelectedFilter,
                            onSelectFileClick = {
                                openDocumentLauncher.launch(
                                    arrayOf(
                                        "application/x-ipynb+json",
                                        "application/json",
                                        "application/octet-stream",
                                        "text/plain",
                                        "*/*"
                                    )
                                )
                            },
                            onCreateNewNotebook = viewModel::createNewNotebook,
                            onOpenRecentNotebook = viewModel::openRecentNotebook,
                            onTogglePinNotebook = viewModel::togglePinRecentNotebook,
                            onRemoveRecentNotebook = viewModel::removeRecentNotebook,
                            onClearAllRecentClick = viewModel::clearAllRecentNotebooks,
                            onRestoreSamplesClick = viewModel::restoreSampleNotebooks
                        )
                    }

                    is ScreenDestination.Editor -> {
                        val editorState = activeNotebook
                        if (editorState != null) {
                            NotebookEditorScreen(
                                state = editorState,
                                themeMode = themeMode,
                                fileError = fileError,
                                onDismissFileError = viewModel::dismissFileError,
                                onThemeModeChange = viewModel::setThemeMode,
                                onNavigateBack = viewModel::navigateHome,
                                onSaveClick = { viewModel.saveCurrentNotebook() },
                                onSaveAndExit = {
                                    viewModel.saveCurrentNotebook {
                                        viewModel.navigateHome()
                                    }
                                },
                                onSaveAsDeviceFileClick = { suggestedName ->
                                    saveAsDocumentLauncher.launch(suggestedName)
                                },
                                onSaveAsWorkspaceCopy = viewModel::saveNotebookAsCopy,
                                onRenameNotebook = viewModel::renameCurrentNotebook,
                                onOpenAnotherFileClick = {
                                    openDocumentLauncher.launch(
                                        arrayOf(
                                            "application/x-ipynb+json",
                                            "application/json",
                                            "application/octet-stream",
                                            "text/plain",
                                            "*/*"
                                        )
                                    )
                                },
                                onSetEditingCell = viewModel::setEditingCell,
                                onUpdateCellSource = viewModel::updateCellSource,
                                onChangeCellType = viewModel::changeCellType,
                                onAddCellAt = viewModel::addCellAt,
                                onDuplicateCell = viewModel::duplicateCell,
                                onMoveCell = viewModel::moveCell,
                                onDeleteCell = viewModel::deleteCell,
                                onToggleCellCollapse = viewModel::toggleCellCollapse,
                                onClearAllOutputs = viewModel::clearAllOutputs,
                                onToggleRawJsonMode = viewModel::toggleRawJsonMode,
                                onUpdateRawJsonDraft = viewModel::updateRawJsonDraft,
                                onToggleSearchBar = viewModel::toggleEditorSearchBar,
                                onSearchQueryChange = viewModel::setEditorSearchQuery
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 72.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    }
}
