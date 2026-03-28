# OpenDataMask — Requirements & Implementation Gaps

This document identifies gaps between the current OpenDataMask implementation and the
target requirements for an open-source reimplementation of
[tonic-structural](https://www.tonic.ai/products/tonic-structural).

> **Legend**
> - **Priority**: CRITICAL / HIGH / MEDIUM / LOW
> - **Status**: NOT STARTED / PARTIAL / DONE

---

## 1. Data Transformation Pipeline — Write to Destination

| Field | Value |
|-------|-------|
| **Priority** | CRITICAL |
| **Status** | NOT STARTED |
| **Affected files** | `backend/src/main/kotlin/com/opendatamask/connector/DatabaseConnector.kt`, `backend/src/main/kotlin/com/opendatamask/service/JobService.kt`, all connector implementations |

### Gap

The `DatabaseConnector` interface only exposes read operations (`testConnection`,
`listTables`, `listColumns`, `fetchData`). There are no methods for writing data to a
destination database (e.g. `writeData`, `insertBatch`, `upsertData`,
`createTable`, `truncateTable`).

`JobService.runJob()` contains an explicit TODO:

```kotlin
// TODO: implement actual data write to destConnector once a write API is defined on DatabaseConnector
```

Without write support the entire masking / generation pipeline is non-functional — data
is read from the source but never written to the destination.

### Required Work

- Add write methods to the `DatabaseConnector` interface (e.g. `writeData`,
  `createTable`, `truncateTable`).
- Implement write methods in all four connectors: `PostgreSQLConnector`,
  `MongoDBConnector`, `AzureSQLConnector`, `MongoDBCosmosConnector`.
- Update `JobService.processTable()` to write transformed data to the destination
  connector.
- Add transaction management and batch-write support for large data sets.

---

## 2. Data Masking & Generation Execution Logic

| Field | Value |
|-------|-------|
| **Priority** | CRITICAL |
| **Status** | NOT STARTED |
| **Affected files** | `backend/src/main/kotlin/com/opendatamask/model/ColumnGenerator.kt`, `backend/src/main/kotlin/com/opendatamask/service/JobService.kt` |

### Gap

Generator types are defined as enums (`NAME`, `EMAIL`, `PHONE`, `ADDRESS`, `SSN`,
`CREDIT_CARD`, `DATE`, `UUID`, `CONSTANT`, `NULL`, `CUSTOM`) but there is **no
execution logic** that transforms data. No `GeneratorService`,
`GeneratorExecutor`, or equivalent class exists. `JobService` fetches generators
from the database but never applies them:

```kotlin
val generators = columnGeneratorRepository.findByTableConfigurationId(tableConfig.id)
val data = sourceConnector.fetchData(tableConfig.tableName, ...)
addLog(jobId, "Masking ${data.size} rows ... with ${generators.size} generator(s)", LogLevel.INFO)
// ← no actual masking/generation happens
```

### Required Work

- Create a `GeneratorService` (or `GeneratorExecutor`) that applies each
  `GeneratorType` to column values.
- Integrate a faker / data-generation library (e.g.
  [datafaker](https://www.datafaker.net/)) for realistic synthetic data.
- Implement each generator type: `NAME`, `EMAIL`, `PHONE`, `ADDRESS`, `SSN`,
  `CREDIT_CARD`, `DATE`, `UUID`, `CONSTANT`, `NULL`, `CUSTOM`.
- Support `generatorParams` (JSON configuration per column) for customization.
- Wire `GeneratorService` into `JobService.processTable()` so masked/generated rows
  are produced before writing to the destination.

---

## 3. File Connector Workspace

| Field | Value |
|-------|-------|
| **Priority** | MEDIUM |
| **Status** | NOT STARTED |
| **Affected files** | None (new feature) |

### Gap

The requirements state:

> *"For a file connector workspace that uses files uploaded from a local file system, the
> Structural application database stores the encrypted source files. It also stores the
> generated destination files."*

There is **no file connector** in the codebase. No file upload endpoint, no file
storage service, no `ConnectionType.FILE` enum value, and no encrypted file
storage mechanism.

### Required Work

- Add a `FILE` connection type to `ConnectionType` enum.
- Create a file-upload REST endpoint (e.g. `POST /api/workspaces/{id}/files`).
- Implement a `FileStorageService` that encrypts source files (using existing
  `EncryptionService` AES logic) and stores them in the application database or on
  disk.
- Create a `FileConnector` implementing `DatabaseConnector` (or a dedicated
  interface) for reading/writing file-based data (CSV, JSON, etc.).
- Store generated/masked destination files in the application database alongside
  source files.
- Add download endpoints for destination files.

---

## 4. Post-Job Actions — Service, Controller & Execution

| Field | Value |
|-------|-------|
| **Priority** | HIGH |
| **Status** | PARTIAL |
| **Affected files** | `backend/src/main/kotlin/com/opendatamask/model/PostJobAction.kt`, `backend/src/main/kotlin/com/opendatamask/repository/PostJobActionRepository.kt` |

### Gap

The `PostJobAction` JPA entity and repository exist, but:

- **No `PostJobActionService`** — no business logic for managing or executing
  actions.
- **No `PostJobActionController`** — no REST API endpoints to create, list, update,
  delete, or execute post-job actions.
- **No execution trigger** — `JobService` does not invoke post-job actions on job
  completion or failure.

The three action types (`WEBHOOK`, `EMAIL`, `SCRIPT`) are defined as enums but
have no implementation.

### Required Work

- Create `PostJobActionService` with CRUD operations and execution logic for each
  action type (`WEBHOOK`, `EMAIL`, `SCRIPT`).
- Create `PostJobActionController` with endpoints scoped to workspaces (e.g.
  `POST /api/workspaces/{id}/actions`, `GET`, `PUT`, `DELETE`).
- Integrate execution into `JobService`: after a job completes (or fails), query
  enabled `PostJobAction` entries for the workspace and execute them.
- Implement webhook HTTP call-out, email dispatch, and script execution.
- Add frontend views/components for managing post-job actions.

---

## 5. Frontend ↔ Backend Type Mismatches

| Field | Value |
|-------|-------|
| **Priority** | HIGH |
| **Status** | NOT STARTED |
| **Affected files** | `frontend/src/types/index.ts`, `backend/src/main/kotlin/com/opendatamask/model/DataConnection.kt`, `backend/src/main/kotlin/com/opendatamask/model/ColumnGenerator.kt` |

### Gap

#### Connection Types

| Frontend (`types/index.ts`) | Backend (`DataConnection.kt`) | Match? |
|-----------------------------|-------------------------------|--------|
| `POSTGRESQL` | `POSTGRESQL` | ✅ |
| `MYSQL` | — | ❌ Frontend-only |
| `MARIADB` | — | ❌ Frontend-only |
| `MSSQL` | — | ❌ Frontend-only |
| `ORACLE` | — | ❌ Frontend-only |
| `SQLITE` | — | ❌ Frontend-only |
| `MONGODB` | `MONGODB` | ✅ |
| — | `AZURE_SQL` | ❌ Backend-only |
| — | `MONGODB_COSMOS` | ❌ Backend-only |

Selecting a frontend-only type (e.g. `MYSQL`) causes backend request deserialization/binding to fail
(typically returning an HTTP 400 response), because the value cannot be mapped to the backend
`ConnectionType` enum; as a result, `ConnectorFactory.createConnector()` is never invoked in this case.

#### Generator Types

The frontend defines 16 generator types (e.g. `RANDOM_STRING`,
`HASH_SHA256`, `FAKER`) while the backend defines 11 different types (e.g.
`NAME`, `SSN`, `CREDIT_CARD`). The two enums are **not aligned**.

#### User Roles

Frontend defines `ADMIN`, `USER`. Backend model includes an additional `VIEWER`
role in `WorkspaceRole`. Misalignment can cause issues when rendering workspace
user lists.

### Required Work

- Align `ConnectionType` enums so frontend and backend are identical.
- Align `GeneratorType` enums (decide on a single canonical list).
- Align `UserRole` / `WorkspaceRole` enums.
- Add end-to-end type validation or a shared schema (e.g. OpenAPI spec
  generation from the backend).

---

## 6. WHERE Clause Support in Connectors (Subsetting)

| Field | Value |
|-------|-------|
| **Priority** | MEDIUM |
| **Status** | PARTIAL |
| **Affected files** | `backend/src/main/kotlin/com/opendatamask/connector/DatabaseConnector.kt`, all connector implementations, `backend/src/main/kotlin/com/opendatamask/model/TableConfiguration.kt` |

### Gap

`TableConfiguration` stores a `whereClause` field (up to 4096 characters) and the
frontend supports entering it, but:

- The `DatabaseConnector.fetchData()` method signature does not accept a
  `whereClause` parameter.
- None of the four connector implementations apply a WHERE clause.
- `JobService.processTable()` for `SUBSET` mode only passes `rowLimit` to
  `fetchData()` — `whereClause` is ignored.

### Required Work

- Extend `DatabaseConnector.fetchData()` to accept an optional `whereClause`
  parameter (with proper SQL-injection prevention via parameterized queries).
- Implement WHERE clause support in `PostgreSQLConnector` and
  `AzureSQLConnector`.
- Implement equivalent filtering for `MongoDBConnector` and
  `MongoDBCosmosConnector` (translate WHERE to MongoDB query filters).
- Wire `TableConfiguration.whereClause` into the `JobService` processing pipeline.

---

## 7. Test Coverage

| Field | Value |
|-------|-------|
| **Priority** | MEDIUM |
| **Status** | PARTIAL |
| **Affected files** | `backend/src/test/`, `frontend/` (test files), `cli/` (test files) |

### Gap

Only four backend test files exist:

- `OpenDataMaskApplicationTests.kt` (context load test)
- `AuthControllerTest.kt`
- `AuthServiceTest.kt`
- `WorkspaceServiceTest.kt`

**Missing test coverage:**

- Connector tests (`PostgreSQLConnector`, `MongoDBConnector`, `AzureSQLConnector`,
  `MongoDBCosmosConnector`).
- `DataConnectionService` and `DataConnectionController` tests.
- `TableConfigurationService` and `TableConfigurationController` tests.
- `JobService` and `JobController` tests.
- `EncryptionService` tests.
- `JwtTokenProvider` and `JwtAuthenticationFilter` tests.
- Frontend: no test files found despite Vitest being configured.
- CLI: no Go test files found.

### Required Work

- Add unit tests for all services and controllers (target ≥ 80 % line coverage).
- Add connector integration tests (use Testcontainers or mocks).
- Add frontend component and API tests using Vitest.
- Add Go CLI tests using `testing` package.
- Set up CI pipeline for automated test execution.

---

## 8. CI/CD Pipeline

| Field | Value |
|-------|-------|
| **Priority** | LOW |
| **Status** | NOT STARTED |
| **Affected files** | `.github/workflows/` (does not exist) |

### Gap

There is no CI/CD pipeline configured. No GitHub Actions workflow file for:

- Building the backend, frontend, or CLI.
- Running tests automatically on push / pull request.
- Linting / static analysis.
- Docker image builds and publishing.

### Required Work

- Create GitHub Actions workflows for build, test, lint across all three components.
- Add Docker build verification in CI.
- Consider adding code quality / coverage gates.

---

## 9. Insecure Defaults in Configuration

| Field | Value |
|-------|-------|
| **Priority** | HIGH |
| **Status** | PARTIAL |
| **Affected files** | `backend/src/main/resources/application.yml` |

### Gap

`application.yml` ships with hard-coded insecure default values for:

- `opendatamask.jwt.secret` — a default string used for JWT signing. If not
  overridden, any token can be forged.
- `opendatamask.encryption.key` — a default 16-character AES key for encrypting
  connection passwords.

While the README warns users to change these, the application starts without
errors using insecure defaults.

### Required Work

- Fail application startup when `JWT_SECRET` or `ENCRYPTION_KEY` environment
  variables are not explicitly set (or at minimum, when running in a
  non-development profile).
- Remove insecure defaults from committed configuration files.
- Document secure key generation in the README (e.g.
  `openssl rand -base64 32`).

---

## 10. Destination Database Architecture

| Field | Value |
|-------|-------|
| **Priority** | HIGH |
| **Status** | PARTIAL |
| **Affected files** | `backend/src/main/kotlin/com/opendatamask/model/DataConnection.kt`, `backend/src/main/kotlin/com/opendatamask/service/JobService.kt` |

### Gap

The `DataConnection` model has `isSource` and `isDestination` boolean flags, and
`JobService` attempts to locate both a source and destination connection for a
workspace. However:

- Schema creation / mirroring on the destination is not implemented.
- No logic to create tables on the destination that match the source schema.
- No logic to handle type mapping differences between heterogeneous databases
  (e.g. PostgreSQL → MongoDB).
- No pre-job validation that source and destination schemas are compatible.

### Required Work

- Implement destination schema creation (mirror source schema before writing
  data).
- Handle cross-database type mapping (SQL ↔ NoSQL, type differences between
  SQL dialects).
- Add pre-job validation to verify source/destination compatibility.

---

## Summary

| # | Gap | Priority | Status |
|---|-----|----------|--------|
| 1 | Data transformation pipeline — write to destination | CRITICAL | NOT STARTED |
| 2 | Data masking & generation execution logic | CRITICAL | NOT STARTED |
| 3 | File connector workspace | MEDIUM | NOT STARTED |
| 4 | Post-job actions — service, controller & execution | HIGH | PARTIAL |
| 5 | Frontend ↔ backend type mismatches | HIGH | NOT STARTED |
| 6 | WHERE clause support in connectors (subsetting) | MEDIUM | PARTIAL |
| 7 | Test coverage | MEDIUM | PARTIAL |
| 8 | CI/CD pipeline | LOW | NOT STARTED |
| 9 | Insecure defaults in configuration | HIGH | PARTIAL |
| 10 | Destination database architecture | HIGH | PARTIAL |
