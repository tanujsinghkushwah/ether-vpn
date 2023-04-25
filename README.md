# Ether VPN
Android VPN client developed using [ics-openvpn](https://github.com/schwabe/ics-openvpn) library.

https://user-images.githubusercontent.com/32940477/234350462-56fd801c-c066-47e0-81e1-47c7d30725a8.mp4

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
<img src="https://user-images.githubusercontent.com/32940477/234352990-4a29fec4-a900-4bc1-92f2-5612134810dc.png" width="200" height="450"/>
<img src="https://user-images.githubusercontent.com/32940477/234353073-8c513e67-a284-4286-b339-23ea7e7d61d4.png" width="200" height="450"/>
<img src="https://user-images.githubusercontent.com/32940477/234353534-635be106-c2fd-448a-89b9-c3bd63355bc2.png" width="200" height="450"/>
<img src="https://user-images.githubusercontent.com/32940477/234353884-7187cbe9-e279-4065-843a-d0d0eb1816a7.png" width="178"/>
<img src="https://user-images.githubusercontent.com/32940477/234353935-238bcd55-1e8f-4f2c-a88f-71de9ee0799d.png" width="200" height="450"/>
<img src="https://user-images.githubusercontent.com/32940477/234353952-aa127901-3f8c-4aa3-8124-b66e52aa9bab.png" width="200" height="450"/>
<img src="https://user-images.githubusercontent.com/32940477/234354032-3d0bd026-add7-415e-b972-a4b96c8aa70f.png" width="200" height="450"/>
<img src="https://user-images.githubusercontent.com/32940477/234354071-4e68e289-0671-427d-93e9-e5915ba0660b.png" width="200" height="450"/>


