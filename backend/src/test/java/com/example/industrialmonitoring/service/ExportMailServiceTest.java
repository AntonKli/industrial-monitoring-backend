package com.example.industrialmonitoring.service;

import com.example.industrialmonitoring.config.ExportMailProperties;
import com.example.industrialmonitoring.export.ExportPeriod;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

class ExportMailServiceTest {

    private JavaMailSender mailSender;
    private ExportFileService exportFileService;
    private ExportMailProperties exportMailProperties;
    private ExportMailService exportMailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        exportFileService = mock(ExportFileService.class);
        exportMailProperties = mock(ExportMailProperties.class);

        exportMailService = new ExportMailService(
                mailSender,
                exportFileService,
                exportMailProperties
        );
    }

    @Test
    void shouldSendPreparedExportAsZipAttachment()
            throws Exception {

        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 1, 1),
                ZoneId.of("Europe/Berlin")
        );

        MimeMessage mimeMessage = new MimeMessage(
                Session.getInstance(new Properties())
        );

        when(exportMailProperties.enabled())
                .thenReturn(true);

        when(exportMailProperties.from())
                .thenReturn(
                        "no-reply@industrial-monitoring.local"
                );

        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);

        doAnswer(invocation -> {
            OutputStream outputStream =
                    invocation.getArgument(1);

            outputStream.write(
                    "test-zip-content".getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            return null;
        }).when(exportFileService)
                .writeFinalExportAsZip(
                        eq(period),
                        any(OutputStream.class)
                );

        exportMailService.sendExport(
                period,
                "operator@example.com"
        );

        verify(exportFileService)
                .writeFinalExportAsZip(
                        eq(period),
                        any(OutputStream.class)
                );

        verify(mailSender).send(mimeMessage);

        assertEquals(
                "Industrial Monitoring Export 2025",
                mimeMessage.getSubject()
        );

        assertEquals(
                "no-reply@industrial-monitoring.local",
                mimeMessage.getFrom()[0].toString()
        );

        assertEquals(
                "operator@example.com",
                mimeMessage.getRecipients(
                        Message.RecipientType.TO
                )[0].toString()
        );
    }

    @Test
    void shouldRejectEmailDeliveryWhenDisabled() {
        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 1, 1),
                ZoneId.of("Europe/Berlin")
        );

        when(exportMailProperties.enabled())
                .thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> exportMailService.sendExport(
                        period,
                        "operator@example.com"
                )
        );

        verifyNoInteractions(
                mailSender,
                exportFileService
        );
    }

    @Test
    void shouldRejectBlankRecipientEmail() {
        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 1, 1),
                ZoneId.of("Europe/Berlin")
        );

        when(exportMailProperties.enabled())
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> exportMailService.sendExport(
                        period,
                        " "
                )
        );

        verifyNoInteractions(
                mailSender,
                exportFileService
        );
    }
}