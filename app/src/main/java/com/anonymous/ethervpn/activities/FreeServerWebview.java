package com.anonymous.ethervpn.activities;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.anonymous.ethervpn.R;

public class FreeServerWebview extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.freeopenvpn_webview);

        WebView webView = (WebView) findViewById(R.id.freeopenvpn_webview);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.loadUrl("https://www.vpnbook.com/");
    }
}
