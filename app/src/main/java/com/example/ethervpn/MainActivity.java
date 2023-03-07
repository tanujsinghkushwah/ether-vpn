package com.example.ethervpn;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedpreferences;

    public static final String mypreference = "appPreferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedpreferences = getSharedPreferences(mypreference, MODE_PRIVATE);

        boolean isLoggedIn = sharedpreferences.getBoolean("isLoggedIn", false);

        Intent intent;
        if (isLoggedIn) {
            intent = new Intent(this, ProfileActivity.class);
        } else {
            intent = new Intent(this, OAuthService.class);
        }
        startActivity(intent);
        finish();
    }

}