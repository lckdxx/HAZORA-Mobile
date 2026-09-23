package com.hazora.app.ui.incidents;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hazora.app.R;

import java.util.ArrayList;
import java.util.List;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.ViewHolder> {

    public interface OnIncidentClickListener {
        void onIncidentClick(Incident incident);
        void onIncidentLongClick(Incident incident);
    }

    private final List<Incident> items = new ArrayList<>();
    private final Context context;
    private final OnIncidentClickListener listener;

    public IncidentAdapter(Context context, OnIncidentClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<Incident> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_incident, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Incident inc = items.get(position);
        holder.title.setText(inc.getTitle());
        holder.cameraTime.setText(inc.getTime());
        holder.camera.setText(inc.getCameraId());
        holder.site.setText(inc.getSite());

        // Optional Firebase image URL
        String imgUrl = inc.getImageUrl();
        if (imgUrl != null && !imgUrl.trim().isEmpty()) {
            holder.photo.setVisibility(View.VISIBLE);
            Glide.with(context).load(imgUrl).into(holder.photo);
        } else {
            holder.photo.setVisibility(View.GONE);
        }

        // Category Icon selection & circular tint
        String lowerTitle = inc.getTitle().toLowerCase();
        if (lowerTitle.contains("hat") || lowerTitle.contains("helmet")) {
            holder.icon.setImageResource(R.drawable.ic_hard_hat);
            holder.icon.setBackgroundResource(R.drawable.bg_badge_red_light);
            holder.icon.setColorFilter(Color.parseColor("#EF4444"));
        } else if (lowerTitle.contains("vest")) {
            holder.icon.setImageResource(R.drawable.ic_vest);
            holder.icon.setBackgroundResource(R.drawable.bg_badge_orange_light);
            holder.icon.setColorFilter(Color.parseColor("#F59E0B"));
        } else if (lowerTitle.contains("shoe") || lowerTitle.contains("boot")) {
            holder.icon.setImageResource(R.drawable.ic_shoes);
            holder.icon.setBackgroundResource(R.drawable.bg_circle_light_purple);
            holder.icon.setColorFilter(Color.parseColor("#8B5CF6"));
        } else {
            holder.icon.setImageResource(R.drawable.ic_warning);
            holder.icon.setBackgroundResource(R.drawable.bg_badge_red_light);
            holder.icon.setColorFilter(Color.parseColor("#EF4444"));
        }

        // Status pill styling
        String status = inc.getStatus();
        if ("New".equalsIgnoreCase(status)) {
            holder.status.setText("New");
            holder.status.setTextColor(Color.parseColor("#EF4444"));
            holder.status.setBackgroundResource(R.drawable.bg_badge_red_light);
        } else if ("Acknowledged".equalsIgnoreCase(status)) {
            holder.status.setText("Acknowledged");
            holder.status.setTextColor(Color.parseColor("#D97706"));
            holder.status.setBackgroundResource(R.drawable.bg_badge_orange_light);
        } else if ("Resolved".equalsIgnoreCase(status) || "Done".equalsIgnoreCase(status)) {
            holder.status.setText("Resolved");
            holder.status.setTextColor(Color.parseColor("#16A34A"));
            holder.status.setBackgroundResource(R.drawable.bg_badge_green_light);
        } else {
            holder.status.setText(status);
            holder.status.setTextColor(Color.parseColor("#0F172A"));
            holder.status.setBackgroundResource(R.drawable.bg_badge_orange_light);
        }

        // Severity pill & dot styling
        String severity = inc.getSeverity();
        if ("Critical".equalsIgnoreCase(severity)) {
            holder.severity.setText("Critical");
            holder.severity.setTextColor(Color.parseColor("#EF4444"));
            holder.severityDot.setImageResource(R.drawable.ic_dot_red);
            holder.layoutSeverity.setBackgroundResource(R.drawable.bg_badge_severity_critical);
        } else if ("High".equalsIgnoreCase(severity)) {
            holder.severity.setText("High");
            holder.severity.setTextColor(Color.parseColor("#D97706"));
            holder.severityDot.setImageResource(R.drawable.ic_dot_orange);
            holder.layoutSeverity.setBackgroundResource(R.drawable.bg_badge_severity_high);
        } else if ("Medium".equalsIgnoreCase(severity)) {
            holder.severity.setText("Medium");
            holder.severity.setTextColor(Color.parseColor("#CA8A04"));
            holder.severityDot.setImageResource(R.drawable.ic_dot_yellow);
            holder.layoutSeverity.setBackgroundResource(R.drawable.bg_badge_severity_medium);
        } else {
            holder.severity.setText("Low");
            holder.severity.setTextColor(Color.parseColor("#16A34A"));
            holder.severityDot.setImageResource(R.drawable.ic_dot_green);
            holder.layoutSeverity.setBackgroundResource(R.drawable.bg_badge_severity_low);
        }

        holder.itemView.setOnClickListener(v -> listener.onIncidentClick(inc));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onIncidentLongClick(inc);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        ImageView icon;
        TextView title;
        TextView cameraTime;
        TextView camera;
        TextView site;
        TextView status;
        View layoutSeverity;
        ImageView severityDot;
        TextView severity;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.img_incident_photo);
            icon = itemView.findViewById(R.id.img_icon);
            title = itemView.findViewById(R.id.tv_title);
            cameraTime = itemView.findViewById(R.id.tv_camera_time);
            camera = itemView.findViewById(R.id.tv_camera);
            site = itemView.findViewById(R.id.tv_site);
            status = itemView.findViewById(R.id.tv_status);
            layoutSeverity = itemView.findViewById(R.id.layout_severity);
            severityDot = itemView.findViewById(R.id.img_severity_dot);
            severity = itemView.findViewById(R.id.tv_severity);
        }
    }
}
