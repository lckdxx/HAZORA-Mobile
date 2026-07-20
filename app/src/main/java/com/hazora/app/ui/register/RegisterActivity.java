package com.hazora.app.ui.register;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hazora.app.R;

/** Collects local registration details until backend registration is introduced. */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout fullNameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private TextInputEditText fullNameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        configureActions();
    }

    private void initializeViews() {
        fullNameInputLayout = findViewById(R.id.input_layout_full_name);
        emailInputLayout = findViewById(R.id.input_layout_register_email);
        passwordInputLayout = findViewById(R.id.input_layout_register_password);
        confirmPasswordInputLayout = findViewById(R.id.input_layout_confirm_password);
        fullNameEditText = findViewById(R.id.edit_text_full_name);
        emailEditText = findViewById(R.id.edit_text_register_email);
        passwordEditText = findViewById(R.id.edit_text_register_password);
        confirmPasswordEditText = findViewById(R.id.edit_text_confirm_password);
    }

    private void configureActions() {
        MaterialButton createAccountButton = findViewById(R.id.button_create_account);
        TextView signInTextView = findViewById(R.id.text_sign_in);

        createAccountButton.setOnClickListener(view -> attemptRegistration());
        signInTextView.setOnClickListener(view -> finish());
    }

    private void attemptRegistration() {
        boolean isFullNameValid = validateFullName();
        boolean isEmailValid = validateEmail();
        boolean isPasswordValid = validatePassword();
        boolean isConfirmationValid = validatePasswordConfirmation();

        if (isFullNameValid && isEmailValid && isPasswordValid && isConfirmationValid) {
            Toast.makeText(this, R.string.registration_backend_message, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateFullName() {
        if (TextUtils.isEmpty(getInputText(fullNameEditText))) {
            fullNameInputLayout.setError(getString(R.string.error_full_name_required));
            return false;
        }
        fullNameInputLayout.setError(null);
        return true;
    }

    private boolean validateEmail() {
        String email = getInputText(emailEditText);
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.error_email_required));
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.error_email_invalid));
            return false;
        }
        emailInputLayout.setError(null);
        return true;
    }

    private boolean validatePassword() {
        String password = getInputText(passwordEditText);
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError(getString(R.string.error_password_required));
            return false;
        }
        if (password.length() < 8) {
            passwordInputLayout.setError(getString(R.string.error_password_minimum));
            return false;
        }
        passwordInputLayout.setError(null);
        return true;
    }

    private boolean validatePasswordConfirmation() {
        String password = getInputText(passwordEditText);
        String confirmation = getInputText(confirmPasswordEditText);
        if (TextUtils.isEmpty(confirmation)) {
            confirmPasswordInputLayout.setError(getString(R.string.error_confirm_password_required));
            return false;
        }
        if (!confirmation.equals(password)) {
            confirmPasswordInputLayout.setError(getString(R.string.error_passwords_do_not_match));
            return false;
        }
        confirmPasswordInputLayout.setError(null);
        return true;
    }

    private String getInputText(TextInputEditText inputEditText) {
        return inputEditText.getText() == null ? "" : inputEditText.getText().toString().trim();
    }
}
