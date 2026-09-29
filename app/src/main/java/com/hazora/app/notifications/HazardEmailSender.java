package com.hazora.app.notifications;

import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Sends a verified Firebase user's hazard reminder to the shared Gmail endpoint. */
public final class HazardEmailSender {

    private static final String TAG = "HazardEmailSender";
    private static final String ENDPOINT = "https://script.google.com/macros/s/AKfycbxpNXvhUKG-3dbYR1PesewoD-V541FCETWNo6FR9c4nOXOd2kBNKD2E93IVzcfwrMU6/exec";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private HazardEmailSender() {}

    public static void send(FirebaseUser user, String hazardType, String severity,
                            String site, String cameraSource) {
        if (user == null || !user.isEmailVerified()) return;

        user.getIdToken(false)
                .addOnSuccessListener(token -> EXECUTOR.execute(() -> postAlert(
                        token.getToken(), hazardType, severity, site, cameraSource)))
                .addOnFailureListener(error -> Log.w(TAG, "Could not get Firebase token for alert", error));
    }

    private static void postAlert(String idToken, String hazardType, String severity,
                                  String site, String cameraSource) {
        HttpURLConnection connection = null;
        try {
            JSONObject payload = new JSONObject();
            payload.put("idToken", idToken);
            payload.put("hazardType", hazardType);
            payload.put("severity", severity);
            payload.put("site", site);
            payload.put("cameraSource", cameraSource);

            connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");

            byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK && status != HttpURLConnection.HTTP_MOVED_TEMP
                    && status != HttpURLConnection.HTTP_SEE_OTHER) {
                Log.w(TAG, "Hazard email endpoint returned HTTP " + status);
            }
        } catch (Exception error) {
            Log.w(TAG, "Hazard email request failed", error);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}