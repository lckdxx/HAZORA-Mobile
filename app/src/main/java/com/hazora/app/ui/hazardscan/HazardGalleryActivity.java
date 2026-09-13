package com.hazora.app.ui.hazardscan;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.hazora.app.R;
import com.hazora.app.ui.incidents.Incident;
import com.hazora.app.ui.incidents.IncidentDetailActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HazardGalleryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GalleryAdapter adapter;
    private List<Map<String, Object>> hazardList = new ArrayList<>();
    private List<Map<String, Object>> filteredList = new ArrayList<>();
    private String currentSearchQuery = "";
    private String currentSeverityFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hazard_gallery);

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rv_gallery);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GalleryAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        setupFilters();
        setupSearch();
        fetchHazards();
    }

    private void setupSearch() {
        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                applyFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                applyFilters();
                return true;
            }
        });
    }

    private void setupFilters() {
        TextView filterAll = findViewById(R.id.filter_all);
        TextView filterCritical = findViewById(R.id.filter_critical);
        TextView filterHigh = findViewById(R.id.filter_high);
        TextView filterLow = findViewById(R.id.filter_low);

        View.OnClickListener filterClickListener = v -> {
            filterAll.setBackgroundResource(R.drawable.bg_message_filter_unselected);
            filterCritical.setBackgroundResource(R.drawable.bg_message_filter_unselected);
            filterHigh.setBackgroundResource(R.drawable.bg_message_filter_unselected);
            filterLow.setBackgroundResource(R.drawable.bg_message_filter_unselected);
            filterAll.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
            filterCritical.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
            filterHigh.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
            filterLow.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));

            v.setBackgroundResource(R.drawable.bg_message_filter_selected);
            ((TextView) v).setTextColor(ContextCompat.getColor(this, R.color.white));
            
            currentSeverityFilter = ((TextView) v).getText().toString();
            applyFilters();
        };

        filterAll.setOnClickListener(filterClickListener);
        filterCritical.setOnClickListener(filterClickListener);
        filterHigh.setOnClickListener(filterClickListener);
        filterLow.setOnClickListener(filterClickListener);
    }

    private void applyFilters() {
        filteredList.clear();
        for (Map<String, Object> item : hazardList) {
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String type = item.get("hazardType") != null ? ((String) item.get("hazardType")).toLowerCase() : "";
                String loc = item.get("location") != null ? ((String) item.get("location")).toLowerCase() : "";
                matchesSearch = type.contains(currentSearchQuery.toLowerCase()) || 
                               loc.contains(currentSearchQuery.toLowerCase());
            }

            boolean matchesSeverity = true;
            if (!currentSeverityFilter.equalsIgnoreCase("All")) {
                String severity = (String) item.get("severity");
                matchesSeverity = currentSeverityFilter.equalsIgnoreCase(severity);
            }

            if (matchesSearch && matchesSeverity) {
                filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void fetchHazards() {
        FirebaseFirestore.getInstance("hazora").collection("incidents")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    hazardList.clear();
                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("id", doc.getId()); // Store document ID
                            hazardList.add(data);
                        }
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private static class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
        private final List<Map<String, Object>> data;

        public GalleryAdapter(List<Map<String, Object>> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hazard_gallery, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = data.get(position);
            holder.tvType.setText((String) item.get("hazardType"));
            
            Object timestamp = item.get("timestamp");
            String timeStr = "Recent";
            if (timestamp instanceof Timestamp) {
                Date date = ((Timestamp) timestamp).toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                timeStr = sdf.format(date);
                holder.tvTimestamp.setText(timeStr);
            }

            // Severity Badge
            String severity = (String) item.get("severity");
            if (severity != null) {
                String type = (String) item.get("hazardType");
                holder.tvType.setText(String.format(Locale.getDefault(), "%s (%s)", type, severity));
            }

            String base64Data = (String) item.get("imageData");
            if (base64Data != null && !base64Data.isEmpty()) {
                byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.ivHazard.setImageBitmap(decodedByte);
            } else {
                String imageUrl = (String) item.get("imageUrl");
                Glide.with(holder.ivHazard.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.hazora_logo)
                        .into(holder.ivHazard);
            }

            final String finalTime = timeStr;
            holder.itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, IncidentDetailActivity.class);
                
                Incident inc = new Incident(
                    (String) item.get("id"),
                    (String) item.get("hazardType"),
                    (String) item.get("cameraSource"),
                    finalTime,
                    (String) item.get("location"),
                    (String) item.get("status"),
                    (String) item.get("severity"),
                    (String) item.get("description"),
                    (String) item.get("prevention")
                );
                intent.putExtra("incident_data", inc);
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivHazard;
            TextView tvType, tvTimestamp;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivHazard = itemView.findViewById(R.id.iv_hazard);
                tvType = itemView.findViewById(R.id.tv_type);
                tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            }
        }
    }
}
