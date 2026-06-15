CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE telemetry_records (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    gateway_timestamp BIGINT NOT NULL,
    sequence_number BIGINT NOT NULL,
    temperature_c NUMERIC(6, 2),
    rpm INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event_records (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    gateway_timestamp BIGINT NOT NULL,
    sequence_number BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE health_records (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    gateway_timestamp BIGINT NOT NULL,
    sequence_number BIGINT NOT NULL,
    state INTEGER,
    mqtt_connected BOOLEAN,
    pub_last_ok BOOLEAN,
    buffer_fill INTEGER,
    buffer_drops BIGINT,
    diag_uptime_s BIGINT,
    diag_reconnects BIGINT,
    diag_pub_ok BIGINT,
    diag_pub_fail BIGINT,
    diag_last_error INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_telemetry_device_created_at
    ON telemetry_records (device_id, created_at DESC);

CREATE INDEX idx_events_device_created_at
    ON event_records (device_id, created_at DESC);

CREATE INDEX idx_health_device_created_at
    ON health_records (device_id, created_at DESC);
