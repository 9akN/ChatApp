package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class FindFriendsActivity extends AppCompatActivity {
    private Toolbar mToolBar;
    private RecyclerView findFriendsRecyclerList;
    private DatabaseReference usersRef;

    private EditText mSearchField;
    private ImageButton mSearchBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        mSearchField = (EditText) findViewById(R.id.search_field);
        mSearchBtn = (ImageButton) findViewById(R.id.search_btn);

        findFriendsRecyclerList = (RecyclerView) findViewById(R.id.find_friend_recycler_list);
        findFriendsRecyclerList.setHasFixedSize(true);
        findFriendsRecyclerList.setLayoutManager(new LinearLayoutManager(this));
        mToolBar = (Toolbar) findViewById(R.id.find_friends_toolbar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Find friends");

        mSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String searchText = mSearchField.getText().toString();

                firebaseUserSearch(searchText);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>().setQuery(usersRef, Contacts.class).build();

        FirebaseRecyclerAdapter<Contacts, FindFriendViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, FindFriendViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FindFriendViewHolder holder, int position, @NonNull Contacts model) {
                holder.userName.setText(model.getName());
                holder.userStatus.setText(model.getStatus());
                Picasso.get().load(model.getImage()).placeholder(R.drawable.profile_image).into(holder.profileImage);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String visitUserId = getRef(position).getKey();

                        Intent profileIntent = new Intent(FindFriendsActivity.this, ProfileActivity.class);
                        profileIntent.putExtra("visitUserId", visitUserId);
                        startActivity(profileIntent);
                    }
                });
            }

            @NonNull
            @Override
            public FindFriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout, parent, false);
                FindFriendViewHolder viewHolder = new FindFriendViewHolder(view);

                return viewHolder;
            }
        };

        findFriendsRecyclerList.setAdapter(adapter);

        adapter.startListening();
    }

    public static class FindFriendViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userStatus;
        CircleImageView profileImage;

        View mView;

        public FindFriendViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;

            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
        }

        public void setDetails(Context ctx, String userName, String userStatus, String userImage){

            TextView user_name = (TextView) mView.findViewById(R.id.user_profile_name);
            TextView user_status = (TextView) mView.findViewById(R.id.user_status);
            ImageView user_image = (ImageView) mView.findViewById(R.id.users_profile_image);


            user_name.setText(userName);
            user_status.setText(userStatus);

            Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(user_image);
        }
    }

    private void firebaseUserSearch(String searchText) {

        Query firebaseSearchQuery = usersRef.orderByChild("name").startAt(searchText).endAt(searchText + "\uf8ff");

        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>().setQuery(firebaseSearchQuery, Contacts.class).build();

        FirebaseRecyclerAdapter<Contacts, FindFriendViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Contacts, FindFriendViewHolder>(
                options
        ) {
            @Override
            protected void onBindViewHolder(@NonNull FindFriendViewHolder holder, int position, @NonNull Contacts model) {
                holder.setDetails(getApplicationContext(), model.getName(), model.getStatus(), model.getImage());
            }

            @NonNull
            @Override
            public FindFriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout, parent, false);
                FindFriendViewHolder viewHolder = new FindFriendViewHolder(view);

                return viewHolder;
            }
        };

        findFriendsRecyclerList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();

    }
}