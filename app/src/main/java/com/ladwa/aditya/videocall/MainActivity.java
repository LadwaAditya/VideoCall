package com.ladwa.aditya.videocall;

import android.Manifest;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.tbruyelle.rxpermissions.RxPermissions;
import com.twilio.video.CameraCapturer;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalMedia;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.Room;
import com.twilio.video.RoomState;
import com.twilio.video.VideoRenderer;
import com.twilio.video.VideoView;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private static final String TWILIO_ACCESS_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImN0eSI6InR3aWxpby1mcGE7dj0xIn0.eyJqdGkiOiJTS2I0OWNkMWEwYWM0MTU3MGYwNjcyY2RhYWY5MDg5OWIxLTE0OTE4MTY0MjAiLCJpc3MiOiJTS2I0OWNkMWEwYWM0MTU3MGYwNjcyY2RhYWY5MDg5OWIxIiwic3ViIjoiQUNlNTUzNzExNWVmZTNhZThhZWFiYTAzOTUzMGMxOWEyNiIsImV4cCI6MTQ5MTgyMDAyMCwiZ3JhbnRzIjp7ImlkZW50aXR5IjoiQW5kcm9pZCIsInJ0YyI6eyJjb25maWd1cmF0aW9uX3Byb2ZpbGVfc2lkIjoiVlNhZTFmODZmMmMyYWUzYmY4MTg0OGJjMGExMjZkNzMzZiJ9fX0.NkuJMQzTRMinibTWd4TzaUO1soTo02XnHQkMxKVMg1I";
    private static final String TAG = "VideoActivity";

    private static final String CLIENT_IDENTITY = "Android";
    public static final String ACCOUNT_SID = "ACe5537115efe3ae8aeaba039530c19a26";
    public static final String API_KEY_SID = "SKb5eeda6d59372fe0e97806b6f32db2c8";

    private VideoView primaryVideoView;
    private VideoView thumbnailVideoView;
    private AudioManager audioManager;

    private LocalMedia localMedia;
    private LocalAudioTrack localAudioTrack;
    private LocalVideoTrack localVideoTrack;
    private CameraCapturer cameraCapturer;
    private VideoRenderer localVideoView;

    private Room room;
    private boolean disconnectedFromOnDestroy;



    private static boolean permissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        primaryVideoView = (VideoView) findViewById(R.id.primary_video_view);
        thumbnailVideoView = (VideoView) findViewById(R.id.thumbnail_video_view);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);


        grantPermission();
        if (permissionGranted) {
            startVideo();
        } else {
            Toast.makeText(this, "Please grant permissions", Toast.LENGTH_SHORT).show();
        }
    }

    private void startVideo() {
        createLocalMedia();

    }


    private void createLocalMedia() {
        localMedia = LocalMedia.create(this);

        // Share your microphone
        localAudioTrack = localMedia.addAudioTrack(true);

        // Share your camera
        cameraCapturer = new CameraCapturer(this, CameraCapturer.CameraSource.FRONT_CAMERA);
        localVideoTrack = localMedia.addVideoTrack(true, cameraCapturer);
        primaryVideoView.setMirror(true);
        localVideoTrack.addRenderer(primaryVideoView);
        localVideoView = primaryVideoView;
    }

    @Override
    protected  void onResume() {
        super.onResume();
        if (localMedia != null && localVideoTrack == null) {
            localVideoTrack = localMedia.addVideoTrack(true, cameraCapturer);
            localVideoTrack.addRenderer(localVideoView);
        }
    }

    @Override
    protected void onPause() {
        if (localMedia != null && localVideoTrack != null) {
            localMedia.removeVideoTrack(localVideoTrack);
            localVideoTrack = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (room != null && room.getState() != RoomState.DISCONNECTED) {
            room.disconnect();
            disconnectedFromOnDestroy = true;
        }

        /*
         * Release the local media ensuring any memory allocated to audio or video is freed.
         */
        if (localMedia != null) {
            localMedia.release();
            localMedia = null;
        }

        super.onDestroy();
    }

    private void grantPermission() {
        final RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (granted) {
                            permissionGranted = true;
                        } else {
                            permissionGranted = false;
                        }
                    }
                });

    }
}
