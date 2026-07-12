package com.anonymous.ethervpn.utilities;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OvpnSyncManager {

    public interface SyncCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    private static final String RTDB_URL = "https://ether-cc1ac-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final ConcurrentHashMap<String, Task<DataSnapshot>> inFlight = new ConcurrentHashMap<>();
    private static ValueEventListener credentialsListener;

    private static DatabaseReference rtdb() {
        return FirebaseDatabase.getInstance(RTDB_URL).getReference();
    }

    public static void listenForCredentials(Context ctx) {
        if (credentialsListener != null) return;
        credentialsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                SharedPreferences prefs = ctx.getSharedPreferences(Constants.APP_PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                if (snapshot.hasChild("username") && snapshot.child("username").getValue() instanceof String) {
                    editor.putString("rtdb_username", (String) snapshot.child("username").getValue());
                }
                if (snapshot.hasChild("password") && snapshot.child("password").getValue() instanceof String) {
                    editor.putString("rtdb_password", (String) snapshot.child("password").getValue());
                }
                editor.apply();
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        };
        rtdb().addValueEventListener(credentialsListener);
    }

    public static String getUsername(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(Constants.APP_PREFS_NAME, Context.MODE_PRIVATE);
        String cached = prefs.getString("rtdb_username", null);
        if (cached != null && !cached.isEmpty()) return cached;
        try {
            String rcUser = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance().getString("username");
            if (!rcUser.isEmpty()) return rcUser;
        } catch (Exception ignored) {}
        return Constants.vpnUserName;
    }

    public static String getPassword(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(Constants.APP_PREFS_NAME, Context.MODE_PRIVATE);
        String cached = prefs.getString("rtdb_password", null);
        if (cached != null && !cached.isEmpty()) return cached;
        try {
            String rcPass = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance().getString("password");
            if (!rcPass.isEmpty()) return rcPass;
        } catch (Exception ignored) {}
        return Constants.vpnPassword;
    }

    public static File ovpnDir(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "ovpn");
        dir.mkdirs();
        return dir;
    }

    public static File localFile(Context ctx, String key) {
        return new File(ovpnDir(ctx), key + ".ovpn");
    }

    public static boolean isCached(Context ctx, String key) {
        File f = localFile(ctx, key);
        return f.exists() && f.length() > 0;
    }

    /**
     * Parse the {@code countries} Remote Config value into a list of enabled OVPN keys
     * (no .ovpn extension). Disabled countries are filtered out.
     *
     * Supported formats:
     *  - JSON object map (preferred): {"usa-1": true, "usa-2": false, ...}
     *  - JSON array of objects: [{"ovpn": "usa-1", "enabled": true}, ...]
     *  - Legacy set format: {"usa-1", "uk-2", "canada-1", ...} — all treated as enabled.
     */
    public static List<String> parseCountries(String raw) {
        List<String> keys = new ArrayList<>();
        if (raw == null) return keys;
        String trimmed = raw.trim();

        // 1) JSON object map: {"usa-1": true, "uk-2": false}
        try {
            JSONObject map = new JSONObject(trimmed);
            java.util.Iterator<String> it = map.keys();
            while (it.hasNext()) {
                String key = it.next();
                if (key == null || key.isEmpty()) continue;
                if (map.optBoolean(key, true)) {
                    keys.add(key.replace(".ovpn", ""));
                }
            }
            Collections.sort(keys);
            return keys;
        } catch (Exception ignored) {}

        // 2) JSON array of objects: [{"ovpn": "usa-1", "enabled": true}, ...]
        try {
            JSONArray array = new JSONArray(trimmed);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String ovpn = obj.optString("ovpn", "");
                if (ovpn.isEmpty()) continue;
                if (obj.optBoolean("enabled", true)) {
                    keys.add(ovpn.replace(".ovpn", ""));
                }
            }
            Collections.sort(keys);
            return keys;
        } catch (Exception ignored) {}

        // 3) Legacy set format: {"usa-1", "uk-2", "canada-1", ...}
        String[] parts = trimmed.replaceAll("[{}\"\\s]", "").split(",");
        for (String p : parts) {
            if (!p.isEmpty()) keys.add(p);
        }
        Collections.sort(keys);
        return keys;
    }

    /**
     * Sync all OVPN files from Realtime Database.
     * Checks cache version first; wipes local cache if remote version is newer.
     * Only downloads files that are missing from local storage.
     * Safe to call on every VpnDock launch — a no-op when all files are present.
     */
    public static void sync(Context ctx, List<String> keys, SyncCallback cb) {
        listenForCredentials(ctx);
        DatabaseReference dbRef = rtdb();
        dbRef.child("ovpn_cache_version").get().addOnCompleteListener(versionTask -> {
            if (versionTask.isSuccessful() && versionTask.getResult().getValue() != null) {
                long remoteVersion = ((Number) versionTask.getResult().getValue()).longValue();
                SharedPreferences prefs = ctx.getSharedPreferences(
                        Constants.APP_PREFS_NAME, Context.MODE_PRIVATE);
                long localVersion = prefs.getLong("ovpn_cache_version", -1);
                if (remoteVersion > localVersion) {
                    wipeOvpnDir(ctx);
                    prefs.edit().putLong("ovpn_cache_version", remoteVersion).apply();
                }
            }
            fetchMissingFiles(ctx, keys, cb);
        });
    }

    /** Ensure a single OVPN file is cached, downloading it if necessary. Thread-safe. */
    public static void ensureFile(Context ctx, String key, SyncCallback cb) {
        if (isCached(ctx, key)) {
            cb.onSuccess();
            return;
        }
        // Dedupe concurrent calls for the same key
        Task<DataSnapshot> existing = inFlight.get(key);
        if (existing != null && !existing.isComplete()) {
            existing.addOnCompleteListener(task -> {
                if (task.isSuccessful()) cb.onSuccess();
                else cb.onFailure(task.getException());
            });
            return;
        }
        DatabaseReference ref = rtdb().child("ovpn").child(key);
        Task<DataSnapshot> fetchTask = ref.get();
        inFlight.put(key, fetchTask);
        fetchTask.addOnCompleteListener(task -> {
            inFlight.remove(key);
            if (task.isSuccessful() && task.getResult().getValue() instanceof String) {
                try {
                    writeOvpn(ctx, key, (String) task.getResult().getValue());
                    cb.onSuccess();
                } catch (IOException e) {
                    cb.onFailure(e);
                }
            } else {
                Exception ex = task.getException() != null
                        ? task.getException()
                        : new IOException("OVPN node missing or not a string: " + key);
                cb.onFailure(ex);
            }
        });
    }

    private static void fetchMissingFiles(Context ctx, List<String> keys, SyncCallback cb) {
        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            if (!isCached(ctx, key)) missing.add(key);
        }
        if (missing.isEmpty()) {
            cb.onSuccess();
            return;
        }
        DatabaseReference ovpnRef = rtdb().child("ovpn");
        List<Task<DataSnapshot>> tasks = new ArrayList<>();
        for (String key : missing) {
            final String k = key;
            Task<DataSnapshot> t = ovpnRef.child(k).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.getValue() instanceof String) {
                            try { writeOvpn(ctx, k, (String) snapshot.getValue()); }
                            catch (IOException ignored) {}
                        }
                    });
            tasks.add(t);
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener(result -> {
            boolean allOk = true;
            for (Task<?> t : tasks) {
                if (!t.isSuccessful()) { allOk = false; break; }
            }
            if (allOk) cb.onSuccess();
            else cb.onFailure(new IOException("Some OVPN files failed to download"));
        });
    }

    private static void writeOvpn(Context ctx, String key, String content) throws IOException {
        File f = localFile(ctx, key);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(content.getBytes("UTF-8"));
        }
    }

    private static void wipeOvpnDir(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "ovpn");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        }
    }
}
