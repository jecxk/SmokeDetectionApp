package com.example.smokedetection;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // 1. Bind Views
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageView imgResult = findViewById(R.id.imgResult);
        VideoView vidResult = findViewById(R.id.vidResult);

        // 2. Handle Back Button
        // When clicked, just finish() this activity to go back to Main
        btnBack.setOnClickListener(v -> finish());

        // 3. Get Data sent from MainActivity
        String url = getIntent().getStringExtra("url");
        boolean isVideo = getIntent().getBooleanExtra("is_video", false);

        if (url == null) {
            Toast.makeText(this, "Error: No media found", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Display Logic
        if (isVideo) {
            // === VIDEO MODE ===
            imgResult.setVisibility(View.GONE);
            vidResult.setVisibility(View.VISIBLE);

            // Set up video player
            Uri videoUri = Uri.parse(url);
            vidResult.setVideoURI(videoUri);

            // Add controls (Pause/Play bar)
            MediaController mediaController = new MediaController(this);
            vidResult.setMediaController(mediaController);
            mediaController.setAnchorView(vidResult);

            // Auto-start playing
            vidResult.setOnPreparedListener(mp -> vidResult.start());

        } else {
            // === IMAGE MODE ===
            vidResult.setVisibility(View.GONE);
            imgResult.setVisibility(View.VISIBLE);

            // Load image using Glide (handles web URLs smoothly)
            Glide.with(this)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Force reload
                    .skipMemoryCache(true)
                    .into(imgResult);
        }
    }
}