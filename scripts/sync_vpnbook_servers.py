#!/usr/bin/env python3
"""
sync_vpnbook_servers.py
=======================
Automates the full pipeline:
  1. Scrapes server metadata from https://www.vpnbook.com/freevpn/openvpn
  2. Downloads the TCP-443 .ovpn file for every discovered server via the API:
       https://www.vpnbook.com/api/openvpn?hostname={hostname}&protocol=tcp443&ip={ip}
  3. Parses each .ovpn file into a config string
  4. Builds the Firebase RTDB JSON payload (same schema as rtdb_import.json)
  5. Uploads the payload to Firebase RTDB, bumping ovpn_cache_version

Usage
-----
  python3 scripts/sync_vpnbook_servers.py [--dry-run] [--output PATH]

Requirements
------------
  pip install requests firebase-admin

Environment / config
--------------------
  FIREBASE_SERVICE_ACCOUNT_JSON  – path to a service-account key JSON file
                                   (falls back to GOOGLE_APPLICATION_CREDENTIALS
                                    or Application Default Credentials)
  FIREBASE_DATABASE_URL          – RTDB URL
"""

import argparse
import json
import logging
import os
import re
import sys
from pathlib import Path

import requests

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

VPNBOOK_PAGE_URL  = "https://www.vpnbook.com/freevpn/openvpn"
VPNBOOK_OVPN_URL  = (
    "https://www.vpnbook.com/api/openvpn"
    "?hostname={hostname}&protocol=tcp443&ip={ip}"
)

# Fallback server list — keeps keys in country order, same as rtdb_import.json
# Format: (server_id, hostname, ip_address, country_key)
FALLBACK_SERVERS = [
    ("us16",  "us16.vpnbook.com",  "147.135.15.16",  "usa"),
    ("us178", "us178.vpnbook.com", "147.135.37.178", "usa"),
    ("ca149", "ca149.vpnbook.com", "144.217.253.149", "canada"),
    ("ca196", "ca196.vpnbook.com", "142.4.216.196",  "canada"),
    ("uk205", "uk205.vpnbook.com", "145.239.252.205", "uk"),
    ("uk68",  "uk68.vpnbook.com",  "145.239.255.68", "uk"),
    ("de20",  "de20.vpnbook.com",  "51.75.145.20",   "germany"),
    ("de220", "de220.vpnbook.com", "51.75.145.220",  "germany"),
    ("fr200", "fr200.vpnbook.com", "5.196.64.200",   "france"),
    ("fr231", "fr2311.vpnbook.com","5.196.64.231",   "france"),
]

COUNTRY_CODE_MAP = {
    "US": "usa",
    "CA": "canada",
    "GB": "uk",
    "DE": "germany",
    "FR": "france",
    "PL": "poland",
}

DEFAULT_PROJECT_ID = os.environ.get("FIREBASE_PROJECT_ID", "")
FIREBASE_DATABASE_URL = os.environ.get(
    "FIREBASE_DATABASE_URL",
    f"https://{DEFAULT_PROJECT_ID}-default-rtdb.asia-southeast1.firebasedatabase.app" if DEFAULT_PROJECT_ID else ""
)

REQUEST_TIMEOUT = 30
REQUEST_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/125.0.0.0 Safari/537.36"
    ),
    "Referer": "https://www.vpnbook.com/freevpn/openvpn",
}

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Step 1 – Discover servers from vpnbook.com
# ---------------------------------------------------------------------------

