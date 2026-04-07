#!/usr/bin/env python3
"""
verify.py — OpenDataMask Sandboxed Verification Script
=======================================================
Connects to SOURCE_DB and TARGET_DB after a masking job has run and
performs the following automated checks:

  1. Record Integrity   — row count in SOURCE matches TARGET.
  2. Key Persistence    — every id present in SOURCE exists in TARGET.
  3. Masking Effectiveness — full_name and email are different between
                             source and target for every row.
  4. Human Readability  — logs a sample of 5 masked records so a human
                          can visually confirm the data looks realistic.

Outputs a "Verification Report" summarising pass / fail status.

Environment variables (with defaults matching docker-compose.yml):
  SOURCE_DB_HOST   / SOURCE_DB_PORT   / SOURCE_DB_NAME
  SOURCE_DB_USER   / SOURCE_DB_PASS
  TARGET_DB_HOST   / TARGET_DB_PORT   / TARGET_DB_NAME
  TARGET_DB_USER   / TARGET_DB_PASS
"""

import os
import sys

try:
    import psycopg2
    import psycopg2.extras
    from psycopg2 import sql as pgsql
except ImportError:
    print("ERROR: psycopg2 is not installed.  Run:  pip install psycopg2-binary")
    sys.exit(1)


# ── Database connection parameters ──────────────────────────────────────────

SOURCE = dict(
    host=os.getenv("SOURCE_DB_HOST", "localhost"),
    port=int(os.getenv("SOURCE_DB_PORT", "5433")),
    dbname=os.getenv("SOURCE_DB_NAME", "source_db"),
    user=os.getenv("SOURCE_DB_USER", "source_user"),
    password=os.getenv("SOURCE_DB_PASS", "source_pass"),
)

TARGET = dict(
    host=os.getenv("TARGET_DB_HOST", "localhost"),
    port=int(os.getenv("TARGET_DB_PORT", "5434")),
    dbname=os.getenv("TARGET_DB_NAME", "target_db"),
    user=os.getenv("TARGET_DB_USER", "target_user"),
    password=os.getenv("TARGET_DB_PASS", "target_pass"),
)

TABLE = "users"


# ── Helpers ──────────────────────────────────────────────────────────────────

class Check:
    PASS = "PASS"
    FAIL = "FAIL"

    def __init__(self, name: str):
        self.name = name
        self.status = Check.PASS
        self.messages: list[str] = []

    def fail(self, msg: str) -> None:
        self.status = Check.FAIL
        self.messages.append(msg)

    def info(self, msg: str) -> None:
        self.messages.append(msg)

    def __str__(self) -> str:
        icon = "✓" if self.status == Check.PASS else "✗"
        lines = [f"  [{icon}] {self.name}: {self.status}"]
        for m in self.messages:
            lines.append(f"       {m}")
        return "\n".join(lines)


def connect(params: dict):
    try:
        conn = psycopg2.connect(**params, cursor_factory=psycopg2.extras.RealDictCursor)
        conn.autocommit = True
        return conn
    except psycopg2.OperationalError as exc:
        print(f"ERROR: Cannot connect to database {params['dbname']}@{params['host']}:{params['port']}")
        print(f"       {exc}")
        sys.exit(1)


def fetch_all(conn, query, params=None) -> list[dict]:
    with conn.cursor() as cur:
        cur.execute(query, params)
        return cur.fetchall()


def count_rows(conn, table: str) -> int:
    # Use pgsql.Identifier to safely quote the table name and prevent SQL injection.
    query = pgsql.SQL("SELECT COUNT(*) AS cnt FROM {}").format(pgsql.Identifier(table))
    rows = fetch_all(conn, query)
    return rows[0]["cnt"]


# ── Verification checks ───────────────────────────────────────────────────────

def check_record_integrity(src_conn, tgt_conn) -> Check:
    chk = Check("Record Integrity (row count matches)")
    src_count = count_rows(src_conn, TABLE)
    tgt_count = count_rows(tgt_conn, TABLE)
    chk.info(f"Source row count : {src_count}")
    chk.info(f"Target row count : {tgt_count}")
    if src_count == 0:
        chk.fail(
            f"Source table '{TABLE}' is empty; verification cannot pass with 0 source rows"
        )
    elif src_count != tgt_count:
        chk.fail(
            f"Row count mismatch: source={src_count}, target={tgt_count}"
        )
    return chk


def check_key_persistence(src_conn, tgt_conn) -> Check:
    chk = Check("Key Persistence (all source IDs present in target)")
    id_query = pgsql.SQL("SELECT id FROM {}").format(pgsql.Identifier(TABLE))
    src_ids = {str(r["id"]) for r in fetch_all(src_conn, id_query)}
    tgt_ids = {str(r["id"]) for r in fetch_all(tgt_conn, id_query)}

    missing = src_ids - tgt_ids
    extra   = tgt_ids - src_ids
    chk.info(f"Source IDs : {len(src_ids)}")
    chk.info(f"Target IDs : {len(tgt_ids)}")

    if missing:
        chk.fail(
            f"{len(missing)} source ID(s) missing from target: "
            f"{sorted(missing)[:5]}{'...' if len(missing) > 5 else ''}"
        )
    if extra:
        chk.fail(
            f"{len(extra)} unexpected ID(s) found only in target: "
            f"{sorted(extra)[:5]}{'...' if len(extra) > 5 else ''}"
        )
    return chk


