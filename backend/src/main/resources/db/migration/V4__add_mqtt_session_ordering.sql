ALTER TABLE telemetry_records
    ADD COLUMN session_generation BIGINT,
    ADD CONSTRAINT ck_telemetry_session_generation_positive
        CHECK (session_generation IS NULL OR session_generation >= 1);

ALTER TABLE event_records
    ADD COLUMN session_generation BIGINT,
    ADD CONSTRAINT ck_event_session_generation_positive
        CHECK (session_generation IS NULL OR session_generation >= 1);

ALTER TABLE health_records
    ADD COLUMN session_generation BIGINT,
    ADD CONSTRAINT ck_health_session_generation_positive
        CHECK (session_generation IS NULL OR session_generation >= 1);

CREATE TABLE mqtt_device_sessions (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    session_generation BIGINT NOT NULL,
    session_id UUID NOT NULL,
    first_received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_mqtt_device_sessions_device
        FOREIGN KEY (device_id) REFERENCES devices (device_id) ON DELETE RESTRICT,
    CONSTRAINT ck_mqtt_device_sessions_generation_positive
        CHECK (session_generation >= 1),
    CONSTRAINT uq_mqtt_device_sessions_device_generation
        UNIQUE (device_id, session_generation),
    CONSTRAINT uq_mqtt_device_sessions_device_session
        UNIQUE (device_id, session_id)
);

DROP INDEX idx_telemetry_device_created_at;
DROP INDEX idx_events_device_created_at;
DROP INDEX idx_health_device_created_at;

CREATE INDEX idx_telemetry_created_at_id
    ON telemetry_records (created_at DESC, id DESC);

CREATE INDEX idx_telemetry_device_created_at
    ON telemetry_records (device_id, created_at DESC, id DESC);

CREATE INDEX idx_telemetry_device_observed
    ON telemetry_records (
        device_id,
        session_generation DESC,
        sequence_number DESC,
        gateway_timestamp DESC,
        created_at DESC,
        id DESC
    )
    WHERE session_generation IS NOT NULL AND session_id IS NOT NULL;

CREATE INDEX idx_events_created_at_id
    ON event_records (created_at DESC, id DESC);

CREATE INDEX idx_events_device_created_at
    ON event_records (device_id, created_at DESC, id DESC);

CREATE INDEX idx_health_created_at_id
    ON health_records (created_at DESC, id DESC);

CREATE INDEX idx_health_device_created_at
    ON health_records (device_id, created_at DESC, id DESC);

CREATE INDEX idx_health_device_observed
    ON health_records (
        device_id,
        session_generation DESC,
        sequence_number DESC,
        gateway_timestamp DESC,
        created_at DESC,
        id DESC
    )
    WHERE session_generation IS NOT NULL AND session_id IS NOT NULL;
