package com.example.smokedetection;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

public class StreamActivity extends AppCompatActivity {

    private WebView webView;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);

        // Bind Views
        webView = findViewById(R.id.webViewStream);
        btnBack = findViewById(R.id.btnBack);

        // Configure WebView for Live Streaming
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false); // Hide the ugly buttons

        // Load the Python Stream URL
        // Make sure your Laptop IP in ApiClient.java is correct!
        String streamUrl = ApiClient.getStreamUrl();
        webView.loadUrl(streamUrl);

        Toast.makeText(this, "Connecting to Camera...", Toast.LENGTH_SHORT).show();

        // Handle Back Button
        btnBack.setOnClickListener(v -> {
            // Stop loading to save battery/bandwidth
            webView.stopLoading();
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Make sure stream stops when we leave this screen
        if (webView != null) {
            webView.loadUrl("about:blank");
        }
    }
}