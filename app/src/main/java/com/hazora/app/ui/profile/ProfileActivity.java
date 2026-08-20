package com.hazora.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hazora.app.auth.GoogleSignInHelper;
import com.hazora.app.auth.SessionManager;
import com.hazora.app.R;
import com.hazora.app.ui.login.LoginActivity;

import android.widget.TextView;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        View back = findViewById(R.id.tv_back);
        back.setOnClickListener(v -> finish());

        String email = sessionManager.getUserEmail();
        TextView emailValue = findViewById(R.id.tv_email_value);
        emailValue.setText(email == null || email.trim().isEmpty() ? "Email not available" : email);

        findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutConfirmation());
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log out?")
                .setMessage("Are you sure you want to log out of HAZORA?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log Out", (dialog, which) -> performLogout())
                .show();
    }

    private void performLogout() {
        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
            new GoogleSignInHelper(this).signOut(task -> completeLogout());
        } else {
            completeLogout();
        }
    }

    private void completeLogout() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
