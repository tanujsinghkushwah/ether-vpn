package com.example.ethervpn;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import java.util.Locale;

public class TimerService extends Service {

    private long mStartTime;

    private final IBinder binder = new LocalBinder();

    private TimerServiceCallback mCallback;

    public String mDuration = "00:00:00";

    private Handler mHandler = new Handler();

    private Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long timeElapsed = SystemClock.elapsedRealtime() - mStartTime;
            int hours = (int) (timeElapsed / 3600000);
            int minutes = (int) ((timeElapsed / 60000) % 60);
            int seconds = (int) ((timeElapsed / 1000) % 60);
            mDuration = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
            updateDuration(mDuration);
            mHandler.postDelayed(this, 1000);
        }
    };

    private void updateDuration(String duration) {
        if (mCallback != null) {
            mCallback.onDurationChanged(duration);
        }
    }

    public class LocalBinder extends Binder {
        TimerService getService() {
            // Return this instance of LocalService so clients can call public methods.
            return TimerService.this;
        }

        public void setCallback(TimerServiceCallback callback) {
            mCallback = callback;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mStartTime = SystemClock.elapsedRealtime();
        mHandler.postDelayed(mTimerRunnable, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Return START_STICKY to indicate that the service should be restarted if it gets terminated
        return START_STICKY;
    }

    public interface TimerServiceCallback {
        void onDurationChanged(String duration);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mTimerRunnable);
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}

