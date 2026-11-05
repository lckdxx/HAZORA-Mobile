package com.hazora.app.notifications;

import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Requests email-delivery tests through the shared Gmail endpoint. */
public final class HazardEmailSender {

    private static final String TAG = "HazardEmailSender";
    private static final String ENDPOINT = "https://script.google.com/macros/s/AKfycbx6dUqAuLZiBZ3yuVk5RuDMvGkxk-XSRsZH5TN9gSEsLYN7ruqA44w-f42TbTWpABzy/exec";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private HazardEmailSender() {}

    public interface TestResultCallback {
        void onResult(String reason);
    }

    public static void sendTest(FirebaseUser user, TestResultCallback callback) {
        if (user == null || !user.isEmailVerified() || user.getEmail() == null) {
            callback.onResult("verified_account_required");
            return;
        }

        user.getIdToken(false)
                .addOnSuccessListener(token -> EXECUTOR.execute(() -> {
                    String reason = postTest(token.getToken(), user.getEmail());
                    callback.onResult(reason);
                }))
                .addOnFailureListener(error -> {
                    Log.w(TAG, "Could not get Firebase token for email test", error);
                    callback.onResult("request_failed");
                });
    }

    private static String postTest(String idToken, String testRecipient) {
        HttpURLConnection connection = null;
        try {
            JSONObject payload = new JSONObject();
            payload.put("idToken", idToken);
            payload.put("action", "test_hazard_email");
            payload.put("testRecipient", testRecipient);

            connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");

            byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            int status = connection.getResponseCode();
            if (status < HttpURLConnection.HTTP_OK || status >= 300) {
                Log.w(TAG, "Hazard email test endpoint returned HTTP " + status);
                return "request_failed";
            }

            ByteArrayOutputStream responseBytes = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    responseBytes.write(buffer, 0, bytesRead);
                }
            }
            String responseBody = new String(responseBytes.toByteArray(), StandardCharsets.UTF_8);
            JSONObject response = new JSONObject(responseBody);
            return response.optBoolean("sent") ? "sent" : response.optString("reason", "request_failed");
        } catch (Exception error) {
            Log.w(TAG, "Hazard email test request failed", error);
            return "request_failed";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}