package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.config.ExportMailProperties;
import com.example.industrialmonitoring.exception.ExportMailException;
import com.example.industrialmonitoring.export.ExportPeriod;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Service
public class ExportMailService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ExportMailService.class);

    private final JavaMailSender mailSender;
    private final ExportFileService exportFileService;
    private final ExportMailProperties exportMailProperties;

    public ExportMailService(
            JavaMailSender mailSender,
            ExportFileService exportFileService,
            ExportMailProperties exportMailProperties
    ) {
        this.mailSender = mailSender;
        this.exportFileService = exportFileService;
        this.exportMailProperties = exportMailProperties;
    }

    public void sendExport(
            ExportPeriod period,
            String recipientEmail
    ) {
        Objects.requireNonNull(
                period,
                "Export period must not be null"
        );

        if (!exportMailProperties.enabled()) {
            throw new IllegalStateException(
                    "Export email delivery is disabled"
            );
        }

        if (recipientEmail == null
                || recipientEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient email address must not be blank"
            );
        }

        Path temporaryZip = createTemporaryZip(period);

        try {
            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            StandardCharsets.UTF_8.name()
                    );

            helper.setFrom(exportMailProperties.from());
            helper.setTo(recipientEmail);
            helper.setSubject(
                    "Industrial Monitoring Export "
                            + period.fileNameKey()
            );

            helper.setText(
                    createMailText(period),
                    false
            );

            helper.addAttachment(
                    createAttachmentFileName(period),
                    new FileSystemResource(temporaryZip)
            );

            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            throw new ExportMailException(
                    "Could not send export email to "
                            + recipientEmail,
                    exception
            );
        } finally {
            deleteTemporaryFile(temporaryZip);
        }
    }

    private Path createTemporaryZip(
            ExportPeriod period
    ) {
        Path temporaryZip = null;

        try {
            temporaryZip = Files.createTempFile(
                    "monitoring-export-",
                    ".zip"
            );

            try (OutputStream outputStream =
                         Files.newOutputStream(temporaryZip)) {

                exportFileService.writeFinalExportAsZip(
                        period,
                        outputStream
                );
            }

            return temporaryZip;
        } catch (IOException | RuntimeException exception) {
            deleteTemporaryFile(temporaryZip);

            throw new ExportMailException(
                    "Could not create ZIP attachment for export "
                            + period.exportKey(),
                    exception
            );
        }
    }

    private String createAttachmentFileName(
            ExportPeriod period
    ) {
        return "monitoring-export-"
                + period.fileNameKey()
                + ".zip";
    }

    private String createMailText(
            ExportPeriod period
    ) {
        return "Im Anhang befindet sich der Monitoring-Export "
                + "für den Zeitraum "
                + period.fromDate()
                + " bis "
                + period.toDateInclusive()
                + ".";
    }

    private void deleteTemporaryFile(
            Path temporaryFile
    ) {
        if (temporaryFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException exception) {
            LOGGER.warn(
                    "Could not delete temporary export file: {}",
                    temporaryFile,
                    exception
            );
        }
    }
}