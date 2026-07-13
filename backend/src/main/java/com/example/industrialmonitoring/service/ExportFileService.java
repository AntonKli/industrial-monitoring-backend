package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.config.ExportProperties;
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

    public Path stagingDirectory(int year) {
        return exportProperties.outputPath()
                .resolve(STAGING_DIRECTORY)
                .resolve(String.valueOf(year));
    }

    public Path finalDirectory(int year) {
        return exportProperties.outputPath()
                .resolve(String.valueOf(year));
    }

    public Path telemetryStagingFile(int year) {
        return stagingDirectory(year)
                .resolve("telemetry-export-" + year + ".csv");
    }

    public Path eventsStagingFile(int year) {
        return stagingDirectory(year)
                .resolve("events-export-" + year + ".csv");
    }

    public Path healthStagingFile(int year) {
        return stagingDirectory(year)
                .resolve("health-export-" + year + ".csv");
    }

    public void prepareStagingDirectory(int year) {
        Path directory = stagingDirectory(year);

        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not create staging directory: " + directory,
                    exception
            );
        }
    }

    public void publish(int year) {
        Path stagingDirectory = stagingDirectory(year);
        Path finalDirectory = finalDirectory(year);

        if (!Files.isDirectory(stagingDirectory)) {
            throw new IllegalStateException(
                    "Staging directory does not exist: " + stagingDirectory
            );
        }

        if (Files.exists(finalDirectory)) {
            throw new IllegalStateException(
                    "Final export directory already exists: " + finalDirectory
            );
        }

        try {
            moveDirectory(stagingDirectory, finalDirectory);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not publish export for year " + year,
                    exception
            );
        }
    }

    private void moveDirectory(Path source, Path target) throws IOException {
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