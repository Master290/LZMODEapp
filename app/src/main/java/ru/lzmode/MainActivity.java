package ru.lzmode;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private MaterialButton playButton;
    private ProgressBar loadingIndicator;
    private boolean isPlaying = false;
    private ImageView trackCoverImage;
    private TextView trackTitle;
    private TextView trackAuthor;
    private TextView liveDj;
    private Handler trackInfoHandler;
    private Runnable trackInfoRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        trackCoverImage = findViewById(R.id.trackCoverImage);
        trackTitle = findViewById(R.id.trackTitle);
        trackAuthor = findViewById(R.id.trackAuthor);
        liveDj = findViewById(R.id.liveDj);
        playButton = findViewById(R.id.playButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);

        playButton.setOnClickListener(v -> togglePlayback());

        trackInfoHandler = new Handler();
        trackInfoRunnable = new Runnable() {
            @Override
            public void run() {
                fetchTrackInfo("https://cast.lzmode.online/api/nowplaying");
                trackInfoHandler.postDelayed(this, 5000);
            }
        };
        trackInfoHandler.post(trackInfoRunnable);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void initializeMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            String streamUrl = "https://cast.lzmode.online/listen/lzmode/mobileapp.mp3";
            mediaPlayer.setDataSource(streamUrl);
            mediaPlayer.setOnPreparedListener(mp -> {
                loadingIndicator.setVisibility(View.GONE);
                playButton.setVisibility(View.VISIBLE);
                mp.start();
                playButton.setText("Pause");
                playButton.setIconResource(R.drawable.ic_pause);
                isPlaying = true;
            });
            mediaPlayer.prepareAsync();
            loadingIndicator.setVisibility(View.VISIBLE);
            playButton.setVisibility(View.GONE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void togglePlayback() {
        if (!isPlaying) {
            initializeMediaPlayer();
        } else {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            playButton.setText("Play");
            playButton.setIconResource(R.drawable.ic_play_arrow);
            isPlaying = false;
        }
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
            JSONArray jsonArray = new JSONArray(jsonData);
            if (jsonArray.length() > 0) {
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                JSONObject nowPlaying = jsonObject.getJSONObject("now_playing");
                JSONObject song = nowPlaying.getJSONObject("song");
                final String artist = song.getString("artist");
                final String title = song.getString("title");
                final String artUrl = song.getString("art");

                JSONObject live = jsonObject.getJSONObject("live");
                final String djName = live.getString("streamer_name");

                trackTitle.setText(title);
                trackAuthor.setText(artist);
                liveDj.setText(djName);
                Picasso.get()
                        .load(artUrl)
                        .transform(new RoundedTransformation(50, 0))
                        .into(trackCoverImage);
            }
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
