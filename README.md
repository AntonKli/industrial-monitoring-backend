# Industrial Monitoring Backend

A Java 21 and Spring Boot 3.5.15 backend that receives telemetry, event and health data from a CODESYS edge gateway over MQTT, persists it in PostgreSQL, exposes REST and Prometheus endpoints, and creates scheduled or on-demand CSV exports with Spring Batch.

## Related Project

[Industrial Edge Gateway – CODESYS](https://github.com/AntonKli/industrial-edge-gateway-codesys)

---

## Overview

Industrial edge devices publish telemetry, event and health data through MQTT.

The backend subscribes to the configured MQTT topics, validates incoming messages, automatically registers devices and stores the received data in PostgreSQL.

The stored information is available through REST APIs and can be monitored with Spring Boot Actuator, Prometheus and Grafana.

A Spring Batch job creates CSV archives for telemetry, event and health records. Exports can cover a completed calendar year or a manually selected date range. The records are processed in configurable chunks and initially written to a staging directory. The completed export is published only after every batch step has finished successfully.

The end-to-end path from the CODESYS gateway through Eclipse Mosquitto into the backend was validated manually. The automated test suite focuses on ingestion services, persistence, REST controllers, scheduling and export behavior.

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
                               CSV Export
```

---

## Key Features

- MQTT ingestion for telemetry, event and health messages
- Integration with a CODESYS-based industrial edge gateway
- Automatic device registration
- PostgreSQL persistence with Spring Data JPA and Hibernate
- Database versioning with Flyway
- Telemetry endpoints with pagination and time-range filtering
- Device, event and health query endpoints
- OpenAPI documentation and Swagger UI
- Spring Boot Actuator endpoints
- Custom Prometheus ingestion metrics and Grafana monitoring
- Scheduled yearly and manual date-range CSV exports, including cross-year ranges
- Chunk-based Spring Batch processing with persistent metadata and duplicate protection
- Staging-to-publication workflow that exposes only completed exports
- Reproducible local development stack with Docker Compose
- Automated tests and continuous integration with GitHub Actions

---

## Technology Stack

### Backend

- Java 21
- Spring Boot 3.5.15
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

### Export APIs

```text
POST /api/exports/yearly?year={year}
POST /api/exports/range?from={from}&to={to}
```

---

## CSV Exports with Spring Batch

The backend uses Spring Batch to create application-level CSV archives of stored telemetry, event and health records.

Two export modes are available:

- Scheduled or manually triggered exports of completed calendar years
- Manually triggered exports for freely selected date ranges

Both modes use the same Spring Batch job, readers, writers, staging workflow and publication process.

### Scheduled Annual Export

The default scheduler runs every January 1 at 02:00 in the `Europe/Berlin` time zone and exports the complete previous calendar year.

Example:

```text
Scheduler execution: January 1, 2027 at 02:00
Export range:        2026-01-01 until 2027-01-01
```

The end date is exclusive.

The selected records therefore satisfy:

```text
createdAt >= 2026-01-01T00:00:00
createdAt <  2027-01-01T00:00:00
```

Only completed calendar years can be started through the yearly REST endpoint. Data from the current year can instead be exported through the range endpoint.

### Flexible Range Export

A manual export can use any valid date range, including a range that crosses calendar-year boundaries.

Example:

```text
from: 2025-10-01
to:   2026-04-01
```

This exports records from October 1, 2025 up to, but not including, April 1, 2026.

The range uses half-open interval semantics:

```text
createdAt >= from
createdAt <  to
```

Using an exclusive end date avoids ambiguous values such as `23:59:59.999999`.

Valid ranges must meet the following conditions:

- `from` must be before `to`
- `from` must not be earlier than `2000-01-01`
- `to` must not be later than the next calendar day in the configured export time zone

Allowing the next day as the exclusive end makes it possible to include the complete current day.

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

#### `prepareAnnualExportStep`

Creates the staging directory for the selected export period.

#### `telemetryExportStep`

Reads telemetry records from PostgreSQL in configurable chunks and writes them to CSV.

#### `eventExportStep`

Reads event records from PostgreSQL in configurable chunks and writes them to CSV.

#### `healthExportStep`

Reads health records from PostgreSQL in configurable chunks and writes them to CSV.

#### `finalizeAnnualExportStep`

Publishes the export only after every previous step has completed successfully.

### Staging and Publication

Files are initially written to a staging directory:

```text
exports/
└── .staging/
    └── 2025-10-01_to_2026-04-01/
        ├── telemetry-export-2025-10-01_to_2026-04-01.csv
        ├── events-export-2025-10-01_to_2026-04-01.csv
        └── health-export-2025-10-01_to_2026-04-01.csv
```

After all batch steps complete successfully, the directory is published:

```text
exports/
└── 2025-10-01_to_2026-04-01/
    ├── telemetry-export-2025-10-01_to_2026-04-01.csv
    ├── events-export-2025-10-01_to_2026-04-01.csv
    └── health-export-2025-10-01_to_2026-04-01.csv
```

A complete calendar-year export keeps the shorter year-based name:

```text
exports/
└── 2025/
    ├── telemetry-export-2025.csv
    ├── events-export-2025.csv
    └── health-export-2025.csv
```

Incomplete exports are never exposed as completed archives.

The generated `exports` directory is excluded from Git.

### Chunk-Based Processing

Records are read and written in configurable chunks instead of loading an entire export period into memory.

The chunk size can be configured through:

```text
EXPORT_CHUNK_SIZE
```

### Spring Batch Metadata and Idempotency

Spring Batch stores job, execution and step metadata in PostgreSQL.

The identifying job parameters are:

```text
fromDate
toDateExclusive
zoneId
```

Starting the same period again therefore refers to the same Spring Batch job instance.

A partial current-year export does not block a later complete yearly export because the two exports use different date ranges.

### Manual Yearly Export

Only completed calendar years are accepted.

#### PowerShell

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/exports/yearly?year=2025" |
  ConvertTo-Json
```

#### curl

```bash
curl -X POST \
  "http://localhost:8080/api/exports/yearly?year=2025"
```

#### Successful Response

```json
{
  "executionId": 1,
  "jobName": "annualMonitoringExportJob",
  "year": 2025,
  "status": "COMPLETED"
}
```

### Manual Range Export

#### PowerShell

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/exports/range?from=2025-10-01&to=2026-04-01" |
  ConvertTo-Json
```

#### curl

```bash
curl -X POST \
  "http://localhost:8080/api/exports/range?from=2025-10-01&to=2026-04-01"
```

#### Successful Response

```json
{
  "executionId": 2,
  "jobName": "annualMonitoringExportJob",
  "fromDate": "2025-10-01",
  "toDateExclusive": "2026-04-01",
  "status": "COMPLETED"
}
```

## Export Error Responses

### Invalid Export Year

A year before 2000 or later than the most recently completed calendar year returns HTTP `400 Bad Request`.

Example when the current year is 2026:

```json
{
  "type": "about:blank",
  "title": "Invalid export year",
  "status": 400,
  "detail": "Export year must be between 2000 and 2025. Use a range export for the current year.",
  "instance": "/api/exports/yearly"
}
```

### Invalid Export Period

An empty, reversed, pre-2000 or excessively future-dated range returns HTTP `400 Bad Request`.

```json
{
  "type": "about:blank",
  "title": "Invalid export period",
  "status": 400,
  "detail": "Export start date must be before the exclusive end date",
  "instance": "/api/exports/range"
}
```

### Export Conflict

A running, completed or currently non-restartable export with the same identifying period returns HTTP `409 Conflict`.

```json
{
  "type": "about:blank",
  "title": "Export conflict",
  "status": 409,
  "detail": "Export for period 2022 is already running, completed or cannot currently be restarted",
  "instance": "/api/exports/yearly"
}
```

> The CSV export is an application-level data archive. It is not a physical PostgreSQL backup and does not replace database backup and recovery procedures.

---

## Export Configuration

CSV export processing and the annual scheduler are configured through environment variables.

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

The application default for `EXPORT_OUTPUT_DIR` is `exports`. When the backend runs through Docker Compose, the directory is available as `/app/exports` and mapped to the local project directory `./exports`:

```yaml
volumes:
  - ./exports:/app/exports
```

Explicit Docker value:

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

Hibernate validates the Flyway-managed schema and does not create or update database tables automatically.

---

## Testing

The project contains unit and integration tests for the main backend workflows.

### Application Context Tests

- Spring Boot application startup
- Flyway migrations
- PostgreSQL connectivity

### Repository Integration Tests

- Telemetry persistence
- PostgreSQL interaction
- Database queries

### Ingestion Service Integration Tests

- Telemetry, event and health processing
- Automatic device registration
- PostgreSQL persistence verification

These automated tests invoke the ingestion services directly. They do not start Mosquitto or publish MQTT packets over the network.

### REST Controller Tests

- Telemetry, event and health endpoints
- Pagination and telemetry time-range filtering
- Yearly and flexible range export endpoints
- HTTP 400 error responses
- HTTP 409 conflict responses

### Export Tests

- Annual and flexible export-period calculation
- Export file and directory management
- CSV escaping and spreadsheet-formula protection
- Identifying Spring Batch job parameters
- Duplicate and conflict handling
- Range validation
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

GitHub Actions runs the Maven test suite for pushes and pull requests targeting the `main` branch.

The workflow compiles the backend and executes automated tests covering:

- Spring Boot application context startup
- Flyway and PostgreSQL Testcontainers integration
- Repository and ingestion-service behavior
- REST controller behavior
- Export-period, file-management and scheduler logic
- Validation and HTTP error handling

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