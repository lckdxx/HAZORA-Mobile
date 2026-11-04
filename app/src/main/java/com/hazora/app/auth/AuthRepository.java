package com.hazora.app.auth;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
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

    public void loginWithGoogle(String idToken, AuthenticationCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null || !user.isEmailVerified()) {
                        firebaseAuth.signOut();
                        callback.onError("Could not verify the Google account. Please try again.");
                        return;
                    }

                    firestore.collection("users").document(user.getUid()).get()
                            .addOnSuccessListener(profile -> {
                                if (profile.exists()) {
                                    ensureMobileAccess(user, callback);
                                    return;
                                }
                                checkExistingMobileAccount(user, callback);
                            })
                            .addOnFailureListener(error -> failGoogleSignIn(
                                    callback,
                                    "Could not load your account. Check your connection and try again."
                            ));
                })
                .addOnFailureListener(error -> callback.onError(
                        "Google sign-in failed. Check your connection and try again."
                ));
    }

    private void checkExistingMobileAccount(FirebaseUser user, AuthenticationCallback callback) {
        firestore.collection("mobile_accounts")
                .whereEqualTo("authUid", user.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(uidMatches -> {
                    if (!uidMatches.isEmpty()) {
                        ensureMobileAccess(user, callback);
                        return;
                    }

                    String email = user.getEmail();
                    if (email == null || email.isEmpty()) {
                        createGooglePendingProfile(user, callback);
                        return;
                    }
                    firestore.collection("mobile_accounts")
                            .whereEqualTo("email", email)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(emailMatches -> {
                                if (!emailMatches.isEmpty()) {
                                    ensureMobileAccess(user, callback);
                                } else {
                                    createGooglePendingProfile(user, callback);
                                }
                            })
                            .addOnFailureListener(error -> failGoogleSignIn(
                                    callback,
                                    "Could not verify mobile access. Check your connection and try again."
                            ));
                })
                .addOnFailureListener(error -> failGoogleSignIn(
                        callback,
                        "Could not verify mobile access. Check your connection and try again."
                ));
    }

    private void createGooglePendingProfile(FirebaseUser user, AuthenticationCallback callback) {
        String email = user.getEmail();
        String displayName = user.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            if (email == null) {
                displayName = "Google user";
            } else {
                int emailSeparator = email.indexOf('@');
                displayName = emailSeparator > 0 ? email.substring(0, emailSeparator) : email;
            }
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("fullName", displayName.trim());
        profile.put("email", email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
        profile.put("phone", "");
        profile.put("role", null);
        profile.put("approvalStatus", "pending");
        profile.put("cameraIP", null);
        profile.put("cameras", Arrays.asList("", "", "", "", ""));
        profile.put("createdAt", FieldValue.serverTimestamp());
        profile.put("lastLogin", FieldValue.serverTimestamp());
        profile.put("registrationSource", "mobile");

        firestore.collection("users").document(user.getUid()).set(profile)
                .addOnSuccessListener(unused -> {
                    firebaseAuth.signOut();
                    callback.onError(
                            "Your Google account was submitted for admin approval. "
                                    + "You can sign in after mobile access is enabled."
                    );
                })
                .addOnFailureListener(error -> failGoogleSignIn(
                        callback,
                        "Could not submit your account for admin approval. Please try again."
                ));
    }

    private void failGoogleSignIn(AuthenticationCallback callback, String message) {
        firebaseAuth.signOut();
        callback.onError(message);
    }

    public void registerWithEmail(String email, String password, String fullName, String phone,
                                  SessionManager sessionManager,
                                  RegistrationCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        firebaseAuth.signOut();
                        callback.onError("Your account could not be created. Please try again.");
                        return;
                    }

                    Map<String, Object> profile = new HashMap<>();
                    profile.put("fullName", fullName.trim());
                    profile.put("email", email.trim().toLowerCase(Locale.ROOT));
                    profile.put("phone", phone.trim());
                    profile.put("role", null);
                    profile.put("approvalStatus", "pending");
                    profile.put("cameraIP", null);
                    profile.put("cameras", Arrays.asList("", "", "", "", ""));
                    profile.put("createdAt", FieldValue.serverTimestamp());
                    profile.put("lastLogin", FieldValue.serverTimestamp());
                    profile.put("registrationSource", "mobile");

                    firestore.collection("users").document(user.getUid()).set(profile)
                            .addOnSuccessListener(unused -> sendRegistrationVerification(
                                    user, sessionManager, callback))
                            .addOnFailureListener(error -> user.delete().addOnCompleteListener(deleteTask -> {
                                firebaseAuth.signOut();
                                callback.onError(deleteTask.isSuccessful()
                                        ? "Could not submit your registration for admin review. Please try again."
                                        : "Your account was created, but its registration could not be saved. "
                                                + "Please contact an administrator before trying again.");
                            }));
                })
                .addOnFailureListener(error -> {
                    if (error instanceof FirebaseAuthUserCollisionException) {
                        callback.onError("An account already exists for this email. Please sign in instead.");
                    } else if (error instanceof FirebaseAuthWeakPasswordException) {
                        callback.onError("Choose a stronger password with at least 8 characters.");
                    } else if (error instanceof FirebaseAuthInvalidCredentialsException) {
                        callback.onError("Enter a valid email address and try again.");
                    } else {
                        callback.onError("Account registration failed. Check your connection and try again.");
                    }
                });
    }

    private void sendRegistrationVerification(FirebaseUser user, SessionManager sessionManager,
                                              RegistrationCallback callback) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            firebaseAuth.signOut();
            if (task.isSuccessful()) {
                sessionManager.recordVerificationEmailSent();
                callback.onVerificationEmailSent();
                return;
            }

            callback.onError(
                    "Your registration was sent for admin review, but the verification email could not be sent. "
                            + "Please sign in to request another verification link."
            );
        });
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

    public interface RegistrationCallback {
        void onVerificationEmailSent();

        void onError(String errorMessage);
    }
}
