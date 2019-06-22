package yee.fma.spotifysleep;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.google.gson.Gson;
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
    private int counter;
    private Track firstTrack;
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            System.out.println("Current song number: " + counter + " \n current song: " + curTrack.name);
            if (curTrack.name != firstTrack.name) {
                counter++;
            }
            if (counter == numSongs) {
                System.out.println("Last song starting. Timeout for " + TimeUnit.MILLISECONDS.toSeconds(curTrack.duration));
                //sleep?
                try {
                    // sleep 10 sec before fetching final sleep time because of callbacks - this can be implemented better?
                    Thread.sleep(10000);
                    Thread.sleep(curTrack.duration - 10000);
                } catch (InterruptedException e) {

                } finally {
                    System.out.println("Last song finished");
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
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public BackgroundService(String name) {
        super(name);
    }

    public BackgroundService() {
        super("BackgroundService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println("YEEFMA");
        getCurrentTrack();
        counter = Integer.parseInt(intent.getStringExtra("counter"));
        numSongs = Integer.parseInt(intent.getStringExtra("numSongs"));
        firstTrack = (new Gson()).fromJson(intent.getStringExtra("firstTrack"), Track.class);
        curTrack = firstTrack;
        startRepeatingTask();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "Service created!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        /* IF YOU WANT THIS SERVICE KILLED WITH THE APP THEN UNCOMMENT THE FOLLOWING LINE */
        //handler.removeCallbacks(runnable);
        Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStart(Intent intent, int startid) {
        Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();
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