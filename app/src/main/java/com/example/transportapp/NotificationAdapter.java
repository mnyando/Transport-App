package com.example.transportapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


// Notification Adapter
class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<Notification> notificationList;
    private OnNotificationClickListener listener;

    public NotificationAdapter(List<Notification> notificationList, OnNotificationClickListener listener) {
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @Override
    public NotificationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false); // Two-line layout for parent and message
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.text1.setText(notification.getParentName() != null ?
                "To: " + notification.getParentName() :
                "System Notification");
        holder.text2.setText(notification.getMessage());
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        NotificationViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1); // Parent ID/name
            text2 = itemView.findViewById(android.R.id.text2); // Message
        }
    }

    interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }
}
