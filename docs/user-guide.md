# OpenDataMask – User & Setup Guide

## Introduction

**OpenDataMask** is an open-source data masking platform that helps developers and data engineers protect sensitive information (PII) by applying realistic fake-data generation, masking, and redaction strategies to datasets. It enables safe use of production-like data in development, testing, and analytics environments, supporting GDPR, CCPA, and HIPAA compliance requirements.

Core capabilities:
- **Multi-database support**: PostgreSQL, MySQL, MongoDB, Azure SQL, Azure Cosmos DB (MongoDB API), flat files
- **60+ generator types**: Names, emails, phones, SSNs, credit cards, addresses, medical IDs, financial data, and more
- **Workspace model**: Isolated masking configurations with role-based access control and inheritance
- **Privacy intelligence**: Automatic sensitive column detection, privacy hub dashboards, and compliance reports
- **Job scheduling**: Cron-based automated masking runs
- **Webhook integration**: Post-job notifications via custom HTTP webhooks or GitHub Actions triggers
- **REST API + CLI**: Full programmatic access and a Go-based CLI tool

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| JDK | 17+ | Temurin/OpenJDK recommended |
| Docker & Docker Compose | 20.10+ | Required for containerised deployment |
| PostgreSQL | 15+ | Production database backend |
| Node.js | 20+ | Required only for local frontend development |
| Go | 1.21+ | Required only for building the CLI from source |

---

## Quick Start (Docker Compose)

The fastest way to run OpenDataMask is with Docker Compose:

```bash
# Clone the repository
git clone https://github.com/MaximumTrainer/OpenDataMask.git
cd OpenDataMask

# Generate secrets
export JWT_SECRET=$(openssl rand -base64 32)
export ENCRYPTION_KEY=$(openssl rand -base64 24 | head -c 32)

# Start all services (PostgreSQL, backend, frontend)
docker-compose up -d

# Access the UI
open http://localhost
```

> **Default services**: Frontend on port 80, Backend API on port 8080, PostgreSQL on port 5432.

---

## Installation & Build from Source

### Backend (Kotlin / Spring Boot)

```bash
cd backend

# Build and test
./gradlew build --no-daemon

# Run locally (requires PostgreSQL)
export DATABASE_URL=jdbc:postgresql://localhost:5432/opendatamask
export DATABASE_USERNAME=opendatamask
export DATABASE_PASSWORD=opendatamask
export JWT_SECRET=<your-secret>
export ENCRYPTION_KEY=<your-16-char-key>

./gradlew bootRun --no-daemon
```

The backend starts on `http://localhost:8080`.

### Frontend (Vue 3 / TypeScript)

```bash
cd frontend

# Install dependencies
npm ci

# Development server (proxies API to localhost:8080)
npm run dev

# Production build
npm run build
```

### CLI (Go)

```bash
cd cli

# Build
go build -o opendatamask-cli ./...

# Run
./opendatamask-cli --help
```

---

## Configuration

### Environment Variables

| Variable | Required | Description | Example |
|---|---|---|---|
| `DATABASE_URL` | Yes | JDBC URL for the PostgreSQL backend database | `jdbc:postgresql://localhost:5432/opendatamask` |
| `DATABASE_USERNAME` | Yes | PostgreSQL username | `opendatamask` |
| `DATABASE_PASSWORD` | Yes | PostgreSQL password | `secret` |
| `JWT_SECRET` | Yes | Secret for signing JWT tokens (min 32 chars) | Output of `openssl rand -base64 32` |
| `ENCRYPTION_KEY` | Yes | Key for encrypting stored credentials (exactly 16 or 32 chars) | Output of `openssl rand -base64 32 \| head -c 32` |
| `SERVER_PORT` | No | Backend HTTP port | `8080` (default) |
| `JWT_EXPIRATION` | No | Token expiry in milliseconds | `86400000` (24 h, default) |
| `MONGODB_URI` | No | MongoDB URI when masking MongoDB sources | `mongodb://localhost:27017` |

### `application.yml` Reference

See `backend/src/main/resources/application.yml` for the full configuration template. Override any property via environment variable using Spring Boot's relaxed binding (e.g., `SPRING_DATASOURCE_URL` overrides `spring.datasource.url`).

---

## Core Concepts

### Workspaces

A **Workspace** is an isolated configuration scope. Each workspace has:
- One or more **Data Connections** (source and target databases)
- **Table Configurations** that define which tables to process and how
- **Column Generators** that specify what fake data to produce per column
- **Team members** with ADMIN or USER roles
- An optional **parent workspace** for configuration inheritance

### Masking Modes

| Mode | Description |
|---|---|
| `MASK` | Replace column values with generated fake data |
| `GENERATE` | Generate a completely new row set |
| `PASSTHROUGH` | Copy data without modification |
| `SUBSET` | Copy a filtered/sampled subset of rows |
| `SKIP` | Exclude the table from processing |

### Generator Types

OpenDataMask includes 60+ built-in generators. Key examples:

