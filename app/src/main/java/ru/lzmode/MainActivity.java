package ru.lzmode;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.squareup.picasso.Picasso;
import org.json.JSONObject;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private ImageButton playButton;
    private boolean isPlaying = false;
    private ImageView trackCoverImage;
    private TextView trackTitle;
    private TextView trackAuthor;
    private Handler trackInfoHandler;
    private Runnable trackInfoRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trackCoverImage = findViewById(R.id.trackCoverImage);
        trackTitle = findViewById(R.id.trackTitle);
        trackAuthor = findViewById(R.id.trackAuthor);
        playButton = findViewById(R.id.playButton);

        String streamUrl = "https://cast.lzmode.online/listen/lzmode/radio.mp3";

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(streamUrl);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

        playButton.setOnClickListener(v -> togglePlayback());

        mediaPlayer.setOnCompletionListener(mp -> resetPlaybackState());

        trackInfoHandler = new Handler();
        trackInfoRunnable = new Runnable() {
            @Override
            public void run() {
                fetchTrackInfo("https://cast.lzmode.online/api/nowplaying/lzmode");
                trackInfoHandler.postDelayed(this, 5000); //update kazhdie 5 sekund
            }
        };
        trackInfoHandler.post(trackInfoRunnable);
    }

    private void togglePlayback() {
        if (!isPlaying) {
            mediaPlayer.start();
            playButton.setImageResource(android.R.drawable.ic_media_pause);
            isPlaying = true;
        } else {
            mediaPlayer.pause();
            playButton.setImageResource(android.R.drawable.ic_media_play);
            isPlaying = false;
        }
    }

    private void resetPlaybackState() {
        playButton.setImageResource(android.R.drawable.ic_media_play);
        isPlaying = false;
    }

    private void fetchTrackInfo(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseData = response.body().string();
                    MainActivity.this.runOnUiThread(() -> updateTrackInfo(responseData));
                }
            }
        });
    }

    private void updateTrackInfo(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONObject nowPlaying = jsonObject.getJSONObject("now_playing");
            JSONObject song = nowPlaying.getJSONObject("song");
            final String artist = song.getString("artist");
            final String title = song.getString("title");
            final String artUrl = song.getString("art");

            trackTitle.setText(title);
            trackAuthor.setText(artist);
            Picasso.get().load(artUrl).into(trackCoverImage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trackInfoHandler.removeCallbacks(trackInfoRunnable);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
