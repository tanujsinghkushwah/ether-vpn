package com.example.ethervpn;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import de.blinkt.openvpn.api.APIVpnProfile;
import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import de.blinkt.openvpn.api.IOpenVPNAPIService;

public class ProfileActivity extends AppCompatActivity {

    ImageView ivImage;

    TextView tvName;

    Button btLogout;

    Button vpnLaunch;

    FirebaseAuth firebaseAuth;

    GoogleSignInClient googleSignInClient;

    SharedPreferences sharedPreferences;

    SharedPreferences.Editor editor;

    boolean hasFile = false;

    boolean EnableConnectButton = false;

    protected IOpenVPNAPIService mService=null;

    private Handler mHandler;

    private static final int MSG_UPDATE_STATE = 0;

    private static final int START_PROFILE_EMBEDDED = 2;

    private static final int START_PROFILE_BYUUID = 3;

    private static final int ICS_OPENVPN_PERMISSION = 7;

    private static final int PROFILE_ADD_NEW = 8;

    private static final int PROFILE_ADD_NEW_EDIT = 9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sharedPreferences = getSharedPreferences("appPreferences",MODE_PRIVATE);
        editor = sharedPreferences.edit();

        ivImage = findViewById(R.id.iv_image);
        tvName = findViewById(R.id.tv_name);
        btLogout = findViewById(R.id.bt_logout);
        vpnLaunch = findViewById(R.id.vpnlaunch);

        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            Glide.with(ProfileActivity.this).load(firebaseUser.getPhotoUrl()).into(ivImage);
            tvName.setText(firebaseUser.getDisplayName());
        }

        googleSignInClient = GoogleSignIn.getClient(ProfileActivity.this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        btLogout.setOnClickListener(view -> {
            googleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        firebaseAuth.signOut();
                        Toast.makeText(getApplicationContext(), "Logout successful", Toast.LENGTH_SHORT).show();

                        editor.putBoolean("isLoggedIn", false);
                        editor.apply();

                        startActivity(new Intent(ProfileActivity.this, OAuthService.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        finish();
                    }
                }
            });
        });

        vpnLaunch.setOnClickListener((View.OnClickListener) view -> {
            try {
                prepareStartProfile(START_PROFILE_BYUUID);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    private void startEmbeddedProfile(boolean addNew, boolean editable, boolean startAfterAdd)
    {
        try {
            InputStream conf;
            /* Try opening test.local.conf first */
            try {
                conf = (ProfileActivity.this).getAssets().open("test.local.conf");
            }
            catch (IOException e) {
                conf = (ProfileActivity.this).getAssets().open("test.conf");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conf));
            StringBuilder config = new StringBuilder();
            String line;
            while(true) {
                line = br.readLine();
                if(line == null)
                    break;
                config.append(line).append("\n");
            }
            br.close();
            conf.close();

            if (addNew) {
                String name = editable ? "Profile from remote App" : "Non editable profile";
                APIVpnProfile profile = mService.addNewVPNProfile(name, editable, config.toString());
                mService.startProfile(profile.mUUID);

            } else
                mService.startVPN(config.toString());
        } catch (IOException | RemoteException e) {
            e.printStackTrace();
        }
        Toast.makeText((ProfileActivity.this), "Profile started/added", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        mHandler = new Handler((Handler.Callback) ProfileActivity.this);
        bindService();
    }


    private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */

        @Override
        public void newStatus(String uuid, String state, String message, String level)
                throws RemoteException {
            Message msg = Message.obtain(mHandler, MSG_UPDATE_STATE, state + "|" + message);
            msg.sendToTarget();

        }

    };


    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.

            mService = IOpenVPNAPIService.Stub.asInterface(service);

            try {
                // Request permission to use the API
                Intent i = mService.prepare((ProfileActivity.this).getPackageName());
                if (i!=null) {
                    startActivityForResult(i, ICS_OPENVPN_PERMISSION);
                } else {
                    onActivityResult(ICS_OPENVPN_PERMISSION, Activity.RESULT_OK,null);
                }

            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;

        }
    };
    private String mStartUUID=null;

    private void bindService() {

        Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
        icsopenvpnService.setPackage("de.blinkt.openvpn");

        (ProfileActivity.this).bindService(icsopenvpnService, mConnection, Context.BIND_AUTO_CREATE);
    }

    protected void listVPNs() {

        try {
            List<APIVpnProfile> list = mService.getProfiles();
            String all="List:";
            for(APIVpnProfile vp:list.subList(0, Math.min(5, list.size()))) {
                all = all + vp.mName + ":" + vp.mUUID + "\n";
            }

            if (list.size() > 5)
                all +="\n And some profiles....";

            if(list.size()> 0) {
//                Button b= mStartVpn;
//                b.setOnClickListener(this);
//                b.setVisibility(View.VISIBLE);
//                b.setText(list.get(0).mName);
                mStartUUID = list.get(0).mUUID;
            }



//            mHelloWorld.setText(all);

        } catch (RemoteException e) {
            // TODO Auto-generated catch block
//            mHelloWorld.setText(e.getMessage());
        }
    }

    private void unbindService() {
        (ProfileActivity.this).unbindService(mConnection);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService();
    }

    private void prepareStartProfile(int requestCode) throws RemoteException {
        Intent requestpermission = mService.prepareVPNService();
        if(requestpermission == null) {
            onActivityResult(requestCode, Activity.RESULT_OK, null);
        } else {
            // Have to call an external Activity since services cannot used onActivityResult
            startActivityForResult(requestpermission, requestCode);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == START_PROFILE_EMBEDDED)
                startEmbeddedProfile(false, false, false);
            if (requestCode == START_PROFILE_BYUUID)
                try {
                    mService.startProfile(mStartUUID);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            if (requestCode == ICS_OPENVPN_PERMISSION) {
                listVPNs();
                try {
                    mService.registerStatusCallback(mCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
            if (requestCode == PROFILE_ADD_NEW) {
                startEmbeddedProfile(true, false, false);
            } else if (requestCode == PROFILE_ADD_NEW_EDIT) {
                startEmbeddedProfile(true, true, false);
            }
        }
    };
}
