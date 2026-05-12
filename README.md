# Ether VPN

An Android VPN client developed using the [ics-openvpn](https://github.com/schwabe/ics-openvpn) library.

[![Play Store](https://img.shields.io/badge/Download-Play%20Store-green.svg)](https://play.google.com/store/apps/details?id=com.anonymous.ethervpn)

## Demo

<div align="center">

https://github.com/user-attachments/assets/bce5d1c9-2a0a-4f27-a93b-140b631d30ba

</div>

## Architecture Overview

- **Authentication**: Google Sign-In → Firebase Auth
- **Server list**: Firebase Remote Config (`countries` key)
- **OVPN configs**: Firebase Realtime Database — fetched on first launch, cached in internal storage (`files/ovpn/`). Never shipped in the APK or assets.
- **VPN engine**: [ics-openvpn](https://github.com/schwabe/ics-openvpn) as a Git submodule (`:openvpn` module)

## 📋 Prerequisites

- **Android Studio** (latest stable)
- **SWIG 4.x** — required to build the ics-openvpn native layer. Download from [swig.org](https://www.swig.org/download.html) and add to your PATH.
- **Java 1.8**
- A **Firebase project** with the following services enabled:
  - Authentication (Google provider)
  - Realtime Database
  - Remote Config
  - Crashlytics

## 🚀 Setup for a New Developer

### 1. Clone with submodules

```bash
git clone --recurse-submodules <repo-url>
# or if already cloned:
git submodule update --init --recursive
```

### 2. Firebase configuration

- In the Firebase Console, create or open the project.
- Download `google-services.json` and place it at `app/google-services.json` (gitignored — never commit this).
- Enable **Google Sign-In** under Authentication → Sign-in method.

### 3. OVPN configs in Realtime Database

OVPN files are **not** in the repo or APK. They live in Firebase Realtime Database under the `/ovpn` node. Each node key is the server identifier (no `.ovpn` extension), and its value is the full `.ovpn` file content as a string.

**RTDB schema:**

```
/ovpn_cache_version: 1          ← integer; bump this to force all clients to re-sync
/ovpn/usa-1:    "<file content>"
/ovpn/usa-2:    "<file content>"
/ovpn/uk-1:     "<file content>"
/ovpn/uk-2:     "<file content>"
/ovpn/canada-1: "<file content>"
/ovpn/france-1: "<file content>"
/ovpn/germany-1:"<file content>"
```

**To upload configs**, build a JSON file and import it via Firebase Console → Realtime Database → ⋮ → Import JSON:

```json
{
  "ovpn_cache_version": 1,
  "ovpn": {
    "usa-1": "<entire .ovpn file contents>",
    "uk-1": "<entire .ovpn file contents>"
  }
}
```

Or run the helper script locally (reads files from a folder, outputs import-ready JSON):

```bash
python3 -c "
import json, os, sys
d = sys.argv[1]
data = {'ovpn_cache_version': 1, 'ovpn': {}}
for f in os.listdir(d):
    if f.endswith('.ovpn'):
        data['ovpn'][f.replace('.ovpn','')] = open(os.path.join(d,f)).read()
print(json.dumps(data))
" /path/to/your/ovpn/files > rtdb_import.json
```

**RTDB security rules** (set in Firebase Console → Realtime Database → Rules):

```json
{
  "rules": {
    "ovpn":               { ".read": "auth != null", ".write": false },
    "ovpn_cache_version": { ".read": "auth != null", ".write": false }
  }
}
```

Reads are gated on Firebase Auth — the app authenticates users via Google Sign-In before any RTDB read occurs.

**Update `RTDB_URL`** in [OvpnSyncManager.java](app/src/main/java/com/anonymous/ethervpn/utilities/OvpnSyncManager.java) to match your database's region URL (visible in Firebase Console → Realtime Database):

```java
private static final String RTDB_URL = "https://<your-project>-default-rtdb.<region>.firebasedatabase.app";
```

### 4. Remote Config

Set the following key in Firebase Console → Remote Config:

| Key | Format | Example |
|-----|--------|---------|
| `countries` | `{"key1","key2",...}` | `{"usa-1","usa-2","uk-1","uk-2","canada-1","france-1","germany-1"}` |
| `username` | string | VPN username (falls back to hardcoded constant if empty) |
| `password` | string | VPN password (falls back to hardcoded constant if empty) |

Keys in `countries` must exactly match node names under `/ovpn` in RTDB.

### 5. CMake / SWIG (ics-openvpn native build)

Add to `openvpn/src/main/cpp/CMakeLists.txt` if not present:

```cmake
set(SWIG_EXECUTABLE "${CMAKE_CURRENT_SOURCE_DIR}/swigwin-4.1.1/swig.exe")
set(SWIG_DIR "${CMAKE_CURRENT_SOURCE_DIR}/swigwin-4.1.1")
```

In `openvpn/build.gradle.kts`:
- Change `id("com.android.application")` → `id("com.android.library")`
- Comment out `splits` and the `applicationVariants` register block

### 6. Build

Two product flavors are defined:

| Flavor | `openvpn3` flag | Use case |
|--------|----------------|----------|
| `skeleton` | `false` | Builds without native OpenVPN3 binaries. Use for UI/sync development. |
| `ui` | `true` | Full build with OpenVPN3 native layer. Requires native `.so` binaries present. |

```bash
# Quick build (no native OpenVPN3 required)
./gradlew assembleSkeletonDebug

# Release build
./gradlew assembleUiRelease
```

## ⚙️ OpenVPN Integration Guide

When integrating the OpenVPN module into a fresh base app:

- Enable **multidex**, **databinding**, and **aidl** in `app/build.gradle`
- Add **productFlavors** `['ui', 'skeleton']` with the `openvpn3` buildConfigField
- Verify `AndroidManifest.xml` includes:
  - Service: `de.blinkt.openvpn.core.OpenVPNService`
  - Activity: `de.blinkt.openvpn.activities.DisconnectVPN`
- Replace `requestIdToken` in the Google Sign-In call with your own Web Client ID from Firebase Console → Authentication → Google provider → Web SDK configuration

## 🔄 Rotating VPN Credentials

No APK release needed. To push new OVPN configs to all users:

1. Upload the new `.ovpn` content to the relevant RTDB nodes (e.g. `/ovpn/usa-1`).
2. Increment `/ovpn_cache_version` by 1 in the Firebase Console.
3. On next app launch, each client detects the version bump, wipes its local cache, and re-downloads all configs.

## 🛠️ Development Environment

| Component | Version |
|-----------|---------|
| Gradle | 8.4 |
| Android Gradle Plugin | 8.x |
| compileSdk | 34 |
| buildTools | 34.0.0 |
| minSdk | 23 |
| targetSdk | 34 |
| Java | 1.8 |

## 📱 Screenshots

<div align="center">
  <img src="https://github.com/user-attachments/assets/a14b5489-3ca7-4838-8c72-35513ac55174" width="22%" alt="Main Screen"/>
  &nbsp;&nbsp;
  <img src="https://github.com/user-attachments/assets/8bc96eff-1501-403d-9be6-511aa738e2b3" width="22%" alt="Server Selection"/>
  &nbsp;&nbsp;
  <img src="https://github.com/user-attachments/assets/ed97e2ea-d537-4236-8353-90b5deb02121" width="22%" alt="Connection Status"/>
  &nbsp;&nbsp;
  <img src="https://github.com/user-attachments/assets/2bed3fb1-28e3-4c20-a0bb-079ce94e18ed" width="22%" alt="Location Drawer"/>
</div>

## 🚧 Future Development Roadmap

- **Subscription Model** — Stable paid VPN servers or token-based governance
- **Multi-Protocol Support** — IKEv2, WireGuard, SSTP
- **Split Tunneling** — App-specific VPN routing

## 💰 Support Development

| Currency | Address |
|----------|---------|
| **Bitcoin (BTC)** | `bc1qm7j9qsn55ue3ke54n2f92el9jx8rfa343yqxq7` |
| **Ethereum (ETH)** | `0x81466D108b0969DC26baE8AC040d15F706E9a231` |
| **Bittensor (TAO)** | `5CK47QMdHnPAApetdhJjn6pKHGGwwQJQ1QHgBBwY5GPQX3MF` |

## 📧 Contact

[tanujsinghkushwah@gmail.com](mailto:tanujsinghkushwah@gmail.com)

## 📄 License

GNU General Public License v3.0. The ics-openvpn module is licensed under GNU GPL v2.0.

Please [read and understand](https://github.com/tj4752/ether-vpn/blob/master/LICENSE.md) the scope of the license before starting development.

---
