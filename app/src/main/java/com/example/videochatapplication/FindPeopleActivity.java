package com.example.videochatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

public class FindPeopleActivity extends AppCompatActivity {

    private RecyclerView findPeopleList;
    private EditText searchET;
    private String str="";
    private DatabaseReference usersRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_people);

        searchET = findViewById(R.id.search_user_text);
        findPeopleList = findViewById(R.id.find_people_list);
        findPeopleList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        searchET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (searchET.getText().toString().equals("")){
                    Toast.makeText(FindPeopleActivity.this, "Invalid name.", Toast.LENGTH_SHORT).show();
                }else{
                    str = charSequence.toString();
                    onStart();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options = null;
        if (str.equals("")){
            options = new FirebaseRecyclerOptions.Builder<Contacts>()
                    .setQuery(usersRef, Contacts.class).build();
        }else{
            options = new FirebaseRecyclerOptions.Builder<Contacts>()
                    .setQuery(usersRef.orderByChild("name").startAt(str).endAt(str + "\uf8ff")
                            , Contacts.class).build();
        }

        FirebaseRecyclerAdapter<Contacts, FindPeopleViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Contacts, FindPeopleViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FindPeopleViewHolder findPeopleViewHolder, @SuppressLint("RecyclerView") final int i, @NonNull Contacts contacts) {
                findPeopleViewHolder.userNameTV.setText(contacts.getName());
                Picasso.get().load(contacts.getImage()).into(findPeopleViewHolder.profileImageView);

                findPeopleViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String visit_user_id = getRef(i).getKey();

                        Intent intent = new Intent(FindPeopleActivity.this, ProfileActivity.class);
                        intent.putExtra("visit_user_id", visit_user_id);
                        intent.putExtra("profile_image", contacts.getImage());
                        intent.putExtra("profile_name", contacts.getName());
                        startActivity(intent);
                        finish();
                    }
                });
            }

            @NonNull
            @Override
            public FindPeopleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_design, parent, false);
                FindPeopleViewHolder viewHolder = new FindPeopleViewHolder(view);
                return viewHolder;
            }
        };
        findPeopleList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    public static class FindPeopleViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTV;
        Button videoCallBtn;
        ImageView profileImageView;
        RelativeLayout cardView;

        public FindPeopleViewHolder(@NonNull View itemView) {
            super(itemView);

            userNameTV = itemView.findViewById(R.id.name_contact);
            videoCallBtn = itemView.findViewById(R.id.call_btn);
            profileImageView = itemView.findViewById(R.id.image_contact);
            cardView = itemView.findViewById(R.id.card_view1);

            videoCallBtn.setVisibility(View.GONE);
        }
    }

}