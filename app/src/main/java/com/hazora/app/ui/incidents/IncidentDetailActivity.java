package com.hazora.app.ui.incidents;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hazora.app.R;
import com.hazora.app.ui.hazardscan.ZoomableImageView;

import java.util.ArrayList;
import java.util.List;

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

        Button btnValidate = findViewById(R.id.btn_validate_hazard);
        Button btnReclassify = findViewById(R.id.btn_reclassify_hazard);

        if (btnValidate != null) {
            btnValidate.setOnClickListener(v -> {
                if (incident != null && incident.getId() != null) {
                    FirebaseFirestore.getInstance("hazora").collection("incidents").document(incident.getId())
                            .update("validationStatus", "Validated",
                                    "isTrainingDatasetCandidate", true)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Hazard validated & saved for AI training dataset!", Toast.LENGTH_LONG).show();
                                btnValidate.setText("VALIDATED ✓");
                                btnValidate.setEnabled(false);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to validate: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(this, "Hazard validated locally!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnReclassify != null) {
            btnReclassify.setOnClickListener(v -> {
                if (incident != null) {
                    showClassifyDialogInDetail(incident);
                }
            });
        }
    }

    private void showClassifyDialogInDetail(Incident inc) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_classify_hazard);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        RadioGroup rgCategory = dialog.findViewById(R.id.rg_hazard_categories);
        View layoutSubPpe = dialog.findViewById(R.id.layout_sub_ppe);
        View layoutSubScaffolding = dialog.findViewById(R.id.layout_sub_scaffolding);
        View layoutSubOthers = dialog.findViewById(R.id.layout_sub_others);

        CheckBox cbNoHelmet = dialog.findViewById(R.id.cb_no_helmet);
        CheckBox cbNoVest = dialog.findViewById(R.id.cb_no_vest);
        CheckBox cbNoShoes = dialog.findViewById(R.id.cb_no_shoes);
        CheckBox cbAllPpe = dialog.findViewById(R.id.cb_all_ppe);

        CheckBox cbHelmetWorn = dialog.findViewById(R.id.cb_helmet_worn);
        CheckBox cbVestWorn = dialog.findViewById(R.id.cb_vest_worn);
        CheckBox cbShoesWorn = dialog.findViewById(R.id.cb_shoes_worn);
        CheckBox cbFullPpeWorn = dialog.findViewById(R.id.cb_full_ppe_worn);

        CheckBox cbNoLedger = dialog.findViewById(R.id.cb_no_ledger);
        CheckBox cbNoCrossBrace = dialog.findViewById(R.id.cb_no_cross_brace);
        CheckBox cbNoLadder = dialog.findViewById(R.id.cb_no_ladder);
        CheckBox cbNoBasePlate = dialog.findViewById(R.id.cb_no_base_plate);
        CheckBox cbAllScaffolding = dialog.findViewById(R.id.cb_all_scaffolding);

        CheckBox cbLedgerPresent = dialog.findViewById(R.id.cb_ledger_present);
        CheckBox cbCrossBracePresent = dialog.findViewById(R.id.cb_cross_brace_present);
        CheckBox cbLadderPresent = dialog.findViewById(R.id.cb_ladder_present);
        CheckBox cbBasePlatePresent = dialog.findViewById(R.id.cb_base_plate_present);
        CheckBox cbScaffoldingCompliant = dialog.findViewById(R.id.cb_scaffolding_compliant);

        EditText etCustomLabel = dialog.findViewById(R.id.et_custom_hazard_label);
        EditText etNotes = dialog.findViewById(R.id.et_safety_notes);

        if (rgCategory != null) {
            rgCategory.setOnCheckedChangeListener((group, checkedId) -> {
                if (layoutSubPpe != null) layoutSubPpe.setVisibility(checkedId == R.id.rb_cat_no_ppe ? View.VISIBLE : View.GONE);
                if (layoutSubScaffolding != null) layoutSubScaffolding.setVisibility(checkedId == R.id.rb_cat_scaffolding ? View.VISIBLE : View.GONE);
                if (layoutSubOthers != null) layoutSubOthers.setVisibility(checkedId == R.id.rb_cat_others ? View.VISIBLE : View.GONE);
            });
        }

        View cancel = dialog.findViewById(R.id.btn_cancel_classification);
        if (cancel != null) cancel.setOnClickListener(v -> dialog.dismiss());

        View submit = dialog.findViewById(R.id.btn_submit_classification);
        if (submit != null) {
            submit.setOnClickListener(v -> {
                String selectedCategory = "PPE Assessment";
                int checkedId = rgCategory != null ? rgCategory.getCheckedRadioButtonId() : R.id.rb_cat_no_ppe;
                List<String> subTypes = new ArrayList<>();

                if (checkedId == R.id.rb_cat_no_ppe) {
                    selectedCategory = "PPE Assessment";
                    if (cbFullPpeWorn != null && cbFullPpeWorn.isChecked()) {
                        subTypes.add("Full PPE Compliant & Worn");
                    } else if (cbAllPpe != null && cbAllPpe.isChecked()) {
                        subTypes.add("All PPE Items Missing");
                    } else {
                        if (cbNoHelmet != null && cbNoHelmet.isChecked()) subTypes.add("No Safety Helmet");
                        if (cbNoVest != null && cbNoVest.isChecked()) subTypes.add("No Safety Vest");
                        if (cbNoShoes != null && cbNoShoes.isChecked()) subTypes.add("No Safety Shoes");
                        if (cbHelmetWorn != null && cbHelmetWorn.isChecked()) subTypes.add("Safety Helmet Worn");
                        if (cbVestWorn != null && cbVestWorn.isChecked()) subTypes.add("Safety Vest Worn");
                        if (cbShoesWorn != null && cbShoesWorn.isChecked()) subTypes.add("Safety Shoes Worn");
                    }
                } else if (checkedId == R.id.rb_cat_tripping) {
                    selectedCategory = "Tripping / Protruding Hazard";
                } else if (checkedId == R.id.rb_cat_scaffolding) {
                    selectedCategory = "Scaffolding Assessment";
                    if (cbScaffoldingCompliant != null && cbScaffoldingCompliant.isChecked()) {
                        subTypes.add("Scaffolding Fully Compliant & Safe");
                    } else if (cbAllScaffolding != null && cbAllScaffolding.isChecked()) {
                        subTypes.add("All Scaffolding Deficiencies");
                    } else {
                        if (cbNoLedger != null && cbNoLedger.isChecked()) subTypes.add("No Horizontal Ledger");
                        if (cbNoCrossBrace != null && cbNoCrossBrace.isChecked()) subTypes.add("No Cross Brace");
                        if (cbNoLadder != null && cbNoLadder.isChecked()) subTypes.add("No Access Ladder");
                        if (cbNoBasePlate != null && cbNoBasePlate.isChecked()) subTypes.add("No Base Plate");
                        if (cbLedgerPresent != null && cbLedgerPresent.isChecked()) subTypes.add("Horizontal Ledger Present");
                        if (cbCrossBracePresent != null && cbCrossBracePresent.isChecked()) subTypes.add("Cross Brace Present");
                        if (cbLadderPresent != null && cbLadderPresent.isChecked()) subTypes.add("Access Ladder Present");
                        if (cbBasePlatePresent != null && cbBasePlatePresent.isChecked()) subTypes.add("Base Plate Present");
                    }
                } else if (checkedId == R.id.rb_cat_material) {
                    selectedCategory = "Unsafe Material Handling";
                } else if (checkedId == R.id.rb_cat_others) {
                    String customLabel = etCustomLabel != null ? etCustomLabel.getText().toString().trim() : "";
                    selectedCategory = customLabel.isEmpty() ? "Others (Custom Hazard)" : customLabel;
                }

                String notes = etNotes != null ? etNotes.getText().toString().trim() : "";
                String userEmail = FirebaseAuth.getInstance().getCurrentUser() != null ?
                        FirebaseAuth.getInstance().getCurrentUser().getEmail() : "Safety Officer";

                String newHazardType = selectedCategory + (subTypes.isEmpty() ? "" : " (" + String.join(", ", subTypes) + ")");

                if (inc.getId() != null) {
                    FirebaseFirestore.getInstance("hazora").collection("incidents").document(inc.getId())
                            .update("hazardType", newHazardType,
                                    "hazardCategory", selectedCategory,
                                    "subTypes", subTypes,
                                    "validationStatus", "Validated",
                                    "isTrainingDatasetCandidate", true,
                                    "classifiedBy", userEmail,
                                    "description", "Classified by Safety Officer: " + (notes.isEmpty() ? "No additional notes" : notes))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Hazard re-classified & validated!", Toast.LENGTH_SHORT).show();
                                TextView headerTitle = findViewById(R.id.tv_header_title);
                                if (headerTitle != null) headerTitle.setText(newHazardType);
                                TextView cardTitle = findViewById(R.id.tv_title);
                                if (cardTitle != null) cardTitle.setText(newHazardType);
                            });
                }
                dialog.dismiss();
            });
        }

        dialog.show();
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
            bgColor = Color.parseColor("#FEE2E2");
            textColor = Color.parseColor("#EF4444");
        } else if ("Acknowledged".equalsIgnoreCase(status)) {
            bgColor = Color.parseColor("#FFF7ED");
            textColor = Color.parseColor("#F59E0B");
        } else if ("Resolved".equalsIgnoreCase(status)) {
            bgColor = Color.parseColor("#DCFCE7");
            textColor = Color.parseColor("#16A34A");
        } else {
            bgColor = Color.WHITE;
            textColor = Color.BLACK;
        }

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(getResources().getDisplayMetrics().density * 12);
        statusTv.setBackground(gd);
        statusTv.setTextColor(textColor);
    }
}
