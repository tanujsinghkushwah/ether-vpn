package com.anonymous.ethervpn.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.anonymous.ethervpn.R;
import com.google.android.material.snackbar.Snackbar;

public class AboutUs extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.about_us);

        View root = findViewById(R.id.aboutRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

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
