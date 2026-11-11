package com.example.transportapp;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    /** One chat message as shown on screen. */
    public static class Message {
        final String senderUid, senderName, senderRole, text;

        public Message(String senderUid, String senderName, String senderRole, String text) {
            this.senderUid = senderUid;
            this.senderName = senderName;
            this.senderRole = senderRole;
            this.text = text;
        }
    }

    private final List<Message> messageList;
    private final String myUid;

    public ChatAdapter(List<Message> messageList, String myUid) {
        this.messageList = messageList;
        this.myUid = myUid;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Message message = messageList.get(position);
        boolean mine = myUid != null && myUid.equals(message.senderUid);

        String who = mine ? "You" : (message.senderName != null ? message.senderName : "Unknown");
        if (!mine && message.senderRole != null && !message.senderRole.isEmpty()) {
            who += " (" + message.senderRole + ")";
        }
        holder.senderTextView.setText(who);
        holder.messageTextView.setText(message.text);
        ((LinearLayout) holder.itemView).setGravity(mine ? Gravity.END : Gravity.START);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {

        TextView senderTextView, messageTextView;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}
