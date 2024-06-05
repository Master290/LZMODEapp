package ru.lzmode;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LikedTracksActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TracksAdapter tracksAdapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liked_tracks);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);

        List<Track> likedTracks = getLikedTracks();
        tracksAdapter = new TracksAdapter(likedTracks);
        recyclerView.setAdapter(tracksAdapter);
    }

    private List<Track> getLikedTracks() {
        List<Track> tracks = new ArrayList<>();
        Cursor cursor = dbHelper.getReadableDatabase().query(
                DatabaseHelper.TABLE_TRACKS,
                null, null, null, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String title = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE));
                    String artist = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ARTIST));
                    tracks.add(new Track(title, artist));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } else {
            Toast.makeText(this, "No liked tracks found", Toast.LENGTH_SHORT).show();
        }
        return tracks;
    }
}
