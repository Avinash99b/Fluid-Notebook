package com.example

import com.example.model.CellType
import com.example.util.IpynbParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun cellTypeAndStripAnsi_worksCorrectly() {
        assertEquals(CellType.CODE, CellType.fromWireName("code"))
        assertEquals(CellType.MARKDOWN, CellType.fromWireName("markdown"))
        assertEquals(CellType.RAW, CellType.fromWireName("raw"))

        val cleaned = IpynbParser.stripAnsiCodes("\u001B[31mZeroDivisionError\u001B[0m")
        assertEquals("ZeroDivisionError", cleaned)
    }
}
