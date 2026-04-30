package lk.kishan.carelink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.kishan.carelink.R;
import lk.kishan.carelink.model.Chat;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int MSG_TYPE_SENT = 1;
    private static final int MSG_TYPE_RECEIVED = 2;

    private List<Chat> chatList;
    private String currentUserId;

    public ChatAdapter(List<Chat> chatList, String currentUserId) {
        this.chatList = chatList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_send, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Chat chatMessage = chatList.get(position);

        String timeText = formatTime(chatMessage.getTimestamp());

        if (holder.getItemViewType() == MSG_TYPE_SENT) {
            SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
            sentHolder.messageTxt.setText(chatMessage.getMessage());
            sentHolder.timeTxt.setText(timeText);
        } else {
            ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
            receivedHolder.messageTxt.setText(chatMessage.getMessage());
            receivedHolder.timeTxt.setText(timeText);
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (chatList.get(position).getSenderId().equals(currentUserId)) {
            return MSG_TYPE_SENT;
        } else {
            return MSG_TYPE_RECEIVED;
        }
    }

    public void updateMessages(List<Chat> newList) {
        this.chatList = newList;
        notifyDataSetChanged();
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTxt, timeTxt;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTxt = itemView.findViewById(R.id.item_chat_send_messageTxt);
            timeTxt = itemView.findViewById(R.id.item_chat_send_time);
        }
    }

    public static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTxt, timeTxt;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTxt = itemView.findViewById(R.id.item_chat_received_messageTxt);
            timeTxt = itemView.findViewById(R.id.item_chat_received_time);
        }
    }
}