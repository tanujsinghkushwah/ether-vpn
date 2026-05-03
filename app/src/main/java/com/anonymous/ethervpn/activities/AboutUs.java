package com.anonymous.ethervpn.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.anonymous.ethervpn.R;
import com.google.android.material.snackbar.Snackbar;

public class AboutUs extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.about_us);

        findViewById(R.id.aboutBack).setOnClickListener(v -> finish());

        setupCopyButton(R.id.btnCopyBtc, getString(R.string.about_btc_address));
        setupCopyButton(R.id.btnCopyEth, getString(R.string.about_eth_address));
        setupCopyButton(R.id.btnCopyTao, getString(R.string.about_tao_address));
    }

    private void setupCopyButton(int btnId, String address) {
        findViewById(btnId).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("address", address));
            Snackbar.make(findViewById(android.R.id.content),
                    R.string.about_copied, Snackbar.LENGTH_SHORT).show();
        });
    }
}
