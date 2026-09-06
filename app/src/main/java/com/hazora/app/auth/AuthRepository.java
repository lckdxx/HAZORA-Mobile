package com.hazora.app.auth;

import android.os.Handler;
import android.os.Looper;

/** Central placeholder for authentication operations before backend integration. */
public class AuthRepository {

    private static final long SIMULATED_AUTHENTICATION_DELAY_MS = 1500L;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void loginWithEmail(String email, String password, AuthenticationCallback callback) {
        mainHandler.postDelayed(callback::onSuccess, SIMULATED_AUTHENTICATION_DELAY_MS);
    }

    public interface AuthenticationCallback {
        void onSuccess();

        void onError(String errorMessage);
    }
}
