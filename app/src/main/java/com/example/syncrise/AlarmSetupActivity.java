package com.example.syncrise;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.syncrise.databinding.ActivityAlarmSetupBinding;

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class AlarmSetupActivity extends AppCompatActivity {

    private ActivityAlarmSetupBinding binding;

    private boolean isCycleAlarm = false;
    private double selectedCycleDurationHours = 0.0;
    
    private String selectedRingtoneUri = "";
    
    private final ActivityResultLauncher<Intent> ringtonePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri uri = result.getData().getData();
                    if (uri != null) {
                        selectedRingtoneUri = uri.toString();
                        
                        // Query System Content Provider (MediaStore) for file name
                        android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                            if (nameIndex != -1) {
                                String name = cursor.getString(nameIndex);
                                binding.tvSelectedRingtone.setText(name);
                            }
                            cursor.close();
                        }
                    }
                }
            });

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

        // Setup Pick Custom Ringtone via Content Provider
        binding.btnPickRingtone.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            ringtonePickerLauncher.launch(intent);
        });

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
        // Clear focus on TimePicker to commit typed edits
        binding.timePicker.clearFocus();

        // Check Exact Alarm permission for Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Please grant Exact Alarm permission for SyncRise to fire alarms.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        int hour = binding.timePicker.getHour();
        int minute = binding.timePicker.getMinute();

        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        String labelStr = binding.etAlarmLabel.getText() != null && !binding.etAlarmLabel.getText().toString().trim().isEmpty()
                ? binding.etAlarmLabel.getText().toString().trim()
                : "Alarm";
        String selectedMission = binding.spinnerMission.getSelectedItem() != null
                ? binding.spinnerMission.getSelectedItem().toString()
                : "None";
        boolean isSmartWakeEnabled = binding.switchSmartWake.isChecked();

        String alarmId = UUID.randomUUID().toString();

        // Build AlarmModel
        AlarmModel alarmModel = new AlarmModel(
                alarmId,
                timeStr,
                labelStr,
                selectedRingtoneUri,
                isCycleAlarm,
                selectedCycleDurationHours,
                selectedMission,
                true,
                isSmartWakeEnabled
        );

        // Calculate trigger time in milliseconds
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If time is before or equal to now, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        long triggerTimeMillis = calendar.getTimeInMillis();

        scheduleAlarmClock(triggerTimeMillis, selectedMission, alarmId, selectedRingtoneUri);
        
        // Schedule bedtime reminder if it's a cycle alarm (remind 15 mins before bedtime)
        if (isCycleAlarm) {
            long bedTimeMillis = triggerTimeMillis - (long) (selectedCycleDurationHours * 60 * 60 * 1000) - (15 * 60 * 1000);
            if (bedTimeMillis > System.currentTimeMillis()) {
                scheduleBedtimeReminder(bedTimeMillis, alarmId);
            }
        }

        // Return result back to MainActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("NEW_ALARM", alarmModel);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void scheduleAlarmClock(long triggerTimeMillis, String missionType, String alarmId, String ringtoneUri) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("MISSION_TYPE", missionType);
        intent.putExtra("ALARM_ID", alarmId);
        intent.putExtra("RINGTONE_URI", ringtoneUri);

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

    private void scheduleBedtimeReminder(long bedtimeMillis, String alarmId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("IS_REMINDER", true);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (alarmId + "_reminder").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                return; // Silently abort reminder if exact alarm permission isn't granted.
            }
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, bedtimeMillis, pendingIntent);
        }
    }
}