# Ether VPN
Android VPN client developed using [ics-openvpn](https://github.com/schwabe/ics-openvpn) library.


https://user-images.githubusercontent.com/32940477/233469745-d11c600d-6f23-4e86-85a8-bfe8125318a6.mp4


Build instructions:
* Download swig[https://www.swig.org/download.html] on the system.
* Add swig executable path to system/android studio environment path variables.
* Check if submodules in cpp folder are fetched on your system using git submodule command.
* Replace requestIdToken and google-services.json configuration according to firebase configurations on your account.
* If current ovpn servers are not working then replace .ovpn configurations. Some free sites to find ovpn configs: [freeopenvpn](https://www.freeopenvpn.org/index.php?lang=en), [vpngate](https://www.vpngate.net/en/).

Tips to build imported openvpn module with latest code while integrating in a base app:
* Change plugin id("com.android.application") to id("com.android.library") in openvpn build.gradle.kts.
* set(SWIG_EXECUTABLE "swig.exe")
 set(SWIG_DIR "swigwin-4.1.1")
 Add above 2 lines to openvpn/src/main/cpp/CMakeLists.txt if not present.
* Enable multiDex on your base app.
* Enable databinding and add productFlavors['ui', 'skeleton' in build.gradle of base app.
* Comment out splits and applicantVariants register function in openvpn build.gradle.kts.
* Verify de.blinkt.openvpn.core.OpenVPNService service and de.blinkt.openvpn.activities.DisconnectVPN activity is added to base project's AndroidManifest file.

Project/IDE configurations:
* Gradle version - 7.5, Gradle plugin version - 7.4.1
* SDK compile version - 33.0.0
* java version "1.8.0_361"
* Kotlin plugin installed on IDE

Screenshots:
<img src="https://user-images.githubusercontent.com/32940477/233471447-f1463c93-9f33-4f02-9a37-3e3c905f053f.png" width="200" height="200"/>
<img src="https://user-images.githubusercontent.com/32940477/233471509-da9414a2-646d-4e1f-b056-8eee6d4854ba.png" width="200" height="200"/>
<img src="https://user-images.githubusercontent.com/32940477/233471553-b51308c7-f84e-4395-b54b-327ba162b648.png" width="200" height="200"/>
<img src="https://user-images.githubusercontent.com/32940477/233471662-6756baa8-0650-4a8f-954e-e52b972634b1.png" width="200" height="200"/>


