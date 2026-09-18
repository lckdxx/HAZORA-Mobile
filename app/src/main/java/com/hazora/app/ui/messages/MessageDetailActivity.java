package com.hazora.app.ui.messages;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.hazora.app.R;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageDetailActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance("hazora");
    private final ArrayList<Message> chatMessages = new ArrayList<>();
    private ChatAdapter adapter;
    private EditText etInput;
    private String recipientEmail;
    private String currentUserId;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_detail);

        recipientEmail = getIntent().getStringExtra("message_sender"); // This is the sender email
        String recipientName = getIntent().getStringExtra("message_display_name");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = user != null ? user.getUid() : "anonymous";
        currentUserEmail = user != null ? user.getEmail() : "anonymous@hazora.com";

        TextView tvHeaderName = findViewById(R.id.tv_header_name);
        tvHeaderName.setText(recipientName != null ? recipientName : recipientEmail);

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());

        RecyclerView rvChat = findViewById(R.id.rv_chat);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(chatMessages);
        rvChat.setAdapter(adapter);

        etInput = findViewById(R.id.et_chat_input);
        findViewById(R.id.btn_send_chat).setOnClickListener(v -> sendMessage(null));
        findViewById(R.id.btn_attach_image).setOnClickListener(v -> pickImage());

        listenForMessages();
    }

    private void listenForMessages() {
        // Listen for messages between current user and the recipient
        // In a real app, this would be a more complex query or a "conversationId"
        db.collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        chatMessages.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String sender = doc.getString("senderEmail");
                            String recipient = doc.getString("recipient");
                            
                            // Check if message belongs to this conversation
                            boolean isRelevant = (sender != null && sender.equals(currentUserEmail) && recipient != null && recipient.equals(recipientEmail)) ||
                                               (sender != null && sender.equals(recipientEmail) && recipient != null && recipient.equals(currentUserEmail));
                            
                            if (isRelevant) {
                                String body = doc.getString("message");
                                String imageUrl = doc.getString("imageUrl");
                                Object createdAt = doc.get("createdAt");
                                String time = "";
                                if (createdAt instanceof Timestamp) {
                                    time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(((Timestamp) createdAt).toDate());
                                }

                                boolean isMine = sender.equals(currentUserEmail);
                                chatMessages.add(new Message(doc.getId(), sender, sender, currentUserId, body, body, time, false, isMine, imageUrl));
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (!chatMessages.isEmpty()) {
                            RecyclerView rvChat = findViewById(R.id.rv_chat);
                            rvChat.scrollToPosition(chatMessages.size() - 1);
                        }
                    }
                });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(selectedImage);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                
                // Compress and convert to Base64 (consistent with scans)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                
                sendMessage(base64);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendMessage(String base64Image) {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() && base64Image == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderEmail", currentUserEmail);
        msg.put("senderId", currentUserId);
        msg.put("recipient", recipientEmail);
        msg.put("message", text);
        msg.put("imageUrl", base64Image);
        msg.put("createdAt", Timestamp.now());
        msg.put("readAt", null);

        db.collection("messages").add(msg)
                .addOnSuccessListener(ref -> etInput.setText(""))
                .addOnFailureListener(e -> Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show());
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private final List<Message> messages;

        ChatAdapter(List<Message> messages) { this.messages = messages; }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bubble, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            Message m = messages.get(position);
            holder.tvMsg.setText(m.getBody());
            holder.tvTime.setText(m.getTime());
            
            if (m.getImageUrl() != null) {
                holder.ivImage.setVisibility(View.VISIBLE);
                try {
                    byte[] decodedString = Base64.decode(m.getImageUrl(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivImage.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    holder.ivImage.setVisibility(View.GONE);
                }
            } else {
                holder.ivImage.setVisibility(View.GONE);
            }

            // Styling bubbles
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.bubble.getLayoutParams();
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(24);
            
            if (m.isMine()) {
                params.gravity = Gravity.END;
                shape.setColor(Color.parseColor("#1F6FB2")); // Messenger Blue
                holder.tvMsg.setTextColor(Color.WHITE);
                holder.tvTime.setTextColor(Color.parseColor("#CCFFFFFF"));
            } else {
                params.gravity = Gravity.START;
                shape.setColor(Color.parseColor("#F1F1F1")); // Light Gray
                holder.tvMsg.setTextColor(Color.BLACK);
                holder.tvTime.setTextColor(Color.GRAY);
            }
            holder.bubble.setLayoutParams(params);
            holder.bubble.setBackground(shape);
        }

        @Override
        public int getItemCount() { return messages.size(); }

        static class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView tvMsg, tvTime;
            ImageView ivImage;
            View bubble;
            ChatViewHolder(View v) {
                super(v);
                tvMsg = v.findViewById(R.id.tv_chat_message);
                tvTime = v.findViewById(R.id.tv_chat_time);
                ivImage = v.findViewById(R.id.iv_chat_image);
                bubble = v.findViewById(R.id.layout_bubble);
            }
        }
    }
}
