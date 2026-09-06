package com.hazora.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.hazora.app.auth.GoogleSignInHelper;
import com.hazora.app.auth.SessionManager;
import com.hazora.app.R;
import com.hazora.app.ui.login.LoginActivity;

import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance("hazora");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        View back = findViewById(R.id.tv_back);
        back.setOnClickListener(v -> finish());

        loadUserData();

        findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutConfirmation());
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userEmail = user != null ? user.getEmail() : sessionManager.getUserEmail();
        
        TextView emailValue = findViewById(R.id.tv_email_value);
        if (emailValue != null) emailValue.setText(userEmail);

        if (userEmail != null && !userEmail.isEmpty()) {
            // 1. Try searching by username (e.g., "MOB - 001")
            db.collection("mobile_accounts")
                    .whereEqualTo("username", userEmail)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            processProfileDoc(queryDocumentSnapshots.getDocuments().get(0));
                        } else {
                            // 2. Try searching by email/createdByEmail
                            db.collection("mobile_accounts")
                                    .whereEqualTo("createdByEmail", userEmail)
                                    .get()
                                    .addOnSuccessListener(snapshots -> {
                                        if (!snapshots.isEmpty()) {
                                            processProfileDoc(snapshots.getDocuments().get(0));
                                        } else {
                                            // 3. Fallback to "users" collection
                                            db.collection("users")
                                                    .whereEqualTo("email", userEmail)
                                                    .get()
                                                    .addOnSuccessListener(userSnapshots -> {
                                                        if (!userSnapshots.isEmpty()) {
                                                            processProfileDoc(userSnapshots.getDocuments().get(0));
                                                        }
                                                    });
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void processProfileDoc(DocumentSnapshot doc) {
        updateUI(doc.getString("name"), doc.getString("role"), doc.getString("site"));
    }

    private void updateUI(String name, String role, String site) {
        TextView profileName = findViewById(R.id.tv_profile_name);
        TextView profileRole = findViewById(R.id.tv_profile_role);
        TextView profileSite = findViewById(R.id.tv_profile_site);
        TextView infoName = findViewById(R.id.tv_info_name);
        TextView infoRole = findViewById(R.id.tv_info_role);
        TextView infoSite = findViewById(R.id.tv_info_site);

        if (name != null) {
            profileName.setText(name);
            infoName.setText(name);
        }
        if (role != null) {
            profileRole.setText(role);
            infoRole.setText(role);
        }
        if (site != null) {
            profileSite.setText(site);
            infoSite.setText(site);
        } else {
            profileSite.setVisibility(View.GONE);
            infoSite.setText("Not Assigned");
        }
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
