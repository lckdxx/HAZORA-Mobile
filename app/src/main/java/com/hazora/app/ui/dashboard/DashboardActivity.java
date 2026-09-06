package com.hazora.app.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.hazora.app.R;
import com.hazora.app.ui.navigation.BottomNavigationHelper;

/** Dashboard UI (static/mock data) for Phase 6A. */
public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        View aiScanCard = findViewById(R.id.card_ai_scan);
        if (aiScanCard != null) {
            aiScanCard.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, com.hazora.app.ui.hazardscan.HazardScanActivity.class);
                startActivity(intent);
            });
        }

        View messagesCard = findViewById(R.id.card_messages);
        if (messagesCard != null) {
            messagesCard.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, com.hazora.app.ui.messages.MessagesActivity.class);
                startActivity(intent);
            });
        }

        BottomNavigationHelper.bind(this, R.id.nav_home);

        View seeAll = findViewById(R.id.tv_see_all);
        if (seeAll != null) {
            seeAll.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, com.hazora.app.ui.incidents.IncidentsActivity.class);
                startActivity(intent);
            });
        }
    }
}
