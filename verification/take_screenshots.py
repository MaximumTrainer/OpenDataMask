#!/usr/bin/env python3
"""
take_screenshots.py — OpenDataMask UI Screenshot Generator
===========================================================

Sets up all demo data via the REST API, then drives the frontend with
Playwright to capture screenshots at every significant UI step.
The resulting images are saved to docs/website/screenshots/ relative to
the repository root (or the directory specified by --output-dir).

Prerequisites
-------------
    pip install playwright
    playwright install chromium --with-deps

Usage
-----
    # Full setup (register + API seed + screenshots):
    python3 take_screenshots.py --source-host source_db --target-host target_db

    # Skip registration when the account already exists:
    python3 take_screenshots.py --no-register \
        --source-host source_db --target-host target_db

    # Skip API setup entirely (data already seeded, supply IDs):
    python3 take_screenshots.py --no-api-setup \
        --ws-id 1 --src-id 1 --tgt-id 2 --job-id 1

Environment variables
---------------------
    ODM_URL        Frontend base URL (default: http://localhost)
    ODM_API        Backend API base URL (default: http://localhost:8080)
    ODM_USERNAME   Account username
    ODM_PASSWORD   Account password
    ODM_EMAIL      Account email (used during registration)
"""

import argparse
import json
import os
import pathlib
import sys
import time
import urllib.request
import urllib.error

try:
    from playwright.sync_api import sync_playwright, Page
except ImportError:
    print("ERROR: playwright is not installed.  Run:  pip install playwright && playwright install chromium")
    sys.exit(1)



# ── Configuration ─────────────────────────────────────────────────────────────

SCRIPT_DIR = pathlib.Path(__file__).parent
REPO_ROOT   = SCRIPT_DIR.parent
DEFAULT_OUT = REPO_ROOT / "docs" / "website" / "screenshots"

DEFAULT_URL      = os.getenv("ODM_URL",      "http://localhost")
DEFAULT_API      = os.getenv("ODM_API",      "http://localhost:8080")
DEFAULT_USERNAME = os.getenv("ODM_USERNAME", "guide_user")
DEFAULT_PASSWORD = os.getenv("ODM_PASSWORD", "Guide!Pass123")
DEFAULT_EMAIL    = os.getenv("ODM_EMAIL",    "guide@odm-docs.local")

VIEWPORT = {"width": 1440, "height": 900}

# ── Globals set by main() ─────────────────────────────────────────────────────

_step_index = 0
_output_dir: pathlib.Path = DEFAULT_OUT
_api_base   = DEFAULT_API


# ── Helpers ───────────────────────────────────────────────────────────────────

def shot(page: Page, filename: str, *, full_page: bool = False) -> None:
    global _step_index
    _step_index += 1
    dest = _output_dir / filename
    page.screenshot(path=str(dest), full_page=full_page)
    print(f"  [{_step_index:02d}] {dest.name}")


def nav(page: Page, url: str) -> None:
    page.goto(url, wait_until="networkidle")


def wait_for_load(page: Page, timeout: int = 10_000) -> None:
    page.wait_for_load_state("networkidle", timeout=timeout)


def try_dismiss_modal(page: Page) -> None:
    """Best-effort modal dismiss via Cancel button or Escape."""
    try:
        cancel = page.query_selector("button:has-text('Cancel')")
        if cancel:
            cancel.click()
        else:
            page.keyboard.press("Escape")
        page.wait_for_selector(
            ".modal-overlay, dialog, [role='dialog']",
            state="hidden", timeout=3_000
        )
    except Exception:
        pass


def wait_loading_done(page: Page, timeout: int = 8_000) -> None:
    """Wait for any loading overlay to disappear."""
    try:
        page.wait_for_selector(".loading-overlay", state="hidden", timeout=timeout)
    except Exception:
        pass


def try_click(page: Page, selector: str, timeout: int = 6_000) -> bool:
    """Click a selector; return True on success, False if not found/timed-out."""
    try:
        page.click(selector, timeout=timeout)
        return True
    except Exception:
        return False


