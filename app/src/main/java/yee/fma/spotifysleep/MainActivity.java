package yee.fma.spotifysleep;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    private int counter = 1;
    private Gson gson;
    private TextView infoText;
    private Button buttonUp;
    private Button buttonDown;
    private Intent intent;

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

        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                infoText.setText("Reset! Click Start to start again.");
                Intent myService = new Intent(MainActivity.this, BackgroundService.class);
                stopService(myService);
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                infoText.setText("Successfully started!");
                counter = 1;
                numSongs = Integer.parseInt(mEdit.getText().toString());
                intent = new Intent(MainActivity.this, BackgroundService.class);
                intent.putExtra("counter", counter);
                intent.putExtra("numSongs", numSongs);
                startService(intent);
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        buildConnection();
    }

    private void buildConnection() {
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-modify-playback-state", "app-remote-control"});
        AuthenticationRequest request = builder.build();
        BackgroundService.request = request;
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);


        this.intent = new Intent(MainActivity.this, BackgroundService.class);
        this.intent.putExtra("counter", counter);
        this.intent.putExtra("numSongs", numSongs);
        startService(this.intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    System.out.println("EXPIRES IN " + response.getExpiresIn() + " YEE12");
                    // Handle successful response - start service
                    this.intent = new Intent(MainActivity.this, BackgroundService.class);
                    this.intent.putExtra("counter", counter);
                    this.intent.putExtra("numSongs", numSongs);
                    startService(this.intent);

                    //auslagern

                    ConnectionParams connectionParams =
                            new ConnectionParams.Builder(CLIENT_ID)
                                    .setRedirectUri(REDIRECT_URI)
                                    .build();
                    SpotifyAppRemote.connect(this, connectionParams,
                            new Connector.ConnectionListener() {
                                @Override
                                public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                                    mSpotifyAppRemote = spotifyAppRemote;
                                    getCurrentTrack();

                                    // Now you can start interacting with App Remote


                                    BackgroundService.listener = this;

                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    Toast.makeText(MainActivity.this, "Something went wrong when attempting to connect", Toast.LENGTH_SHORT).show();

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
