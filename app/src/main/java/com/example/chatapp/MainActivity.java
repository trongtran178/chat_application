package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.chatapp.adapter.MessageListAdapter;
import com.example.chatapp.models.Message;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView messageListRecyclerView;
    private MessageListAdapter messageListAdapter;

    // UI COMPONENT
    private ImageButton sendMessageButton;
    private EditText messageContentEditText;

    // FIREBASE DATABASE REFERENCE
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    private String startAtId;
    private static final String FIREBASE_INSTANCE_ID_TAG = "FIREBASE_INSTANCE_ID_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();

        sendMessageButton = findViewById(R.id.send_message_button);
        messageContentEditText = findViewById(R.id.message_content_edit_text);
        messageListRecyclerView = findViewById(R.id.recyclerview_message_list);

        sendMessageButton.setOnClickListener(sendMessageButtonOnClickListener);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageListRecyclerView.setLayoutManager(linearLayoutManager);

        messageListAdapter = new MessageListAdapter(this, messageListRecyclerView);

        messageListRecyclerView.setAdapter(messageListAdapter);
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(FIREBASE_INSTANCE_ID_TAG, "getInstanceId failed", task.getException());
                            return;
                        }
                        String token = task.getResult().getToken();
                        System.out.println("54, " + token);

                    }
                });
        readMessage();
    }

    // Read first 30  message
    private void readMessage() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Chats");
        databaseReference.limitToLast(500).addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Message message = dataSnapshot.getValue(Message.class);
                List<Message> messages = messageListAdapter.getMessages();
                messages.add(message);
                messageListAdapter.setMessages(messages);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private View.OnClickListener sendMessageButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (messageContentEditText.getText() == null ||
                    messageContentEditText.getText().toString().isEmpty())
                return;

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

            String sender = firebaseAuth.getCurrentUser().getEmail();
            String content = messageContentEditText.getText().toString();
            Date createdAt = new Date();

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("sender", sender);
            hashMap.put("content", content);
            hashMap.put("createdAt", createdAt);

            reference.child("Chats").push().setValue(hashMap);

            messageContentEditText.setText(null);
            messageContentEditText.setCursorVisible(false);
        }
    };


}
