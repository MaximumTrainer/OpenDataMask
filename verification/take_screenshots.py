#!/usr/bin/env python3
"""
take_screenshots.py — OpenDataMask UI Screenshot Generator
===========================================================

Drives the OpenDataMask frontend with Playwright, following the same
end-to-end workflow as run_verification.sh, and captures a screenshot at
every significant UI step.  The resulting images are saved to
  docs/website/screenshots/
relative to the repository root (or the directory specified by --output-dir).

Prerequisites
-------------
    pip install playwright
    playwright install chromium

Usage
-----
    # Start the verification environment first, then:
    python3 take_screenshots.py

    # Custom frontend URL and output directory:
    python3 take_screenshots.py --url http://localhost --output-dir /tmp/shots

    # Use an existing account instead of registering a new one:
    python3 take_screenshots.py --username admin --password secret

Environment variables (same defaults as docker-compose.yml)
------------------------------------------------------------
    ODM_URL        Frontend base URL (default: http://localhost)
    ODM_USERNAME   Username for the guide account
    ODM_PASSWORD   Password for the guide account
"""

import argparse
import os
import pathlib
import sys
import time

try:
    from playwright.sync_api import sync_playwright, Page, expect
except ImportError:
    print("ERROR: playwright is not installed.  Run:  pip install playwright && playwright install chromium")
    sys.exit(1)


# ── Configuration ─────────────────────────────────────────────────────────────

SCRIPT_DIR = pathlib.Path(__file__).parent
REPO_ROOT   = SCRIPT_DIR.parent
DEFAULT_OUT = REPO_ROOT / "docs" / "website" / "screenshots"

DEFAULT_URL      = os.getenv("ODM_URL",      "http://localhost")
DEFAULT_USERNAME = os.getenv("ODM_USERNAME", "guide_user")
DEFAULT_PASSWORD = os.getenv("ODM_PASSWORD", "Guide!Pass123")
DEFAULT_EMAIL    = os.getenv("ODM_EMAIL",    "guide@odm-docs.local")

VIEWPORT = {"width": 1440, "height": 900}

# ── Screenshot helper ─────────────────────────────────────────────────────────

_step_index = 0
_output_dir: pathlib.Path = DEFAULT_OUT


def shot(page: Page, filename: str, *, full_page: bool = False) -> None:
    """Take a screenshot and print progress."""
    global _step_index
    _step_index += 1
    dest = _output_dir / filename
    page.screenshot(path=str(dest), full_page=full_page)
    print(f"  [{_step_index:02d}] {dest.name}")


def wait_for_load(page: Page, timeout: int = 10_000) -> None:
    """Wait for network idle so the page is fully rendered."""
    page.wait_for_load_state("networkidle", timeout=timeout)


# ── Workflow steps ────────────────────────────────────────────────────────────

def step_login_page(page: Page, base_url: str) -> None:
    page.goto(f"{base_url}/login", wait_until="networkidle")
    shot(page, "01-login-page.png")


def step_register(page: Page, base_url: str, username: str, password: str, email: str) -> None:
    page.goto(f"{base_url}/register", wait_until="networkidle")
    shot(page, "02-register-page.png")

    page.fill("input[placeholder*='sername'], input[name='username']", username)
    page.fill("input[placeholder*='mail'], input[name='email']", email)
    page.fill("input[type='password'][name='password'], input[placeholder*='assword']", password)
    shot(page, "03-register-filled.png")

    page.click("button[type='submit']")
    # May redirect to login or directly to workspaces
    page.wait_for_url(lambda url: "/login" in url or "/workspaces" in url, timeout=10_000)


def step_sign_in(page: Page, base_url: str, username: str, password: str) -> None:
    if "/login" in page.url:
        pass  # already on login page
    else:
        page.goto(f"{base_url}/login", wait_until="networkidle")

    page.fill("input[placeholder*='sername'], input[name='username']", username)
    page.fill("input[type='password'], input[name='password']", password)
    shot(page, "04-login-filled.png")

    page.click("button[type='submit']")
    page.wait_for_url(lambda url: "/workspaces" in url, timeout=15_000)
    wait_for_load(page)
    shot(page, "05-workspaces-empty.png")


