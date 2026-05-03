# Scripts

## update_vpnbook_password.py

Scrapes the current OpenVPN password from vpnbook.com and updates the Firebase Remote Config `password` parameter if they differ.

### One-time setup

**1. Generate a service account key**

Open [Firebase Console](https://console.firebase.google.com/project/ether-cc1ac/settings/serviceaccounts/adminsdk) → Project Settings → Service Accounts → **Generate new private key**. Save the downloaded JSON as:

```
scripts/credentials/service-account.json
```

This path is gitignored — never commit the key.

**2. Install dependencies**

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r scripts/requirements.txt
```

**3. Set credentials path**

```bash
export GOOGLE_APPLICATION_CREDENTIALS="$(pwd)/scripts/credentials/service-account.json"
```

### Usage

```bash
# Dry run — shows what would change without writing anything
python scripts/update_vpnbook_password.py --dry-run -v

# Live update
python scripts/update_vpnbook_password.py

# Explicit options
python scripts/update_vpnbook_password.py \
  --credentials scripts/credentials/service-account.json \
  --project-id ether-cc1ac \
  --url https://www.vpnbook.com/freevpn/openvpn \
  --dry-run -v
```

### Exit codes

| Code | Meaning |
|------|---------|
| 0    | Already in sync or updated successfully |
| 1    | Scrape failure (vpnbook unreachable or page structure changed) |
| 2    | Auth or Firebase API failure |
| 3    | Scraped value failed format validation |
