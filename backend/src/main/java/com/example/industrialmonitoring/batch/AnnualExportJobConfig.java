package com.example.industrialmonitoring.batch;

import com.example.industrialmonitoring.export.ExportPeriod;
import com.example.industrialmonitoring.config.ExportProperties;
import com.example.industrialmonitoring.entity.EventRecordEntity;
import com.example.industrialmonitoring.entity.HealthRecordEntity;
import com.example.industrialmonitoring.entity.TelemetryRecordEntity;
import com.example.industrialmonitoring.export.CsvLineFormatter;
import com.example.industrialmonitoring.service.ExportFileService;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;


import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class AnnualExportJobConfig {

    public static final String JOB_NAME = "annualMonitoringExportJob";

    private static final String PREPARE_STEP_NAME =
            "prepareAnnualExportStep";

    private static final String TELEMETRY_STEP_NAME =
            "telemetryExportStep";

    private static final String EVENT_STEP_NAME =
            "eventExportStep";

    private static final String HEALTH_STEP_NAME =
            "healthExportStep";

    private static final String FINALIZE_STEP_NAME =
            "finalizeAnnualExportStep";

    private static final String TELEMETRY_HEADER =
            "id,device_id,gateway_timestamp,sequence_number,"
                    + "temperature_c,rpm,created_at";

    private static final String EVENT_HEADER =
            "id,device_id,gateway_timestamp,sequence_number,"
                    + "event_type,created_at";

    private static final String HEALTH_HEADER =
            "id,device_id,gateway_timestamp,sequence_number,"
                    + "state,mqtt_connected,pub_last_ok,buffer_fill,"
                    + "buffer_drops,diag_uptime_s,diag_reconnects,"
                    + "diag_pub_ok,diag_pub_fail,diag_last_error,created_at";

    @Bean
    public Job annualMonitoringExportJob(
            JobRepository jobRepository,
            @Qualifier("prepareAnnualExportStep")
            Step prepareAnnualExportStep,
            @Qualifier("telemetryExportStep")
            Step telemetryExportStep,
            @Qualifier("eventExportStep")
            Step eventExportStep,
            @Qualifier("healthExportStep")
            Step healthExportStep,
            @Qualifier("finalizeAnnualExportStep")
            Step finalizeAnnualExportStep
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(prepareAnnualExportStep)
                .next(telemetryExportStep)
                .next(eventExportStep)
                .next(healthExportStep)
                .next(finalizeAnnualExportStep)
                .build();
    }

    @Bean
    public Step prepareAnnualExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("prepareAnnualExportTasklet")
            Tasklet prepareAnnualExportTasklet
    ) {
        return new StepBuilder(
                PREPARE_STEP_NAME,
                jobRepository
        )
                .tasklet(
                        prepareAnnualExportTasklet,
                        transactionManager
                )
                .build();
    }

    @Bean
    public Step finalizeAnnualExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("finalizeAnnualExportTasklet")
            Tasklet finalizeAnnualExportTasklet
    ) {
        return new StepBuilder(
                FINALIZE_STEP_NAME,
                jobRepository
        )
                .tasklet(
                        finalizeAnnualExportTasklet,
                        transactionManager
                )
                .build();
    }

    @Bean
@StepScope
public Tasklet prepareAnnualExportTasklet(
        ExportFileService exportFileService,
        @Value("#{jobParameters['fromDate']}")
        String fromDate,
        @Value("#{jobParameters['toDateExclusive']}")
        String toDateExclusive,
        @Value("#{jobParameters['zoneId']}")
        String zoneId
) {
    return (contribution, chunkContext) -> {
        ExportPeriod period = exportPeriod(
                fromDate,
                toDateExclusive,
                zoneId
        );

        exportFileService.prepareStagingDirectory(period);

        return RepeatStatus.FINISHED;
    };

    }

    @Bean
