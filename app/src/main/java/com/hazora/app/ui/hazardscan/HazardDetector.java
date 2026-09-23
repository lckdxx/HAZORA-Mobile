package com.hazora.app.ui.hazardscan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AI-Driven PPE Detection engine for HAZORA.
 * Uses YOLOv8 TFLite model for object detection and spatial grouping for body analysis.
 */
public class HazardDetector {

    public enum ScanMode {
        AUTO,         // Adaptive: required PPE inferred from how much of the body is visible
        HELMET_ONLY,  // Headshot / Selfie scan
        UPPER_BODY,   // Helmet + Vest
        FULL_BODY     // Helmet + Vest + Shoes
    }

    private final Context context;
    private YOLODetector yoloDetector;
    private ScanMode scanMode = ScanMode.AUTO;
    private boolean requireHelmet = true;
    private boolean requireVest = false;
    private boolean requireShoes = false;

    public HazardDetector(Context context) {
        this.context = context;
        setupDetector();
    }

    public void setScanMode(ScanMode mode) {
        this.scanMode = mode;
        switch (mode) {
            case AUTO:
                // Requirements are decided per person at detection time.
                this.requireHelmet = true;
                this.requireVest = false;
                this.requireShoes = false;
                break;
            case HELMET_ONLY:
                this.requireHelmet = true;
                this.requireVest = false;
                this.requireShoes = false;
                break;
            case UPPER_BODY:
                this.requireHelmet = true;
                this.requireVest = true;
                this.requireShoes = false;
                break;
            case FULL_BODY:
                this.requireHelmet = true;
                this.requireVest = true;
                this.requireShoes = true;
                break;
        }
    }

    public ScanMode getScanMode() { return scanMode; }

    public void setRequiredPPE(boolean helmet, boolean vest, boolean shoes) {
        this.requireHelmet = helmet;
        this.requireVest = vest;
        this.requireShoes = shoes;
    }

    public boolean isRequireHelmet() { return requireHelmet; }
    public boolean isRequireVest() { return requireVest; }
    public boolean isRequireShoes() { return requireShoes; }

    private void setupDetector() {
        yoloDetector = new YOLODetector(context);
    }

    /** Analyzes image for multiple people and their PPE compliance using YOLO AI. */
    public List<DetectionResult> detect(Bitmap bitmap) {
        List<DetectionResult> results = new ArrayList<>();
        
        // 1. Image Quality Check: Detect if the image is too blurry for AI
        boolean isBlurry = isImageBlurry(bitmap);
        
        // 2. Lighting Normalization: Enhance image for better AI shape recognition
        float lightScale = getAutoBrightnessScale(bitmap);
        Bitmap analysisBitmap = bitmap;
        if (lightScale != 1.0f) {
            analysisBitmap = boostBrightness(bitmap, lightScale);
        }

        // 3. Run YOLOv8 AI Detection
        List<YOLODetector.Recognition> yoloRecognitions = yoloDetector.detect(analysisBitmap);
        Rect fullFrameRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        if (!yoloRecognitions.isEmpty()) {
            List<PPECluster> clusters = groupDetectionsIntoPeople(yoloRecognitions, bitmap.getWidth());
            int totalPeople = clusters.size();
            int violations = 0;

            int frameHeight = bitmap.getHeight();
            for (int i = 0; i < clusters.size(); i++) {
                PPECluster cluster = clusters.get(i);

                // In AUTO mode, decide required PPE per person from how much of
                // the body is visible in the frame. Otherwise use the fixed mode.
                boolean reqHelmet = requireHelmet;
                boolean reqVest = requireVest;
                boolean reqShoes = requireShoes;
                if (scanMode == ScanMode.AUTO) {
                    boolean[] req = cluster.inferRequiredPpe(frameHeight);
                    reqHelmet = req[0];
                    reqVest = req[1];
                    reqShoes = req[2];
                }

                boolean helmetOk = !reqHelmet || cluster.hasHelmet;
                boolean vestOk = !reqVest || cluster.hasVest;
                boolean shoesOk = !reqShoes || cluster.hasShoes;
                boolean isSecure = helmetOk && vestOk && shoesOk;
                boolean isUncertain = cluster.minConfidence < 0.55f;
                
                if (!isSecure) violations++;

                String label;
                StringBuilder checklist = new StringBuilder();
                if (reqHelmet) checklist.append(String.format("Helmet: %s", cluster.hasHelmet ? "YES" : "NO"));
                if (reqVest) {
                    if (checklist.length() > 0) checklist.append(" | ");
                    checklist.append(String.format("Vest: %s", cluster.hasVest ? "YES" : "NO"));
                }
                if (reqShoes) {
                    if (checklist.length() > 0) checklist.append(" | ");
                    checklist.append(String.format("Shoes: %s", cluster.hasShoes ? "YES" : "NO"));
                }
                if (checklist.length() == 0) {
                    checklist.append(String.format("Helmet: %s", cluster.hasHelmet ? "YES" : "NO"));
                }

                String missingRemarks = cluster.getMissingRemarks(reqHelmet, reqVest, reqShoes);
                String missingAction = cluster.getMissingAction(reqHelmet, reqVest, reqShoes);

                String recommendation = isSecure ? "Worker is safe to proceed." : "Action Required: " + missingAction;
                String finalDescription = checklist.toString() + "\n" + recommendation;
                
                if (isUncertain) {
                    finalDescription = "Note: Detection is uncertain due to low visibility.\n" + finalDescription;
                }

                if (totalPeople > 1) {
                    label = "Person " + (i + 1) + ": " + (isSecure ? (isUncertain ? "Likely Secure" : "Secure") : missingRemarks);
                } else {
                    label = isSecure ? (isUncertain ? "AREA LIKELY SECURE" : "AREA SECURE: Required PPE Verified") : "Violation: No " + missingRemarks;
                }

                if (isBlurry) finalDescription = "⚠️ Warning: Image is blurry!\n" + finalDescription;

                results.add(new DetectionResult(label, finalDescription, cluster.minConfidence, isSecure, cluster.getCombinedBounds(), DetectionType.PERSON, isSecure ? "Low" : "Critical"));
                
                for (YOLODetector.Recognition rec : cluster.detections) {
                    results.add(new DetectionResult("PPE: " + rec.title, "Verified via AI", rec.confidence, true, 
                        new Rect((int)rec.location.left, (int)rec.location.top, (int)rec.location.right, (int)rec.location.bottom), 
                        DetectionType.HELMET, "Low"));
                }
            }

            if (totalPeople > 1) {
                String summary = String.format(Locale.getDefault(), "%d People: %d Secure, %d Violations", totalPeople, (totalPeople - violations), violations);
                results.add(0, new DetectionResult(summary, "Please review individual assessments below.", 1.0f, violations == 0, fullFrameRect, DetectionType.PERSON, violations > 0 ? "High" : "Low"));
            }
        } else {
            String advice = isBlurry ? "Hold the phone steady and move closer." : "Ensure the person is centered in the frame.";
            String missingReq;
            if (scanMode == ScanMode.AUTO) {
                missingReq = "PPE";
            } else {
                missingReq = requireHelmet && !requireVest && !requireShoes ? "Helmet" : "Required PPE";
            }
            results.add(new DetectionResult("Violation: No " + missingReq + " Detected", "Action: Equip all required PPE. " + advice, 0.0f, false, fullFrameRect, DetectionType.PERSON, "Critical"));
        }
        
        return results;
    }

