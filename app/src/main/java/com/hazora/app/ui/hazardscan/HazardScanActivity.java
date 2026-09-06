package com.hazora.app.ui.hazardscan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
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
import com.hazora.app.R;
import com.hazora.app.ui.incidents.IncidentDetailActivity;

import java.util.List;
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
    private View scanOverlay;
    private View cameraLabel;
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
        scanOverlay = findViewById(R.id.scan_overlay);
        cameraLabel = findViewById(R.id.tv_camera_label);

        startButton.setBackgroundTintList(null);
        ((Button) findViewById(R.id.btn_view_incident)).setBackgroundTintList(null);
        ((Button) findViewById(R.id.btn_scan_again)).setBackgroundTintList(null);

        startButton.setOnClickListener(v -> {
            if (allPermissionsGranted()) {
                startScan();
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }
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
                startScan();
            } else {
                Toast.makeText(this, "Camera permission is required for AI scan.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startScan() {
        startButton.setEnabled(false);
        startButton.setAlpha(0.55f);
        isScanning = true;
        
        // Start camera preview and analysis
        startCamera();
        
        handler.postDelayed(() -> {
            analyzingLayout.setVisibility(View.VISIBLE);
            resultCard.setVisibility(View.GONE);
            scanOverlay.setVisibility(View.VISIBLE);
            
            // In a real scenario, the detector would trigger the result.
            // Here we simulate a detection after 3 seconds of "analyzing".
            handler.postDelayed(() -> {
                isScanning = false;
                analyzingLayout.setVisibility(View.GONE);
                resultCard.setVisibility(View.VISIBLE);
                scanOverlay.setVisibility(View.GONE);
            }, 3000);
        }, 800);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Set up Image Analysis
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    if (isScanning) {
                        processImage(image);
                    } else {
                        image.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

                cameraPlaceholder.setVisibility(View.GONE);
                cameraLabel.setVisibility(View.VISIBLE);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImage(ImageProxy image) {
        // Here we would convert ImageProxy to Bitmap and pass to hazardDetector.
        // For the sake of this template, we just close the image.
        // In a real implementation:
        // Bitmap bitmap = imageToBitmap(image);
        // List<HazardDetector.DetectionResult> results = hazardDetector.detect(bitmap);
        // if (!results.isEmpty()) { ... handle detection ... }
        
        image.close();
    }

    private void resetScan() {
        isScanning = false;
        handler.removeCallbacksAndMessages(null);
        analyzingLayout.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);
        scanOverlay.setVisibility(View.GONE);
        startButton.setEnabled(true);
        startButton.setAlpha(1f);
        
        // Unbind camera
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        
        cameraPlaceholder.setVisibility(View.VISIBLE);
        cameraLabel.setVisibility(View.GONE);
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
