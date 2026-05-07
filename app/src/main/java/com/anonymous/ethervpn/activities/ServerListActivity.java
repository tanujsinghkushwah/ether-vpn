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
import com.anonymous.ethervpn.utilities.SharedPreference;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.anonymous.ethervpn.utilities.Constants.APP_PREFS_NAME;

public class ServerListActivity extends AppCompatActivity {

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
        disambiguateNames();

        Set<String> favs = new SharedPreference(this).getFavorites();
        for (Server s : serverList) s.setFavorite(favs.contains(s.getOvpn()));

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
        FirebaseRemoteConfig rc = FirebaseRemoteConfig.getInstance();
        String username = rc.getString("username");
        String password = rc.getString("password");
        if (username.isEmpty()) username = Constants.vpnUserName;
        if (password.isEmpty()) password = Constants.vpnPassword;

        String raw = rc.getString("countries");
        try {
            // Try rich JSON array format first (future schema)
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String u = obj.optString("username", "");
                String p = obj.optString("password", "");
                Server s = new Server(
                        obj.optString("country", ""),
                        obj.optString("flag", ""),
                        obj.optString("ovpn", ""),
                        u.isEmpty() ? username : u,
                        p.isEmpty() ? password : p);
                s.setIsoCode(obj.optString("iso", ""));
                s.setCity(obj.optString("city", ""));
                s.setPremium(obj.optBoolean("premium", false));
                serverList.add(s);
            }
        } catch (Exception e) {
            // Fallback: simple list format — {"usa-1", "uk-2", "canada", ...}
            String[] keys = raw.replaceAll("[{}\"\\s]", "").split(",");
            for (String key : keys) {
                if (key.isEmpty()) continue;
                serverList.add(new Server(
                        displayName(key), "", key + ".ovpn", username, password));
            }
        }
    }

    private void disambiguateNames() {
        disambiguateNames(serverList);
    }

    static void disambiguateNames(List<Server> list) {
        Map<String, Integer> counts = new HashMap<>();
        for (Server s : list) {
            String name = s.getCountry();
            counts.put(name, counts.containsKey(name) ? counts.get(name) + 1 : 1);
        }
        Map<String, Integer> seen = new HashMap<>();
        for (Server s : list) {
            String name = s.getCountry();
            if (counts.get(name) > 1) {
                int n = seen.containsKey(name) ? seen.get(name) + 1 : 1;
                seen.put(name, n);
                s.setCountry(name + "-" + n);
            }
        }
    }

    static String displayName(String key) {
        // Strip trailing numeric suffix so "canada-1" and "canada" share a base name
        // (disambiguateNames will re-apply the suffix when there are duplicates).
        String base = stripNumberSuffix(key);
        if (base.startsWith("usa"))                          return "United States";
        if (base.startsWith("uk") || base.startsWith("gb"))  return "United Kingdom";
        switch (base) {
            case "canada":      return "Canada";
            case "france":      return "France";
            case "germany":     return "Germany";
            case "japan":       return "Japan";
            case "netherlands": return "Netherlands";
            case "singapore":   return "Singapore";
            case "sweden":      return "Sweden";
            case "switzerland": return "Switzerland";
            case "australia":   return "Australia";
            case "brazil":      return "Brazil";
            default:            return base.isEmpty() ? key
                                       : Character.toUpperCase(base.charAt(0)) + base.substring(1);
        }
    }

    private static String stripNumberSuffix(String s) {
        int dash = s.lastIndexOf('-');
        if (dash > 0 && dash < s.length() - 1) {
            String suffix = s.substring(dash + 1);
            if (suffix.matches("\\d+")) return s.substring(0, dash);
        }
        return s;
    }
}