# ── REST API helpers ──────────────────────────────────────────────────────────

def api_call(method: str, path: str, body: dict | None = None, token: str = "") -> dict:
    url = f"{_api_base}{path}"
    data = json.dumps(body).encode("utf-8") if body else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.loads(resp.read())


# ── API data-setup phase ───────────────────────────────────────────────────────

def api_setup(
    username: str, password: str, email: str,
    src_host: str, src_db: str, src_user: str, src_pass: str,
    tgt_host: str, tgt_db: str, tgt_user: str, tgt_pass: str,
    do_register: bool = True,
) -> tuple[str, int, int, int]:
    """Register (optional) + login + create workspace, two connections, table config with generators.
    Returns (jwt_token, workspace_id, source_connection_id, target_connection_id)."""

    if do_register:
        print("  Registering user…")
        try:
            api_call("POST", "/api/auth/register", {
                "username": username, "email": email, "password": password,
            })
        except Exception:
            pass  # user may already exist

    print("  Logging in…")
    resp  = api_call("POST", "/api/auth/login", {"username": username, "password": password})
    token = resp["token"]

    print("  Creating workspace…")
    ws    = api_call("POST", "/api/workspaces",
                     {"name": "Guide Workspace", "description": "OpenDataMask step-by-step guide demo"},
                     token)
    ws_id = ws["id"]

    print("  Creating source connection…")
    src   = api_call("POST", f"/api/workspaces/{ws_id}/connections", {
        "name": "source-db", "type": "POSTGRESQL",
        "connectionString": f"jdbc:postgresql://{src_host}:5432/{src_db}",
        "username": src_user, "password": src_pass,
        "isSource": True, "isDestination": False,
    }, token)
    src_id = src["id"]

    print("  Creating target connection…")
    tgt   = api_call("POST", f"/api/workspaces/{ws_id}/connections", {
        "name": "target-db", "type": "POSTGRESQL",
        "connectionString": f"jdbc:postgresql://{tgt_host}:5432/{tgt_db}",
        "username": tgt_user, "password": tgt_pass,
        "isSource": False, "isDestination": True,
    }, token)
    tgt_id = tgt["id"]

    print("  Creating table configuration…")
    tbl    = api_call("POST", f"/api/workspaces/{ws_id}/tables",
                      {"connectionId": src_id, "tableName": "users",
                       "schemaName": "public", "mode": "MASK"}, token)
    tbl_id = tbl["id"]

    print("  Adding column generators…")
    for col, gen in [
        ("full_name",     "FULL_NAME"),
        ("email",         "EMAIL"),
        ("phone_number",  "PHONE"),
        ("date_of_birth", "BIRTH_DATE"),
    ]:
        api_call("POST", f"/api/workspaces/{ws_id}/tables/{tbl_id}/generators",
                 {"columnName": col, "generatorType": gen}, token)
    api_call("POST", f"/api/workspaces/{ws_id}/tables/{tbl_id}/generators",
             {"columnName": "salary", "generatorType": "RANDOM_INT",
              "generatorParams": json.dumps({"min": "30000", "max": "200000"})}, token)

    return token, ws_id, src_id, tgt_id


def api_run_job(token: str, ws_id: int, src_id: int, tgt_id: int) -> int:
    job = api_call("POST", f"/api/workspaces/{ws_id}/jobs",
                   {"name": "Guide Masking Job",
                    "sourceConnectionId": src_id,
                    "targetConnectionId": tgt_id}, token)
    return job["id"]


