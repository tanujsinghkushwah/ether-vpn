/*
 * Copyright (c) 2023-Present Tanuj Singh Kushwah
 * Distributed under the GNU GPL v3 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.anonymous.ethervpn;

import android.app.ActivityManager;
import android.app.Application;
import android.os.Build;
import android.os.Process;
import android.os.StrictMode;

import com.google.android.gms.ads.MobileAds;

import java.util.List;

public class EtherVPNApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (isMainProcess()) {
            MobileAds.initialize(this, initializationStatus -> { /* no-op */ });
        }
        if (BuildConfig.BUILD_TYPE.equals("debug"))
            enableStrictModes();
    }

    private boolean isMainProcess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return getPackageName().equals(getProcessName());
        }
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am == null) return true;
        int myPid = Process.myPid();
        List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
        if (procs == null) return true;
        for (ActivityManager.RunningAppProcessInfo info : procs) {
            if (info.pid == myPid) return getPackageName().equals(info.processName);
        }
        return true;
    }

    private void enableStrictModes() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog();

        StrictMode.VmPolicy policy = builder.build();
        StrictMode.setVmPolicy(policy);
    }
}
