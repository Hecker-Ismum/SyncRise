package com.example.syncrise;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.syncrise.databinding.ActivitySoundscapeBinding;

import java.util.Locale;

public class SoundscapeActivity extends AppCompatActivity {

    private ActivitySoundscapeBinding binding;
    private MediaPlayer mediaPlayer;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySoundscapeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarSoundscape);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarSoundscape.setNavigationOnClickListener(v -> finish());

        binding.btnRain.setOnClickListener(v -> playSoundscape("Rain"));
        binding.btnOcean.setOnClickListener(v -> playSoundscape("Ocean Waves"));
        binding.btnWhiteNoise.setOnClickListener(v -> playSoundscape("White Noise"));

        binding.btnStopSound.setOnClickListener(v -> stopSoundscape());
    }

    private void playSoundscape(String soundName) {
        stopSoundscape(); // Clear previous

        Toast.makeText(this, "Playing " + soundName + " (Mock)", Toast.LENGTH_SHORT).show();
        
        try {
            // Using default ringtone as a placeholder for actual raw audio files (e.g., R.raw.rain)
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, soundUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();

            startTimer();
            binding.btnStopSound.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        // 15 minutes = 15 * 60 * 1000 = 900,000 ms
        countDownTimer = new CountDownTimer(900000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                binding.tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                binding.tvTimer.setText("00:00");
                stopSoundscape();
            }
        }.start();
    }

    private void stopSoundscape() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        binding.tvTimer.setText("15:00");
        binding.btnStopSound.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSoundscape();
    }
}