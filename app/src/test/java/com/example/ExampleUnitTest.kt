package com.example

import com.example.model.CellOutput
import com.example.model.CellType
import com.example.util.IpynbParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun evaluateCodeCell_andStripAnsi_worksCorrectly() {
        assertEquals(CellType.CODE, CellType.fromWireName("code"))
        assertEquals(CellType.MARKDOWN, CellType.fromWireName("markdown"))

        val cleaned = IpynbParser.stripAnsiCodes("\u001B[31mZeroDivisionError\u001B[0m")
        assertEquals("ZeroDivisionError", cleaned)

        val outputs = IpynbParser.evaluateCodeCellLocally(
            source = """
                x = 15
                y = 27
                print(f"Sum is {x}")
                x + y
            """.trimIndent(),
            nextExecutionCount = 4
        )
        assertEquals(2, outputs.size)
        assertTrue(outputs[0] is CellOutput.StreamOutput)
        val resultOutput = outputs[1] as CellOutput.ExecuteResultOutput
        assertEquals("42", resultOutput.plainText)
    }
}