def step_create_workspace(page: Page) -> str:
    """Create a workspace and return the workspace URL."""
    # Click "New Workspace" or "+" button
    page.click("button:has-text('New Workspace'), button:has-text('Workspace'), button:has-text('＋')")
    page.wait_for_selector("input[placeholder*='orkspace'], input[name='name']", timeout=5_000)
    shot(page, "06-create-workspace-modal.png")

    page.fill("input[placeholder*='orkspace name'], input[name='name']", "Verification Workspace")

    # Fill description if field exists
    desc_sel = "textarea[name='description'], textarea[placeholder*='escription']"
    if page.query_selector(desc_sel):
        page.fill(desc_sel, "PII masking demo — follows the verification workflow")

    shot(page, "07-create-workspace-filled.png")
    page.click("button[type='submit'], button:has-text('Create'), button:has-text('Save')")
    wait_for_load(page)
    shot(page, "08-workspace-created.png")

    # Navigate into the workspace
    page.click("a:has-text('Verification Workspace'), text=Verification Workspace")
    wait_for_load(page)
    shot(page, "09-workspace-overview.png")
    return page.url


def step_connections(page: Page, ws_url: str,
                     source_host: str, source_db: str, source_user: str, source_pass: str,
                     target_host: str, target_db: str, target_user: str, target_pass: str) -> None:
    # Navigate to Connections tab
    page.click("a:has-text('Connections'), button:has-text('Connections'), [href*='/connections']")
    wait_for_load(page)
    shot(page, "10-connections-empty.png")

    # ── Add source connection ───────────────────────────────────────────────
    page.click("button:has-text('Add Connection'), button:has-text('＋')")
    page.wait_for_selector("input[name='name'], input[placeholder*='onnection name']", timeout=5_000)
    shot(page, "11-add-connection-modal.png")

    page.fill("input[name='name'], input[placeholder*='onnection name']", "source-db")

    # Select PostgreSQL type
    type_sel = "select[name='type'], [data-testid='connection-type']"
    if page.query_selector(type_sel):
        page.select_option(type_sel, "POSTGRESQL")

    page.fill("input[name='host'], input[placeholder*='ost']", source_host)
    page.fill("input[name='database'], input[placeholder*='atabase']", source_db)
    page.fill("input[name='username'], input[placeholder*='sername']", source_user)
    page.fill("input[name='password'][type='password']", source_pass)

    # Check "Source" role
    src_chk = page.query_selector("input[type='checkbox'][name*='ource'], label:has-text('Source') input")
    if src_chk:
        if not src_chk.is_checked():
            src_chk.click()

    shot(page, "12-source-connection-filled.png")
    page.click("button[type='submit'], button:has-text('Save'), button:has-text('Add')")
    wait_for_load(page)
    shot(page, "13-source-connection-saved.png")

    # ── Add destination connection ──────────────────────────────────────────
    page.click("button:has-text('Add Connection'), button:has-text('＋')")
    page.wait_for_selector("input[name='name'], input[placeholder*='onnection name']", timeout=5_000)

    page.fill("input[name='name'], input[placeholder*='onnection name']", "target-db")

    type_sel2 = "select[name='type'], [data-testid='connection-type']"
    if page.query_selector(type_sel2):
        page.select_option(type_sel2, "POSTGRESQL")

    page.fill("input[name='host'], input[placeholder*='ost']", target_host)
    page.fill("input[name='database'], input[placeholder*='atabase']", target_db)
    page.fill("input[name='username'], input[placeholder*='sername']", target_user)
    page.fill("input[name='password'][type='password']", target_pass)

    # Check "Destination" role
    dst_chk = page.query_selector("input[type='checkbox'][name*='estination'], label:has-text('Destination') input")
    if dst_chk:
        if not dst_chk.is_checked():
            dst_chk.click()

    shot(page, "14-destination-connection-filled.png")
    page.click("button[type='submit'], button:has-text('Save'), button:has-text('Add')")
    wait_for_load(page)
    shot(page, "15-connections-configured.png")


