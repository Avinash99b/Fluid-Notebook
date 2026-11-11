package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.NotebookRepository
import com.example.data.RecentNotebookEntity
import com.example.model.CellType
import com.example.model.JupyterNotebook
import com.example.model.NotebookCell
import com.example.ui.theme.ThemeMode
import com.example.util.IpynbParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class ScreenDestination {
    data object Home : ScreenDestination()
    data class Editor(val notebookId: String) : ScreenDestination()
}

enum class RecentFilter(val label: String) {
    ALL("All Files"),
    PINNED("Pinned"),
    PYTHON("Python"),
    EXTERNAL("Device URI")
}

data class ActiveNotebookState(
    val entityId: String = "",
    val displayName: String = "untitled.ipynb",
    val sourceUri: String = "",
    val notebook: JupyterNotebook = JupyterNotebook(),
    val activeEditingCellId: String? = null,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val isRawJsonMode: Boolean = false,
    val rawJsonDraft: String = "",
    val rawJsonError: String? = null,
    val searchQuery: String = "",
    val isSearchBarVisible: Boolean = false,
    val lastSavedTimestamp: Long? = null
)

class NotebookViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = NotebookRepository(application, database.recentNotebookDao())

    val themeMode: StateFlow<ThemeMode> = repository.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    private val _homeSearchQuery = MutableStateFlow("")
    val homeSearchQuery: StateFlow<String> = _homeSearchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(RecentFilter.ALL)
    val selectedFilter: StateFlow<RecentFilter> = _selectedFilter.asStateFlow()

    val recentNotebooks: StateFlow<List<RecentNotebookEntity>> = combine(
        repository.recentNotebooksFlow,
        _homeSearchQuery,
        _selectedFilter
    ) { allItems, query, filter ->
        allItems.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.displayName.contains(query, ignoreCase = true) ||
                item.kernelName.contains(query, ignoreCase = true) ||
                item.language.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                RecentFilter.ALL -> true
                RecentFilter.PINNED -> item.isPinned
                RecentFilter.PYTHON -> item.language.equals("Python", ignoreCase = true)
                RecentFilter.EXTERNAL -> item.sourceUri.startsWith("content://") || item.sourceUri.startsWith("file://")
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalRecentCount: StateFlow<Int> = repository.recentNotebooksFlow
        .combine(_homeSearchQuery) { list, _ -> list.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Home)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _activeNotebook = MutableStateFlow<ActiveNotebookState?>(null)
    val activeNotebook: StateFlow<ActiveNotebookState?> = _activeNotebook.asStateFlow()

    private val _isLoadingFile = MutableStateFlow(false)
    val isLoadingFile: StateFlow<Boolean> = _isLoadingFile.asStateFlow()

    private val _fileError = MutableStateFlow<String?>(null)
    val fileError: StateFlow<String?> = _fileError.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialSamplesSeeded()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setHomeSearchQuery(query: String) {
        _homeSearchQuery.value = query
    }

    fun setSelectedFilter(filter: RecentFilter) {
        _selectedFilter.value = filter
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun dismissFileError() {
        _fileError.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun openRecentNotebook(entityId: String) {
        if (_isLoadingFile.value) return
        viewModelScope.launch {
            _isLoadingFile.value = true
            _fileError.value = null
            val entity = repository.getNotebookEntityById(entityId)
            if (entity == null) {
                _isLoadingFile.value = false
                _fileError.value = "Notebook entry could not be found in recent history."
                return@launch
            }
            val parsedResult = withContext(Dispatchers.Default) {
                IpynbParser.parse(entity.notebookJson)
            }
            parsedResult.fold(
                onSuccess = { parsed ->
                    _activeNotebook.value = ActiveNotebookState(
                        entityId = entity.id,
                        displayName = entity.displayName,
                        sourceUri = entity.sourceUri,
                        notebook = parsed,
                        activeEditingCellId = null,
                        isDirty = false,
                        isSaving = false,
                        isRawJsonMode = false,
                        rawJsonDraft = entity.notebookJson,
                        lastSavedTimestamp = entity.lastModifiedAt
                    )
                    _isLoadingFile.value = false
                    _currentScreen.value = ScreenDestination.Editor(entity.id)
                },
                onFailure = { err ->
                    _isLoadingFile.value = false
                    _fileError.value = "Failed to parse notebook JSON: ${err.message}"
                }
            )
        }
    }

    fun openExternalFileUri(uri: Uri) {
        if (_isLoadingFile.value) return
        viewModelScope.launch {
            _isLoadingFile.value = true
            _fileError.value = null
            repository.openExternalUri(uri)
                .onSuccess { entity ->
                    val parsed = withContext(Dispatchers.Default) {
                        IpynbParser.parse(entity.notebookJson).getOrDefault(JupyterNotebook())
                    }
                    _activeNotebook.value = ActiveNotebookState(
                        entityId = entity.id,
                        displayName = entity.displayName,
                        sourceUri = entity.sourceUri,
                        notebook = parsed,
                        isDirty = false,
                        isSaving = false,
                        rawJsonDraft = entity.notebookJson,
                        lastSavedTimestamp = entity.lastModifiedAt
                    )
                    _isLoadingFile.value = false
                    _currentScreen.value = ScreenDestination.Editor(entity.id)
                    _snackbarMessage.value = "Opened ${entity.displayName}"
                }
                .onFailure { err ->
                    _isLoadingFile.value = false
                    val friendlyError = err.message ?: "The selected file could not be read or is not a valid .ipynb notebook."
                    _fileError.value = friendlyError
                    _snackbarMessage.value = "Unable to open file"
                }
        }
    }

    fun createNewNotebook(fileName: String, kernelDisplayName: String, language: String) {
        if (_isLoadingFile.value) return
        viewModelScope.launch {
            _isLoadingFile.value = true
            _fileError.value = null
            val entity = repository.createNewNotebook(fileName, kernelDisplayName, language)
            val parsed = withContext(Dispatchers.Default) {
                IpynbParser.parse(entity.notebookJson).getOrDefault(JupyterNotebook())
            }
            _activeNotebook.value = ActiveNotebookState(
                entityId = entity.id,
                displayName = entity.displayName,
                sourceUri = entity.sourceUri,
                notebook = parsed,
                activeEditingCellId = parsed.cells.lastOrNull()?.id,
                isDirty = false,
                isSaving = false,
                rawJsonDraft = entity.notebookJson,
                lastSavedTimestamp = entity.lastModifiedAt
            )
            _isLoadingFile.value = false
            _currentScreen.value = ScreenDestination.Editor(entity.id)
            _snackbarMessage.value = "Created ${entity.displayName}"
        }
    }

    fun restoreSampleNotebooks() {
        viewModelScope.launch {
            repository.restoreSampleNotebooks()
            _snackbarMessage.value = "Sample .ipynb notebooks restored"
        }
    }

    fun clearAllRecentNotebooks() {
        viewModelScope.launch {
            repository.clearAllRecentNotebooks()
            _snackbarMessage.value = "Cleared all recently opened files"
        }
    }

    fun removeRecentNotebook(id: String) {
        viewModelScope.launch {
            repository.removeRecentById(id)
            _snackbarMessage.value = "Removed from recently opened"
        }
    }

    fun togglePinRecentNotebook(id: String, currentPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePin(id, !currentPinned)
        }
    }

    fun navigateHome() {
        _currentScreen.value = ScreenDestination.Home
        _activeNotebook.value = null
    }

    fun setEditingCell(cellId: String?) {
        _activeNotebook.update { state ->
            state?.copy(activeEditingCellId = cellId)
        }
    }

    /**
     * Updates cell source in-memory without running full JSON serialization on the main thread
     * on every keystroke, eliminating layout/recomposition jank during typing.
     */
    fun updateCellSource(cellId: String, newSource: String) {
        _activeNotebook.update { state ->
            if (state == null) return@update null
            val updatedCells = state.notebook.cells.map { cell ->
                if (cell.id == cellId) cell.copy(source = newSource) else cell
            }
            state.copy(
                notebook = state.notebook.copy(cells = updatedCells),
                isDirty = true
            )
        }
    }

    fun changeCellType(cellId: String, newType: CellType) {
        _activeNotebook.update { state ->
            if (state == null) return@update null
            val updatedCells = state.notebook.cells.map { cell ->
                if (cell.id == cellId) {
                    cell.copy(
                        cellType = newType,
                        executionCount = if (newType == CellType.CODE) (cell.executionCount ?: 1) else null
                    )
                } else cell
            }
            state.copy(
                notebook = state.notebook.copy(cells = updatedCells),
                isDirty = true
            )
        }
    }

    fun addCellAt(index: Int, cellType: CellType) {
        _activeNotebook.update { state ->
            if (state == null) return@update null
            val newId = UUID.randomUUID().toString().take(8)
            val newCell = NotebookCell(
                id = newId,
                cellType = cellType,
                source = if (cellType == CellType.MARKDOWN) "## New Section\nWrite your markdown notes here." else "# Write Python code here\n",
                executionCount = null
            )
            val mutable = state.notebook.cells.toMutableList()
            val targetIdx = index.coerceIn(0, mutable.size)
            mutable.add(targetIdx, newCell)
            state.copy(
                notebook = state.notebook.copy(cells = mutable),
                activeEditingCellId = newId,
                isDirty = true
            )
        }
    }

    fun duplicateCell(cellId: String) {
        _activeNotebook.update { state ->
            if (state == null) return@update null
            val idx = state.notebook.cells.indexOfFirst { it.id == cellId }
            if (idx < 0) return@update state
            val original = state.notebook.cells[idx]
            val copyId = UUID.randomUUID().toString().take(8)
            val copyCell = original.copy(id = copyId)
            val mutable = state.notebook.cells.toMutableList()
            mutable.add(idx + 1, copyCell)
            state.copy(
                notebook = state.notebook.copy(cells = mutable),
                activeEditingCellId = copyId,
                isDirty = true
            )
        }
    }

    fun moveCell(cellId: String, delta: Int) {
        _activeNotebook.update { state ->
            if (state == null) return@update null
            val currentIdx = state.notebook.cells.indexOfFirst { it.id == cellId }
            if (currentIdx < 0) return@update state
            val targetIdx = (currentIdx + delta).coerceIn(0, state.notebook.cells.lastIndex)
            if (currentIdx == targetIdx) return@update state
            val mutable = state.notebook.cells.toMutableList()
            val item = mutable.removeAt(currentIdx)
            mutable.add(targetIdx, item)
            state.copy(
                notebook = state.notebook.copy(cells = mutable),
                isDirty = true
            )
        }
    }

    fun deleteCell(cellId: String) {
        _activeNotebook.update { state ->
            if (state == null) return@update null
            if (state.notebook.cells.size <= 1) {
                _snackbarMessage.value = "A notebook must contain at least one cell."
                return@update state
            }
            val updatedCells = state.notebook.cells.filterNot { it.id == cellId }
            state.copy(
                notebook = state.notebook.copy(cells = updatedCells),
                activeEditingCellId = if (state.activeEditingCellId == cellId) null else state.activeEditingCellId,
                isDirty = true
            )
        }
    }

    fun toggleCellCollapse(cellId: String) {
        _activeNotebook.update { state ->
            if (state == null) return@update null
            val updatedCells = state.notebook.cells.map { cell ->
                if (cell.id == cellId) cell.copy(isCollapsed = !cell.isCollapsed) else cell
            }
            state.copy(notebook = state.notebook.copy(cells = updatedCells))
        }
    }

    fun clearAllOutputs() {
        _activeNotebook.update { state ->
            if (state == null) return@update null
            val updatedCells = state.notebook.cells.map { cell ->
                cell.copy(outputs = emptyList(), executionCount = null)
            }
            state.copy(
                notebook = state.notebook.copy(cells = updatedCells),
                isDirty = true
            )
        }
        _snackbarMessage.value = "Cleared all cell outputs"
    }

    fun toggleRawJsonMode() {
        val current = _activeNotebook.value ?: return
        viewModelScope.launch {
            if (!current.isRawJsonMode) {
                val serialized = withContext(Dispatchers.Default) {
                    IpynbParser.serialize(current.notebook)
                }
                _activeNotebook.update { state ->
                    state?.copy(
                        isRawJsonMode = true,
                        rawJsonDraft = serialized,
                        rawJsonError = null
                    )
                }
            } else {
                val parsed = withContext(Dispatchers.Default) {
                    IpynbParser.parse(current.rawJsonDraft)
                }
                _activeNotebook.update { state ->
                    if (state == null) return@update null
                    parsed.fold(
                        onSuccess = { nb ->
                            state.copy(
                                notebook = nb,
                                isRawJsonMode = false,
                                rawJsonError = null
                            )
                        },
                        onFailure = { err ->
                            state.copy(rawJsonError = "Invalid JSON: ${err.message}")
                        }
                    )
                }
            }
        }
    }

    fun updateRawJsonDraft(newJson: String) {
        _activeNotebook.update { state ->
            state?.copy(rawJsonDraft = newJson, isDirty = true)
        }
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.Default) {
                IpynbParser.parse(newJson)
            }
            _activeNotebook.update { state ->
                if (state == null || state.rawJsonDraft != newJson) return@update state
                parsed.fold(
                    onSuccess = { nb ->
                        state.copy(
                            notebook = nb,
                            rawJsonError = null
                        )
                    },
                    onFailure = { err ->
                        state.copy(rawJsonError = err.message)
                    }
                )
            }
        }
    }

    fun toggleEditorSearchBar() {
        _activeNotebook.update { state ->
            if (state == null) return@update null
            val nextVisible = !state.isSearchBarVisible
            state.copy(
                isSearchBarVisible = nextVisible,
                searchQuery = if (!nextVisible) "" else state.searchQuery
            )
        }
    }

    fun setEditorSearchQuery(query: String) {
        _activeNotebook.update { state ->
            state?.copy(searchQuery = query)
        }
    }

    fun saveCurrentNotebook(onComplete: (() -> Unit)? = null) {
        val current = _activeNotebook.value ?: return
        if (current.isSaving) return
        viewModelScope.launch {
            _activeNotebook.update { it?.copy(isSaving = true) }
            repository.saveNotebook(
                entityId = current.entityId,
                displayName = current.displayName,
                sourceUri = current.sourceUri,
                notebook = current.notebook
            ).onSuccess { savedEntity ->
                _activeNotebook.update { state ->
                    state?.copy(
                        displayName = savedEntity.displayName,
                        isDirty = false,
                        isSaving = false,
                        lastSavedTimestamp = savedEntity.lastModifiedAt,
                        rawJsonDraft = savedEntity.notebookJson
                    )
                }
                _snackbarMessage.value = "Saved ${savedEntity.displayName}"
                onComplete?.invoke()
            }.onFailure { err ->
                _activeNotebook.update { it?.copy(isSaving = false) }
                _snackbarMessage.value = "Save failed: ${err.message}"
            }
        }
    }

    fun saveNotebookAsExternalUri(uri: Uri) {
        val current = _activeNotebook.value ?: return
        if (current.isSaving) return
        viewModelScope.launch {
            _activeNotebook.update { it?.copy(isSaving = true) }
            repository.saveNotebookAsExternalUri(
                targetUri = uri,
                fallbackName = current.displayName,
                notebook = current.notebook
            ).onSuccess { savedEntity ->
                _activeNotebook.value = current.copy(
                    entityId = savedEntity.id,
                    displayName = savedEntity.displayName,
                    sourceUri = savedEntity.sourceUri,
                    isDirty = false,
                    isSaving = false,
                    lastSavedTimestamp = savedEntity.lastModifiedAt,
                    rawJsonDraft = savedEntity.notebookJson
                )
                _snackbarMessage.value = "Saved As ${savedEntity.displayName}"
            }.onFailure { err ->
                _activeNotebook.update { it?.copy(isSaving = false) }
                _snackbarMessage.value = "Save As failed: ${err.message}"
            }
        }
    }

    fun saveNotebookAsCopy(newFileName: String) {
        val current = _activeNotebook.value ?: return
        if (current.isSaving) return
        viewModelScope.launch {
            _activeNotebook.update { it?.copy(isSaving = true) }
            repository.saveNotebookAsInternalCopy(
                newDisplayName = newFileName,
                notebook = current.notebook
            ).onSuccess { savedEntity ->
                _activeNotebook.value = current.copy(
                    entityId = savedEntity.id,
                    displayName = savedEntity.displayName,
                    sourceUri = savedEntity.sourceUri,
                    isDirty = false,
                    isSaving = false,
                    lastSavedTimestamp = savedEntity.lastModifiedAt,
                    rawJsonDraft = savedEntity.notebookJson
                )
                _snackbarMessage.value = "Saved copy as ${savedEntity.displayName}"
            }.onFailure { err ->
                _activeNotebook.update { it?.copy(isSaving = false) }
                _snackbarMessage.value = "Could not save copy: ${err.message}"
            }
        }
    }

    fun renameCurrentNotebook(newName: String) {
        val current = _activeNotebook.value ?: return
        if (current.isSaving) return
        viewModelScope.launch {
            _activeNotebook.update { it?.copy(isSaving = true) }
            repository.renameNotebook(current.entityId, newName)
                .onSuccess { renamedEntity ->
                    repository.saveNotebook(
                        entityId = renamedEntity.id,
                        displayName = renamedEntity.displayName,
                        sourceUri = renamedEntity.sourceUri,
                        notebook = current.notebook
                    )
                    _activeNotebook.update { state ->
                        state?.copy(
                            displayName = renamedEntity.displayName,
                            sourceUri = renamedEntity.sourceUri,
                            isSaving = false
                        )
                    }
                    _snackbarMessage.value = "Renamed to ${renamedEntity.displayName}"
                }
                .onFailure { err ->
                    _activeNotebook.update { it?.copy(isSaving = false) }
                    _snackbarMessage.value = "Rename failed: ${err.message}"
                }
        }
    }
}
