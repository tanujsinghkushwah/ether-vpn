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
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
    private FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    private Fragment fragment;
    private RecyclerView serverListRv;
    private ArrayList<Server> serverLists;
    private ServerListRVAdapter serverListRVAdapter;
    private DrawerLayout drawer;
    private ChangeServer changeServer;
    private ImageView navbar_left;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vpn_dock);

        // Initialize all variable
        initializeAll();
        sharedPreferences = getSharedPreferences(APP_PREFS_NAME,MODE_PRIVATE);
        editor = sharedPreferences.edit();
        firebaseAuth = FirebaseAuth.getInstance();

        googleSignInClient = GoogleSignIn.getClient(VpnDock.this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        ImageButton menuRight = findViewById(R.id.navbar_right);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);

        menuRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDrawer();
            }
        });

        PopupMenu popupMenu = new PopupMenu(this, navbar_left);
        popupMenu.getMenuInflater().inflate(R.menu.nav_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                onMenuOptionSelected(item);
                return true;
            }
        });
        navbar_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenu.show();
            }
        });

        transaction.add(R.id.container, fragment);
        transaction.commit();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel("openvpn_newstat", "VPN foreground service", NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationChannel chanBgVPN = new NotificationChannel("openvpn_bg", "VPN background service", NotificationManager.IMPORTANCE_NONE);
            chanBgVPN.setLightColor(Color.BLUE);
            chanBgVPN.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            service.createNotificationChannel(chan);
            service.createNotificationChannel(chanBgVPN);
        }
    }

    /**
     * Initialize all object, listener etc
     */
    private void initializeAll() {
        drawer = findViewById(R.id.drawer_layout);
        navbar_left = findViewById(R.id.navbar_left);

        fragment = new MainFragment();
        serverListRv = findViewById(R.id.serverListRv);
        serverListRv.setHasFixedSize(true);

        serverListRv.setLayoutManager(new LinearLayoutManager(this));

        setServerList();
        changeServer = (ChangeServer) fragment;

    }

    /**
     * Close navigation drawer
     */
    public void closeDrawer(){
        if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END);
        } else {
            drawer.openDrawer(GravityCompat.END);
        }
    }

    /**
     * Generate server array list
     */
    private void setServerList() {

        ArrayList<Server> servers = new ArrayList<>();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        String[] countryArray = mFirebaseRemoteConfig.getString("countries").replaceAll("[{}\"]", "").split(", ");

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        AtomicInteger downloadCounter = new AtomicInteger(countryArray.length);

        //Reference to the Firebase Storage path where the images are stored
        String imagesPathPrefix = Constants.imagesPathPrefix;

        for (String country : countryArray) {
            StorageReference flagRef = storageRef.child(imagesPathPrefix + country+".png");
            try{
                flagRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String flagUrl = uri.toString();

                    servers.add(new Server(country,
                            flagUrl,
                            country+".ovpn",
                            Constants.vpnUserName,
                            Constants.vpnPassword
                    ));

                    if (downloadCounter.decrementAndGet() == 0){
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
            } catch (Exception e){
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().log("Failed to fetch country data: " + e.getMessage());
            }
        }
    }

    /**
     * On navigation item click, close drawer and change server
     * @param index: server index
     */
    @Override
    public void clickedItem(int index) {
        closeDrawer();
        changeServer.newServer(serverLists.get(index));
    }

    private boolean onMenuOptionSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.nav_about_us){
            Intent intent = new Intent(this, AboutUs.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.nav_logout) {
            googleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        firebaseAuth.signOut();

                        editor.putBoolean("isLoggedIn", false);
                        editor.apply();

                        startActivity(new Intent(VpnDock.this, OAuthService.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        finish();
                    }
                }
            });
            return true;
        }
        return true;
    }
}
