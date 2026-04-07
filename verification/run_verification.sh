#!/usr/bin/env bash
# run_verification.sh — End-to-end verification runner for OpenDataMask.
#
# This script:
#   1. Starts the sandboxed Docker environment (source_db, target_db, app_db, backend).
#   2. Waits for the backend service API to become healthy.
#   3. Configures OpenDataMask via its REST API (workspace, connections,
#      table configuration, column generators).
#   4. Triggers a masking job and waits for it to complete.
#   5. Invokes verify.py to validate masking results.
#
# Prerequisites: docker compose (v2), curl, python3 (with pip module).
# Run from the repository root or from the verification/ directory.

set -euo pipefail

# ── Resolve paths ─────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Colour helpers ────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; }
die()     { error "$*"; exit 1; }

# backend_is_healthy  —  returns 0 when /actuator/health reports status=UP, else 1.
backend_is_healthy() {
    curl -sf "${API_BASE}/actuator/health" \
        | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
    sys.exit(0 if isinstance(d, dict) and d.get("status") == "UP" else 1)
except Exception:
    sys.exit(1)
'
}

# ── Configuration ─────────────────────────────────────────────────────────────
API_BASE="http://localhost:8080"
ODM_USER="verifier"
ODM_PASS="Verif1cation!Pass"
ODM_EMAIL="verifier@odm-sandbox.local"

# ── DB credentials — read from env with same defaults as docker-compose.yml ──
SOURCE_DB_NAME="${SOURCE_DB_NAME:-source_db}"
SOURCE_DB_USER="${SOURCE_DB_USER:-source_user}"
SOURCE_DB_PASS="${SOURCE_DB_PASS:-source_pass}"
TARGET_DB_NAME="${TARGET_DB_NAME:-target_db}"
TARGET_DB_USER="${TARGET_DB_USER:-target_user}"
TARGET_DB_PASS="${TARGET_DB_PASS:-target_pass}"

# ── Prerequisites check ───────────────────────────────────────────────────────
info "Checking prerequisites…"
command -v docker   >/dev/null 2>&1 || die "docker is required but not installed."
command -v curl     >/dev/null 2>&1 || die "curl is required but not installed."
command -v python3  >/dev/null 2>&1 || die "python3 is required but not installed."

# Support both `docker compose` (v2) and `docker-compose` (v1)
if docker compose version >/dev/null 2>&1; then
    DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
    DC="docker-compose"
else
    die "docker compose (v2) or docker-compose (v1) is required but not found."
fi

# ── Install Python dependencies ───────────────────────────────────────────────
# Use `python3 -m pip` to avoid a hard dependency on a separately-installed pip3.
info "Installing Python dependencies…"
python3 -m pip install -q -r requirements.txt

# ── Start Docker environment ──────────────────────────────────────────────────
info "Starting Docker environment…"
$DC -f docker-compose.yml up -d --build

# ── Wait for backend health ───────────────────────────────────────────────────
info "Waiting for OpenDataMask backend to become healthy (up to 3 min)…"
MAX_WAIT=180
ELAPSED=0
until backend_is_healthy; do
    if [ $ELAPSED -ge $MAX_WAIT ]; then
        die "Backend did not become healthy within ${MAX_WAIT}s."
    fi
    sleep 5
    ELAPSED=$((ELAPSED + 5))
    echo -n "."
done
echo ""
info "Backend is healthy."

# ── Helper: call the API ──────────────────────────────────────────────────────
# api_post <path> <json_body>  → response body
api_post() {
    local path="$1" body="$2"
    curl -sf -X POST "${API_BASE}${path}" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${TOKEN:-}" \
        -d "$body"
}

# api_get <path>  → response body
api_get() {
    local path="$1"
    curl -sf -X GET "${API_BASE}${path}" \
        -H "Authorization: Bearer ${TOKEN:-}"
}

# ── Register user (ignore error if already exists) ────────────────────────────
info "Registering user '${ODM_USER}'…"
curl -sf -X POST "${API_BASE}/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${ODM_USER}\",\"email\":\"${ODM_EMAIL}\",\"password\":\"${ODM_PASS}\"}" \
    > /dev/null 2>&1 || true   # silently continue if user already exists

# ── Login ─────────────────────────────────────────────────────────────────────
info "Logging in…"
LOGIN_RESP=$(curl -sf -X POST "${API_BASE}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${ODM_USER}\",\"password\":\"${ODM_PASS}\"}" \
    || die "Login request failed. Check that the backend is running and reachable at ${API_BASE}.")

TOKEN=$(echo "$LOGIN_RESP" \
    | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('token',''))" \
    2>/dev/null || true)
[ -n "$TOKEN" ] || die "Failed to obtain JWT token. Login response: ${LOGIN_RESP}"
info "Authenticated successfully."

# ── Create workspace ──────────────────────────────────────────────────────────
info "Creating workspace…"
WS_RESP=$(api_post "/api/workspaces" \
    '{"name":"Verification Workspace","description":"Automated PII masking verification"}')
