# Industrial Monitoring Backend

Spring Boot backend for the Industrial Edge Gateway ecosystem.

This project extends the Industrial Edge Gateway implemented in CODESYS and provides data persistence, REST APIs and monitoring capabilities for industrial telemetry data.

Related project:

https://github.com/AntonKli/industrial-edge-gateway-codesys

---

## Overview

The Industrial Edge Gateway publishes telemetry, event and health information via MQTT.

This backend subscribes to MQTT topics, stores incoming data in PostgreSQL and exposes the data through REST endpoints and Swagger UI.

### Architecture

```text
CODESYS Edge Gateway
        |
        | MQTT
        v
Mosquitto Broker
        |
        v
Spring Boot Backend
        |
        v
PostgreSQL
        |
        v
REST API / Swagger UI
```

---

## Technology Stack

### Backend

* Java 21
* Spring Boot 3.5
* Spring Data JPA
* Hibernate
* Flyway

### Messaging

* MQTT
* Eclipse Paho MQTT Client
* Mosquitto Broker

### Database

* PostgreSQL 16

### Documentation

* OpenAPI
* Swagger UI

### Infrastructure

* Docker
* Docker Compose

---

## MQTT Topics

The backend subscribes to the following topics:

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

## Swagger UI

Available at:

```text
http://localhost:8080/swagger-ui.html
```

---

## Running with Docker

Build:

```bash
docker compose build
```

Start:

```bash
docker compose up -d
```

Stop:

```bash
docker compose down
```

---

## Environment Variables

Configuration is provided through environment variables.

Example configuration is available in:

```text
.env.example
```

Local development configuration should be stored in:

```text
.env
```

---

## Project Status

Current implementation includes:

* MQTT ingestion
* Device registration
* Telemetry persistence
* Event persistence
* Health persistence
* PostgreSQL integration
* REST API
* Swagger documentation
* Docker deployment

Planned extensions:

* Integration tests using Testcontainers
* Metrics and monitoring
* Dashboard integration
* Authentication and authorization
* Historical analytics

```
```