    private boolean isImageBlurry(Bitmap bitmap) {
        // Simple variance check: sample pixels and look for sharp color changes
        // If the colors change too gradually, the image is likely blurry.
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        long totalDiff = 0;
        int count = 0;
        
        for (int y = 32; y < height - 32; y += 32) {
            for (int x = 32; x < width - 32; x += 32) {
                int p1 = bitmap.getPixel(x, y);
                int p2 = bitmap.getPixel(x + 1, y);
                totalDiff += Math.abs(Color.red(p1) - Color.red(p2));
                count++;
            }
        }
        return (count > 0 && (totalDiff / count) < 4); // Threshold for low variance (blur)
    }

    private List<PPECluster> groupDetectionsIntoPeople(List<YOLODetector.Recognition> recognitions, int frameWidth) {
        List<PPECluster> clusters = new ArrayList<>();
        for (YOLODetector.Recognition rec : recognitions) {
            boolean added = false;
            for (PPECluster cluster : clusters) {
                if (cluster.isPartOfBody(rec, frameWidth)) {
                    cluster.add(rec);
                    added = true;
                    break;
                }
            }
            if (!added) {
                clusters.add(new PPECluster(rec));
            }
        }
        return clusters;
    }

    private static class PPECluster {
        List<YOLODetector.Recognition> detections = new ArrayList<>();
        boolean hasHelmet = false, hasVest = false, hasShoes = false;
        float centerX;
        float minConfidence = 1.0f;

        PPECluster(YOLODetector.Recognition first) { add(first); }

        void add(YOLODetector.Recognition rec) {
            detections.add(rec);
            String label = rec.title.toLowerCase();
            if (label.contains("helmet") || label.contains("hard hat") || label.contains("hat") || label.contains("head")) {
                hasHelmet = true;
            }
            if (label.contains("vest") || label.contains("jacket") || label.contains("high-vis")) {
                hasVest = true;
            }
            if (label.contains("shoes") || label.contains("shoe") || label.contains("boot") || label.contains("footwear")) {
                hasShoes = true;
            }
            
            minConfidence = Math.min(minConfidence, rec.confidence);
            
            float totalX = 0;
            for (YOLODetector.Recognition d : detections) totalX += d.location.centerX();
            centerX = totalX / detections.size();
        }

