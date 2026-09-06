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
    private static final float HELMET_COLOR_THRESHOLD = 0.14f;
    private static final float HELMET_CONFIDENCE_THRESHOLD = 0.65f;

    public HazardDetector(Context context) {
        this.context = context;
        setupDetector();
    }

    private void setupDetector() {
        // TFLite Initialization would go here
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
        return new Rect(
            (int) (personBbox.left + width * 0.2),
            personBbox.top,
            (int) (personBbox.left + width * 0.8),
            (int) (personBbox.top + personBbox.height() * 0.22)
        );
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        List<DetectionResult> results = new ArrayList<>();
        
        // Mocking the detection flow from the JS code:
        // In reality, you'd use TFLite models here.
        
        // 1. Detect Person (Mocking a person in the center)
        Rect personRect = new Rect(
            (int)(bitmap.getWidth() * 0.2),
            (int)(bitmap.getHeight() * 0.1),
            (int)(bitmap.getWidth() * 0.8),
            (int)(bitmap.getHeight() * 0.9)
        );
        results.add(new DetectionResult("Person", 0.92f, true, personRect, DetectionType.PERSON));

        // 2. Detect Face (Mocking a face in the upper half of person)
        Rect faceRect = new Rect(
            (int)(personRect.left + personRect.width() * 0.25),
            (int)(personRect.top + personRect.height() * 0.1),
            (int)(personRect.left + personRect.width() * 0.75),
            (int)(personRect.top + personRect.height() * 0.4)
        );
        results.add(new DetectionResult("Face", 0.88f, true, faceRect, DetectionType.FACE));

        // 3. Identify Helmet Region
        Rect helmetRegion = getHelmetRegionFromPerson(personRect);
        
        // 3. Classify Helmet (Color Heuristics)
        DetectionResult helmetResult = classifyHelmetRegion(bitmap, helmetRegion, 0.0f, 0.0f);
        if (helmetResult != null) {
            results.add(helmetResult);
        }

        return results;
    }

    public enum DetectionType { PERSON, FACE, HELMET }

    public DetectionResult classifyHelmetRegion(Bitmap bitmap, Rect region, float helmetAiScore, float noHelmetAiScore) {
        // Clamp region to bitmap bounds
        int left = Math.max(0, region.left);
        int top = Math.max(0, region.top);
        int right = Math.min(bitmap.getWidth(), region.right);
        int bottom = Math.min(bitmap.getHeight(), region.bottom);
        int width = right - left;
        int height = bottom - top;

        if (width < 8 || height < 8) return null;

        Bitmap cropped;
        if (left == 0 && top == 0 && width == bitmap.getWidth() && height == bitmap.getHeight()) {
            cropped = bitmap;
        } else {
            cropped = Bitmap.createBitmap(bitmap, left, top, width, height);
        }
        
        HelmetStats stats = getHelmetRegionStats(cropped);

        boolean hasHelmet = resolveHelmetDecision(helmetAiScore, noHelmetAiScore, stats);
        
        String label = hasHelmet ? "Helmet" : "No helmet";
        float confidence = Math.max(helmetAiScore, stats.colorScore);

        return new DetectionResult(label, confidence, hasHelmet, new Rect(left, top, right, bottom), DetectionType.HELMET);
    }

    private HelmetStats getHelmetRegionStats(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int helmetPixels = 0;
        int lowerHelmetPixels = 0;
        int darkPixels = 0;
        int visiblePixels = 0;
        int lowerVisiblePixels = 0;

        // Sampling step to keep it performant
        int step = 4; 

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                boolean inCenter = x > width * 0.12 && x < width * 0.88;
                boolean inLowerBand = y > height * 0.32;

                if (!inCenter) continue;

                int pixel = bitmap.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                float[] hsv = new float[3];
                Color.RGBToHSV(r, g, b, hsv);
                float saturation = hsv[1];
                float brightness = hsv[2];

                if (brightness < 0.18f) continue;
                visiblePixels++;
                if (inLowerBand) lowerVisiblePixels++;

                // Dark hair-like detection
                boolean darkHairLike = brightness < 0.32f && saturation < 0.55f;
                if (darkHairLike && inLowerBand) {
                    darkPixels++;
                }

                // Color ranges for typical safety helmets
                boolean yellow = r > 125 && g > 95 && b < 105 && saturation > 0.22f;
                boolean orange = r > 135 && g > 55 && g < 175 && b < 105 && saturation > 0.28f;
                boolean red = r > 125 && g < 115 && b < 115 && saturation > 0.28f;
                boolean blue = b > 95 && r < 130 && g > 50 && saturation > 0.24f;
                boolean green = g > 95 && r < 130 && b < 135 && saturation > 0.24f;
                boolean brightWhite = r > 165 && g > 165 && b > 155 && saturation < 0.3f;
                
                boolean isHelmetColor = yellow || orange || red || blue || green || brightWhite;

                if (isHelmetColor) {
                    helmetPixels++;
                    if (inLowerBand) lowerHelmetPixels++;
                }
            }
        }

        HelmetStats stats = new HelmetStats();
        stats.colorScore = visiblePixels > 0 ? (float) helmetPixels / visiblePixels : 0;
        stats.lowerColorScore = lowerVisiblePixels > 0 ? (float) lowerHelmetPixels / lowerVisiblePixels : 0;
        stats.darkScore = lowerVisiblePixels > 0 ? (float) darkPixels / lowerVisiblePixels : 0;
        return stats;
    }

    private boolean resolveHelmetDecision(float helmetScore, float noHelmetScore, HelmetStats stats) {
        boolean strongHelmetEvidence = 
            helmetScore >= 0.65f && 
            helmetScore > noHelmetScore + 0.12f && 
            stats.lowerColorScore >= 0.12f;

        boolean fallbackHelmet = 
            (stats.lowerColorScore >= 0.2f || (
                stats.colorScore >= HELMET_COLOR_THRESHOLD && 
                helmetScore >= HELMET_CONFIDENCE_THRESHOLD && 
                helmetScore > noHelmetScore
            )) && stats.darkScore < 0.18f;

        return strongHelmetEvidence || fallbackHelmet;
    }

    private static class HelmetStats {
        float colorScore;
        float lowerColorScore;
        float darkScore;
    }

    public static class DetectionResult {
        public final String label;
        public final float confidence;
        public final boolean isSecure;
        public final Rect region;
        public final DetectionType type;

        public DetectionResult(String label, float confidence, boolean isSecure, Rect region, DetectionType type) {
            this.label = label;
            this.confidence = confidence;
            this.isSecure = isSecure;
            this.region = region;
            this.type = type;
        }
    }
}
