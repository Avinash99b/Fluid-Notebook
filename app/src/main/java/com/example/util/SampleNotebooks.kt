package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.Base64
import com.example.data.RecentNotebookEntity
import com.example.model.CellOutput
import com.example.model.CellType
import com.example.model.JupyterNotebook
import com.example.model.NotebookCell
import com.example.model.NotebookMetadata
import java.io.ByteArrayOutputStream
import kotlin.math.cos
import kotlin.math.sin

object SampleNotebooks {

    /**
     * Generates a real, crisp PNG chart encoded as Base64 for our sample `.ipynb` `image/png` cell output
     * so users immediately see rich inline plot rendering inside the notebook viewer.
     */
    private fun generateSampleWavePlotBase64(): String = runCatching {
        val width = 640
        val height = 280
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Deep Midnight Plot Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(0xFF0B1325.toInt(), 0xFF131F3A.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 24f, 24f, bgPaint)

        // Subtle Grid Lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x2894A3B8
            strokeWidth = 1.5f
        }
        for (y in 40..240 step 50) {
            canvas.drawLine(36f, y.toFloat(), (width - 36).toFloat(), y.toFloat(), gridPaint)
        }
        for (x in 60..580 step 80) {
            canvas.drawLine(x.toFloat(), 28f, x.toFloat(), (height - 28).toFloat(), gridPaint)
        }

        // Fill under primary curve
        val fillPath = Path()
        val linePath = Path()
        val secondaryPath = Path()

        val startX = 36f
        val endX = width - 36f
        val centerY = height / 2f

        fillPath.moveTo(startX, height - 28f)
        val steps = 120
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = startX + t * (endX - startX)
            val y1 = centerY - (sin(t * Math.PI * 3.2) * 76f * (1f - 0.15f * t)).toFloat()
            val y2 = centerY - (cos(t * Math.PI * 2.5) * 52f).toFloat()

