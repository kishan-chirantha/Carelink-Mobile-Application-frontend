package lk.kishan.carelink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lk.kishan.carelink.R;
import lk.kishan.carelink.model.Notification;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        holder.title.setText(notification.getTitle());
        holder.body.setText(notification.getBody());
        holder.time.setText(notification.getTimestamp());

        if (!notification.isRead()) {
            holder.unreadAccent.setVisibility(View.VISIBLE);
            holder.title.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.unreadAccent.setVisibility(View.GONE);
            holder.title.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        String type = notification.getType() != null ? notification.getType().toUpperCase() : "SYSTEM";

        switch (type) {
            case "ORDER":
                holder.icon.setImageResource(R.drawable.order);
                holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_primary));
                holder.tag.setVisibility(View.VISIBLE);
                holder.tag.setText("Order Update");
                holder.tag.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_primary));
                break;
            case "PROMO":
                holder.icon.setImageResource(R.drawable.notification);
                holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_orange_dark));
                holder.tag.setVisibility(View.VISIBLE);
                holder.tag.setText("Offer");
                holder.tag.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_orange_dark));
                break;
            default:
                holder.icon.setImageResource(R.drawable.notification);
                holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_outline));
                holder.tag.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return notifications == null ? 0 : notifications.size();
    }

    public void updateList(List<Notification> newList) {
        this.notifications = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, body, time, tag;
        ImageView icon;
        View unreadAccent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            body = itemView.findViewById(R.id.notification_body);
            time = itemView.findViewById(R.id.notification_time);
            tag = itemView.findViewById(R.id.notification_tag);
            icon = itemView.findViewById(R.id.notification_icon);
            unreadAccent = itemView.findViewById(R.id.view_unread_accent);
        }
    }
}