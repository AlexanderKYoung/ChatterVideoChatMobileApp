package com.example.videochatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView notificationsList;
    private DatabaseReference friendRequestRef, contactsRef, usersRef;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private FirebaseRecyclerAdapter<Contacts, NotificationsViewHolder> firebaseRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        notificationsList = findViewById(R.id.notifications_list);
        notificationsList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        friendRequestRef = FirebaseDatabase.getInstance().getReference().child("Friend Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
    }

    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(friendRequestRef.child(currentUserId), Contacts.class).build();

        firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Contacts, NotificationsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull NotificationsViewHolder notificationsViewHolder, int i, @NonNull Contacts contacts) {
                notificationsViewHolder.acceptBtn.setVisibility(View.VISIBLE);
                notificationsViewHolder.cancelBtn.setVisibility(View.VISIBLE);

                final String listUserId = getRef(i).getKey();

                DatabaseReference requestTypeRef = getRef(i).child("request_type").getRef();
                requestTypeRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            String type = snapshot.getValue().toString();

                            if (type.equals("received")){
                                notificationsViewHolder.cardView.setVisibility(View.VISIBLE);
                                usersRef.child(listUserId).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                        if (snapshot.hasChild("image")){
                                            final String imageStr = snapshot.child("image").getValue().toString();
                                            Picasso.get().load(imageStr).into(notificationsViewHolder.profileImageView);
                                        }

                                        final String nameStr = snapshot.child("name").getValue().toString();

                                        notificationsViewHolder.userNameTV.setText(nameStr);

                                        notificationsViewHolder.acceptBtn.setOnClickListener(view -> contactsRef.child(currentUserId).child(listUserId)
                                                .child("Contact").setValue("Saved").addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()){
                                                        contactsRef.child(listUserId).child(currentUserId)
                                                                .child("Contact").setValue("Saved").addOnCompleteListener(task14 -> {
                                                                    if (task14.isSuccessful()){
                                                                        friendRequestRef.child(currentUserId).child(listUserId)
                                                                                .removeValue().addOnCompleteListener(task13 -> {
                                                                                    if (task13.isSuccessful()){
                                                                                        friendRequestRef.child(listUserId).child(currentUserId)
                                                                                                .removeValue().addOnCompleteListener(task12 -> {
                                                                                                    if (task12.isSuccessful()){
                                                                                                        Toast.makeText(NotificationsActivity.this, "Contact saved.", Toast.LENGTH_SHORT).show();
                                                                                                    }
                                                                                                });
                                                                                    }
                                                                                });
                                                                    }
                                                                });
                                                    }
                                                }));

                                        notificationsViewHolder.cancelBtn.setOnClickListener(view ->
                                                friendRequestRef.child(currentUserId).child(listUserId)
                                                .removeValue().addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()){
                                                        friendRequestRef.child(listUserId).child(currentUserId)
                                                                .removeValue().addOnCompleteListener(task1 -> {
                                                                    if (task1.isSuccessful()){
                                                                        Toast.makeText(NotificationsActivity.this, "Request cancelled.", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                    }
                                                }));
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @NonNull
            @Override
            public NotificationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.find_people_design, parent, false);
                NotificationsViewHolder viewHolder = new NotificationsViewHolder(view);
                return viewHolder;
            }
        };

        notificationsList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    public static class NotificationsViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTV;
        Button acceptBtn, cancelBtn;
        ImageView profileImageView;
        RelativeLayout cardView;

        public NotificationsViewHolder(@NonNull View itemView) {
            super(itemView);

            userNameTV = itemView.findViewById(R.id.name_notification);
            acceptBtn = itemView.findViewById(R.id.request_accept_btn);
            cancelBtn = itemView.findViewById(R.id.request_decline_btn);
            profileImageView = itemView.findViewById(R.id.image_notification);
            cardView = itemView.findViewById(R.id.card_view1);
        }
    }
}