| Generator | Sample Output |
|---|---|
| `NAME` / `FULL_NAME` | `Sofia Martinez` |
| `EMAIL` | `j.smith@example.net` |
| `PHONE` | `+1-555-0147` |
| `SSN` | `123-45-6789` |
| `CREDIT_CARD` | `4111-1111-1111-1111` |
| `ADDRESS` | `742 Evergreen Terrace` |
| `IP_ADDRESS` | `192.168.1.42` |
| `ICD_CODE` | `J45.909` |
| `IBAN` | `GB29NWBK60161331926819` |
| `PARTIAL_MASK` | `J*** D***` (preserves format) |
| `NULL` | `null` |
| `CONSTANT` | Fixed value (configured in params) |

### Privacy Hub & Sensitivity Scanning

OpenDataMask automatically scans workspaces to detect sensitive columns using pattern matching and confidence scoring. The **Privacy Hub** provides:
- A summary of sensitive column coverage
- Actionable recommendations (e.g., "Add EMAIL generator to `users.email`")
- Exportable compliance reports (JSON)

---

## Operational Workflow

### Step-by-step: Mask a database

1. **Register / log in** at `http://localhost` (or API `/api/auth/register`)
2. **Create a Workspace** — give it a name and optional description
3. **Add a Source Connection** — provide host, port, database, username, password, and connection type
4. **Add a Target Connection** — the database that will receive masked data (can be the same database with a different schema)
5. **Import schema** — OpenDataMask discovers tables and columns automatically
6. **Configure tables** — set the masking mode for each table
7. **Configure columns** — assign a generator type to each sensitive column
8. **Run a Job** — trigger a masking job; monitor progress and logs in real time
9. **Review Privacy Report** — verify all sensitive columns are masked

---

## CLI Usage

The Go CLI provides quick access to common operations:

```bash
# Authenticate and save credentials locally
opendatamask-cli auth login --url http://localhost:8080 --username admin --password secret

# List workspaces
opendatamask-cli workspace list

# Get workspace details
opendatamask-cli workspace get <workspace-id>

# List jobs
opendatamask-cli job list --workspace <workspace-id>

# Trigger a masking job
opendatamask-cli job run --workspace <workspace-id>
```

Configuration is stored at `~/.opendatamask/config.yaml`.

---

## API Reference

The backend exposes a RESTful API at `/api/`. All endpoints (except `/api/auth/*`) require a Bearer JWT token.

### Authentication

```bash
# Register
POST /api/auth/register
{"username":"alice","email":"alice@example.com","password":"secret123"}

# Login
POST /api/auth/login
{"username":"alice","password":"secret123"}
# Response: {"token":"<jwt>","user":{...}}
```

### Key Endpoints

| Resource | Endpoint | Methods |
|---|---|---|
| Workspaces | `/api/workspaces` | GET, POST |
| Workspace | `/api/workspaces/{id}` | GET, PUT, DELETE |
| Workspace Stats | `/api/workspaces/{id}/stats` | GET |
| Connections | `/api/workspaces/{id}/connections` | GET, POST |
| Tables | `/api/workspaces/{id}/tables` | GET, POST |
| Jobs | `/api/workspaces/{id}/jobs` | GET, POST |
| Job Schedules | `/api/workspaces/{id}/schedules` | GET, POST, PUT, DELETE |
| Webhooks | `/api/workspaces/{id}/webhooks` | GET, POST, PUT, DELETE |
| Privacy Hub | `/api/workspaces/{id}/privacy-hub` | GET |
| Privacy Report | `/api/workspaces/{id}/privacy-report` | GET |
| Sensitivity Scan | `/api/workspaces/{id}/sensitivity-scan/run` | POST |
| Schema Changes | `/api/workspaces/{id}/schema-changes` | GET, POST |
| Workspace Export | `/api/workspaces/{id}/export` | GET |
| Workspace Import | `/api/workspaces/{id}/import` | POST |

---

## Deployment Options

### Docker Compose (recommended for evaluation)

```bash
docker-compose up -d
```

### Kubernetes

Build and push Docker images, then deploy using standard Kubernetes manifests or Helm. Each component (backend, frontend) has its own `Dockerfile`.

```bash
# Build images
docker build -t opendatamask-backend ./backend
docker build -t opendatamask-frontend ./frontend
```

### CI / CD

OpenDataMask includes three GitHub Actions workflows:

| Workflow | File | Description |
|---|---|---|
| CI | `.github/workflows/ci.yml` | Build, lint, test on every push/PR |
| Docker Build | `.github/workflows/docker.yml` | Build and push images to GHCR |
| CodeQL | `.github/workflows/codeql.yml` | Weekly security analysis |
| Deployment Verification | `.github/workflows/verify-deployment.yml` | Smoke tests after CI passes |

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| `JWT_SECRET must be set` | Missing env var | Set `JWT_SECRET` before starting |
| `ENCRYPTION_KEY must be set` | Missing env var | Set `ENCRYPTION_KEY` (exactly 16 or 32 chars) |
| Backend fails to start | PostgreSQL not ready | Ensure PostgreSQL is running and accessible |
| `Connection refused` on port 8080 | Backend not started | Check `docker-compose logs backend` |
| Empty workspace stats | No connections/jobs created | Add a connection and run at least one job |
| Sensitivity scan finds nothing | No table configurations yet | Add table configurations and re-run the scan |

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes and add tests
4. Run `./gradlew build --no-daemon` (backend) and `npm test -- --run` (frontend)
5. Open a pull request against `main`

All contributions must maintain the existing test coverage and pass the CI pipeline.
