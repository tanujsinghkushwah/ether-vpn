package com.example.ethervpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.blinkt.openvpn.core.OpenVPNService;

public class OpenVpnReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Start the OpenVPN service
            Intent serviceIntent = new Intent(context, OpenVPNService.class);
            context.startService(serviceIntent);
        }
    }
}
