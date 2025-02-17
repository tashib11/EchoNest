package echonest.sociogram.connectus;


import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;
import com.example.connectus.R;

public class FullScreenVideoActivity extends AppCompatActivity {
    private PlayerView playerView;
    private ExoPlayer exoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_video);
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        ((Window) window).setStatusBarColor(this.getResources().getColor(R.color.black));


        // Retrieve the video URL
        String videoUrl = getIntent().getStringExtra("videoUrl");

        // Initialize PlayerView and ExoPlayer
        playerView = findViewById(R.id.exoPlayerView);
        exoPlayer = new ExoPlayer.Builder(this).build();

        // Bind ExoPlayer to the PlayerView
        playerView.setPlayer(exoPlayer);

        // Prepare and play video
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();

        // Handle activity close
        findViewById(R.id.closeButton).setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}