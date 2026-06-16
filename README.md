# Industrial Monitoring Backend

Industrial monitoring backend built with Spring Boot for ingesting, storing and exposing telemetry data from distributed industrial edge devices.

The backend is part of the Industrial Edge Gateway ecosystem and integrates with a custom MQTT gateway implemented in CODESYS.

## Related Project

Industrial Edge Gateway (CODESYS)

https://github.com/AntonKli/industrial-edge-gateway-codesys

---

## Overview

Industrial edge devices publish telemetry, event and health data via MQTT.

This backend subscribes to MQTT topics, validates and persists incoming messages, and exposes operational data through REST APIs and OpenAPI documentation.

---

## Architecture

```text
CODESYS PLC Runtime
        │
        ▼
Industrial Edge Gateway
        │
        │ MQTT
        ▼
Mosquitto Broker
        │
        ▼
Spring Boot Backend
        │
        ▼
PostgreSQL
        │
        ├── REST API
        └── Swagger UI
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

### Testing

- JUnit 5
- Testcontainers
- MockMvc

### Infrastructure

- Docker
- Docker Compose

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
GET /api/telemetry/device/{deviceId}
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

Automated integration testing is implemented using Testcontainers.

### Test Coverage

#### Application Context

- Spring Boot startup
- PostgreSQL connectivity
- Flyway migrations

#### Repository Integration Tests

- Telemetry persistence
- PostgreSQL interaction

#### REST API Integration Tests

- Telemetry endpoints
- Controller-Service-Repository flow

#### MQTT Ingestion Integration Tests

```text
TelemetryMessage
        ↓
MqttIngestionService
        ↓
Device Registration
        ↓
Telemetry Persistence
        ↓
PostgreSQL
```

### Run Tests

```bash
mvn test
```

---

## Current Features

- MQTT ingestion pipeline
- Automatic device registration
- Telemetry persistence
- Event persistence
- Health persistence
- PostgreSQL integration
- Flyway migrations
- REST API
- OpenAPI documentation
- Docker deployment
- Testcontainers integration
- Repository integration tests
- REST integration tests
- MQTT ingestion integration tests

---

## Roadmap

- GitHub Actions CI/CD
- Metrics and observability
- Dashboard integration
- Authentication and authorization
- Historical analytics