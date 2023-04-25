package com.anonymous.ethervpn.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.anonymous.ethervpn.services.OAuthService;
import com.anonymous.ethervpn.R;

public class OnBoardingFragment extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.freeopenvpn_onboardingfragment);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    }

    public void skip(View view) {
        Intent intent = new Intent(this, OAuthService.class);
        intent.putExtra("first_run", true);
        startActivity(intent);
        finish();
    }
}
