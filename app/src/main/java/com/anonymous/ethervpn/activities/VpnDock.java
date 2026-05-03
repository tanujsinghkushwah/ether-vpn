package com.anonymous.ethervpn.activities;

import static com.anonymous.ethervpn.utilities.Constants.APP_PREFS_NAME;
import static com.anonymous.ethervpn.utilities.Utils.serverComparator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anonymous.ethervpn.adapter.ServerListRVAdapter;
import com.anonymous.ethervpn.interfaces.ChangeServer;
import com.anonymous.ethervpn.interfaces.NavItemClickListener;
import com.anonymous.ethervpn.model.Server;
import com.anonymous.ethervpn.services.OAuthService;
import com.anonymous.ethervpn.R;
import com.anonymous.ethervpn.utilities.Constants;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class VpnDock extends AppCompatActivity implements NavItemClickListener {

    private FirebaseAuth firebaseAuth;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private GoogleSignInClient googleSignInClient;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Fragment fragment;
    private RecyclerView serverListRv;
    private ArrayList<Server> serverLists;
    private ServerListRVAdapter serverListRVAdapter;
    private DrawerLayout drawer;
    private ChangeServer changeServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.vpn_dock);

        sharedPreferences = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        firebaseAuth = FirebaseAuth.getInstance();
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        initializeAll();

        // Left button: popup menu (About / Logout)
        ImageButton navbarLeft = findViewById(R.id.navbar_left);
        PopupMenu popupMenu = new PopupMenu(this, navbarLeft);
        popupMenu.getMenuInflater().inflate(R.menu.nav_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            onMenuOptionSelected(item);
            return true;
        });
        navbarLeft.setOnClickListener(v -> popupMenu.show());

        // Right button: open/close the end-gravity server drawer
        ImageButton navbarRight = findViewById(R.id.navbar_right);
        navbarRight.setOnClickListener(v -> toggleDrawer());

        // Also wire the globe icon inside the fragment's own chrome (chromeRight)
        // That is handled inside MainFragment via openServerList()

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.container, fragment);
        transaction.commit();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel chan = new NotificationChannel("openvpn_newstat",
                    "VPN foreground service", NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationChannel chanBg = new NotificationChannel("openvpn_bg",
                    "VPN background service", NotificationManager.IMPORTANCE_NONE);
            chanBg.setLightColor(Color.BLUE);
            chanBg.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            nm.createNotificationChannel(chan);
            nm.createNotificationChannel(chanBg);
        }
    }

    private void initializeAll() {
        drawer = findViewById(R.id.drawer_layout);
        fragment = new MainFragment();
        serverListRv = findViewById(R.id.serverListRv);
        serverListRv.setHasFixedSize(true);
        serverListRv.setLayoutManager(new LinearLayoutManager(this));
        setServerList();
        changeServer = (ChangeServer) fragment;
    }

    private void toggleDrawer() {
        if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END);
        } else {
            drawer.openDrawer(GravityCompat.END);
        }
    }

    public void closeDrawer() {
        if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END);
        }
    }

    /** Called from MainFragment when the globe icon is tapped — opens the full-screen server picker. */
    public void openServerList() {
        // For v1, open the full-screen ServerListActivity
        Intent intent = new Intent(this, ServerListActivity.class);
        startActivityForResult(intent, Constants.REQUEST_SERVER_CHANGE);
    }

    private void setServerList() {
        ArrayList<Server> servers = new ArrayList<>();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        String[] countryArray = mFirebaseRemoteConfig.getString("countries")
                .replaceAll("[{}\"]", "").split(", ");

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        AtomicInteger downloadCounter = new AtomicInteger(countryArray.length);
        String imagesPathPrefix = Constants.imagesPathPrefix;

        for (String country : countryArray) {
            StorageReference flagRef = storageRef.child(imagesPathPrefix + country + ".png");
            try {
                flagRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    servers.add(new Server(country, uri.toString(),
                            country + ".ovpn", Constants.vpnUserName, Constants.vpnPassword));
                    if (downloadCounter.decrementAndGet() == 0) {
                        Collections.sort(servers, serverComparator);
                        serverLists = servers;
                        serverListRVAdapter = new ServerListRVAdapter(serverLists, this);
                        serverListRv.setAdapter(serverListRVAdapter);
                    }
                }).addOnFailureListener(e -> {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().log("Failed to download image: " + e.getMessage());
                    if (downloadCounter.decrementAndGet() == 0) {
                        Collections.sort(servers, serverComparator);
                        serverLists = servers;
                        serverListRVAdapter = new ServerListRVAdapter(serverLists, this);
                        serverListRv.setAdapter(serverListRVAdapter);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().log("Failed to fetch country data: " + e.getMessage());
            }
        }
    }

    @Override
    public void clickedItem(int index) {
        closeDrawer();
        changeServer.newServer(serverLists.get(index));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_SERVER_CHANGE && resultCode == RESULT_OK && data != null) {
            int index = data.getIntExtra(Constants.EXTRA_SERVER_INDEX, -1);
            if (index >= 0 && serverLists != null && index < serverLists.size()) {
                changeServer.newServer(serverLists.get(index));
            }
        }
    }

    private boolean onMenuOptionSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_about_us) {
            startActivity(new Intent(this, AboutUs.class));
            return true;
        } else if (id == R.id.nav_logout) {
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    firebaseAuth.signOut();
                    editor.putBoolean("isLoggedIn", false);
                    editor.apply();
                    startActivity(new Intent(VpnDock.this, OAuthService.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    finish();
                }
            });
            return true;
        }
        return true;
    }
}
