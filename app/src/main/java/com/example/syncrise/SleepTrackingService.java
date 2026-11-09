package com.example.syncrise;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.SleepSegmentRequest;

public class SleepTrackingService extends Service implements SensorEventListener {

    private static final String CHANNEL_ID = "SleepTrackingChannel";
    private SensorManager sensorManager;
    private Sensor accelerometer;
    
    private long startTimeMillis;
    private double targetCyclesHours;
    private boolean isSmartWakeEnabled;
    private long targetTimeMillis;

    private android.app.PendingIntent sleepReceiverPendingIntent;
    
    private android.content.BroadcastReceiver batteryReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
                android.util.Log.d("SleepTrackingService", "Low battery detected. Stopping service to save power.");
                stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        Intent intent = new Intent(this, SleepReceiver.class);
        sleepReceiverPendingIntent = android.app.PendingIntent.getBroadcast(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_MUTABLE
        );
        
        // Register System Broadcast Receiver for Battery
        registerReceiver(batteryReceiver, new android.content.IntentFilter(Intent.ACTION_BATTERY_LOW));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SyncRise")
                .setContentText("Tracking sleep cycles...")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // Run as Foreground Service
        startForeground(1, notification);

        if (intent != null) {
            targetCyclesHours = intent.getDoubleExtra("TARGET_CYCLES", 0);
            isSmartWakeEnabled = intent.getBooleanExtra("IS_SMART_WAKE", true);
            targetTimeMillis = intent.getLongExtra("TARGET_TIME_MILLIS", 0);
        }
        startTimeMillis = System.currentTimeMillis();

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Subscribe to Google Play Services Sleep API
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityRecognition.getClient(this).requestSleepSegmentUpdates(
                    sleepReceiverPendingIntent,
                    SleepSegmentRequest.getDefaultSleepSegmentRequest()
            );
            android.util.Log.d("SleepTrackingService", "Subscribed to Google Play Services Sleep API");
        }

        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            
            // Subtle movement calculation (simplified magnitude)
            double magnitude = Math.sqrt(x * x + y * y + z * z);
            double delta = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);
            
            long currentTime = System.currentTimeMillis();

            // 1. Predictive Smart Wake Window Check (Within 30 minutes before target time)
            if (isSmartWakeEnabled && targetTimeMillis > 0) {
                long windowStartTime = targetTimeMillis - (30 * 60 * 1000L); // 30 min window

                if (currentTime >= windowStartTime && currentTime < targetTimeMillis) {
                    // Light sleep detected via movement spike (delta > 1.5 m/s^2)
                    if (delta > 1.5) {
                        android.util.Log.d("SleepTrackingService", "Predictive Smart Wake triggered early during Light Sleep! Delta: " + delta);
                        
                        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
                        alarmIntent.putExtra("MISSION_TYPE", "Smart Wake");
                        alarmIntent.putExtra("SMART_WAKE_TRIGGERED", true);
                        sendBroadcast(alarmIntent);
                        
                        stopSelf();
                        return;
                    }
                }
            }

            long elapsedMillis = currentTime - startTimeMillis;
            long targetMillis = (long) (targetCyclesHours * 60 * 60 * 1000);

            // 2. Standard Cycle Target Check
            if (targetMillis > 0 && elapsedMillis >= targetMillis) {
                Intent alarmIntent = new Intent(this, AlarmReceiver.class);
                alarmIntent.putExtra("MISSION_TYPE", "Movement Goal Reached");
                sendBroadcast(alarmIntent);
                
                stopSelf();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed for this requirement
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        
        // Unsubscribe from Sleep API
        ActivityRecognition.getClient(this).removeSleepSegmentUpdates(sleepReceiverPendingIntent);
        
        // Unregister system broadcast
        try {
            unregisterReceiver(batteryReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Sleep Tracking",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}