package com.hazora.app.ui.hazardscan;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.hazora.app.R;

public class HazardScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hazard_scan);

        View back = findViewById(R.id.tv_back);
        if (back != null) back.setOnClickListener(v -> finish());

        View startBtn = findViewById(R.id.btn_start_scan);
        if (startBtn != null) {
            startBtn.setOnClickListener(v ->
                    android.widget.Toast.makeText(this, "AI Hazard Scan coming soon.", android.widget.Toast.LENGTH_SHORT).show());
        }
    }
}
