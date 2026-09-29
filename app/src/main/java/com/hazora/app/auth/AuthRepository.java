package com.hazora.app.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Firebase email/password authentication for mobile users. */
public class AuthRepository {

    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    public void loginWithEmail(String email, String password, SessionManager sessionManager,
                               AuthenticationCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        firebaseAuth.signOut();
                        callback.onError("Unable to sign in. Please try again.");
                        return;
                    }

                    if (!user.isEmailVerified()) {
                        sendVerificationIfAllowed(user, sessionManager, callback);
                        return;
                    }

                    callback.onSuccess(user);
                })
                .addOnFailureListener(error -> callback.onError("Sign-in failed. Check your email and password."));
    }

    private void sendVerificationIfAllowed(FirebaseUser user, SessionManager sessionManager,
                                           AuthenticationCallback callback) {
        if (!sessionManager.canSendVerificationEmail()) {
            firebaseAuth.signOut();
            callback.onError("Verify your email using the link already sent, then sign in again.");
            return;
        }

        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) sessionManager.recordVerificationEmailSent();
            firebaseAuth.signOut();
            callback.onError(task.isSuccessful()
                    ? "A verification link was sent. Verify your email, then sign in again."
                    : "Your email is unverified. Contact your administrator for a verification link.");
        });
    }

    public interface AuthenticationCallback {
        void onSuccess(FirebaseUser user);

        void onError(String errorMessage);
    }
}
