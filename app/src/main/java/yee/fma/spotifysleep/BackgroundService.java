package yee.fma.spotifysleep;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.util.concurrent.TimeUnit;

public class BackgroundService extends IntentService {

    public static SpotifyAppRemote mSpotifyAppRemote = null;
    public Context context = this;
    public Handler handler = null;
    private Subscription<PlayerState> playerState;
    private Track curTrack;
    private Handler mHandler = new Handler();
    private int numSongs;
    private int counter = 1;
    private Track firstTrack;
    long startTime;
    private long totalTime;
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            totalTime += curTrack.duration;
            Log.d("TEST", "Current totaltime: " + totalTime);
            Log.d("TEST", "Current track duration: " + curTrack.duration);
            if (totalTime >= 360000) {
                try {
                    Thread.sleep(System.currentTimeMillis() - startTime - 3000);
                } catch (InterruptedException e) {
                } finally {
                    Log.d("TEST", "Last song finished");
                    mSpotifyAppRemote.getPlayerApi().pause();
                    playerState.cancel();

                }
            }
            Log.d("TEST", "Current song number: " + counter + " \n current song: " + curTrack.name);
            if (curTrack.name != firstTrack.name) {
                counter++;
            }
            if (counter == numSongs) {

                Log.d("TEST", "Last song starting. Timeout for " + TimeUnit.MILLISECONDS.toSeconds(curTrack.duration));
                //sleep?
                try {
                    // sleep 10 sec before fetching final sleep time because of callbacks - this can be implemented better?
                    Thread.sleep(10000);
                    Thread.sleep(curTrack.duration - 10000);
                } catch (InterruptedException e) {

                } finally {
                    Log.d("TEST", "Last song finished");
                    mSpotifyAppRemote.getPlayerApi().pause();
                    playerState.cancel();
                }
            } else if (counter < numSongs) {
                mHandler.postDelayed(mRunnable, curTrack.duration);
            }
        }
    };

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public BackgroundService() {
        super("BackgroundService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        startTime = intent.getLongExtra("startTime", 0);
        PlayerApi playerApi = mSpotifyAppRemote.getPlayerApi();
        playerApi.seekTo(0);
        playerApi.resume();
        Log.d("TEST", "YEE");
        getCurrentTrack();
        numSongs = intent.getIntExtra("numSongs", 3);
        firstTrack = (new Gson()).fromJson(intent.getStringExtra("firstTrack"), Track.class);
        curTrack = firstTrack;
        startRepeatingTask();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
}