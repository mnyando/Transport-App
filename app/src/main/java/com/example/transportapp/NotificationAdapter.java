package com.example.transportapp;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

/**
 * The parent's alert feed. A {@link ListAdapter}: when the live feed updates, DiffUtil redraws
 * only the alerts that changed instead of the whole list (review #14).
 */
class NotificationAdapter extends ListAdapter<Notification, NotificationAdapter.NotificationViewHolder> {

    interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private static final DiffUtil.ItemCallback<Notification> DIFF = new DiffUtil.ItemCallback<Notification>() {
        @Override
        public boolean areItemsTheSame(@NonNull Notification a, @NonNull Notification b) {
            return Objects.equals(a.getId(), b.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Notification a, @NonNull Notification b) {
            // Alerts are never edited after they are sent.
            return Objects.equals(a.getId(), b.getId()) && a.getTimestamp() == b.getTimestamp();
        }
    };

    private final OnNotificationClickListener listener;

    NotificationAdapter(OnNotificationClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        NotificationViewHolder holder = new NotificationViewHolder(view);
        view.setOnClickListener(v -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) listener.onNotificationClick(getItem(position));
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification n = getItem(position);
        String title = n.getTitle() != null && !n.getTitle().isEmpty()
                ? n.getTitle() : holder.itemView.getContext().getString(R.string.app_name);
        CharSequence when = n.getTimestamp() > 0
                ? DateUtils.getRelativeTimeSpanString(n.getTimestamp(), System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS) : "";
        holder.text1.setText(when.length() > 0 ? title + " · " + when : title);
        holder.text2.setText(n.getMessage());
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final TextView text1, text2;

        NotificationViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}
