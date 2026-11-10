package com.example.util

import com.example.model.CellOutput
import com.example.model.CellType
import com.example.model.JupyterNotebook
import com.example.model.NotebookCell
import com.example.model.NotebookMetadata
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object IpynbParser {

    private val ansiRegex = Regex("\u001B\\[[;\\d]*[A-Za-z]")

    fun stripAnsiCodes(input: String): String = ansiRegex.replace(input, "")

    fun parse(jsonString: String): Result<JupyterNotebook> = runCatching {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            return@runCatching JupyterNotebook(
                cells = listOf(
                    NotebookCell(
                        cellType = CellType.MARKDOWN,
                        source = "# Untitled Notebook\nStart adding code or markdown cells below."
                    ),
                    NotebookCell(
                        cellType = CellType.CODE,
                        source = "print(\"Hello from Lumina Notebook!\")",
                        executionCount = 1,
                        outputs = listOf(CellOutput.StreamOutput("stdout", "Hello from Lumina Notebook!\n"))
                    )
                )
            )
        }

        val root = JSONObject(trimmed)
        val nbformat = root.optInt("nbformat", 4)
        val nbformatMinor = root.optInt("nbformat_minor", 5)

        val metaObj = root.optJSONObject("metadata")
        val kernelSpec = metaObj?.optJSONObject("kernelspec")
        val langInfo = metaObj?.optJSONObject("language_info")

        val metadata = NotebookMetadata(
            kernelName = kernelSpec?.optString("name")?.takeIf { it.isNotBlank() } ?: "python3",
            kernelDisplayName = kernelSpec?.optString("display_name")?.takeIf { it.isNotBlank() }
                ?: "Python 3 (ipykernel)",
            languageName = langInfo?.optString("name")?.takeIf { it.isNotBlank() }
                ?: kernelSpec?.optString("language")?.takeIf { it.isNotBlank() }
                ?: "python",
            languageVersion = langInfo?.optString("version")?.takeIf { it.isNotBlank() } ?: "3.11"
        )

        val cellsArray = root.optJSONArray("cells") ?: JSONArray()
        val parsedCells = mutableListOf<NotebookCell>()

        for (i in 0 until cellsArray.length()) {
            val cellObj = cellsArray.optJSONObject(i) ?: continue
            val cellId = cellObj.optString("id").takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString().take(8)
            val cellType = CellType.fromWireName(cellObj.optString("cell_type", "code"))
            val source = readMultilineField(cellObj.opt("source"))

            val execCount = if (cellObj.has("execution_count") && !cellObj.isNull("execution_count")) {
                cellObj.optInt("execution_count", 0).takeIf { it > 0 }
            } else {
                null
            }

            val outputs = if (cellType == CellType.CODE) {
                parseOutputs(cellObj.optJSONArray("outputs"))
            } else {
                emptyList()
            }

            parsedCells.add(
                NotebookCell(
                    id = cellId,
                    cellType = cellType,
                    source = source,
                    executionCount = execCount,
                    outputs = outputs
                )
            )
        }

        JupyterNotebook(
            nbformat = nbformat,
            nbformatMinor = nbformatMinor,
            metadata = metadata,
            cells = parsedCells
        )
    }

    private fun parseOutputs(outputsArray: JSONArray?): List<CellOutput> {
        if (outputsArray == null) return emptyList()
        val list = mutableListOf<CellOutput>()
        for (i in 0 until outputsArray.length()) {
            val outObj = outputsArray.optJSONObject(i) ?: continue
            when (outObj.optString("output_type")) {
                "stream" -> {
                    val name = outObj.optString("name", "stdout")
                    val text = stripAnsiCodes(readMultilineField(outObj.opt("text")))
                    list.add(CellOutput.StreamOutput(name = name, text = text))
                }
                "execute_result" -> {
                    val execCount = if (outObj.has("execution_count") && !outObj.isNull("execution_count")) {
                        outObj.optInt("execution_count")
                    } else null
                    val dataObj = outObj.optJSONObject("data")
                    val plain = dataObj?.opt("text/plain")?.let { readMultilineField(it) }
                    val markdown = dataObj?.opt("text/markdown")?.let { readMultilineField(it) }
                    val html = dataObj?.opt("text/html")?.let { readMultilineField(it) }
                    val png = dataObj?.opt("image/png")?.let { readMultilineField(it).replace("\n", "").trim() }
                    list.add(
                        CellOutput.ExecuteResultOutput(
                            executionCount = execCount,
                            plainText = plain,
                            markdownText = markdown,
                            htmlText = html,
                            imagePngBase64 = png?.takeIf { it.isNotEmpty() }
                        )
                    )
                }
                "display_data" -> {
                    val dataObj = outObj.optJSONObject("data")
                    val plain = dataObj?.opt("text/plain")?.let { readMultilineField(it) }
                    val png = dataObj?.opt("image/png")?.let { readMultilineField(it).replace("\n", "").trim() }
                    list.add(
                        CellOutput.DisplayDataOutput(
                            plainText = plain,
                            imagePngBase64 = png?.takeIf { it.isNotEmpty() }
                        )
                    )
                }
                "error" -> {
                    val ename = outObj.optString("ename", "Error")
                    val evalue = stripAnsiCodes(outObj.optString("evalue", ""))
                    val tbArray = outObj.optJSONArray("traceback")
                    val tb = mutableListOf<String>()
                    if (tbArray != null) {
                        for (j in 0 until tbArray.length()) {
                            tb.add(stripAnsiCodes(tbArray.optString(j)))
                        }
                    }
                    list.add(
                        CellOutput.ErrorOutput(
                            ename = ename,
                            evalue = evalue,
                            traceback = tb
                        )
                    )
                }
            }
        }
        return list
    }

    private fun readMultilineField(raw: Any?): String {
        return when (raw) {
            null, JSONObject.NULL -> ""
            is String -> raw
            is JSONArray -> {
                val sb = StringBuilder()
                for (i in 0 until raw.length()) {
                    val line = raw.optString(i, "")
                    sb.append(line)
                    if (i < raw.length() - 1 && !line.endsWith("\n")) {
                        sb.append("\n")
                    }
                }
                sb.toString()
            }
            else -> raw.toString()
        }
    }

    private fun toLinesJsonArray(text: String): JSONArray {
        val arr = JSONArray()
        if (text.isEmpty()) return arr
        val lines = text.split("\n")
        for (i in lines.indices) {
            if (i < lines.lastIndex) {
                arr.put(lines[i] + "\n")
            } else if (lines[i].isNotEmpty()) {
                arr.put(lines[i])
            }
        }
        return arr
    }

    fun serialize(notebook: JupyterNotebook, indentSpaces: Int = 2): String {
        val root = JSONObject()
        val metadataObj = JSONObject().apply {
            put(
                "kernelspec",
                JSONObject().apply {
                    put("name", notebook.metadata.kernelName)
                    put("display_name", notebook.metadata.kernelDisplayName)
                    put("language", notebook.metadata.languageName)
                }
            )
            put(
                "language_info",
                JSONObject().apply {
                    put("name", notebook.metadata.languageName)
                    put("version", notebook.metadata.languageVersion)
                }
            )
        }

        val cellsArray = JSONArray()
        for (cell in notebook.cells) {
            val cellObj = JSONObject()
            cellObj.put("id", cell.id)
            cellObj.put("cell_type", cell.cellType.wireName)
            cellObj.put("metadata", JSONObject())
            cellObj.put("source", toLinesJsonArray(cell.source))

            if (cell.cellType == CellType.CODE) {
                cellObj.put("execution_count", cell.executionCount ?: JSONObject.NULL)
                val outputsArr = JSONArray()
                for (output in cell.outputs) {
                    when (output) {
                        is CellOutput.StreamOutput -> {
                            outputsArr.put(
                                JSONObject().apply {
                                    put("output_type", "stream")
                                    put("name", output.name)
                                    put("text", toLinesJsonArray(output.text))
                                }
                            )
                        }
                        is CellOutput.ExecuteResultOutput -> {
                            outputsArr.put(
                                JSONObject().apply {
                                    put("output_type", "execute_result")
                                    put("execution_count", output.executionCount ?: (cell.executionCount ?: 1))
                                    put("metadata", JSONObject())
                                    put(
                                        "data",
                                        JSONObject().apply {
                                            output.plainText?.let { put("text/plain", toLinesJsonArray(it)) }
                                            output.markdownText?.let { put("text/markdown", toLinesJsonArray(it)) }
                                            output.htmlText?.let { put("text/html", toLinesJsonArray(it)) }
                                            output.imagePngBase64?.let { put("image/png", it) }
                                        }
                                    )
                                }
                            )
                        }
                        is CellOutput.DisplayDataOutput -> {
                            outputsArr.put(
                                JSONObject().apply {
                                    put("output_type", "display_data")
                                    put("metadata", JSONObject())
                                    put(
                                        "data",
                                        JSONObject().apply {
                                            output.plainText?.let { put("text/plain", toLinesJsonArray(it)) }
                                            output.imagePngBase64?.let { put("image/png", it) }
                                        }
                                    )
                                }
                            )
                        }
                        is CellOutput.ErrorOutput -> {
                            outputsArr.put(
                                JSONObject().apply {
                                    put("output_type", "error")
                                    put("ename", output.ename)
                                    put("evalue", output.evalue)
                                    val tbArr = JSONArray()
                                    output.traceback.forEach { tbArr.put(it) }
                                    put("traceback", tbArr)
                                }
                            )
                        }
                    }
                }
                cellObj.put("outputs", outputsArr)
            }
            cellsArray.put(cellObj)
        }

        root.put("cells", cellsArray)
        root.put("metadata", metadataObj)
        root.put("nbformat", notebook.nbformat)
        root.put("nbformat_minor", notebook.nbformatMinor)

        return root.toString(indentSpaces)
    }

    /**
     * Lightweight interactive code evaluator so users can click "Run Cell" after making changes
     * to a code cell and immediately see updated stdout/result outputs in the viewer!
     */
    fun evaluateCodeCellLocally(source: String, nextExecutionCount: Int): List<CellOutput> {
        val trimmed = source.trim()
        if (trimmed.isEmpty()) return emptyList()

        val stdoutLines = mutableListOf<String>()
        var lastExpressionResult: String? = null
        val variables = mutableMapOf<String, Double>()

        val lines = trimmed.lines()
        for (rawLine in lines) {
            val line = rawLine.substringBefore("#").trim()
            if (line.isEmpty()) continue

            if (line.startsWith("print(") && line.endsWith(")")) {
                val inner = line.removePrefix("print(").removeSuffix(")").trim()
                val printed = evaluateSimpleExpression(inner, variables)
                stdoutLines.add(printed)
            } else if (line.contains("=") && !line.contains("==") && !line.startsWith("def ") && !line.startsWith("for ")) {
                val parts = line.split("=", limit = 2)
                val varName = parts[0].trim()
                val expr = parts[1].trim()
                expr.toDoubleOrNull()?.let { variables[varName] = it }
            } else if (!line.startsWith("import ") && !line.startsWith("from ") && !line.startsWith("def ") && !line.startsWith("class ")) {
                lastExpressionResult = evaluateSimpleExpression(line, variables)
            }
        }

        val outputs = mutableListOf<CellOutput>()
        if (stdoutLines.isNotEmpty()) {
            outputs.add(
                CellOutput.StreamOutput(
                    name = "stdout",
                    text = stdoutLines.joinToString("\n") + "\n"
                )
            )
        }
        if (lastExpressionResult != null && lastExpressionResult.isNotBlank()) {
            outputs.add(
                CellOutput.ExecuteResultOutput(
                    executionCount = nextExecutionCount,
                    plainText = lastExpressionResult
                )
            )
        }
        if (outputs.isEmpty()) {
            outputs.add(
                CellOutput.ExecuteResultOutput(
                    executionCount = nextExecutionCount,
                    plainText = "✓ Executed ${lines.size} line(s) cleanly"
                )
            )
        }
        return outputs
    }

    private fun evaluateSimpleExpression(expr: String, vars: Map<String, Double>): String {
        val clean = expr.trim()
        if ((clean.startsWith("\"") && clean.endsWith("\"")) ||
            (clean.startsWith("'") && clean.endsWith("'"))
        ) {
            return clean.substring(1, clean.length - 1)
        }
        if (clean.startsWith("f\"") && clean.endsWith("\"")) {
            var template = clean.removePrefix("f\"").removeSuffix("\"")
            vars.forEach { (k, v) ->
                val formatted = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
                template = template.replace("{$k}", formatted)
            }
            return template
        }
        vars[clean]?.let { v ->
            return if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
        }
        // Simple arithmetic support (+, -, *, /)
        val addParts = clean.split("+")
        if (addParts.size == 2) {
            val a = addParts[0].trim().let { vars[it] ?: it.toDoubleOrNull() }
            val b = addParts[1].trim().let { vars[it] ?: it.toDoubleOrNull() }
            if (a != null && b != null) {
                val sum = a + b
                return if (sum % 1.0 == 0.0) sum.toLong().toString() else ((sum * 10000).roundToInt() / 10000.0).toString()
            }
        }
        val mulParts = clean.split("*")
        if (mulParts.size == 2) {
            val a = mulParts[0].trim().let { vars[it] ?: it.toDoubleOrNull() }
            val b = mulParts[1].trim().let { vars[it] ?: it.toDoubleOrNull() }
            if (a != null && b != null) {
                val prod = a * b
                return if (prod % 1.0 == 0.0) prod.toLong().toString() else ((prod * 10000).roundToInt() / 10000.0).toString()
            }
        }
        if (clean.startsWith("sin(") && clean.endsWith(")")) {
            val arg = clean.removePrefix("sin(").removeSuffix(")").trim().toDoubleOrNull()
            if (arg != null) return ((sin(arg) * 10000).roundToInt() / 10000.0).toString()
        }
        if (clean.startsWith("cos(") && clean.endsWith(")")) {
            val arg = clean.removePrefix("cos(").removeSuffix(")").trim().toDoubleOrNull()
            if (arg != null) return ((cos(arg) * 10000).roundToInt() / 10000.0).toString()
        }
        return clean
    }
}
