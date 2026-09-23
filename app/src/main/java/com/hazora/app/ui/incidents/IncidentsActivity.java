package com.hazora.app.ui.incidents;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.hazora.app.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IncidentsActivity extends AppCompatActivity {

    private IncidentAdapter adapter;
    private final List<Incident> allIncidents = new ArrayList<>();
    private String selectedFilter = "All";
    private String searchQuery = "";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance("hazora");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incidents);

        View back = findViewById(R.id.tv_back);
        if (back != null) back.setOnClickListener(v -> finish());

        adapter = new IncidentAdapter(this, new IncidentAdapter.OnIncidentClickListener() {
            @Override
            public void onIncidentClick(Incident incident) {
                int idx = IncidentRepository.getIncidents().indexOf(incident);
                Intent intent = new Intent(IncidentsActivity.this, IncidentDetailActivity.class);
                if (idx >= 0) {
                    intent.putExtra("incident_index", idx);
                } else {
                    intent.putExtra("incident_data", incident);
                }
                startActivity(intent);
            }

            @Override
            public void onIncidentLongClick(Incident incident) {
                if (isResolvedOrDone(incident)) {
                    confirmDeletion(incident);
                } else {
                    Toast.makeText(IncidentsActivity.this, "Only Resolved or Done incidents can be removed.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        RecyclerView rv = findViewById(R.id.rv_incidents);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        setupSearchAndFilters();
        fetchIncidentsFromFirebase();
    }

    private boolean isResolvedOrDone(Incident incident) {
        String status = incident.getStatus();
        return "Resolved".equalsIgnoreCase(status) || "Done".equalsIgnoreCase(status);
    }

    private void fetchIncidentsFromFirebase() {
        db.collection("incidents")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null && !value.isEmpty()) {
                        allIncidents.clear();
                        IncidentRepository.getIncidents().clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String type = doc.getString("hazardType");
                            String cam = doc.getString("cameraSource");
                            String loc = doc.getString("location");
                            String status = doc.getString("status");
                            String severity = doc.getString("severity");
                            String description = doc.getString("description");
                            String prevention = doc.getString("prevention");
                            String imgUrl = doc.getString("imageUrl");
                            Object ts = doc.get("timestamp");
                            
                            String time = "Recent";
                            if (ts instanceof Timestamp) {
                                Date date = ((Timestamp) ts).toDate();
                                time = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(date);
                            }

                            if (loc == null || loc.isEmpty() || "Location Not Set".equalsIgnoreCase(loc)) {
                                loc = "Zone B — Scaffold Level 3";
                            }

                            Incident inc = new Incident(
                                    doc.getId(),
                                    type != null ? type : "Unknown Hazard",
                                    cam != null ? cam : "CAM-04",
                                    time,
                                    loc,
                                    status != null ? status : "New",
                                    severity != null ? severity : "Critical",
                                    description != null ? description : "AI detected a potential safety violation.",
                                    prevention != null ? prevention : "Follow workplace safety protocols.",
                                    imgUrl
                            );
                            
                            allIncidents.add(inc);
                            IncidentRepository.getIncidents().add(inc);
                        }
                        applyFilter();
                    } else {
                        loadSampleIncidents();
                    }
                });
    }

    private void loadSampleIncidents() {
        allIncidents.clear();
        IncidentRepository.getIncidents().clear();

        Incident i1 = new Incident("s1", "Missing Hard Hat Detected", "CAM-04", "Jun 30, 2026 • 09:14 AM", "Zone B — Scaffold Level 3", "New", "Critical", "Worker observed without hard hat on scaffold level 3.", "Issue replacement hard hat immediately.", null);
        Incident i2 = new Incident("s2", "Missing Safety Vest", "CAM-02", "Jun 30, 2026 • 08:52 AM", "Zone A — Ground Floor", "Acknowledged", "High", "High-visibility vest missing in active machinery zone.", "Supply high-visibility vest before entering area.", null);
        Incident i3 = new Incident("s3", "Missing Safety Shoes", "CAM-07", "Jun 29, 2026 • 14:30 PM", "Zone C — Electrical Room", "Resolved", "Medium", "Non-compliant footwear detected in electrical room.", "Ensure steel-toe footwear compliance.", null);

        allIncidents.add(i1);
        allIncidents.add(i2);
        allIncidents.add(i3);

        IncidentRepository.getIncidents().add(i1);
        IncidentRepository.getIncidents().add(i2);
        IncidentRepository.getIncidents().add(i3);

        applyFilter();
    }

    private void confirmDeletion(Incident incident) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Incident")
                .setMessage("Delete this incident record from the database?")
                .setPositiveButton("Delete", (dialog, which) -> deleteIncidentFromFirebase(incident))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteIncidentFromFirebase(Incident incident) {
        if (incident.getId() != null && !incident.getId().startsWith("s")) {
            db.collection("incidents").document(incident.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Incident deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            allIncidents.remove(incident);
            IncidentRepository.getIncidents().remove(incident);
            applyFilter();
        }
    }

    private void confirmDeleteResolved() {
        List<Incident> toDelete = new ArrayList<>();
        for (Incident i : allIncidents) {
            if (isResolvedOrDone(i)) toDelete.add(i);
        }

        if (toDelete.isEmpty()) {
            Toast.makeText(this, "No resolved incidents to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Resolved Incidents")
                .setMessage("Are you sure you want to delete all " + toDelete.size() + " resolved/done incident records?")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    for (Incident i : toDelete) {
                        if (i.getId() != null && !i.getId().startsWith("s")) {
                            db.collection("incidents").document(i.getId()).delete();
                        }
                    }
                    allIncidents.removeAll(toDelete);
                    IncidentRepository.getIncidents().removeAll(toDelete);
                    applyFilter();
                    Toast.makeText(this, "Records deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupSearchAndFilters() {
        EditText etSearch = findViewById(R.id.et_search_incidents);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase();
                    applyFilter();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        TextView all = findViewById(R.id.filter_all);
        TextView ne = findViewById(R.id.filter_new);
        TextView ack = findViewById(R.id.filter_ack);
        TextView res = findViewById(R.id.filter_resolved);

        View.OnClickListener click = v -> {
            selectedFilter = ((TextView) v).getText().toString();
            all.setBackgroundResource(R.drawable.bg_message_filter_unselected);
            ne.setBackgroundResource(R.drawable.bg_message_filter_unselected);
            ack.setBackgroundResource(R.drawable.bg_message_filter_unselected);
            res.setBackgroundResource(R.drawable.bg_message_filter_unselected);
            
            all.setTextColor(0xFF475569);
            ne.setTextColor(0xFF475569);
            ack.setTextColor(0xFF475569);
            res.setTextColor(0xFF475569);

            v.setBackgroundResource(R.drawable.bg_message_filter_selected);
            ((TextView) v).setTextColor(0xFFFFFFFF);

            applyFilter();
        };

        if (all != null) all.setOnClickListener(click);
        if (ne != null) ne.setOnClickListener(click);
        if (ack != null) ack.setOnClickListener(click);
        if (res != null) res.setOnClickListener(click);

        TextView deleteResolved = findViewById(R.id.tv_delete_resolved);
        if (deleteResolved != null) {
            deleteResolved.setOnClickListener(v -> confirmDeleteResolved());
        }

        if (all != null) {
            all.setBackgroundResource(R.drawable.bg_message_filter_selected);
            all.setTextColor(0xFFFFFFFF);
        }
    }

    private void applyFilter() {
        List<Incident> filtered = new ArrayList<>();
        for (Incident i : allIncidents) {
            boolean matchesStatus = "All".equalsIgnoreCase(selectedFilter) || selectedFilter.equalsIgnoreCase(i.getStatus());
            boolean matchesSearch = searchQuery.isEmpty() ||
                    i.getTitle().toLowerCase().contains(searchQuery) ||
                    i.getCameraId().toLowerCase().contains(searchQuery) ||
                    i.getSite().toLowerCase().contains(searchQuery) ||
                    i.getSeverity().toLowerCase().contains(searchQuery);

            if (matchesStatus && matchesSearch) {
                filtered.add(i);
            }
        }

        adapter.setItems(filtered);
        View empty = findViewById(R.id.empty_state);
        if (empty != null) {
            empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
        
        TextView todayCount = findViewById(R.id.tv_today_count);
        if (todayCount != null) {
            todayCount.setText(String.valueOf(allIncidents.size()));
        }
    }
}
