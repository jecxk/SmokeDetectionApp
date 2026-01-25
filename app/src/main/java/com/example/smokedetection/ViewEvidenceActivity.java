package com.example.smokedetection;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;

public class ViewEvidenceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_evidence);

        WebView webView = findViewById(R.id.webViewImage);
        Button btnClose = findViewById(R.id.btnClose);

        // Enable Zoom
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        // Make image fit screen
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        // Load URL
        String imageUrl = getIntent().getStringExtra("IMAGE_URL");
        if (imageUrl != null) {
            webView.loadUrl(imageUrl);
        }

        // Close
        btnClose.setOnClickListener(v -> finish());
    }
}