WS_ID=$(echo "$WS_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
info "Workspace created: id=${WS_ID}"

# ── Create source connection ──────────────────────────────────────────────────
info "Creating source data connection (SOURCE_DB)…"
SRC_RESP=$(api_post "/api/workspaces/${WS_ID}/connections" \
    "{\"name\":\"source-db\",\"type\":\"POSTGRESQL\",
      \"connectionString\":\"jdbc:postgresql://source_db:5432/${SOURCE_DB_NAME}\",
      \"username\":\"${SOURCE_DB_USER}\",\"password\":\"${SOURCE_DB_PASS}\",
      \"isSource\":true,\"isDestination\":false}")
SRC_CONN_ID=$(echo "$SRC_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
info "Source connection created: id=${SRC_CONN_ID}"

# ── Create destination connection ─────────────────────────────────────────────
info "Creating destination data connection (TARGET_DB)…"
DST_RESP=$(api_post "/api/workspaces/${WS_ID}/connections" \
    "{\"name\":\"target-db\",\"type\":\"POSTGRESQL\",
      \"connectionString\":\"jdbc:postgresql://target_db:5432/${TARGET_DB_NAME}\",
      \"username\":\"${TARGET_DB_USER}\",\"password\":\"${TARGET_DB_PASS}\",
      \"isSource\":false,\"isDestination\":true}")
DST_CONN_ID=$(echo "$DST_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
info "Destination connection created: id=${DST_CONN_ID}"

# ── Create table configuration (MASK mode) ────────────────────────────────────
info "Creating table configuration for 'users' (MASK mode)…"
TABLE_RESP=$(api_post "/api/workspaces/${WS_ID}/tables" \
    '{"tableName":"users","mode":"MASK"}')
TABLE_ID=$(echo "$TABLE_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
info "Table configuration created: id=${TABLE_ID}"

# ── Add column generators ─────────────────────────────────────────────────────
# The 'id' column has no generator → it is passed through unchanged (PK preserved).

add_generator() {
    local col="$1" gtype="$2" params="${3:-}"
    # Build JSON payload via Python so that generatorParams is properly serialised
    # as a JSON *string* value (the backend field is String?, not an embedded object).
    # sys.argv avoids any shell-quoting issues with special characters in params.
    if [ -z "$params" ]; then
        BODY=$(python3 -c "
import json, sys
print(json.dumps({'columnName': sys.argv[1], 'generatorType': sys.argv[2]}))
" -- "$col" "$gtype")
    else
        BODY=$(python3 -c "
import json, sys
print(json.dumps({'columnName': sys.argv[1], 'generatorType': sys.argv[2], 'generatorParams': sys.argv[3]}))
" -- "$col" "$gtype" "$params")
    fi
    api_post "/api/workspaces/${WS_ID}/tables/${TABLE_ID}/generators" "$BODY" > /dev/null
    info "  Generator added: ${col} → ${gtype}"
}

info "Configuring column generators…"
add_generator "full_name"     "FULL_NAME"
add_generator "email"         "EMAIL"
add_generator "phone_number"  "PHONE"
add_generator "date_of_birth" "BIRTH_DATE"
add_generator "salary"        "RANDOM_INT" '{"min":"30000","max":"200000"}'

# ── Run masking job ───────────────────────────────────────────────────────────
info "Triggering masking job…"
JOB_RESP=$(api_post "/api/workspaces/${WS_ID}/jobs" '{}')
JOB_ID=$(echo "$JOB_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
info "Job started: id=${JOB_ID}"

# ── Poll until job completes ──────────────────────────────────────────────────
info "Waiting for job ${JOB_ID} to complete…"
MAX_WAIT=120
ELAPSED=0
while true; do
    STATUS=$(api_get "/api/workspaces/${WS_ID}/jobs/${JOB_ID}" \
        | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])")
    if [ "$STATUS" = "COMPLETED" ]; then
        info "Job completed successfully."
        break
    elif [ "$STATUS" = "FAILED" ] || [ "$STATUS" = "CANCELLED" ]; then
        # Print job logs for debugging
        warn "Job ended with status: ${STATUS}. Fetching logs…"
        api_get "/api/workspaces/${WS_ID}/jobs/${JOB_ID}/logs" \
            | python3 -c "
import sys, json
logs = json.load(sys.stdin)
for l in logs:
    print(f'[{l[\"level\"]}] {l[\"message\"]}')
"
        die "Masking job ${JOB_ID} did not complete successfully (status=${STATUS})."
    fi
    if [ $ELAPSED -ge $MAX_WAIT ]; then
        die "Job did not complete within ${MAX_WAIT}s."
    fi
    sleep 5
    ELAPSED=$((ELAPSED + 5))
    echo -n "."
done
echo ""

# ── Run Python verification ───────────────────────────────────────────────────
# Use `if/else` so that a non-zero exit from verify.py is caught by our
# explicit handler — not by `set -e` — ensuring the result banner always prints.
#
# Set VERIFY_JUNIT_XML to a file path to also write a JUnit XML report, e.g.:
#   VERIFY_JUNIT_XML=/tmp/report.xml ./run_verification.sh
info "Running verification script…"
JUNIT_ARGS=()
if [ -n "${VERIFY_JUNIT_XML:-}" ]; then
    JUNIT_ARGS=(--junit-xml "${VERIFY_JUNIT_XML}")
fi
if python3 verify.py "${JUNIT_ARGS[@]}"; then
    echo ""
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo -e "${GREEN}  ✓  ALL VERIFICATION CHECKS PASSED     ${NC}"
    echo -e "${GREEN}════════════════════════════════════════${NC}"
else
    echo ""
    echo -e "${RED}════════════════════════════════════════${NC}"
    echo -e "${RED}  ✗  ONE OR MORE VERIFICATION CHECKS FAILED ${NC}"
    echo -e "${RED}════════════════════════════════════════${NC}"
    exit 1
fi
