package com.example.chatapp.views;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.chatapp.OnLoadMoreMessageListener;
import com.example.chatapp.R;
import com.example.chatapp.adapter.MessageListAdapter;
import com.example.chatapp.models.Message;
import com.example.chatapp.viewmodels.MainViewModel;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView messageListRecyclerView;
    private MessageListAdapter messageListAdapter;
    private LinearLayout linearLayout;
    // UI COMPONENT
    private ImageButton sendMessageButton;
    private EditText messageContentEditText;

    private MainViewModel mainViewModel;

    // UPDATE !!
    private AppUpdater appUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appUpdater = new AppUpdater(this);

        sendMessageButton = findViewById(R.id.send_message_button);
        messageContentEditText = findViewById(R.id.message_content_edit_text);
        messageListRecyclerView = findViewById(R.id.recyclerview_message_list);
        linearLayout = findViewById(R.id.background);

        Glide.with(MainActivity.this)
                .load("https://picsum.photos/412/732")
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true).into(new SimpleTarget<Drawable>() {

            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    linearLayout.setBackground(resource);
                }
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageListRecyclerView.setLayoutManager(linearLayoutManager);

        messageListAdapter = new MessageListAdapter(this, messageListRecyclerView);
        messageListRecyclerView.setAdapter(messageListAdapter);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // register device and update user fcm token
        mainViewModel.registerDevice();

        mainViewModel.loadMessagesAndListenNewMessage().observe(this, new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messages) {
                messageListAdapter.setMessages(messages);

            }
        });

        messageListAdapter.setOnLoadMoreMessageListener(onLoadMoreMessageListener);

        sendMessageButton.setOnClickListener(sendMessageButtonOnClickListener);

//        appUpdater.setUpdateFrom(UpdateFrom.GITHUB)
//                .setGitHubUserAndRepo("trongtran178", "chat_application")
//                .setDisplay(Display.NOTIFICATION)
//                .start();

        appUpdater.setUpdateFrom(UpdateFrom.JSON)
                .setUpdateJSON("https://github.com/trongtran178/chat_application/blob/master/app/update-changelog.json")
                .setDisplay(Display.NOTIFICATION)
                .start();

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
                //
                mainViewModel.signOut();

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

            String sender = mainViewModel.getCurrentUser().getEmail();
            String content = messageContentEditText.getText().toString();

            // viewModel send message
            mainViewModel.sendMessage(sender, content);

            messageContentEditText.setText(null);
            messageContentEditText.setCursorVisible(false);

            if (messageListAdapter.getMessages().size() > 0)
                messageListRecyclerView.smoothScrollToPosition(messageListAdapter.getMessages().size() - 1);
        }
    };

    private OnLoadMoreMessageListener onLoadMoreMessageListener = new OnLoadMoreMessageListener() {
        @Override
        public void onLoadMore() {
            if (mainViewModel.isFullMessages()) return;
            mainViewModel.loading();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mainViewModel.loadOldMessages().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            messageListAdapter.setLoading(false);
                            messageListRecyclerView.scrollToPosition(50);
                        }
                    });

                }
            }, 2000);
        }
    };
}
