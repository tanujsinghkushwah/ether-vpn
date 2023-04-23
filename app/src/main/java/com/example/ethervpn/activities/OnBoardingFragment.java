package com.example.ethervpn.activities;

import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ethervpn.R;
import com.example.ethervpn.services.OAuthService;

public class OnBoardingFragment extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.freeopenvpn_onboardingfragment);

    }

    public void skip(View view) {
        Intent intent = new Intent(this, OAuthService.class);
        intent.putExtra("first_run", true);
        startActivity(intent);
        finish();
    }
}
