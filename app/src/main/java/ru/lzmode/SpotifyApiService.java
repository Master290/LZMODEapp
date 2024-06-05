package ru.lzmode;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public class SpotifyApiService {
    private static final String BASE_URL = "https://api.spotify.com/v1/";

    private Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private SpotifyApi spotifyApi = retrofit.create(SpotifyApi.class);

    public interface SpotifyApi {
        @GET("search")
        Call<SpotifySearchResponse> searchTrack(@Header("Authorization") String auth, @Query("q") String query, @Query("type") String type);
    }

    public void searchTrack(String accessToken, String query, Callback<SpotifySearchResponse> callback) {
        String auth = "Bearer " + accessToken;
        Call<SpotifySearchResponse> call = spotifyApi.searchTrack(auth, query, "track");
        call.enqueue(callback);
    }
}
