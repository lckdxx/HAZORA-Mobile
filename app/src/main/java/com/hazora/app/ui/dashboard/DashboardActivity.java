package com.hazora.app.ui.dashboard;

import android.content.Intent;
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
                    Intent intent = new Intent(DashboardActivity.this, com.hazora.app.ui.incidents.IncidentsActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_camera) {
                    Intent intent = new Intent(DashboardActivity.this, com.hazora.app.ui.hazardscan.HazardScanActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_messages) {
                    Intent intent = new Intent(DashboardActivity.this, com.hazora.app.ui.messages.MessagesActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_profile) {
                    Intent intent = new Intent(DashboardActivity.this, com.hazora.app.ui.profile.ProfileActivity.class);
                    startActivity(intent);
                }
                return true;
            });
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}
