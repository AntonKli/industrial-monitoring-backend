# Export Documentation

This document describes the Spring Batch export workflow, date handling, API behavior, validation, error responses and configuration used by the Industrial Monitoring Platform.

[Back to the main README](../README.md)

---

## Overview

The backend creates CSV exports for stored telemetry, event and health records.

Two export modes are available:

* Scheduled or manually triggered exports of completed calendar years
* Manually triggered exports for selected date ranges

Both modes use the same Spring Batch job, readers, writers, staging workflow and publication process.

For browser downloads, the backend streams the three completed CSV files as a ZIP archive. If a completed export already exists for the selected period, the download endpoint reuses it instead of starting the batch job again.

> These exports do not replace PostgreSQL backup and recovery procedures.

---

## Authentication and Authorization

All export endpoints require a valid Keycloak access token.

```text
Required realm role: OPERATOR
```

The configured `monitoring-operator` and `monitoring-admin` users have this role.

A user who is not authenticated receives:

```text
401 Unauthorized
```

An authenticated user without the `OPERATOR` role receives:

```text
403 Forbidden
```

For example, `monitoring-viewer` can access monitoring data but cannot start or download exports.

---

## Export API

```text
POST /api/exports/yearly?year={year}
POST /api/exports/range?from={from}&to={to}
POST /api/exports/range/download?from={from}&to={to}
```

The `to` parameter is always interpreted as an exclusive end date.

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

Using an exclusive end date avoids end-of-day values such as:

```text
23:59:59.999999
```

### Angular Date Handling

The Angular frontend presents both selected dates as inclusive.

Before sending the request, it adds one calendar day to the selected end date.

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

Only completed calendar years can be started through the yearly endpoint.

Current-year data can be exported through the range endpoint.

---

## Flexible Range Export

A manual range export can use any valid date range, including ranges that cross calendar-year boundaries.

Valid ranges must meet these conditions:

* `from` must be before `to`
* `from` must not be earlier than `2000-01-01`
* `to` must not be later than the next calendar day in the configured export time zone

Allowing the following day as the exclusive end date makes it possible to export the complete current day.

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

The step names originate from the initial annual-export implementation. The same steps are also used for flexible date ranges.

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

Files are first written to a staging directory:

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

An export is not moved out of the staging directory until all three CSV steps have completed successfully.

The generated `exports` directory is excluded from Git.

---

## Chunk Processing

Records are read and written in configurable chunks instead of loading an entire export period into memory.

The chunk size is configured through:

```text
EXPORT_CHUNK_SIZE
```

Each chunk is processed within a Spring Batch transaction.

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

The ZIP download endpoint first checks whether the final export directory already exists. If it does, the existing CSV files are reused.

A partial current-year export does not block a later complete yearly export because the periods use different identifying parameters.

---

## Access Token in Command-Line Examples

The following examples assume that a valid Keycloak access token has already been obtained.

PowerShell:

```powershell
$token = "ACCESS_TOKEN"
```

Linux or macOS:

```bash
export TOKEN="ACCESS_TOKEN"
```

The Angular application obtains its access token through Authorization Code Flow with PKCE. Direct username-and-password token requests are not enabled for the frontend client.

---

## Manual Yearly Export

Only completed calendar years are accepted.

### PowerShell

```powershell
Invoke-RestMethod `
  -Method Post `
  -Headers @{
    Authorization = "Bearer $token"
  } `
  -Uri "http://localhost:8080/api/exports/yearly?year=2025" |
  ConvertTo-Json
```

### curl

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
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
  -Headers @{
    Authorization = "Bearer $token"
  } `
  -Uri "http://localhost:8080/api/exports/range?from=2025-10-01&to=2026-04-01" |
  ConvertTo-Json
```

### curl

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
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

The ZIP endpoint either reuses an existing completed export or starts the batch job before streaming the archive.

### PowerShell

```powershell
curl.exe -X POST `
  -H "Authorization: Bearer $token" `
  "http://localhost:8080/api/exports/range/download?from=2026-07-16&to=2026-07-17" `
  -o "monitoring-export-2026-07-16_to_2026-07-16.zip"
```

### curl

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/exports/range/download?from=2026-07-16&to=2026-07-17" \
  --output "monitoring-export-2026-07-16_to_2026-07-16.zip"
```

Example archive content:

```text
events-export-2026-07-16_to_2026-07-17.csv
health-export-2026-07-16_to_2026-07-17.csv
telemetry-export-2026-07-16_to_2026-07-17.csv
```

The downloaded ZIP name uses the inclusive period shown to the user:

```text
monitoring-export-2026-07-16_to_2026-07-16.zip
```

The CSV filenames currently use the backend's exclusive end date:

```text
2026-07-16_to_2026-07-17
```

This difference affects the displayed filenames only. The exported data uses the same half-open period in both cases.

---

## Error Responses

### Missing or Invalid Access Token

A request without a valid access token returns HTTP `401 Unauthorized`.

```text
WWW-Authenticate: Bearer
```

### Missing Operator Role

An authenticated user without the `OPERATOR` role receives HTTP `403 Forbidden`.

Example header:

```text
Bearer error="insufficient_scope"
```

### Invalid Export Year

A year before 2000 or later than the most recently completed calendar year returns HTTP `400 Bad Request`.

Example for a request made during 2026:

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

The yearly and range job-start endpoints can return HTTP `409 Conflict` when the same job instance is already running, already completed or cannot currently be restarted.

```json
{
  "type": "about:blank",
  "title": "Export conflict",
  "status": 409,
  "detail": "Export for period 2022 is already running, completed or cannot currently be restarted",
  "instance": "/api/exports/yearly"
}
```

The ZIP download endpoint handles completed exports differently: it reuses the existing final directory instead of starting the same job again.

---

## Configuration

| Variable            |         Default | Description                              |
| ------------------- | --------------: | ---------------------------------------- |
| `EXPORT_ENABLED`    |          `true` | Enables or disables the annual scheduler |
| `EXPORT_OUTPUT_DIR` |       `exports` | Base directory for generated CSV files   |
| `EXPORT_CHUNK_SIZE` |           `100` | Records processed per transaction        |
| `EXPORT_CRON`       |   `0 0 2 1 1 *` | Scheduler cron expression                |
| `EXPORT_ZONE`       | `Europe/Berlin` | Scheduler and export time zone           |

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

The backend test suite includes coverage for:

* Annual and flexible export-period calculation
* Date-range validation
* Export file and directory management
* CSV escaping and spreadsheet-formula protection
* Spring Batch identifying parameters
* Duplicate and conflict handling
* Yearly scheduler calculation
* Scheduler error handling
* REST responses for successful, invalid and conflicting exports