def api_wait_for_job(token: str, ws_id: int, job_id: int, timeout: int = 180) -> str:
    for _ in range(timeout // 5):
        time.sleep(5)
        resp   = api_call("GET", f"/api/workspaces/{ws_id}/jobs/{job_id}", token=token)
        status = resp.get("status", "")
        print(f"    job status: {status}")
        if status in ("COMPLETED", "FAILED", "CANCELLED"):
            return status
    return "TIMEOUT"


# ── Browser screenshot steps ───────────────────────────────────────────────────

def step_login_page(page: Page, base_url: str) -> None:
    nav(page, f"{base_url}/login")
    shot(page, "01-login-page.png")


def step_register_page(page: Page, base_url: str) -> None:
    nav(page, f"{base_url}/register")
    shot(page, "02-register-page.png")


def step_login_filled(page: Page, base_url: str, username: str, password: str) -> None:
    nav(page, f"{base_url}/login")
    # Actual selectors from LoginView.vue: id="username", id="password"
    page.fill("#username", username)
    page.fill("#password", password)
    shot(page, "03-login-filled.png")
    page.click("button[type='submit']")
    page.wait_for_url(lambda u: "/workspaces" in u, timeout=15_000)
    wait_for_load(page)


def step_workspaces_list(page: Page, base_url: str) -> None:
    nav(page, f"{base_url}/workspaces")
    wait_loading_done(page)
    shot(page, "04-workspaces-list.png")

    # Open create workspace modal — optional screenshot
    if try_click(page, "button:has-text('New Workspace')"):
        try:
            page.wait_for_selector("input.form-control", timeout=5_000)
            shot(page, "05-create-workspace-modal.png")
            page.locator("input.form-control").first.fill("My Production Workspace")
            shot(page, "06-create-workspace-filled.png")
        except Exception:
            pass
        try_dismiss_modal(page)


def step_workspace_overview(page: Page, base_url: str, ws_id: int) -> None:
    nav(page, f"{base_url}/workspaces/{ws_id}")
    wait_for_load(page)
    shot(page, "07-workspace-overview.png")


def step_connections(page: Page, base_url: str, ws_id: int) -> None:
    nav(page, f"{base_url}/workspaces/{ws_id}/connections")
    wait_loading_done(page)
    shot(page, "08-connections-configured.png")

    # Open add connection modal — optional screenshot
    if try_click(page, "button:has-text('Add Connection')"):
        try:
            page.wait_for_selector("input[placeholder='Production DB']", timeout=5_000)
            shot(page, "09-add-connection-modal.png")
            page.fill("input[placeholder='Production DB']", "demo-source")
            page.fill("input[placeholder='localhost']", "source_db")
            page.fill("input[placeholder='mydb']", "source_db")
            page.fill("input[placeholder='admin']", "source_user")
            page.fill("input[type='password'][autocomplete='new-password']", "•••••••••")
            src_label = page.locator("label").filter(has_text="Source (read data")
            src_cb = src_label.locator("input[type='checkbox']")
            if not src_cb.is_checked():
                src_cb.click()
            shot(page, "10-connection-form-filled.png")
        except Exception:
            pass
        try_dismiss_modal(page)


def step_tables(page: Page, base_url: str, ws_id: int) -> None:
    nav(page, f"{base_url}/workspaces/{ws_id}/tables")
    wait_loading_done(page)
    shot(page, "11-tables-configured.png")

    # Expand the first table to show column generators
    expand_btn = page.query_selector("button:has-text('▼ Columns')")
    if expand_btn:
        try:
            expand_btn.click()
            time.sleep(0.5)
            shot(page, "12-generators-expanded.png", full_page=True)
        except Exception:
            pass

    # Open add-table modal — optional screenshot
    if try_click(page, "button:has-text('Add Table')"):
        try:
            page.wait_for_selector("input[placeholder='users']", timeout=5_000)
            shot(page, "13-add-table-modal.png")
        except Exception:
            pass
        try_dismiss_modal(page)


def step_data_mappings(page: Page, base_url: str, ws_id: int) -> None:
    nav(page, f"{base_url}/workspaces/{ws_id}/mappings")
    wait_loading_done(page)
    shot(page, "14-data-mapping-wizard.png")

    # Click the first connection card to advance the wizard
    try:
        card = page.query_selector(".card button, .cursor-pointer, [role='button']")
        if card:
            card.click()
            time.sleep(0.5)
            shot(page, "15-data-mapping-columns.png")
    except Exception:
        pass


def step_jobs(page: Page, base_url: str, ws_id: int) -> None:
    nav(page, f"{base_url}/workspaces/{ws_id}/jobs")
    wait_loading_done(page)
    shot(page, "16-jobs-list.png")

    # Open the "Run New Job" modal — optional
    if try_click(page, "button:has-text('Run New Job')"):
        try:
            page.wait_for_selector("input[placeholder*='Mask Production']", timeout=5_000)
            shot(page, "17-run-job-modal.png")
            selects = page.query_selector_all(".modal-body select.form-control, dialog select.form-control")
            if len(selects) >= 2:
                selects[0].select_option(index=0)
                selects[1].select_option(index=0)
                shot(page, "18-run-job-filled.png")
        except Exception:
            pass
        try_dismiss_modal(page)

    # Expand logs for an existing job if present
    try:
        log_btn = page.query_selector("button:has-text('View Logs')")
        if log_btn:
            log_btn.click()
            time.sleep(0.5)
            shot(page, "19-job-logs.png", full_page=True)
    except Exception:
        pass


def step_sensitivity_rules(page: Page, base_url: str) -> None:
    nav(page, f"{base_url}/settings/sensitivity-rules")
    shot(page, "20-sensitivity-rules.png")

    new_btn = page.query_selector(
        "button:has-text('New Rule'), button:has-text('＋ New'), button:has-text('Create')"
    )
    if new_btn:
        new_btn.click()
        time.sleep(0.4)
        shot(page, "21-new-pii-rule-drawer.png")

        name_input = page.query_selector("input[placeholder*='ule name'], input[placeholder*='Name']")
        if name_input:
            name_input.fill("EMPLOYEE_ID")

        add_matcher = page.query_selector("button:has-text('Add Matcher'), button:has-text('Matcher')")
        if add_matcher:
            add_matcher.click()
            time.sleep(0.2)
            matcher_val = page.query_selector("input[placeholder*='alue'], input[placeholder*='pattern']")
            if matcher_val:
                matcher_val.fill("employee_id")

        shot(page, "22-pii-rule-configured.png")

        close_btn = page.query_selector("button:has-text('Cancel'), button[aria-label='Close']")
        if close_btn:
            close_btn.click()
        else:
            page.keyboard.press("Escape")


# ── Argument parsing ───────────────────────────────────────────────────────────

def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Generate OpenDataMask UI screenshots for documentation.")
    p.add_argument("--url",          default=DEFAULT_URL,       help="Frontend base URL")
    p.add_argument("--api",          default=DEFAULT_API,       help="Backend API base URL")
    p.add_argument("--username",     default=DEFAULT_USERNAME,  help="Account username")
    p.add_argument("--password",     default=DEFAULT_PASSWORD,  help="Account password")
    p.add_argument("--email",        default=DEFAULT_EMAIL,     help="Account email (for registration)")
    p.add_argument("--output-dir",   default=str(DEFAULT_OUT),  help="Directory to write screenshots into")
    p.add_argument("--headed",       action="store_true",       help="Show browser window")
    p.add_argument("--no-register",  action="store_true",       help="Skip registration (account already exists)")
    p.add_argument("--no-api-setup", action="store_true",       help="Skip API data setup (already seeded)")
    p.add_argument("--ws-id",        type=int, default=0,       help="Workspace ID (when --no-api-setup)")
    p.add_argument("--src-id",       type=int, default=0,       help="Source connection ID (when --no-api-setup)")
    p.add_argument("--tgt-id",       type=int, default=0,       help="Target connection ID (when --no-api-setup)")
    p.add_argument("--job-id",       type=int, default=0,       help="Job ID (when --no-api-setup)")
    # DB connection params (used by API setup, not the browser)
    p.add_argument("--source-host",  default="source_db",   help="Source DB Docker hostname")
    p.add_argument("--source-db",    default="source_db",   help="Source DB name")
    p.add_argument("--source-user",  default="source_user")
    p.add_argument("--source-pass",  default="source_pass")
    p.add_argument("--target-host",  default="target_db",   help="Target DB Docker hostname")
    p.add_argument("--target-db",    default="target_db",   help="Target DB name")
    p.add_argument("--target-user",  default="target_user")
    p.add_argument("--target-pass",  default="target_pass")
    p.add_argument("--run-job",      action="store_true",   help="Trigger a masking job and wait for completion")
    return p.parse_args()


# ── Entry point ───────────────────────────────────────────────────────────────

def main() -> int:
    args = parse_args()
    global _output_dir, _api_base
    _output_dir = pathlib.Path(args.output_dir)
    _output_dir.mkdir(parents=True, exist_ok=True)
    _api_base   = args.api

    print(f"\nOpenDataMask Screenshot Generator")
    print(f"  Frontend : {args.url}")
    print(f"  API      : {_api_base}")
    print(f"  Output   : {_output_dir.resolve()}")
    print(f"  Browser  : {'headed' if args.headed else 'headless'}\n")

    # ── Phase 1: REST API data setup ───────────────────────────────────────
    ws_id  = args.ws_id
    src_id = args.src_id
    tgt_id = args.tgt_id
    token  = ""

    if not args.no_api_setup:
        print("→ Setting up demo data via API…")
        token, ws_id, src_id, tgt_id = api_setup(
            args.username, args.password, args.email,
            args.source_host, args.source_db, args.source_user, args.source_pass,
            args.target_host, args.target_db, args.target_user, args.target_pass,
            do_register=not args.no_register,
        )
        print(f"  Created: workspace={ws_id}, source={src_id}, target={tgt_id}")

        if args.run_job:
            print("→ Running masking job…")
            job_id = api_run_job(token, ws_id, src_id, tgt_id)
            print(f"  Job {job_id} started, polling…")
            status = api_wait_for_job(token, ws_id, job_id)
            print(f"  Job finished: {status}")

    # ── Phase 2: browser screenshots ───────────────────────────────────────
    print("\n→ Taking screenshots…")
    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=not args.headed)
        page    = browser.new_page(viewport=VIEWPORT)
        page.set_default_timeout(30_000)

        try:
            # Auth pages — must succeed for subsequent steps
            step_login_page(page, args.url)
            step_register_page(page, args.url)
            step_login_filled(page, args.url, args.username, args.password)
        except Exception as exc:
            print(f"\n[WARN] Auth step failed: {exc}")
            try:
                shot(page, "error-state.png", full_page=True)
            except Exception:
                pass
            browser.close()
            count = len(list(_output_dir.glob("*.png")))
            print(f"\n✓ {count} screenshots saved to {_output_dir.resolve()}")
            return 0

        # Remaining sections — each is independent; failure doesn't block others
        for label, fn, kwargs in [
            ("workspaces",    step_workspaces_list,   {"base_url": args.url}),
            ("overview",      step_workspace_overview, {"base_url": args.url, "ws_id": ws_id}),
            ("connections",   step_connections,        {"base_url": args.url, "ws_id": ws_id}),
            ("tables",        step_tables,             {"base_url": args.url, "ws_id": ws_id}),
            ("data-mappings", step_data_mappings,      {"base_url": args.url, "ws_id": ws_id}),
            ("jobs",          step_jobs,               {"base_url": args.url, "ws_id": ws_id}),
            ("pii-rules",     step_sensitivity_rules,  {"base_url": args.url}),
        ]:
            try:
                fn(page, **kwargs)
            except Exception as exc:
                print(f"  [WARN] {label} step failed: {exc}")
                try:
                    shot(page, f"error-{label}.png", full_page=True)
                except Exception:
                    pass

        browser.close()

    count = len(list(_output_dir.glob("*.png")))
    print(f"\n✓ {count} screenshots saved to {_output_dir.resolve()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())