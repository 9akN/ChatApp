package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    private String receiverUserId, senderUserId, currentState;

    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button sendMessageRequestButton, declineMessageRequestButton;

    private DatabaseReference userRef, chatRequestRef, contactsRef, notificationRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        mAuth = FirebaseAuth.getInstance();
        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        notificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");

        receiverUserId = getIntent().getExtras().get("visitUserId").toString();
        senderUserId = mAuth.getCurrentUser().getUid();

        userProfileImage = (CircleImageView) findViewById(R.id.visit_profile_image);
        userProfileName = (TextView) findViewById(R.id.visit_user_name);
        userProfileStatus = (TextView) findViewById(R.id.visit_profile_status);
        sendMessageRequestButton = (Button) findViewById(R.id.send_message_request_button);
        declineMessageRequestButton = (Button) findViewById(R.id.decline_message_request_button);
        currentState = "new";

        retrieveUserInfo();
    }

    private void retrieveUserInfo() {
        userRef.child(receiverUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if ((snapshot.exists()) && (snapshot.hasChild("image"))) {
                    String userImage = snapshot.child("image").getValue().toString();
                    String userName = snapshot.child("name").getValue().toString();
                    String userStatus = snapshot.child("status").getValue().toString();

                    Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);
                    
                    manageChatRequest();
                } else {
                    String userName = snapshot.child("name").getValue().toString();
                    String userStatus = snapshot.child("status").getValue().toString();

                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);

                    manageChatRequest();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void manageChatRequest() {
        chatRequestRef.child(senderUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild(receiverUserId)) {
                    String requestType = snapshot.child(receiverUserId).child("requestType").getValue().toString();

                    if (requestType.equals("sent")) {
                        currentState = "requestSent";
                        sendMessageRequestButton.setText("Cancel chat request");
                    } else if (requestType.equals("received")) {
                        currentState = "requestReceived";
                        sendMessageRequestButton.setText("Accept request");

                        declineMessageRequestButton.setVisibility(View.VISIBLE);
                        declineMessageRequestButton.setEnabled(true);

                        declineMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                cancelChatRequest();
                            }
                        });
                    }
                } else {
                    contactsRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.hasChild(receiverUserId)) {
                                currentState = "friends";
                                sendMessageRequestButton.setText("Remove contact");
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

        if (!senderUserId.equals(receiverUserId)) {
            sendMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMessageRequestButton.setEnabled(false);

                    if (currentState.equals("new")) {
                        sendChatRequest();
                    }
                    if (currentState.equals("requestSent")) {
                        cancelChatRequest();
                    }
                    if (currentState.equals("requestReceived")) {
                        acceptChatRequest();
                    }
                    if (currentState.equals("friends")) {
                        removeSpecificContact();
                    }
                }
            });
        } else {
            sendMessageRequestButton.setVisibility(View.INVISIBLE);
        }
    }

    private void sendChatRequest() {
        chatRequestRef.child(senderUserId).child(receiverUserId).child("requestType").setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    chatRequestRef.child(receiverUserId).child(senderUserId).child("requestType").setValue("received").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                HashMap<String, String> chatNotificationMap = new HashMap<>();
                                chatNotificationMap.put("from", senderUserId);
                                chatNotificationMap.put("type", "request");

                                notificationRef.child(receiverUserId).push().setValue(chatNotificationMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            sendMessageRequestButton.setEnabled(true);
                                            currentState = "requestSent";
                                            sendMessageRequestButton.setText("Cancel chat request");
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

    private void cancelChatRequest() {
        chatRequestRef.child(senderUserId).child(receiverUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    chatRequestRef.child(receiverUserId).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                sendMessageRequestButton.setEnabled(true);
                                currentState = "new";
                                sendMessageRequestButton.setText("Send message");

                                declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                declineMessageRequestButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
    }

    private void acceptChatRequest() {
        contactsRef.child(senderUserId).child(receiverUserId).child("Contacts").setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    contactsRef.child(receiverUserId).child(senderUserId).child("Contacts").setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                chatRequestRef.child(senderUserId).child(receiverUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        chatRequestRef.child(receiverUserId).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    sendMessageRequestButton.setEnabled(true);
                                                    currentState = "friends";
                                                    sendMessageRequestButton.setText("Remove contact");

                                                    declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                    declineMessageRequestButton.setEnabled(false);
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void removeSpecificContact() {
        contactsRef.child(senderUserId).child(receiverUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    contactsRef.child(receiverUserId).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                sendMessageRequestButton.setEnabled(true);
                                currentState = "new";
                                sendMessageRequestButton.setText("Send message");

                                declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                declineMessageRequestButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
    }
}