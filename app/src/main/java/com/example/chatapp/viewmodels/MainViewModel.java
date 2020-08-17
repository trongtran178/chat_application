package com.example.chatapp.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chatapp.models.Message;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

public class MainViewModel extends AndroidViewModel {

    private MutableLiveData<List<Message>> messages;
    private DocumentSnapshot start, end;
    private FirebaseAuth firebaseAuth;

    private static final String FIRESTORE_CHAT_COLLECTION_NAME = "Chat";
    private static final String FIRESTORE_DEVICE_REGISTRATION_COLLECTION_NAME = "DeviceRegistrationToken";
    private static final String FIREBASE_INSTANCE_ID_TAG = "FIREBASE_INSTANCE_ID_TAG";

    private boolean isFullMessages = false;
    private boolean isScrollBehaviour = false;
    private boolean isListenNewMessage = false;

    public MainViewModel(@NonNull Application application) {
        super(application);
        messages = new MutableLiveData<>();
        messages.setValue(new ArrayList<Message>());
        firebaseAuth = FirebaseAuth.getInstance();

    }

    private static CollectionReference fireStoreChat() {
        return FirebaseFirestore.getInstance().collection(FIRESTORE_CHAT_COLLECTION_NAME);
    }

    private static CollectionReference fireStoreDeviceRegistration() {
        return FirebaseFirestore.getInstance().collection(FIRESTORE_DEVICE_REGISTRATION_COLLECTION_NAME);
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public LiveData<List<Message>> loadMessagesAndListenNewMessage() {
        isListenNewMessage = true;
        // Query 50 message first
        fireStoreChat().orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                List<DocumentSnapshot> messagesDocumentSnapshot = task.getResult().getDocuments();
                start = messagesDocumentSnapshot.get(0);
                end = messagesDocumentSnapshot.get(messagesDocumentSnapshot.size() - 1);
                if (task.isSuccessful()) {
                    List<Message> messagesData = new ArrayList<>();
                    for (DocumentSnapshot messageSnapshot : task.getResult()) {

                        Message newMessage = getMessageFromDocumentSnapshot(messageSnapshot);

                        isFullMessages = (boolean) messageSnapshot.getData().getOrDefault("isFirstMessage", false);

                        messagesData = messages.getValue();
                        if (!messagesData.contains(newMessage))
                            messagesData.add(0, newMessage);
                    }
                    messages.setValue(messagesData);


                    // Then, add listener to listen new message
                    EventListener<QuerySnapshot> listener = new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            List<Message> ms = messages.getValue();
                            for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                                if (dc != null && dc.getType().equals(DocumentChange.Type.ADDED)) {
                                    Message newMessage = getMessageFromDocumentSnapshot(dc.getDocument());
                                    if (!ms.contains(newMessage))
                                        ms.add(newMessage);
                                }
                            }
                            messages.setValue(ms);
                        }
                    };
                    fireStoreChat().orderBy("createdAt").startAfter(start).addSnapshotListener(listener);
                }
            }
        });
        return messages;
    }

    // add 0 to first index, viewHolder will show circle loading process
    public void loading() {
        final List<Message> ms = messages.getValue();
        ms.add(0, null);
        messages.setValue(ms);

    }

    public Task<QuerySnapshot> loadOldMessages() {
        final List<Message> ms = messages.getValue();
        return fireStoreChat().orderBy("createdAt", Query.Direction.DESCENDING).startAt(end).limit(51).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                isScrollBehaviour = true;
                if (task.isSuccessful()) {
                    ms.remove(0);
                    messages.setValue(ms);
                    List<DocumentSnapshot> messagesDocumentSnapshot = task.getResult().getDocuments();
                    end = messagesDocumentSnapshot.get(messagesDocumentSnapshot.size() - 1);
                    boolean ignore = true;
                    for (DocumentSnapshot messageSnapshot : messagesDocumentSnapshot) {
                        // ignore first message, because first message has existed !
                        if (ignore) {
                            ignore = false;
                            continue;
                        }
                        Message newMessage = getMessageFromDocumentSnapshot(messageSnapshot);

                        isFullMessages = (boolean) messageSnapshot.getData().getOrDefault("isFirstMessage", false);
                        List<Message> ms = messages.getValue();
                        ms.add(0, newMessage);
                        messages.setValue(ms);
                    }
                }
            }
        });
    }

    public void sendMessage(String sender, String content) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("content", content);
        hashMap.put("createdAt", new Date());

        fireStoreChat().add(hashMap).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    public void signOut() {
        firebaseAuth.signOut();
    }

    //    Register devices to receive messages from FCM, then save device token
    public void registerDevice() {
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
                    }
                });

    }

    public boolean isFullMessages() {
        return isFullMessages;
    }

    private Message getMessageFromDocumentSnapshot(DocumentSnapshot messageDocumentSnapshot) {
        Message newMessage = new Message();
        newMessage.setContent((String) messageDocumentSnapshot.getData().getOrDefault("content", ""));
        newMessage.setSender((String) messageDocumentSnapshot.getData().getOrDefault("sender", ""));
        Timestamp createdAtTimestamp = (Timestamp) messageDocumentSnapshot.getData().getOrDefault("createdAt", "");
        newMessage.setCreatedAt(createdAtTimestamp.toDate());
        return newMessage;
    }


    public boolean isScrollBehaviour() {
        return isScrollBehaviour;
    }

    public void setScrollBehaviour(boolean scrollBehaviour) {
        isScrollBehaviour = scrollBehaviour;
    }

    public boolean isListenNewMessage() {
        return isListenNewMessage;
    }
}
