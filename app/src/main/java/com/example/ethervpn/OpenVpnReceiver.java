package com.example.ethervpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.IOException;

import de.blinkt.openvpn.core.OpenVPNService;

public class OpenVpnReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED) ||
                intent.getAction().equals(Intent.ACTION_PACKAGE_CHANGED) ||
                intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
            // Start the ConnectVPN command
            String command = "adb shell am start -a android.intent.action.MAIN -n com.example.ethervpn/de.blinkt.openvpn.LaunchVPN --es de.blinkt.openvpn.api.profileName myvpnprofile";
            try {
                Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
