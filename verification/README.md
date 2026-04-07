# OpenDataMask — Sandboxed Verification Environment

This directory contains a self-contained, Docker-based environment for
proving that OpenDataMask correctly masks sensitive PII data while preserving
referential integrity.

## What It Does

| Step | Description |
|------|-------------|
| **SOURCE_DB** | PostgreSQL database pre-seeded with 50 realistic user records (UUID PK, full_name, email, phone_number, date_of_birth, salary). |
| **TARGET_DB** | Empty PostgreSQL database that receives the masked data. |
| **Masking job** | OpenDataMask reads every row from SOURCE_DB, applies Datafaker-powered generators to all PII columns, and writes the anonymised rows to TARGET_DB — keeping the original UUID primary keys intact. |
| **Verification** | A Python script connects to both databases and validates: row counts, key persistence, masking effectiveness, and human-readability of the output. |

## Directory Layout

```
verification/
├── docker-compose.yml        # SOURCE_DB, TARGET_DB, app_db, backend, frontend
├── init/
│   └── source_db.sql         # DDL + 50 seed records for SOURCE_DB
├── run_verification.sh       # Full end-to-end orchestration script
├── verify.py                 # Python validation script
├── requirements.txt          # Python dependencies (psycopg2-binary)
└── README.md                 # This file
```

## Prerequisites

| Tool | Version |
|------|---------|
| Docker Engine | ≥ 24 |
| Docker Compose | v2 (`docker compose`) or v1 (`docker-compose`) |
| curl | any |
| Python 3 | ≥ 3.10 (must include `pip` module — standard in most distributions) |

## Quick Start

```bash
# Run from the repository root or the verification/ directory:
cd verification/
chmod +x run_verification.sh
./run_verification.sh
```

The script will:

1. Build the backend and frontend Docker images.
2. Start all services and wait for them to be healthy.
3. Register a user and authenticate with the OpenDataMask API.
4. Create a workspace, source & destination connections, table configuration,
   and per-column masking generators.
5. Trigger a masking job and poll until it completes.
6. Run `verify.py` and print a Verification Report.

## Running Only the Verification Script

If the environment is already running and the masking job has already completed:

```bash
python3 -m pip install -r requirements.txt
python3 verify.py
```

### JUnit XML Output

Both the orchestration script and the standalone script support a JUnit-compatible XML report (no external dependencies — uses stdlib `xml.etree.ElementTree`):

```bash
# Via the orchestration script (sets --junit-xml automatically):
VERIFY_JUNIT_XML=report.xml ./run_verification.sh

# Directly against an already-running environment:
python3 verify.py --junit-xml report.xml
```

