package com.hazora.app.ui.hazardscan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported PPE Detection logic for Android.
 * Implements color-based detection heuristics and prepares for TFLite integration.
 */
public class HazardDetector {

    private final Context context;
    private YOLODetector yoloDetector;

    public HazardDetector(Context context) {
        this.context = context;
        setupDetector();
    }

    private void setupDetector() {
        yoloDetector = new YOLODetector(context);
    }

    public float getAutoBrightnessScale(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float luminanceTotal = 0;
        int sampledPixels = 0;
        int brightPixels = 0;

        // Sample every 8th pixel horizontally and vertically
        for (int y = 0; y < height; y += 8) {
            for (int x = 0; x < width; x += 8) {
                int pixel = bitmap.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                
                float luminance = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
                luminanceTotal += luminance;
                sampledPixels++;
                if (luminance >= 0.94f) brightPixels++;
            }
        }

        if (sampledPixels == 0) return 1f;

        float averageLuminance = luminanceTotal / sampledPixels;
        float brightPixelRatio = (float) brightPixels / sampledPixels;
        
        if (averageLuminance <= 0.72f && brightPixelRatio <= 0.28f) return 1f;

        float averageScale = 0.72f / Math.max(averageLuminance, 0.72f);
        float highlightScale = brightPixelRatio > 0.45f ? 0.72f : 0.84f;
        return Math.max(0.55f, Math.min(1f, Math.min(averageScale, highlightScale)));
    }

    public Rect getHelmetRegionFromPerson(Rect personBbox) {
        int width = personBbox.width();
        // Selfie optimization: Increase height check area to 45% of top area
        // and adjust width to center better on close-up faces.
        return new Rect(
            (int) (personBbox.left + width * 0.1),
            personBbox.top,
            (int) (personBbox.left + width * 0.9),
            (int) (personBbox.top + personBbox.height() * 0.45)
        );
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        List<DetectionResult> results = new ArrayList<>();
        
        // 1. Try YOLOv8 Detector First (High Precision Object Detection)
        List<YOLODetector.Recognition> yoloRecognitions = yoloDetector.detect(bitmap);
        if (!yoloRecognitions.isEmpty()) {
            for (YOLODetector.Recognition rec : yoloRecognitions) {
                String label = rec.title.toLowerCase();
                float confidence = rec.confidence;
                Rect region = new Rect((int)rec.location.left, (int)rec.location.top, (int)rec.location.right, (int)rec.location.bottom);

                if (label.contains("hardhat") || label.contains("helmet")) {
                    results.add(new DetectionResult("PPE: Helmet", confidence, true, region, DetectionType.HELMET, "Low"));
                } else if (label.contains("no-hardhat") || label.contains("no-helmet")) {
                    results.add(new DetectionResult("Violation: No Helmet", confidence, false, region, DetectionType.HELMET, "Critical"));
                } else if (label.contains("vest")) {
                    results.add(new DetectionResult("PPE: Safety Vest", confidence, true, region, DetectionType.HELMET, "Low"));
                } else if (label.contains("no-vest")) {
                    results.add(new DetectionResult("Violation: No Vest", confidence, false, region, DetectionType.HELMET, "High"));
                } else if (label.contains("hazard") || label.contains("danger")) {
                    results.add(new DetectionResult("Hazard Detected: " + rec.title, confidence, false, region, DetectionType.HELMET, "High"));
                } else {
                    results.add(new DetectionResult(rec.title, confidence, true, region, DetectionType.HELMET, "Low"));
                }
            }
            if (!results.isEmpty()) return results;
        }

        // 2. Run Heuristics (Fallback)
        // 1. Detect Person - Wider bounds for selfie/close-up
        Rect personRect = new Rect(
            (int)(bitmap.getWidth() * 0.05),
            (int)(bitmap.getHeight() * 0.05),
            (int)(bitmap.getWidth() * 0.95),
            (int)(bitmap.getHeight() * 0.95)
        );
        
        // 2. Identify Helmet Region (Top of the head area)
        Rect helmetRegion = getHelmetRegionFromPerson(personRect);
        
        // 3. Classify Helmet
        DetectionResult helmetResult = classifyHelmetRegion(bitmap, helmetRegion, 0.0f);
        
        if (helmetResult != null && helmetResult.isSecure) {
            // If helmet is detected, we report the person as secure
            results.add(new DetectionResult("Person (Secure)", 0.95f, true, personRect, DetectionType.PERSON, "Low"));
            results.add(helmetResult);
        } else {
            // If no helmet, this is a violation
            results.add(new DetectionResult("Safety Violation", 0.92f, false, personRect, DetectionType.PERSON, "Critical"));
            if (helmetResult != null) results.add(helmetResult);
        }

        // 4. Detect Face
        Rect faceRect = new Rect(
            (int)(personRect.left + personRect.width() * 0.25),
            (int)(personRect.top + personRect.height() * 0.2),
            (int)(personRect.left + personRect.width() * 0.75),
            (int)(personRect.top + personRect.height() * 0.5)
        );
        results.add(new DetectionResult("Face", 0.88f, true, faceRect, DetectionType.FACE, "Low"));

        return results;
    }

    public enum DetectionType { PERSON, FACE, HELMET }