@StepScope
public Tasklet finalizeAnnualExportTasklet(
        ExportFileService exportFileService,
        @Value("#{jobParameters['fromDate']}")
        String fromDate,
        @Value("#{jobParameters['toDateExclusive']}")
        String toDateExclusive,
        @Value("#{jobParameters['zoneId']}")
        String zoneId
) {
    return (contribution, chunkContext) -> {
        ExportPeriod period = exportPeriod(
                fromDate,
                toDateExclusive,
                zoneId
        );

        exportFileService.publish(period);

        return RepeatStatus.FINISHED;
    };
}

    @Bean
    public Step telemetryExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ExportProperties exportProperties,
            @Qualifier("telemetryRecordReader")
            JpaPagingItemReader<TelemetryRecordEntity> telemetryRecordReader,
            @Qualifier("telemetryRecordWriter")
            FlatFileItemWriter<TelemetryRecordEntity> telemetryRecordWriter
    ) {
        return new StepBuilder(
                TELEMETRY_STEP_NAME,
                jobRepository
        )
                .<TelemetryRecordEntity, TelemetryRecordEntity>chunk(
                        exportProperties.chunkSize(),
                        transactionManager
                )
                .reader(telemetryRecordReader)
                .writer(telemetryRecordWriter)
                .build();
    }

    @Bean
    public Step eventExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ExportProperties exportProperties,
            @Qualifier("eventRecordReader")
            JpaPagingItemReader<EventRecordEntity> eventRecordReader,
            @Qualifier("eventRecordWriter")
            FlatFileItemWriter<EventRecordEntity> eventRecordWriter
    ) {
        return new StepBuilder(
                EVENT_STEP_NAME,
                jobRepository
        )
                .<EventRecordEntity, EventRecordEntity>chunk(
                        exportProperties.chunkSize(),
                        transactionManager
                )
                .reader(eventRecordReader)
                .writer(eventRecordWriter)
                .build();
    }

    @Bean
    public Step healthExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ExportProperties exportProperties,
            @Qualifier("healthRecordReader")
            JpaPagingItemReader<HealthRecordEntity> healthRecordReader,
            @Qualifier("healthRecordWriter")
            FlatFileItemWriter<HealthRecordEntity> healthRecordWriter
    ) {
        return new StepBuilder(
                HEALTH_STEP_NAME,
                jobRepository
        )
                .<HealthRecordEntity, HealthRecordEntity>chunk(
                        exportProperties.chunkSize(),
                        transactionManager
                )
                .reader(healthRecordReader)
                .writer(healthRecordWriter)
                .build();
    }

    @Bean
