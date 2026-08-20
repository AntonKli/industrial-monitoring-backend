ALTER TABLE telemetry_records
    ADD COLUMN session_id UUID;

ALTER TABLE event_records
    ADD COLUMN session_id UUID;

ALTER TABLE health_records
    ADD COLUMN session_id UUID;

ALTER TABLE telemetry_records
    ADD CONSTRAINT uq_telemetry_device_session_seq
        UNIQUE (device_id, session_id, sequence_number);

ALTER TABLE event_records
    ADD CONSTRAINT uq_event_device_session_seq
        UNIQUE (device_id, session_id, sequence_number);

ALTER TABLE health_records
    ADD CONSTRAINT uq_health_device_session_seq
        UNIQUE (device_id, session_id, sequence_number);
