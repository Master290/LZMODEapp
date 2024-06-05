package ru.lzmode;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_PLAY = "ru.lzmode.ACTION_PLAY";
    public static final String ACTION_PAUSE = "ru.lzmode.ACTION_PAUSE";
    private static final int REQUEST_PERMISSIONS = 1;
    private static MainActivity instance;

    private MediaPlayer mediaPlayer;
    private MaterialButton playButton;
    private MaterialButton likeButton;
    private MaterialButton viewLikedTracksButton;
    private ProgressBar loadingIndicator;
    private boolean isPlaying = false;
    private ImageView trackCoverImage;
    private TextView trackTitle;
    private TextView trackAuthor;
    private TextView liveDj;
    private Handler trackInfoHandler;
    private Runnable trackInfoRunnable;
    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        setContentView(R.layout.activity_main);

        // Check and request permissions
        checkAndRequestPermissions();

        createNotificationChannel();

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        trackCoverImage = findViewById(R.id.trackCoverImage);
        trackTitle = findViewById(R.id.trackTitle);
        trackAuthor = findViewById(R.id.trackAuthor);
        liveDj = findViewById(R.id.liveDj);
        playButton = findViewById(R.id.playButton);
        likeButton = findViewById(R.id.likeButton);
        viewLikedTracksButton = findViewById(R.id.viewLikedTracksButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);

        playButton.setOnClickListener(v -> togglePlayback());
        likeButton.setOnClickListener(v -> likeCurrentTrack());
        viewLikedTracksButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LikedTracksActivity.class);
            startActivity(intent);
        });

        trackInfoHandler = new Handler();
        trackInfoRunnable = new Runnable() {
            @Override
            public void run() {
                fetchTrackInfo("https://cast.lzmode.online/api/nowplaying");
                trackInfoHandler.postDelayed(this, 5000); // update every 5 seconds
            }
        };
        trackInfoHandler.post(trackInfoRunnable);
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.FOREGROUND_SERVICE
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (!allPermissionsGranted) {
                // Handle the case where permissions are not granted
                // For simplicity, we'll just close the app
                finish();
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Media Playback";
            String description = "Media Playback Controls";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("MEDIA_PLAYBACK_CHANNEL", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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
                showMediaNotification(trackTitle.getText().toString(), trackAuthor.getText().toString(), trackCoverImage.getTag().toString());
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
            pause();
        }
    }

    public void play() {
        if (mediaPlayer == null) {
            initializeMediaPlayer();
        } else {
            mediaPlayer.start();
            playButton.setText("Pause");
            playButton.setIconResource(R.drawable.ic_pause);
            isPlaying = true;
            updateMediaNotification(trackTitle.getText().toString(), trackAuthor.getText().toString(), trackCoverImage.getTag().toString());
        }
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            playButton.setText("Play");
            playButton.setIconResource(R.drawable.ic_play_arrow);
            isPlaying = false;
            removeMediaNotification();
        }
    }

    private void showMediaNotification(String title, String artist, String artUrl) {
        notificationManager = NotificationManagerCompat.from(this);

        Picasso.get().load(artUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                notificationBuilder = new NotificationCompat.Builder(MainActivity.this, "MEDIA_PLAYBACK_CHANNEL")
                        .setSmallIcon(R.drawable.ic_music_note)
                        .setContentTitle(title)
                        .setContentText(artist)
                        .setLargeIcon(bitmap)
                        .setStyle(new MediaStyle()
                                .setShowActionsInCompactView(0, 1)
                                .setMediaSession(new MediaSessionCompat(MainActivity.this, "MediaSession").getSessionToken()))
                        .addAction(new NotificationCompat.Action(
                                R.drawable.ic_pause, "Pause", getActionIntent(ACTION_PAUSE)))
                        .addAction(new NotificationCompat.Action(
                                R.drawable.ic_play_arrow, "Play", getActionIntent(ACTION_PLAY)))
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true);

                notificationManager.notify(1, notificationBuilder.build());
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                // Handle failure
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                // Handle preparation
            }
        });
    }

    private PendingIntent getActionIntent(String action) {
        Intent intent = new Intent(this, MediaActionReceiver.class);
        intent.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(this, 0, intent, flags);
    }

    private void updateMediaNotification(String title, String artist, String artUrl) {
        showMediaNotification(title, artist, artUrl);
    }

    private void removeMediaNotification() {
        notificationManager.cancel(1);
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

                fetchCoverImageFromSpotify(artist, title);

                JSONObject live = jsonObject.getJSONObject("live");
                final String djName = live.getString("streamer_name");

                trackTitle.setText(title);
                trackAuthor.setText(artist);
                liveDj.setText(djName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchCoverImageFromSpotify(String artist, String title) {
        try {
            String accessToken = SpotifyAuth.getAccessToken();
            SpotifyApiService apiService = new SpotifyApiService();
            apiService.searchTrack(accessToken, artist + " " + title, new Callback<SpotifySearchResponse>() {
                @Override
                public void onResponse(Call<SpotifySearchResponse> call, retrofit2.Response<SpotifySearchResponse> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().getTracks().getItems().isEmpty()) {
                        String imageUrl = response.body().getTracks().getItems().get(0).getAlbum().getImages().get(0).getUrl();
                        trackCoverImage.setTag(imageUrl);
                        Picasso.get()
                                .load(imageUrl)
                                .transform(new RoundedTransformation(30, 0))
                                .into(trackCoverImage);

                        if (isPlaying) {
                            updateMediaNotification(title, artist, imageUrl);
                        }
                    }
                }

                @Override
                public void onFailure(Call<SpotifySearchResponse> call, Throwable t) {
                    // Handle failure
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void likeCurrentTrack() {
        String title = trackTitle.getText().toString();
        String artist = trackAuthor.getText().toString();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TITLE, title);
        values.put(DatabaseHelper.COLUMN_ARTIST, artist);

        db.insert(DatabaseHelper.TABLE_TRACKS, null, values);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trackInfoHandler.removeCallbacks(trackInfoRunnable);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        db.close();
    }
}
