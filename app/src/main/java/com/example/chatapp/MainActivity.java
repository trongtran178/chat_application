package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.chatapp.adapter.MessageListAdapter;
import com.example.chatapp.models.Message;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

public class MainActivity extends AppCompatActivity {

    private RecyclerView messageListRecyclerView;
    private MessageListAdapter messageListAdapter;

    // UI COMPONENT
    private ImageButton sendMessageButton;
    private EditText messageContentEditText;

    // FIREBASE DATABASE REFERENCE
    private FirebaseAuth firebaseAuth;

    private static String FIREBASE_CHAT_COLLECTION_KEY = "Chat";
    private static String FIREBASE_DEVICE_REGISTRATION_COLLECTION_KEY = "DeviceRegistrationToken";

    private static final String FIREBASE_INSTANCE_ID_TAG = "FIREBASE_INSTANCE_ID_TAG";
    private DocumentSnapshot start, end;
    private List<Message> messages;
    private List<EventListener> listeners;
    private boolean isTouchBoundary = false;

    private static final String CHANNEL_ID = "CHAT_NOTIFICATION_CHANNEL_ID";
    private static final String CHANNEL_DESCRIPTION = "CHAT_NOTIFICATION_DESCRIPTION";


    private static CollectionReference fireStoreChat() {
        return FirebaseFirestore.getInstance().collection(FIREBASE_CHAT_COLLECTION_KEY);
    }

    private static CollectionReference fireStoreDeviceRegistration() {
        return FirebaseFirestore.getInstance().collection(FIREBASE_DEVICE_REGISTRATION_COLLECTION_KEY);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();
        firebaseAuth = FirebaseAuth.getInstance();
        messages = new ArrayList<>();
        listeners = new ArrayList<>();

        sendMessageButton = findViewById(R.id.send_message_button);
        messageContentEditText = findViewById(R.id.message_content_edit_text);
        messageListRecyclerView = findViewById(R.id.recyclerview_message_list);

        sendMessageButton.setOnClickListener(sendMessageButtonOnClickListener);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageListRecyclerView.setLayoutManager(linearLayoutManager);

        messageListAdapter = new MessageListAdapter(this, messageListRecyclerView);
        messageListRecyclerView.setAdapter(messageListAdapter);
        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {

                        if (!task.isSuccessful()) {
                            Log.w(FIREBASE_INSTANCE_ID_TAG, "getInstanceId failed", task.getException());
                            return;
                        }
                        String user = firebaseAuth.getCurrentUser().getEmail();
                        String token = task.getResult().getToken();

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("token", token);
                        hashMap.put("created", new Date());
                        fireStoreDeviceRegistration().document(user).set(hashMap);
                        System.out.println("54, " + token);

                    }
                });

        realtimeUpdateListener();

        messageListAdapter.setOnLoadMoreMessageListener(new OnLoadMoreMessageListener() {
            @Override
            public void onLoadMore() {
                if (isTouchBoundary) return;
                messages.add(0, null);
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        messageListAdapter.setMessages(messages);
                        messageListAdapter.setLoading(true);
                        messages.remove(0);
                        loadOlderMessage();
                    }
                });
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_DESCRIPTION, NotificationManager.IMPORTANCE_HIGH);
        Uri sound = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.incoming_message);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();
        channel.setSound(sound, audioAttributes);
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out: {
                firebaseAuth.signOut();
                this.startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private View.OnClickListener sendMessageButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (messageContentEditText.getText() == null ||
                    messageContentEditText.getText().toString().isEmpty())
                return;

            String sender = firebaseAuth.getCurrentUser().getEmail();
            String content = messageContentEditText.getText().toString();
            Date createdAt = new Date();

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("sender", sender);
            hashMap.put("content", content);
            hashMap.put("createdAt", createdAt);

            fireStoreChat().add(hashMap).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });
            messageContentEditText.setText(null);
            messageContentEditText.setCursorVisible(false);
        }
    };

    private void realtimeUpdateListener() {
        fireStoreChat().orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                System.out.println(203 + ", " + task.getResult().getDocuments().get(0).getData().toString());
                List<DocumentSnapshot> messagesDocumentSnapshot = task.getResult().getDocuments();
                start = messagesDocumentSnapshot.get(0);
                end = messagesDocumentSnapshot.get(messagesDocumentSnapshot.size() - 1);
                if (task.isSuccessful()) {
                    for (DocumentSnapshot messageSnapshot : task.getResult()) {
                        System.out.println(messageSnapshot.getData().toString());
                        Message newMessage = new Message();
                        newMessage.setContent((String) messageSnapshot.getData().getOrDefault("content", ""));
                        newMessage.setSender((String) messageSnapshot.getData().getOrDefault("sender", ""));
                        Timestamp createdAtTimestamp = (Timestamp) messageSnapshot.getData().getOrDefault("createdAt", "");
                        newMessage.setCreatedAt(createdAtTimestamp.toDate());
                        isTouchBoundary = (boolean) messageSnapshot.getData().getOrDefault("isFirstMessage", false);
                        messages.add(0, newMessage);
                    }
                    messageListAdapter.setMessages(messages);
                    System.out.println(task.getResult().getDocuments().size());
                    EventListener<QuerySnapshot> listener = new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                                if (dc != null && dc.getType().equals(DocumentChange.Type.ADDED)) {
                                    Message newMessage = new Message();
                                    newMessage.setContent((String) dc.getDocument().getData().getOrDefault("content", ""));
                                    newMessage.setSender((String) dc.getDocument().getData().getOrDefault("sender", ""));
                                    Timestamp createdAtTimestamp = (Timestamp) dc.getDocument().getData().get("createdAt");
                                    newMessage.setCreatedAt(createdAtTimestamp.toDate());
                                    messages.add(newMessage);
                                }
                            }
                            messageListAdapter.setMessages(messages);
                            messageListRecyclerView.smoothScrollToPosition(messageListAdapter.getMessages().size() - 1);
                        }
                    };
                    fireStoreChat().orderBy("createdAt").startAfter(start).addSnapshotListener(listener);
                    System.out.println(236 + ", " + messageListAdapter.getMessages().size());
                }
            }
        });
    }

    private void loadOlderMessage() {
        fireStoreChat().orderBy("createdAt", Query.Direction.DESCENDING).startAt(end).limit(51).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                messageListAdapter.setLoading(false);
                if (task.isSuccessful()) {
                    // end = null;
                    List<DocumentSnapshot> messagesDocumentSnapshot = task.getResult().getDocuments();
                    end = messagesDocumentSnapshot.get(messagesDocumentSnapshot.size() - 1);
                    boolean ignore = true;
                    for (DocumentSnapshot messageSnapshot : messagesDocumentSnapshot) {
                        if (ignore) {
                            ignore = false;
                            continue;
                        }
                        Message newMessage = new Message();
                        newMessage.setContent((String) messageSnapshot.getData().getOrDefault("content", ""));
                        newMessage.setSender((String) messageSnapshot.getData().getOrDefault("sender", ""));
                        Timestamp createdAtTimestamp = (Timestamp) messageSnapshot.getData().getOrDefault("createdAt", "");
                        newMessage.setCreatedAt(createdAtTimestamp.toDate());
                        isTouchBoundary = (boolean) messageSnapshot.getData().getOrDefault("isFirstMessage", false);
                        messages.add(0, newMessage);
                        System.out.println(305 + ", " + messageSnapshot.getData().toString());
                    }
                    messageListAdapter.setMessages(messages);
                    messageListRecyclerView.scrollToPosition(50 + 15);

                }
            }
        });
    }


}
