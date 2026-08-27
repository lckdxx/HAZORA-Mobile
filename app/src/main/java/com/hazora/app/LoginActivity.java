package com.hazora.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startDashboard();
        }

        MaterialButton btnGoogle = findViewById(R.id.btn_google_signin);
        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        MaterialButton btnSignIn = findViewById(R.id.btn_sign_in);
        btnSignIn.setOnClickListener(v -> {
            // Placeholder for Email/Password sign in
            Toast.makeText(this, "Email sign-in coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("94553925828-slbf7dcvm5kd8rgokp210sh7qg3s9jkm.apps.googleusercontent.com")
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();

        credentialManager.getCredentialAsync(this, request, null, executor, new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
            @Override
            public void onResult(GetCredentialResponse result) {
                handleSignIn(result.getCredential());
            }

            @Override
            public void onError(@NonNull GetCredentialException e) {
                Log.e(TAG, "Credential Manager Error", e);
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Google Sign-in failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void handleSignIn(Credential credential) {
        if (credential instanceof GoogleIdTokenCredential) {
            GoogleIdTokenCredential googleIdTokenCredential = (GoogleIdTokenCredential) credential;
            String idToken = googleIdTokenCredential.getIdToken();
            firebaseAuthWithGoogle(idToken);
        } else {
            Log.e(TAG, "Unexpected credential type");
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startDashboard();
                    } else {
                        Log.w(TAG, "Firebase Auth Failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}
