# ether-vpn
Android VPN client developed using openvpn[https://github.com/schwabe/ics-openvpn] library.

Build instructions:
* Download swig[https://www.swig.org/download.html] on the system.
* Add swig executable path to system/android studio environment path variables.
* Check if submodules in cpp folder are fetched on your system using git submodule command.
* Replace requestIdToken and google-services.json configuration according to firebase configurations on your account.
* If current ovpn servers are not working then replace .ovpn configurations. Some free sites to find ovpn configs: https://www.freeopenvpn.org/index.php?lang=en, https://www.vpngate.net/en/.

Tips to build imported openvpn module with latest code while integrating in a base app:
*Change plugin id("com.android.application") to id("com.android.library") in openvpn build.gradle.kts.
*set(SWIG_EXECUTABLE "swig.exe")
 set(SWIG_DIR "swigwin-4.1.1")
 Add above 2 lines to openvpn/src/main/cpp/CMakeLists.txt if not present.
*Enable multiDex on your base app.
* Enable databinding and add productFlavors['ui', 'skeleton' in build.gradle of base app.
*Comment out splits and applicantVariants register function in openvpn build.gradle.kts.
* Verify de.blinkt.openvpn.core.OpenVPNService service and de.blinkt.openvpn.activities.DisconnectVPN activity is added to base project's AndroidManifest file.
