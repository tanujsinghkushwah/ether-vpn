package com.example.ethervpn;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private TextView connectionSpeed;
    private Button createConnection;
    private Boolean connectionStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionSpeed = findViewById(R.id.connectionSpeed);
        createConnection = findViewById(R.id.createConnection);

        createConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVPNService();
            }
        });
    }

    private void startVPNService() {
        if (!connectionStatus) {
            Intent intent = new Intent(getApplicationContext(), VpnService.class);
            startService(intent);
            Toast.makeText(this, "VPN service started", Toast.LENGTH_SHORT).show();
        }
    }
}