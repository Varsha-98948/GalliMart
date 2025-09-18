package com.example.gallimart;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseUploader {
    private static final String TAG = "SupabaseUploader";
    private final OkHttpClient client = new OkHttpClient();

    private final String SUPABASE_URL;      // e.g. "https://<project>.supabase.co"
    private final String SUPABASE_ANON_KEY; // anon public key

    public interface UploadCallback {
        void onSuccess(String publicUrl);
        void onError(int httpCode, String body);
        void onException(Exception e);
    }

    public SupabaseUploader(String supabaseUrl, String anonKey) {
        this.SUPABASE_URL = supabaseUrl;
        this.SUPABASE_ANON_KEY = anonKey;
    }

    // --- PUBLIC: upload File object asynchronously ---
    public void uploadFileAsync(String bucketName, String objectPath, File file, UploadCallback cb) {
        if (objectPath.startsWith("/")) objectPath = objectPath.substring(1);
        if (!file.exists()) {
            cb.onException(new IOException("File not found: " + file.getAbsolutePath()));
            return;
        }

        String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + bucketName + "/" + objectPath;
        String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + bucketName + "/" + objectPath;

        String mime = guessMime(file.getName());
        MediaType mediaType = MediaType.parse(mime);

        // NOTE: OkHttp requestBody creation signature differs by version.
        // If your OkHttp version complains, swap the arguments in RequestBody.create(...).
        RequestBody fileBody = RequestBody.create(mediaType, file);

        Request request = new Request.Builder()
                .url(uploadUrl)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("x-upsert", "true")
                .addHeader("Content-Type", mime)
                .put(fileBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Upload failed (io):", e);
                cb.onException(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    Log.d(TAG, "Upload success: " + publicUrl);
                    cb.onSuccess(publicUrl);
                } else {
                    Log.e(TAG, "Upload failed! Code: " + response.code());
                    Log.e(TAG, "Response body: " + respBody);
                    cb.onError(response.code(), respBody);
                }
            }
        });
    }

    // --- PUBLIC: upload Uri (content Uri from gallery) asynchronously ---
    // Copies the Uri into a temp file in app cache then calls uploadFileAsync()
    public void uploadUriAsync(Context ctx, Uri uri, String bucketName, String objectPath, UploadCallback cb) {
        File cacheFile;
        try {
            String filename = queryFileName(ctx, uri);
            cacheFile = new File(ctx.getCacheDir(), "upload_" + System.currentTimeMillis() + "_" + filename);
            try (InputStream in = ctx.getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(cacheFile)) {
                if (in == null) throw new IOException("Unable to open Uri input stream");
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
        } catch (Exception e) {
            cb.onException(e);
            return;
        }

        // call upload, delete temp file after completion
        uploadFileAsync(bucketName, objectPath, cacheFile, new UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                cacheFile.delete();
                cb.onSuccess(publicUrl);
            }
            @Override
            public void onError(int httpCode, String body) {
                cacheFile.delete();
                cb.onError(httpCode, body);
            }
            @Override
            public void onException(Exception e) {
                cacheFile.delete();
                cb.onException(e);
            }
        });
    }

    // --- small helpers ---
    private String guessMime(String name) {
        String mime = URLConnection.guessContentTypeFromName(name);
        return mime == null ? "application/octet-stream" : mime;
    }

    private String queryFileName(Context ctx, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = ctx.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) result = cursor.getString(idx);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                if (cut != -1) result = path.substring(cut + 1);
                else result = path;
            }
        }
        if (result == null) result = "file_" + System.currentTimeMillis();
        return result;
    }
}
