package com.example.syncrise;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.syncrise.databinding.ActivityAlarmSetupBinding;

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class AlarmSetupActivity extends AppCompatActivity {

    private ActivityAlarmSetupBinding binding;

    private boolean isCycleAlarm = false;
    private double selectedCycleDurationHours = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlarmSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Mission Selection Spinner
        String[] missions = new String[]{"Math Problem", "Barcode", "None"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, missions);
        binding.spinnerMission.setAdapter(spinnerAdapter);

        // Check if target cycles was passed from intent
        float initialTargetCycles = getIntent().getFloatExtra("TARGET_CYCLES", 0.0f);
        if (initialTargetCycles > 0) {
            calculateAndSetCycleTime(initialTargetCycles);
        }

        // Sleep Cycle Assistant Buttons
        binding.btnCycle3.setOnClickListener(v -> calculateAndSetCycleTime(3.0));
        binding.btnCycle4.setOnClickListener(v -> calculateAndSetCycleTime(4.0));
        binding.btnCycle5.setOnClickListener(v -> calculateAndSetCycleTime(5.0));
        binding.btnCycle6.setOnClickListener(v -> calculateAndSetCycleTime(6.0));

        // Save Button
        binding.btnSaveAlarm.setOnClickListener(v -> saveAndScheduleAlarm());

        // Cancel Button
        binding.btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private void calculateAndSetCycleTime(double cycles) {
        int totalMinutes = (int) (cycles * 90 + 15); // 90 mins per cycle + 15 mins fall-asleep latency
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, totalMinutes);

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        binding.timePicker.setHour(hour);
        binding.timePicker.setMinute(minute);

        isCycleAlarm = true;
        selectedCycleDurationHours = cycles * 1.5; // e.g. 5 cycles = 7.5 hours

        binding.tvCycleInfo.setVisibility(View.VISIBLE);
        binding.tvCycleInfo.setText(String.format(Locale.getDefault(), "Selected: %d Cycles (%.1f hrs sleep)", (int) cycles, selectedCycleDurationHours));
    }

    private void saveAndScheduleAlarm() {
        int hour = binding.timePicker.getHour();
        int minute = binding.timePicker.getMinute();

        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        String labelStr = binding.etAlarmLabel.getText() != null && !binding.etAlarmLabel.getText().toString().trim().isEmpty()
                ? binding.etAlarmLabel.getText().toString().trim()
                : "Alarm";
        String selectedMission = binding.spinnerMission.getSelectedItem() != null
                ? binding.spinnerMission.getSelectedItem().toString()
                : "None";

        String alarmId = UUID.randomUUID().toString();

        // Build AlarmModel
        AlarmModel alarmModel = new AlarmModel(
                alarmId,
                timeStr,
                labelStr,
                isCycleAlarm,
                selectedCycleDurationHours,
                selectedMission,
                true
        );

        // Calculate trigger time in milliseconds
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If time is before now, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        scheduleAlarmClock(calendar.getTimeInMillis(), selectedMission, alarmId);

        // Return result back to MainActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("NEW_ALARM", alarmModel);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void scheduleAlarmClock(long triggerTimeMillis, String missionType, String alarmId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("MISSION_TYPE", missionType);
        intent.putExtra("ALARM_ID", alarmId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                alarmId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent);
        if (alarmManager != null) {
            try {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
                Toast.makeText(this, "Alarm set for " + String.format(Locale.getDefault(), "%tT", triggerTimeMillis), Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Toast.makeText(this, "Permission required for exact alarms!", Toast.LENGTH_LONG).show();
            }
        }
    }
}