            if (i == 0) {
                linePath.moveTo(x, y1)
                secondaryPath.moveTo(x, y2)
            } else {
                linePath.lineTo(x, y1)
                secondaryPath.lineTo(x, y2)
            }
            fillPath.lineTo(x, y1)
        }
        fillPath.lineTo(endX, height - 28f)
        fillPath.close()

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, 40f, 0f, height.toFloat(),
                intArrayOf(0x6638BDF8, 0x0538BDF8),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(fillPath, fillPaint)

        // Secondary indigo curve
        val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = 0xFF818CF8.toInt()
        }
        canvas.drawPath(secondaryPath, secondaryPaint)

        // Primary cyan curve
        val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = 0xFF38BDF8.toInt()
        }
        canvas.drawPath(linePath, primaryPaint)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }.getOrDefault("")

    fun buildDefaultSamples(): List<RecentNotebookEntity> {
        val now = System.currentTimeMillis()
        val wavePng = generateSampleWavePlotBase64()

        val transformerNotebook = JupyterNotebook(
            metadata = NotebookMetadata(
                kernelName = "python3",
                kernelDisplayName = "Python 3.11 (PyTorch 2.4 CUDA)",
                languageName = "python",
                languageVersion = "3.11.8"
            ),
            cells = listOf(
                NotebookCell(
                    id = "cell_01",
                    cellType = CellType.MARKDOWN,
                    source = """
                        # Spectral Attention & Cosine Warmup Analysis
                        Welcome to **Lumina Notebook**. You can inspect, edit, reorder, run, and save any `.ipynb` cell with full **Liquid Glass** clarity.
                        
                        - **Tap any cell** to inspect or edit its source code or Markdown.
                        - **Use the top bar** to **Save**, **Save As…**, **Rename**, or toggle **Raw JSON**.
                        - **Switch themes** anytime between **Light**, **Dark**, and **Auto**.
                    """.trimIndent()
                ),
                NotebookCell(
                    id = "cell_02",
                    cellType = CellType.CODE,
                    executionCount = 1,
                    source = """
                        import math
                        import torch
                        import numpy as np

                        # Hyperparameters for Multi-Head Spectral Attention
                        d_model = 512
                        num_heads = 8
                        head_dim = d_model / num_heads
                        warmup_steps = 4000
                        print(f"Initialized Transformer: d_model={d_model}, head_dim={head_dim}")
                    """.trimIndent(),
                    outputs = listOf(
                        CellOutput.StreamOutput(
                            name = "stdout",
                            text = "Initialized Transformer: d_model=512, head_dim=64\n"
                        )
                    )
                ),
                NotebookCell(
                    id = "cell_03",
                    cellType = CellType.MARKDOWN,
                    source = """
                        ## 1. Damped Harmonic Cosine Schedule
                        Below we compute the spectral attentuation curve and render the inline `image/png` figure output directly from the `.ipynb` payload:
                    """.trimIndent()
                ),
                NotebookCell(
                    id = "cell_04",
                    cellType = CellType.CODE,
                    executionCount = 2,
                    source = """
                        # Plot harmonic cosine learning rate vs spectral decay
                        epochs = 120
                        peak_lr = 0.0003
                        final_loss = 0.0418
                        print(f"Validation perplexity converged to {final_loss} after {epochs} epochs")
                    """.trimIndent(),
                    outputs = buildList {
                        add(
                            CellOutput.StreamOutput(
                                name = "stdout",
                                text = "Validation perplexity converged to 0.0418 after 120 epochs\n"
                            )
                        )
                        if (wavePng.isNotEmpty()) {
                            add(
                                CellOutput.DisplayDataOutput(
                                    plainText = "<Figure size 640x280 with 1 Axes>",
                                    imagePngBase64 = wavePng
                                )
                            )
                        }
                    }
                ),
                NotebookCell(
                    id = "cell_05",
                    cellType = CellType.CODE,
                    executionCount = 3,
                    source = """
                        # Benchmark Throughput Summary
                        tokens_per_sec = 48200
                        gpu_count = 4
                        total_throughput = tokens_per_sec * gpu_count
                        total_throughput
                    """.trimIndent(),
                    outputs = listOf(
                        CellOutput.ExecuteResultOutput(
                            executionCount = 3,
                            plainText = "192800"
                        )
                    )
                )
            )
        )

        val dataScienceNotebook = JupyterNotebook(
            metadata = NotebookMetadata(
                kernelName = "python3",
                kernelDisplayName = "Python 3 (Pandas & DuckDB)",
                languageName = "python",
                languageVersion = "3.11.8"
            ),
            cells = listOf(
                NotebookCell(
                    id = "ds_01",
                    cellType = CellType.MARKDOWN,
                    source = """
                        # Global Renewable Grid Telemetry (2026 Cohort)
                        Exploratory time-series aggregation of solar, wind, and hydro storage capacity across regional microgrids.
                        
                        > **Tip:** Try editing the numbers in the code cell below and tapping the **Run Cell** button to recompute the output live!
                    """.trimIndent()
                ),
                NotebookCell(
                    id = "ds_02",
                    cellType = CellType.CODE,
                    executionCount = 1,
                    source = """
                        solar_gw = 412.5
                        wind_gw = 289.5
                        combined_gw = solar_gw + wind_gw
                        print(f"Combined Clean Capacity: {combined_gw} GW")
                        combined_gw
                    """.trimIndent(),
                    outputs = listOf(
                        CellOutput.StreamOutput(
                            name = "stdout",
                            text = "Combined Clean Capacity: 702.0 GW\n"
                        ),
                        CellOutput.ExecuteResultOutput(
                            executionCount = 1,
                            plainText = "702.0"
                        )
                    )
                ),
                NotebookCell(
                    id = "ds_03",
                    cellType = CellType.CODE,
                    executionCount = 2,
                    source = """
                        # Check anomaly threshold diagnostics
                        grid_frequency_hz = 50.02
                        print("Grid synchronization nominal: 99.98% uptime")
                    """.trimIndent(),
                    outputs = listOf(
                        CellOutput.StreamOutput(
                            name = "stdout",
                            text = "Grid synchronization nominal: 99.98% uptime\n"
                        )
                    )
                )
            )
        )

        val quantumSimNotebook = JupyterNotebook(
            metadata = NotebookMetadata(
                kernelName = "julia-1.10",
                kernelDisplayName = "Julia 1.10 (Qiskit / Yao)",
                languageName = "julia",
                languageVersion = "1.10.2"
            ),
            cells = listOf(
                NotebookCell(
                    id = "q_01",
                    cellType = CellType.MARKDOWN,
                    source = """
                        # Variational Quantum Eigensolver (VQE) Ansatz
                        Ground-state energy estimation for H₂ bond lengths using hardware-efficient parameterized Ry-Rz circuits.
                    """.trimIndent()
                ),
                NotebookCell(
                    id = "q_02",
                    cellType = CellType.CODE,
                    executionCount = 1,
                    source = """
                        bond_distance_angstrom = 0.7414
                        hartree_energy = -1.1372
                        print(f"Equilibrium bond distance: {bond_distance_angstrom} A")
                        hartree_energy
                    """.trimIndent(),
                    outputs = listOf(
                        CellOutput.StreamOutput(
                            name = "stdout",
                            text = "Equilibrium bond distance: 0.7414 A\n"
                        ),
                        CellOutput.ExecuteResultOutput(
                            executionCount = 1,
                            plainText = "-1.1372"
                        )
                    )
                )
            )
        )

        val json1 = IpynbParser.serialize(transformerNotebook)
        val json2 = IpynbParser.serialize(dataScienceNotebook)
        val json3 = IpynbParser.serialize(quantumSimNotebook)

        return listOf(
            RecentNotebookEntity(
                id = "sample_spectral_attention",
                displayName = "spectral_attention_warmup.ipynb",
                sourceUri = "internal://samples/spectral_attention_warmup.ipynb",
                notebookJson = json1,
                kernelName = transformerNotebook.metadata.kernelDisplayName,
                language = "Python",
                cellCount = transformerNotebook.cells.size,
                codeCellCount = transformerNotebook.codeCellCount,
                markdownCellCount = transformerNotebook.markdownCellCount,
                fileSizeBytes = json1.toByteArray().size.toLong(),
                lastOpenedAt = now - 120_000L,
                lastModifiedAt = now - 120_000L,
                isPinned = true
            ),
            RecentNotebookEntity(
                id = "sample_renewable_telemetry",
                displayName = "renewable_grid_telemetry_2026.ipynb",
                sourceUri = "internal://samples/renewable_grid_telemetry_2026.ipynb",
                notebookJson = json2,
                kernelName = dataScienceNotebook.metadata.kernelDisplayName,
                language = "Python",
                cellCount = dataScienceNotebook.cells.size,
                codeCellCount = dataScienceNotebook.codeCellCount,
                markdownCellCount = dataScienceNotebook.markdownCellCount,
                fileSizeBytes = json2.toByteArray().size.toLong(),
                lastOpenedAt = now - 3_600_000L,
                lastModifiedAt = now - 3_600_000L,
                isPinned = false
            ),
            RecentNotebookEntity(
                id = "sample_quantum_vqe",
                displayName = "quantum_vqe_h2_ansatz.ipynb",
                sourceUri = "internal://samples/quantum_vqe_h2_ansatz.ipynb",
                notebookJson = json3,
                kernelName = quantumSimNotebook.metadata.kernelDisplayName,
                language = "Julia",
                cellCount = quantumSimNotebook.cells.size,
                codeCellCount = quantumSimNotebook.codeCellCount,
                markdownCellCount = quantumSimNotebook.markdownCellCount,
                fileSizeBytes = json3.toByteArray().size.toLong(),
                lastOpenedAt = now - 86_400_000L,
                lastModifiedAt = now - 86_400_000L,
                isPinned = false
            )
        )
    }
}
