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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hazard_gallery);

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rv_gallery);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GalleryAdapter(hazardList);
        recyclerView.setAdapter(adapter);

        fetchHazards();
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
                    adapter.notifyDataSetChanged();
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
                
                // Create a temporary Incident object to pass data
                Incident inc = new Incident(
                    (String) item.get("id"), // We should store ID in map if available
                    (String) item.get("hazardType"),
                    (String) item.get("cameraSource"),
                    finalTime,
                    (String) item.get("location"),
                    (String) item.get("status"),
                    "High",
                    "AI detected safety violation"
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
