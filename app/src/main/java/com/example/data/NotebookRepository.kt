package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CellOutput
import com.example.model.CellType
import com.example.model.JupyterNotebook
import com.example.model.NotebookCell
import com.example.model.NotebookMetadata
import com.example.ui.theme.ThemeMode
import com.example.util.IpynbParser
import com.example.util.SampleNotebooks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "lumina_preferences")

class NotebookRepository(
    private val context: Context,
    private val dao: RecentNotebookDao
) {
    private val keyThemeMode = stringPreferencesKey("theme_mode")
    private val keyHasSeeded = booleanPreferencesKey("has_seeded_samples_v1")

    val recentNotebooksFlow: Flow<List<RecentNotebookEntity>> = dao.observeAllRecentNotebooks()

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[keyThemeMode]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[keyThemeMode] = mode.name
        }
    }

    suspend fun ensureInitialSamplesSeeded() = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val alreadySeeded = prefs[keyHasSeeded] ?: false
        if (!alreadySeeded) {
            if (dao.getCount() == 0) {
                dao.upsertAll(SampleNotebooks.buildDefaultSamples())
            }
            context.dataStore.edit { it[keyHasSeeded] = true }
        }
    }

    suspend fun restoreSampleNotebooks() = withContext(Dispatchers.IO) {
        dao.upsertAll(SampleNotebooks.buildDefaultSamples())
    }

    suspend fun getNotebookEntityById(id: String): RecentNotebookEntity? = withContext(Dispatchers.IO) {
        val existing = dao.getNotebookById(id) ?: return@withContext null
        val updated = existing.copy(lastOpenedAt = System.currentTimeMillis())
        dao.upsertNotebook(updated)
        updated
    }

    suspend fun openExternalUri(uri: Uri): Result<RecentNotebookEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val scheme = uri.scheme?.lowercase()
            if (scheme == "content") {
                // Take persistable read/write permission when offered by the document provider
                val rwFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, rwFlags)
                }.onFailure {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }
            }

            val rawName = resolveDisplayName(uri) ?: "imported_notebook.ipynb"
            val normalizedName = ensureIpynbExtension(rawName)

            val rawJson = when (scheme) {
                "file" -> {
                    val filePath = uri.path
                        ?: throw IllegalArgumentException("Invalid file:// URI path.")
                    val file = File(filePath)
                    if (!file.exists() || !file.canRead()) {
                        throw IllegalStateException("Cannot read file '$rawName'. Permission denied or file does not exist.")
                    }
                    file.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
                else -> {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: throw IllegalStateException("Unable to open stream for '$rawName'. The file may have been moved or deleted.")
                }
            }

            val parsedNotebook = IpynbParser.parse(rawJson).getOrElse { parseErr ->
                throw IllegalArgumentException(
                    "Could not parse '$rawName' as a valid .ipynb notebook: ${parseErr.message}"
                )
            }

            val serialized = IpynbParser.serialize(parsedNotebook)
            val now = System.currentTimeMillis()
            val existing = dao.getNotebookByUri(uri.toString())
            val id = existing?.id ?: "ext_${UUID.randomUUID().toString().take(8)}"

            val entity = RecentNotebookEntity(
                id = id,
                displayName = normalizedName,
                sourceUri = uri.toString(),
                notebookJson = serialized,
                kernelName = parsedNotebook.metadata.kernelDisplayName,
                language = parsedNotebook.metadata.languageName.replaceFirstChar { it.uppercase() },
                cellCount = parsedNotebook.cells.size,
                codeCellCount = parsedNotebook.codeCellCount,
                markdownCellCount = parsedNotebook.markdownCellCount,
                fileSizeBytes = serialized.toByteArray().size.toLong(),
                lastOpenedAt = now,
                lastModifiedAt = now,
                isPinned = existing?.isPinned ?: false
            )
            dao.upsertNotebook(entity)
            entity
        }
    }

    suspend fun createNewNotebook(
        rawFileName: String,
        kernelDisplayName: String,
        language: String
    ): RecentNotebookEntity = withContext(Dispatchers.IO) {
        val displayName = ensureIpynbExtension(rawFileName.ifBlank { "untitled_notebook.ipynb" })
        val titleBase = displayName.removeSuffix(".ipynb").replace("_", " ").replace("-", " ")
            .replaceFirstChar { it.uppercase() }

        val notebook = JupyterNotebook(
            metadata = NotebookMetadata(
                kernelName = language.lowercase(),
                kernelDisplayName = kernelDisplayName,
                languageName = language.lowercase(),
                languageVersion = if (language.equals("Python", ignoreCase = true)) "3.11.8" else "1.0"
            ),
            cells = listOf(
                NotebookCell(
                    id = UUID.randomUUID().toString().take(8),
                    cellType = CellType.MARKDOWN,
                    source = "# $titleBase\nCreated in **Lumina Notebook**. Add or edit code and markdown cells below."
                ),
                NotebookCell(
                    id = UUID.randomUUID().toString().take(8),
                    cellType = CellType.CODE,
                    executionCount = 1,
                    source = "message = \"Welcome to $displayName\"\nprint(message)",
                    outputs = listOf(
                        CellOutput.StreamOutput(
                            name = "stdout",
                            text = "Welcome to $displayName\n"
                        )
                    )
                )
            )
        )

        val json = IpynbParser.serialize(notebook)
        val localFile = File(context.filesDir, displayName)
        runCatching { localFile.writeText(json) }

        val now = System.currentTimeMillis()
        val entity = RecentNotebookEntity(
            id = "local_${UUID.randomUUID().toString().take(8)}",
            displayName = displayName,
            sourceUri = "internal://files/${localFile.name}",
            notebookJson = json,
            kernelName = kernelDisplayName,
            language = language,
            cellCount = notebook.cells.size,
            codeCellCount = notebook.codeCellCount,
            markdownCellCount = notebook.markdownCellCount,
            fileSizeBytes = json.toByteArray().size.toLong(),
            lastOpenedAt = now,
            lastModifiedAt = now,
            isPinned = false
        )
        dao.upsertNotebook(entity)
        entity
    }

    suspend fun saveNotebook(
        entityId: String,
        displayName: String,
        sourceUri: String,
        notebook: JupyterNotebook
    ): Result<RecentNotebookEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val serialized = IpynbParser.serialize(notebook)
            val normalizedName = ensureIpynbExtension(displayName)

            // Write back to content://, file://, or internal app storage
            when {
                sourceUri.startsWith("content://") -> {
                    runCatching {
                        val uri = Uri.parse(sourceUri)
                        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                            out.write(serialized.toByteArray(Charsets.UTF_8))
                            out.flush()
                        }
                    }
                }
                sourceUri.startsWith("file://") -> {
                    runCatching {
                        val uri = Uri.parse(sourceUri)
                        uri.path?.let { File(it).writeText(serialized, Charsets.UTF_8) }
                    }
                }
                else -> {
                    val localFile = File(context.filesDir, normalizedName)
                    runCatching { localFile.writeText(serialized, Charsets.UTF_8) }
                }
            }

            val existing = dao.getNotebookById(entityId)
            val now = System.currentTimeMillis()
            val updated = RecentNotebookEntity(
                id = entityId,
                displayName = normalizedName,
                sourceUri = sourceUri,
                notebookJson = serialized,
                kernelName = notebook.metadata.kernelDisplayName,
                language = notebook.metadata.languageName.replaceFirstChar { it.uppercase() },
                cellCount = notebook.cells.size,
                codeCellCount = notebook.codeCellCount,
                markdownCellCount = notebook.markdownCellCount,
                fileSizeBytes = serialized.toByteArray().size.toLong(),
                lastOpenedAt = now,
                lastModifiedAt = now,
                isPinned = existing?.isPinned ?: false
            )
            dao.upsertNotebook(updated)
            updated
        }
    }

    suspend fun saveNotebookAsExternalUri(
        targetUri: Uri,
        fallbackName: String,
        notebook: JupyterNotebook
    ): Result<RecentNotebookEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val serialized = IpynbParser.serialize(notebook)
            context.contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
                out.write(serialized.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: throw IllegalStateException("Could not open destination file for writing.")

            val resolvedName = ensureIpynbExtension(resolveDisplayName(targetUri) ?: fallbackName)
            val now = System.currentTimeMillis()
            val existing = dao.getNotebookByUri(targetUri.toString())
            val id = existing?.id ?: "ext_${UUID.randomUUID().toString().take(8)}"

            val savedEntity = RecentNotebookEntity(
                id = id,
                displayName = resolvedName,
                sourceUri = targetUri.toString(),
                notebookJson = serialized,
                kernelName = notebook.metadata.kernelDisplayName,
                language = notebook.metadata.languageName.replaceFirstChar { it.uppercase() },
                cellCount = notebook.cells.size,
                codeCellCount = notebook.codeCellCount,
                markdownCellCount = notebook.markdownCellCount,
                fileSizeBytes = serialized.toByteArray().size.toLong(),
                lastOpenedAt = now,
                lastModifiedAt = now,
                isPinned = existing?.isPinned ?: false
            )
            dao.upsertNotebook(savedEntity)
            savedEntity
        }
    }

    suspend fun saveNotebookAsInternalCopy(
        newDisplayName: String,
        notebook: JupyterNotebook
    ): Result<RecentNotebookEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedName = ensureIpynbExtension(newDisplayName)
            val serialized = IpynbParser.serialize(notebook)
            val localFile = File(context.filesDir, normalizedName)
            runCatching { localFile.writeText(serialized) }

            val now = System.currentTimeMillis()
            val savedEntity = RecentNotebookEntity(
                id = "local_${UUID.randomUUID().toString().take(8)}",
                displayName = normalizedName,
                sourceUri = "internal://files/$normalizedName",
                notebookJson = serialized,
                kernelName = notebook.metadata.kernelDisplayName,
                language = notebook.metadata.languageName.replaceFirstChar { it.uppercase() },
                cellCount = notebook.cells.size,
                codeCellCount = notebook.codeCellCount,
                markdownCellCount = notebook.markdownCellCount,
                fileSizeBytes = serialized.toByteArray().size.toLong(),
                lastOpenedAt = now,
                lastModifiedAt = now,
                isPinned = false
            )
            dao.upsertNotebook(savedEntity)
            savedEntity
        }
    }

    suspend fun renameNotebook(
        entityId: String,
        newRawName: String
    ): Result<RecentNotebookEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = dao.getNotebookById(entityId)
                ?: throw IllegalArgumentException("Notebook not found.")
            val normalizedName = ensureIpynbExtension(newRawName)
            var updatedUri = existing.sourceUri

            if (existing.sourceUri.startsWith("content://")) {
                runCatching {
                    val uri = Uri.parse(existing.sourceUri)
                    val renamedUri = DocumentsContract.renameDocument(
                        context.contentResolver,
                        uri,
                        normalizedName
                    )
                    if (renamedUri != null) {
                        updatedUri = renamedUri.toString()
                    }
                }
            } else {
                val oldFile = File(context.filesDir, existing.displayName)
                val newFile = File(context.filesDir, normalizedName)
                if (oldFile.exists()) {
                    runCatching { oldFile.renameTo(newFile) }
                }
                updatedUri = "internal://files/$normalizedName"
            }

            val now = System.currentTimeMillis()
            val updated = existing.copy(
                displayName = normalizedName,
                sourceUri = updatedUri,
                lastModifiedAt = now,
                lastOpenedAt = now
            )
            dao.upsertNotebook(updated)
            updated
        }
    }

    suspend fun togglePin(id: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        dao.setPinned(id, isPinned)
    }

    suspend fun removeRecentById(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAllRecentNotebooks() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    private fun resolveDisplayName(uri: Uri): String? {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            val name = cursor.getString(idx)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    fun ensureIpynbExtension(rawName: String): String {
        val cleaned = rawName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
        if (cleaned.isEmpty()) return "untitled.ipynb"
        return if (cleaned.endsWith(".ipynb", ignoreCase = true)) {
            cleaned.substring(0, cleaned.length - 6) + ".ipynb"
        } else {
            "$cleaned.ipynb"
        }
    }
}
