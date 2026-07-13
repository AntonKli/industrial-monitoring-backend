package com.example.industrialmonitoring.export;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class CsvLineFormatter {

    private static final String DELIMITER = ",";

    private CsvLineFormatter() {
        // Utility class: no instances required.
    }

    public static String formatRow(Object... values) {
        return Arrays.stream(values)
                .map(CsvLineFormatter::formatValue)
                .collect(Collectors.joining(DELIMITER));
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "";
        }

        String text = value.toString();

        if (value instanceof String) {
            text = protectAgainstSpreadsheetFormula(text);
        }

        String escapedText = text.replace("\"", "\"\"");

        if (requiresQuotes(escapedText)) {
            return "\"" + escapedText + "\"";
        }

        return escapedText;
    }

    private static boolean requiresQuotes(String value) {
        if (value.isEmpty()) {
            return false;
        }

        return value.contains(DELIMITER)
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r")
                || Character.isWhitespace(value.charAt(0))
                || Character.isWhitespace(value.charAt(value.length() - 1));
    }

    private static String protectAgainstSpreadsheetFormula(String value) {
        if (value.isEmpty()) {
            return value;
        }

        return switch (value.charAt(0)) {
            case '=', '+', '-', '@', '\t', '\r' -> "'" + value;
            default -> value;
        };
    }
}