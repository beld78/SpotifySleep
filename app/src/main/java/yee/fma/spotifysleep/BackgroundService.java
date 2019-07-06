package yee.fma.spotifysleep;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.authentication.AuthenticationRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public class BackgroundService extends IntentService {
    private Subscription<PlayerState> playerState;
    private Track curTrack;
    private Handler mHandler = new Handler();
    private Track firstTrack;
    private static final String CLIENT_ID = "f3d32cc698f74ec6a22d05d86411f31f";
    private static final String REDIRECT_URI = "spotifysleep://callback";

    public static int numSongs;
    public static int counter;
    public static AuthenticationRequest request = null;
    public static SpotifyAppRemote mSpotifyAppRemote = null;
    public static Connector.ConnectionListener listener = null;


    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {

            Toast.makeText(BackgroundService.this, "Current song number: " + counter + " \n current song: " + curTrack.name, Toast.LENGTH_LONG).show();
            if (curTrack.name != firstTrack.name) {
                counter++;
            }
            if (counter == numSongs) {
                Toast.makeText(BackgroundService.this, "Last song starting. Timeout for " + TimeUnit.MILLISECONDS.toSeconds(curTrack.duration), Toast.LENGTH_LONG).show();
                //sleep?
                try {
                    // sleep 10 sec before fetching final sleep time because of callbacks - this can be implemented better?
                    Thread.sleep(10000);
                    Thread.sleep(curTrack.duration - 10000);
                } catch (InterruptedException e) {

                } finally {
                    Toast.makeText(BackgroundService.this, "Last song finished", Toast.LENGTH_LONG).show();
                    mSpotifyAppRemote.getPlayerApi().pause();
                    playerState.cancel();
                    mHandler.removeCallbacks(mRunnable);
                }
            } else if (counter < numSongs) {
                mHandler.postDelayed(mRunnable, curTrack.duration);
            }
        }
    };

    public BackgroundService() {
        super("BackgroundService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("Service:", "Application started");



        getCurrentTrack();
        counter = Integer.parseInt(intent.getStringExtra("counter"));
        numSongs = Integer.parseInt(intent.getStringExtra("numSongs"));
        firstTrack = (new Gson()).fromJson(intent.getStringExtra("firstTrack"), Track.class);
        curTrack = firstTrack;
        PlayerApi playerApi = mSpotifyAppRemote.getPlayerApi();
        playerApi.seekTo(0);
        playerApi.resume();
        startRepeatingTask();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStart(Intent intent, int startid) {
    }

    private void startRepeatingTask() {
        mRunnable.run();
    }

    private void getCurrentTrack() {
        playerState = mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState();
        playerState.setEventCallback(playerState -> {
            curTrack = playerState.track;
            if (curTrack != null) {
            }
        });
    }

    private String getAccessTokenFromJsonStr(String spotifyJsonStr) throws JSONException {
        final String OWM_ACCESS_TOKEN = "access_token";
        String accessToken;

        try {
            JSONObject spotifyJson = new JSONObject(spotifyJsonStr);
            accessToken = spotifyJson.getString(OWM_ACCESS_TOKEN);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        return accessToken;
    }

    private String getSpotifyAccessToken() {
        String response;
        String accessToken;
        try {
            String serviceURL = "https://accounts.spotify.com/api/token";
            URL myURL = new URL(serviceURL);

            HttpsURLConnection myURLConnection = (HttpsURLConnection) myURL.openConnection();

            String userCredentials = "YOUR_USER_CREDENTIALS:YOUR_USER_CREDENTIALS";
            int flags = Base64.NO_WRAP | Base64.URL_SAFE;
            byte[] encodedString = Base64.encode(userCredentials.getBytes(), flags);
            String basicAuth = "Basic " + new String(encodedString);
            myURLConnection.setRequestProperty("Authorization", basicAuth);

            myURLConnection.setRequestMethod("POST");
            myURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            myURLConnection.setUseCaches(false);
            myURLConnection.setDoInput(true);
            myURLConnection.setDoOutput(true);
            System.setProperty("http.agent", "");

            HashMap postDataParams = new HashMap<String, String>();
            postDataParams.put("grant_type", "client_credentials");
            OutputStream os = myURLConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, StandardCharsets.UTF_8));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();

            response = "";
            int responseCode = myURLConnection.getResponseCode();

            Log.d(LOG_TAG, "response code is " + responseCode);

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(myURLConnection.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else {
                response = "";
                String errLine;
                String errResponse = "";
                BufferedReader br = new BufferedReader(new InputStreamReader(myURLConnection.getErrorStream()));
                while ((errLine = br.readLine()) != null) {
                    errResponse += errLine;
                }
                Log.d(LOG_TAG, "error response is " + errResponse);

            }

            Log.d(LOG_TAG, "response is " + response);


        } catch (Exception e) {
            e.printStackTrace();
        }

        String accessTokenJsonStr = response;
        try {
            accessToken = getAccessTokenFromJsonStr(accessTokenJsonStr);
            return accessToken;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
}