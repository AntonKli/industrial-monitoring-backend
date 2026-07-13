package com.example.industrialmonitoring.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvLineFormatterTest {

    @Test
    void shouldFormatSimpleCsvRow() {
        String result = CsvLineFormatter.formatRow(
                1L,
                "edge01",
                31.5
        );

        assertEquals(
                "1,edge01,31.5",
                result
        );
    }

    @Test
    void shouldWriteNullAsEmptyField() {
        String result = CsvLineFormatter.formatRow(
                1L,
                null,
                "edge01"
        );

        assertEquals(
                "1,,edge01",
                result
        );
    }

    @Test
    void shouldEscapeCommaQuotesAndLineBreaks() {
        String result = CsvLineFormatter.formatRow(
                "Alarm, \"HIGH\"\nSecond line"
        );

        assertEquals(
                "\"Alarm, \"\"HIGH\"\"\nSecond line\"",
                result
        );
    }

    @Test
    void shouldProtectTextAgainstSpreadsheetFormulas() {
        String result = CsvLineFormatter.formatRow(
                "=2+3",
                "+SUM(A1:A2)",
                "-danger",
                "@command",
                -12.5
        );

        assertEquals(
                "'=2+3,'+SUM(A1:A2),'-danger,'@command,-12.5",
                result
        );
    }

    @Test
    void shouldQuoteValuesWithLeadingOrTrailingWhitespace() {
        String result = CsvLineFormatter.formatRow(
                " leading",
                "trailing "
        );

        assertEquals(
                "\" leading\",\"trailing \"",
                result
        );
    }
}