package com.example.industrialmonitoring.export;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.util.Objects;

public record ExportPeriod(
        LocalDate fromDate,
        LocalDate toDateExclusive,
        ZoneId zoneId
) {

    public ExportPeriod {
        Objects.requireNonNull(
                fromDate,
                "Export start date must not be null"
        );

        Objects.requireNonNull(
                toDateExclusive,
                "Export end date must not be null"
        );

        Objects.requireNonNull(
                zoneId,
                "Export time zone must not be null"
        );

        if (!fromDate.isBefore(toDateExclusive)) {
            throw new IllegalArgumentException(
                    "Export start date must be before the exclusive end date"
            );
        }
    }

    public static ExportPeriod forYear(
            int year,
            ZoneId zoneId
    ) {
        LocalDate fromDate = Year.of(year).atDay(1);

        return new ExportPeriod(
                fromDate,
                fromDate.plusYears(1),
                zoneId
        );
    }

    public OffsetDateTime fromTimestamp() {
        return fromDate
                .atStartOfDay(zoneId)
                .toOffsetDateTime();
    }

    public OffsetDateTime toTimestamp() {
        return toDateExclusive
                .atStartOfDay(zoneId)
                .toOffsetDateTime();
    }

    public LocalDate toDateInclusive() {
        return toDateExclusive.minusDays(1);
    }

    public String fileNameKey() {
        if (isFullCalendarYear()) {
            return String.valueOf(fromDate.getYear());
        }

        return fromDate + "_to_" + toDateInclusive();
    }

    public String exportKey() {
        if (isFullCalendarYear()) {
            return String.valueOf(fromDate.getYear());
        }

        return fromDate + "_to_" + toDateExclusive;
    }

    public boolean isFullCalendarYear() {
        return fromDate.getMonthValue() == 1
                && fromDate.getDayOfMonth() == 1
                && toDateExclusive.equals(fromDate.plusYears(1));
    }
}