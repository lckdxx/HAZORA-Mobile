package com.hazora.app.ui.messages;

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
        ((TextView) findViewById(R.id.tv_sender)).setText(getIntent().getStringExtra("message_sender"));
        ((TextView) findViewById(R.id.tv_role)).setText(getIntent().getStringExtra("message_role"));
        ((TextView) findViewById(R.id.tv_subject)).setText(getIntent().getStringExtra("message_subject"));
        ((TextView) findViewById(R.id.tv_time)).setText(getIntent().getStringExtra("message_time"));
        ((TextView) findViewById(R.id.tv_body)).setText(getIntent().getStringExtra("message_body"));
    }
}