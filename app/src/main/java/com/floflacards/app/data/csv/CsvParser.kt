/*
 * Copyright (C) 2026 FloFla Dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.floflacards.app.data.csv

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.CodingErrorAction

/**
 * Logger interface for CsvParser to allow unit testing without Android dependencies.
 */
interface CsvLogger {
    fun warn(message: String)
    fun error(message: String, throwable: Throwable? = null)

    companion object {
        /** Default implementation using android.util.Log. */
        val Android: CsvLogger = AndroidCsvLogger()
    }
}

/** Android implementation using Log. */
private class AndroidCsvLogger : CsvLogger {
    override fun warn(message: String) {
        android.util.Log.w("CsvParser", message)
    }
    override fun error(message: String, throwable: Throwable?) {
        android.util.Log.e("CsvParser", message, throwable)
    }
}

/**
 * Parses CSV files into CsvFlashcard objects.
 *
 * Handles quoted fields, escaped quotes, embedded newlines within quoted fields,
 * and common encoding issues. Follows RFC 4180 CSV format.
 */
class CsvParser(
    private val logger: CsvLogger = CsvLogger.Android
) {

    companion object {
        // Common column name patterns for auto-detection (multi-language)
        private val QUESTION_PATTERNS = listOf(
            "question", "front", "term", "prompt",
            "frage", "seite1", "vorderseite", "begriff",
            "pytanie", "przod", "pojecie", "haslo",
            "q"
        )

        private val ANSWER_PATTERNS = listOf(
            "answer", "back", "definition", "response",
            "antwort", "seite2", "ruckseite", "definition",
            "odpowiedz", "tyl", "definicja",
            "a"
        )

        private val CATEGORY_PATTERNS = listOf(
            "category", "cat", "deck", "group", "tag",
            "kategorie", "kat",
            "kategoria",
            "c"
        )

        // UTF-8 BOM bytes
        private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

        /**
         * Detects the delimiter from a line of content.
         * Counts commas vs tabs outside of quoted fields.
         */
        fun detectDelimiter(line: String): CsvDelimiter {
            var commaCount = 0
            var tabCount = 0
            var inQuotes = false

            for (char in line) {
                when (char) {
                    '"' -> inQuotes = !inQuotes
                    ',' -> if (!inQuotes) commaCount++
                    '\t' -> if (!inQuotes) tabCount++
                }
            }

            return if (tabCount > commaCount) CsvDelimiter.TAB else CsvDelimiter.COMMA
        }
    }

    /**
     * Parses a CSV file from an InputStream.
     * Auto-detects header row, column mapping, and delimiter.
     */
    fun parse(inputStream: InputStream): CsvParseResult {
        val validCards = mutableListOf<CsvFlashcard>()
        val errors = mutableListOf<CsvParseError>()

        try {
            val reader = createReader(inputStream)
            reader.use {
                // Skip leading blank lines so header detection runs against the real header,
                // not an empty first line (which would find no columns and silently fall back
                // to treating every row — including the real header — as headerless data).
                var headerLine = reader.readLine()
                while (headerLine != null && headerLine.isBlank()) {
                    headerLine = reader.readLine()
                }
                if (headerLine == null) {
                    return CsvParseResult(
                        validCards = emptyList(),
                        errors = listOf(CsvParseError(0, "", "Empty file"))
                    )
                }

                val detectedDelimiter = detectDelimiter(headerLine)
                val (mapping, dataStartRow) = detectHeaderAndMapping(headerLine, detectedDelimiter)

                // Read logical records (handling quoted newlines)
                var rowNumber = dataStartRow

                // If no header was detected, the first line IS data — prepend it to the stream
                var firstLineToProcess: String? = if (dataStartRow == 0) headerLine else null

                var recordBuffer = StringBuilder()
                var runningQuoteCount = 0
                var eof = false
                // When dataStartRow == 0, rowNumber starts at 0 (first record will be row 1)
                // When dataStartRow == 1, rowNumber starts at 1 (first data record will be row 2)
                var currentRow = rowNumber

                while (!eof) {
                    // Use the prepended first line if available, otherwise read from stream
                    val line = firstLineToProcess ?: reader.readLine()
                    firstLineToProcess = null

                    if (line == null) {
                        // End of file
                        if (recordBuffer.isNotEmpty()) {
                            processRecord(
                                record = recordBuffer.toString(),
                                delimiter = detectedDelimiter,
                                mapping = mapping,
                                rowNumber = currentRow + 1,
                                validCards = validCards,
                                errors = errors
                            )
                            currentRow++
                        }
                        eof = true
                    } else {
                        if (recordBuffer.isEmpty()) {
                            recordBuffer.append(line)
                            runningQuoteCount = countUnescapedQuotes(line)
                        } else {
                            recordBuffer.append('\n')
                            recordBuffer.append(line)
                            runningQuoteCount += countUnescapedQuotes(line)
                        }

                        if (runningQuoteCount % 2 == 0) {
                            processRecord(
                                record = recordBuffer.toString(),
                                delimiter = detectedDelimiter,
                                mapping = mapping,
                                rowNumber = currentRow + 1,
                                validCards = validCards,
                                errors = errors
                            )
                            currentRow++
                            recordBuffer.clear()
                            runningQuoteCount = 0
                        }
                        // Else: still inside a quoted field, keep reading
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Unexpected parse error", e)
            errors.add(CsvParseError(0, "", "Parse error: ${e.message}"))
        }

        return CsvParseResult(validCards, errors)
    }

    // -- Encoding / Reader Setup --

    /**
     * Creates a BufferedReader with proper encoding detection.
     */
    private fun createReader(inputStream: InputStream): BufferedReader {
        val allBytes = inputStream.readBytes()

        if (allBytes.isEmpty()) {
            return BufferedReader(
                InputStreamReader(
                    java.io.ByteArrayInputStream(allBytes),
                    Charsets.UTF_8
                )
            )
        }

        // Remove UTF-8 BOM if present
        val rawBytes = if (allBytes.size >= UTF8_BOM.size &&
            allBytes[0] == UTF8_BOM[0] &&
            allBytes[1] == UTF8_BOM[1] &&
            allBytes[2] == UTF8_BOM[2]
        ) {
            allBytes.copyOfRange(UTF8_BOM.size, allBytes.size)
        } else {
            allBytes
        }

        // Validate UTF-8 on a sample
        val sampleSize = minOf(rawBytes.size, 4096)
        val sample = rawBytes.copyOfRange(0, sampleSize)
        val charset = if (isValidUtf8(sample)) {
            Charsets.UTF_8
        } else {
            logger.warn("File does not appear to be valid UTF-8, using ISO-8859-1")
            Charsets.ISO_8859_1
        }

        return BufferedReader(
            InputStreamReader(
                java.io.ByteArrayInputStream(rawBytes),
                charset
            )
        )
    }

    /**
     * Checks if the given bytes are valid UTF-8.
     */
    private fun isValidUtf8(bytes: ByteArray): Boolean {
        val decoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
        decoder.onMalformedInput(CodingErrorAction.REPORT)
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT)

        return try {
            decoder.decode(java.nio.ByteBuffer.wrap(bytes))
            true
        } catch (e: java.nio.charset.CharacterCodingException) {
            false
        }
    }

    // -- Header Detection --

    /**
     * Checks if a field matches any of the given patterns.
     * Uses exact match for short patterns (single char), substring for longer ones.
     */
    private fun matchesPattern(field: String, patterns: List<String>): Boolean {
        return patterns.any { pattern ->
            if (pattern.length <= 2) {
                field == pattern
            } else {
                field.contains(pattern)
            }
        }
    }

    /**
     * Detects if the first line is a header and auto-maps columns.
     */
    private fun detectHeaderAndMapping(
        firstLine: String,
        delimiter: CsvDelimiter
    ): Pair<CsvColumnMapping, Int> {
        val fields = parseCsvFields(firstLine, delimiter)
        val normalizedFields = fields.map {
            it.lowercase().replace(Regex("[^a-z0-9]"), "")
        }

        var questionCol = -1
        var answerCol = -1
        var categoryCol: Int? = null

        for ((index, field) in normalizedFields.withIndex()) {
            if (questionCol == -1 && matchesPattern(field, QUESTION_PATTERNS)) {
                questionCol = index
            }
            if (answerCol == -1 && matchesPattern(field, ANSWER_PATTERNS)) {
                answerCol = index
            }
            if (categoryCol == null && matchesPattern(field, CATEGORY_PATTERNS)) {
                categoryCol = index
            }
        }

        if (questionCol != -1 && answerCol != -1) {
            return Pair(
                CsvColumnMapping(
                    questionColumn = questionCol,
                    answerColumn = answerCol,
                    categoryColumn = categoryCol
                ),
                1
            )
        }

        return Pair(
            CsvColumnMapping(questionColumn = 0, answerColumn = 1),
            0
        )
    }

    // -- Field Parsing --

    /**
     * Parses a single CSV record into fields.
     */
    private fun parseCsvFields(record: String, delimiter: CsvDelimiter): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        val delimiterChar = if (delimiter == CsvDelimiter.COMMA) ',' else '\t'

        var i = 0
        while (i < record.length) {
            val char = record[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < record.length && record[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == delimiterChar && !inQuotes -> {
                    fields.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(char)
            }
            i++
        }

        fields.add(current.toString().trim())
        return fields
    }

    // -- Record Processing --

    /**
     * Processes a single logical record.
     */
    private fun processRecord(
        record: String,
        delimiter: CsvDelimiter,
        mapping: CsvColumnMapping,
        rowNumber: Int,
        validCards: MutableList<CsvFlashcard>,
        errors: MutableList<CsvParseError>
    ) {
        if (record.isBlank()) return

        val fields = parseCsvFields(record, delimiter)
        val card = validateAndCreateCard(fields, mapping, rowNumber, errors)

        if (card != null) {
            validCards.add(card)
        }
    }

    /**
     * Counts unescaped quote characters in text.
     */
    private fun countUnescapedQuotes(text: String): Int {
        var count = 0
        var i = 0
        while (i < text.length) {
            if (text[i] == '"') {
                if (i + 1 < text.length && text[i + 1] == '"') {
                    i += 2 // Escaped quote
                } else {
                    count++
                    i++
                }
            } else {
                i++
            }
        }
        return count
    }

    /**
     * Validates fields and creates a CsvFlashcard, or returns null on error.
     */
    private fun validateAndCreateCard(
        fields: List<String>,
        mapping: CsvColumnMapping,
        rowNumber: Int,
        errors: MutableList<CsvParseError>
    ): CsvFlashcard? {
        if (fields.size < 2) {
            errors.add(
                CsvParseError(
                    rowNumber = rowNumber,
                    rawLine = fields.joinToString(", ").take(100),
                    reason = "Need at least 2 columns (question, answer), found ${fields.size}"
                )
            )
            return null
        }

        val question = fields.getOrNull(mapping.questionColumn)?.trim().orEmpty()
        val answer = fields.getOrNull(mapping.answerColumn)?.trim().orEmpty()
        val category = mapping.categoryColumn
            ?.let { fields.getOrNull(it)?.trim() }
            ?.takeIf { it.isNotBlank() }

        if (question.isBlank()) {
            errors.add(
                CsvParseError(
                    rowNumber = rowNumber,
                    rawLine = fields.joinToString(", ").take(100),
                    reason = "Question is empty"
                )
            )
            return null
        }

        if (answer.isBlank()) {
            errors.add(
                CsvParseError(
                    rowNumber = rowNumber,
                    rawLine = fields.joinToString(", ").take(100),
                    reason = "Answer is empty"
                )
            )
            return null
        }

        return CsvFlashcard(
            question = question,
            answer = answer,
            category = category
        )
    }
}