def scrape_servers():
    """
    Returns a list of (server_id, hostname, ip, country_key) tuples scraped
    from the VPNBook OpenVPN page JSON embed.
    Falls back to FALLBACK_SERVERS if the page cannot be parsed.
    """
    log.info("Scraping server list from %s …", VPNBOOK_PAGE_URL)
    try:
        resp = requests.get(
            VPNBOOK_PAGE_URL,
            headers=REQUEST_HEADERS,
            timeout=REQUEST_TIMEOUT
        )
        resp.raise_for_status()
    except requests.RequestException as exc:
        log.warning("Could not fetch server page (%s) – using fallback list.", exc)
        return list(FALLBACK_SERVERS)

    # The page embeds Next.js RSC JSON with server objects like:
    # {"id":"us16","name":"US Server 1","hostname":"us16.vpnbook.com",
    #  "ipAddress":"147.135.15.16","countryCode":"US","countryName":"United States"}
    pattern = re.compile(
        r'"id"\s*:\s*"([a-z0-9]+)"'
        r'.*?"hostname"\s*:\s*"([^"]+)"'
        r'.*?"ipAddress"\s*:\s*"([^"]+)"'
        r'.*?"countryCode"\s*:\s*"([A-Z]{2})"',
        re.DOTALL
    )

    servers = []
    seen_ids: set[str] = set()

    for m in pattern.finditer(resp.text):
        server_id, hostname, ip, cc = m.group(1), m.group(2), m.group(3), m.group(4)
        if server_id not in seen_ids:
            seen_ids.add(server_id)
            country_key = COUNTRY_CODE_MAP.get(cc, cc.lower())
            servers.append((server_id, hostname, ip, country_key))

    if servers:
        log.info("Scraped %d servers from page.", len(servers))
        return servers

    log.warning("Could not parse server list from page – using fallback list.")
    return list(FALLBACK_SERVERS)


def scrape_credentials(html_text=None):
    """
    Scrapes the global OpenVPN username and password from vpnbook.com/freevpn/openvpn.
    Returns (username, password) tuple.
    """
    log.info("Scraping VPN credentials from %s …", VPNBOOK_PAGE_URL)
    if not html_text:
        try:
            resp = requests.get(
                VPNBOOK_PAGE_URL,
                headers=REQUEST_HEADERS,
                timeout=REQUEST_TIMEOUT
            )
            resp.raise_for_status()
            html_text = resp.text
        except requests.RequestException as exc:
            log.warning("Could not fetch page for credentials (%s) – using env var fallbacks.", exc)
            return (os.environ.get("VPN_USERNAME", ""), os.environ.get("VPN_PASSWORD", ""))

    user_m = re.search(r'>Username</label>.*?<code[^>]*>([^<]+)</code>', html_text, re.DOTALL | re.IGNORECASE)
    pass_m = re.search(r'>Password</label>.*?<code[^>]*>([^<]+)</code>', html_text, re.DOTALL | re.IGNORECASE)

    username = user_m.group(1).strip() if user_m else os.environ.get("VPN_USERNAME", "")
    password = pass_m.group(1).strip() if pass_m else os.environ.get("VPN_PASSWORD", "")
    log.info("Scraped VPN credentials → username: '%s', password: '%s'", username, password)
    return username, password


# ---------------------------------------------------------------------------
# Step 2 – Build RTDB key (e.g. "usa-1", "uk-2")
# ---------------------------------------------------------------------------

def build_rtdb_key(country_key, index):
    return f"{country_key}-{index}"


# ---------------------------------------------------------------------------
# Step 3 – Download .ovpn config directly from the VPNBook API
# ---------------------------------------------------------------------------

def download_ovpn_config(server_id, hostname, ip):
    """
    Downloads the .ovpn file for the given server via the VPNBook API.
    Returns the file contents as a string, or None on failure.
    """
    url = VPNBOOK_OVPN_URL.format(hostname=hostname, ip=ip)
    log.info("  ↓  %s  →  %s …", server_id, url)
    try:
        resp = requests.get(
            url,
            headers=REQUEST_HEADERS,
            timeout=REQUEST_TIMEOUT,
        )
        resp.raise_for_status()
    except requests.RequestException as exc:
        log.error("  ✗  Failed to download %s: %s", server_id, exc)
        return None

    content_type = resp.headers.get("content-type", "")
    if "openvpn" not in content_type and "octet" not in content_type and "text" not in content_type:
        log.warning("  ⚠  Unexpected content-type for %s: %s", server_id, content_type)

    text = resp.content.decode("utf-8", errors="replace")
    if not text.strip().startswith("client"):
        log.warning("  ⚠  %s: response doesn't look like an OpenVPN config (starts with: %r)",
                    server_id, text[:60])

    # Normalise line endings
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    log.info("  ✓  Got %d chars for %s", len(text), server_id)
    return text


# ---------------------------------------------------------------------------
# Step 4 – Build RTDB payload
# ---------------------------------------------------------------------------

