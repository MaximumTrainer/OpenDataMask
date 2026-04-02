# OpenDataMask

An open source reimplementation of [tonic-structural](https://www.tonic.ai/products/tonic-structural) — a data masking and anonymization platform.

## Overview

OpenDataMask connects to your source databases, applies configurable masking/generation rules column-by-column, and writes realistic anonymized data to a destination. It ships with a web UI, a full REST API, and a CLI tool (`odm`).

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌──────────────────────┐
│  Vue.js UI  │────▶│  Spring Boot │────▶│ PostgreSQL (app DB)  │
│ (TypeScript)│     │  Backend     │     └──────────────────────┘
└─────────────┘     │  (Kotlin)    │
                    │              │────▶ Source DB (PostgreSQL /
┌─────────────┐     │              │      MongoDB / Azure SQL /
│  Go CLI     │────▶│              │      MySQL / File)
│   (odm)     │     └──────────────┘
└─────────────┘
```

## Features

### Data Connectors
- **PostgreSQL** — full read/write with batch INSERT, schema introspection, FK discovery
- **MongoDB / MongoDB on Cosmos** — document read/write via sync driver
- **Azure SQL** — MSSQL-compatible JDBC connector
- **MySQL** — backtick quoting, `SHOW TABLES/COLUMNS`, batch INSERT
- **File (CSV/JSON)** — AES-encrypted upload/download, multipart REST endpoints

### Data Masking & Generation
- **47 generator types** powered by [Datafaker](https://datafaker.net/): names, addresses, emails, phones, SSNs, credit cards, IBANs, IPs, VINs, medical codes, and more
- **Composite generators**: `PARTIAL_MASK`, `FORMAT_PRESERVING`, `CONDITIONAL`, `SEQUENTIAL`
- **Deterministic consistency**: HMAC-SHA256 seeding ensures same input → same output across runs; linked columns share a Faker instance
- **Generator presets**: 24 built-in system presets; workspace-level custom presets; one-click apply

### Sensitivity & Privacy
- **Sensitivity scanning**: 19 PII detection rules (column name + value pattern matching) with `FULL`/`HIGH`/`MEDIUM` confidence; daily scheduled scan
- **Privacy Hub**: AT_RISK / PROTECTED / NOT_SENSITIVE column classification, bulk recommendation apply
- **Privacy reports**: downloadable current-config and per-job JSON reports; auto-generated on job completion
- **Column comments**: per-column discussion threads with email notification

### Job Orchestration
- **Table modes**: `PASSTHROUGH`, `MASK`, `GENERATE`, `SUBSET`, `UPSERT`, `SKIP`
- **Full write pipeline**: destination schema mirroring, type mapping (PostgreSQL ↔ Azure SQL ↔ MySQL ↔ MongoDB), batch writes
- **Subsetting with referential integrity**: FK graph traversal (real + virtual FKs), topological execution order, circular-dependency handling
- **Job scheduling**: cron-based scheduler with `FULL_GENERATION` / `UPSERT` modes, next-run tracking
- **Webhooks**: custom HTTP POST and GitHub Actions workflow dispatch; template variable substitution; per-job and per-schema-change triggers
- **Post-job actions**: Webhook, Email, Script

### Schema Management
- **Schema change detection**: background scan every 2 hours; detects new/dropped tables, new/dropped columns, type/nullability changes
- **Schema change resolution API**: resolve exposing changes, dismiss notifications, per-workspace `BLOCK_ALL` / `BLOCK_EXPOSING` / `NEVER_BLOCK` policy
- **Workspace configuration export/import**: portable JSON snapshots (table configs, column generators, subset settings)
- **Workspace tags**: tag-based filtering on the workspace list
- **Workspace inheritance**: parent → child config propagation; child can override any generator; sync-with-parent

### Access Control
- **JWT authentication** with BCrypt-hashed passwords
- **Workspace roles**: Admin, User, Viewer
- **Fine-grained permissions**: 9 permission types (`CONFIGURE_GENERATORS`, `PREVIEW_SOURCE_DATA`, `PREVIEW_DESTINATION_DATA`, `CONFIGURE_SUBSETTING`, `CONFIGURE_SENSITIVITY`, `CONFIGURE_POST_JOB_ACTIONS`, `CONFIGURE_SCHEMA_CHANGE_SETTINGS`, `RESOLVE_SCHEMA_CHANGES`, `RUN_JOBS`); role defaults + custom per-user overrides
- **Startup security validator**: refuses to start if `JWT_SECRET` or `ENCRYPTION_KEY` are unset/insecure

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Backend   | Kotlin + Spring Boot 3.2 |
| Frontend  | TypeScript + Vue 3 + Vite |
| CLI       | Go (cobra) |
| App DB    | PostgreSQL |
| Test DB   | H2 (PostgreSQL mode, in-memory) |
| Tests     | JUnit 5 + Mockito + MockMvc (400+ tests) |
| Deploy    | Docker + Docker Compose |

## Quick Start

### With Docker Compose

```bash
docker-compose up -d
```

Then open http://localhost in your browser.

Default API is available at http://localhost:8080/api.

### Development

**Backend:**
```bash
cd backend
./gradlew bootRun
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

**CLI:**
```bash
cd cli
go build -o odm .
./odm auth login --username admin --password secret
```

## CLI Usage

```bash
# Authenticate
odm auth login --username admin --password secret

# List workspaces
odm workspace list

# Create a workspace
odm workspace create --name "My Workspace" --description "Production data masking"

# Add a data connection
odm connection create <workspace-id> \
  --name "prod-db" \
  --type POSTGRESQL \
  --connection-string "host=localhost port=5432 dbname=mydb" \
  --username myuser \
  --password mypassword

# Run a masking job
odm job run <workspace-id>

# View job logs
odm job logs <workspace-id> <job-id>
```

## API Reference

The REST API is available at `http://localhost:8080/api`. Key endpoints:

### Auth
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | Authenticate and get JWT token |
| POST | `/api/auth/register` | Register a new user |

### Workspaces
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/workspaces` | List workspaces (supports `?tag=` filter) |
| POST | `/api/workspaces` | Create workspace |
| GET | `/api/workspaces/{id}/tags` | List workspace tags |
| POST | `/api/workspaces/{id}/tags` | Add tag |
| DELETE | `/api/workspaces/{id}/tags/{tag}` | Remove tag |
| GET | `/api/workspaces/{id}/export` | Export workspace config as JSON |
| POST | `/api/workspaces/{id}/import` | Import workspace config |
| GET | `/api/workspaces/{id}/children` | List child workspaces |
| POST | `/api/workspaces/{id}/children` | Create child workspace |
| POST | `/api/workspaces/{id}/inherit/{parentId}` | Inherit config from parent |
| POST | `/api/workspaces/{id}/sync-parent` | Re-sync with parent |
| GET | `/api/workspaces/{id}/inherited-configs` | List inherited configs |
| POST | `/api/workspaces/{id}/inherited-configs/{configId}/override` | Mark as overridden |
| GET | `/api/workspaces/{id}/schedules` | List job schedules |
| POST | `/api/workspaces/{id}/schedules` | Create schedule (cron) |
| PUT | `/api/workspaces/{id}/schedules/{sid}` | Update schedule |
| DELETE | `/api/workspaces/{id}/schedules/{sid}` | Delete schedule |

### Connections & Tables
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/workspaces/{id}/connections` | List data connections |
| POST | `/api/workspaces/{id}/connections` | Create data connection |
| POST | `/api/workspaces/{id}/connections/{cid}/test` | Test connection |
| GET | `/api/workspaces/{id}/tables` | List table configurations |
| POST | `/api/workspaces/{id}/tables` | Create table configuration |
| GET | `/api/workspaces/{id}/tables/{table}/columns/{col}/preview` | Preview original vs masked values |
| GET | `/api/workspaces/{id}/tables/{table}/columns/{col}/comments` | List column comments |
| POST | `/api/workspaces/{id}/tables/{table}/columns/{col}/comments` | Add comment |

### Jobs
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/workspaces/{id}/jobs` | Start a masking job |
| GET | `/api/workspaces/{id}/jobs/{jid}/logs` | Get job logs |
| GET | `/api/workspaces/{id}/jobs/{jid}/privacy-report` | Per-job privacy report |

### Files (FILE connector)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/workspaces/{id}/files` | Upload source/destination file |
| GET | `/api/workspaces/{id}/files` | List workspace files |
| GET | `/api/workspaces/{id}/files/{fid}` | Download file |
| DELETE | `/api/workspaces/{id}/files/{fid}` | Delete file |

### Sensitivity & Privacy
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/workspaces/{id}/sensitivity-scan/run` | Trigger sensitivity scan |
| GET | `/api/workspaces/{id}/sensitivity-scan/status` | Scan status |
| GET | `/api/workspaces/{id}/sensitivity-scan/log` | Scan log with per-column entries |
| GET | `/api/workspaces/{id}/sensitivity-scan/log/download` | Download scan log |
| GET | `/api/workspaces/{id}/privacy-hub` | AT_RISK / PROTECTED / NOT_SENSITIVE summary |
| GET | `/api/workspaces/{id}/privacy-hub/recommendations` | Unprotected sensitive columns |
| POST | `/api/workspaces/{id}/privacy-hub/recommendations/apply` | Bulk-apply recommended generators |
| GET | `/api/workspaces/{id}/privacy-report` | Current-config privacy report |
| GET | `/api/workspaces/{id}/privacy-report/download` | Download as JSON file |

### Schema Changes & Webhooks
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/workspaces/{id}/schema-changes` | List schema changes (exposing + notifications) |
| POST | `/api/workspaces/{id}/schema-changes/{cid}/resolve` | Resolve a change |
| POST | `/api/workspaces/{id}/schema-changes/{cid}/dismiss` | Dismiss a change |
| POST | `/api/workspaces/{id}/schema-changes/resolve-all` | Resolve all exposing changes |
| POST | `/api/workspaces/{id}/schema-changes/dismiss-all` | Dismiss all notifications |
| PATCH | `/api/workspaces/{id}/settings/schema` | Update schema-change handling policy |
| GET | `/api/workspaces/{id}/webhooks` | List webhooks |
| POST | `/api/workspaces/{id}/webhooks` | Create webhook |
| POST | `/api/workspaces/{id}/webhooks/{wid}/test` | Test webhook |

### Generator Presets & Permissions
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/generator-presets` | List system presets |
| GET | `/api/workspaces/{id}/generator-presets` | List workspace presets |
| POST | `/api/workspaces/{id}/generator-presets` | Create preset |
| GET | `/api/workspaces/{id}/users/{uid}/permissions` | Get effective permissions |
| PUT | `/api/workspaces/{id}/users/{uid}/permissions` | Update permission overrides |
| DELETE | `/api/workspaces/{id}/users/{uid}/permissions` | Reset to role defaults |

## Configuration

Configure via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/opendatamask` | App database URL |
| `DATABASE_USERNAME` | `opendatamask` | App database username |
| `DATABASE_PASSWORD` | `opendatamask` | App database password |
| `JWT_SECRET` | *(required — no default)* | JWT signing secret (generate with `openssl rand -base64 32`) |
| `ENCRYPTION_KEY` | *(required — no default)* | AES key for encrypting connection passwords (generate with `openssl rand -base64 32`) |
| `SERVER_PORT` | `8080` | Backend server port |

## Security Notes

> ⚠️ **The application will refuse to start if `JWT_SECRET` or `ENCRYPTION_KEY` are not set or are insecure defaults.**

### Required Environment Variables

You **must** set the following before running in any non-test environment:

| Variable | Description | How to generate |
|----------|-------------|-----------------|
| `JWT_SECRET` | JWT signing secret (min 256 bits) | `openssl rand -base64 32` |
| `ENCRYPTION_KEY` | AES key for encrypting connection passwords | `openssl rand -base64 32` |

Example:
```bash
export JWT_SECRET=$(openssl rand -base64 32)
export ENCRYPTION_KEY=$(openssl rand -base64 32)
```

### Other Security Practices

- Use HTTPS in production (the CLI supports `--insecure` only for development)
- Connection passwords are AES-encrypted before storage
- User passwords are BCrypt-hashed

## License

Open source — see [LICENSE](LICENSE) for details.

