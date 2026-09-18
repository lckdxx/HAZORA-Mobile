package com.hazora.app.ui.messages;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.hazora.app.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MessagesActivity extends AppCompatActivity {

    private final ArrayList<Message> messages = new ArrayList<>();
    private final ArrayList<Message> visibleMessages = new ArrayList<>();
    private final Map<String, String> nameCache = new HashMap<>();
    private MessageAdapter adapter;
    private TextView unreadSummary;
    private View emptyState;
    private Button allFilter;
    private Button unreadFilter;
    private boolean showingUnread;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance("hazora");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        View back = findViewById(R.id.tv_back);
        back.setOnClickListener(v -> finish());

        unreadSummary = findViewById(R.id.tv_unread_summary);
        emptyState = findViewById(R.id.layout_empty_state);
        allFilter = findViewById(R.id.btn_filter_all);
        unreadFilter = findViewById(R.id.btn_filter_unread);

        RecyclerView recyclerView = findViewById(R.id.rv_messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(visibleMessages, new MessageAdapter.OnMessageClickListener() {
            @Override
            public void onMessageClick(Message message) {
                openMessage(message);
            }

            @Override
            public void onMessageLongClick(Message message) {
                confirmDeletion(message);
            }
        });
        recyclerView.setAdapter(adapter);
        allFilter.setOnClickListener(v -> setFilter(false));
        unreadFilter.setOnClickListener(v -> setFilter(true));

        TextView deleteRead = findViewById(R.id.tv_delete_read);
        deleteRead.setOnClickListener(v -> confirmDeleteAllRead());

        findViewById(R.id.fab_compose).setOnClickListener(v -> 
            startActivity(new Intent(this, ComposeMessageActivity.class)));

        fetchMessagesFromFirebase();
    }

    private void fetchMessagesFromFirebase() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                             FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        db.collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error fetching messages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        messages.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String id = doc.getId();
                            String body = doc.getString("message");
                            String senderEmail = doc.getString("senderEmail");
                            String senderId = doc.getString("senderId");
                            String imageUrl = doc.getString("imageUrl");
                            Object createdAt = doc.get("createdAt");
                            Object readAt = doc.get("readAt");

                            String time = "Recent";
                            if (createdAt instanceof Timestamp) {
                                Date date = ((Timestamp) createdAt).toDate();
                                time = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date);
                            }

                            boolean isMine = senderId != null && senderId.equals(currentUserId);
                            String displayName = resolveName(senderEmail);

                            messages.add(new Message(
                                    id,
                                    displayName,
                                    senderEmail,
                                    senderId,
                                    body != null ? body : (imageUrl != null ? "Sent an image" : ""),
                                    body != null ? body : "",
                                    time,
                                    readAt == null && !isMine,
                                    isMine,
                                    imageUrl
                            ));
                        }
                        refreshMessages();
                    }
                });
    }

    private String resolveName(String email) {
        if (email == null) return "Safety System";
        if (nameCache.containsKey(email)) return nameCache.get(email);

        // Perform lazy lookup (the UI will refresh once found)
        db.collection("mobile_accounts").whereEqualTo("username", email).get()
            .addOnSuccessListener(snaps -> {
                if (!snaps.isEmpty()) {
                    String name = snaps.getDocuments().get(0).getString("name");
                    if (name != null) {
                        nameCache.put(email, name);
                        refreshMessages();
                    }
                } else {
                    db.collection("users").whereEqualTo("email", email).get()
                        .addOnSuccessListener(uSnaps -> {
                            if (!uSnaps.isEmpty()) {
                                String name = uSnaps.getDocuments().get(0).getString("name");
                                if (name != null) {
                                    nameCache.put(email, name);
                                    refreshMessages();
                                }
                            }
                        });
                }
            });

        return email; // Return email until name is resolved
    }

    private void confirmDeleteAllRead() {
        ArrayList<Message> readMessages = new ArrayList<>();
        for (Message m : messages) if (!m.isUnread()) readMessages.add(m);

        if (readMessages.isEmpty()) {
            Toast.makeText(this, "No read messages to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Read Messages")
                .setMessage("Delete all " + readMessages.size() + " read messages from the database?")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    for (Message m : readMessages) {
                        db.collection("messages").document(m.getId()).delete();
                    }
                    Toast.makeText(this, "Read messages deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeletion(Message message) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Message")
                .setMessage("Delete this message from the database?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("messages").document(message.getId()).delete();
                    Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        if (message.isUnread()) {
            db.collection("messages").document(message.getId())
                    .update("readAt", Timestamp.now());
        }

        Intent intent = new Intent(this, MessageDetailActivity.class);
        intent.putExtra("message_id", message.getId());
        intent.putExtra("message_sender", message.getSenderEmail());
        intent.putExtra("message_display_name", message.getSender());
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
