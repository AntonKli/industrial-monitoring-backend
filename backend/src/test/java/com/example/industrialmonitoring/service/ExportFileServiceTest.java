package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.config.ExportProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportFileServiceTest {

    private static final int EXPORT_YEAR = 2026;

    @TempDir
    Path tempDirectory;

    private ExportFileService exportFileService;

    @BeforeEach
    void setUp() {
        ExportProperties exportProperties = new ExportProperties(
                false,
                tempDirectory.toString(),
                10,
                "0 0 2 1 1 *",
                "Europe/Berlin"
        );

        exportFileService = new ExportFileService(exportProperties);
    }

    @Test
    void shouldCreateStagingDirectoryAndExpectedFilePaths() {
        exportFileService.prepareStagingDirectory(EXPORT_YEAR);

        assertTrue(
                Files.isDirectory(
                        exportFileService.stagingDirectory(EXPORT_YEAR)
                )
        );

        assertEquals(
                exportFileService.stagingDirectory(EXPORT_YEAR)
                        .resolve("telemetry-export-2026.csv"),
                exportFileService.telemetryStagingFile(EXPORT_YEAR)
        );

        assertEquals(
                exportFileService.stagingDirectory(EXPORT_YEAR)
                        .resolve("events-export-2026.csv"),
                exportFileService.eventsStagingFile(EXPORT_YEAR)
        );

        assertEquals(
                exportFileService.stagingDirectory(EXPORT_YEAR)
                        .resolve("health-export-2026.csv"),
                exportFileService.healthStagingFile(EXPORT_YEAR)
        );
    }

    @Test
    void shouldPublishCompletedExportDirectory() throws IOException {
        exportFileService.prepareStagingDirectory(EXPORT_YEAR);

        Files.writeString(
                exportFileService.telemetryStagingFile(EXPORT_YEAR),
                "id,device_id"
        );

        exportFileService.publish(EXPORT_YEAR);

        Path publishedFile = exportFileService.finalDirectory(EXPORT_YEAR)
                .resolve("telemetry-export-2026.csv");

        assertFalse(
                Files.exists(
                        exportFileService.stagingDirectory(EXPORT_YEAR)
                )
        );

        assertTrue(Files.exists(publishedFile));

        assertEquals(
                "id,device_id",
                Files.readString(publishedFile)
        );
    }
}