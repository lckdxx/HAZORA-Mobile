package com.hazora.app.ui.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hazora.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Entry point for the authentication feature. */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        configureLoginButton();
    }

    private void initializeViews() {
        emailInputLayout = findViewById(R.id.input_layout_email);
        passwordInputLayout = findViewById(R.id.input_layout_password);
        emailEditText = findViewById(R.id.edit_text_email);
        passwordEditText = findViewById(R.id.edit_text_password);
    }

    private void configureLoginButton() {
        MaterialButton loginButton = findViewById(R.id.button_login);
        loginButton.setOnClickListener(view -> attemptLogin());
    }

    private void attemptLogin() {
        boolean isEmailValid = validateEmail();
        boolean isPasswordValid = validatePassword();

        if (isEmailValid && isPasswordValid) {
            Toast.makeText(this, R.string.authentication_phase_four_message, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateEmail() {
        String email = getInputText(emailEditText);
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.error_email_required));
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

        passwordInputLayout.setError(null);
        return true;
    }

    private String getInputText(TextInputEditText inputEditText) {
        return inputEditText.getText() == null ? "" : inputEditText.getText().toString().trim();
    }
}
