package com.example.botapp;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Worker.Result;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadWorker extends Worker {

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String filePath = getInputData().getString("file_path");
        if (filePath == null) {
            return Result.failure();
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return Result.success();
        }

        try {
            // ඔබේ Cloudflare Worker එකේ සැබෑ URL එක මෙහි දමන්න
            String workerUrl = "https://telegram-audio-uploader.sybertools66.workers.dev/";
            // Cloudflare Worker එකේ දුන් රහස් මුරපදයම මෙහිද දමන්න
            String secretPassword = "audio@2235";

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("audio", file.getName(),
                            RequestBody.create(file, MediaType.parse("audio/3gpp")))
                    .build();

            Request request = new Request.Builder()
                    .url(workerUrl)
                    .addHeader("X-Secret-Key", secretPassword)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                if (file.exists()) {
                    file.delete();
                }
                return Result.success();
            } else {
                return Result.retry();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
