package echonest.sociogram.connectus;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.VideoView;

import com.example.connectus.R;

public class FullScreenVideoActivity extends AppCompatActivity {
    private VideoView videoView;
    private ProgressBar progressBar;
    private ImageView playPauseIcon;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_video);

        videoView = findViewById(R.id.videoView);
        progressBar = findViewById(R.id.progressBar);
        playPauseIcon = findViewById(R.id.playIcon);

        String videoUrl = getIntent().getStringExtra("video_url");
        Uri videoUri = Uri.parse(videoUrl);

        videoView.setVideoURI(videoUri);
        videoView.setOnPreparedListener(mp -> {
            progressBar.setVisibility(View.GONE); // Hide loading indicator
            videoView.start();
            isPlaying = true;
            playPauseIcon.setVisibility(View.GONE); // Hide play icon
        });

        videoView.setOnInfoListener((mp, what, extra) -> {
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                progressBar.setVisibility(View.VISIBLE); // Show loading indicator
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                progressBar.setVisibility(View.GONE); // Hide loading indicator
            }
            return false;
        });

        videoView.setOnCompletionListener(mp -> {
            isPlaying = false;
            playPauseIcon.setImageResource(R.drawable.ic_replay_circle); // Show replay icon
            playPauseIcon.setVisibility(View.VISIBLE);
            videoView.seekTo(0);
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            progressBar.setVisibility(View.GONE);
            playPauseIcon.setVisibility(View.VISIBLE);
            playPauseIcon.setImageResource(R.drawable.ic_error); // Show error icon
            return true;
        });

        // Toggle play/pause on video click
        videoView.setOnClickListener(v -> togglePlayback());
        playPauseIcon.setOnClickListener(v -> togglePlayback());
    }

    private void togglePlayback() {
        if (isPlaying) {
            videoView.pause();
            playPauseIcon.setImageResource(R.drawable.ic_play_circle); // Show play icon
            playPauseIcon.setVisibility(View.VISIBLE);
        } else {
            videoView.start();
            playPauseIcon.setImageResource(R.drawable.ic_pause_circle); // Show pause icon
            playPauseIcon.postDelayed(() -> playPauseIcon.setVisibility(View.GONE), 1000);
        }
        isPlaying = !isPlaying;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("playbackPosition", videoView.getCurrentPosition());
        outState.putBoolean("isPlaying", isPlaying);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int playbackPosition = savedInstanceState.getInt("playbackPosition");
        isPlaying = savedInstanceState.getBoolean("isPlaying");
        videoView.seekTo(playbackPosition);
        if (isPlaying) {
            videoView.start();
            playPauseIcon.setVisibility(View.GONE);
        } else {
            playPauseIcon.setVisibility(View.VISIBLE);
        }
    }
}
