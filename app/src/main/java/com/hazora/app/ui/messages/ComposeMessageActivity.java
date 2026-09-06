package com.hazora.app.ui.messages;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hazora.app.R;

import java.util.HashMap;
import java.util.Map;

public class ComposeMessageActivity extends AppCompatActivity {

    private EditText etRecipient;
    private EditText etMessage;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance("hazora");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_message);

        etRecipient = findViewById(R.id.et_recipient);
        etMessage = findViewById(R.id.et_message);

        String replyTo = getIntent().getStringExtra("reply_to");
        if (replyTo != null) {
            etRecipient.setText(replyTo);
        }

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String recipient = etRecipient.getText().toString().trim();
        String body = etMessage.getText().toString().trim();

        if (recipient.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String senderEmail = user != null ? user.getEmail() : "anonymous@hazora.com";
        String senderId = user != null ? user.getUid() : "anonymous";

        Map<String, Object> message = new HashMap<>();
        message.put("message", body);
        message.put("recipient", recipient);
        message.put("senderEmail", senderEmail);
        message.put("senderId", senderId);
        message.put("createdAt", Timestamp.now());
        message.put("readAt", null);
        message.put("recipientType", "email"); // Or search for the user name in database

        db.collection("messages").add(message)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
