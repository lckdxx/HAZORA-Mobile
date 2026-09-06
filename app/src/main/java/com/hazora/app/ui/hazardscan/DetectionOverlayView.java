package com.hazora.app.ui.hazardscan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom view that draws detection boxes (like face/person rectangles) 
 * on top of the camera preview.
 */
public class DetectionOverlayView extends View {

    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final List<HazardDetector.DetectionResult> results = new ArrayList<>();

    public DetectionOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8f);
        
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setFakeBoldText(true);
    }

    public void updateResults(List<HazardDetector.DetectionResult> newResults) {
        results.clear();
        results.addAll(newResults);
        invalidate(); // Trigger redraw
    }

    public void clear() {
        results.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean personOrFaceDetected = false;
        for (HazardDetector.DetectionResult result : results) {
            if (result.type == HazardDetector.DetectionType.PERSON || 
                result.type == HazardDetector.DetectionType.FACE) {
                personOrFaceDetected = true;
                break;
            }
        }

        if (!personOrFaceDetected) return;

        for (HazardDetector.DetectionResult result : results) {
            Rect region = result.region;
            if (region == null) continue;
            
            float left = region.left;
            float top = region.top;
            float right = region.right;
            float bottom = region.bottom;

            // Draw Box
            if (result.type == HazardDetector.DetectionType.PERSON) {
                boxPaint.setColor(Color.CYAN);
                boxPaint.setStrokeWidth(4f);
            } else if (result.type == HazardDetector.DetectionType.FACE) {
                boxPaint.setColor(Color.YELLOW);
                boxPaint.setStrokeWidth(4f);
            } else if (result.isSecure) {
                boxPaint.setColor(Color.GREEN);
                boxPaint.setStrokeWidth(8f);
            } else {
                boxPaint.setColor(Color.RED);
                boxPaint.setStrokeWidth(8f);
            }
            
            canvas.drawRect(left, top, right, bottom, boxPaint);

            // Draw Label
            String text = result.label + " (" + (int)(result.confidence * 100) + "%)";
            float textWidth = textPaint.measureText(text);
            canvas.drawRect(left, top - 60, left + textWidth + 20, top, boxPaint); 
            canvas.drawText(text, left + 10, top - 15, textPaint);
        }
    }
}
