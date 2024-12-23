package echonest.sociogram.connectus;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.connectus.R;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.connectus.R;

public class FullScreenImageActivity extends AppCompatActivity {

    private long downloadId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ImageView fullScreenImageView = findViewById(R.id.fullScreenImageView);
        Button downloadButton = findViewById(R.id.downloadButton);

        // Get the image URL from the intent
        String imageUrl = getIntent().getStringExtra("image_url");

        // Load the image using Glide
        Glide.with(this)
                .load(imageUrl)
                .into(fullScreenImageView);

        // Register the receiver for download complete
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        // Handle download button click
        downloadButton.setOnClickListener(v -> {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                downloadImage(imageUrl);
            } else {
                Toast.makeText(this, "Image URL is invalid", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadImage(String imageUrl) {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(imageUrl);
            DownloadManager.Request request = new DownloadManager.Request(uri);

            // Set destination folder and file name
            String fileName = "image_" + System.currentTimeMillis() + ".jpg";
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            // Set download properties
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setTitle("Downloading Image");
            request.setDescription("Image is being downloaded...");

            // Enqueue download and save the download ID
            downloadId = downloadManager.enqueue(request);
            Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // BroadcastReceiver to listen for download completion
    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadId) {
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver to avoid memory leaks
        unregisterReceiver(onDownloadComplete);
    }
}
