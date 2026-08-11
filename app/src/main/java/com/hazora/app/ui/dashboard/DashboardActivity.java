package com.hazora.app.ui.dashboard;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hazora.app.R;

/** Dashboard UI (static/mock data) for Phase 6A. */
public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        View aiScanCard = findViewById(R.id.card_ai_scan);
        if (aiScanCard != null) {
            aiScanCard.setOnClickListener(v ->
                    Toast.makeText(this, "AI Hazard Scan coming soon", Toast.LENGTH_SHORT).show());
        }

        FloatingActionButton fab = findViewById(R.id.fab_camera);
        if (fab != null) {
            fab.setOnClickListener(v ->
                    Toast.makeText(this, "AI Hazard Scan coming soon", Toast.LENGTH_SHORT).show());
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    // stay on dashboard
                    return true;
                } else if (id == R.id.nav_incidents) {
                    Toast.makeText(this, "Incidents coming soon", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_camera) {
                    Toast.makeText(this, "AI Hazard Scan coming soon", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_messages) {
                    Toast.makeText(this, "Messages coming soon", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_profile) {
                    Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}
