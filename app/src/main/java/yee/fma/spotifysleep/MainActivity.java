package yee.fma.spotifysleep;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class MainActivity extends AppCompatActivity {
    private static final String CLIENT_ID = "f3d32cc698f74ec6a22d05d86411f31f";
    private static final String REDIRECT_URI = "spotifysleep://callback";
    private SpotifyAppRemote mSpotifyAppRemote;
    private Subscription<PlayerState> playerState;
    private Button button;
    private Button buttonReset;
    private EditText mEdit;
    // Request code will be used to verify if result comes from the login activity. Can be set to any integer.
    private static final int REQUEST_CODE = 1337;
    private Track curTrack;
    private int numSongs;
    private int counter;
    Gson gson;
    private TextView infoText;
    private Button buttonUp;
    private Button buttonDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gson = new Gson();
        button = findViewById(R.id.button);
        buttonReset = findViewById(R.id.buttonReset);
        mEdit = findViewById(R.id.editText);
        infoText = findViewById(R.id.textView);
        buttonUp = findViewById(R.id.buttonUp);
        buttonDown = findViewById(R.id.buttonDown);
        buttonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                int num = Integer.parseInt(mEdit.getText().toString()) + 1;
                mEdit.setText(Integer.toString(num));
            }
        });
        buttonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                int num = Integer.parseInt(mEdit.getText().toString()) - 1;
                if (num > 0) {
                    mEdit.setText(Integer.toString(num));
                } else {
                    infoText.setText("Number of songs cannot be negative!");
                }
            }
        });
    }



    @Override
    protected void onStart() {
        super.onStart();
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-modify-playback-state", "app-remote-control"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    ConnectionParams connectionParams =
                            new ConnectionParams.Builder(CLIENT_ID)
                                    .setRedirectUri(REDIRECT_URI)
                                    .build();
                    SpotifyAppRemote.connect(MainActivity.this, connectionParams,
                            new Connector.ConnectionListener() {

                                @Override
                                public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                                    mSpotifyAppRemote = spotifyAppRemote;
                                    getCurrentTrack();

                                    // Now you can start interacting with App Remote
                                    button.setOnClickListener(new View.OnClickListener() {

                                        @Override
                                        public void onClick(final View v) {
                                            infoText.setText("Successfully started!");
                                            counter = 1;
                                            numSongs = Integer.parseInt(mEdit.getText().toString());
                                            Log.d("TEST", "counter: " + counter + " num songs: " + numSongs);
                                            Intent intent = new Intent(MainActivity.this, BackgroundService.class);
                                            intent.putExtra("counter", counter);
                                            intent.putExtra("numSongs", numSongs);
                                            intent.putExtra("firstTrack", gson.toJson(curTrack, Track.class));
                                            BackgroundService.mSpotifyAppRemote = mSpotifyAppRemote;
                                            MainActivity.this.startService(intent);
                                        }
                                    });
                                    buttonReset.setOnClickListener(new View.OnClickListener() {

                                        @Override
                                        public void onClick(final View v) {
                                            infoText.setText("Reset! Click Start to start again.");
                                            Intent intent = new Intent(MainActivity.this, BackgroundService.class);
                                            stopService(intent);
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(Throwable throwable) {

                                    // Something went wrong when attempting to connect! Handle errors here
                                }
                            });

                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
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
