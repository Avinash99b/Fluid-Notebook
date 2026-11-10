package com.example.model

import java.util.UUID

enum class CellType(val wireName: String, val displayLabel: String) {
    CODE("code", "Code"),
    MARKDOWN("markdown", "Markdown"),
    RAW("raw", "Raw");

    companion object {
        fun fromWireName(value: String?): CellType = when (value?.lowercase()?.trim()) {
            "markdown" -> MARKDOWN
            "raw" -> RAW
            else -> CODE
        }
    }
}

sealed class CellOutput {
    data class StreamOutput(
        val name: String = "stdout",
        val text: String
    ) : CellOutput()

    data class ExecuteResultOutput(
        val executionCount: Int? = null,
        val plainText: String? = null,
        val markdownText: String? = null,
        val htmlText: String? = null,
        val imagePngBase64: String? = null
    ) : CellOutput()

    data class DisplayDataOutput(
        val plainText: String? = null,
        val imagePngBase64: String? = null
    ) : CellOutput()

    data class ErrorOutput(
        val ename: String,
        val evalue: String,
        val traceback: List<String> = emptyList()
    ) : CellOutput()
}

data class NotebookCell(
    val id: String = UUID.randomUUID().toString().take(8),
    val cellType: CellType = CellType.CODE,
    val source: String = "",
    val executionCount: Int? = null,
    val outputs: List<CellOutput> = emptyList(),
    val isCollapsed: Boolean = false,
    val isOutputCollapsed: Boolean = false
)

data class NotebookMetadata(
    val kernelName: String = "python3",
    val kernelDisplayName: String = "Python 3 (ipykernel)",
    val languageName: String = "python",
    val languageVersion: String = "3.11.8"
)

data class JupyterNotebook(
    val nbformat: Int = 4,
    val nbformatMinor: Int = 5,
    val metadata: NotebookMetadata = NotebookMetadata(),
    val cells: List<NotebookCell> = emptyList()
) {
    val codeCellCount: Int
        get() = cells.count { it.cellType == CellType.CODE }

    val markdownCellCount: Int
        get() = cells.count { it.cellType == CellType.MARKDOWN }
}
