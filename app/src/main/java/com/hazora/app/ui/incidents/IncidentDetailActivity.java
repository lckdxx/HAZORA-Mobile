package com.hazora.app.ui.incidents;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hazora.app.R;

public class IncidentDetailActivity extends AppCompatActivity {

    private int incidentIndex = -1;
    private Incident incident;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_detail);

        View back = findViewById(R.id.tv_back);
        if (back != null) back.setOnClickListener(v -> finish());

        incidentIndex = getIntent().getIntExtra("incident_index", -1);
        if (incidentIndex >= 0) {
            incident = IncidentRepository.getIncident(incidentIndex);
        }

        if (incident != null) {
            // Set header and card titles
            TextView headerTitle = findViewById(R.id.tv_header_title);
            if (headerTitle != null) headerTitle.setText(incident.getTitle());
            TextView cardTitle = findViewById(R.id.tv_title);
            if (cardTitle != null) cardTitle.setText(incident.getTitle());

            TextView statusTv = findViewById(R.id.tv_status);
            if (statusTv != null) {
                statusTv.setText(incident.getStatus());
                applyStatusStyle(statusTv, incident.getStatus());
            }

            TextView sev = findViewById(R.id.tv_severity);
            if (sev != null) sev.setText(incident.getSeverity());
            TextView cam = findViewById(R.id.tv_camera);
            if (cam != null) cam.setText(incident.getCameraId());
            TextView time = findViewById(R.id.tv_time);
            if (time != null) time.setText(incident.getTime());
            TextView site = findViewById(R.id.tv_site);
            if (site != null) site.setText(incident.getSite());
            TextView desc = findViewById(R.id.tv_description);
            if (desc != null) desc.setText(incident.getDescription());
        }

        Button ack = findViewById(R.id.btn_acknowledge);
        if (ack != null) {
            if (incident == null || "Resolved".equalsIgnoreCase(incident.getStatus())) {
                ack.setVisibility(View.GONE);
            } else if ("New".equalsIgnoreCase(incident.getStatus())) {
                ack.setVisibility(View.VISIBLE);
                ack.setOnClickListener(v -> {
                    IncidentRepository.updateStatus(incidentIndex, "Acknowledged");
                    TextView statusTv = findViewById(R.id.tv_status);
                    statusTv.setText("Acknowledged");
                    applyStatusStyle(statusTv, "Acknowledged");
                    ack.setVisibility(View.GONE);
                });
            } else {
                ack.setVisibility(View.GONE);
            }
        }
    }

    private void applyStatusStyle(TextView statusTv, String status) {
        if (statusTv == null) return;
        int bgColor;
        int textColor;
        if ("New".equalsIgnoreCase(status)) {
            bgColor = android.graphics.Color.parseColor("#FEE2E2");
            textColor = android.graphics.Color.parseColor("#EF4444");
        } else if ("Acknowledged".equalsIgnoreCase(status)) {
            bgColor = android.graphics.Color.parseColor("#FFF7ED");
            textColor = android.graphics.Color.parseColor("#F59E0B");
        } else if ("Resolved".equalsIgnoreCase(status)) {
            bgColor = android.graphics.Color.parseColor("#DCFCE7");
            textColor = android.graphics.Color.parseColor("#16A34A");
        } else {
            bgColor = android.graphics.Color.WHITE;
            textColor = android.graphics.Color.BLACK;
        }

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(getResources().getDisplayMetrics().density * 12);
        statusTv.setBackground(gd);
        statusTv.setTextColor(textColor);
    }
}
