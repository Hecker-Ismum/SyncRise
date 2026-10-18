package com.example.syncrise;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.syncrise.databinding.ActivityAlarmSetupBinding;

public class AlarmSetupActivity extends AppCompatActivity {
    private ActivityAlarmSetupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlarmSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        float targetCycles = getIntent().getFloatExtra("TARGET_CYCLES", 0.0f);
        binding.tvTargetCycles.setText("Target Cycles: " + targetCycles);
        
        // Example schedule logic (you could call this from a button click)
        // scheduleAlarm(System.currentTimeMillis() + 10000); // 10 seconds from now
    }

    private void scheduleAlarm(long triggerTimeMillis) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
        android.content.Intent intent = new android.content.Intent(this, AlarmReceiver.class);
        intent.putExtra("MISSION_TYPE", "Math Problem");
        
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        android.app.AlarmManager.AlarmClockInfo alarmClockInfo = new android.app.AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent);
        if (alarmManager != null) {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
        }
    }
}