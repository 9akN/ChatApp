package com.example.chatapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Messages> userMessagesList;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    public MessageAdapter (List<Messages> userMessagesList) {
        this.userMessagesList = userMessagesList;
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView senderMessageText, receiverMessageText;
        public CircleImageView receiverProfileImage;
        public ImageView messageSenderPicture, messageReceiverPicture;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            senderMessageText = (TextView) itemView.findViewById(R.id.sender_message_text);
            receiverMessageText = (TextView) itemView.findViewById(R.id.receiver_message_text);
            receiverProfileImage = (CircleImageView) itemView.findViewById(R.id.message_profile_image);
            messageReceiverPicture = itemView.findViewById(R.id.message_receiver_image_view);
            messageSenderPicture = itemView.findViewById(R.id.message_sender_image_view);
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_messages_layout, parent, false);

        mAuth = FirebaseAuth.getInstance();

        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        String messageSenderId = mAuth.getCurrentUser().getUid();
        Messages messages = userMessagesList.get(position);

        String fromUserId = messages.getFrom();
        String fromMessagesType = messages.getType();

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserId);
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild("image")) {
                    String receiverImage = snapshot.child("image").getValue().toString();
                    Picasso.get().load(receiverImage).placeholder(R.drawable.profile_image).into(holder.receiverProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        holder.receiverMessageText.setVisibility(View.GONE);
        holder.receiverProfileImage.setVisibility(View.GONE);
        holder.senderMessageText.setVisibility(View.GONE);
        holder.messageSenderPicture.setVisibility(View.GONE);
        holder.messageReceiverPicture.setVisibility(View.GONE);

        if (fromMessagesType.equals("text")) {
            if (fromUserId.equals(messageSenderId)) {
                holder.senderMessageText.setVisibility(View.VISIBLE);

                holder.senderMessageText.setBackgroundResource(R.drawable.senders_messages_layout);
                holder.senderMessageText.setText(messages.getMessage() + "\n \n" + messages.getTime() + " - " + messages.getDate());
            } else {
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.receiverMessageText.setVisibility(View.VISIBLE);

                holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
                holder.receiverMessageText.setText(messages.getMessage() + "\n \n" + messages.getTime() + " - " + messages.getDate());
            }
        } else if (fromMessagesType.equals("image")) {
            if (fromUserId.equals(messageSenderId)) {
                holder.messageSenderPicture.setVisibility(View.VISIBLE);

                Picasso.get().load(messages.getMessage()).into(holder.messageSenderPicture);
            } else {
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.messageReceiverPicture.setVisibility(View.VISIBLE);

                Picasso.get().load(messages.getMessage()).into(holder.messageReceiverPicture);
            }
        } else if (fromMessagesType.equals("pdf") || fromMessagesType.equals("docx")) {
            if (fromUserId.equals(messageSenderId)) {
                holder.messageSenderPicture.setVisibility(View.VISIBLE);

                Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/chatapp-59864.appspot.com/o/Image%20files%2Ffile.png?alt=media&token=2c193b5b-c38d-4501-b9d7-ca9eaa219bb8").into(holder.messageSenderPicture);
                //holder.messageSenderPicture.setBackgroundResource(R.drawable.file);
            } else {
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.messageReceiverPicture.setVisibility(View.VISIBLE);

                Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/chatapp-59864.appspot.com/o/Image%20files%2Ffile.png?alt=media&token=2c193b5b-c38d-4501-b9d7-ca9eaa219bb8").into(holder.messageReceiverPicture);
                //holder.messageReceiverPicture.setBackgroundResource(R.drawable.file);
            }
        }

        if (fromUserId.equals(messageSenderId)) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (userMessagesList.get(position).getType().equals("pdf") || userMessagesList.get(position).getType().equals("docx")) {
                        CharSequence options[] = new CharSequence[] {
                                "Delete for me",
                                "Download and view this document",
                                "Cancel",
                                "Delete for everyone"
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteSentMessage(which, holder);

                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 1) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(userMessagesList.get(position).getMessage()));
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 3) {
                                    deleteMessageForEveryone(which, holder);

                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    } else if (userMessagesList.get(position).getType().equals("text")) {
                        CharSequence options[] = new CharSequence[] {
                                "Delete for me",
                                "Cancel",
                                "Delete for everyone"
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteSentMessage(which, holder);

                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 2) {
                                    deleteMessageForEveryone(which, holder);

                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    } else if (userMessagesList.get(position).getType().equals("image")) {
                        CharSequence options[] = new CharSequence[] {
                                "Delete for me",
                                "View this image",
                                "Cancel",
                                "Delete for everyone"
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteSentMessage(which, holder);

                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 1) {
                                    Intent intent = new Intent(holder.itemView.getContext(), ImageViewerActivity.class);
                                    intent.putExtra("url", userMessagesList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 3) {
                                    deleteMessageForEveryone(which, holder);

                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                }
            });
        } else {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (userMessagesList.get(position).getType().equals("pdf") || userMessagesList.get(position).getType().equals("docx")) {
                        CharSequence options[] = new CharSequence[] {
                                "Delete for me",
                                "Download and view this document",
                                "Cancel"
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteReceivedMessage(which, holder);

                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (position == 1) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(userMessagesList.get(position).getMessage()));
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    } else if (userMessagesList.get(position).getType().equals("text")) {
                        CharSequence options[] = new CharSequence[] {
                                "Delete for me",
                                "Cancel"
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteReceivedMessage(which, holder);

                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    } else if (userMessagesList.get(position).getType().equals("image")) {
                        CharSequence options[] = new CharSequence[] {
                                "Delete for me",
                                "View this image",
                                "Cancel"
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteReceivedMessage(which, holder);

                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 1) {
                                    Intent intent = new Intent(holder.itemView.getContext(), ImageViewerActivity.class);
                                    intent.putExtra("url", userMessagesList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }

    private void deleteSentMessage(final int position, final MessageViewHolder holder) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages").child(userMessagesList.get(position).getFrom()).child(userMessagesList.get(position).getTo()).child(userMessagesList.get(position).getMessageId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(holder.itemView.getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(holder.itemView.getContext(), "Error occurred", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteReceivedMessage(final int position, final MessageViewHolder holder) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages").child(userMessagesList.get(position).getTo()).child(userMessagesList.get(position).getFrom()).child(userMessagesList.get(position).getMessageId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(holder.itemView.getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(holder.itemView.getContext(), "Error occurred", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteMessageForEveryone(final int position, final MessageViewHolder holder) {
        final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages").child(userMessagesList.get(position).getTo()).child(userMessagesList.get(position).getFrom()).child(userMessagesList.get(position).getMessageId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    rootRef.child("Messages").child(userMessagesList.get(position).getFrom()).child(userMessagesList.get(position).getTo()).child(userMessagesList.get(position).getMessageId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(holder.itemView.getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(holder.itemView.getContext(), "Error occurred", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
