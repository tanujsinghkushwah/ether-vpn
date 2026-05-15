package com.anonymous.ethervpn.utilities;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;

public final class AdManager {

    private static final String TAG = "AdManager";
    private static final String RC_AD_UNIT_KEY = "ADMOB_AD_UNIT_ID";
    // Fallback to Google's public test ID if RC has no value yet
    private static final String TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";

    private static InterstitialAd sAd = null;

    private AdManager() {}

    public static void preload(Context ctx) {
        if (!isAdsEnabled()) return;
        if (sAd != null) return; // already loaded

        String adUnitId = FirebaseRemoteConfig.getInstance().getString(RC_AD_UNIT_KEY);
        if (adUnitId.isEmpty()) adUnitId = TEST_AD_UNIT_ID;
        AdRequest request = new AdRequest.Builder().build();
        InterstitialAd.load(ctx.getApplicationContext(), adUnitId, request,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        sAd = ad;
                        Log.d(TAG, "Interstitial preloaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        sAd = null;
                        Log.w(TAG, "Interstitial load failed: " + error.getMessage());
                    }
                });
    }

    /**
     * Shows the preloaded interstitial, then runs onClosed.
     * If ads are disabled or no ad is loaded, runs onClosed immediately.
     * onClosed is guaranteed to run exactly once.
     */
    public static void showThen(Activity activity, Runnable onClosed) {
        AtomicBoolean done = new AtomicBoolean(false);
        Runnable once = () -> {
            if (done.compareAndSet(false, true)) onClosed.run();
        };

        if (!isAdsEnabled() || sAd == null) {
            once.run();
            return;
        }

        InterstitialAd ad = sAd;
        sAd = null;

        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                once.run();
                preload(activity.getApplicationContext());
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                Log.w(TAG, "Interstitial failed to show: " + error.getMessage());
                once.run();
            }
        });

        ad.show(activity);
    }

    private static boolean isAdsEnabled() {
        return FirebaseRemoteConfig.getInstance().getBoolean("ENABLE_ADS");
    }
}
