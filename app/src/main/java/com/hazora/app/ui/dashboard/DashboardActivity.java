package com.hazora.app.ui.dashboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.hazora.app.auth.SessionManager;
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
    private TextView tvName, tvActiveHazards, tvTotalIncidents, tvResolvedIncidents, tvSiteContext;
    private LinearLayout containerRecent;
    private SessionManager sessionManager;
    
    // SOS Features
    private MaterialCardView cardSos;
    private View cardSosAlert;
    private boolean isSosActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);
        initViews();
        setupNavigation();
        loadUserData();
        fetchStats();
        fetchRecentDetections();
        showUserGuideIfNeeded();
    }

    private int tutorialStep = 0;
    private View tutorialOverlay;
    private View tutorialHighlight;
    private View tutorialBubble;
    private TextView tvTutorialTitle, tvTutorialDesc;
    private Button btnTutorialNext;

    private void showUserGuideIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("hazora_prefs", MODE_PRIVATE);
        boolean guideShown = prefs.getBoolean("user_guide_shown", false);
        
        if (!guideShown) {
            tutorialOverlay = findViewById(R.id.layout_tutorial_overlay);
            tutorialHighlight = findViewById(R.id.tutorial_highlight);
            tutorialBubble = findViewById(R.id.card_tutorial_bubble);
            tvTutorialTitle = findViewById(R.id.tv_tutorial_title);
            tvTutorialDesc = findViewById(R.id.tv_tutorial_desc);
            btnTutorialNext = findViewById(R.id.btn_tutorial_next);

            if (tutorialOverlay == null) return;
            tutorialOverlay.setVisibility(View.VISIBLE);
            
            findViewById(R.id.btn_tutorial_skip).setOnClickListener(v -> finishTutorial(prefs));
            btnTutorialNext.setOnClickListener(v -> nextTutorialStep(prefs));
            
            // Start Step 0
            updateTutorialUI();
        }
    }

    private void nextTutorialStep(SharedPreferences prefs) {
        tutorialStep++;
        if (tutorialStep > 7) {
            finishTutorial(prefs);
        } else {
            updateTutorialUI();
        }
    }

    private void updateTutorialUI() {
        View target = null;
        String title = "";
        String desc = "";

        switch (tutorialStep) {
            case 0:
                target = findViewById(R.id.card_ai_scan);
                title = "AI Hazard Scan";
                desc = "Check PPE compliance in any lighting.";
                break;
            case 1:
                target = findViewById(R.id.card_sos);
                title = "SOS Alert";
                desc = "Fast emergency signaling for help.";
                break;
            case 2:
                target = findViewById(R.id.card_messages);
                title = "Messenger Chat";
                desc = "Real-time chat and photo sharing.";
                break;
            case 3:
                target = findViewById(R.id.layout_stats_summary);
                title = "Safety Stats";
                desc = "Quick summary of site hazards and resolutions.";
                break;
            case 4:
                target = findViewById(R.id.card_incidents_list);
                title = "Incident History";
                desc = "View the detailed logs of all previous detections.";
                break;
            case 5:
                target = findViewById(R.id.container_recent_detections);
                title = "Recent Feed";
                desc = "Your most recent safety scans at a glance.";
                break;
            case 6:
                target = findViewById(R.id.img_avatar);
                title = "Profile & Settings";
                desc = "Update your info or log out from here.";
                break;
            case 7:
                target = findViewById(R.id.bottom_nav);
                title = "Navigation";
                desc = "Switch quickly between app sections.";
                btnTutorialNext.setText("Finish");
                break;
        }

        if (target != null) {
            tvTutorialTitle.setText(title);
            tvTutorialDesc.setText(desc);
            moveHighlightTo(target);
        }
    }

    private void moveHighlightTo(View target) {
        target.post(() -> {
            int[] location = new int[2];
            target.getLocationInWindow(location);
            
            // Move highlight
            tutorialHighlight.setVisibility(View.VISIBLE);
            tutorialHighlight.setX(location[0] + (target.getWidth() / 2f) - (tutorialHighlight.getWidth() / 2f));
            tutorialHighlight.setY(location[1] + (target.getHeight() / 2f) - (tutorialHighlight.getHeight() / 2f));
            
            // Move bubble (above or below target)
            float bubbleY = location[1] > 1000 ? location[1] - tutorialBubble.getHeight() - 60 : location[1] + target.getHeight() + 60;
            tutorialBubble.setY(Math.max(100, bubbleY));
        });
    }

    private void finishTutorial(SharedPreferences prefs) {
        prefs.edit().putBoolean("user_guide_shown", true).apply();
        tutorialOverlay.setVisibility(View.GONE);
    }

    private void initViews() {
        tvName = findViewById(R.id.tv_name);
        tvSiteContext = findViewById(R.id.tv_site_context);
        tvActiveHazards = findViewById(R.id.tv_active_hazards);
        tvTotalIncidents = findViewById(R.id.tv_total_incidents);
        tvResolvedIncidents = findViewById(R.id.tv_resolved_incidents);
        containerRecent = findViewById(R.id.container_recent_detections);
        
        // SOS Initialization
        cardSos = findViewById(R.id.card_sos);
        cardSosAlert = findViewById(R.id.card_sos_alert);
        
        if (cardSos != null) {
            cardSos.setOnClickListener(v -> toggleSos());
        }

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

    private void toggleSos() {
        if (!isSosActive) {
            // Confirmation for activation
            new MaterialAlertDialogBuilder(this)
                .setTitle("Trigger SOS?")
                .setMessage("Are you sure you want to signal an emergency? This will notify the response team.")
                .setPositiveButton("Trigger", (dialog, which) -> applySosState(true))
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            // Confirmation for deactivation
            new MaterialAlertDialogBuilder(this)
                .setTitle("Stop SOS?")
                .setMessage("Are you sure you want to stop the emergency alert?")
                .setPositiveButton("Stop", (dialog, which) -> applySosState(false))
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    private void applySosState(boolean active) {
        isSosActive = active;
        
        if (isSosActive) {
            // SOS ON: Show alert and change card color to active red
            if (cardSosAlert != null) cardSosAlert.setVisibility(View.VISIBLE);
            if (cardSos != null) {
                cardSos.setCardBackgroundColor(Color.parseColor("#EF4444")); // hazora_danger
                ImageView icon = cardSos.findViewById(R.id.iv_sos_icon);
                if (icon != null) icon.setColorFilter(Color.WHITE);
                TextView label = cardSos.findViewById(R.id.tv_sos_label);
                if (label != null) label.setTextColor(Color.WHITE);
            }
            Toast.makeText(this, "Emergency team is on the way!", Toast.LENGTH_LONG).show();
        } else {
            // SOS OFF: Hide alert and return card to white
            if (cardSosAlert != null) cardSosAlert.setVisibility(View.GONE);
            if (cardSos != null) {
                cardSos.setCardBackgroundColor(Color.WHITE);
                ImageView icon = cardSos.findViewById(R.id.iv_sos_icon);
                if (icon != null) icon.setColorFilter(Color.parseColor("#EF4444"));
                TextView label = cardSos.findViewById(R.id.tv_sos_label);
                if (label != null) label.setTextColor(Color.parseColor("#0D1B2A"));
            }
        }
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String searchKey = user != null && user.getEmail() != null ? user.getEmail() : sessionManager.getUserEmail();

        if (searchKey == null || searchKey.isEmpty()) {
            if (tvSiteContext != null) tvSiteContext.setText("Not assigned location site");
            return;
        }

        // 1. Try searching by username (e.g., "MOB - 001")
        db.collection("mobile_accounts")
                .whereEqualTo("username", searchKey)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        updateProfileHeader(queryDocumentSnapshots.getDocuments().get(0));
                    } else {
                        // 2. Try searching by email/createdByEmail
                        db.collection("mobile_accounts")
                                .whereEqualTo("createdByEmail", searchKey)
                                .get()
                                .addOnSuccessListener(snapshots -> {
                                    if (!snapshots.isEmpty()) {
                                        updateProfileHeader(snapshots.getDocuments().get(0));
                                    } else {
                                        // 3. Fallback to "users" collection
                                        db.collection("users")
                                                .whereEqualTo("email", searchKey)
                                                .get()
                                                .addOnSuccessListener(userSnapshots -> {
                                                    if (!userSnapshots.isEmpty()) {
                                                        updateProfileHeader(userSnapshots.getDocuments().get(0));
                                                    } else {
                                                        if (tvSiteContext != null) tvSiteContext.setText("Not assigned location site");
                                                    }
                                                });
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvSiteContext != null) tvSiteContext.setText("Not assigned location site");
                });
    }

    private void updateProfileHeader(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String site = doc.getString("site");

        if (name != null && !name.isEmpty() && tvName != null) {
            tvName.setText(name);
        }

        if (tvSiteContext != null) {
            if (site != null && !site.isEmpty()) {
                tvSiteContext.setText(site);
            } else {
                tvSiteContext.setText("Not assigned location site");
            }
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
            // Use realtime format with Date
            time = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(date);
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
