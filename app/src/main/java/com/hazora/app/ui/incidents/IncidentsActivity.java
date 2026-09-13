package com.hazora.app.ui.incidents;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
                int idx = allIncidents.indexOf(incident);
                Intent intent = new Intent(IncidentsActivity.this, IncidentDetailActivity.class);
                intent.putExtra("incident_index", idx);
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

        setupFilters();
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
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
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
                            Object ts = doc.get("timestamp");
                            
                            String time = "Recent";
                            if (ts instanceof Timestamp) {
                                Date date = ((Timestamp) ts).toDate();
                                // Realtime format with Date as requested
                                time = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(date);
                            }

                            // Handle alternative word for unassigned location
                            if (loc == null || loc.isEmpty() || "Location Not Set".equalsIgnoreCase(loc)) {
                                loc = "Not assigned location site";
                            }

                            Incident inc = new Incident(
                                    doc.getId(),
                                    type != null ? type : "Unknown Hazard",
                                    cam != null ? cam : "CAM-XX",
                                    time,
                                    loc,
                                    status != null ? status : "New",
                                    severity != null ? severity : "High",
                                    description != null ? description : "AI detected a potential safety violation.",
                                    prevention != null ? prevention : "Follow safety protocols."
                            );
                            
                            allIncidents.add(inc);
                            IncidentRepository.getIncidents().add(inc);
                        }
                        applyFilter();
                    }
                });
    }

    private void confirmDeletion(Incident incident) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Incident")
                .setMessage("Delete this incident record from the database?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteIncidentFromFirebase(incident);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteIncidentFromFirebase(Incident incident) {
        db.collection("incidents").document(incident.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Incident deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
                        db.collection("incidents").document(i.getId()).delete();
                    }
                    Toast.makeText(this, "Records deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupFilters() {
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
            all.setTextColor(getResources().getColor(R.color.primary_blue));
            ne.setTextColor(getResources().getColor(R.color.primary_blue));
            ack.setTextColor(getResources().getColor(R.color.primary_blue));
            res.setTextColor(getResources().getColor(R.color.primary_blue));

            v.setBackgroundResource(R.drawable.bg_message_filter_selected);
            ((TextView) v).setTextColor(getResources().getColor(R.color.white));

            applyFilter();
        };

        all.setOnClickListener(click);
        ne.setOnClickListener(click);
        ack.setOnClickListener(click);
        res.setOnClickListener(click);

        TextView deleteResolved = findViewById(R.id.tv_delete_resolved);
        deleteResolved.setOnClickListener(v -> confirmDeleteResolved());

        all.setBackgroundResource(R.drawable.bg_message_filter_selected);
        all.setTextColor(getResources().getColor(R.color.white));
    }

    private void applyFilter() {
        List<Incident> filtered = new ArrayList<>();
        if ("All".equalsIgnoreCase(selectedFilter)) {
            filtered.addAll(allIncidents);
        } else {
            for (Incident i : allIncidents) {
                if (selectedFilter.equalsIgnoreCase(i.getStatus())) filtered.add(i);
            }
        }

        adapter.setItems(filtered);
        View empty = findViewById(R.id.empty_state);
        if (filtered.isEmpty()) empty.setVisibility(View.VISIBLE); else empty.setVisibility(View.GONE);
        
        TextView todayCount = findViewById(R.id.tv_today_count);
        if (todayCount != null) todayCount.setText(String.valueOf(allIncidents.size()));
    }
}
