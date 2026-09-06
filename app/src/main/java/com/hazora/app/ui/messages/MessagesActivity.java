package com.hazora.app.ui.messages;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hazora.app.R;

import java.util.ArrayList;

public class MessagesActivity extends AppCompatActivity {

    private static final ArrayList<Message> messages = new ArrayList<>();
    private final ArrayList<Message> visibleMessages = new ArrayList<>();
    private MessageAdapter adapter;
    private TextView unreadSummary;
    private View emptyState;
    private Button allFilter;
    private Button unreadFilter;
    private boolean showingUnread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        seedMessages();

        View back = findViewById(R.id.tv_back);
        back.setOnClickListener(v -> finish());

        unreadSummary = findViewById(R.id.tv_unread_summary);
        emptyState = findViewById(R.id.layout_empty_state);
        allFilter = findViewById(R.id.btn_filter_all);
        unreadFilter = findViewById(R.id.btn_filter_unread);

        RecyclerView recyclerView = findViewById(R.id.rv_messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(visibleMessages, this::openMessage);
        recyclerView.setAdapter(adapter);
        allFilter.setOnClickListener(v -> setFilter(false));
        unreadFilter.setOnClickListener(v -> setFilter(true));
        refreshMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) refreshMessages();
    }

    private void seedMessages() {
        if (!messages.isEmpty()) return;
        messages.add(new Message("MSG-001", "Safety Management", "Safety Officer", "New Safety Alert",
                "A new workplace hazard has been detected at Construction Site A.",
                "A new safety hazard was detected by the HAZORA monitoring system at Construction Site A — Zone B. Please review the incident and take appropriate action.", "09:20 AM", true));
        messages.add(new Message("MSG-002", "Site Supervisor", "Site Supervisor", "Incident Acknowledged",
                "The missing hard hat incident has been acknowledged.",
                "The missing hard hat incident detected by CAM-04 has been reviewed and acknowledged by the site supervisor.", "09:05 AM", true));
        messages.add(new Message("MSG-003", "Safety Management", "Safety Officer", "Daily Safety Reminder",
                "Please ensure all workers comply with PPE requirements.",
                "Reminder: all personnel working in monitored areas must wear the required personal protective equipment, including hard hats, safety vests, and safety shoes.", "08:00 AM", true));
        messages.add(new Message("MSG-004", "System", "HAZORA Monitoring System", "Incident Resolved",
                "The missing safety shoes incident has been resolved.",
                "The safety shoes violation detected by CAM-07 has been marked as resolved.", "Yesterday", false));
        messages.add(new Message("MSG-005", "Safety Management", "Safety Officer", "Weekly Safety Report",
                "The latest workplace safety report is available.",
                "The weekly HAZORA safety monitoring report has been prepared for review.", "Yesterday", false));
    }

    private void setFilter(boolean unreadOnly) {
        showingUnread = unreadOnly;
        refreshMessages();
    }

    private void refreshMessages() {
        visibleMessages.clear();
        for (Message message : messages) {
            if (!showingUnread || message.isUnread()) visibleMessages.add(message);
        }
        adapter.notifyDataSetChanged();
        emptyState.setVisibility(visibleMessages.isEmpty() ? View.VISIBLE : View.GONE);
        unreadSummary.setText(getUnreadCount() + " unread messages");
        updateFilterStyles();
    }

    private int getUnreadCount() {
        int count = 0;
        for (Message message : messages) if (message.isUnread()) count++;
        return count;
    }

    private void openMessage(Message message) {
        message.setUnread(false);
        Intent intent = new Intent(this, MessageDetailActivity.class);
        intent.putExtra("message_id", message.getId());
        intent.putExtra("message_sender", message.getSender());
        intent.putExtra("message_role", message.getRole());
        intent.putExtra("message_subject", message.getSubject());
        intent.putExtra("message_time", message.getTime());
        intent.putExtra("message_body", message.getBody());
        startActivity(intent);
    }

    private void updateFilterStyles() {
        applyFilterStyle(allFilter, !showingUnread);
        applyFilterStyle(unreadFilter, showingUnread);
    }

    private void applyFilterStyle(Button button, boolean selected) {
        int blue = Color.parseColor("#1F6FB2");
        int white = Color.WHITE;
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(getResources().getDisplayMetrics().density * 12);
        if (selected) {
            background.setColor(blue);
            button.setTextColor(white);
        } else {
            background.setColor(white);
            background.setStroke((int) (getResources().getDisplayMetrics().density), Color.parseColor("#D6E2F0"));
            button.setTextColor(blue);
        }
        button.setBackgroundTintList(null);
        button.setBackground(background);
    }
}
