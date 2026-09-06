package com.hazora.app.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hazora.app.R;
import com.hazora.app.auth.AuthRepository;
import com.hazora.app.auth.GoogleSignInHelper;
import com.hazora.app.auth.SessionManager;
import com.hazora.app.ui.dashboard.DashboardActivity;
import com.hazora.app.ui.forgotpassword.ForgotPasswordActivity;

/** Coordinates login UI with the reusable authentication layer. */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private AuthRepository authRepository;
    private GoogleSignInHelper googleSignInHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        configureActions();
    }

    private void initializeViews() {
        emailInputLayout = findViewById(R.id.input_layout_email);
        passwordInputLayout = findViewById(R.id.input_layout_password);
        emailEditText = findViewById(R.id.edit_text_email);
        passwordEditText = findViewById(R.id.edit_text_password);
        loginButton = findViewById(R.id.button_login);
        authRepository = new AuthRepository();
        googleSignInHelper = new GoogleSignInHelper(this);
        sessionManager = new SessionManager(this);
    }

    private void configureActions() {
        MaterialButton googleSignInButton = findViewById(R.id.button_google_sign_in);
        loginButton.setOnClickListener(view -> attemptLogin());
        googleSignInButton.setOnClickListener(view -> googleSignInHelper.signIn(this));
        findViewById(R.id.text_forgot_password).setOnClickListener(view ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void attemptLogin() {
        boolean isEmailValid = validateEmail();
        boolean isPasswordValid = validatePassword();

        if (isEmailValid && isPasswordValid) {
            setLoginLoadingState(true);
            authRepository.loginWithEmail(getInputText(emailEditText), getInputText(passwordEditText),
                    new AuthRepository.AuthenticationCallback() {
                        @Override
                        public void onSuccess() {
                            sessionManager.saveLoginState(true);
                            sessionManager.saveUserEmail(getInputText(emailEditText));
                            openDashboard();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            setLoginLoadingState(false);
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void setLoginLoadingState(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        loginButton.setText(isLoading ? R.string.signing_in : R.string.sign_in);
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

    private void openDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    @Override
    @Deprecated
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GoogleSignInHelper.GOOGLE_SIGN_IN_REQUEST_CODE) {
            googleSignInHelper.handleResult(data, new GoogleSignInHelper.GoogleSignInCallback() {
                @Override
                public void onSuccess(GoogleSignInAccount account) {
                    sessionManager.saveLoginState(true);
                    sessionManager.saveUserEmail(account.getEmail() == null ? "" : account.getEmail());
                    openDashboard();
                }

                @Override
                public void onError(int statusCode) {
                    Toast.makeText(LoginActivity.this, R.string.google_sign_in_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