def build_rtdb_payload(current_version, server_configs, username="", password=""):
    return {
        "ovpn_cache_version": current_version + 1,
        "ovpn": server_configs,
        "username": username,
        "password": password,
    }


# ---------------------------------------------------------------------------
# Firebase helpers
# ---------------------------------------------------------------------------

def resolve_cred_path(sa_json_input=None):
    """
    Resolves Firebase Service Account credentials path.
    Supports either:
      - A file path passed via CLI (--sa-json) or env vars (FIREBASE_SERVICE_ACCOUNT_JSON, GOOGLE_APPLICATION_CREDENTIALS)
      - A raw JSON string containing the service account private key injected via env vars (e.g. in GitHub Actions secrets)
    """
    import tempfile
    candidate = (sa_json_input
                 or os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON")
                 or os.environ.get("FIREBASE_SERVICE_ACCOUNT_KEY")
                 or os.environ.get("GOOGLE_APPLICATION_CREDENTIALS"))
    if not candidate:
        return None
    candidate = str(candidate).strip()
    if candidate.startswith("{") and "private_key" in candidate:
        tmp = tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".json", encoding="utf-8")
        tmp.write(candidate)
        tmp.close()
        log.info("Resolved service account JSON string from environment variable into temporary file.")
        return tmp.name
    if Path(candidate).exists():
        return candidate
    return None


def get_firebase_tools_args(project_id):
    args = ["--project", project_id]
    token = os.environ.get("FIREBASE_TOKEN")
    if token:
        args.extend(["--token", token])
    return args


def init_firebase(sa_json_path=None):
    import firebase_admin
    from firebase_admin import credentials, db

    if firebase_admin._apps:
        return db

    cred_path = resolve_cred_path(sa_json_path)
    if cred_path and Path(cred_path).exists():
        log.info("Using service-account credentials: %s", cred_path)
        cred = credentials.Certificate(cred_path)
    else:
        log.info("No service-account JSON found – using Application Default Credentials / CLI tokens.")
        cred = credentials.ApplicationDefault()

    firebase_admin.initialize_app(cred, {"databaseURL": FIREBASE_DATABASE_URL})
    return db


def get_current_version(root_ref):
    try:
        val = root_ref.child("ovpn_cache_version").get()
        if isinstance(val, int):
            log.info("Current RTDB ovpn_cache_version = %d", val)
            return val
    except Exception as exc:
        log.warning("Could not read current version: %s", exc)
    return 0


def upload_to_rtdb(payload, sa_json_path=None, project_id=None):
    project_id = project_id or DEFAULT_PROJECT_ID
    try:
        db = init_firebase(sa_json_path)
        ref = db.reference("/")
        ref.set(payload)
        log.info("✅ Successfully uploaded to Firebase RTDB via SDK (version %d).",
                 payload["ovpn_cache_version"])
        return True
    except Exception as exc:
        log.warning("Firebase SDK upload attempt failed (%s). Trying firebase-tools CLI …", exc)
        try:
            import subprocess
            import tempfile
            with tempfile.TemporaryDirectory() as tmpdir:
                tmp_json = Path(tmpdir) / "rtdb_payload.json"
                tmp_json.write_text(json.dumps(payload, ensure_ascii=False, indent=2), "utf-8")
                res = subprocess.run(
                    ["npx", "-y", "firebase-tools@latest", "database:set", "/", str(tmp_json), *get_firebase_tools_args(project_id), "--force"],
                    capture_output=True, text=True
                )
                if res.returncode == 0:
                    log.info("✅ Successfully uploaded to Firebase RTDB via firebase-tools CLI (version %d).",
                             payload["ovpn_cache_version"])
                    return True
                else:
                    log.error("❌ CLI RTDB upload failed: %s", res.stderr)
                    return False
        except Exception as cli_exc:
            log.error("❌ Firebase RTDB upload failed: %s", cli_exc)
            return False