def step_tables(page: Page) -> None:
    page.click("a:has-text('Tables'), button:has-text('Tables'), [href*='/tables']")
    wait_for_load(page)
    shot(page, "16-tables-empty.png")

    # Add table configuration
    page.click("button:has-text('Add Table'), button:has-text('＋')")
    page.wait_for_selector("input[name='tableName'], input[placeholder*='able']", timeout=5_000)
    shot(page, "17-add-table-modal.png")

    page.fill("input[name='tableName'], input[placeholder*='able name']", "users")

    # Select MASK mode
    mode_sel = "select[name='mode']"
    if page.query_selector(mode_sel):
        page.select_option(mode_sel, "MASK")

    shot(page, "18-table-config-filled.png")
    page.click("button[type='submit'], button:has-text('Save'), button:has-text('Add')")
    wait_for_load(page)
    shot(page, "19-table-saved.png")

    # Expand the table to configure column generators
    page.click("button:has-text('Columns'), button:has-text('▼'), [data-action='expand']")
    time.sleep(0.5)
    shot(page, "20-table-expanded.png")

    # Add full_name generator
    add_col_btn = page.query_selector("button:has-text('Add Column'), button:has-text('＋ Add')")
    if add_col_btn:
        add_col_btn.click()
        page.wait_for_selector("input[name='columnName'], input[placeholder*='olumn']", timeout=5_000)
        page.fill("input[name='columnName'], input[placeholder*='olumn']", "full_name")
        gen_sel = "select[name='generatorType'], select[name='type']"
        if page.query_selector(gen_sel):
            page.select_option(gen_sel, "FULL_NAME")
        page.click("button[type='submit'], button:has-text('Save'), button:has-text('Add')")
        wait_for_load(page)

    shot(page, "21-generators-configured.png", full_page=True)


def step_data_mapping(page: Page) -> None:
    page.click("a:has-text('Data Mapping'), a:has-text('Mappings'), [href*='/mappings']")
    wait_for_load(page)
    shot(page, "22-data-mapping-step1.png")

    # Click first connection card if visible
    conn_card = page.query_selector(".connection-card, [data-type='connection']")
    if conn_card:
        conn_card.click()
        wait_for_load(page)
        shot(page, "23-data-mapping-step2-tables.png")

        # Click the users table card
        table_card = page.query_selector("text=users")
        if table_card:
            table_card.click()
            wait_for_load(page)
            shot(page, "24-data-mapping-step3-columns.png", full_page=True)


def step_jobs(page: Page) -> None:
    page.click("a:has-text('Jobs'), button:has-text('Jobs'), [href*='/jobs']")
    wait_for_load(page)
    shot(page, "25-jobs-empty.png")

    # Click "Run New Job"
    page.click("button:has-text('Run'), button:has-text('New Job'), button:has-text('⚙')")
    page.wait_for_selector("input[name='name'], [placeholder*='ob name']", timeout=5_000)
    shot(page, "26-run-job-modal.png")

    # Fill job name
    name_input = page.query_selector("input[name='name'], [placeholder*='ob name']")
    if name_input:
        name_input.fill("Verification Masking Job")

    # Select source connection
    src_sel = page.query_selector("select[name='sourceConnectionId'], [data-testid='source-connection']")
    if src_sel:
        src_sel.select_option(index=1)

    # Select destination connection
    dst_sel = page.query_selector("select[name='destinationConnectionId'], [data-testid='destination-connection']")
    if dst_sel:
        dst_sel.select_option(index=1)

    shot(page, "27-run-job-filled.png")
    page.click("button[type='submit'], button:has-text('Run'), button:has-text('Start')")
    wait_for_load(page)
    shot(page, "28-job-running.png")

    # Wait for job to complete (up to 60s)
    print("  Waiting for masking job to complete…")
    for _ in range(12):
        time.sleep(5)
        page.reload()
        wait_for_load(page)
        completed = page.query_selector(".badge:has-text('COMPLETED'), [data-status='COMPLETED'], text=COMPLETED")
        if completed:
            break

    shot(page, "29-job-completed.png", full_page=True)

    # Expand logs
    log_btn = page.query_selector("button:has-text('View Logs'), button:has-text('Logs')")
    if log_btn:
        log_btn.click()
        time.sleep(0.3)
        shot(page, "30-job-logs.png", full_page=True)


