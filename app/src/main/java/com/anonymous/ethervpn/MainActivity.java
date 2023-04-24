package com.anonymous.ethervpn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.anonymous.ethervpn.activities.OnBoardingFragment;
import com.anonymous.ethervpn.activities.VpnDock;
import com.anonymous.ethervpn.services.OAuthService;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedpreferences;

    private boolean isOnBoardingFragmentShown = false;

    private boolean firstRun;

    private boolean isLoggedIn;

    private boolean onBoardingFlag = false;

    public static final String mypreference = "appPreferences";

    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedpreferences = getSharedPreferences(mypreference, MODE_PRIVATE);
        editor = sharedpreferences.edit();

        isOnBoardingFragmentShown = sharedpreferences.getBoolean("isOnBoardingFragmentShown", false);
        firstRun = sharedpreferences.getBoolean("first_run", true);

        if(firstRun) {
            editor.clear();
            editor.apply();

            if (!isOnBoardingFragmentShown) {
                Intent intent = new Intent(this, OnBoardingFragment.class);
                onBoardingFlag = true;
                startActivity(intent);

                // Save that the app has been run before
                sharedpreferences.edit().putBoolean("isOnBoardingFragmentShown", true).apply();
            }

            editor.putBoolean("first_run", false);
            editor.apply();
            finish();
        }

        isLoggedIn = sharedpreferences.getBoolean("isLoggedIn", false);

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

}