@StepScope
public JpaPagingItemReader<TelemetryRecordEntity> telemetryRecordReader(
        EntityManagerFactory entityManagerFactory,
        ExportProperties exportProperties,
        @Value("#{jobParameters['fromDate']}")
        String fromDate,
        @Value("#{jobParameters['toDateExclusive']}")
        String toDateExclusive,
        @Value("#{jobParameters['zoneId']}")
        String zoneId
) {
    return new JpaPagingItemReaderBuilder<TelemetryRecordEntity>()
            .name("telemetryRecordReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("""
                    select telemetryRecord
                    from TelemetryRecordEntity telemetryRecord
                    where telemetryRecord.createdAt >= :from
                      and telemetryRecord.createdAt < :to
                    order by telemetryRecord.id
                    """)
            .parameterValues(
                    periodRange(
                            fromDate,
                            toDateExclusive,
                            zoneId
                    )
            )
            .pageSize(exportProperties.chunkSize())
            .saveState(true)
            .build();
}
    @Bean
@StepScope
public JpaPagingItemReader<EventRecordEntity> eventRecordReader(
        EntityManagerFactory entityManagerFactory,
        ExportProperties exportProperties,
        @Value("#{jobParameters['fromDate']}")
        String fromDate,
        @Value("#{jobParameters['toDateExclusive']}")
        String toDateExclusive,
        @Value("#{jobParameters['zoneId']}")
        String zoneId
) {
    return new JpaPagingItemReaderBuilder<EventRecordEntity>()
            .name("eventRecordReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("""
                    select eventRecord
                    from EventRecordEntity eventRecord
                    where eventRecord.createdAt >= :from
                      and eventRecord.createdAt < :to
                    order by eventRecord.id
                    """)
            .parameterValues(
                    periodRange(
                            fromDate,
                            toDateExclusive,
                            zoneId
                    )
            )
            .pageSize(exportProperties.chunkSize())
            .saveState(true)
            .build();
}

    @Bean
@StepScope
public JpaPagingItemReader<HealthRecordEntity> healthRecordReader(
        EntityManagerFactory entityManagerFactory,
        ExportProperties exportProperties,
        @Value("#{jobParameters['fromDate']}")
        String fromDate,
        @Value("#{jobParameters['toDateExclusive']}")
        String toDateExclusive,
        @Value("#{jobParameters['zoneId']}")
        String zoneId
) {
    return new JpaPagingItemReaderBuilder<HealthRecordEntity>()
            .name("healthRecordReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("""
                    select healthRecord
                    from HealthRecordEntity healthRecord
                    where healthRecord.createdAt >= :from
                      and healthRecord.createdAt < :to
                    order by healthRecord.id
                    """)
            .parameterValues(
                    periodRange(
                            fromDate,
                            toDateExclusive,
                            zoneId
                    )
            )
            .pageSize(exportProperties.chunkSize())
            .saveState(true)
            .build();
}

    @Bean
    @StepScope
    public FlatFileItemWriter<TelemetryRecordEntity> telemetryRecordWriter(
            ExportFileService exportFileService,
            @Value("#{jobParameters['year']}")
            Long year
    ) {
        int exportYear = Math.toIntExact(year);

        return new FlatFileItemWriterBuilder<TelemetryRecordEntity>()
                .name("telemetryRecordWriter")
                .resource(
                        new FileSystemResource(
                                exportFileService.telemetryStagingFile(
                                        exportYear
                                )
                        )
                )
                .encoding(StandardCharsets.UTF_8.name())
                .lineSeparator("\n")
                .headerCallback(
                        writer -> writer.write(TELEMETRY_HEADER)
                )
                .lineAggregator(
                        telemetryRecord -> CsvLineFormatter.formatRow(
                                telemetryRecord.getId(),
                                telemetryRecord.getDeviceId(),
                                telemetryRecord.getGatewayTimestamp(),
                                telemetryRecord.getSequenceNumber(),
                                telemetryRecord.getTemperatureC(),
                                telemetryRecord.getRpm(),
                                telemetryRecord.getCreatedAt()
                        )
                )
                .shouldDeleteIfExists(true)
                .shouldDeleteIfEmpty(false)
                .saveState(true)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<EventRecordEntity> eventRecordWriter(
            ExportFileService exportFileService,
            @Value("#{jobParameters['year']}")
            Long year
    ) {
        int exportYear = Math.toIntExact(year);

        return new FlatFileItemWriterBuilder<EventRecordEntity>()
                .name("eventRecordWriter")
                .resource(
                        new FileSystemResource(
                                exportFileService.eventsStagingFile(
                                        exportYear
                                )
                        )
                )
                .encoding(StandardCharsets.UTF_8.name())
                .lineSeparator("\n")
                .headerCallback(
                        writer -> writer.write(EVENT_HEADER)
                )
                .lineAggregator(
                        eventRecord -> CsvLineFormatter.formatRow(
                                eventRecord.getId(),
                                eventRecord.getDeviceId(),
                                eventRecord.getGatewayTimestamp(),
                                eventRecord.getSequenceNumber(),
                                eventRecord.getEventType(),
                                eventRecord.getCreatedAt()
                        )
                )
                .shouldDeleteIfExists(true)
                .shouldDeleteIfEmpty(false)
                .saveState(true)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<HealthRecordEntity> healthRecordWriter(
            ExportFileService exportFileService,
            @Value("#{jobParameters['year']}")
            Long year
    ) {
        int exportYear = Math.toIntExact(year);

        return new FlatFileItemWriterBuilder<HealthRecordEntity>()
                .name("healthRecordWriter")
                .resource(
                        new FileSystemResource(
                                exportFileService.healthStagingFile(
                                        exportYear
                                )
                        )
                )
                .encoding(StandardCharsets.UTF_8.name())
                .lineSeparator("\n")
                .headerCallback(
                        writer -> writer.write(HEALTH_HEADER)
                )
                .lineAggregator(
                        healthRecord -> CsvLineFormatter.formatRow(
                                healthRecord.getId(),
                                healthRecord.getDeviceId(),
                                healthRecord.getGatewayTimestamp(),
                                healthRecord.getSequenceNumber(),
                                healthRecord.getState(),
                                healthRecord.getMqttConnected(),
                                healthRecord.getPubLastOk(),
                                healthRecord.getBufferFill(),
                                healthRecord.getBufferDrops(),
                                healthRecord.getDiagUptimeS(),
                                healthRecord.getDiagReconnects(),
                                healthRecord.getDiagPubOk(),
                                healthRecord.getDiagPubFail(),
                                healthRecord.getDiagLastError(),
                                healthRecord.getCreatedAt()
                        )
                )
                .shouldDeleteIfExists(true)
                .shouldDeleteIfEmpty(false)
                .saveState(true)
                .build();
    }
    private ExportPeriod exportPeriod(
        String fromDate,
        String toDateExclusive,
        String zoneId
) {
    return new ExportPeriod(
            LocalDate.parse(fromDate),
            LocalDate.parse(toDateExclusive),
            ZoneId.of(zoneId)
    );
}
    private Map<String, Object> periodRange(
        String fromDate,
        String toDateExclusive,
        String zoneId
) {
    ExportPeriod period = exportPeriod(
            fromDate,
            toDateExclusive,
            zoneId
    );

    return Map.of(
            "from", period.fromTimestamp(),
            "to", period.toTimestamp()
    );
}
}