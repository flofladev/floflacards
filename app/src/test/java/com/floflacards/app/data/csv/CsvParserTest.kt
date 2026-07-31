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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Unit tests for CsvParser.
 * Covers RFC 4180 compliance, encoding detection, TSV, header auto-detection,
 * and edge cases.
 */
class CsvParserTest {

    /** No-op logger for unit tests — we don't need log output in tests. */
    private val testLogger = object : CsvLogger {
        override fun warn(message: String) {}
        override fun error(message: String, throwable: Throwable?) {}
    }

    private val parser = CsvParser(testLogger)

    // -- Basic Parsing --

    @Test
    fun `simple CSV with header parses correctly`() {
        val csv = "question,answer\nWhat is 2+2?,4\nCapital of France?,Paris\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(2, result.validCards.size)
        assertEquals("What is 2+2?", result.validCards[0].question)
        assertEquals("4", result.validCards[0].answer)
        assertEquals("Capital of France?", result.validCards[1].question)
        assertEquals("Paris", result.validCards[1].answer)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `CSV without header uses first two columns`() {
        val csv = "Hello,World\nFoo,Bar\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(2, result.validCards.size)
        assertEquals("Hello", result.validCards[0].question)
        assertEquals("World", result.validCards[0].answer)
    }

    @Test
    fun `empty file returns error`() {
        val csv = ""
        val result = parser.parse(csv.toByteArray().inputStream())

        assertTrue(result.validCards.isEmpty())
        assertEquals(1, result.errors.size)
        assertEquals("Empty file", result.errors[0].reason)
    }

    // -- Quoted Fields --

    @Test
    fun `quoted fields with commas parse correctly`() {
        val csv = "question,answer\n\"What is the capital, of France?\",\"Paris, France\"\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(1, result.validCards.size)
        assertEquals("What is the capital, of France?", result.validCards[0].question)
        assertEquals("Paris, France", result.validCards[0].answer)
    }

    @Test
    fun `escaped quotes within fields parse correctly`() {
        val csv = "question,answer\n\"What is \"\"quoted\"\" text?\",\"He said \"\"hello\"\"\"\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(1, result.validCards.size)
        assertEquals("What is \"quoted\" text?", result.validCards[0].question)
        assertEquals("He said \"hello\"", result.validCards[0].answer)
    }

    @Test
    fun `newlines within quoted fields are preserved`() {
        val csv = "question,answer\n\"Line 1\nLine 2\",\"Answer\"\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(1, result.validCards.size)
        assertEquals("Line 1\nLine 2", result.validCards[0].question)
        assertEquals("Answer", result.validCards[0].answer)
    }

    // -- TSV Support --

    @Test
    fun `TSV with header parses correctly`() {
        val tsv = "question\tanswer\nWhat is 2+2?\t4\nCapital of France?\tParis\n"
        val result = parser.parse(tsv.toByteArray().inputStream())

        assertEquals(2, result.validCards.size)
        assertEquals("What is 2+2?", result.validCards[0].question)
        assertEquals("4", result.validCards[0].answer)
        assertEquals("Capital of France?", result.validCards[1].question)
        assertEquals("Paris", result.validCards[1].answer)
    }

    @Test
    fun `TSV without header uses first two columns`() {
        val tsv = "Hello\tWorld\nFoo\tBar\n"
        val result = parser.parse(tsv.toByteArray().inputStream())

        assertEquals(2, result.validCards.size)
        assertEquals("Hello", result.validCards[0].question)
        assertEquals("World", result.validCards[0].answer)
    }

    // -- Header Auto-Detection --

    @Test
    fun `detects German header patterns`() {
        val csv = "Frage,Antwort\nWas ist 2+2?,4\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(1, result.validCards.size)
        assertEquals("Was ist 2+2?", result.validCards[0].question)
        assertEquals("4", result.validCards[0].answer)
    }

    @Test
    fun `detects Polish header patterns`() {
        val csv = "Pytanie,Odpowiedz\nCo to jest?,Rzecz\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(1, result.validCards.size)
        assertEquals("Co to jest?", result.validCards[0].question)
        assertEquals("Rzecz", result.validCards[0].answer)
    }

    @Test
    fun `detects front and back header patterns`() {
        val csv = "Front,Back\nVocab,Definition\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(1, result.validCards.size)
        assertEquals("Vocab", result.validCards[0].question)
        assertEquals("Definition", result.validCards[0].answer)
    }

    @Test
    fun `detects category column when present`() {
        val csv = "question,answer,category\nQ1,A1,Science\nQ2,A2,Math\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(2, result.validCards.size)
        assertEquals("Science", result.validCards[0].category)
        assertEquals("Math", result.validCards[1].category)
    }

    // -- Error Handling --

    @Test
    fun `rows with empty question are reported as errors`() {
        val csv = "question,answer\n,Answer\nValid Q,Valid A\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(1, result.validCards.size)
        assertEquals(1, result.errors.size)
        assertEquals("Question is empty", result.errors[0].reason)
    }

    @Test
    fun `rows with empty answer are reported as errors`() {
        val csv = "question,answer\nQ1,\nValid Q,Valid A\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(1, result.validCards.size)
        assertEquals(1, result.errors.size)
        assertEquals("Answer is empty", result.errors[0].reason)
    }

    @Test
    fun `rows with insufficient columns are reported as errors`() {
        val csv = "question,answer\nSingleColumnOnly\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertTrue(result.validCards.isEmpty())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].reason.contains("at least 2 columns"))
    }

    @Test
    fun `blank lines are skipped`() {
        val csv = "question,answer\n\nQ1,A1\n\n\nQ2,A2\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(2, result.validCards.size)
    }

    @Test
    fun `leading blank line before header does not break header detection`() {
        val csv = "\nquestion,answer,category\nQ1,A1,Science\nQ2,A2,Math\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertTrue(result.errors.isEmpty())
        assertEquals(2, result.validCards.size)
        assertEquals("Q1", result.validCards[0].question)
        assertEquals("Science", result.validCards[0].category)
        assertEquals("Math", result.validCards[1].category)
    }

    @Test
    fun `multiple leading blank lines before header do not break header detection`() {
        val csv = "\n\n\nquestion,answer,category\nQ1,A1,Science\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.validCards.size)
        assertEquals("Science", result.validCards[0].category)
    }

    // -- UTF-8 BOM --

    @Test
    fun `handles UTF-8 BOM in header detection`() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val csvContent = "question,answer\nQ1,A1\n"
        val csvWithBom = bom + csvContent.toByteArray()
        val result = parser.parse(csvWithBom.inputStream())

        assertEquals(1, result.validCards.size)
        assertEquals("Q1", result.validCards[0].question)
    }

