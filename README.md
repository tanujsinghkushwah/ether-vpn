# Ether VPN

An Android VPN client developed using the [ics-openvpn](https://github.com/schwabe/ics-openvpn) library.

[![Play Store](https://img.shields.io/badge/Download-Play%20Store-green.svg)](https://play.google.com/store/apps/details?id=com.anonymous.ethervpn)

## Demo

https://github.com/tj4752/ether-vpn/assets/32940477/dc84bbfe-3b74-49e0-a28a-9c818059ffe1

## 📋 Prerequisites

Before building this project, ensure you have the following installed:

- **SWIG**: Download from [swig.org](https://www.swig.org/download.html)
- **Android Studio** with Kotlin plugin
- **Java 1.8.0_361** or compatible version

## 🚀 Build Instructions

1. **Install SWIG**
   - Download SWIG from the official website
   - Add the SWIG executable path to your system/Android Studio environment variables

2. **Initialize Submodules**
   ```bash
   git submodule update --init --recursive
   ```
   - Verify that submodules in the `cpp` folder are properly fetched

3. **Firebase Configuration**
   - Replace `requestIdToken` in your code
   - Update `google-services.json` with your Firebase project configuration

4. **VPN Server Configuration**
   - If current OVPN servers are not working, replace `.ovpn` configurations
   - **Free OVPN config sources:**
     - [FreeOpenVPN](https://www.freeopenvpn.org/index.php?lang=en)
     - [VPN Gate](https://www.vpngate.net/en/)
     - [VPN Book](https://www.vpnbook.com/)

## ⚙️ OpenVPN Integration Guide

When integrating the OpenVPN module with the latest code in your base app:

### Module Configuration
- Change `id("com.android.application")` to `id("com.android.library")` in `openvpn/build.gradle.kts`

### CMake Configuration
Add the following lines to `openvpn/src/main/cpp/CMakeLists.txt` if not present:
```cmake
set(SWIG_EXECUTABLE "${CMAKE_CURRENT_SOURCE_DIR}/swigwin-4.1.1/swig.exe")
set(SWIG_DIR "${CMAKE_CURRENT_SOURCE_DIR}/swigwin-4.1.1")
```

### Base App Configuration
- Enable **multidex** in your base app
- Enable **databinding**
- Add **productFlavors** `['ui', 'skeleton']` in `build.gradle`
- Comment out **splits** and **applicantVariants** register function in `openvpn/build.gradle.kts`

### AndroidManifest Requirements
Verify the following components are added to your base project's `AndroidManifest.xml`:
- Service: `de.blinkt.openvpn.core.OpenVPNService`
- Activity: `de.blinkt.openvpn.activities.DisconnectVPN`

## 🛠️ Development Environment

| Component | Version |
|-----------|---------|
| Gradle | 7.5 |
| Gradle Plugin | 7.4.1 |
| SDK Compile Version | 33.0.0 |
| Java | 1.8.0_361 |
| IDE | Android Studio with Kotlin Plugin |

## 📱 Screenshots

<div align="center">
  <img src="https://github.com/tj4752/ether-vpn/assets/32940477/68342c85-996b-4fe5-9415-6699bea9bf56" width="200" height="450" alt="Main Screen"/>
  <img src="https://github.com/tj4752/ether-vpn/assets/32940477/fe16471b-08df-49e5-85a7-8f82e19425e8" width="200" height="450" alt="Server Selection"/>
  <img src="https://github.com/tj4752/ether-vpn/assets/32940477/b76f6481-92f6-429e-b1d2-222d23ee5001" width="200" height="450" alt="Connection Status"/>
</div>

<div align="center">
  <img src="https://github.com/tj4752/ether-vpn/assets/32940477/6ccd67b5-5f3f-41b0-9428-871ef906e71e" width="200" height="450" alt="Settings"/>
  <img src="https://github.com/tj4752/ether-vpn/assets/32940477/92f22d3d-6626-4f28-988d-d0b280b0af05" width="200" height="450" alt="About"/>
  <img src="https://github.com/tj4752/ether-vpn/assets/32940477/3f47f872-0fa9-46b8-9bde-c936ea110875" width="200" height="450" alt="Profile"/>
</div>

## 🚧 Future Development Roadmap

- **UI/UX Improvements** - Enhanced user interface and experience
- **Subscription Model** - Implementation for stable paid VPN servers or token-based governance
- **Multi-Protocol Support** - Addition of IKEv2, WireGuard, and SSTP protocols
- **Split Tunneling** - App-specific VPN routing capabilities

## 💰 Support Development

Help us maintain free servers and continue development:

| Currency | Address |
|----------|---------|
| **Bitcoin (BTC)** | `bc1qm7j9qsn55ue3ke54n2f92el9jx8rfa343yqxq7` |
| **Ethereum (ETH)** | `0x81466D108b0969DC26baE8AC040d15F706E9a231` |
| **Bittensor (TAO)** | `5CK47QMdHnPAApetdhJjn6pKHGGwwQJQ1QHgBBwY5GPQX3MF` |

## 📧 Contact

For business inquiries, please contact: [tanujsinghkushwah@gmail.com](mailto:tanujsinghkushwah@gmail.com)

## 📄 License

This project is licensed under the **GNU General Public License v3.0**.

The project uses the ics-openvpn module, which is licensed under **GNU General Public License v2.0**.

Please [read and understand](https://github.com/tj4752/ether-vpn/blob/master/LICENSE.md) the scope of the license before starting development.

---