The XML report contains one `<testcase>` per check. Skipped checks (e.g., Human Readability when masking didn't pass) are written as `<skipped/>` rather than `<failure/>` so CI tools count them correctly.

### Environment Variables (optional overrides)

| Variable | Default | Description |
|----------|---------|-------------|
| `SOURCE_DB_HOST` | `localhost` | Source DB hostname |
| `SOURCE_DB_PORT` | `5433` | Source DB port (host-mapped) |
| `SOURCE_DB_NAME` | `source_db` | Source DB database name |
| `SOURCE_DB_USER` | `source_user` | Source DB username |
| `SOURCE_DB_PASS` | `source_pass` | Source DB password |
| `TARGET_DB_HOST` | `localhost` | Target DB hostname |
| `TARGET_DB_PORT` | `5434` | Target DB port (host-mapped) |
| `TARGET_DB_NAME` | `target_db` | Target DB database name |
| `TARGET_DB_USER` | `target_user` | Target DB username |
| `TARGET_DB_PASS` | `target_pass` | Target DB password |
| `VERIFY_JUNIT_XML` | *(unset)* | If set, `run_verification.sh` writes a JUnit XML report to this path |

## Verification Checks

### 1 · Record Integrity
Confirms the row count in SOURCE_DB matches TARGET_DB (both should be **50**).

### 2 · Key Persistence
For every `id` (UUID) in SOURCE_DB, verifies the exact same `id` exists in
TARGET_DB. This proves the tool does **not** hash or alter primary keys.

### 3 · Masking Effectiveness
Compares `full_name` and `email` for every matching `id`. The check **passes**
only if:

```
source.id == target.id  AND
source.full_name != target.full_name  AND
source.email != target.email
```

### 4 · Human Readability
Prints a sample of 5 masked records (ordered by `id`, for deterministic output) so a human can visually confirm the output looks realistic (e.g., a real-looking name and a valid e-mail address rather than random strings like `asdfghjkl`).

The sample is only printed when Masking Effectiveness has already passed. If masking failed, this check is reported as **SKIP** (not FAIL) to avoid exposing potential real PII and to prevent it inflating the failure count in CI reports.

### Sample Report Output

```
════════════════════════════════════════════════════════════
  OpenDataMask — Verification Report
════════════════════════════════════════════════════════════

Connecting to SOURCE_DB (localhost:5433/source_db)…
Connecting to TARGET_DB (localhost:5434/target_db)…

  -- Masked Record Sample (TARGET_DB) ----------------------------------
  [1] id            : a1b2c3d4-0001-4000-8000-000000000001
       full_name     : Johnathan Mraz
       email         : cordell.okon@yahoo.com
       phone_number  : 1-541-388-3947
       date_of_birth : Mon Jan 15 00:00:00 UTC 1990
       salary        : 97432

------------------------------------------------------------
  Results
------------------------------------------------------------
  [✓] Record Integrity (row count matches): PASS
       Source row count : 50
       Target row count : 50
  [✓] Key Persistence (all source IDs present in target): PASS
       Source IDs : 50
       Target IDs : 50
  [✓] Masking Effectiveness (PII fields differ between source and target): PASS
       Rows compared : 50
       Name unchanged  (should be 0) : 0
       Email unchanged (should be 0) : 0
  [✓] Human Readability (sample of 5 masked records): PASS

============================================================
  OK  4/4 CHECKS PASSED
============================================================
```

When Masking Effectiveness fails the Human Readability check is skipped instead:

```
  [–] Human Readability (sample of 5 masked records): SKIP
       Sample skipped: masking effectiveness check did not pass. Printing TARGET_DB rows could expose real PII.

============================================================
  FAIL  1/4 CHECK(S) FAILED  (2 passed, 1 skipped)
============================================================
```

## Masking Rules Applied

| Column | Generator | Behaviour |
|--------|-----------|-----------|
| `id` | *(none — passthrough)* | UUID primary key is preserved exactly. |
| `full_name` | `FULL_NAME` | Replaced with a random realistic full name via Datafaker. |
| `email` | `EMAIL` | Replaced with a random realistic e-mail address. |
| `phone_number` | `PHONE` | Replaced with a random phone number. |
| `date_of_birth` | `BIRTH_DATE` | Replaced with a random birthday. |
| `salary` | `RANDOM_INT` (30 000–200 000) | Replaced with a random integer in range. |

## Tearing Down

```bash
cd verification/
docker compose -f docker-compose.yml down -v
```

The `-v` flag also removes the named volume (`app_db_data`) so the next run
starts with a clean OpenDataMask application database.

## GitHub Actions

The workflow `.github/workflows/sandbox-verification.yml` runs this full verification suite automatically on every push and pull request to `main`, and can be triggered on demand via `workflow_dispatch`.

It:

1. Builds the backend Docker image from source (with layer caching).
2. Starts `source_db`, `target_db`, `app_db`, and `backend` via `docker compose`.
3. Orchestrates the masking job through the REST API (register → login → workspace → connections → table config → generators → trigger → poll).
4. Runs `verify.py --junit-xml` to produce a structured test report.
5. Publishes the report as a **workflow check** via `dorny/test-reporter` (per-check annotations on PRs).
6. Uploads the JUnit XML as a **downloadable artifact** (`sandbox-verification-report`, 30-day retention).
7. Writes a **markdown job summary** with overall pass/fail status.
8. Always tears down the sandbox; collects Docker container logs on failure.
