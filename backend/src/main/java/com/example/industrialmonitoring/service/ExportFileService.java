package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.export.ExportPeriod;
import org.springframework.stereotype.Service;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
                                + period.fileNameKey()
                                + ".csv"
                );
    }

    public Path eventsStagingFile(ExportPeriod period) {
        return stagingDirectory(period)
                .resolve(
                        "events-export-"
                                + period.fileNameKey()
                                + ".csv"
                );
    }

    public Path healthStagingFile(ExportPeriod period) {
        return stagingDirectory(period)
                .resolve(
                        "health-export-"
                                + period.fileNameKey()
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
    public void writeFinalExportAsZip(
            ExportPeriod period,
            OutputStream outputStream
    ) {
        Path directory = finalDirectory(period);

        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException(
                    "Final export directory does not exist: "
                            + directory
            );
        }

        try (Stream<Path> pathStream = Files.list(directory)) {
            List<Path> csvFiles = pathStream
                    .filter(path ->
                            Files.isRegularFile(path)
                                    && path.getFileName()
                                    .toString()
                                    .endsWith(".csv")
                    )
                    .sorted()
                    .collect(Collectors.toList());

            if (csvFiles.isEmpty()) {
                throw new IllegalStateException(
                        "Final export directory contains no CSV files: "
                                + directory
                );
            }

            OutputStream nonClosingOutputStream =
                    new FilterOutputStream(outputStream) {

                        @Override
                        public void close() throws IOException {
                            flush();
                        }
                    };

            try (
                    ZipOutputStream zipOutputStream =
                            new ZipOutputStream(
                                    nonClosingOutputStream
                            )
            ) {
                for (Path csvFile : csvFiles) {
                    ZipEntry zipEntry = new ZipEntry(
                            csvFile.getFileName().toString()
                    );

                    zipOutputStream.putNextEntry(zipEntry);
                    Files.copy(csvFile, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not create ZIP archive for export "
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