def check_masking_effectiveness(src_conn, tgt_conn) -> Check:
    chk = Check("Masking Effectiveness (PII fields differ between source and target)")

    pii_query = pgsql.SQL("SELECT id, full_name, email FROM {}").format(
        pgsql.Identifier(TABLE)
    )
    src_rows = {str(r["id"]): r for r in fetch_all(src_conn, pii_query)}
    tgt_rows = {str(r["id"]): r for r in fetch_all(tgt_conn, pii_query)}

    unmasked_name  = 0
    unmasked_email = 0
    checked        = 0

    for uid, src in src_rows.items():
        tgt = tgt_rows.get(uid)
        if tgt is None:
            continue
        checked += 1
        if src["full_name"] == tgt["full_name"]:
            unmasked_name += 1
        if src["email"] == tgt["email"]:
            unmasked_email += 1

    chk.info(f"Rows compared : {checked}")
    chk.info(f"Name unchanged  (should be 0) : {unmasked_name}")
    chk.info(f"Email unchanged (should be 0) : {unmasked_email}")

    if checked == 0:
        chk.fail("No rows could be compared (source or target may be empty).")
    else:
        if unmasked_name > 0:
            chk.fail(f"{unmasked_name} row(s) have the same full_name in source and target.")
        if unmasked_email > 0:
            chk.fail(f"{unmasked_email} row(s) have the same email in source and target.")

    return chk


def check_human_readability(tgt_conn, masking_passed: bool = True) -> Check:
    """
    Print a sample of masked records for visual human inspection.

    The sample is only printed when *masking_passed* is True.  If masking
    effectiveness failed, the target may still contain real source data, so
    printing it here could expose genuine PII — in that case we skip the
    sample and report the reason.

    When masking has passed, the values printed are the anonymised (fake)
    output produced by OpenDataMask's Datafaker-powered generators.
    """
    chk = Check("Human Readability (sample of 5 masked records)")

    if not masking_passed:
        chk.fail(
            "Sample skipped: masking effectiveness check did not pass. "
            "Printing TARGET_DB rows could expose real PII."
        )
        return chk

    # ORDER BY id gives a stable, deterministic sample across runs.
    sample_query = pgsql.SQL(
        "SELECT id, full_name, email, phone_number, date_of_birth, salary "
        "FROM {} ORDER BY id LIMIT 5"
    ).format(pgsql.Identifier(TABLE))
    # Values retrieved here are already-anonymised fakes, not real sensitive data.
    sample = fetch_all(tgt_conn, sample_query)

    print("\n  -- Masked Record Sample (TARGET_DB) ----------------------------------")
    for i, row in enumerate(sample, 1):
        # All fields below are Datafaker-generated fakes.
        print(f"  [{i}] id            : {row['id']}")
        print(f"       full_name     : {row['full_name']}")
        print(f"       email         : {row['email']}")
        print(f"       phone_number  : {row['phone_number']}")
        print(f"       date_of_birth : {row['date_of_birth']}")
        print(f"       salary        : {row['salary']}")
        print()

    # Heuristic: Faker-generated full names always contain at least one space
    # (first name + last name).  A missing space suggests the generator may not
    # be producing realistic output.
    suspicious_names = [
        str(row["full_name"])
        for row in sample
        if " " not in str(row["full_name"])
    ]
    if suspicious_names:
        chk.fail(
            f"The following masked names do not look like realistic full names "
            f"(no space found): {suspicious_names}"
        )

    # Masked emails must contain '@' to be valid e-mail addresses.
    bad_emails = [
        str(row["email"])
        for row in sample
        if "@" not in str(row["email"])
    ]
    if bad_emails:
        chk.fail(f"The following masked emails are missing '@': {bad_emails}")

    return chk


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> int:
    print("\n" + "=" * 60)
    print("  OpenDataMask -- Verification Report")
    print("=" * 60)

    print(
        f"\nConnecting to SOURCE_DB "
        f"({SOURCE['host']}:{SOURCE['port']}/{SOURCE['dbname']})..."
    )
    src_conn = connect(SOURCE)

    print(
        f"Connecting to TARGET_DB "
        f"({TARGET['host']}:{TARGET['port']}/{TARGET['dbname']})..."
    )
    tgt_conn = connect(TARGET)

    checks = [
        check_record_integrity(src_conn, tgt_conn),
        check_key_persistence(src_conn, tgt_conn),
    ]

    masking_chk = check_masking_effectiveness(src_conn, tgt_conn)
    checks.append(masking_chk)

    # Only print TARGET_DB sample when masking has been confirmed effective —
    # if masking failed the target may still hold real source data.
    checks.append(
        check_human_readability(
            tgt_conn, masking_passed=(masking_chk.status == Check.PASS)
        )
    )

    src_conn.close()
    tgt_conn.close()

    print("\n" + "-" * 60)
    print("  Results")
    print("-" * 60)
    for chk in checks:
        print(chk)

    passed = sum(1 for c in checks if c.status == Check.PASS)
    failed = sum(1 for c in checks if c.status == Check.FAIL)

    print("\n" + "=" * 60)
    if failed == 0:
        print(f"  OK  ALL {passed}/{len(checks)} CHECKS PASSED")
    else:
        print(f"  FAIL  {failed}/{len(checks)} CHECK(S) FAILED  ({passed} passed)")
    print("=" * 60 + "\n")

    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
