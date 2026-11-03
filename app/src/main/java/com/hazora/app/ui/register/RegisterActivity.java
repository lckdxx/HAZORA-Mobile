package com.hazora.app.ui.register;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hazora.app.R;
import com.hazora.app.auth.AuthRepository;
import com.hazora.app.auth.SessionManager;
import com.hazora.app.ui.login.LoginActivity;

/** Registers an email/password account and requires email verification before sign-in. */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputLayout fullNameInputLayout;
    private TextInputLayout phoneInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private TextInputEditText fullNameEditText;
    private TextInputEditText phoneEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private MaterialButton registerButton;
    private AuthRepository authRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        fullNameInputLayout = findViewById(R.id.input_layout_register_full_name);
        phoneInputLayout = findViewById(R.id.input_layout_register_phone);
        emailInputLayout = findViewById(R.id.input_layout_register_email);
        passwordInputLayout = findViewById(R.id.input_layout_register_password);
        confirmPasswordInputLayout = findViewById(R.id.input_layout_confirm_password);
        fullNameEditText = findViewById(R.id.edit_text_register_full_name);
        phoneEditText = findViewById(R.id.edit_text_register_phone);
        emailEditText = findViewById(R.id.edit_text_register_email);
        passwordEditText = findViewById(R.id.edit_text_register_password);
        confirmPasswordEditText = findViewById(R.id.edit_text_confirm_password);
        registerButton = findViewById(R.id.button_register);
        authRepository = new AuthRepository();
        sessionManager = new SessionManager(this);

        registerButton.setOnClickListener(view -> attemptRegistration());
        findViewById(R.id.text_back_to_sign_in).setOnClickListener(view -> finish());
    }

    private void attemptRegistration() {
        String fullName = getText(fullNameEditText).trim();
        String phone = getText(phoneEditText).trim();
        String email = getText(emailEditText).trim();
        String password = getText(passwordEditText);
        String confirmPassword = getText(confirmPasswordEditText);

        if (fullName.length() < 3 || fullName.length() > 100) {
            fullNameInputLayout.setError(getString(R.string.error_full_name_required));
            return;
        }
        fullNameInputLayout.setError(null);

        if (phone.isEmpty()) {
            phoneInputLayout.setError(getString(R.string.error_phone_required));
            return;
        }
        String normalizedPhone = phone.replaceAll("[\\s-]", "");
        if (!normalizedPhone.matches("^(\\+63|0)?9\\d{9}$")) {
            phoneInputLayout.setError(getString(R.string.error_phone_invalid));
            return;
        }
        phoneInputLayout.setError(null);

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.error_email_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.error_email_invalid));
            return;
        }
        emailInputLayout.setError(null);

        if (password.length() < 8) {
            passwordInputLayout.setError(getString(R.string.error_password_minimum));
            return;
        }
        passwordInputLayout.setError(null);

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInputLayout.setError(getString(R.string.error_confirm_password_required));
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError(getString(R.string.error_passwords_do_not_match));
            return;
        }
        confirmPasswordInputLayout.setError(null);

        setLoading(true);
        authRepository.registerWithEmail(
                email, password, fullName, phone, sessionManager,
                new AuthRepository.RegistrationCallback() {
            @Override
            public void onVerificationEmailSent() {
                setLoading(false);
                showVerificationInstructions(email);
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        registerButton.setEnabled(!isLoading);
        registerButton.setText(isLoading ? R.string.creating_account : R.string.create_account);
    }

    private String getText(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private void showVerificationInstructions(String email) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.verify_email_title)
                .setMessage(getString(R.string.verify_email_message, email))
                .setPositiveButton(R.string.back_to_sign_in, (dialog, which) -> {
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}
