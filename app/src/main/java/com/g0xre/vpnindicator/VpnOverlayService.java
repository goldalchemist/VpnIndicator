package com.g0xre.vpnindicator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;

public class VpnOverlayService extends Service {

    private static final String CHANNEL_ID = "vpn_indicator_channel";
    private static final int POLL_INTERVAL_MS = 3000; // Check every 3 seconds

    private WindowManager windowManager;
    private View overlayView;
    private TextView emojiView;
    private Handler handler;
    private boolean lastVpnState = false;

    private final Runnable vpnCheckRunnable = new Runnable() {
        @Override
        public void run() {
            boolean vpnActive = isVpnActive();
            if (vpnActive != lastVpnState) {
                lastVpnState = vpnActive;
                updateWidget(vpnActive);
            }
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        startForeground(1, buildNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler = new Handler(Looper.getMainLooper());

        setupOverlayWidget();
        startPolling();
    }

    // -----------------------------------------------------------------------
    // Widget setup
    // -----------------------------------------------------------------------

    private void setupOverlayWidget() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_widget, null);
        emojiView = overlayView.findViewById(R.id.tv_emoji);

        // Initial state
        boolean vpnActive = isVpnActive();
        lastVpnState = vpnActive;
        emojiView.setText(vpnActive ? "👍" : "👎");

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        );

        // Position: top-right corner
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 16;  // margin from right edge (dp — close enough at runtime)
        params.y = 80;  // margin from top (below status bar)

        // Allow dragging the widget
        overlayView.setOnTouchListener(new DragTouchListener(params));

        windowManager.addView(overlayView, params);
    }

    private void updateWidget(boolean vpnActive) {
        if (emojiView != null) {
            emojiView.post(() -> emojiView.setText(vpnActive ? "👍" : "👎"));
        }
    }

    // -----------------------------------------------------------------------
    // VPN detection
    // -----------------------------------------------------------------------

    private boolean isVpnActive() {
        ConnectivityManager cm =
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        for (Network network : cm.getAllNetworks()) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return true;
            }
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Polling
    // -----------------------------------------------------------------------

    private void startPolling() {
        handler.post(vpnCheckRunnable);
    }

    private void stopPolling() {
        handler.removeCallbacks(vpnCheckRunnable);
    }

    // -----------------------------------------------------------------------
    // Drag listener — lets user reposition the widget
    // -----------------------------------------------------------------------

    private class DragTouchListener implements View.OnTouchListener {
        private final WindowManager.LayoutParams params;
        private int initialX, initialY;
        private float initialTouchX, initialTouchY;
        private long touchDownTime;

        DragTouchListener(WindowManager.LayoutParams params) {
            this.params = params;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    touchDownTime = System.currentTimeMillis();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    // Gravity END means x is offset from right, so invert dx
                    params.x = initialX - (int)(event.getRawX() - initialTouchX);
                    params.y = initialY + (int)(event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(overlayView, params);
                    return true;

                case MotionEvent.ACTION_UP:
                    long elapsed = System.currentTimeMillis() - touchDownTime;
                    float dx = Math.abs(event.getRawX() - initialTouchX);
                    float dy = Math.abs(event.getRawY() - initialTouchY);
                    // Treat as tap (not drag) → stop service
                    if (elapsed < 300 && dx < 10 && dy < 10) {
                        stopSelf();
                    }
                    return true;
            }
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Notification (required for foreground service on Android 8+)
    // -----------------------------------------------------------------------

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "VPN Indicator",
            NotificationManager.IMPORTANCE_MIN
        );
        channel.setDescription("Keeps the VPN overlay widget running");
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VPN Indicator active")
            .setContentText("Tap the widget to dismiss.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build();
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Restart if killed
    }

    @Override
    public void onDestroy() {
        stopPolling();
        if (overlayView != null && windowManager != null) {
            windowManager.removeView(overlayView);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
