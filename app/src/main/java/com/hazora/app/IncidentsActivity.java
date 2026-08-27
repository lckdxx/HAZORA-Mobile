package com.hazora.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class IncidentsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incidents);
        
        // Default behavior: ll_empty_state is visible by default in XML
    }
}
