package com.example.syncrise;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "AlarmChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "SyncRise Alarm Triggered!", Toast.LENGTH_LONG).show();

        // Extract intent extras
        String missionType = intent.getStringExtra("MISSION_TYPE");

        // Launch MissionActivity with required flags
        Intent missionIntent = new Intent(context, MissionActivity.class);
        if (missionType != null) {
            missionIntent.putExtra("MISSION_TYPE", missionType);
        }
        missionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Android 10+ requires Full-Screen Intent from background
        android.app.PendingIntent fullScreenPendingIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                missionIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        createNotificationChannel(context);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("SyncRise Alarm")
                .setContentText("Wake up! Mission time.")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true);

        android.app.NotificationManager notificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1001, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    CHANNEL_ID,
                    "Alarm Notifications",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            android.app.NotificationManager manager = context.getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}