    // -- Delimiter Detection --

    @Test
    fun `detectDelimiter returns COMMA for comma-separated line`() {
        val line = "col1,col2,col3"
        assertEquals(CsvDelimiter.COMMA, CsvParser.detectDelimiter(line))
    }

    @Test
    fun `detectDelimiter returns TAB for tab-separated line`() {
        val line = "col1\tcol2\tcol3"
        assertEquals(CsvDelimiter.TAB, CsvParser.detectDelimiter(line))
    }

    @Test
    fun `detectDelimiter returns COMMA when tabs are inside quotes`() {
        val line = "\"col1\tcol2\",col3"
        assertEquals(CsvDelimiter.COMMA, CsvParser.detectDelimiter(line))
    }

    // -- Extra Columns --

    @Test
    fun `ignores extra columns beyond question and answer`() {
        val csv = "question,answer,extra1,extra2\nQ1,A1,X1,X2\n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals(1, result.validCards.size)
        assertEquals("Q1", result.validCards[0].question)
        assertEquals("A1", result.validCards[0].answer)
        assertNull(result.validCards[0].category)
    }

    @Test
    fun `trims whitespace from fields`() {
        val csv = "question,answer\n  Q1  ,  A1  \n"
        val result = parser.parse(csv.toByteArray().inputStream())

        assertEquals("Q1", result.validCards[0].question)
        assertEquals("A1", result.validCards[0].answer)
    }

    // -- Regression: GitHub issue #21 --

    @Test
    fun `BOM-prefixed 3-column CSV with quoted multi-comma German text parses cleanly (issue #21)`() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val csvContent = "question,answer,category\n" +
            "Welche Leinen- und Seilarten nennt die FwDV 1 für den Feuerwehrdienst?," +
            "\"Feuerwehrleine, Mehrzweckleine, Dynamikseile/Kernmantelseile.\",Grundlagen\n" +
            "Wofür dient der Halbschlag?," +
            "\"Zum Führen von Gegenständen in Zugrichtung, z. B. beim Hochziehen.\",Halbschlag\n"
        val csvWithBom = bom + csvContent.toByteArray(Charsets.UTF_8)

        val result = parser.parse(csvWithBom.inputStream())

        assertTrue(result.errors.isEmpty())
        assertEquals(2, result.validCards.size)
        assertEquals(
            "Feuerwehrleine, Mehrzweckleine, Dynamikseile/Kernmantelseile.",
            result.validCards[0].answer
        )
        assertEquals("Grundlagen", result.validCards[0].category)
        assertEquals("Halbschlag", result.validCards[1].category)
    }
}
