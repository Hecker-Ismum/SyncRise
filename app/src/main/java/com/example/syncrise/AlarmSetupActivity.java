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
    }
}