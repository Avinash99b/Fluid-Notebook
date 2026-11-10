package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.LiquidGlassColors

object SyntaxHighlighter {

    private val pythonKeywords = setOf(
        "def", "class", "import", "from", "as", "return", "yield", "if", "elif", "else",
        "for", "while", "in", "not", "and", "or", "is", "None", "True", "False",
        "try", "except", "finally", "raise", "with", "async", "await", "lambda", "pass",
        "break", "continue", "global", "nonlocal", "assert", "del"
    )

    private val builtinFunctions = setOf(
        "print", "len", "range", "enumerate", "zip", "map", "filter", "sorted", "sum",
        "min", "max", "abs", "round", "int", "float", "str", "list", "dict", "set",
        "tuple", "type", "isinstance", "open", "super", "np", "pd", "plt", "torch", "nn"
    )

    private val tokenRegex = Regex(
        """(#.*$)|('''[\s\S]*?'''|\"\"\"[\s\S]*?\"\"\"|'(?:\\.|[^'\\])*'|"(?:\\.|[^"\\])*")|(@[A-Za-z_][A-Za-z0-9_.]*)|(\b\d+(?:\.\d+)?(?:e[+-]?\d+)?\b)|(\b[A-Za-z_][A-Za-z0-9_]*\b)""",
        setOf(RegexOption.MULTILINE)
    )

    fun highlightPython(
        code: String,
        glassColors: LiquidGlassColors,
        baseColor: Color,
        searchQuery: String = ""
    ): AnnotatedString = buildAnnotatedString {
        append(code)
        addStyle(SpanStyle(color = baseColor), 0, code.length)

        tokenRegex.findAll(code).forEach { match ->
            val range = match.range
            val start = range.first
            val end = range.last + 1

            when {
                match.groups[1] != null -> {
                    // Comment
                    addStyle(
                        SpanStyle(
                            color = glassColors.syntaxComment,
                            fontStyle = FontStyle.Italic
                        ),
                        start,
                        end
                    )
                }
                match.groups[2] != null -> {
                    // String literal
                    addStyle(SpanStyle(color = glassColors.syntaxString), start, end)
                }
                match.groups[3] != null -> {
                    // Decorator
                    addStyle(
                        SpanStyle(
                            color = glassColors.syntaxDecorator,
                            fontWeight = FontWeight.SemiBold
                        ),
                        start,
                        end
                    )
                }
                match.groups[4] != null -> {
                    // Number
                    addStyle(SpanStyle(color = glassColors.syntaxNumber), start, end)
                }
                match.groups[5] != null -> {
                    val word = match.value
                    when {
                        word in pythonKeywords -> {
                            addStyle(
                                SpanStyle(
                                    color = glassColors.syntaxKeyword,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                start,
                                end
                            )
                        }
                        word in builtinFunctions -> {
                            addStyle(
                                SpanStyle(color = glassColors.syntaxFunction),
                                start,
                                end
                            )
                        }
                        end < code.length && code[end] == '(' -> {
                            addStyle(
                                SpanStyle(
                                    color = glassColors.syntaxFunction,
                                    fontWeight = FontWeight.Medium
                                ),
                                start,
                                end
                            )
                        }
                    }
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            var index = code.indexOf(searchQuery, ignoreCase = true)
            while (index >= 0) {
                addStyle(
                    SpanStyle(
                        background = Color(0x66FBBF24),
                        fontWeight = FontWeight.Bold
                    ),
                    index,
                    (index + searchQuery.length).coerceAtMost(code.length)
                )
                index = code.indexOf(searchQuery, startIndex = index + searchQuery.length, ignoreCase = true)
            }
        }
    }
}
