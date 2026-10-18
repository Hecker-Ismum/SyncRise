package com.example.syncrise;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.syncrise.databinding.ActivityMissionBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class MissionActivity extends AppCompatActivity {

    private ActivityMissionBinding binding;
    private MediaPlayer mediaPlayer;

    // Math Mission Variables
    private int mathProblemsSolved = 0;
    private int currentMathAnswer = 0;
    private static final int TOTAL_MATH_PROBLEMS = 3;

    // Barcode Mission Variables
    private static final String TARGET_BARCODE = "SYNCRISE_WAKEUP_123";
    private static final int CAMERA_PERMISSION_CODE = 101;
    private String missionType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Configure Window Flags for Lock-Screen Wake-up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        binding = ActivityMissionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. Override Back Button to prevent dismissal
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(MissionActivity.this, "Mission incomplete. Cannot exit!", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Initialize Audio Loop
        startAlarmAudio();

        // 4. Setup Mission Mode
        missionType = getIntent().getStringExtra("MISSION_TYPE");
        if (missionType == null) {
            missionType = "Math Problem";
        }

        binding.tvMissionTitle.setText("Mission: " + missionType);

        if ("Barcode".equalsIgnoreCase(missionType)) {
            setupBarcodeMission();
        } else {
            setupMathMission();
        }
    }

    private void startAlarmAudio() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopAlarmAndFinish() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        Toast.makeText(this, "Mission Completed! Good Morning.", Toast.LENGTH_LONG).show();
        finish();
    }

    // ======================= MATH MISSION =======================
    private void setupMathMission() {
        binding.mathLayout.setVisibility(View.VISIBLE);
        binding.previewView.setVisibility(View.GONE);

        generateMathProblem();

        binding.btnSubmitMath.setOnClickListener(v -> {
            String inputStr = binding.etMathAnswer.getText().toString();
            if (!inputStr.isEmpty()) {
                int input = Integer.parseInt(inputStr);
                if (input == currentMathAnswer) {
                    mathProblemsSolved++;
                    if (mathProblemsSolved >= TOTAL_MATH_PROBLEMS) {
                        stopAlarmAndFinish();
                    } else {
                        int remaining = TOTAL_MATH_PROBLEMS - mathProblemsSolved;
                        Toast.makeText(this, "Correct! " + remaining + " left.", Toast.LENGTH_SHORT).show();
                        generateMathProblem();
                    }
                } else {
                    Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show();
                    binding.etMathAnswer.setText("");
                }
            }
        });
    }

    private void generateMathProblem() {
        Random random = new Random();
        int num1 = random.nextInt(10) + 1; // 1 to 10
        int num2 = random.nextInt(10) + 1; // 1 to 10
        int num3 = random.nextInt(20) + 1; // 1 to 20

        currentMathAnswer = (num1 * num2) + num3;
        binding.tvMathEquation.setText(num1 + " * " + num2 + " + " + num3 + " = ?");
        binding.etMathAnswer.setText("");
    }

    // ===================== BARCODE MISSION ======================
    private void setupBarcodeMission() {
        binding.mathLayout.setVisibility(View.GONE);
        binding.previewView.setVisibility(View.VISIBLE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    @SuppressLint("UnsafeOptInUsageError")
                    android.media.Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        BarcodeScanner scanner = BarcodeScanning.getClient();
                        scanner.process(image)
                                .addOnSuccessListener(barcodes -> {
                                    for (Barcode barcode : barcodes) {
                                        String rawValue = barcode.getRawValue();
                                        if (TARGET_BARCODE.equals(rawValue)) {
                                            stopAlarmAndFinish();
                                            break;
                                        }
                                    }
                                })
                                .addOnCompleteListener(task -> imageProxy.close());
                    } else {
                        imageProxy.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else if ("Barcode".equalsIgnoreCase(missionType)) {
                Toast.makeText(this, "Camera permission is required for Barcode Mission!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}