package com.example.videochatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import java.util.EmptyStackException;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoChatActivity extends AppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener {

    private static String API_KEY = "47489791";
    private final static String SESSION_ID = "2_MX40NzQ4OTc5MX5-MTY1MTAwODM2MDg3MH5TeXo1dzRGb0dGc1JqWkFlOU5WNkp3L0Z-fg";
    private final static String TOKEN = "T1==cGFydG5lcl9pZD00NzQ4OTc5MSZzaWc9NTg1YTRmY2JiODVjYmNjMTM0YTU0MjY3OGFhOGU5MWNkNzg3NzU1NDpzZXNzaW9uX2lkPTJfTVg0ME56UTRPVGM1TVg1LU1UWTFNVEF3T0RNMk1EZzNNSDVUZVhvMWR6UkdiMGRHYzFKcVdrRmxPVTVXTmtwM0wwWi1mZyZjcmVhdGVfdGltZT0xNjUxMDA4Mzg2Jm5vbmNlPTAuODY2OTE2ODMxNDcxODMxJnJvbGU9cHVibGlzaGVyJmV4cGlyZV90aW1lPTE2NTM2MDAzODcmaW5pdGlhbF9sYXlvdXRfY2xhc3NfbGlzdD0=";
    private final static int RC_VIDEO_APP_PERM = 124;

    private ImageView closeVideoChatBtn;
    private DatabaseReference usersRef;
    private String userId = "";

    private FrameLayout mPublisherViewController;
    private FrameLayout mSubscriberViewController;
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        closeVideoChatBtn = findViewById(R.id.close_video_chat_btn);
        closeVideoChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usersRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.child(userId).hasChild("Ringing")){
                            usersRef.child(userId)
                                    .child("Ringing")
                                    .removeValue();

                            if (mPublisher != null){
                                mPublisher.destroy();
                            }

                            if (mSubscriber != null){
                                mSubscriber.destroy();
                            }
                            mSession.disconnect();
                            startActivity(new Intent(VideoChatActivity.this, RegistrationActivity.class));
                            finish();
                        }

                        if (snapshot.child(userId).hasChild("Calling")){
                            usersRef.child(userId)
                                    .child("Calling")
                                    .removeValue();

                            if (mPublisher != null){
                                mPublisher.destroy();
                            }

                            if (mSubscriber != null){
                                mSubscriber.destroy();
                            }
                            mSession.disconnect();
                            startActivity(new Intent(VideoChatActivity.this, RegistrationActivity.class));
                            finish();
                        }

                        if (mPublisher != null){
                            mPublisher.destroy();
                        }

                        if (mSubscriber != null){
                            mSubscriber.destroy();
                        }
                        mSession.disconnect();
                        startActivity(new Intent(VideoChatActivity.this, RegistrationActivity.class));
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, VideoChatActivity.this);
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions(){
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO};

        if (EasyPermissions.hasPermissions(this, perms)){
            mPublisherViewController = findViewById(R.id.publisher_container);
            mSubscriberViewController = findViewById(R.id.subscriber_container);

            mSession = new Session.Builder(this, API_KEY, SESSION_ID).build();
            mSession.setSessionListener(VideoChatActivity.this);
            mSession.connect(TOKEN);
        }else{
            EasyPermissions.requestPermissions(this, "Video chat requires camera and audio permissions.", RC_VIDEO_APP_PERM, perms);
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    @Override
    public void onConnected(Session session) {
        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(VideoChatActivity.this);

        mPublisherViewController.addView(mPublisher.getView());
        if (mPublisher.getView() instanceof GLSurfaceView){
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {

    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        if (mSubscriber == null){
            mSubscriber = new Subscriber.Builder(this, stream).build();
            mSession.subscribe(mSubscriber);
            mSubscriberViewController.addView(mSubscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        if (mSubscriber != null){
            mSubscriber = null;
            mSubscriberViewController.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {

    }
}