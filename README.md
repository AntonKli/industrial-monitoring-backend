# Industrial Monitoring Backend

A Spring Boot backend for ingesting, validating, storing, monitoring and exporting data from industrial edge devices.

The backend is part of an industrial monitoring setup and integrates with a custom MQTT edge gateway implemented in CODESYS.

## Related Project

[Industrial Edge Gateway – CODESYS](https://github.com/AntonKli/industrial-edge-gateway-codesys)

---

## Overview

Industrial edge devices publish telemetry, event and health data through MQTT.

The backend subscribes to the configured MQTT topics, validates incoming messages, automatically registers devices and stores the received data in PostgreSQL.

The stored information is available through REST APIs and can be monitored with Spring Boot Actuator, Prometheus and Grafana.

A scheduled Spring Batch job creates annual CSV archives for telemetry, event and health records. The records are processed in configurable chunks and initially written to a staging directory. The completed export is published only after every batch step has finished successfully.

The complete data flow has been tested with a CODESYS-based Industrial Edge Gateway publishing MQTT messages through Eclipse Mosquitto into the Spring Boot backend.

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
Eclipse Mosquitto
        |
        v
Spring Boot Backend
        |
        +--------------------> PostgreSQL
        |
        +--------------------> REST API
        |
        +--------------------> OpenAPI / Swagger UI
        |
        +--------------------> Spring Boot Actuator
        |
        +--------------------> Prometheus Metrics
        |                           |
        |                           v
        |                       Prometheus
        |                           |
        |                           v
        |                        Grafana
        |
        +--------------------> Spring Batch
                                    |
                                    v
                              Annual CSV Export
```

---

## Key Features

- MQTT ingestion for telemetry, event and health messages
- Integration with a CODESYS-based industrial edge gateway
- Automatic device registration
- PostgreSQL persistence with Spring Data JPA and Hibernate
- Database versioning with Flyway
- REST APIs with pagination and time-range filtering
- OpenAPI documentation and Swagger UI
- Spring Boot Actuator endpoints
- Custom Prometheus metrics
- Grafana monitoring dashboard
- Scheduled annual CSV exports with Spring Batch
- Chunk-based processing for larger datasets
- Persistent Spring Batch job metadata
- Staging and publication workflow for complete exports
- Manual REST endpoint for starting annual exports
- Duplicate export protection
- Docker Compose deployment
- Unit and integration tests
- Continuous integration with GitHub Actions

---

## Technology Stack

### Backend

- Java 21
- Spring Boot 3.5
- Spring Batch
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
- Mockito
- MockMvc
- Testcontainers

### Infrastructure

- Docker
- Docker Compose
- Maven
- GitHub Actions

### Documentation

- OpenAPI 3
- Swagger UI

---

## MQTT Integration

The backend subscribes to the following MQTT topic patterns:

```text
rtz/+/telemetry
rtz/+/events
rtz/+/health
```

Example topics for the device `edge01`:

```text
rtz/edge01/telemetry
rtz/edge01/events
rtz/edge01/health
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
  "pub_last_ok": true,
  "buffer_fill": 0,
  "buffer_drops": 0,
  "diag_uptime_s": 123,
  "diag_reconnects": 1,
  "diag_pub_ok": 120,
  "diag_pub_fail": 0,
  "diag_last_error": 0
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

GET /api/telemetry/device/{deviceId}/range
    ?from=2026-01-01T00:00:00Z
    &to=2026-12-31T23:59:59Z
    &page=0
    &size=50
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

### Annual Exports

```text
POST /api/exports/yearly?year={year}
```

---

## Scheduled Annual CSV Export

The backend uses Spring Batch to create yearly application-level archives of stored telemetry, event and health records.

The default scheduler runs every January 1 at 02:00 in the `Europe/Berlin` time zone and exports the complete previous calendar year.

Example:

```text
Scheduler execution: January 1, 2027 at 02:00
Export year:         2026
Export range:        January 1, 2026 until January 1, 2027
```

The export range is based on the backend persistence timestamp `createdAt`:

```text
createdAt >= beginning of export year
createdAt <  beginning of following year
```

### Batch Job Flow

```text
prepareAnnualExportStep
          |
          v
telemetryExportStep
          |
          v
eventExportStep
          |
          v
healthExportStep
          |
          v
finalizeAnnualExportStep
```

### Batch Steps

#### `prepareAnnualExportStep`

Creates the staging directory for the selected export year.

#### `telemetryExportStep`

Reads telemetry records in pages and writes them to a CSV file.

#### `eventExportStep`

Reads event records in pages and writes them to a CSV file.

#### `healthExportStep`

Reads health records in pages and writes them to a CSV file.

#### `finalizeAnnualExportStep`

Publishes the annual export only after every previous step has completed successfully.

---

## Staging and Publication

CSV files are initially written to:

```text
exports/.staging/{year}/
```

After every export step succeeds, the staging directory is moved to:

```text
exports/{year}/
```

This prevents incomplete exports from appearing in the final export directory.

A completed export contains:

```text
exports/
└── 2026/
    ├── telemetry-export-2026.csv
    ├── events-export-2026.csv
    └── health-export-2026.csv
```

The generated `exports` directory is excluded from Git.

---

## Chunk-Based Processing

The configured chunk size determines how many database records are processed within one Spring Batch transaction.

```dotenv
EXPORT_CHUNK_SIZE=100
```

The job reads records page by page instead of loading the complete annual dataset into application memory.

---

## Spring Batch Metadata

Spring Batch stores job and step information in PostgreSQL.

The metadata includes:

- Job instances
- Job executions
- Job parameters
- Step executions
- Execution status
- Read and write counts
- Restart information
- Execution contexts

The export year is an identifying job parameter.

A successfully completed export therefore cannot accidentally be started again with the same year.

---

## Manual Annual Export

An annual export can also be started manually through the REST API.

### PowerShell

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/exports/yearly?year=2026" |
  ConvertTo-Json
```

### cURL

```bash
curl -X POST \
  "http://localhost:8080/api/exports/yearly?year=2026"
```

### Successful Response

```json
{
  "executionId": 1,
  "jobName": "annualMonitoringExportJob",
  "year": 2026,
  "status": "COMPLETED"
}
```

---

## Export Error Responses

### Invalid Export Year

A year before 2000 or after the current year returns:

```text
HTTP 400 Bad Request
Content-Type: application/problem+json
```

Example response:

```json
{
  "type": "about:blank",
  "title": "Invalid export year",
  "status": 400,
  "detail": "Export year must be between 2000 and 2026",
  "instance": "/api/exports/yearly"
}
```

### Existing or Running Export

An export that is already running, completed or currently not restartable returns:

```text
HTTP 409 Conflict
Content-Type: application/problem+json
```

Example response:

```json
{
  "type": "about:blank",
  "title": "Annual export conflict",
  "status": 409,
  "detail": "Annual export for year 2022 is already running, completed or cannot currently be restarted",
  "instance": "/api/exports/yearly"
}
```

> The CSV export is an application-level data archive. It is not a physical PostgreSQL backup and does not replace database backup and recovery procedures.

---

## Export Configuration

The annual export is configured through environment variables.

| Variable | Default | Description |
|---|---:|---|
| `EXPORT_ENABLED` | `true` | Enables or disables the annual scheduler |
| `EXPORT_OUTPUT_DIR` | `exports` | Base directory for generated CSV files |
| `EXPORT_CHUNK_SIZE` | `100` | Records processed per transaction |
| `EXPORT_CRON` | `0 0 2 1 1 *` | Scheduler cron expression |
| `EXPORT_ZONE` | `Europe/Berlin` | Scheduler and export time zone |

Default cron expression:

```text
0 0 2 1 1 *
```

Meaning:

```text
Second:       0
Minute:       0
Hour:         2
Day of month: 1
Month:        January
Day of week:  any
```

When the backend runs through Docker Compose, `/app/exports` is mapped to the local project directory `./exports`:

```yaml
volumes:
  - ./exports:/app/exports
```

Recommended Docker value:

```dotenv
EXPORT_OUTPUT_DIR=/app/exports
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

The backend exposes application and custom ingestion metrics through the Prometheus endpoint.

Prometheus collects these metrics and Grafana visualizes the current backend and ingestion state.

### Grafana Dashboard

![Grafana Dashboard](docs/images/grafana-dashboard.png)

---

## API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

---

## Running with Docker Compose

### Requirements

- Docker Desktop or Docker Engine
- Docker Compose
- Git

### Configuration

Copy the example environment file.

#### Windows PowerShell

```powershell
Copy-Item .env.example .env
```

#### Linux or macOS

```bash
cp .env.example .env
```

Replace the placeholder credentials in the local `.env` file.

Keep local credentials in `.env`. This file is excluded from Git.

For Docker-based exports, use:

```dotenv
EXPORT_OUTPUT_DIR=/app/exports
```

### Build and Start

```bash
docker compose up --build -d
```

### Service Status

```bash
docker compose ps
```

### Backend Logs

```bash
docker compose logs backend --tail=200
```

### Stop

```bash
docker compose down
```

### Stop and Remove PostgreSQL Data

```bash
docker compose down -v
```

> Removing the PostgreSQL volume deletes the local database data used by Docker Compose.

### Service URLs

```text
Backend API:  http://localhost:8080
Swagger UI:   http://localhost:8080/swagger-ui.html
Prometheus:   http://localhost:19090
Grafana:      http://localhost:13000
Mosquitto:    localhost:1884
PostgreSQL:   localhost:5432
```

Default Grafana development login:

```text
Username: admin
Password: admin
```

The default Grafana credentials are intended only for local development.

---

## Local Backend Development

The Maven wrapper is included in the `backend` directory.

### Windows

```powershell
cd backend
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

### Linux or macOS

```bash
cd backend
./mvnw clean test
./mvnw spring-boot:run
```

A locally started backend requires reachable PostgreSQL and MQTT services matching the configured environment variables.

---

## Database Migrations

Flyway manages the database schema.

Current migrations:

```text
V1  Application tables
    - devices
    - telemetry_records
    - events
    - health_records

V2  Spring Batch metadata tables and sequences
```

Hibernate validates the existing database schema and does not create or modify the production schema automatically.

---

## Testing

The project contains unit and integration tests for the main backend workflows.

### Application Context Tests

- Spring Boot application startup
- Configuration binding
- Flyway migrations
- PostgreSQL connectivity

### Repository Integration Tests

- Telemetry persistence
- PostgreSQL interaction
- Database queries

### MQTT Ingestion Integration Tests

- Telemetry ingestion
- Event ingestion
- Health ingestion
- Automatic device registration
- Persistence verification

### REST API Tests

- Telemetry endpoints
- Pagination
- Time-range filtering
- Event endpoints
- Health endpoints
- Export validation
- HTTP 400 error responses
- HTTP 409 conflict responses

### Export Tests

- Export file and directory management
- CSV escaping
- Spreadsheet-formula protection
- Export job parameter validation
- Spring Batch conflict translation
- Scheduler calculation of the previous year
- Scheduler error handling

### Run Tests

#### Windows

```powershell
cd backend
.\mvnw.cmd clean test
```

#### Linux or macOS

```bash
cd backend
./mvnw clean test
```

---

## Continuous Integration

GitHub Actions automatically runs the test suite for pushes and pull requests targeting the main branch.

The pipeline validates:

- Maven build
- Spring Boot application context
- Unit tests
- Integration tests
- PostgreSQL Testcontainers
- MQTT ingestion
- REST API behavior
- Prometheus metrics

---

## Project Structure

```text
industrial-monitoring-backend/
├── backend/
│   ├── src/main/java/com/example/industrialmonitoring/
│   │   ├── batch/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── export/
│   │   ├── mapper/
│   │   ├── mqtt/
│   │   ├── repository/
│   │   ├── scheduler/
│   │   └── service/
│   ├── src/main/resources/
│   │   └── db/migration/
│   ├── src/test/
│   ├── Dockerfile
│   └── pom.xml
├── docker/
│   └── mosquitto/
├── monitoring/
│   └── prometheus/
├── docs/
│   └── images/
│       └── grafana-dashboard.png
├── .env.example
├── docker-compose.yml
└── README.md
```

---

## Current Capabilities

- End-to-end CODESYS-to-backend MQTT pipeline
- Telemetry, event and health ingestion
- Automatic device registration
- PostgreSQL persistence
- Versioned database migrations
- REST access to stored operational data
- Pagination and time-range filtering
- OpenAPI documentation
- Application health and metrics endpoints
- Prometheus and Grafana monitoring
- Scheduled annual CSV archiving
- Manual annual export execution
- Persistent Spring Batch metadata
- Duplicate export protection
- Staging and publication workflow
- Docker-based deployment
- Automated testing
- Continuous integration
