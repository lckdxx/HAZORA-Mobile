package com.hazora.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        TextView tvUserName = findViewById(R.id.tv_user_name);
        LinearLayout navIncidents = findViewById(R.id.nav_incidents);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            if (email != null && email.contains("@")) {
                String nameFromEmail = email.split("@")[0];
                String formattedName = nameFromEmail.replace(".", " ").replace("_", " ");
                String[] words = formattedName.split(" ");
                StringBuilder sb = new StringBuilder();
                for (String word : words) {
                    if (word.length() > 0) {
                        sb.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1))
                          .append(" ");
                    }
                }
                tvUserName.setText(sb.toString().trim());
            } else {
                tvUserName.setText("User");
            }
        }

        // Set up navigation to Incidents Activity
        navIncidents.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, IncidentsActivity.class);
            startActivity(intent);
        });
    }
}