        boolean isPartOfBody(YOLODetector.Recognition rec, int frameWidth) {
            float maxDistance = frameWidth > 0 ? (frameWidth * 0.25f) : 160f;
            return Math.abs(rec.location.centerX() - centerX) < maxDistance;
        }

        Rect getCombinedBounds() {
            float left = Float.MAX_VALUE, top = Float.MAX_VALUE, right = 0, bottom = 0;
            for (YOLODetector.Recognition d : detections) {
                left = Math.min(left, d.location.left);
                top = Math.min(top, d.location.top);
                right = Math.max(right, d.location.right);
                bottom = Math.max(bottom, d.location.bottom);
            }
            return new Rect((int)left, (int)(top - 20), (int)right, (int)(bottom + 50));
        }

        /**
         * Adaptive requirement inference for AUTO mode. Decides which PPE items
         * to check based on how far down the frame this person's detections
         * reach, plus which items were actually detected.
         * Returns [requireHelmet, requireVest, requireShoes].
         */
        boolean[] inferRequiredPpe(int frameHeight) {
            float lowestBottom = 0;
            for (YOLODetector.Recognition d : detections) {
                lowestBottom = Math.max(lowestBottom, d.location.bottom);
            }
            // Fraction of the frame the person's body occupies vertically.
            float reach = frameHeight > 0 ? lowestBottom / frameHeight : 0f;

            // Always check helmet. Add vest once the torso region is in view,
            // add shoes only when the body extends near the bottom of the frame.
            boolean reqHelmet = true;
            boolean reqVest = hasVest || reach >= 0.55f;
            boolean reqShoes = hasShoes || reach >= 0.85f;

            return new boolean[] { reqHelmet, reqVest, reqShoes };
        }

        String getMissingRemarks(boolean requireHelmet, boolean requireVest, boolean requireShoes) {
            List<String> missing = new ArrayList<>();
            if (requireHelmet && !hasHelmet) missing.add("Helmet");
            if (requireVest && !hasVest) missing.add("Vest");
            if (requireShoes && !hasShoes) missing.add("Shoes");
            if (missing.isEmpty()) return "None";
            return String.join(" & ", missing);
        }

        String getMissingAction(boolean requireHelmet, boolean requireVest, boolean requireShoes) {
            List<String> actions = new ArrayList<>();
            if (requireHelmet && !hasHelmet) actions.add("Wear a safety helmet");
            if (requireVest && !hasVest) actions.add("Equip high-vis vest");
            if (requireShoes && !hasShoes) actions.add("Wear safety shoes");
            if (actions.isEmpty()) return "All required PPE equipped.";
            return String.join(", ", actions) + ".";
        }
    }

    public float getAutoBrightnessScale(Bitmap bitmap) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        float luminanceTotal = 0;
        int sampledPixels = 0, darkPixels = 0;

        for (int y = 0; y < height; y += 8) {
            for (int x = 0; x < width; x += 8) {
                int pixel = bitmap.getPixel(x, y);
                float luminance = (0.2126f * Color.red(pixel) + 0.7152f * Color.green(pixel) + 0.0722f * Color.blue(pixel)) / 255f;
                luminanceTotal += luminance;
                sampledPixels++;
                if (luminance < 0.25f) darkPixels++;
            }
        }
        if (sampledPixels == 0) return 1f;
        float avgLuminance = luminanceTotal / sampledPixels;
        if (avgLuminance < 0.35f || ((float)darkPixels/sampledPixels) > 0.5f) {
            return Math.min(2.5f, 0.5f / Math.max(avgLuminance, 0.1f));
        }
        return avgLuminance > 0.75f ? 0.75f / avgLuminance : 1f;
    }

    private Bitmap boostBrightness(Bitmap bitmap, float factor) {
        if (factor == 1.0f) return bitmap;
        Bitmap output = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int[] pixels = new int[output.getWidth() * output.getHeight()];
        output.getPixels(pixels, 0, output.getWidth(), 0, 0, output.getWidth(), output.getHeight());
        for (int i = 0; i < pixels.length; i++) {
            int r = Math.min(255, (int)(Color.red(pixels[i]) * factor));
            int g = Math.min(255, (int)(Color.green(pixels[i]) * factor));
            int b = Math.min(255, (int)(Color.blue(pixels[i]) * factor));
            pixels[i] = Color.rgb(r, g, b);
        }
        output.setPixels(pixels, 0, output.getWidth(), 0, 0, output.getWidth(), output.getHeight());
        return output;
    }

    public enum DetectionType { PERSON, FACE, HELMET }

    public static class DetectionResult {
        public final String label;
        public final String description;
        public final float confidence;
        public final boolean isSecure;
        public final Rect region;
        public final DetectionType type;
        public final String severity;

        public DetectionResult(String label, String description, float confidence, boolean isSecure, Rect region, DetectionType type, String severity) {
            this.label = label;
            this.description = description;
            this.confidence = confidence;
            this.isSecure = isSecure;
            this.region = region;
            this.type = type;
            this.severity = severity;
        }
    }
}
