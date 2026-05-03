package com.anonymous.ethervpn.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anonymous.ethervpn.R;
import com.anonymous.ethervpn.adapter.ServerListV2Adapter;
import com.anonymous.ethervpn.model.Server;
import com.anonymous.ethervpn.utilities.Constants;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.anonymous.ethervpn.utilities.Constants.APP_PREFS_NAME;

public class ServerListActivity extends AppCompatActivity {

    private static final String PREF_AUTO_SELECT = "pref_auto_select";
    private static final String PREF_SELECTED_INDEX = "pref_selected_server_index";

    private ServerListV2Adapter adapter;
    private List<Server> serverList = new ArrayList<>();
    private String activeTab = "all";
    private String currentQuery = "";

    private TextView tabAll, tabFree, tabPremium, tabFavorites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_server_list);

        SharedPreferences prefs = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE);

        loadServersFromRemoteConfig();

        adapter = new ServerListV2Adapter(this, serverList);
        adapter.setSelectedIndex(prefs.getInt(PREF_SELECTED_INDEX, 0));
        adapter.setOnServerSelectedListener((index, server) -> {
            prefs.edit().putInt(PREF_SELECTED_INDEX, index).apply();
            Intent result = new Intent();
            result.putExtra(Constants.EXTRA_SERVER_INDEX, index);
            setResult(RESULT_OK, result);
            finish();
        });

        RecyclerView rv = findViewById(R.id.serverListRv2);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // Search
        EditText search = findViewById(R.id.serverSearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim();
                adapter.filter(currentQuery, activeTab);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Auto-select toggle
        SwitchMaterial autoSwitch = findViewById(R.id.autoSelectSwitch);
        autoSwitch.setChecked(prefs.getBoolean(PREF_AUTO_SELECT, false));
        autoSwitch.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(PREF_AUTO_SELECT, checked).apply());

        // Tabs
        tabAll = findViewById(R.id.tabAll);
        tabFree = findViewById(R.id.tabFree);
        tabPremium = findViewById(R.id.tabPremium);
        tabFavorites = findViewById(R.id.tabFavorites);

        tabAll.setOnClickListener(v -> selectTab("all"));
        tabFree.setOnClickListener(v -> selectTab("free"));
        tabPremium.setOnClickListener(v -> selectTab("premium"));
        tabFavorites.setOnClickListener(v -> selectTab("favorites"));

        // Back
        findViewById(R.id.serverListBack).setOnClickListener(v -> finish());
    }

    private void selectTab(String tab) {
        activeTab = tab;
        updateTabVisuals();
        adapter.filter(currentQuery, activeTab);
    }

    private void updateTabVisuals() {
        int activeBg = R.drawable.bg_tab_chip_selected;
        int inactiveBg = R.drawable.bg_tab_chip;
        int activeColor = getColor(R.color.ether_bg_0);
        int inactiveColor = getColor(R.color.ether_ink_1);

        tabAll.setBackgroundResource(activeTab.equals("all") ? activeBg : inactiveBg);
        tabFree.setBackgroundResource(activeTab.equals("free") ? activeBg : inactiveBg);
        tabPremium.setBackgroundResource(activeTab.equals("premium") ? activeBg : inactiveBg);
        tabFavorites.setBackgroundResource(activeTab.equals("favorites") ? activeBg : inactiveBg);

        tabAll.setTextColor(activeTab.equals("all") ? activeColor : inactiveColor);
        tabFree.setTextColor(activeTab.equals("free") ? activeColor : inactiveColor);
        tabPremium.setTextColor(activeTab.equals("premium") ? activeColor : inactiveColor);
        tabFavorites.setTextColor(activeTab.equals("favorites") ? activeColor : inactiveColor);
    }

    private void loadServersFromRemoteConfig() {
        serverList.clear();
        try {
            String json = FirebaseRemoteConfig.getInstance().getString("countries");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Server s = new Server(
                        obj.optString("country", ""),
                        obj.optString("flag", ""),
                        obj.optString("ovpn", ""),
                        obj.optString("username", ""),
                        obj.optString("password", "")
                );
                s.setIsoCode(obj.optString("iso", ""));
                s.setCity(obj.optString("city", ""));
                s.setPremium(obj.optBoolean("premium", false));
                serverList.add(s);
            }
        } catch (Exception e) {
            // Remote config not yet populated; list stays empty
        }
    }
}
