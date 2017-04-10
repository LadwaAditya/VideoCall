package com.ladwa.aditya.videocall;

import android.Manifest;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.tbruyelle.rxpermissions.RxPermissions;
import com.twilio.video.AudioTrack;
import com.twilio.video.CameraCapturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalMedia;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.Media;
import com.twilio.video.Participant;
import com.twilio.video.Room;
import com.twilio.video.RoomState;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoRenderer;
import com.twilio.video.VideoTrack;
import com.twilio.video.VideoView;

import java.util.Map;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private static final String TWILIO_ACCESS_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImN0eSI6InR3aWxpby1mcGE7dj0xIn0.eyJqdGkiOiJTS2I0OWNkMWEwYWM0MTU3MGYwNjcyY2RhYWY5MDg5OWIxLTE0OTE4MzA1MTciLCJpc3MiOiJTS2I0OWNkMWEwYWM0MTU3MGYwNjcyY2RhYWY5MDg5OWIxIiwic3ViIjoiQUNlNTUzNzExNWVmZTNhZThhZWFiYTAzOTUzMGMxOWEyNiIsImV4cCI6MTQ5MTgzNDExNywiZ3JhbnRzIjp7ImlkZW50aXR5IjoiY29tLmxhZHdhLmFkaXR5YS52aWRlb2NhbGwiLCJydGMiOnsiY29uZmlndXJhdGlvbl9wcm9maWxlX3NpZCI6IlZTYWUxZjg2ZjJjMmFlM2JmODE4NDhiYzBhMTI2ZDczM2YifX19.wcbIhpTYAtfdrhe5Uhbb4B1uFP1xbsTmyTbho56jNfw";
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

    private String participantIdentity;


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

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


        grantPermission();
        if (permissionGranted) {
            startVideo();
        } else {
            Toast.makeText(this, "Please grant permissions", Toast.LENGTH_SHORT).show();
        }
    }

    private void startVideo() {
        createLocalMedia();
        connectToRoom("defaultRoom");
    }

    private void connectToRoom(String roomName) {
        ConnectOptions connectOptions = new ConnectOptions.Builder(TWILIO_ACCESS_TOKEN)
                .roomName(roomName)
                .localMedia(localMedia)
                .build();
        room = Video.connect(this, connectOptions, roomListener());
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
    protected void onResume() {
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

    private void addParticipant(Participant participant) {
        if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
            return;
        }
        participantIdentity = participant.getIdentity();

        if (participant.getMedia().getVideoTracks().size() > 0) {
            addParticipantVideo(participant.getMedia().getVideoTracks().get(0));
        }

        participant.getMedia().setListener(mediaListener());
    }

    private void addParticipantVideo(VideoTrack videoTrack) {
        moveLocalVideoToThumbnailView();
        primaryVideoView.setMirror(false);
        videoTrack.addRenderer(primaryVideoView);
    }

    private void moveLocalVideoToThumbnailView() {
        if (thumbnailVideoView.getVisibility() == View.GONE) {
            thumbnailVideoView.setVisibility(View.VISIBLE);
            localVideoTrack.removeRenderer(primaryVideoView);
            localVideoTrack.addRenderer(thumbnailVideoView);
            localVideoView = thumbnailVideoView;
            thumbnailVideoView.setMirror(cameraCapturer.getCameraSource() ==
                    CameraCapturer.CameraSource.FRONT_CAMERA);
        }
    }


    private void removeParticipant(Participant participant) {
        if (!participant.getIdentity().equals(participantIdentity)) {
            return;
        }

        if (participant.getMedia().getVideoTracks().size() > 0) {
            removeParticipantVideo(participant.getMedia().getVideoTracks().get(0));
        }
        participant.getMedia().setListener(null);
        moveLocalVideoToPrimaryView();
    }

    private void moveLocalVideoToPrimaryView() {
        if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
            localVideoTrack.removeRenderer(thumbnailVideoView);
            thumbnailVideoView.setVisibility(View.GONE);
            localVideoTrack.addRenderer(primaryVideoView);
            localVideoView = primaryVideoView;
            primaryVideoView.setMirror(cameraCapturer.getCameraSource() ==
                    CameraCapturer.CameraSource.FRONT_CAMERA);
        }
    }

    private void removeParticipantVideo(VideoTrack videoTrack) {
        videoTrack.removeRenderer(primaryVideoView);
    }

    private Room.Listener roomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                setTitle(room.getName());

                for (Map.Entry<String, Participant> entry : room.getParticipants().entrySet()) {
                    addParticipant(entry.getValue());
                    break;
                }
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                Log.e(TAG, e.getExplanation(),e);
                Log.e(TAG, "Error");
                e.printStackTrace();
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {
                MainActivity.this.room = null;
                if (!disconnectedFromOnDestroy) {
                    moveLocalVideoToPrimaryView();
                }
            }

            @Override
            public void onParticipantConnected(Room room, Participant participant) {
                addParticipant(participant);

            }

            @Override
            public void onParticipantDisconnected(Room room, Participant participant) {
                removeParticipant(participant);
            }

            @Override
            public void onRecordingStarted(Room room) {
                Log.d(TAG, "onRecordingStarted");
            }

            @Override
            public void onRecordingStopped(Room room) {
                Log.d(TAG, "onRecordingStopped");
            }
        };
    }

    private Media.Listener mediaListener() {
        return new Media.Listener() {

            @Override
            public void onAudioTrackAdded(Media media, AudioTrack audioTrack) {
                Log.d(TAG,"onAudioTrackAdded");
            }

            @Override
            public void onAudioTrackRemoved(Media media, AudioTrack audioTrack) {
                Log.d(TAG,"onAudioTrackRemoved");

            }

            @Override
            public void onVideoTrackAdded(Media media, VideoTrack videoTrack) {
                addParticipantVideo(videoTrack);
                Log.d(TAG,"onVideoTrackAdded");

            }

            @Override
            public void onVideoTrackRemoved(Media media, VideoTrack videoTrack) {
                removeParticipantVideo(videoTrack);
                Log.d(TAG,"onVideoTrackRemoved");

            }

            @Override
            public void onAudioTrackEnabled(Media media, AudioTrack audioTrack) {

            }

            @Override
            public void onAudioTrackDisabled(Media media, AudioTrack audioTrack) {

            }

            @Override
            public void onVideoTrackEnabled(Media media, VideoTrack videoTrack) {

            }

            @Override
            public void onVideoTrackDisabled(Media media, VideoTrack videoTrack) {

            }
        };
    }
}