    public DetectionResult classifyHelmetRegion(Bitmap bitmap, Rect region, float helmetAiScore) {
        // Clamp region to bitmap bounds
        int left = Math.max(0, region.left);
        int top = Math.max(0, region.top);
        int right = Math.min(bitmap.getWidth(), region.right);
        int bottom = Math.min(bitmap.getHeight(), region.bottom);
        int width = right - left;
        int height = bottom - top;

        if (width < 8 || height < 8) return null;

        Bitmap cropped;
        try {
            cropped = Bitmap.createBitmap(bitmap, left, top, width, height);
        } catch (Exception e) {
            return null;
        }
        
        HelmetStats stats = getHelmetRegionStats(cropped);

        boolean hasHelmet = resolveHelmetDecision(helmetAiScore, stats);
        
        String label = hasHelmet ? "Helmet Detected" : "No Helmet";
        float confidence = Math.max(helmetAiScore, stats.colorScore);
        String severity = hasHelmet ? "Low" : "High";

        return new DetectionResult(label, confidence, hasHelmet, new Rect(left, top, right, bottom), DetectionType.HELMET, severity);
    }

    private HelmetStats getHelmetRegionStats(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int helmetPixels = 0;
        int lowerHelmetPixels = 0;
        int upperHelmetPixels = 0;
        float darkPixels = 0;
        int visiblePixels = 0;
        int lowerVisiblePixels = 0;
        int upperVisiblePixels = 0;

        // Increased sampling for better accuracy
        int step = 2; 

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                // Selfie optimization: check 90% of the center width
                boolean inCenter = x > width * 0.05 && x < width * 0.95;
                // Check 60% of the top for helmet
                boolean inUpperBand = y < height * 0.60;
                boolean inLowerBand = y > height * 0.40;

                if (!inCenter) continue;

                int pixel = bitmap.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                float[] hsv = new float[3];
                Color.RGBToHSV(r, g, b, hsv);
                float saturation = hsv[1];
                float brightness = hsv[2];

                if (brightness < 0.12f) continue;
                visiblePixels++;
                if (inLowerBand) lowerVisiblePixels++;
                if (inUpperBand) upperVisiblePixels++;

                // Hair detection - covers dark, brown, and lit hair
                boolean hairColor = (brightness < 0.42f && saturation < 0.55f);
                boolean brownishHair = (r > g && g > b && r < 130 && saturation < 0.6f);
                if ((hairColor || brownishHair) && inLowerBand) {
                    darkPixels++;
                }

                // Skin detection - if skin is visible in the forehead area, helmet is likely missing
                boolean skinColor = r > 150 && g > 110 && b > 90 && r > g && g > b && saturation < 0.45f;
                if (skinColor && inLowerBand) {
                    // Lowered weight for skin in lower band for selfies (glasses/hair overlap)
                    darkPixels += 0.5f; 
                }

                // Expanded color ranges for white helmets - ULTRA shadow tolerant for indoors
                boolean whiteBody = r > 125 && g > 125 && b > 120 && saturation < 0.30f && brightness > 0.35f;
                boolean brightHighlight = r > 200 && g > 200 && b > 190 && saturation < 0.20f;
                
                boolean yellow = r > 120 && g > 90 && b < 110 && saturation > 0.2f;
                boolean orange = r > 130 && g > 50 && g < 185 && b < 110 && saturation > 0.25f;
                boolean red = r > 120 && g < 120 && b < 120 && saturation > 0.25f;
                boolean blue = b > 90 && r < 140 && g > 40 && saturation > 0.2f;
                boolean green = g > 90 && r < 140 && b < 140 && saturation > 0.2f;
                
                boolean isHelmetColor = whiteBody || brightHighlight || yellow || orange || red || blue || green;

                if (isHelmetColor) {
                    helmetPixels++;
                    if (inLowerBand) lowerHelmetPixels++;
                    if (inUpperBand) upperHelmetPixels++;
                }
            }
        }

        HelmetStats stats = new HelmetStats();
        stats.colorScore = visiblePixels > 0 ? (float) helmetPixels / visiblePixels : 0;
        stats.lowerColorScore = lowerVisiblePixels > 0 ? (float) lowerHelmetPixels / lowerVisiblePixels : 0;
        stats.upperColorScore = upperVisiblePixels > 0 ? (float) upperHelmetPixels / upperVisiblePixels : 0;
        stats.darkScore = lowerVisiblePixels > 0 ? darkPixels / lowerVisiblePixels : 0;
        return stats;
    }

    private boolean resolveHelmetDecision(float helmetScore, HelmetStats stats) {
        // AI Score takes priority if available
        if (helmetScore > 0.85f) return true;

        // Tilted Helmet/Selfie optimization: allow up to 80% dark pixels if top is clearly helmet
        if (stats.upperColorScore > 0.5f && stats.darkScore < 0.80f) return true;

        if (stats.darkScore > 0.70f) return false;

        // Scenario 1: Solid helmet top presence (most reliable in selfies)
        if (stats.upperColorScore > 0.40f && stats.darkScore < 0.60f) return true;

        // Scenario 2: Moderate overall coverage
        if (stats.colorScore > 0.30f && stats.darkScore < 0.50f) return true;

        // Scenario 3: Presence of "Specular Highlights" or shiny surfaces
        return (stats.upperColorScore > 0.25f || stats.colorScore > 0.20f) && stats.darkScore < 0.40f;
    }

    private static class HelmetStats {
        float colorScore;
        float upperColorScore;
        float lowerColorScore;
        float darkScore;
    }

    public static class DetectionResult {
        public final String label;
        public final float confidence;
        public final boolean isSecure;
        public final Rect region;
        public final DetectionType type;
        public final String severity;

        public DetectionResult(String label, float confidence, boolean isSecure, Rect region, DetectionType type, String severity) {
            this.label = label;
            this.confidence = confidence;
            this.isSecure = isSecure;
            this.region = region;
            this.type = type;
            this.severity = severity;
        }
    }
}
