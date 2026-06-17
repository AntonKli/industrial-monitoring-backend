# Industrial Monitoring Backend

Industrial monitoring backend built with Spring Boot for ingesting, storing and exposing telemetry data from distributed industrial edge devices.

The backend is part of the Industrial Edge Gateway ecosystem and integrates with a custom MQTT gateway implemented in CODESYS.

## Related Project

Industrial Edge Gateway (CODESYS)

https://github.com/AntonKli/industrial-edge-gateway-codesys

---

## Overview

Industrial edge devices publish telemetry, event and health data via MQTT.

This backend subscribes to MQTT topics, validates and persists incoming messages, and exposes operational data through REST APIs, OpenAPI documentation and operational monitoring endpoints.

The project provides a complete industrial data pipeline from MQTT based edge communication to backend persistence, API access, Prometheus metrics and Grafana visualization.

---

## Architecture

```text
CODESYS PLC Runtime
        |
        v
Industrial Edge Gateway
        |
        | MQTT
        v
Mosquitto Broker
        |
        v
Spring Boot Backend
        |
        +--> REST API
        +--> OpenAPI / Swagger UI
        +--> Actuator Endpoints
        +--> Prometheus Metrics
        |
        v
PostgreSQL

Prometheus
        |
        v
Grafana
```

---

## Technology Stack

### Backend

- Java 21
- Spring Boot 3.5
- Spring Data JPA
- Hibernate
- Flyway

### Messaging

- MQTT
- Eclipse Paho MQTT Client
- Eclipse Mosquitto

### Database

- PostgreSQL 16

### Monitoring

- Spring Boot Actuator
- Prometheus
- Grafana

### Testing

- JUnit 5
- Testcontainers
- MockMvc

### Infrastructure

- Docker
- Docker Compose
- GitHub Actions

### Documentation

- OpenAPI 3
- Swagger UI

---

## MQTT Topics

The backend subscribes to:

```text
rtz/+/telemetry
rtz/+/events
rtz/+/health
```

### Example Telemetry Message

```json
{
  "v": 1,
  "ts": 123000,
  "seq": 3,
  "temp_c": 30.2,
  "rpm": 1600
}
```

### Example Event Message

```json
{
  "v": 1,
  "ts": 123001,
  "seq": 4,
  "type": "ALARM_RAISED"
}
```

### Example Health Message

```json
{
  "v": 1,
  "ts": 123002,
  "seq": 5,
  "state": 2,
  "mqtt_connected": true,
  "pub_last_ok": true
}
```

---

## REST API

### Devices

```text
GET /api/devices
GET /api/devices/{deviceId}
```

### Telemetry

```text
GET /api/telemetry
GET /api/telemetry/latest
GET /api/telemetry/paged?page=0&size=50

GET /api/telemetry/device/{deviceId}
GET /api/telemetry/device/{deviceId}/paged?page=0&size=50

GET /api/telemetry/device/{deviceId}/range?from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z&page=0&size=50
```

### Events

```text
GET /api/events
GET /api/events/device/{deviceId}
```

### Health

```text
GET /api/health
GET /api/health/latest
GET /api/health/device/{deviceId}
```

---

## Operational Monitoring

### Actuator Endpoints

```text
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
GET /actuator/prometheus
```

### Example Health Response

```json
{
  "status": "UP"
}
```

### Custom Prometheus Metrics

```text
industrial_telemetry_records_saved_total
industrial_event_records_saved_total
industrial_health_records_saved_total
```

---

## Monitoring Dashboard

The backend exports custom Prometheus metrics and provides a Grafana dashboard for operational monitoring.

### Grafana Dashboard

![Grafana Dashboard](docs/images/grafana-dashboard.png)

---

## API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

---

## Running Locally

### Build

```bash
docker compose build
```

### Start

```bash
docker compose up -d
```

### Stop

```bash
docker compose down
```

### Service URLs

```text
Backend API:   http://localhost:8080
Swagger UI:    http://localhost:8080/swagger-ui.html
Prometheus:    http://localhost:19090
Grafana:       http://localhost:13000
```

Default Grafana login:

```text
Username: admin
Password: admin
```

---

## Configuration

Application configuration is provided through environment variables.

Example configuration:

```text
.env.example
```

Local development configuration:

```text
.env
```

---

## Testing

Automated integration testing is implemented using Testcontainers and executed automatically through GitHub Actions.

### Test Coverage

#### Application Context Tests

- Spring Boot startup
- PostgreSQL connectivity
- Flyway migrations

#### Repository Integration Tests

- Telemetry persistence
- PostgreSQL interaction

#### MQTT Ingestion Integration Tests

- Telemetry ingestion
- Event ingestion
- Health ingestion
- Automatic device registration
- Persistence verification

#### REST API Integration Tests

- Telemetry endpoints
- Pagination endpoints
- Time-range filtering endpoints
- Event endpoints
- Health endpoints
- Controller-Service-Repository flow

### Run Tests

```bash
mvn test
```

---

## CI Pipeline

GitHub Actions automatically executes the test suite for every push and pull request targeting the main branch.

Pipeline stages include:

- Build validation
- Spring Boot context verification
- Integration test execution
- PostgreSQL Testcontainer provisioning

---

## Current Capabilities

- MQTT telemetry ingestion
- Event ingestion
- Health monitoring ingestion
- Automatic device registration
- Telemetry pagination
- Telemetry time-range filtering
- PostgreSQL persistence
- Database versioning with Flyway
- REST API access layer
- OpenAPI documentation
- Spring Boot Actuator monitoring
- Prometheus metrics export
- Grafana dashboard visualization
- Docker based deployment
- Automated integration testing
- Continuous Integration with GitHub Actions

---

