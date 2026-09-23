package com.hazora.app.ui.incidents;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hazora.app.R;

public class IncidentDetailActivity extends AppCompatActivity {

    private int incidentIndex = -1;
    private Incident incident;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_detail);

        View back = findViewById(R.id.btn_detail_back);
        if (back != null) back.setOnClickListener(v -> finish());

        incidentIndex = getIntent().getIntExtra("incident_index", -1);
        if (incidentIndex >= 0) {
            incident = IncidentRepository.getIncident(incidentIndex);
        } else {
            incident = (Incident) getIntent().getSerializableExtra("incident_data");
        }

        if (incident != null) {
            setupViews();
        }

        Button ack = findViewById(R.id.btn_acknowledge);
        if (ack != null) {
            if (incident == null || "Resolved".equalsIgnoreCase(incident.getStatus()) || "Acknowledged".equalsIgnoreCase(incident.getStatus())) {
                ack.setVisibility(View.GONE);
            } else {
                ack.setVisibility(View.VISIBLE);
                ack.setOnClickListener(v -> {
                    if (incident != null && incident.getId() != null && !incident.getId().startsWith("s")) {
                        FirebaseFirestore.getInstance().collection("incidents").document(incident.getId())
                                .update("status", "Acknowledged")
                                .addOnSuccessListener(aVoid -> {
                                    incident.setStatus("Acknowledged");
                                    if (incidentIndex >= 0) {
                                        IncidentRepository.updateStatus(incidentIndex, "Acknowledged");
                                    }
                                    updateStatusUI("Acknowledged");
                                    ack.setVisibility(View.GONE);
                                    Toast.makeText(this, "Incident Acknowledged", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        if (incident != null) incident.setStatus("Acknowledged");
                        if (incidentIndex >= 0) {
                            IncidentRepository.updateStatus(incidentIndex, "Acknowledged");
                        }
                        updateStatusUI("Acknowledged");
                        ack.setVisibility(View.GONE);
                        Toast.makeText(this, "Incident Acknowledged", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        Button share = findViewById(R.id.btn_share_team);
        if (share != null) {
            share.setOnClickListener(v -> {
                if (incident != null) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "HAZORA Safety Incident: " + incident.getTitle());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "HAZORA Safety Alert:\n" +
                            "Hazard: " + incident.getTitle() + "\n" +
                            "Location: " + incident.getSite() + "\n" +
                            "Camera: " + incident.getCameraId() + "\n" +
                            "Severity: " + incident.getSeverity() + "\n" +
                            "Status: " + incident.getStatus());
                    startActivity(Intent.createChooser(shareIntent, "Share Incident Report"));
                }
            });
        }
    }

    private void setupViews() {
        ImageView photo = findViewById(R.id.img_detail_photo);
        if (photo != null && incident.getImageUrl() != null && !incident.getImageUrl().trim().isEmpty()) {
            photo.setVisibility(View.VISIBLE);
            Glide.with(this).load(incident.getImageUrl()).into(photo);
        }

        TextView bannerTitle = findViewById(R.id.tv_detail_banner_title);
        if (bannerTitle != null) bannerTitle.setText(incident.getTitle());

        TextView bannerSub = findViewById(R.id.tv_detail_banner_subtitle);
        if (bannerSub != null) bannerSub.setText(incident.getCameraId() + " • " + incident.getTime());

        TextView hazardType = findViewById(R.id.tv_detail_hazard_type);
        if (hazardType != null) hazardType.setText(incident.getTitle());

        TextView location = findViewById(R.id.tv_detail_location);
        if (location != null) location.setText(incident.getSite());

        TextView camera = findViewById(R.id.tv_detail_camera);
        if (camera != null) camera.setText(incident.getCameraId());

        TextView reportedBy = findViewById(R.id.tv_detail_reported_by);
        if (reportedBy != null) reportedBy.setText(incident.getCameraId() + " (AI Detection)");

        // Date and Time parsing
        String fullTime = incident.getTime();
        String dateStr = "Jun 30, 2026";
        String timeStr = fullTime;
        if (fullTime.contains("•")) {
            String[] parts = fullTime.split("•");
            dateStr = parts[0].trim();
            timeStr = parts[1].trim();
        }

        TextView tvDate = findViewById(R.id.tv_detail_date);
        if (tvDate != null) tvDate.setText(dateStr);

        TextView tvTime = findViewById(R.id.tv_detail_time);
        if (tvTime != null) tvTime.setText(timeStr);

        updateStatusUI(incident.getStatus());
        updateSeverityUI(incident.getSeverity());

        TextView rec = findViewById(R.id.tv_detail_recommendation);
        if (rec != null) {
            String desc = incident.getDescription();
            String prev = incident.getPrevention();
            if (desc != null && !desc.isEmpty()) {
                rec.setText("• " + desc + (prev != null && !prev.isEmpty() ? "\n• " + prev : ""));
            }
        }

        ImageView catIcon = findViewById(R.id.img_detail_category_icon);
        if (catIcon != null) {
            String lower = incident.getTitle().toLowerCase();
            if (lower.contains("hat") || lower.contains("helmet")) catIcon.setImageResource(R.drawable.ic_hard_hat);
            else if (lower.contains("vest")) catIcon.setImageResource(R.drawable.ic_vest);
            else if (lower.contains("shoe") || lower.contains("boot")) catIcon.setImageResource(R.drawable.ic_shoes);
            else catIcon.setImageResource(R.drawable.ic_warning);
        }
    }

    private void updateStatusUI(String status) {
        TextView bannerStatus = findViewById(R.id.tv_detail_status_banner);
        TextView tableStatus = findViewById(R.id.tv_detail_status_table);

        String text = status != null ? status : "New";
        int bgRes = R.drawable.bg_badge_red_light;
        String colorHex = "#EF4444";

        if ("Acknowledged".equalsIgnoreCase(text)) {
            bgRes = R.drawable.bg_badge_orange_light;
            colorHex = "#D97706";
        } else if ("Resolved".equalsIgnoreCase(text) || "Done".equalsIgnoreCase(text)) {
            text = "Resolved";
            bgRes = R.drawable.bg_badge_green_light;
            colorHex = "#16A34A";
        }

        if (bannerStatus != null) {
            bannerStatus.setText(text);
            bannerStatus.setBackgroundResource(bgRes);
            bannerStatus.setTextColor(Color.parseColor(colorHex));
        }

        if (tableStatus != null) {
            tableStatus.setText(text);
            tableStatus.setBackgroundResource(bgRes);
            tableStatus.setTextColor(Color.parseColor(colorHex));
        }
    }

    private void updateSeverityUI(String severity) {
        TextView tvSev = findViewById(R.id.tv_detail_severity);
        ImageView dot = findViewById(R.id.img_detail_severity_dot);
        View layout = findViewById(R.id.layout_detail_severity);

        String sev = severity != null ? severity : "Critical";
        int bgRes = R.drawable.bg_badge_severity_critical;
        int dotRes = R.drawable.ic_dot_red;
        String colorHex = "#EF4444";

        if ("High".equalsIgnoreCase(sev)) {
            bgRes = R.drawable.bg_badge_severity_high;
            dotRes = R.drawable.ic_dot_orange;
            colorHex = "#D97706";
        } else if ("Medium".equalsIgnoreCase(sev)) {
            bgRes = R.drawable.bg_badge_severity_medium;
            dotRes = R.drawable.ic_dot_yellow;
            colorHex = "#CA8A04";
        } else if ("Low".equalsIgnoreCase(sev)) {
            bgRes = R.drawable.bg_badge_severity_low;
            dotRes = R.drawable.ic_dot_green;
            colorHex = "#16A34A";
        }

        if (tvSev != null) {
            tvSev.setText(sev);
            tvSev.setTextColor(Color.parseColor(colorHex));
        }
        if (dot != null) {
            dot.setImageResource(dotRes);
        }
        if (layout != null) {
            layout.setBackgroundResource(bgRes);
        }
    }
}
