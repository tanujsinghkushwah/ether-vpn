package com.example.ethervpn;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.example.ethervpn.activities.OnBoardingFragment;
import com.example.ethervpn.activities.VpnDock;
import com.example.ethervpn.services.OAuthService;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedpreferences;

    private boolean isOnBoardingFragmentShown = false;

    private boolean firstRun;

    private boolean isLoggedIn;

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
                getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new OnBoardingFragment())
                        .commit();

                // Save that the app has been run before
                sharedpreferences.edit().putBoolean("isOnBoardingFragmentShown", true).apply();
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
            startActivity(intent);

            editor.putBoolean("first_run", false);
            editor.apply();
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
        startActivity(intent);
        finish();
    }

}