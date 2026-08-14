package com.hazora.app.ui.incidents;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.hazora.app.R;

/** Placeholder Incidents screen for Phase 7A. */
public class IncidentsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incidents);

        View back = findViewById(R.id.tv_back);
        if (back != null) back.setOnClickListener(v -> finish());
    }
}
