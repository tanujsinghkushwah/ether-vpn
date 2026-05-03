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
import com.anonymous.ethervpn.utilities.Constants;
import com.anonymous.ethervpn.utilities.FlagResolver;
import com.anonymous.ethervpn.utilities.SharedPreference;
import com.anonymous.ethervpn.views.EtherOrbView;
import com.bumptech.glide.Glide;
import com.anonymous.ethervpn.R;
import com.anonymous.ethervpn.databinding.FragmentMainBinding;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.blinkt.openvpn.api.APIVpnProfile;
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
        server = preference.getServer();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        connection = new CheckInternetConnection();

        updateServerCard(server);
        status("connect");
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
            if (vpnStart) {
                boolean disconnectSwitch = binding.primaryCta.getText()
                        .equals(getContext().getString(R.string.disconnect));
                if (disconnectSwitch) {
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
                (dialog, id) -> stopVpn());
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

    private void startVpn() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String assetsPathPrefix = Constants.assetsPathPrefix;
        StorageReference ovpnRef = storageRef.child(assetsPathPrefix + server.getOvpn());
        try {
            File localFile = File.createTempFile("temp", ".ovpn");
            ovpnRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                try {
                    InputStream conf = new FileInputStream(localFile);
                    InputStreamReader isr = new InputStreamReader(conf);
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder config = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        config.append(line).append("\n");
                    }
                    APIVpnProfile profile = mService.addNewVPNProfile(server.getCountry(), false, config.toString());
                    mService.startProfile(profile.mUUID);
                    mService.startVPN(config.toString());
                    auth_failed = false;
                } catch (IOException | RemoteException e) {
                    e.printStackTrace();
                    logger.log("openvpn server connection failed: " + e.getMessage());
                }
            }).addOnFailureListener(exception -> {
                exception.printStackTrace();
                logger.log("Failed to download ovpn config: " + exception.getMessage());
            });
        } catch (IOException e) {
            e.printStackTrace();
            logger.log("openvpn server connection failed: " + e.getMessage());
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
                binding.statStrip.setVisibility(View.VISIBLE);
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
                binding.statStrip.setVisibility(View.GONE);
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
                binding.statStrip.setVisibility(View.GONE);
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
                binding.statStrip.setVisibility(View.GONE);
                binding.serverCard.setVisibility(View.VISIBLE);
                break;
        }
    }

    /** Update the server card with the given server's flag + country name. */
    private void updateServerCard(Server s) {
        if (s == null || binding == null) return;
        binding.serverCountry.setText(s.getCountry());
        // Try bundled vector flag first, fall back to Glide URL
        int flagResId = FlagResolver.resolve(s.getCountry());
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
            if (mBound && binding != null) {
                binding.homeTitle.setText(duration);
            }
        }
    };

    @Override
    public void newServer(Server server) {
        this.server = server;
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
            Message msg = Message.obtain(mHandler, MSG_UPDATE_STATE, state + "|" + message);

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
        if (server == null) server = preference.getServer();
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
