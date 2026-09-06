package com.hazora.app.ui.hazardscan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hazora.app.R;
import com.hazora.app.ui.incidents.IncidentDetailActivity;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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
    private DetectionOverlayView detectionOverlay;
    private Button captureButton;
    private ImageView ivCapturedResult;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private HazardDetector hazardDetector;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hazard_scan);

        cameraExecutor = Executors.newSingleThreadExecutor();
        hazardDetector = new HazardDetector(this);

        View back = findViewById(R.id.tv_back);
        back.setOnClickListener(v -> finish());

        analyzingLayout = findViewById(R.id.layout_analyzing);
        resultCard = findViewById(R.id.card_scan_result);
        startButton = findViewById(R.id.btn_start_scan);
        previewView = findViewById(R.id.previewView);
        cameraPlaceholder = findViewById(R.id.camera_placeholder);
        detectionOverlay = findViewById(R.id.detection_overlay);
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
            // Run AI Detection on the captured frame
            List<HazardDetector.DetectionResult> detections = hazardDetector.detect(bitmap);
            
            // Artificial delay to show "Analyzing" state for a moment
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

            runOnUiThread(() -> {
                analyzingLayout.setVisibility(View.GONE);
                
                if (!detections.isEmpty()) {
                    HazardDetector.DetectionResult bestMatch = detections.get(0);
                    
                    // Show bounding boxes on the UI overlay
                    detectionOverlay.updateResults(detections);
                    
                    // Display result card and upload
                    showDetectionResult(bestMatch, bitmap);
                } else {
                    detectionOverlay.clear();
                    Toast.makeText(this, "No hazards detected in this capture.", Toast.LENGTH_SHORT).show();
                    isScanning = false;
                }
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
        
        if (tvTitle != null) tvTitle.setText(result.label);
        if (tvDetails != null) {
            String details = "Confidence:  " + String.format(Locale.US, "%.1f", result.confidence * 100) + "%";
            tvDetails.setText(details);
        }

        // 1. Resize and Compress for Firestore (to stay under 1MB limit)
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 480, (int)(480 * ((float)bitmap.getHeight()/bitmap.getWidth())), true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 60, baos); // Lower quality to save space
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
        incident.put("imageData", base64Data); // Store the actual image as text
        incident.put("timestamp", Timestamp.now());
        incident.put("status", "New");
        incident.put("location", "Location Not Set");
        incident.put("cameraSource", "Mobile Capture");

        db.collection("incidents")
                .add(incident)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Incident saved to database", Toast.LENGTH_SHORT).show();
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
        detectionOverlay.clear();
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
        Intent intent = new Intent(this, IncidentDetailActivity.class);
        intent.putExtra("incident_index", 0);
        startActivity(intent);
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
