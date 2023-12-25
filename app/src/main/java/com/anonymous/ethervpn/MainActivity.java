package com.anonymous.ethervpn;

import static com.anonymous.ethervpn.utilities.Constants.APP_PREFS_NAME;
import static com.anonymous.ethervpn.utilities.Constants.WELCOME_MESSAGE_KEY;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.anonymous.ethervpn.activities.OnBoardingFragment;
import com.anonymous.ethervpn.activities.VpnDock;
import com.anonymous.ethervpn.services.OAuthService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.remoteconfig.ConfigUpdate;
import com.google.firebase.remoteconfig.ConfigUpdateListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedpreferences;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    private boolean isOnBoardingFragmentShown = false;

    private boolean firstRun;

    private boolean isLoggedIn;

    private boolean onBoardingFlag = false;

    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedpreferences = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE);
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        editor = sharedpreferences.edit();

        isOnBoardingFragmentShown = sharedpreferences.getBoolean("isOnBoardingFragmentShown", false);
        firstRun = sharedpreferences.getBoolean("first_run", true);

        if(firstRun) {
            editor.clear();
            editor.apply();

//            if (!isOnBoardingFragmentShown) {
//                Intent intent = new Intent(this, OnBoardingFragment.class);
//                onBoardingFlag = true;
//                startActivity(intent);
//
//                // Save that the app has been run before
//                sharedpreferences.edit().putBoolean("isOnBoardingFragmentShown", true).apply();
//            }

            editor.putBoolean("first_run", false);
            editor.apply();
            finish();
        }

        isLoggedIn = sharedpreferences.getBoolean("isLoggedIn", false);
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);

        mFirebaseRemoteConfig.addOnConfigUpdateListener(new ConfigUpdateListener() {
            @Override
            public void onUpdate(ConfigUpdate configUpdate) {

                if (configUpdate.getUpdatedKeys().contains(WELCOME_MESSAGE_KEY)) {
                    mFirebaseRemoteConfig.activate().addOnCompleteListener(new OnCompleteListener<Boolean>() {
                        @Override
                        public void onComplete(@NonNull Task<Boolean> task) {
                            Log.d("firebase remote-config", "Updated keys: " + configUpdate.getUpdatedKeys());
                        }
                    });
                }
            }

            @Override
            public void onError(FirebaseRemoteConfigException error) {
                Log.w("firebase remote-config", "Config update error with code: " + error.getCode(), error);
                FirebaseCrashlytics.getInstance().log("Config update error: " + error.getMessage());
            }
        });
        fetchRemoteConfig();

        Intent intent;
        if (isLoggedIn) {
            intent = new Intent(this, VpnDock.class);
            intent.putExtra("first_run", firstRun);
        } else {
            intent = new Intent(this, OAuthService.class);
            intent.putExtra("first_run", firstRun);
        }
        if(!onBoardingFlag){
            startActivity(intent);
        }
        finish();
    }

    private void fetchRemoteConfig() {
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d("firebase remote-config", "Config params updated: " + updated);

                        } else {
                            FirebaseCrashlytics.getInstance().log("Config fetch failed");
                        }
                    }
                });
    }
}