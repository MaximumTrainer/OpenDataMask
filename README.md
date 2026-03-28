# OpenDataMask

An open source reimplementation of [tonic-structural](https://www.tonic.ai/products/tonic-structural) — a data masking and anonymization platform.

## Overview

OpenDataMask allows you to connect to your databases and generate realistic, anonymized copies of your data. It supports multiple database types and provides a web UI, REST API, and CLI tool.

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌──────────────────────┐
│  Vue.js UI  │────▶│  Spring Boot │────▶│ PostgreSQL (app DB)  │
│ (TypeScript)│     │  Backend     │     └──────────────────────┘
└─────────────┘     │  (Kotlin)    │
                    │              │────▶ Source DB (MongoDB /
┌─────────────┐     │              │      PostgreSQL / Azure SQL /
│  Go CLI     │────▶│              │      MongoDB on Cosmos)
│   (odm)     │     └──────────────┘
└─────────────┘
```

## Features

- **User authentication & security** (JWT-based)
- **Workspace management** with user roles (Admin, User, Viewer)
- **Database connectors**: PostgreSQL, MongoDB, Azure SQL, MongoDB on Cosmos
- **Data masking generators**: Name, Email, Phone, Address, SSN, Credit Card, Date, UUID, Constant, Null, Custom
- **Table modes**: Passthrough, Mask, Generate, Subset
- **Job tracking** with logs
- **Post-job actions**: Webhook, Email, Script
- **REST API** for programmatic access
- **CLI tool** (`odm`) for command-line management

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Backend   | Kotlin + Spring Boot 3.2 |
| Frontend  | TypeScript + Vue 3 + Vite |
| CLI       | Go (cobra) |
| App DB    | PostgreSQL |
| Test DB   | H2 (in-memory) |
| Tests     | JUnit 5 |
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

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | Authenticate and get JWT token |
| POST | `/api/auth/register` | Register a new user |
| GET | `/api/workspaces` | List workspaces |
| POST | `/api/workspaces` | Create workspace |
| GET | `/api/workspaces/{id}/connections` | List data connections |
| POST | `/api/workspaces/{id}/connections` | Create data connection |
| POST | `/api/workspaces/{id}/connections/{cid}/test` | Test connection |
| GET | `/api/workspaces/{id}/tables` | List table configurations |
| POST | `/api/workspaces/{id}/tables` | Create table configuration |
| POST | `/api/workspaces/{id}/jobs` | Start a masking job |
| GET | `/api/workspaces/{id}/jobs/{jid}/logs` | Get job logs |

## Configuration

Configure via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/opendatamask` | App database URL |
| `DATABASE_USERNAME` | `opendatamask` | App database username |
| `DATABASE_PASSWORD` | `opendatamask` | App database password |
| `JWT_SECRET` | *(insecure default)* | JWT signing secret (change in production!) |
| `ENCRYPTION_KEY` | *(insecure default)* | AES key for encrypting connection passwords |
| `SERVER_PORT` | `8080` | Backend server port |

## Security Notes

- Change `JWT_SECRET` and `ENCRYPTION_KEY` in production
- Use HTTPS in production (the CLI supports `--insecure` only for development)
- Connection passwords are AES-encrypted before storage
- User passwords are BCrypt-hashed

## License

Open source — see [LICENSE](LICENSE) for details.

