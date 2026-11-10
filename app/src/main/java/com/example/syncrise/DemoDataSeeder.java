package com.example.syncrise;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DemoDataSeeder {

    private static final String TAG = "DemoDataSeeder";

    public interface SeedCallback {
        void onComplete(boolean success);
    }

    public static void seedData(android.content.Context context, SeedCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onComplete(false);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();
        WriteBatch batch = db.batch();

        // 1. Realistic Demo Alarms
        AlarmModel alarm1 = new AlarmModel(UUID.randomUUID().toString(), "06:30", "Morning Class", "", true, 4.5, "Math Problem", "Drink Water!", true, true);
        AlarmModel alarm2 = new AlarmModel(UUID.randomUUID().toString(), "07:45", "Morning Gym", "", true, 6.0, "Barcode", "Bring towel", true, true);
        AlarmModel alarm3 = new AlarmModel(UUID.randomUUID().toString(), "09:00", "Weekend Wakeup", "", true, 7.5, "None", "", false, false);

        batch.set(db.collection("users").document(uid).collection("alarms").document(alarm1.getId()), alarm1);
        batch.set(db.collection("users").document(uid).collection("alarms").document(alarm2.getId()), alarm2);
        batch.set(db.collection("users").document(uid).collection("alarms").document(alarm3.getId()), alarm3);

        // 2. Demo Sleep Logs for Past 7 Days (Cycles: 5, 4, 5, 6, 3, 5, 5)
        double[] cycleCounts = new double[]{5.0, 4.0, 5.0, 6.0, 3.0, 5.0, 5.0};

        for (int i = 0; i < 7; i++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -i);

            // Wake-up time set around 7:30 AM for each day
            cal.set(Calendar.HOUR_OF_DAY, 7);
            cal.set(Calendar.MINUTE, 30);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long endTime = cal.getTimeInMillis();

            double hours = cycleCounts[i] * 1.5; // Each cycle = 90 mins (1.5h)
            long durationMillis = (long) (hours * 60 * 60 * 1000);
            long startTime = endTime - durationMillis;
            int cycles = (int) cycleCounts[i];

            Map<String, Object> logMap = new HashMap<>();
            logMap.put("startTimeMillis", startTime);
            logMap.put("endTimeMillis", endTime);
            logMap.put("durationMillis", durationMillis);
            logMap.put("cyclesCompleted", cycles);
            logMap.put("status", 100);

            String logId = "demo_log_" + i;
            batch.set(db.collection("users").document(uid).collection("sleepLogs").document(logId), logMap);
            
            // Also seed SQLite (SleepLogProvider)
            if (context != null) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put("DATE", String.valueOf(endTime));
                values.put("CYCLES_COMPLETED", cycles);
                values.put("SCORE", 100);
                context.getContentResolver().insert(android.net.Uri.parse("content://com.example.syncrise.provider/logs"), values);
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Demo data successfully seeded to Firebase!");
                    if (callback != null) callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to seed demo data to Firebase", e);
                    if (callback != null) callback.onComplete(false);
                });
    }
}