package com.example.videochatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class ProfileActivity extends AppCompatActivity {

    private String receiverUserId = "";
    private String receiverUserImage ="";
    private String receiverUserName="";

    private ImageView backgroung_profile_view;
    private TextView name_profile;
    private Button add_contact, decline_request;

    private FirebaseAuth mAuth;
    private String senderUserId;
    private String currentState = "new";
    private DatabaseReference friendRequestRef, contactsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        senderUserId = mAuth.getCurrentUser().getUid();
        friendRequestRef = FirebaseDatabase.getInstance().getReference().child("Friend Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        receiverUserId = getIntent().getExtras().get("visit_user_id").toString();
        receiverUserImage = getIntent().getExtras().get("profile_image").toString();
        receiverUserName = getIntent().getExtras().get("profile_name").toString();

        backgroung_profile_view = findViewById(R.id.background_profile_view);
        name_profile = findViewById(R.id.name_profile);
        add_contact = findViewById(R.id.add_contact);
        decline_request = findViewById(R.id.decline_request);

        Picasso.get().load(receiverUserImage).into(backgroung_profile_view);
        name_profile.setText(receiverUserName);

        manageClickEvents();
    }

    private void manageClickEvents() {
        friendRequestRef.child(senderUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild(receiverUserId)){
                    String request_type = snapshot.child(receiverUserId).child("request_type").getValue().toString();

                    if (request_type.equals("sent")){
                        currentState = "request_sent";
                        add_contact.setText("Cancel Request");
                    }else if (request_type.equals("received")){
                        currentState = "request_received";
                        add_contact.setText("Accept Request");

                        decline_request.setVisibility(View.VISIBLE);
                        decline_request.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                cancelFriendRequest();
                            }
                        });
                    }
                } else{
                    contactsRef.child(senderUserId).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.hasChild(receiverUserId)){
                                currentState = "friends";
                                add_contact.setText("Delete Contact");
                            }else{
                                currentState = "new";
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        if (senderUserId.equals(receiverUserId)){
            add_contact.setVisibility(View.GONE);
        }else{
            add_contact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(currentState.equals("new")){
                        sendFriendRequest();
                    }
                    if (currentState.equals("request_sent")){
                        cancelFriendRequest();
                    }
                    if (currentState.equals("request_received")){
                        acceptFriendRequest();
                    }
                    if (currentState.equals("request_accepted")){
                        cancelFriendRequest();
                    }
                }
            });
        }
    }

    private void acceptFriendRequest() {
        contactsRef.child(senderUserId).child(receiverUserId)
                .child("Contact").setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    contactsRef.child(receiverUserId).child(senderUserId)
                            .child("Contact").setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                friendRequestRef.child(senderUserId).child(receiverUserId)
                                        .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            friendRequestRef.child(receiverUserId).child(senderUserId)
                                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()){
                                                        currentState = "friends";
                                                        add_contact.setText("Delete Contact");
                                                        Toast.makeText(ProfileActivity.this, "Request accepted.", Toast.LENGTH_SHORT).show();
                                                        decline_request.setVisibility(View.GONE);
                                                    }
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void cancelFriendRequest() {
        friendRequestRef.child(senderUserId).child(receiverUserId)
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    friendRequestRef.child(receiverUserId).child(senderUserId)
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(ProfileActivity.this, "Request cancelled.", Toast.LENGTH_SHORT).show();
                                currentState = "new";
                                add_contact.setText("Add Contact");
                            }
                        }
                    });
                }
            }
        });
    }

    private void sendFriendRequest() {
        friendRequestRef.child(senderUserId).child(receiverUserId)
            .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            friendRequestRef.child(receiverUserId).child(senderUserId)
                                    .child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                currentState = "request_sent";
                                                add_contact.setText("Cancel Request");
                                                Toast.makeText(ProfileActivity.this, "Request sent.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }

                    }
                });

    }
}