package com.anonymous.ethervpn.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.anonymous.ethervpn.interfaces.ChangeServer;
import com.anonymous.ethervpn.model.Server;
import com.anonymous.ethervpn.services.TimerService;
import com.anonymous.ethervpn.utilities.CheckInternetConnection;
import com.anonymous.ethervpn.utilities.FlagResolver;
import com.anonymous.ethervpn.utilities.SharedPreference;
import com.anonymous.ethervpn.views.EtherOrbView;
import com.bumptech.glide.Glide;
import com.anonymous.ethervpn.R;
import com.anonymous.ethervpn.databinding.FragmentMainBinding;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.anonymous.ethervpn.utilities.AdManager;
import com.anonymous.ethervpn.utilities.OvpnSyncManager;
import com.anonymous.ethervpn.utilities.Constants;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;
import de.blinkt.openvpn.core.VpnStatus;

public class MainFragment extends Fragment implements View.OnClickListener, ChangeServer, Handler.Callback {

    private Server server;
    private CheckInternetConnection connection;

    boolean vpnStart = false;
    private SharedPreference preference;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    private FragmentMainBinding binding;

    private FirebaseCrashlytics logger = FirebaseCrashlytics.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);
        View view = binding.getRoot();
        initializeAll();
        return view;
    }

    private static final int MSG_UPDATE_STATE = 0;
    private static final int ICS_OPENVPN_PERMISSION = 7;
    private static final int NOTIFICATIONS_PERMISSION_REQUEST_CODE = 11;

    protected IOpenVPNAPIService mService = null;
    protected TimerService mTimerService = null;
    boolean mBound = false;
    private Handler mHandler;
    private Boolean auth_failed = false;

    private void initializeAll() {
        preference = new SharedPreference(getContext());
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        connection = new CheckInternetConnection();

        server = resolveInitialServer();

        updateServerCard(server);
        status("connect");
    }

    /**
     * Resolves the initial server from the active country list.
     *
     * Always rebuilt from current Remote Config — never trust stale fields in
     * SharedPreferences:
     *   - Old installs persisted the raw key ("uk-2") as the country name.
     *   - Saved username/password may be the legacy hardcoded defaults and fail auth
     *     against the current Remote Config credentials.
     * If the saved key isn't in the active list (or is missing), we snap to the
     * first active key.
     */
    private Server resolveInitialServer() {
        List<String> activeKeys = OvpnSyncManager.parseCountries(
                mFirebaseRemoteConfig.getString("countries"));
        if (activeKeys.isEmpty()) return preference.getServer(); // RC not fetched yet

        Server saved = preference.getServer();
        String savedKey = saved.getOvpn() != null ? saved.getOvpn().replace(".ovpn", "") : "";
        String chosenKey = (!savedKey.isEmpty() && activeKeys.contains(savedKey))
                ? savedKey : activeKeys.get(0);

        String username = OvpnSyncManager.getUsername(requireContext());
        String password = OvpnSyncManager.getPassword(requireContext());

        // Build the full active list to derive the same disambiguated display name
        // that the server picker uses (e.g. "United Kingdom-2" when uk-1 + uk-2 coexist).
        java.util.ArrayList<Server> all = new java.util.ArrayList<>();
        Server picked = null;
        for (String k : activeKeys) {
            Server s = new Server(
                    ServerListActivity.displayName(k), null, k + ".ovpn", username, password);
            all.add(s);
            if (k.equals(chosenKey)) picked = s;
        }
        ServerListActivity.disambiguateNames(all);
        if (picked == null) picked = all.get(0);
        preference.saveServer(picked);
        return picked;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.primaryCta.setOnClickListener(this);
        binding.serverCard.setOnClickListener(v -> {
            // Open full-screen server picker
            if (getActivity() instanceof VpnDock) {
                ((VpnDock) getActivity()).openServerList();
            }
        });

        isServiceRunning();
        VpnStatus.initLogCache(getActivity().getCacheDir());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.primaryCta) {
            String label = binding.primaryCta.getText().toString();
            if (label.equals(getString(R.string.cta_cancel))) {
                // User hit Cancel during a connecting attempt — abort cleanly
                if (mService != null) {
                    try { mService.disconnect(); } catch (RemoteException ignored) {}
                }
                vpnStart = false;
                status("connect");
                return;
            }
            if (vpnStart) {
                if (label.equals(getString(R.string.disconnect))) {
                    confirmDisconnect();
                } else {
                    resumeVpn();
                }
            } else {
                try {
                    prepareVpn();
                } catch (RemoteException e) {
                    logger.log("openvpn initialization failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void confirmDisconnect() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getActivity().getString(R.string.connection_close_confirm));
        builder.setPositiveButton(getActivity().getString(R.string.yes),
                (dialog, id) -> AdManager.showThen(getActivity(), this::stopVpn));
        builder.setNegativeButton(getActivity().getString(R.string.no), null);
        builder.create().show();
    }

    private void prepareVpn() throws RemoteException {
        if (!vpnStart) {
            if (getInternetStatus()) {
                Intent intent = mService.prepareVPNService();
                if (intent != null) {
                    startActivityForResult(intent, 1);
                } else {
                    startVpn();
                }
                status("connecting");
            }
        } else if (stopVpn()) {
            System.out.println("Disconnect Successfully");
        }
    }

    public boolean stopVpn() {
        try {
            mService.disconnect();
            status("connect");
            vpnStart = false;
            return true;
        } catch (RemoteException e) {
            logger.log("openvpn disconnect failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public void resumeVpn() {
        try {
            mService.resume();
            status("connected");
            vpnStart = true;
        } catch (RemoteException e) {
            logger.log("openvpn connection resume failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ICS_OPENVPN_PERMISSION) {
            try {
                mService.registerStatusCallback(mCallback);
            } catch (RemoteException e) {
                logger.log("openvpn status callback failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean getInternetStatus() {
        return connection.netCheck(getContext());
    }

    public void isServiceRunning() {
        // Initialize UI to disconnected state on start
        status("connect");
    }

    private InputStream openOvpnConfig() throws IOException {
        String key = server.getOvpn().replace(".ovpn", "");
        File f = OvpnSyncManager.localFile(requireContext(), key);
        if (f.exists()) return new FileInputStream(f);
        // Fallback: try key-1.ovpn (e.g. server configured as "canada" → local file "canada-1.ovpn")
        File fallback = OvpnSyncManager.localFile(requireContext(), key + "-1");
        if (fallback.exists()) return new FileInputStream(fallback);
        throw new IOException("ovpn not cached: " + server.getOvpn());
    }

    private void startVpn() {
        String key = server.getOvpn().replace(".ovpn", "");
        if (!OvpnSyncManager.isCached(requireContext(), key)) {
            OvpnSyncManager.ensureFile(requireContext(), key, new OvpnSyncManager.SyncCallback() {
                @Override public void onSuccess() {
                    if (isAdded()) requireActivity().runOnUiThread(MainFragment.this::launchVpn);
                }
                @Override public void onFailure(Exception e) {
                    if (isAdded()) requireActivity().runOnUiThread(() -> {
                        logger.log("OVPN fetch failed: " + e.getMessage());
                        Log.e("EtherVPN", "OVPN fetch failed for " + key, e);
                        status("connect");
                    });
                }
            });
            return;
        }
        launchVpn();
    }

    private void launchVpn() {
        try {
            InputStream conf = openOvpnConfig();
            InputStreamReader isr = new InputStreamReader(conf);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder config = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                // ics-openvpn requires generic "dev tun" — named tun interfaces don't exist on Android
                if (trimmed.matches("dev\\s+tun\\d+")) {
                    config.append("dev tun\n");
                } else if (trimmed.equals("auth-user-pass")) {
                    // Inject credentials inline so OpenVPN never shows the password dialog
                    String liveUser = OvpnSyncManager.getUsername(requireContext());
                    String livePass = OvpnSyncManager.getPassword(requireContext());
                    if (liveUser == null || liveUser.isEmpty()) liveUser = server.getOvpnUserName();
                    if (livePass == null || livePass.isEmpty()) livePass = server.getOvpnUserPassword();
                    config.append("<auth-user-pass>\n");
                    config.append(liveUser).append("\n");
                    config.append(livePass).append("\n");
                    config.append("</auth-user-pass>\n");
                } else if (trimmed.equals("fast-io")) {
                    // fast-io is not supported on Android (TCP_NODELAY equivalent), skip
                } else if (trimmed.startsWith("data-ciphers ") || trimmed.equals("data-ciphers")) {
                    // ics-openvpn 0.7.x uses ncp-ciphers; data-ciphers may be unrecognised. Skip.
                } else {
                    config.append(line).append("\n");
                }
            }
            String finalConfig = config.toString();
            Log.d("EtherVPN", "Starting OpenVPN with config len=" + finalConfig.length()
                    + " server=" + server.getCountry() + " ovpn=" + server.getOvpn());
            mService.startVPN(finalConfig);
            auth_failed = false;
        } catch (IOException e) {
            Log.e("EtherVPN", "startVpn IOException: " + e.getMessage(), e);
            logger.log("openvpn server config read failed: " + e.getMessage());
            status("connect");
        } catch (RemoteException e) {
            Log.e("EtherVPN", "startVpn RemoteException (config rejected by ics-openvpn): "
                    + e.getMessage(), e);
            logger.log("openvpn config rejected: " + e.getMessage());
            status("connect");
        }
    }

    /** Drives UI state from OpenVPN connection state strings. */
    public void setStatus(String connectionState) {
        if (connectionState == null) return;
        switch (connectionState) {
            case "NOPROCESS":
                vpnStart = false;
                status("connect");
                break;
            case "CONNECTED":
                vpnStart = true;
                status("connected");
                AdManager.preload(requireContext().getApplicationContext());
                break;
            case "WAIT":
            case "AUTH":
                status("connecting");
                break;
            case "CONNECTRETRY":
            case "AUTH_FAILED":
                status("retry");
                try {
                    mService.disconnect();
                } catch (RemoteException ex) {
                    logger.log("openvpn disconnect failed: " + ex.getMessage());
                }
                break;
            case "EXITING":
                status("connect");
                break;
            default:
                vpnStart = false;
                break;
        }
    }

    /**
     * Updates all UI elements for the given status.
     * status: "connect" | "connecting" | "connected" | "retry"
     */
    public void status(String s) {
        if (getContext() == null || binding == null) return;

        switch (s) {
            case "connected":
                binding.orbView.setState(EtherOrbView.State.CONNECTED);
                binding.statusChipContainer.setBackground(
                        getContext().getDrawable(R.drawable.bg_chip_protected));
                binding.statusChipText.setText(R.string.status_protected);
                binding.statusChipText.setTextColor(getContext().getColor(R.color.ether_status_protected));
                binding.homeTitle.setText(R.string.home_title_connected);
                binding.homeSubtitle.setText(R.string.home_subtitle_connected);
                binding.primaryCta.setText(R.string.disconnect);
                binding.primaryCta.setBackground(
                        getContext().getDrawable(R.drawable.bg_button_destructive));
                binding.primaryCta.setTextColor(getContext().getColor(R.color.ether_status_unprotected));
                binding.groupConnecting.setVisibility(View.GONE);
                binding.serverCard.setVisibility(View.VISIBLE);
                break;

            case "connecting":
                binding.orbView.setState(EtherOrbView.State.CONNECTING);
                binding.statusChipContainer.setBackground(
                        getContext().getDrawable(R.drawable.bg_chip_securing));
                binding.statusChipText.setText(R.string.status_securing);
                binding.statusChipText.setTextColor(getContext().getColor(R.color.ether_accent));
                binding.homeTitle.setText(R.string.home_subtitle_connecting);
                binding.homeSubtitle.setText("");
                binding.primaryCta.setText(R.string.cta_cancel);
                binding.primaryCta.setBackground(
                        getContext().getDrawable(R.drawable.bg_button_destructive));
                binding.primaryCta.setTextColor(getContext().getColor(R.color.ether_status_unprotected));
                binding.groupConnecting.setVisibility(View.VISIBLE);
                binding.serverCard.setVisibility(View.GONE);
                break;

            case "retry":
                binding.orbView.setState(EtherOrbView.State.IDLE);
                binding.statusChipContainer.setBackground(
                        getContext().getDrawable(R.drawable.bg_chip_unprotected));
                binding.statusChipText.setText(R.string.status_unprotected);
                binding.statusChipText.setTextColor(getContext().getColor(R.color.ether_status_unprotected));
                binding.homeTitle.setText(R.string.home_title_idle);
                binding.homeSubtitle.setText("Retrying…");
                binding.primaryCta.setText(R.string.retry);
                binding.primaryCta.setBackground(
                        getContext().getDrawable(R.drawable.bg_button_primary));
                binding.primaryCta.setTextColor(getContext().getColor(R.color.ether_bg_0));
                binding.groupConnecting.setVisibility(View.GONE);
                binding.serverCard.setVisibility(View.VISIBLE);
                break;

            default: // "connect"
                binding.orbView.setState(EtherOrbView.State.IDLE);
                binding.statusChipContainer.setBackground(
                        getContext().getDrawable(R.drawable.bg_chip_unprotected));
                binding.statusChipText.setText(R.string.status_unprotected);
                binding.statusChipText.setTextColor(getContext().getColor(R.color.ether_status_unprotected));
                binding.homeTitle.setText(R.string.home_title_idle);
                binding.homeSubtitle.setText(R.string.home_subtitle_idle);
                binding.primaryCta.setText(R.string.cta_connect);
                binding.primaryCta.setBackground(
                        getContext().getDrawable(R.drawable.bg_button_primary));
                binding.primaryCta.setTextColor(getContext().getColor(R.color.ether_bg_0));
                binding.groupConnecting.setVisibility(View.GONE);
                binding.serverCard.setVisibility(View.VISIBLE);
                break;
        }
    }

    /** Update the server card with the given server's flag + country name. */
    private void updateServerCard(Server s) {
        if (s == null || binding == null) return;
        binding.serverCountry.setText(s.getCountry());
        // Preferred: resolve flag directly from the RTDB key (immune to disambiguated suffixes
        // like "United States-2"). Fall back to display-name lookup, then to a Glide URL.
        int flagResId = FlagResolver.resolveKey(s.getOvpn());
        if (flagResId == 0) flagResId = FlagResolver.resolve(s.getCountry());
        if (flagResId != 0) {
            binding.serverFlag.setImageResource(flagResId);
        } else if (s.getFlagUrl() != null) {
            Glide.with(this)
                    .load(s.getFlagUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.serverFlag);
        }
    }

    /** Called by TimerService when duration changes (while connected). */
    private TimerService.TimerServiceCallback mTimerServiceCallback = new TimerService.TimerServiceCallback() {
        @Override
        public void onDurationChanged(String duration) {
            Activity activity = getActivity();
            if (activity == null || !mBound || binding == null) return;
            activity.runOnUiThread(() -> binding.homeTitle.setText(duration));
        }
    };

    @Override
    public void newServer(Server server) {
        this.server = server;
        preference.saveServer(server);
        updateServerCard(server);
        if (vpnStart) stopVpn();
        try {
            prepareVpn();
        } catch (RemoteException e) {
            logger.log("openvpn initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mHandler = new Handler(this);
        bindService();
    }

    private ServiceConnection mTimerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TimerService.LocalBinder binder = (TimerService.LocalBinder) iBinder;
            mTimerService = binder.getService();
            binder.setCallback(mTimerServiceCallback);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

    private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
        @Override
        public void newStatus(String uuid, String state, String message, String level) throws RemoteException {
            Activity activity = getActivity();
            if (activity == null) return;
            activity.runOnUiThread(() -> {
                if (state.equals("AUTH_FAILED") || state.equals("CONNECTRETRY")) {
                    auth_failed = true;
                }
                if (!auth_failed) {
                    try {
                        setStatus(state);
                    } catch (Exception e) {
                        logger.log("openvpn status callback failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                    Message msg = Message.obtain(mHandler, MSG_UPDATE_STATE, state + "|" + message);
                    msg.sendToTarget();
                }
                if (auth_failed) {
                    setStatus("AUTH_FAILED");
                }
                if (state.equals("CONNECTED")) {
                    auth_failed = false;
                    if (ActivityCompat.checkSelfPermission(getContext(),
                            android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                                NOTIFICATIONS_PERMISSION_REQUEST_CODE);
                    }
                    bindTimerService();
                } else {
                    unbindTimerService();
                }
            });
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IOpenVPNAPIService.Stub.asInterface(service);
            try {
                Intent i = mService.prepare(getActivity().getPackageName());
                if (i != null) {
                    startActivityForResult(i, ICS_OPENVPN_PERMISSION);
                } else {
                    onActivityResult(ICS_OPENVPN_PERMISSION, Activity.RESULT_OK, null);
                }
            } catch (RemoteException e) {
                logger.log("openvpn service connection failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    private void bindService() {
        Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
        icsopenvpnService.setPackage("com.anonymous.ethervpn");
        getActivity().bindService(icsopenvpnService, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void bindTimerService() {
        Intent serviceIntent = new Intent(getActivity(), TimerService.class);
        serviceIntent.setPackage("com.anonymous.ethervpn");
        getActivity().bindService(serviceIntent, mTimerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        if (server == null) server = resolveInitialServer();
        super.onResume();
        bindService();
    }

    @Override
    public void onPause() {
        if (mService != null) unbindService();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (server != null) preference.saveServer(server);
        super.onStop();
    }

    private void unbindService() {
        getActivity().unbindService(mConnection);
    }

    private void unbindTimerService() {
        if (mBound) {
            getActivity().unbindService(mTimerServiceConnection);
            mBound = false;
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == MSG_UPDATE_STATE) {
            String messageText = (String) msg.obj;
            String[] stateDetails = messageText.split("\\|");
            String currState = stateDetails[0];
            if (currState.equals("NOPROCESS")) {
                status("connect");
            } else if (currState.equals("USERPAUSE")) {
                vpnStart = true;
                if (binding != null)
                    binding.primaryCta.setText(getContext().getString(R.string.resume));
            }
        }
        return true;
    }
}
