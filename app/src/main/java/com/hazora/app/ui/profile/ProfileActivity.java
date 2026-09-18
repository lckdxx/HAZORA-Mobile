package com.hazora.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hazora.app.auth.GoogleSignInHelper;
import com.hazora.app.auth.SessionManager;
import com.hazora.app.R;
import com.hazora.app.ui.login.LoginActivity;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance("hazora");
    private DocumentSnapshot currentDoc;
    private String currentCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        View back = findViewById(R.id.tv_back);
        back.setOnClickListener(v -> finish());

        loadUserData();

        findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutConfirmation());
        
        View editBtn = findViewById(R.id.btn_edit_profile);
        if (editBtn != null) {
            editBtn.setOnClickListener(v -> showEditProfileDialog());
        }
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
                            currentCollection = "mobile_accounts";
                            currentDoc = queryDocumentSnapshots.getDocuments().get(0);
                            processProfileDoc(currentDoc);
                        } else {
                            // 2. Try searching by email/createdByEmail
                            db.collection("mobile_accounts")
                                    .whereEqualTo("createdByEmail", userEmail)
                                    .get()
                                    .addOnSuccessListener(snapshots -> {
                                        if (!snapshots.isEmpty()) {
                                            currentCollection = "mobile_accounts";
                                            currentDoc = snapshots.getDocuments().get(0);
                                            processProfileDoc(currentDoc);
                                        } else {
                                            // 3. Fallback to "users" collection
                                            db.collection("users")
                                                    .whereEqualTo("email", userEmail)
                                                    .get()
                                                    .addOnSuccessListener(userSnapshots -> {
                                                        if (!userSnapshots.isEmpty()) {
                                                            currentCollection = "users";
                                                            currentDoc = userSnapshots.getDocuments().get(0);
                                                            processProfileDoc(currentDoc);
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
            profileSite.setVisibility(View.VISIBLE);
            profileSite.setText(site);
            infoSite.setText(site);
        } else {
            profileSite.setVisibility(View.GONE);
            infoSite.setText("Not Assigned");
        }
    }

    private void showEditProfileDialog() {
        if (currentDoc == null) {
            Toast.makeText(this, "Profile data not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        EditText etName = dialogView.findViewById(R.id.et_edit_name);
        EditText etUsername = dialogView.findViewById(R.id.et_edit_username);
        EditText etEmail = dialogView.findViewById(R.id.et_edit_email);
        EditText etPassword = dialogView.findViewById(R.id.et_edit_password);

        etName.setText(currentDoc.getString("name"));
        etUsername.setText(currentDoc.getString("username"));
        etEmail.setText(currentDoc.getString("email") != null ? currentDoc.getString("email") : currentDoc.getString("createdByEmail"));
        etPassword.setText(currentDoc.getString("password"));

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    saveProfileChanges(
                            etName.getText().toString(),
                            etUsername.getText().toString(),
                            etEmail.getText().toString(),
                            etPassword.getText().toString()
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveProfileChanges(String name, String username, String email, String password) {
        if (currentDoc == null || currentCollection == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("username", username);
        
        // Handle different email field names in different collections
        if (currentDoc.contains("email")) {
            updates.put("email", email);
        } else if (currentDoc.contains("createdByEmail")) {
            updates.put("createdByEmail", email);
        }
        
        if (password != null && !password.isEmpty()) {
            updates.put("password", password);
        }

        // Show loading toast
        Toast.makeText(this, "Updating profile...", Toast.LENGTH_SHORT).show();

        db.collection(currentCollection).document(currentDoc.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    // Update session manager if email changed
                    if (sessionManager.getUserEmail().equals(currentDoc.getString("email")) || 
                        sessionManager.getUserEmail().equals(currentDoc.getString("createdByEmail"))) {
                        sessionManager.saveUserEmail(email);
                    }
                    loadUserData(); // Reload to reflect changes
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Update failed", e);
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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
