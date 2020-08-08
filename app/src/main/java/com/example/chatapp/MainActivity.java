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

import java.util.ArrayList;
import java.util.Collections;
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

    private static String FIREBASE_CHAT_COLLECTION_KEY = "Chat";
    private static String FIREBASE_DEVICE_REGISTRATION_COLLECTION_KEY = "DeviceRegistrationToken";

    private boolean isFirstLoading = false;
    private static final String FIREBASE_INSTANCE_ID_TAG = "FIREBASE_INSTANCE_ID_TAG";
    private DocumentSnapshot firstVisible;

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


        // LOAD MORE LOGIC
//        messageListAdapter.setOnLoadMoreMessageListener(new OnLoadMoreMessageListener() {
//            @Override
//            public void onLoadMore() {
//                final List<Message> messages = messageListAdapter.getMessages();
//                messages.add(0, null);
//
//                fireStoreChat().orderBy("createdAt")
//                        .endBefore(firstVisible)
//                        // .endBefore(firstVisible)
//                        .limit(30)
//                        .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//                    @Override
//                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                        firstVisible = null;
//                        messages.remove(0); // remove loading
//
//                        List<Message> messages = messageListAdapter.getMessages();
//                        List<Message> olderMessages = new ArrayList<>();
//                        System.out.println(114 + ", " + queryDocumentSnapshots.getDocumentChanges().size());
//                        for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
//                            if (firstVisible == null)
//                                firstVisible = dc.getDocument();
//                            Message message = new Message();
//                            message.setContent((String) dc.getDocument().getData().getOrDefault("content", ""));
//                            message.setSender((String) dc.getDocument().getData().getOrDefault("sender", ""));
//                            Timestamp createdAtTimestamp = (Timestamp) dc.getDocument().getData().get("createdAt");
//                            message.setCreatedAt(createdAtTimestamp.toDate());
//                            olderMessages.add(0, message);
//                            System.out.println(128 + ", " + dc.getDocument().get("content"));
//
//                        }
//                        Collections.reverse(olderMessages);
//                        messages.addAll(0, olderMessages);
//                        messageListAdapter.setMessages(messages);
//                    }
//                });
//            }
//        });

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
        fireStoreChat().orderBy("createdAt", Query.Direction.DESCENDING).limit(150).addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(FIREBASE_INSTANCE_ID_TAG, "Listen failed.", e);
                    return;
                }
                DocumentSnapshot lastVisible = queryDocumentSnapshots.getDocuments().get(0);
                System.out.println(148 + ", " + lastVisible.getData().get("content"));

                for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                    if (dc != null && dc.getType().equals(DocumentChange.Type.ADDED)) {

                        Log.d(FIREBASE_INSTANCE_ID_TAG, "Current data: " + dc.getDocument().getData());
                        List<Message> messages = messageListAdapter.getMessages();
                        Message newMessage = new Message();
                        newMessage.setContent((String) dc.getDocument().getData().getOrDefault("content", ""));
                        newMessage.setSender((String) dc.getDocument().getData().getOrDefault("sender", ""));
                        Timestamp createdAtTimestamp = (Timestamp) dc.getDocument().getData().get("createdAt");
                        newMessage.setCreatedAt(createdAtTimestamp.toDate());
                        if (!isFirstLoading) {
                            firstVisible = dc.getDocument();
                            messages.add(0, newMessage);
                        } else messages.add(newMessage);
                        messageListAdapter.setMessages(messages);

                    } else {
                        Log.d(FIREBASE_INSTANCE_ID_TAG, "Current data: null");
                    }
                }
                messageListRecyclerView.smoothScrollToPosition(messageListAdapter.getMessages().size() - 1);
                System.out.println(176 + ", " + firstVisible.get("content"));
                if (!isFirstLoading) isFirstLoading = true;
            }
        });
    }


}
