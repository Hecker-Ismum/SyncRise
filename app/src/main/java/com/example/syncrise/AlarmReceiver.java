package com.example.syncrise;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

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
        context.startActivity(missionIntent);
    }
}