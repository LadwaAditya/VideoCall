package com.ladwa.aditya.videocall;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.tbruyelle.rxpermissions.RxPermissions;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private static final String TWILIO_ACCESS_TOKEN = "TWILIO_ACCESS_TOKEN";
    private static final String TAG = "VideoActivity";


    private static boolean permissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        grantPermission();
        if (permissionGranted) {
            startVideo();
        }
    }

    private void startVideo() {

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
