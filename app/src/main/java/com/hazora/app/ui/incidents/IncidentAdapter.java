package com.hazora.app.ui.incidents;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hazora.app.R;

import java.util.ArrayList;
import java.util.List;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.ViewHolder> {

    private final List<Incident> items = new ArrayList<>();
    private final Context context;

    public IncidentAdapter(Context context) {
        this.context = context;
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
        holder.cameraTime.setText(inc.getCameraId() + " • " + inc.getTime());
        holder.site.setText(inc.getSite());

        // Icon selection and circular background + tint
        if (inc.getTitle().toLowerCase().contains("hard hat")) {
            holder.icon.setImageResource(R.drawable.ic_hard_hat);
            holder.icon.setBackgroundResource(R.drawable.bg_icon_red);
            holder.icon.setColorFilter(Color.parseColor("#EF4444"));
        } else if (inc.getTitle().toLowerCase().contains("vest")) {
            holder.icon.setImageResource(R.drawable.ic_vest);
            holder.icon.setBackgroundResource(R.drawable.bg_icon_orange);
            holder.icon.setColorFilter(Color.parseColor("#F59E0B"));
        } else if (inc.getTitle().toLowerCase().contains("shoes")) {
            holder.icon.setImageResource(R.drawable.ic_shoes);
            holder.icon.setBackgroundResource(R.drawable.bg_icon_green);
            holder.icon.setColorFilter(Color.parseColor("#22C55E"));
        } else {
            holder.icon.setImageResource(R.drawable.ic_warning);
            holder.icon.setBackgroundResource(R.drawable.bg_icon_blue_light);
            holder.icon.setColorFilter(context.getResources().getColor(R.color.primary_blue));
        }

        // Status pill styling
        String status = inc.getStatus();
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
            bgColor = Color.parseColor("#FFFFFFFF");
            textColor = Color.parseColor("#1A1A1A");
        }

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dpToPx(12));
        holder.status.setBackground(gd);
        holder.status.setTextColor(textColor);
        holder.status.setText(status.toUpperCase());

        holder.itemView.setOnClickListener(v -> {
            // find index in repository
            int idx = IncidentRepository.getIncidents().indexOf(inc);
            Intent intent = new Intent(context, IncidentDetailActivity.class);
            intent.putExtra("incident_index", idx);
            context.startActivity(intent);
        });
    }

    private float dpToPx(int dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView cameraTime;
        TextView site;
        TextView status;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.img_icon);
            title = itemView.findViewById(R.id.tv_title);
            cameraTime = itemView.findViewById(R.id.tv_camera_time);
            site = itemView.findViewById(R.id.tv_site);
            status = itemView.findViewById(R.id.tv_status);
        }
    }
}
