package com.hazora.app.ui.messages;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hazora.app.R;

public class MessageDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_detail);

        View back = findViewById(R.id.tv_back);
        back.setOnClickListener(v -> finish());

        String sender = getIntent().getStringExtra("message_sender");
        String subject = getIntent().getStringExtra("message_subject");
        String body = getIntent().getStringExtra("message_body");

        ((TextView) findViewById(R.id.tv_sender)).setText(sender);
        ((TextView) findViewById(R.id.tv_role)).setText(getIntent().getStringExtra("message_role"));
        ((TextView) findViewById(R.id.tv_subject)).setText(subject);
        ((TextView) findViewById(R.id.tv_time)).setText(getIntent().getStringExtra("message_time"));
        ((TextView) findViewById(R.id.tv_body)).setText(body);

        findViewById(R.id.btn_reply).setOnClickListener(v -> {
            Intent intent = new Intent(this, ComposeMessageActivity.class);
            intent.putExtra("reply_to", sender);
            startActivity(intent);
        });
    }
}