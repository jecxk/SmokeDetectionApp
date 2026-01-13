package com.example.smokedetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;

public class MainActivity extends AppCompatActivity {

    // Supabase credentials for cloud storage
    private static final String SUPABASE_URL = "https://gklovokybxmonmnuoflk.supabase.co";
    private static final String SUPABASE_KEY = "<YOUR_SUPABASE_KEY>";

    // URL for your local Python server (emulator uses 10.0.2.2)
    private static final String SERVER_URL = "http://10.0.2.2:8000";

    private SupabaseClient supabase;
    private boolean isGuest = true;  // Default as guest if not logged in

    // UI elements
    private Button btnUploadImage, btnUploadVideo, btnLogout;
    private View loadingOverlay;

    // Track current processing so multiple uploads do not conflict
    private long currentProcessID = 0;
    private Call currentCall;

    // Launcher to pick images or videos from gallery
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        String type = getContentResolver().getType(selectedUri);
                        boolean isVideo = type != null && type.startsWith("video");
                        processFile(selectedUri, isVideo);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Supabase client
        supabase = new SupabaseClient(SUPABASE_URL, SUPABASE_KEY);
        checkLoginStatus();

        // Bind UI elements
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnUploadVideo = findViewById(R.id.btnUploadVideo);
        btnLogout = findViewById(R.id.btnLogout);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        Button btnStopProcessing = findViewById(R.id.btnStopProcessing);

        // Set up click listeners
        btnUploadImage.setOnClickListener(v -> openGallery("image/*"));
        btnUploadVideo.setOnClickListener(v -> openGallery("video/*"));
        btnLogout.setOnClickListener(v -> logout());

        // Stop current processing if needed
        btnStopProcessing.setOnClickListener(v -> {
            currentProcessID = 0;
            if(currentCall != null) currentCall.cancel();
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(MainActivity.this, "Stop Processing", Toast.LENGTH_SHORT).show();
        });
    }

    // Check if user is logged in and set auth token
    private void checkLoginStatus() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);
        isGuest = (token == null);
        if (!isGuest) supabase.setAuthToken(token);
    }

    // Open gallery to pick image or video
    private void openGallery(String type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(type);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryLauncher.launch(Intent.createChooser(intent, "Select File"));
    }

    // Process the selected file by sending it to the server
    private void processFile(Uri uri, boolean isVideo) {
        Toast.makeText(this, "Processing on Server...", Toast.LENGTH_SHORT).show();
        loadingOverlay.setVisibility(View.VISIBLE);

        long myProcessID = System.currentTimeMillis();
        currentProcessID = myProcessID;

        // Decide bucket based on guest/user and type
        String sourceBucket = isGuest ? (isVideo ? "guest-videos" : "guest-images") : (isVideo ? "user-videos" : "user-images");

        // Convert file to byte array
        byte[] fileBytes = isVideo ? getVideoBytesFromUri(uri) : getImageBytesFromUri(uri);

        if (fileBytes == null) {
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
            loadingOverlay.setVisibility(View.GONE);
            return;
        }

        String mediaType = isVideo ? "video" : "image";
        String filename = "upload_" + System.currentTimeMillis() + (isVideo ? ".mp4" : ".jpg");

        // Send file to detection server
        currentCall = supabase.sendToDetectionServer(SERVER_URL, mediaType, fileBytes, filename, sourceBucket, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (currentProcessID != myProcessID) return;
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Server Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(responseBody);

                        String processedUrl = json.getString("processed_url");
                        boolean isConfirmed = json.optBoolean("confirmed", false);

                        runOnUiThread(() -> {
                            if(currentProcessID != myProcessID) return;
                            handleDetectionResult(processedUrl, "processed", isConfirmed, filename);
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            if (currentProcessID != myProcessID) return;
                            loadingOverlay.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Failed to parse result", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        if (currentProcessID != myProcessID) return;
                        loadingOverlay.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Server Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // Handle results returned from server
    private void handleDetectionResult(String url, String bucket, boolean isConfirmed, String originalFileName) {
        Log.d("UPLOAD", "Processed URL: " + url);

        // Save metadata to database
        saveMetadataToDatabase(bucket, url, originalFileName, isConfirmed);

        runOnUiThread(() -> {
            loadingOverlay.setVisibility(View.GONE);

            if (isConfirmed) {
                Toast.makeText(MainActivity.this, "SMOKING CONFIRMED!", Toast.LENGTH_LONG).show();

                // Open ResultActivity to show processed media
                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.putExtra("url", url);
                intent.putExtra("is_video", url.endsWith(".mp4"));
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Analysis Complete: No smoking detected.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Save file metadata and confirmation status to Supabase database
    private void saveMetadataToDatabase(String bucket, String filePath, String originalFileName, boolean isConfirmed) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("bucket_name", bucket);
            jsonObject.put("file_path", filePath);

            JSONObject meta = new JSONObject();
            meta.put("is_confirmed", isConfirmed);
            meta.put("original_file", originalFileName);
            meta.put("analyzed_at", System.currentTimeMillis());

            jsonObject.put("processed_result", meta);
        } catch (Exception e) {
            e.printStackTrace();
        }

        supabase.insert("media_uploads", jsonObject.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DB", "Database error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) Log.d("DB", "Metadata saved");
            }
        });
    }

    // Logout the user
    private void logout() {
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    // Convert image URI to byte array with correct orientation
    private byte[] getImageBytesFromUri(Uri uri) {
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();

            if (bitmap == null) return null;

            InputStream exifInput = getContentResolver().openInputStream(uri);
            ExifInterface exif = new ExifInterface(exifInput);
            exifInput.close();

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            Matrix matrix = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) matrix.postRotate(90);
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) matrix.postRotate(180);
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) matrix.postRotate(270);

            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix, true
            );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            return out.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Convert video URI to byte array
    private byte[] getVideoBytesFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            inputStream.close();
            return buffer.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}