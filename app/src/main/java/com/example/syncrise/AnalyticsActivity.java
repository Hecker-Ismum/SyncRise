package com.example.syncrise;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.syncrise.databinding.ActivityAnalyticsBinding;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class AnalyticsActivity extends AppCompatActivity {

    private static final String TAG = "AnalyticsActivity";
    private ActivityAnalyticsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarAnalytics);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbarAnalytics.setNavigationOnClickListener(v -> finish());

        setupChart();
        fetchAnalyticsData();
    }

    private void setupChart() {
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.setDrawGridBackground(false);
        binding.barChart.getAxisRight().setEnabled(false);
        
        XAxis xAxis = binding.barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        
        binding.barChart.getAxisLeft().setAxisMinimum(0f); // Minimum 0 hours
        binding.barChart.getAxisLeft().setAxisMaximum(12f); // Max 12 hours for scale
    }

    private void fetchAnalyticsData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            binding.progressBarAnalytics.setVisibility(View.GONE);
            return;
        }

        // Calculate 7 days ago timestamp
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6); // Today + 6 previous days = 7 days
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long sevenDaysAgoMillis = cal.getTimeInMillis();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(user.getUid())
                .collection("sleepLogs")
                .whereGreaterThanOrEqualTo("endTimeMillis", sevenDaysAgoMillis)
                .orderBy("endTimeMillis", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.progressBarAnalytics.setVisibility(View.GONE);
                    processDataForChart(queryDocumentSnapshots);
                })
                .addOnFailureListener(e -> {
                    binding.progressBarAnalytics.setVisibility(View.GONE);
                    Log.e(TAG, "Error fetching analytics data", e);
                    Toast.makeText(AnalyticsActivity.this, "Failed to load trends.", Toast.LENGTH_SHORT).show();
                });
    }

    private void processDataForChart(Iterable<QueryDocumentSnapshot> snapshots) {
        // Group durations by Date String (e.g., "Oct 24") using a TreeMap to sort automatically
        TreeMap<String, Double> dailySleepMap = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());

        for (QueryDocumentSnapshot doc : snapshots) {
            Long endTime = doc.getLong("endTimeMillis");
            Long durationMillis = doc.getLong("durationMillis");
            
            if (endTime != null && durationMillis != null) {
                String dateStr = sdf.format(new Date(endTime));
                double hours = durationMillis / (1000.0 * 60.0 * 60.0);
                
                // Accumulate if multiple sleeps in one day
                double currentHours = dailySleepMap.getOrDefault(dateStr, 0.0);
                dailySleepMap.put(dateStr, currentHours + hours);
            }
        }

        if (dailySleepMap.isEmpty()) {
            Toast.makeText(this, "Not enough data for the past 7 days.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Double> entry : dailySleepMap.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            xLabels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Hours Slept");
        dataSet.setColor(Color.parseColor("#4A90E2"));
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        binding.barChart.setData(barData);
        
        binding.barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        binding.barChart.invalidate(); // refresh chart
    }
}