#!/usr/bin/env python3
"""
Scrapes the current OpenVPN password from vpnbook.com and updates the
Firebase Remote Config 'password' parameter if it differs.

Usage:
    python scripts/update_vpnbook_password.py [--dry-run] [-v]

Exit codes:
    0 - no change needed or update succeeded
    1 - scrape failure
    2 - auth / Firebase API failure
    3 - scraped value failed validation
"""

import argparse
import json
import os
import re
import sys
import logging

import requests
from bs4 import BeautifulSoup
from google.auth.transport.requests import Request
from google.oauth2 import service_account

VPNBOOK_URL = "https://www.vpnbook.com/freevpn/openvpn"
FIREBASE_PROJECT_ID = "ether-cc1ac"
REMOTE_CONFIG_SCOPE = "https://www.googleapis.com/auth/firebase.remoteconfig"
REMOTE_CONFIG_BASE = "https://firebaseremoteconfig.googleapis.com/v1/projects/{project_id}/remoteConfig"
PASSWORD_PATTERN = re.compile(r"^[a-z0-9]{5,12}$")

log = logging.getLogger(__name__)


def scrape_vpnbook_password(url: str) -> str:
    headers = {
        "User-Agent": (
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/124.0.0.0 Safari/537.36"
        )
    }
    resp = requests.get(url, headers=headers, timeout=15)
    if resp.status_code != 200:
        log.error("vpnbook returned HTTP %s", resp.status_code)
        sys.exit(1)

    soup = BeautifulSoup(resp.text, "html.parser")

    # Strategy 1: find the label "PASSWORD" in the credentials card, then
    # walk to the sibling/child that holds the actual value.
    password = _parse_structured(soup)

    # Strategy 2: regex fallback over the raw HTML
    if not password:
        log.debug("Structured parse failed; trying regex fallback")
        password = _parse_regex(resp.text)

    if not password:
        log.error(
            "Could not locate password in vpnbook page. "
            "The page structure may have changed."
        )
        sys.exit(1)

    return password


def _parse_structured(soup: BeautifulSoup) -> str | None:
    """
    vpnbook renders:
      <div class="rounded-lg border ...">
        <label ...>Password</label>
        <div ...><code ...>ke9zw74</code>...</div>
      </div>

    Find the Password label, then grab the <code> in the sibling div.
    """
    label = soup.find("label", string=re.compile(r"^\s*Password\s*$", re.I))
    if not label:
        return None
    code = label.find_next("code")
    if code:
        text = code.get_text(strip=True)
        if PASSWORD_PATTERN.match(text):
            return text
    return None


def _parse_regex(html: str) -> str | None:
    # Captures an alphanumeric token that immediately follows a PASSWORD label
    match = re.search(
        r"PASSWORD\s*</[^>]+>\s*(?:<[^>]+>\s*)*([a-z0-9]{5,12})",
        html,
        re.IGNORECASE,
    )
    if match:
        return match.group(1)
    return None


def get_access_token(credentials_path: str) -> str:
    if not os.path.isfile(credentials_path):
        log.error("Service account file not found: %s", credentials_path)
        sys.exit(2)
    creds = service_account.Credentials.from_service_account_file(
        credentials_path, scopes=[REMOTE_CONFIG_SCOPE]
    )
    creds.refresh(Request())
    return creds.token


def fetch_remote_config(project_id: str, token: str) -> tuple[dict, str]:
    url = REMOTE_CONFIG_BASE.format(project_id=project_id)
    resp = requests.get(
        url,
        headers={"Authorization": f"Bearer {token}"},
        timeout=15,
    )
    if resp.status_code != 200:
        log.error("Remote Config GET failed: HTTP %s\n%s", resp.status_code, resp.text)
        sys.exit(2)
    etag = resp.headers.get("ETag", "*")
    return resp.json(), etag


def update_remote_config(
    project_id: str, token: str, template: dict, etag: str
) -> dict:
    url = REMOTE_CONFIG_BASE.format(project_id=project_id)
    resp = requests.put(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json; UTF-8",
            "If-Match": etag,
        },
        data=json.dumps(template),
        timeout=15,
    )
    if resp.status_code == 412:
        log.error(
            "ETag mismatch (412) — someone else modified the template between "
            "GET and PUT. Re-run the script to retry."
        )
        sys.exit(2)
    if resp.status_code not in (200, 201):
        log.error("Remote Config PUT failed: HTTP %s\n%s", resp.status_code, resp.text)
        sys.exit(2)
    return resp.json()


def get_rc_password(template: dict) -> str | None:
    try:
        return template["parameters"]["password"]["defaultValue"]["value"]
    except KeyError:
        return None


def set_rc_password(template: dict, new_password: str) -> None:
    template.setdefault("parameters", {}).setdefault("password", {}).setdefault(
        "defaultValue", {}
    )["value"] = new_password


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="Skip the PUT request")
    parser.add_argument("--project-id", default=FIREBASE_PROJECT_ID)
    parser.add_argument(
        "--credentials",
        default=os.environ.get("GOOGLE_APPLICATION_CREDENTIALS", ""),
        help="Path to service account JSON (default: $GOOGLE_APPLICATION_CREDENTIALS)",
    )
    parser.add_argument("--url", default=VPNBOOK_URL)
    parser.add_argument("-v", "--verbose", action="store_true")
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(levelname)s  %(message)s",
    )

    if not args.credentials:
        log.error(
            "No credentials path specified. Set $GOOGLE_APPLICATION_CREDENTIALS "
            "or use --credentials."
        )
        sys.exit(2)

    log.info("Scraping password from %s ...", args.url)
    scraped = scrape_vpnbook_password(args.url)

    if not PASSWORD_PATTERN.match(scraped):
        log.error(
            "Scraped value '%s' does not look like a valid vpnbook password.", scraped
        )
        sys.exit(3)

    log.info("Scraped password: %s", scraped)

    log.info("Fetching Firebase Remote Config (project: %s) ...", args.project_id)
    token = get_access_token(args.credentials)
    template, etag = fetch_remote_config(args.project_id, token)

    current = get_rc_password(template)
    log.info("Current Remote Config password: %s", current or "<not set>")

    if current == scraped:
        log.info("Already in sync — no update needed.")
        return

    log.info("Update required: '%s' → '%s'", current, scraped)

    if args.dry_run:
        log.info("[dry-run] Would have updated the password. No changes written.")
        return

    set_rc_password(template, scraped)
    result = update_remote_config(args.project_id, token, template, etag)
    new_version = result.get("version", {}).get("versionNumber", "?")
    log.info("Remote Config updated. New version: %s", new_version)


if __name__ == "__main__":
    main()
