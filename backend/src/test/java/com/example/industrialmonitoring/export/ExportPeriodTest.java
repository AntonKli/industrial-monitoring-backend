package com.example.industrialmonitoring.export;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportPeriodTest {

    private static final ZoneId BERLIN =
            ZoneId.of("Europe/Berlin");

    @Test
    void shouldCreateAnnualPeriod() {
        ExportPeriod period =
                ExportPeriod.forYear(2025, BERLIN);

        assertEquals(
                LocalDate.of(2025, 1, 1),
                period.fromDate()
        );

        assertEquals(
                LocalDate.of(2026, 1, 1),
                period.toDateExclusive()
        );

        assertEquals("2025", period.exportKey());
        assertTrue(period.isFullCalendarYear());
    }

    @Test
    void shouldCreateFlexibleRange() {
        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2026, 2, 15),
                LocalDate.of(2026, 5, 1),
                BERLIN
        );

        assertEquals(
                "2026-02-15_to_2026-05-01",
                period.exportKey()
        );

        assertFalse(period.isFullCalendarYear());
    }
    @Test
    void shouldCreateFileNameKeyWithInclusiveEndDate() {
        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2026, 2, 15),
                LocalDate.of(2026, 5, 1),
                BERLIN
        );

        assertEquals(
                LocalDate.of(2026, 4, 30),
                period.toDateInclusive()
        );

        assertEquals(
                "2026-02-15_to_2026-04-30",
                period.fileNameKey()
        );

        assertEquals(
                "2026-02-15_to_2026-05-01",
                period.exportKey()
        );
    }

    @Test
    void shouldRespectDaylightSavingTime() {
        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 1),
                BERLIN
        );

        assertEquals(
                OffsetDateTime.parse(
                        "2026-01-01T00:00:00+01:00"
                ),
                period.fromTimestamp()
        );

        assertEquals(
                OffsetDateTime.parse(
                        "2026-04-01T00:00:00+02:00"
                ),
                period.toTimestamp()
        );
    }

    @Test
    void shouldRejectEmptyPeriod() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExportPeriod(
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 1),
                        BERLIN
                )
        );
    }

    @Test
    void shouldRejectReversedPeriod() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExportPeriod(
                        LocalDate.of(2026, 4, 2),
                        LocalDate.of(2026, 4, 1),
                        BERLIN
                )
        );
    }
}