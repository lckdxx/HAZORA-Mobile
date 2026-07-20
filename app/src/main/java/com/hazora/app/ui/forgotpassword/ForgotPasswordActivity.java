package com.hazora.app.ui.forgotpassword;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hazora.app.R;

/** Captures a reset email until password-reset backend support is available. */
public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputEditText emailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInputLayout = findViewById(R.id.input_layout_company_email);
        emailEditText = findViewById(R.id.edit_text_company_email);
        MaterialButton sendResetButton = findViewById(R.id.button_send_reset_link);

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
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.password_reset_title)
                .setMessage(R.string.password_reset_message)
                .setPositiveButton(R.string.back_to_sign_in, (dialog, which) -> finish())
                .show();
    }

    private String getEmail() {
        return emailEditText.getText() == null ? "" : emailEditText.getText().toString().trim();
    }
}
