package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.export.ExportPeriod;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class ExportFileService {

    private static final String STAGING_DIRECTORY = ".staging";

    private final ExportProperties exportProperties;

    public ExportFileService(ExportProperties exportProperties) {
        this.exportProperties = exportProperties;
    }

    public Path stagingDirectory(ExportPeriod period) {
        return exportProperties.outputPath()
                .resolve(STAGING_DIRECTORY)
                .resolve(period.exportKey());
    }

    public Path finalDirectory(ExportPeriod period) {
        return exportProperties.outputPath()
                .resolve(period.exportKey());
    }

    public Path telemetryStagingFile(ExportPeriod period) {
        return stagingDirectory(period)
                .resolve(
                        "telemetry-export-"
                                + period.exportKey()
                                + ".csv"
                );
    }

    public Path eventsStagingFile(ExportPeriod period) {
        return stagingDirectory(period)
                .resolve(
                        "events-export-"
                                + period.exportKey()
                                + ".csv"
                );
    }

    public Path healthStagingFile(ExportPeriod period) {
        return stagingDirectory(period)
                .resolve(
                        "health-export-"
                                + period.exportKey()
                                + ".csv"
                );
    }

    public void prepareStagingDirectory(ExportPeriod period) {
        Path directory = stagingDirectory(period);

        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not create staging directory: "
                            + directory,
                    exception
            );
        }
    }

    public void publish(ExportPeriod period) {
        Path stagingDirectory = stagingDirectory(period);
        Path finalDirectory = finalDirectory(period);

        if (!Files.isDirectory(stagingDirectory)) {
            throw new IllegalStateException(
                    "Staging directory does not exist: "
                            + stagingDirectory
            );
        }

        if (Files.exists(finalDirectory)) {
            throw new IllegalStateException(
                    "Final export directory already exists: "
                            + finalDirectory
            );
        }

        try {
            moveDirectory(stagingDirectory, finalDirectory);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not publish export "
                            + period.exportKey(),
                    exception
            );
        }
    }

    /*
     * Temporary compatibility adapters.
     *
     * They keep the existing annual batch job working while
     * its callers are migrated from year to ExportPeriod.
     */

    public Path stagingDirectory(int year) {
        return stagingDirectory(annualPeriod(year));
    }

    public Path finalDirectory(int year) {
        return finalDirectory(annualPeriod(year));
    }

    public Path telemetryStagingFile(int year) {
        return telemetryStagingFile(annualPeriod(year));
    }

    public Path eventsStagingFile(int year) {
        return eventsStagingFile(annualPeriod(year));
    }

    public Path healthStagingFile(int year) {
        return healthStagingFile(annualPeriod(year));
    }

    public void prepareStagingDirectory(int year) {
        prepareStagingDirectory(annualPeriod(year));
    }

    public void publish(int year) {
        publish(annualPeriod(year));
    }

    private ExportPeriod annualPeriod(int year) {
        return ExportPeriod.forYear(
                year,
                exportProperties.zoneId()
        );
    }

    private void moveDirectory(
            Path source,
            Path target
    ) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }
}