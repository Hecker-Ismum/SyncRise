package com.example.syncrise;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.syncrise.databinding.ActivitySleepQualityBinding;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class SleepQualityActivity extends AppCompatActivity {

    private ActivitySleepQualityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySleepQualityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarSleepQuality);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarSleepQuality.setNavigationOnClickListener(v -> finish());

        long durationMillis = getIntent().getLongExtra("DURATION_MILLIS", 0);
        setupChart(durationMillis);
    }

    private void setupChart(long durationMillis) {
        binding.lineChartSleepQuality.getDescription().setEnabled(false);
        binding.lineChartSleepQuality.setDrawGridBackground(false);
        binding.lineChartSleepQuality.getAxisRight().setEnabled(false);
        binding.lineChartSleepQuality.getLegend().setTextColor(Color.WHITE);

        // X-Axis (Time in Hours)
        XAxis xAxis = binding.lineChartSleepQuality.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);

        // Y-Axis (Sleep Stages)
        YAxis yAxis = binding.lineChartSleepQuality.getAxisLeft();
        yAxis.setTextColor(Color.WHITE);
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(3f);
        yAxis.setLabelCount(4, true);
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0f) return "Deep";
                if (value == 1f) return "Light";
                if (value == 2f) return "REM";
                if (value == 3f) return "Awake";
                return "";
            }
        });

        // Generate Hypnogram Data
        List<Entry> entries = new ArrayList<>();
        double totalHours = durationMillis / (1000.0 * 60 * 60);
        if (totalHours <= 0) totalHours = 8.0; // fallback

        // Simulate 90 min cycles (1.5 hours)
        float currentHour = 0f;
        entries.add(new Entry(currentHour, 3f)); // Start Awake

        while (currentHour < totalHours) {
            // Drop to Light
            currentHour += 0.2f;
            if (currentHour > totalHours) break;
            entries.add(new Entry(currentHour, 1f));

            // Drop to Deep
            currentHour += 0.5f;
            if (currentHour > totalHours) break;
            entries.add(new Entry(currentHour, 0f));

            // Rise to Light
            currentHour += 0.4f;
            if (currentHour > totalHours) break;
            entries.add(new Entry(currentHour, 1f));

            // Rise to REM
            currentHour += 0.4f;
            if (currentHour > totalHours) break;
            entries.add(new Entry(currentHour, 2f));
        }
        
        // Final Awake
        entries.add(new Entry((float) totalHours, 3f));

        LineDataSet dataSet = new LineDataSet(entries, "Sleep Stage");
        dataSet.setColor(Color.parseColor("#00F0FF")); // Neon Cyan
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(false);
        dataSet.setMode(LineDataSet.Mode.STEPPED); // Crucial for Hypnogram look
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        binding.lineChartSleepQuality.setData(lineData);
        binding.lineChartSleepQuality.invalidate();
    }
}