# Export Documentation

This document describes the Spring Batch export workflow, date handling, API endpoints, validation, error responses and configuration used by the Industrial Monitoring Platform.

[Back to the main README](../README.md)

---

## Overview

The backend creates application-level CSV archives for stored telemetry, event and health records.

Two export modes are available:

- Scheduled or manually triggered exports of completed calendar years
- Manually triggered exports for freely selected date ranges

Both modes use the same Spring Batch job, readers, writers, staging workflow and publication process.

For browser downloads, the backend streams the three completed CSV files as a ZIP archive. Existing completed exports are reused, allowing the same period to be downloaded repeatedly without starting the same batch job again.

> These exports do not replace physical PostgreSQL backup and recovery procedures.

---

## Export API

```text
POST /api/exports/yearly?year={year}
POST /api/exports/range?from={from}&to={to}
POST /api/exports/range/download?from={from}&to={to}
```

---

## Date Semantics

The backend uses half-open date ranges:

```text
createdAt >= from
createdAt <  to
```

The `from` date is inclusive and the `to` date is exclusive.

Example:

```text
from: 2025-10-01
to:   2026-04-01
```

This includes all records from October 1, 2025 through March 31, 2026. Records from April 1, 2026 are not included.

Using an exclusive end date avoids ambiguous values such as `23:59:59.999999`.

### Angular Date Handling

The Angular frontend presents both selected dates as inclusive. It adds one calendar day to the selected end date before sending the request to the backend.

Example user selection:

```text
From: 2026-07-16
To:   2026-07-16
```

Backend request:

```text
from=2026-07-16
to=2026-07-17
```

This includes the complete day of July 16, 2026.

---

## Scheduled Annual Export

The default scheduler runs every January 1 at 02:00 in the `Europe/Berlin` time zone and exports the previous completed calendar year.

Example:

```text
Scheduler execution: January 1, 2027 at 02:00
Export range:        2026-01-01 until 2027-01-01
```

The selected records satisfy:

```text
createdAt >= 2026-01-01T00:00:00
createdAt <  2027-01-01T00:00:00
```

Only completed calendar years can be started through the yearly endpoint. Current-year data can be exported through the range endpoint.

---

## Flexible Range Export

A manual range export can use any valid date range, including ranges that cross calendar-year boundaries.

Valid ranges must meet these conditions:

- `from` must be before `to`
- `from` must not be earlier than `2000-01-01`
- `to` must not be later than the next calendar day in the configured export time zone

Allowing the next day as the exclusive end makes it possible to export the complete current day.

---

## Batch Job Flow

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

### `prepareAnnualExportStep`

Creates the staging directory for the selected export period.

### `telemetryExportStep`

Reads telemetry records from PostgreSQL in configurable chunks and writes them to CSV.

### `eventExportStep`

Reads event records from PostgreSQL in configurable chunks and writes them to CSV.

### `healthExportStep`

Reads health records from PostgreSQL in configurable chunks and writes them to CSV.

### `finalizeAnnualExportStep`

Publishes the export only after every previous step has completed successfully.

---

## Staging and Publication

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

A complete calendar-year export uses the shorter year-based name:

```text
exports/
└── 2025/
    ├── telemetry-export-2025.csv
    ├── events-export-2025.csv
    └── health-export-2025.csv
```

Incomplete exports are never exposed as completed archives. The generated `exports` directory is excluded from Git.

---

## Chunk Processing

Records are read and written in configurable chunks instead of loading an entire export period into memory.

The chunk size is configured through:

```text
EXPORT_CHUNK_SIZE
```

---

## Spring Batch Metadata and Idempotency

Spring Batch stores job, execution and step metadata in PostgreSQL.

The identifying job parameters are:

```text
fromDate
toDateExclusive
zoneId
```

Starting the same period again therefore refers to the same Spring Batch job instance.

The ZIP download endpoint first checks whether a completed export already exists. When available, it reuses the published files instead of starting the same completed job again.

A partial current-year export does not block a later complete yearly export because the two exports use different date ranges.

---

## Manual Yearly Export

Only completed calendar years are accepted.

### PowerShell

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/exports/yearly?year=2025" |
  ConvertTo-Json
```

### curl

```bash
curl -X POST \
  "http://localhost:8080/api/exports/yearly?year=2025"
```

### Successful Response

```json
{
  "executionId": 1,
  "jobName": "annualMonitoringExportJob",
  "year": 2025,
  "status": "COMPLETED"
}
```

---

## Manual Range Export

### PowerShell

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/exports/range?from=2025-10-01&to=2026-04-01" |
  ConvertTo-Json
```

### curl

```bash
curl -X POST \
  "http://localhost:8080/api/exports/range?from=2025-10-01&to=2026-04-01"
```

### Successful Response

```json
{
  "executionId": 2,
  "jobName": "annualMonitoringExportJob",
  "fromDate": "2025-10-01",
  "toDateExclusive": "2026-04-01",
  "status": "COMPLETED"
}
```

---

## ZIP Download

The ZIP endpoint either reuses an existing completed export or creates the selected range before streaming the archive.

### PowerShell

```powershell
curl.exe -X POST `
  "http://localhost:8080/api/exports/range/download?from=2026-07-16&to=2026-07-17" `
  -o "monitoring-export-2026-07-16_to_2026-07-16.zip"
```

### curl

```bash
curl -X POST \
  "http://localhost:8080/api/exports/range/download?from=2026-07-16&to=2026-07-17" \
  --output "monitoring-export-2026-07-16_to_2026-07-16.zip"
```

Example archive content:

```text
events-export-2026-07-16_to_2026-07-17.csv
health-export-2026-07-16_to_2026-07-17.csv
telemetry-export-2026-07-16_to_2026-07-17.csv
```

The archive name uses the inclusive period visible to the user. The internal CSV names retain the backend's exclusive end date.

---

## Error Responses

### Invalid Export Year

A year before 2000 or later than the most recently completed calendar year returns HTTP `400 Bad Request`.

Example response when the current year is 2026:

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

---

## Configuration

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

When the backend runs through Docker Compose, `/app/exports` is mapped to the local `./exports` directory:

```yaml
volumes:
  - ./exports:/app/exports
```

Docker environment value:

```dotenv
EXPORT_OUTPUT_DIR=/app/exports
```

---

## Export Test Coverage

The automated backend tests cover:

- Annual and flexible export-period calculation
- Date-range validation
- Export file and directory management
- CSV escaping and spreadsheet-formula protection
- Spring Batch identifying parameters
- Duplicate and conflict handling
- Yearly scheduler calculation
- Scheduler error handling
- REST responses for successful, invalid and conflicting exports
