package com.hazora.app.ui.messages;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.hazora.app.R;

public class MessagesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        View back = findViewById(R.id.tv_back);
        if (back != null) back.setOnClickListener(v -> finish());
    }
}
