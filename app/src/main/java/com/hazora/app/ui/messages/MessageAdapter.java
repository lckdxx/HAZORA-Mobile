package com.hazora.app.ui.messages;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hazora.app.R;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    public interface OnMessageClickListener {
        void onMessageClick(Message message);
    }

    private final List<Message> messages;
    private final OnMessageClickListener listener;

    public MessageAdapter(List<Message> messages, OnMessageClickListener listener) {
        this.messages = messages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.sender.setText(message.getSender());
        holder.subject.setText(message.getSubject());
        holder.preview.setText(message.getPreview());
        holder.time.setText(message.getTime());
        if ("Site Supervisor".equals(message.getSender())) {
            holder.icon.setImageResource(R.drawable.ic_person);
        } else if ("System".equals(message.getSender())) {
            holder.icon.setImageResource(R.drawable.ic_pulse);
        } else {
            holder.icon.setImageResource(R.drawable.ic_warning);
        }
        int weight = message.isUnread() ? Typeface.BOLD : Typeface.NORMAL;
        holder.sender.setTypeface(null, weight);
        holder.subject.setTypeface(null, weight);
        holder.unreadDot.setVisibility(message.isUnread() ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onMessageClick(message));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView sender;
        final TextView subject;
        final TextView preview;
        final TextView time;
        final View unreadDot;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.img_message_icon);
            sender = itemView.findViewById(R.id.tv_sender);
            subject = itemView.findViewById(R.id.tv_subject);
            preview = itemView.findViewById(R.id.tv_preview);
            time = itemView.findViewById(R.id.tv_time);
            unreadDot = itemView.findViewById(R.id.view_unread_dot);
        }
    }
}