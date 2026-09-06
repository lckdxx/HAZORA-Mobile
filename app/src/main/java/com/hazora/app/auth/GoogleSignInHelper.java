package com.hazora.app.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/** Encapsulates Google Sign-In setup and result handling for authentication screens. */
public class GoogleSignInHelper {

    public static final int GOOGLE_SIGN_IN_REQUEST_CODE = 401;

    private final GoogleSignInClient googleSignInClient;

    public GoogleSignInHelper(Context context) {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(context, signInOptions);
    }

    public void signIn(Activity activity) {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE);
    }

    public void handleResult(Intent data, GoogleSignInCallback callback) {
        Task<GoogleSignInAccount> signInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = signInTask.getResult(ApiException.class);
            callback.onSuccess(account);
        } catch (ApiException exception) {
            callback.onError(exception.getStatusCode());
        }
    }

    public void signOut(OnCompleteListener<Void> completionListener) {
        googleSignInClient.signOut().addOnCompleteListener(completionListener);
    }

    public interface GoogleSignInCallback {
        void onSuccess(GoogleSignInAccount account);

        void onError(int statusCode);
    }
}
