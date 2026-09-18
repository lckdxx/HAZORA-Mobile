package com.hazora.app.ui.forgotpassword;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.hazora.app.R;

/** Captures a reset email and sends a real reset link via Firebase. */
public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputEditText emailEditText;
    private MaterialButton sendResetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInputLayout = findViewById(R.id.input_layout_company_email);
        emailEditText = findViewById(R.id.edit_text_company_email);
        sendResetButton = findViewById(R.id.button_send_reset_link);

        sendResetButton.setOnClickListener(view -> sendResetLink());
        findViewById(R.id.text_back_to_sign_in).setOnClickListener(view -> finish());
    }

    private void sendResetLink() {
        String email = getEmail();
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.error_email_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.error_email_invalid));
            return;
        }

        emailInputLayout.setError(null);
        setLoadingState(true);

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoadingState(false);
                    if (task.isSuccessful()) {
                        showSuccessDialog();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoadingState(boolean isLoading) {
        sendResetButton.setEnabled(!isLoading);
        sendResetButton.setText(isLoading ? "Sending..." : getString(R.string.send_reset_link));
    }

    private void showSuccessDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Email Sent")
                .setMessage("A password reset link has been sent to " + getEmail() + ". Please check your inbox.")
                .setPositiveButton("Back to Login", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private String getEmail() {
        return emailEditText.getText() == null ? "" : emailEditText.getText().toString().trim();
    }
}