def update_remote_config(username, password, sa_json_path=None, project_id=None):
    """
    Updates the 'username' and 'password' parameters in Firebase Remote Config.
    Attempts:
      1. REST API via google-auth / service-account credentials
      2. Fallback: firebase-tools CLI (npx firebase remoteconfig:get / deploy)
    """
    project_id = project_id or DEFAULT_PROJECT_ID
    log.info("Updating Firebase Remote Config (%s) → username: '%s', password: '%s' …", project_id, username, password)

    # Attempt 1: REST API via google-auth / service account
    try:
        import urllib.request
        from google.oauth2 import service_account
        import google.auth
        from google.auth.transport.requests import Request

        scopes = ["https://www.googleapis.com/auth/firebase.remoteconfig"]
        cred_path = resolve_cred_path(sa_json_path)

        if cred_path and Path(cred_path).exists():
            log.info("Using service account for Remote Config: %s", cred_path)
            cred = service_account.Credentials.from_service_account_file(cred_path, scopes=scopes)
        else:
            cred, _ = google.auth.default(scopes=scopes)

        cred.refresh(Request())
        token = cred.token

        url = f"https://firebaseremoteconfig.googleapis.com/v1/projects/{project_id}/remoteConfig"
        req_get = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
        with urllib.request.urlopen(req_get) as resp:
            etag = resp.headers.get("ETag", "*")
            template = json.loads(resp.read().decode("utf-8"))

        if "parameters" not in template:
            template["parameters"] = {}

        template["parameters"]["username"] = {
            "defaultValue": {"value": username},
            "description": "vpn server username",
            "valueType": "STRING"
        }
        template["parameters"]["password"] = {
            "defaultValue": {"value": password},
            "description": "vpn server password",
            "valueType": "STRING"
        }

        body_bytes = json.dumps(template).encode("utf-8")
        req_put = urllib.request.Request(
            url,
            data=body_bytes,
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json; UTF-8",
                "If-Match": etag
            },
            method="PUT"
        )
        with urllib.request.urlopen(req_put) as resp:
            if resp.status == 200:
                log.info("✅ Successfully updated Remote Config via REST API.")
                return True
    except Exception as exc:
        log.warning("Remote Config REST API attempt failed (%s). Trying firebase-tools CLI …", exc)

    # Attempt 2: Fallback to firebase-tools CLI
    try:
        import subprocess
        import tempfile

        with tempfile.TemporaryDirectory() as tmpdir:
            rc_file = Path(tmpdir) / "remote_config.json"
            fb_json = Path(tmpdir) / "firebase.json"

            res_get = subprocess.run(
                ["npx", "-y", "firebase-tools@latest", "remoteconfig:get", "-o", str(rc_file), *get_firebase_tools_args(project_id)],
                capture_output=True, text=True
            )
            if res_get.returncode != 0 or not rc_file.exists():
                log.error("❌ Failed to fetch Remote Config via CLI: %s", res_get.stderr)
                return False

            template = json.loads(rc_file.read_text("utf-8"))
            if "parameters" not in template:
                template["parameters"] = {}

            template["parameters"]["username"] = {
                "defaultValue": {"value": username},
                "description": "vpn server username",
                "valueType": "STRING"
            }
            template["parameters"]["password"] = {
                "defaultValue": {"value": password},
                "description": "vpn server password",
                "valueType": "STRING"
            }

            rc_file.write_text(json.dumps(template, indent=2), "utf-8")
            fb_json.write_text(json.dumps({"remoteconfig": {"template": "remote_config.json"}}), "utf-8")

            res_deploy = subprocess.run(
                ["npx", "-y", "firebase-tools@latest", "deploy", "--only", "remoteconfig", *get_firebase_tools_args(project_id), "--force"],
                cwd=tmpdir, capture_output=True, text=True
            )
            if res_deploy.returncode == 0:
                log.info("✅ Successfully updated Remote Config via firebase-tools CLI.")
                return True
            else:
                log.error("❌ CLI Remote Config deploy failed: %s", res_deploy.stderr)
                return False
    except Exception as exc:
        log.error("❌ Remote Config CLI attempt failed: %s", exc)
        return False


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Download VPNBook TCP-443 configs and sync credentials & servers to Firebase."
    )
    parser.add_argument(
        "--dry-run", action="store_true",
        help="Build the JSON payload and print summary without uploading."
    )
    parser.add_argument(
        "--output", metavar="PATH",
        help="Save the resulting JSON to this file (e.g. rtdb_import.json)."
    )
    parser.add_argument(
        "--sa-json", metavar="PATH",
        default=resolve_cred_path(),
        help="Path to Firebase service-account JSON key file or raw JSON env string."
    )
    parser.add_argument(
        "--version", type=int, default=None,
        help="Override the base ovpn_cache_version (default: read from RTDB)."
    )
    parser.add_argument(
        "--username", type=str, default=os.environ.get("VPN_USERNAME"),
        help="Override the scraped VPN username."
    )
    parser.add_argument(
        "--password", type=str, default=os.environ.get("VPN_PASSWORD"),
        help="Override the scraped VPN password."
    )
    parser.add_argument(
        "--sync-rc", action="store_true", default=True,
        help="Sync updated credentials to Firebase Remote Config (default: True)."
    )
    parser.add_argument(
        "--no-sync-rc", action="store_false", dest="sync_rc",
        help="Do NOT sync credentials to Firebase Remote Config."
    )
    parser.add_argument(
        "--project-id", type=str, default=DEFAULT_PROJECT_ID,
        help="Firebase project ID for Remote Config updates."
    )
    args = parser.parse_args()

    # ── 1. Discover servers and credentials ──────────────────────────────
    servers = scrape_servers()
    scraped_user, scraped_pass = scrape_credentials()
    username = args.username if args.username else (os.environ.get("VPN_USERNAME") or scraped_user)
    password = args.password if args.password else (os.environ.get("VPN_PASSWORD") or scraped_pass)
    log.info("Processing %d server(s) | credentials → %s / %s", len(servers), username, password)

    # ── 2. Download each ovpn config ─────────────────────────────────────
    country_counters: dict[str, int] = {}
    server_configs: dict[str, str] = {}
    failed: list[str] = []

    for entry in servers:
        server_id, hostname, ip, country_key = entry
        country_counters[country_key] = country_counters.get(country_key, 0) + 1
        rtdb_key = build_rtdb_key(country_key, country_counters[country_key])

        ovpn_text = download_ovpn_config(server_id, hostname, ip)
        if ovpn_text is None:
            failed.append(server_id)
            continue

        server_configs[rtdb_key] = ovpn_text
        log.info("  → stored as '%s'", rtdb_key)

    if not server_configs:
        log.error("No configs downloaded successfully. Aborting.")
        sys.exit(1)

    if failed:
        log.warning("⚠️  %d server(s) failed to download: %s", len(failed), failed)

    # ── 3. Get current version ───────────────────────────────────────────
    current_version = args.version
    if current_version is None:
        if not args.dry_run:
            try:
                db = init_firebase(args.sa_json)
                current_version = get_current_version(db.reference("/"))
            except Exception:
                log.warning("Could not contact Firebase – defaulting version to 0.")
                current_version = 0
        else:
            current_version = 0

    # ── 4. Build payload ─────────────────────────────────────────────────
    payload = build_rtdb_payload(current_version, server_configs, username, password)
    log.info(
        "Payload ready: %d server configs, ovpn_cache_version → %d",
        len(server_configs), payload["ovpn_cache_version"]
    )

    # ── 5. Save to file (optional) ───────────────────────────────────────
    if args.output:
        out_path = Path(args.output)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        with open(out_path, "w", encoding="utf-8") as fh:
            json.dump(payload, fh, ensure_ascii=False, indent=2)
        log.info("Saved JSON payload to %s", out_path)

    # ── 6. Upload to Firebase ────────────────────────────────────────────
    if args.dry_run:
        log.info("--dry-run mode: skipping Firebase upload.")
        summary = {
            "ovpn_cache_version": payload["ovpn_cache_version"],
            "username": payload["username"],
            "password": payload["password"],
            "ovpn_keys": list(payload["ovpn"].keys()),
        }
        print(json.dumps(summary, indent=2))
        sys.exit(0)

    success_rtdb = upload_to_rtdb(payload, args.sa_json, project_id=args.project_id)
    success_rc = True
    if args.sync_rc:
        success_rc = update_remote_config(username, password, sa_json_path=args.sa_json, project_id=args.project_id)

    sys.exit(0 if (success_rtdb and success_rc) else 1)


if __name__ == "__main__":
    main()
