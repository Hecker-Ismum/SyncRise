package com.example.syncrise;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Triggered when an alarm goes off
        Toast.makeText(context, "SyncRise Alarm Triggered!", Toast.LENGTH_LONG).show();

        // Launch MissionActivity
        Intent missionIntent = new Intent(context, MissionActivity.class);
        missionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(missionIntent);
    }
}