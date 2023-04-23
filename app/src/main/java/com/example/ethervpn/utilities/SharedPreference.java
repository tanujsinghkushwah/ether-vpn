package com.example.ethervpn.utilities;

import static com.example.ethervpn.utilities.Utils.getImgURL;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.ethervpn.R;
import com.example.ethervpn.model.Server;

public class SharedPreference {

    private static final String APP_PREFS_NAME = "EtherVPNPreference";

    private SharedPreferences mPreference;
    private SharedPreferences.Editor mPrefEditor;
    private Context context;

    private static final String SERVER_COUNTRY = "server_country";
    private static final String SERVER_FLAG = "server_flag";
    private static final String SERVER_OVPN = "server_ovpn";
    private static final String SERVER_OVPN_USER = "server_ovpn_user";
    private static final String SERVER_OVPN_PASSWORD = "server_ovpn_password";

    public SharedPreference(Context context) {
        this.mPreference = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE);
        this.mPrefEditor = mPreference.edit();
        this.context = context;
    }

    /**
     * Save server details
     * @param server details of ovpn server
     */
    public void saveServer(Server server){
        mPrefEditor.putString(SERVER_COUNTRY, server.getCountry());
        mPrefEditor.putString(SERVER_FLAG, server.getFlagUrl());
        mPrefEditor.putString(SERVER_OVPN, server.getOvpn());
        mPrefEditor.putString(SERVER_OVPN_USER, server.getOvpnUserName());
        mPrefEditor.putString(SERVER_OVPN_PASSWORD, server.getOvpnUserPassword());
        mPrefEditor.commit();
    }

    /**
     * Get server data from shared preference
     * @return server model object
     */
    public Server getServer() {

        Server server = new Server(
                mPreference.getString(SERVER_COUNTRY,"United States"),
                mPreference.getString(SERVER_FLAG,getImgURL(R.drawable.usa_flag)),
                mPreference.getString(SERVER_OVPN, "us.ovpn"),
                mPreference.getString(SERVER_OVPN_USER,"freeopenvpn"),
                mPreference.getString(SERVER_OVPN_PASSWORD,"714974189")
        );

        return server;
    }
}
