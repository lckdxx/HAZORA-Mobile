package com.hazora.app.ui.incidents;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hazora.app.R;
import com.hazora.app.ui.hazardscan.ZoomableImageView;

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
        } else {
            // Check if passed directly via object
            incident = (Incident) getIntent().getSerializableExtra("incident_data");
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
            if (site != null) {
                String siteStr = incident.getSite();
                if (siteStr == null || siteStr.isEmpty() || "Location Not Set".equalsIgnoreCase(siteStr)) {
                    site.setText("Not assigned location site");
                } else {
                    site.setText(siteStr);
                }
            }
            TextView desc = findViewById(R.id.tv_description);
            if (desc != null) desc.setText(incident.getDescription());
            TextView prev = findViewById(R.id.tv_prevention);
            if (prev != null) prev.setText(incident.getPrevention());

            showCapturedImage(incident.getImageData());
        }

        Button ack = findViewById(R.id.btn_acknowledge);
        if (ack != null) {
            if (incident == null || "Resolved".equalsIgnoreCase(incident.getStatus())) {
                ack.setVisibility(View.GONE);
            } else if ("New".equalsIgnoreCase(incident.getStatus())) {
                ack.setVisibility(View.VISIBLE);
                ack.setOnClickListener(v -> {
                    if (incident != null && incident.getId() != null) {
                        FirebaseFirestore.getInstance().collection("incidents").document(incident.getId())
                                .update("status", "Acknowledged")
                                .addOnSuccessListener(aVoid -> {
                                    IncidentRepository.updateStatus(incidentIndex, "Acknowledged");
                                    TextView statusTv = findViewById(R.id.tv_status);
                                    if (statusTv != null) {
                                        statusTv.setText("Acknowledged");
                                        applyStatusStyle(statusTv, "Acknowledged");
                                    }
                                    ack.setVisibility(View.GONE);
                                });
                    }
                });
            } else {
                ack.setVisibility(View.GONE);
            }
        }
    }

    // Decode the captured snapshot (base64) and show it. Handles both the app's
    // raw base64 and the website's "data:image/jpeg;base64,..." data URLs.
    private void showCapturedImage(String imageData) {
        MaterialCardView card = findViewById(R.id.card_image);
        ImageView img = findViewById(R.id.img_capture);
        if (card == null || img == null) return;

        Bitmap bitmap = decodeBase64Image(imageData);
        if (bitmap == null) {
            card.setVisibility(View.GONE);
            return;
        }

        card.setVisibility(View.VISIBLE);
        img.setImageBitmap(bitmap);
        img.setOnClickListener(v -> showFullscreenImage(bitmap));
    }

    private Bitmap decodeBase64Image(String imageData) {
        if (imageData == null || imageData.trim().isEmpty()) return null;
        try {
            String base64 = imageData;
            int commaIndex = base64.indexOf(',');
            // Strip a data URL prefix like "data:image/jpeg;base64," if present.
            if (base64.startsWith("data:") && commaIndex >= 0) {
                base64 = base64.substring(commaIndex + 1);
            }
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException | OutOfMemoryError e) {
            Log.w("IncidentDetail", "Failed to decode incident image: " + e.getMessage());
            return null;
        }
    }

    private void showFullscreenImage(Bitmap bitmap) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_image_fullscreen);

        ZoomableImageView iv = dialog.findViewById(R.id.iv_fullscreen);
        if (iv != null) iv.setImageBitmap(bitmap);

        View close = dialog.findViewById(R.id.btn_close_fullscreen);
        if (close != null) close.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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
