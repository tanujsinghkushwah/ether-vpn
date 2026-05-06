package com.anonymous.ethervpn.services;

import static android.content.ContentValues.TAG;
import static com.anonymous.ethervpn.utilities.Constants.APP_PREFS_NAME;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.anonymous.ethervpn.activities.VpnDock;
import com.anonymous.ethervpn.R;
import com.anonymous.ethervpn.utilities.CheckInternetConnection;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class OAuthService extends AppCompatActivity {

    private Button btnGoogle;

    private CheckInternetConnection connection;
    private SignInClient signInClient;
    private FirebaseAuth firebaseAuth;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean firstRun;

    private final ActivityResultLauncher<IntentSenderRequest> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> handleSignInResult(result.getData())
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        firstRun = getIntent().getBooleanExtra("first_run", false);

        btnGoogle = findViewById(R.id.btnGoogle);
        connection = new CheckInternetConnection();

        signInClient = Identity.getSignInClient(this);
        firebaseAuth = FirebaseAuth.getInstance();

        btnGoogle.setOnClickListener(v -> {
            if (getInternetStatus()) {
                signIn();
            } else {
                Toast.makeText(this, "Please check your Internet Connection",
                        Toast.LENGTH_LONG).show();
            }
        });

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        // Auto-advance if already signed in
        if (firebaseUser != null && !firstRun) {
            navigateToVpnDock();
            return;
        }

        // Attempt One Tap sign-in if connected and not already signed in
        if (firebaseUser == null && getInternetStatus()) {
            oneTapSignIn();
        }
    }

    private void navigateToVpnDock() {
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
        startActivity(new Intent(this, VpnDock.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    private void handleSignInResult(Intent data) {
        try {
            SignInCredential credential = signInClient.getSignInCredentialFromIntent(data);
            String idToken = credential.getGoogleIdToken();
            Log.d("googleAuthToken", "firebaseAuthWithGoogle:" + credential.getId());
            firebaseAuthWithGoogle(idToken);
        } catch (ApiException e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().log("Google sign in failed: " + e.getMessage());
        }
    }

    private void signIn() {
        GetSignInIntentRequest signInRequest = GetSignInIntentRequest.builder()
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();
        signInClient.getSignInIntent(signInRequest)
                .addOnSuccessListener(this::launchSignIn)
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().log("Google Sign-in failed: " + e.getMessage());
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "firebaseAuth:success");
                        navigateToVpnDock();
                    } else {
                        Log.w(TAG, "firebaseAuth:failure", task.getException());
                        FirebaseCrashlytics.getInstance().log(
                                "Firebase authentication failed: " + task.getException().getMessage());
                    }
                });
    }

    private void oneTapSignIn() {
        BeginSignInRequest oneTapRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(true)
                                .build())
                .build();
        signInClient.beginSignIn(oneTapRequest)
                .addOnSuccessListener(result -> launchSignIn(result.getPendingIntent()))
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().log("One-tap sign-in failed: " + e.getMessage());
                });
    }

    private void launchSignIn(PendingIntent pendingIntent) {
        try {
            IntentSenderRequest req = new IntentSenderRequest.Builder(pendingIntent).build();
            signInLauncher.launch(req);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().log("Couldn't start Sign In: " + e.getMessage());
        }
    }

    private boolean getInternetStatus() {
        return connection.netCheck(this);
    }
}
