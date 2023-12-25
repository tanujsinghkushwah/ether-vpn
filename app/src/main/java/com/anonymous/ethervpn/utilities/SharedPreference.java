package com.anonymous.ethervpn.utilities;

import static com.anonymous.ethervpn.utilities.Constants.APP_PREFS_NAME;
import static com.anonymous.ethervpn.utilities.Constants.DEFAULT_COUNTRY;
import static com.anonymous.ethervpn.utilities.Constants.SERVER_COUNTRY;
import static com.anonymous.ethervpn.utilities.Constants.SERVER_FLAG;
import static com.anonymous.ethervpn.utilities.Constants.SERVER_OVPN;
import static com.anonymous.ethervpn.utilities.Constants.SERVER_OVPN_PASSWORD;
import static com.anonymous.ethervpn.utilities.Constants.SERVER_OVPN_USER;
import static com.anonymous.ethervpn.utilities.Utils.getImgURL;

import android.content.Context;
import android.content.SharedPreferences;

import com.anonymous.ethervpn.model.Server;
import com.anonymous.ethervpn.R;

public class SharedPreference {
    private SharedPreferences mPreference;
    private SharedPreferences.Editor mPrefEditor;
    private Context context;

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
                mPreference.getString(SERVER_COUNTRY, DEFAULT_COUNTRY),
                mPreference.getString(SERVER_FLAG,getImgURL(R.drawable.uk_flag)),
                mPreference.getString(SERVER_OVPN, DEFAULT_COUNTRY+".ovpn"),
                mPreference.getString(SERVER_OVPN_USER,Constants.vpnUserName),
                mPreference.getString(SERVER_OVPN_PASSWORD,Constants.vpnPassword)
        );

        return server;
    }
}
