/*
 * Copyright (c) 2023-Present Tanuj Singh Kushwah
 * Distributed under the GNU GPL v3 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.anonymous.ethervpn;

import android.app.Application;
import android.os.StrictMode;

public class EtherVPNApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.BUILD_TYPE.equals("debug"))
            enableStrictModes();

    }
    private void enableStrictModes() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog();

        StrictMode.VmPolicy policy = builder.build();
        StrictMode.setVmPolicy(policy);
    }
}
