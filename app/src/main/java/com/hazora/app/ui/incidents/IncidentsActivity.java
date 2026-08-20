package com.hazora.app.ui.incidents;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hazora.app.R;

import java.util.ArrayList;
import java.util.List;

public class IncidentsActivity extends AppCompatActivity {

    private IncidentAdapter adapter;
    private List<Incident> allIncidents = new ArrayList<>();
    private String selectedFilter = "All";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incidents);

        View back = findViewById(R.id.tv_back);
        if (back != null) back.setOnClickListener(v -> finish());

        adapter = new IncidentAdapter(this);
        RecyclerView rv = findViewById(R.id.rv_incidents);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // load mock data
        allIncidents.clear();
        allIncidents.addAll(IncidentRepository.getIncidents());

        setupFilters();
        applyFilter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // refresh in case detail updated status
        allIncidents.clear();
        allIncidents.addAll(IncidentRepository.getIncidents());
        applyFilter();
    }

    private void setupFilters() {
        TextView all = findViewById(R.id.filter_all);
        TextView ne = findViewById(R.id.filter_new);
        TextView ack = findViewById(R.id.filter_ack);
        TextView res = findViewById(R.id.filter_resolved);

        View.OnClickListener click = v -> {
            selectedFilter = ((TextView) v).getText().toString();
            // update visual selection
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

        // default selection All
        all.setBackgroundResource(R.drawable.bg_message_filter_selected);
        all.setTextColor(getResources().getColor(R.color.white));
    }

    private void applyFilter() {
        List<Incident> filtered = new ArrayList<>();
        if ("All".equalsIgnoreCase(selectedFilter)) {
            filtered.addAll(allIncidents);
        } else if ("New".equalsIgnoreCase(selectedFilter)) {
            for (Incident i : allIncidents) if ("New".equalsIgnoreCase(i.getStatus())) filtered.add(i);
        } else if ("Acknowledged".equalsIgnoreCase(selectedFilter)) {
            for (Incident i : allIncidents) if ("Acknowledged".equalsIgnoreCase(i.getStatus())) filtered.add(i);
        } else if ("Resolved".equalsIgnoreCase(selectedFilter)) {
            for (Incident i : allIncidents) if ("Resolved".equalsIgnoreCase(i.getStatus())) filtered.add(i);
        }

        adapter.setItems(filtered);

        View empty = findViewById(R.id.empty_state);
        if (filtered.isEmpty()) empty.setVisibility(View.VISIBLE); else empty.setVisibility(View.GONE);
    }
}
