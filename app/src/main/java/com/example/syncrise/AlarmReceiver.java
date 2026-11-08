package com.example.syncrise;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "AlarmChannel";
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "AlarmReceiver triggered!");
        
        boolean isReminder = intent.getBooleanExtra("IS_REMINDER", false);
        
        if (isReminder) {
            showBedtimeReminder(context);
            return;
        }

        Toast.makeText(context, "SyncRise Alarm Triggered!", Toast.LENGTH_LONG).show();

        // Extract intent extras
        String missionType = intent.getStringExtra("MISSION_TYPE");
        String ringtoneUri = intent.getStringExtra("RINGTONE_URI");
        int snoozeCount = intent.getIntExtra("SNOOZE_COUNT", 0);

        // Prepare MissionActivity intent
        Intent missionIntent = new Intent(context, MissionActivity.class);
        if (missionType != null) {
            missionIntent.putExtra("MISSION_TYPE", missionType);
        }
        if (ringtoneUri != null) {
            missionIntent.putExtra("RINGTONE_URI", ringtoneUri);
        }
        missionIntent.putExtra("SNOOZE_COUNT", snoozeCount);
        
        missionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                | Intent.FLAG_ACTIVITY_CLEAR_TOP 
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // 1. Direct Start Activity (Works seamlessly when screen is ON / unlocked)
        try {
            context.startActivity(missionIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start MissionActivity directly", e);
        }

        // 2. FullScreen Intent Notification (Wakes device screen if locked or app in background)
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                missionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("SyncRise Alarm")
                .setContentText("Wake up! Mission: " + (missionType != null ? missionType : "Default"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1001, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alarm Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showBedtimeReminder(Context context) {
        createNotificationChannel(context);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Bedtime Reminder")
                .setContentText("It's almost time for bed! Wind down now to hit your sleep cycles.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1002, builder.build());
        }
    }
}