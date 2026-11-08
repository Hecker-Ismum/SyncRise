package com.example.syncrise;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.SleepClassifyEvent;
import com.google.android.gms.location.SleepSegmentEvent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SleepReceiver extends BroadcastReceiver {
    private static final String TAG = "SleepReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Sleep API Event Received");

        if (SleepSegmentEvent.hasEvents(intent)) {
            List<SleepSegmentEvent> sleepSegmentEvents = SleepSegmentEvent.extractEvents(intent);
            for (SleepSegmentEvent event : sleepSegmentEvents) {
                Log.d(TAG, "SleepSegmentEvent: " + event.getStartTimeMillis() + " to " + event.getEndTimeMillis() + " Status: " + event.getStatus());
                saveSleepSegment(context, event);
            }
        }

        if (SleepClassifyEvent.hasEvents(intent)) {
            List<SleepClassifyEvent> sleepClassifyEvents = SleepClassifyEvent.extractEvents(intent);
            for (SleepClassifyEvent event : sleepClassifyEvents) {
                Log.d(TAG, "SleepClassifyEvent: Confidence " + event.getConfidence() + " - Light: " + event.getLight() + " Motion: " + event.getMotion());
            }
        }
    }

    private void saveSleepSegment(Context context, SleepSegmentEvent event) {
        ContentValues values = new ContentValues();
        values.put(SleepLogProvider.COLUMN_DATE, String.valueOf(event.getEndTimeMillis()));
        
        // Calculate rough cycles (90 min = 5400000 ms)
        long durationMillis = event.getEndTimeMillis() - event.getStartTimeMillis();
        int cyclesCompleted = (int) (durationMillis / (90 * 60 * 1000));
        
        values.put(SleepLogProvider.COLUMN_CYCLES_COMPLETED, cyclesCompleted);
        values.put(SleepLogProvider.COLUMN_SCORE, event.getStatus()); // Using status as a placeholder score

        try {
            context.getContentResolver().insert(SleepLogProvider.CONTENT_URI, values);
            Log.d(TAG, "Sleep segment saved to local database.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save sleep segment locally", e);
        }
        
        // Also upload to Firebase Firestore if logged in
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> sleepData = new HashMap<>();
            sleepData.put("startTimeMillis", event.getStartTimeMillis());
            sleepData.put("endTimeMillis", event.getEndTimeMillis());
            sleepData.put("durationMillis", durationMillis);
            sleepData.put("cyclesCompleted", cyclesCompleted);
            sleepData.put("status", event.getStatus());

            db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("sleepLogs")
                .add(sleepData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Sleep log uploaded to Firebase: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to upload sleep log to Firebase", e));
        }
    }
}