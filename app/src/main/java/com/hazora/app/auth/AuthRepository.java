package com.hazora.app.auth;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

/** Firebase email/password authentication for mobile users. */
public class AuthRepository {

    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance("hazora");

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

                    ensureMobileAccess(user, callback);
                })
                .addOnFailureListener(error -> callback.onError("Sign-in failed. Check your email and password."));
    }

    public void ensureMobileAccess(FirebaseUser user, AuthenticationCallback callback) {
        firestore.collection("mobile_accounts")
                .whereEqualTo("authUid", user.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(uidMatches -> {
                    if (!uidMatches.isEmpty()) {
                        completeMobileSetup(uidMatches.getDocuments().get(0), user, callback);
                        return;
                    }
                    String email = user.getEmail();
                    if (email == null || email.isEmpty()) {
                        rejectMobileAccess(callback, "This account has no email address. Contact your administrator.");
                        return;
                    }
                    firestore.collection("mobile_accounts")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener(emailMatches -> {
                                DocumentSnapshot matchingAccount = null;
                                for (DocumentSnapshot candidate : emailMatches.getDocuments()) {
                                    String linkedUid = candidate.getString("authUid");
                                    if (linkedUid == null || linkedUid.isEmpty() || linkedUid.equals(user.getUid())) {
                                        matchingAccount = candidate;
                                        break;
                                    }
                                }
                                if (matchingAccount == null) {
                                    rejectMobileAccess(callback, "Mobile app access is not enabled for this account. Contact your administrator.");
                                    return;
                                }
                                completeMobileSetup(matchingAccount, user, callback);
                            })
                            .addOnFailureListener(error -> rejectMobileAccess(
                                    callback,
                                    "Could not verify mobile access. Check your connection and try again."
                            ));
                })
                .addOnFailureListener(error -> rejectMobileAccess(
                        callback,
                        "Could not verify mobile access. Check your connection and try again."
                ));
    }

    private void completeMobileSetup(DocumentSnapshot account, FirebaseUser user,
                                     AuthenticationCallback callback) {
        String linkedUid = account.getString("authUid");
        if (linkedUid != null && !linkedUid.equals(user.getUid())) {
            rejectMobileAccess(callback, "This mobile account is linked to another login. Contact your administrator.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        if (linkedUid == null || linkedUid.isEmpty()) {
            updates.put("authUid", user.getUid());
            updates.put("linkedWebsiteUid", user.getUid());
        }
        if ("setup_required".equals(account.getString("mobileSetupStatus"))) {
            updates.put("mobileSetupStatus", "active");
            updates.put("mobileSetupCompletedAt", FieldValue.serverTimestamp());
        }

        if (updates.isEmpty()) {
            callback.onSuccess(user);
            return;
        }

        account.getReference().update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess(user))
                .addOnFailureListener(error -> rejectMobileAccess(
                        callback,
                        "Could not finish mobile account setup. Please try again."
                ));
    }

    private void rejectMobileAccess(AuthenticationCallback callback, String message) {
        firebaseAuth.signOut();
        callback.onError(message);
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
