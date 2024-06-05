package ru.lzmode;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.FormBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Base64;

public class SpotifyAuth {
    private static final String CLIENT_ID = "587a2227ec9f4cb6b79fcd7bc56dc4d8";
    private static final String CLIENT_SECRET = "b6892be6fce042b29f890dec45c62da6";
    private static final String AUTH_URL = "https://accounts.spotify.com/api/token";

    public static String getAccessToken() throws IOException {
        OkHttpClient client = new OkHttpClient();

        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .build();

        Request request = new Request.Builder()
                .url(AUTH_URL)
                .post(formBody)
                .addHeader("Authorization", basicAuth)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

        String responseBody = response.body().string();
        try {
            JSONObject jsonObject = new JSONObject(responseBody);
            return jsonObject.getString("access_token");
        } catch (JSONException e) {
            e.printStackTrace();
            throw new IOException("Failed to parse access token from response", e);
        }
    }
}
