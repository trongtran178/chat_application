package com.example.chatapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.OnLoadMoreMessageListener;
import com.example.chatapp.R;
import com.example.chatapp.models.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class MessageListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<Message> messages;
    private LayoutInflater inflater;
    private FirebaseAuth firebaseAuth;
    private OnLoadMoreMessageListener onLoadMoreMessageListener;

    private boolean isLoading = false;
    private int visibleThreshold = 5;
    private int firstVisibleRow, totalRowCount;

    public MessageListAdapter(Context context, RecyclerView recyclerView) {
        this.context = context;
        this.inflater = LayoutInflater.from(this.context);
        this.messages = new ArrayList<>();
        this.firebaseAuth = FirebaseAuth.getInstance();

        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    totalRowCount = linearLayoutManager.getItemCount();
                    firstVisibleRow = linearLayoutManager.findFirstVisibleItemPosition();
                    System.out.println("totalRowCount: " + totalRowCount + ", lastVisibleRow: " + firstVisibleRow);
                    if (!isLoading && firstVisibleRow <= visibleThreshold) {
                        if (onLoadMoreMessageListener != null) {
                            System.out.println("On load more");
                            onLoadMoreMessageListener.onLoadMore();
                        }
                        isLoading = true;
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE.TYPE_MY_MSG.ordinal())
            return new MyMessageHolder(this.inflater.inflate(R.layout.item_my_message, parent, false));
        else if (viewType == ITEM_TYPE.TYPE_FRIENDS_MSG.ordinal())
            return new OthersMessageHolder(this.inflater.inflate(R.layout.item_others_message, parent, false));
        else return new LoadingHolder(this.inflater.inflate(R.layout.item_loading, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        int itemViewType = getItemViewType(position);
        if (itemViewType == ITEM_TYPE.TYPE_MY_MSG.ordinal()) {
            ((MyMessageHolder) holder).content.setVisibility(View.VISIBLE);
            ((MyMessageHolder) holder).content.setText(message.getContent());
            handleMessageAvatar(messages.get(position).getSender(), holder, itemViewType);
        } else if (itemViewType == ITEM_TYPE.TYPE_FRIENDS_MSG.ordinal()) {
            ((OthersMessageHolder) holder).content.setVisibility(View.VISIBLE);
            ((OthersMessageHolder) holder).content.setText(message.getContent());
            handleMessageAvatar(messages.get(position).getSender(), holder, itemViewType);
        } else {
            ((LoadingHolder) holder).progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void handleMessageAvatar(String sender, @NonNull RecyclerView.ViewHolder holder,
                                     int itemViewType) {
        switch (sender) {
            case "trong.it203@gmail.com": {
                if (itemViewType == ITEM_TYPE.TYPE_MY_MSG.ordinal())
                    Glide.with(context).load(R.drawable.trongtran).into(((MyMessageHolder) holder).headImage);
                else
                    Glide.with(context).load(R.drawable.trongtran).into(((OthersMessageHolder) holder).headImage);
                break;
            }
            case "tuanmai@gmail.com": {
                if (itemViewType == ITEM_TYPE.TYPE_MY_MSG.ordinal())
                    Glide.with(context).load(R.drawable.tuanmai).into(((MyMessageHolder) holder).headImage);
                else
                    Glide.with(context).load(R.drawable.tuanmai).into(((OthersMessageHolder) holder).headImage);
                break;
            }
            case "vunguyen@gmail.com": {
                if (itemViewType == ITEM_TYPE.TYPE_MY_MSG.ordinal())
                    Glide.with(context).load(R.drawable.vunguyen).into(((MyMessageHolder) holder).headImage);
                else
                    Glide.with(context).load(R.drawable.vunguyen).into(((OthersMessageHolder) holder).headImage);
                break;
            }
            case "tuho@gmail.com": {
                if (itemViewType == ITEM_TYPE.TYPE_MY_MSG.ordinal())
                    Glide.with(context).load(R.drawable.tuho).into(((MyMessageHolder) holder).headImage);
                else
                    Glide.with(context).load(R.drawable.tuho).into(((OthersMessageHolder) holder).headImage);
                break;
            }
        }

    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setOnLoadMoreMessageListener(OnLoadMoreMessageListener
                                                     onLoadMoreMessageListener) {
        this.onLoadMoreMessageListener = onLoadMoreMessageListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position) == null) return ITEM_TYPE.TYPE_LOADING.ordinal();
        else if (messages.get(position).getSender().equalsIgnoreCase(firebaseAuth.getCurrentUser().getEmail()))
            return ITEM_TYPE.TYPE_MY_MSG.ordinal();
        else return ITEM_TYPE.TYPE_FRIENDS_MSG.ordinal();
    }

    public enum ITEM_TYPE {
        TYPE_MY_MSG,
        TYPE_FRIENDS_MSG,
        TYPE_LOADING
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public static class MyMessageHolder extends RecyclerView.ViewHolder {
        private ImageView headImage;
        private TextView content;

        public MyMessageHolder(@NonNull View itemView) {
            super(itemView);
            this.headImage = itemView.findViewById(R.id.my_message_avatar);
            this.content = itemView.findViewById(R.id.my_message_content);
        }
    }

    public static class OthersMessageHolder extends RecyclerView.ViewHolder {
        private ImageView headImage;
        private TextView content;


        public OthersMessageHolder(@NonNull View itemView) {
            super(itemView);
            this.headImage = itemView.findViewById(R.id.others_message_avatar);
            this.content = itemView.findViewById(R.id.others_message_content);
        }
    }

    public static class LoadingHolder extends RecyclerView.ViewHolder {
        private ProgressBar progressBar;

        public LoadingHolder(@NonNull View itemView) {
            super(itemView);
            this.progressBar = itemView.findViewById(R.id.loading_progress_bar);
        }

    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }
}