def step_sensitivity_rules(page: Page, base_url: str) -> None:
    page.goto(f"{base_url}/settings/sensitivity-rules", wait_until="networkidle")
    shot(page, "31-sensitivity-rules.png")

    # Open create drawer
    new_btn = page.query_selector("button:has-text('New Rule'), button:has-text('＋')")
    if new_btn:
        new_btn.click()
        time.sleep(0.3)
        shot(page, "32-add-pii-rule-drawer.png")

        # Fill name
        name_input = page.query_selector("input[name='name'], input[placeholder*='ule name']")
        if name_input:
            name_input.fill("INTERNAL_EMPLOYEE_ID")

        # Add matcher
        add_matcher_btn = page.query_selector("button:has-text('Add Matcher'), button:has-text('Matcher')")
        if add_matcher_btn:
            add_matcher_btn.click()
            time.sleep(0.2)
            matcher_val = page.query_selector("input[placeholder*='alue'], input[name='matcherValue']")
            if matcher_val:
                matcher_val.fill("employee_id")

        shot(page, "33-pii-rule-configured.png")

        # Close drawer without saving (this is just a screenshot demo)
        esc = page.query_selector("button:has-text('Cancel'), button[aria-label='Close']")
        if esc:
            esc.click()
        else:
            page.keyboard.press("Escape")


# ── Entry point ───────────────────────────────────────────────────────────────

def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Generate OpenDataMask UI screenshots for documentation.")
    p.add_argument("--url",        default=DEFAULT_URL,      help="Frontend base URL (default: %(default)s)")
    p.add_argument("--username",   default=DEFAULT_USERNAME, help="Account username")
    p.add_argument("--password",   default=DEFAULT_PASSWORD, help="Account password")
    p.add_argument("--email",      default=DEFAULT_EMAIL,    help="Account email (for registration)")
    p.add_argument("--output-dir", default=str(DEFAULT_OUT), help="Directory to write screenshots into")
    p.add_argument("--headed",     action="store_true",      help="Show browser window (default: headless)")
    p.add_argument("--no-register", action="store_true",     help="Skip registration (account already exists)")
    # Source DB defaults match verification/docker-compose.yml
    p.add_argument("--source-host", default="localhost", help="Source DB host")
    p.add_argument("--source-db",   default="source_db", help="Source DB name")
    p.add_argument("--source-user", default="source_user")
    p.add_argument("--source-pass", default="source_pass")
    p.add_argument("--target-host", default="localhost")
    p.add_argument("--target-db",   default="target_db")
    p.add_argument("--target-user", default="target_user")
    p.add_argument("--target-pass", default="target_pass")
    return p.parse_args()


def main() -> int:
    args = parse_args()
    global _output_dir
    _output_dir = pathlib.Path(args.output_dir)
    _output_dir.mkdir(parents=True, exist_ok=True)

    print(f"\nOpenDataMask Screenshot Generator")
    print(f"  Frontend : {args.url}")
    print(f"  Output   : {_output_dir.resolve()}")
    print(f"  Browser  : {'headed' if args.headed else 'headless'}\n")

    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=not args.headed)
        page = browser.new_page(viewport=VIEWPORT)
        page.set_default_timeout(15_000)

        try:
            step_login_page(page, args.url)

            if not args.no_register:
                step_register(page, args.url, args.username, args.password, args.email)

            step_sign_in(page, args.url, args.username, args.password)
            step_create_workspace(page)
            step_connections(
                page, page.url,
                args.source_host, args.source_db, args.source_user, args.source_pass,
                args.target_host, args.target_db, args.target_user, args.target_pass,
            )
            step_tables(page)
            step_data_mapping(page)
            step_jobs(page)
            step_sensitivity_rules(page, args.url)

        except Exception as exc:
            print(f"\n[WARN] Screenshot step failed: {exc}")
            print("       Partial screenshots may have been saved.")
            shot(page, "error-state.png", full_page=True)
        finally:
            browser.close()

    count = len(list(_output_dir.glob("*.png")))
    print(f"\n✓ {count} screenshots saved to {_output_dir.resolve()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
