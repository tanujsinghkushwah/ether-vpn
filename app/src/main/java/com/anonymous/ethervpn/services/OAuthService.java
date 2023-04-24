package com.anonymous.ethervpn.services;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.anonymous.ethervpn.activities.VpnDock;
import com.anonymous.ethervpn.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class OAuthService extends AppCompatActivity {

    SignInButton btSignIn;

    GoogleSignInClient googleSignInClient;

    FirebaseAuth firebaseAuth;

    SharedPreferences sharedPreferences;

    SharedPreferences.Editor editor;

    boolean firstRun;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences("appPreferences",MODE_PRIVATE);
        editor = sharedPreferences.edit();
        firstRun = getIntent().getBooleanExtra("first_run", false);

        btSignIn = findViewById(R.id.sign_in_button);

        // Initialize sign in options the client-id is copied form google-services.json file
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("1094407803885-gdih713nm1hkl31a2vbg8bgbc26srhu4.apps.googleusercontent.com")
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(OAuthService.this, googleSignInOptions);

        btSignIn.setOnClickListener((View.OnClickListener) view -> {
            Intent intent = googleSignInClient.getSignInIntent();
            startActivityForResult(intent, 100);
        });

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null && !firstRun) {
            startActivity(new Intent(OAuthService.this, VpnDock.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            Task<GoogleSignInAccount> signInAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);

            if (signInAccountTask.isSuccessful()) {
                editor.putBoolean("isLoggedIn", true);
                editor.apply();

                try {
                    GoogleSignInAccount googleSignInAccount = signInAccountTask.getResult(ApiException.class);

                    if (googleSignInAccount != null) {

                        AuthCredential authCredential = GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(), null);

                        firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {

                                if (task.isSuccessful()) {
                                    startActivity(new Intent(OAuthService.this, VpnDock.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                } else {
                                    FirebaseCrashlytics.getInstance().log("Firebase authentication failed: " + task.getException().getMessage());
                                }
                            }
                        });
                    }
                } catch (ApiException e) {
                    FirebaseCrashlytics.getInstance().log("Firebase authentication failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
