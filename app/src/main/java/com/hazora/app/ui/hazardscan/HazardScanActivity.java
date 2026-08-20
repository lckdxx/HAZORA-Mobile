package com.hazora.app.ui.hazardscan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.hazora.app.R;
import com.hazora.app.ui.incidents.IncidentDetailActivity;

public class HazardScanActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private View analyzingLayout;
    private View resultCard;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hazard_scan);

        View back = findViewById(R.id.tv_back);
        back.setOnClickListener(v -> finish());

        analyzingLayout = findViewById(R.id.layout_analyzing);
        resultCard = findViewById(R.id.card_scan_result);
        startButton = findViewById(R.id.btn_start_scan);

        startButton.setBackgroundTintList(null);
        ((Button) findViewById(R.id.btn_view_incident)).setBackgroundTintList(null);
        ((Button) findViewById(R.id.btn_scan_again)).setBackgroundTintList(null);

        startButton.setOnClickListener(v -> startScan());
        findViewById(R.id.btn_view_incident).setOnClickListener(v -> openIncident());
        findViewById(R.id.btn_scan_again).setOnClickListener(v -> resetScan());
    }

    private void startScan() {
        startButton.setEnabled(false);
        startButton.setAlpha(0.55f);
        analyzingLayout.setVisibility(View.VISIBLE);
        resultCard.setVisibility(View.GONE);

        handler.postDelayed(() -> {
            analyzingLayout.setVisibility(View.GONE);
            resultCard.setVisibility(View.VISIBLE);
        }, 2400);
    }

    private void resetScan() {
        handler.removeCallbacksAndMessages(null);
        analyzingLayout.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);
        startButton.setEnabled(true);
        startButton.setAlpha(1f);
    }

    private void openIncident() {
        Intent intent = new Intent(this, IncidentDetailActivity.class);
        intent.putExtra("incident_index", 0);
        startActivity(intent);
    }
}
