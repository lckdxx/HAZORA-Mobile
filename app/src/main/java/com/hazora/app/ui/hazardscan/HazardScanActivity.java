package com.hazora.app.ui.hazardscan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hazora.app.R;
import com.hazora.app.ui.incidents.Incident;
import com.hazora.app.ui.incidents.IncidentDetailActivity;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HazardScanActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private View analyzingLayout;
    private View resultCard;
    private Button startButton;
    private PreviewView previewView;
    private View cameraPlaceholder;
    private Button captureButton;
    private ImageView ivCapturedResult;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private HazardDetector hazardDetector;
    private boolean isScanning = false;
    private String userAssignedSite = "Not assigned location site";
    private Incident lastDetectedIncident;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hazard_scan);

        cameraExecutor = Executors.newSingleThreadExecutor();
        hazardDetector = new HazardDetector(this);
        hazardDetector.setScanMode(HazardDetector.ScanMode.HELMET_ONLY); // Default to Helmet-Only for Headshot & Selfie scans

        View back = findViewById(R.id.tv_back);
        back.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tv_title);
        if (tvTitle != null) {
            tvTitle.setText("AI Hazard Scan (Helmet Check)");
            tvTitle.setOnClickListener(v -> {
                if (hazardDetector.isRequireVest()) {
                    hazardDetector.setScanMode(HazardDetector.ScanMode.HELMET_ONLY);
                    tvTitle.setText("AI Hazard Scan (Helmet Check)");
                    Toast.makeText(this, "Scan Mode: Helmet Only Check (Headshot)", Toast.LENGTH_SHORT).show();
                } else {
                    hazardDetector.setScanMode(HazardDetector.ScanMode.FULL_BODY);
                    tvTitle.setText("AI Hazard Scan (Full PPE Check)");
                    Toast.makeText(this, "Scan Mode: Full Body PPE Check (Helmet, Vest & Shoes)", Toast.LENGTH_SHORT).show();
                }
            });
        }

        analyzingLayout = findViewById(R.id.layout_analyzing);
        resultCard = findViewById(R.id.card_scan_result);
        startButton = findViewById(R.id.btn_start_scan);
        previewView = findViewById(R.id.previewView);
        if (previewView != null) {
            previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        }
        cameraPlaceholder = findViewById(R.id.camera_placeholder);
        captureButton = findViewById(R.id.btn_capture);
        ivCapturedResult = findViewById(R.id.iv_captured_result);

        startButton.setBackgroundTintList(null);
        captureButton.setBackgroundTintList(null);
        ((Button) findViewById(R.id.btn_view_gallery)).setBackgroundTintList(null);

        startButton.setOnClickListener(v -> {
            if (allPermissionsGranted()) {
                openCameraAndPrepareCapture();
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }
        });

        captureButton.setOnClickListener(v -> captureAndScan());

        findViewById(R.id.btn_view_gallery).setOnClickListener(v -> {
            startActivity(new Intent(this, HazardGalleryActivity.class));
        });

        findViewById(R.id.btn_view_incident).setOnClickListener(v -> openIncident());
        findViewById(R.id.btn_scan_again).setOnClickListener(v -> resetScan());

        loadUserSite();
    }

    private void loadUserSite() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userEmail = user.getEmail();
        if (userEmail == null || userEmail.isEmpty()) return;

        FirebaseFirestore.getInstance("hazora").collection("mobile_accounts")
                .whereEqualTo("username", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String site = queryDocumentSnapshots.getDocuments().get(0).getString("site");
                        if (site != null && !site.isEmpty()) userAssignedSite = site;
                    } else {
                        FirebaseFirestore.getInstance("hazora").collection("mobile_accounts")
                                .whereEqualTo("createdByEmail", userEmail)
                                .get()
                                .addOnSuccessListener(snapshots -> {
                                    if (!snapshots.isEmpty()) {
                                        String site = snapshots.getDocuments().get(0).getString("site");
                                        if (site != null && !site.isEmpty()) userAssignedSite = site;
                                    } else {
                                        FirebaseFirestore.getInstance("hazora").collection("users")
                                                .whereEqualTo("email", userEmail)
                                                .get()
                                                .addOnSuccessListener(userSnapshots -> {
                                                    if (!userSnapshots.isEmpty()) {
                                                        String site = userSnapshots.getDocuments().get(0).getString("site");
                                                        if (site != null && !site.isEmpty()) userAssignedSite = site;
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                openCameraAndPrepareCapture();
            } else {
                Toast.makeText(this, "Camera permission is required for AI scan.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCameraAndPrepareCapture() {
        if (cameraProvider == null) {
            startCamera();
            startButton.setText("Camera Active");
            startButton.setEnabled(false);
            captureButton.setVisibility(View.VISIBLE);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);

                cameraPlaceholder.setVisibility(View.GONE);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureAndScan() {
        if (isScanning) return;
        
        Bitmap bitmap = previewView.getBitmap();
        if (bitmap == null) {
            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            return;
        }

        isScanning = true;
        analyzingLayout.setVisibility(View.VISIBLE);
        resultCard.setVisibility(View.GONE);

        cameraExecutor.execute(() -> {
            // Run Local TFLite/YOLOv8 Detection
            List<HazardDetector.DetectionResult> detections = hazardDetector.detect(bitmap);
            
            runOnUiThread(() -> {
                analyzingLayout.setVisibility(View.GONE);
                
                // UNCONDITIONAL: Always show the result card
                HazardDetector.DetectionResult bestMatch;
                if (!detections.isEmpty()) {
                    bestMatch = detections.get(0);
                } else {
                    // Safety Fallback: Should not happen with new Detector logic
                    bestMatch = new HazardDetector.DetectionResult("Violation: No PPE Detected", "Action: Equip all required PPE.", 0f, false, 
                        new Rect(0,0,bitmap.getWidth(), bitmap.getHeight()),
                        HazardDetector.DetectionType.PERSON, "Critical");
                }
                showDetectionResult(bestMatch, bitmap);
            });
        });
    }

    private void showDetectionResult(HazardDetector.DetectionResult result, Bitmap bitmap) {
        isScanning = false;
        resultCard.setVisibility(View.VISIBLE);
        captureButton.setVisibility(View.VISIBLE);

        ivCapturedResult.setImageBitmap(bitmap);

        TextView tvTitle = findViewById(R.id.tv_hazard_title);
        TextView tvDetails = findViewById(R.id.tv_hazard_details);
        View statusDot = findViewById(R.id.tv_hazard_detected_dot);
        TextView statusText = findViewById(R.id.tv_hazard_detected_label);
        
        if (tvTitle != null) tvTitle.setText(result.label);
        if (tvDetails != null) {
            // Show the smart solution/description in the details area
            tvDetails.setText(result.description);
        }

        if (statusText != null && statusDot != null) {
            if (result.isSecure) {
                statusText.setText("AREA SECURE");
                statusText.setTextColor(ContextCompat.getColor(this, R.color.hazora_success));
                statusDot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.hazora_success)));
            } else {
                statusText.setText("HAZARD DETECTED");
                statusText.setTextColor(ContextCompat.getColor(this, R.color.hazora_danger));
                statusDot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.hazora_danger)));
            }
        }

        // 1. Resize and Compress for Firestore
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 480, (int)(480 * ((float)bitmap.getHeight()/bitmap.getWidth())), true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 60, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        // 2. Save directly to Firestore
        saveIncidentToFirestore(result, base64Image);
    }

    private void saveIncidentToFirestore(HazardDetector.DetectionResult result, String base64Data) {
        FirebaseFirestore db = FirebaseFirestore.getInstance("hazora");
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : "Josh";

        Map<String, Object> incident = new HashMap<>();
        incident.put("userId", userId);
        incident.put("hazardType", result.label);
        incident.put("confidence", result.confidence);
        incident.put("imageData", base64Data);
        incident.put("timestamp", Timestamp.now());
        incident.put("status", result.isSecure ? "Resolved" : "New");
        incident.put("severity", result.severity);
        incident.put("location", userAssignedSite);
        incident.put("cameraSource", "Mobile Capture");
        incident.put("description", "Detected via on-device AI model.");
        incident.put("prevention", "Follow standard safety protocols.");

        db.collection("incidents")
                .add(incident)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Incident saved to database", Toast.LENGTH_SHORT).show();
                    
                    String time = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(new Date());
                    lastDetectedIncident = new Incident(
                            doc.getId(),
                            result.label,
                            "Mobile Capture",
                            time,
                            userAssignedSite,
                            result.isSecure ? "Resolved" : "New",
                            result.severity,
                            "Detected via on-device AI model.",
                            "Follow standard safety protocols."
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e("HazardScan", "Error saving", e);
                    Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Removed old uploadToFirebase method as Storage is not available on your plan

    private void resetScan() {
        isScanning = false;
        handler.removeCallbacksAndMessages(null);
        analyzingLayout.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);
        startButton.setEnabled(true);
        startButton.setAlpha(1f);
        startButton.setText("Open Camera");
        captureButton.setVisibility(View.GONE);
        
        // Unbind camera
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        
        cameraPlaceholder.setVisibility(View.VISIBLE);
    }

    private void openIncident() {
        if (lastDetectedIncident != null) {
            Intent intent = new Intent(this, IncidentDetailActivity.class);
            intent.putExtra("incident_data", lastDetectedIncident);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, IncidentDetailActivity.class);
            intent.putExtra("incident_index", 0);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}
