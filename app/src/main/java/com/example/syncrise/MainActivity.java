package com.example.syncrise;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import android.annotation.SuppressLint;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.syncrise.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "LifecycleLog";
    private ActivityMainBinding binding;
    
    private AlarmAdapter alarmAdapter;
    private ArrayList<AlarmModel> alarmList;

    private final androidx.activity.result.ActivityResultLauncher<Intent> alarmSetupLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    AlarmModel newAlarm = (AlarmModel) result.getData().getSerializableExtra("NEW_ALARM");
                    if (newAlarm != null) {
                        alarmList.add(newAlarm);
                        alarmAdapter.notifyItemInserted(alarmList.size() - 1);
                        binding.rvAlarms.smoothScrollToPosition(alarmList.size() - 1);
                        Toast.makeText(this, "Alarm set for " + newAlarm.getTime(), Toast.LENGTH_SHORT).show();
                        
                        saveAlarmToFirebase(newAlarm);
                    }
                }
            });

    private void saveAlarmToFirebase(AlarmModel alarm) {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .collection("alarms")
                    .document(alarm.getId())
                    .set(alarm)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Alarm saved to Firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save alarm to Firebase", e));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchAlarmsFromFirebase() {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            
            // 1. Fetch Alarms
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .collection("alarms")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        alarmList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            AlarmModel alarm = doc.toObject(AlarmModel.class);
                            if (alarm != null) {
                                alarmList.add(alarm);
                            }
                        }
                        alarmAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch alarms from Firebase", e));
                    
            // 2. Fetch Sleep Logs to calculate Today's Goal Progress
            fetchTodaySleepProgress(db, auth.getCurrentUser().getUid());
        }
    }

    private void fetchTodaySleepProgress(com.google.firebase.firestore.FirebaseFirestore db, String uid) {
        // Calculate start of today in millis
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        long startOfToday = calendar.getTimeInMillis();

        db.collection("users")
                .document(uid)
                .collection("sleepLogs")
                .whereGreaterThanOrEqualTo("endTimeMillis", startOfToday)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long totalSleepMillisToday = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Long duration = doc.getLong("durationMillis");
                        if (duration != null) {
                            totalSleepMillisToday += duration;
                        }
                    }
                    updateSleepProgressUI(totalSleepMillisToday);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch today's sleep logs", e));
    }

    private void updateSleepProgressUI(long totalSleepMillisToday) {
        // Goal is 8 hours (8 * 60 * 60 * 1000 = 28,800,000 ms)
        double targetMillis = 8.0 * 60 * 60 * 1000;
        int progressPercentage = (int) Math.min(100, (totalSleepMillisToday / targetMillis) * 100);
        
        binding.progressSleepGoal.setProgressCompat(progressPercentage, true);
        binding.tvProgressText.setText(progressPercentage + "%");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        Log.d(TAG, "onCreate called");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION}, 102);
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize RecyclerView
        binding.rvAlarms.setLayoutManager(new LinearLayoutManager(this));
        
        // Mock Data for Alarms
        alarmList = new ArrayList<>();
        alarmList.add(new AlarmModel("1", "06:30", "Morning Class", true, 4.5, "Math Problem", true));
        alarmList.add(new AlarmModel("2", "07:00", "Workout", false, 0.0, "None", false));
        alarmList.add(new AlarmModel("3", "08:15", "Weekend Wakeup", true, 6.0, "Barcode", true));
        
        // Initialize Adapter with OnAlarmToggleListener
        alarmAdapter = new AlarmAdapter(alarmList, (alarm, isChecked) -> {
            alarm.setEnabled(isChecked);
            if (isChecked) {
                scheduleAlarmForModel(alarm);
                Toast.makeText(MainActivity.this, "Alarm " + alarm.getTime() + " enabled", Toast.LENGTH_SHORT).show();
            } else {
                cancelAlarmForModel(alarm);
                Toast.makeText(MainActivity.this, "Alarm " + alarm.getTime() + " disabled", Toast.LENGTH_SHORT).show();
            }
            saveAlarmToFirebase(alarm);
        });
        
        binding.rvAlarms.setAdapter(alarmAdapter);
        
        // Fetch Alarms from Firebase
        fetchAlarmsFromFirebase();

        // Explicit Intent to AlarmSetupActivity using ActivityResultLauncher
        binding.btnCreateAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AlarmSetupActivity.class);
            alarmSetupLauncher.launch(intent);
        });

        // Start Sleep Tracking Service
        binding.btnCreateAlarm.setOnLongClickListener(v -> {
            Intent serviceIntent = new Intent(MainActivity.this, SleepTrackingService.class);
            serviceIntent.putExtra("TARGET_CYCLES", 4.5);
            androidx.core.content.ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
            Toast.makeText(MainActivity.this, "Sleep Tracking Started", Toast.LENGTH_SHORT).show();
            return true;
        });

        // Implicit Intent to share statistics
        binding.btnShareStats.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "Here are my sleep statistics from SyncRise!");
            startActivity(Intent.createChooser(intent, "Share Stats via"));
        });
        
        // Explicit Intent to open Sleep History Activity
        binding.btnViewSleepLogs.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SleepLogsActivity.class)));
        
        // Explicit Intent to open Analytics/Trends Activity
        binding.btnViewAnalytics.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AnalyticsActivity.class)));

        // Query SleepLogProvider to demonstrate inter-component communication
        queryProvider();
    }

    private void scheduleAlarmForModel(AlarmModel alarm) {
        try {
            String[] parts = alarm.getTime().split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hour);
            calendar.set(java.util.Calendar.MINUTE, minute);
            calendar.set(java.util.Calendar.SECOND, 0);
            calendar.set(java.util.Calendar.MILLISECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1);
            }

            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("MISSION_TYPE", alarm.getMissionType());
            intent.putExtra("ALARM_ID", alarm.getId());

            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                    this,
                    alarm.getId().hashCode(),
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );

            android.app.AlarmManager.AlarmClockInfo alarmClockInfo = new android.app.AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent);
            if (alarmManager != null) {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm model", e);
        }
    }

    private void cancelAlarmForModel(AlarmModel alarm) {
        try {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                    this,
                    alarm.getId().hashCode(),
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error canceling alarm model", e);
        }
    }

    private void queryProvider() {
        android.net.Uri uri = android.net.Uri.parse("content://com.example.syncrise.provider/logs");
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int dateIndex = cursor.getColumnIndex("DATE");
                    int cyclesIndex = cursor.getColumnIndex("CYCLES_COMPLETED");
                    if (dateIndex != -1 && cyclesIndex != -1) {
                        String date = cursor.getString(dateIndex);
                        int cycles = cursor.getInt(cyclesIndex);
                        Log.d(TAG, "Log: Date=" + date + ", Cycles=" + cycles);
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "SleepLogProvider is empty.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to query SleepLogProvider", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
    }
}