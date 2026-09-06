package com.hazora.app.ui.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.hazora.app.R;
import com.hazora.app.ui.hazardscan.HazardScanActivity;
import com.hazora.app.ui.incidents.IncidentsActivity;
import com.hazora.app.ui.messages.MessagesActivity;
import com.hazora.app.ui.profile.ProfileActivity;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance("hazora");
    private TextView tvName, tvActiveHazards, tvTotalIncidents, tvResolvedIncidents;
    private LinearLayout containerRecent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initViews();
        setupNavigation();
        loadUserData();
        fetchStats();
        fetchRecentDetections();
    }

    private void initViews() {
        tvName = findViewById(R.id.tv_name);
        tvActiveHazards = findViewById(R.id.tv_active_hazards);
        tvTotalIncidents = findViewById(R.id.tv_total_incidents);
        tvResolvedIncidents = findViewById(R.id.tv_resolved_incidents);
        containerRecent = findViewById(R.id.container_recent_detections);

        findViewById(R.id.card_ai_scan).setOnClickListener(v -> 
            startActivity(new Intent(this, HazardScanActivity.class)));
            
        findViewById(R.id.card_messages).setOnClickListener(v -> 
            startActivity(new Intent(this, MessagesActivity.class)));

        View incidentsList = findViewById(R.id.card_incidents_list);
        if (incidentsList != null) {
            incidentsList.setOnClickListener(v -> 
                startActivity(new Intent(this, IncidentsActivity.class)));
        }

        findViewById(R.id.tv_see_all).setOnClickListener(v -> 
            startActivity(new Intent(this, IncidentsActivity.class)));
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && tvName != null) {
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = user.getEmail();
            }
            tvName.setText(name);
        }
    }

    private void fetchStats() {
        // Fetch from "incidents" collection
        db.collection("incidents").addSnapshotListener((value, error) -> {
            if (value != null) {
                int total = value.size();
                int resolved = 0;
                int active = 0;
                for (DocumentSnapshot doc : value.getDocuments()) {
                    String status = doc.getString("status");
                    if ("Resolved".equalsIgnoreCase(status)) resolved++;
                    else active++;
                }
                tvTotalIncidents.setText(String.valueOf(total));
                tvResolvedIncidents.setText(String.valueOf(resolved));
                tvActiveHazards.setText(String.valueOf(active));
            }
        });
    }

    private void fetchRecentDetections() {
        db.collection("incidents")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(3)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        containerRecent.removeAllViews();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            addRecentDetectionCard(doc);
                        }
                    }
                });
    }

    private void addRecentDetectionCard(DocumentSnapshot doc) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_recent_detection, containerRecent, false);
        
        TextView title = card.findViewById(R.id.tv_detection_title);
        TextView details = card.findViewById(R.id.tv_detection_details);
        ImageView icon = card.findViewById(R.id.img_detection_icon);
        TextView status = card.findViewById(R.id.tv_detection_status);

        String type = doc.getString("hazardType");
        String cam = doc.getString("cameraSource");
        Object ts = doc.get("timestamp");
        String stat = doc.getString("status");

        title.setText(type != null ? type : "Safety Violation");
        
        String time = "Recent";
        if (ts instanceof Timestamp) {
            Date date = ((Timestamp) ts).toDate();
            time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
        }
        details.setText((cam != null ? cam : "CAM-XX") + " • " + time);
        
        if (stat != null) {
            status.setText(stat);
            if ("New".equalsIgnoreCase(stat)) status.setTextColor(Color.parseColor("#EF4444"));
            else if ("Resolved".equalsIgnoreCase(stat)) status.setTextColor(Color.parseColor("#22C55E"));
        }

        containerRecent.addView(card);
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;
                else if (id == R.id.nav_incidents) startActivity(new Intent(this, IncidentsActivity.class));
                else if (id == R.id.nav_camera) startActivity(new Intent(this, HazardScanActivity.class));
                else if (id == R.id.nav_messages) startActivity(new Intent(this, MessagesActivity.class));
                else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
                return true;
            });
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}
