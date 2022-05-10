package com.example.videochatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Message;
import android.printservice.CustomPrinterIconCallback;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class CallingActivity extends AppCompatActivity {

    private TextView nameContact;
    private ImageView profileImage;
    private ImageView cancelCallBtn, makeCallBtn;

    private String receiverUserId, receiverUserImage, receiverUserName;
    private String senderUserId, senderUserImage, senderUserName;
    private String checker = "";
    private String callingId = "", ringingId = "";
    private DatabaseReference usersRef;

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        senderUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        receiverUserId = getIntent().getExtras().get("visit_user_id").toString();

        nameContact = findViewById(R.id.calling_name);
        profileImage = findViewById(R.id.calling_profile_image);
        cancelCallBtn = findViewById(R.id.cancel_call);
        makeCallBtn = findViewById(R.id.make_call);

        mediaPlayer = MediaPlayer.create(this, R.raw.ringing);

        cancelCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.stop();
                checker = "clicked";
                cancelCallingUser();
            }
        });

        makeCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.stop();

                final HashMap<String, Object> callingPickUpMap = new HashMap<>();
                callingPickUpMap.put("picked", "picked");

                usersRef.child(senderUserId).child("Ringing").updateChildren(callingPickUpMap)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()){
                                    Intent intent = new Intent(CallingActivity.this, VideoChatActivity.class);
                                    startActivity(intent);
                                    finish();
                                    return;
                                }
                            }
                        });
            }
        });

        getAndSetUserProfileInfo();
    }

    private void getAndSetUserProfileInfo() {

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(receiverUserId).exists()){
                    receiverUserImage = snapshot.child(receiverUserId).child("image").getValue().toString();
                    receiverUserName = snapshot.child(receiverUserId).child("name").getValue().toString();

                    nameContact.setText(receiverUserName);
                    Picasso.get().load(receiverUserImage).placeholder(R.drawable.profile_image).into(profileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mediaPlayer.start();

        usersRef.child(receiverUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!checker.equals("clicked") && !snapshot.hasChild("Calling") && !snapshot.hasChild("Ringing")){
                    mediaPlayer.start();

                    final HashMap<String, Object> callingInfo = new HashMap<>();
                    callingInfo.put("calling", receiverUserId);

                    usersRef.child(senderUserId)
                            .child("Calling")
                            .updateChildren(callingInfo)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()){
                                    final HashMap<String, Object> ringingInfo = new HashMap<>();
                                    ringingInfo.put("ringing", senderUserId);

                                    usersRef.child(receiverUserId)
                                            .child("Ringing")
                                            .updateChildren(ringingInfo);
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(senderUserId).hasChild("Ringing") && !snapshot.child(senderUserId).hasChild("Calling")){
                    makeCallBtn.setVisibility(View.VISIBLE);
                }

                if (snapshot.child(receiverUserId).child("Ringing").hasChild("picked")){
                    Intent intent = new Intent(CallingActivity.this, VideoChatActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void cancelCallingUser() {
        usersRef.child(senderUserId)
                .child("Calling")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("calling")){
                    callingId = snapshot.child("calling").getValue().toString();

                    usersRef.child(callingId)
                            .child("Ringing")
                            .removeValue()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                usersRef.child(senderUserId)
                                        .child("Calling")
                                        .removeValue()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Intent intent = new Intent(CallingActivity.this, ContactsActivity.class);
                                                startActivity(intent);
                                                finish();
                                                return;
                                            }
                                        });
                            }else{
                                Intent intent = new Intent(CallingActivity.this, ContactsActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        usersRef.child(receiverUserId)
                .child("Ringing")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChild("ringing")){
                            ringingId = snapshot.child("ringing").getValue().toString();

                            usersRef.child(ringingId)
                                    .child("Calling")
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                usersRef.child(receiverUserId)
                                                        .child("Ringing")
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                Intent intent = new Intent(CallingActivity.this, ContactsActivity.class);
                                                                startActivity(intent);
                                                                finish();
                                                                return;
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }else{
                            Intent intent = new Intent(CallingActivity.this, ContactsActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        senderUserId = "";
        receiverUserId = "";
        checker = "";
    }
}