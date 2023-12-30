# Ether VPN
Android VPN client developed using [ics-openvpn](https://github.com/schwabe/ics-openvpn) library.

Available on Play Store now :arrow_right:: <https://play.google.com/store/apps/details?id=com.anonymous.ethervpn>


https://github.com/tj4752/ether-vpn/assets/32940477/dc84bbfe-3b74-49e0-a28a-9c818059ffe1


Build instructions:
* Download swig[https://www.swig.org/download.html] on the system.
* Add swig executable path to system/android studio environment path variables.
* Check if submodules in cpp folder are fetched on your system using git submodule command.
* Replace requestIdToken and google-services.json configuration according to firebase configurations on your account.
* If current ovpn servers are not working then replace .ovpn configurations. Some free sites to find ovpn configs: [freeopenvpn](https://www.freeopenvpn.org/index.php?lang=en), [vpngate](https://www.vpngate.net/en/), [vpnbook](https://www.vpnbook.com/).

Tips to build imported openvpn module with latest code while integrating in a base app:
* Change plugin id("com.android.application") to id("com.android.library") in openvpn build.gradle.kts.
* set(SWIG_EXECUTABLE "${CMAKE_CURRENT_SOURCE_DIR}/swigwin-4.1.1/swig.exe")
  set(SWIG_DIR "${CMAKE_CURRENT_SOURCE_DIR}/swigwin-4.1.1")
  Add above 2 lines to openvpn/src/main/cpp/CMakeLists.txt if not present.
* Enable multiDex on your base app.
* Enable databinding and add productFlavors['ui', 'skeleton'] in build.gradle of base app.
* Comment out splits and applicantVariants register function in openvpn build.gradle.kts.
* Verify de.blinkt.openvpn.core.OpenVPNService service and de.blinkt.openvpn.activities.DisconnectVPN activity is added to base project's AndroidManifest file.

Project/IDE configurations:
* Gradle version - 7.5, Gradle plugin version - 7.4.1
* SDK compile version - 33.0.0
* java version "1.8.0_361"
* Kotlin plugin installed on IDE

Screenshots:

<img src="https://github.com/tj4752/ether-vpn/assets/32940477/68342c85-996b-4fe5-9415-6699bea9bf56" width="200" height="450"/>
<img src="https://github.com/tj4752/ether-vpn/assets/32940477/fe16471b-08df-49e5-85a7-8f82e19425e8" width="200" height="450"/>
<img src="https://github.com/tj4752/ether-vpn/assets/32940477/b76f6481-92f6-429e-b1d2-222d23ee5001" width="200" height="450"/>
<img src="https://github.com/tj4752/ether-vpn/assets/32940477/6ccd67b5-5f3f-41b0-9428-871ef906e71e" width="200" height="450"/>  
<img src="https://github.com/tj4752/ether-vpn/assets/32940477/92f22d3d-6626-4f28-988d-d0b280b0af05" width="200" height="450"/>
<img src="https://github.com/tj4752/ether-vpn/assets/32940477/3f47f872-0fa9-46b8-9bde-c936ea110875" width="200" height="450"/>


Future developments possible: 
* UI/UX Improvements.
* Implementation of the subscription model for stable paid VPN servers or token-based governance integration.
* Multiple protocols added support [IKEv2/Wireguard/SSTP].
* Addition of split tunneling for apps.

Support the development for more free servers:  
* BTC - bc1qm7j9qsn55ue3ke54n2f92el9jx8rfa343yqxq7
* ETH - 0x81466D108b0969DC26baE8AC040d15F706E9a231

For business queries, please drop an email to tanujsinghkushwah@gmail.com

License: 
  
This project is covered under GNUv3 license. It uses ics-openvpn module which uses GNUv2 license. Please [read](https://github.com/tj4752/ether-vpn/blob/master/LICENSE.md) and understand scope of the license before starting the development.
