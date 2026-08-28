package com.example.botapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    private MediaRecorder mediaRecorder;
    private String currentFilePath = "";
    private boolean isRecording = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable chunkRunnable;
    private static final long CHUNK_DURATION = 60 * 60 * 1000L; // පැයකට වරක්

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnRecordToggle = findViewById(R.id.btnSubmit);
        if (btnRecordToggle != null) {
            btnRecordToggle.setText("පටිගත කිරීම අරඹන්න");

            btnRecordToggle.setOnClickListener(v -> {
                if (checkAudioPermission()) {
                    if (!isRecording) {
                        startRecordingSession();
                        btnRecordToggle.setText("පටිගත කිරීම නවත්වන්න");
                        isRecording = true;
                    } else {
                        stopRecordingSession();
                        btnRecordToggle.setText("පටිගත කිරීම අරඹන්න");
                        isRecording = false;
                    }
                } else {
                    requestAudioPermission();
                }
            });
        }
    }

    private boolean checkAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                PERMISSION_REQUEST_CODE
        );
    }

    private void startRecordingSession() {
        startNewChunk();

        chunkRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    switchToNextChunk();
                    handler.postDelayed(this, CHUNK_DURATION);
                }
            }
        };
        handler.postDelayed(chunkRunnable, CHUNK_DURATION);
    }

    private void startNewChunk() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File cacheDir = getExternalCacheDir();
        if (cacheDir != null) {
            currentFilePath = cacheDir.getAbsolutePath() + "/audio_" + timeStamp + ".3gp";
        }

        mediaRecorder = new MediaRecorder();
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void switchToNextChunk() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            if (!currentFilePath.isEmpty()) {
                scheduleUpload(currentFilePath);
            }

            startNewChunk();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingSession() {
        try {
            handler.removeCallbacks(chunkRunnable);

            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            if (!currentFilePath.isEmpty()) {
                scheduleUpload(currentFilePath);
            }

            Toast.makeText(this, "පටිගත කිරීම සම්පූර්ණයෙන්ම නැවැතුණි.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scheduleUpload(String filePath) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data inputData = new Data.Builder()
                .putString("file_path", filePath)
                .build();

        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(UploadWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(uploadWorkRequest);
    }
}
