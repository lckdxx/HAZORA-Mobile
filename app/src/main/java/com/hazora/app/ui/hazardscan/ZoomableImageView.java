package com.hazora.app.ui.hazardscan;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * ImageView with pinch-to-zoom, double-tap-to-zoom, and pan support.
 * Used for full-screen viewing of a captured hazard scan image.
 */
public class ZoomableImageView extends AppCompatImageView {

    private final Matrix matrix = new Matrix();
    private final float[] matrixValues = new float[9];

    private float minScale = 1f;
    private final float maxScale = 5f;
    private float currentScale = 1f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private float lastTouchX, lastTouchY;
    private boolean isDragging = false;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        setImageMatrix(matrix);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (getDrawable() != null) fitToScreen();
    }

    private void fitToScreen() {
        if (getDrawable() == null) return;
        float viewW = getWidth();
        float viewH = getHeight();
        float drawableW = getDrawable().getIntrinsicWidth();
        float drawableH = getDrawable().getIntrinsicHeight();
        if (drawableW <= 0 || drawableH <= 0) return;

        float scale = Math.min(viewW / drawableW, viewH / drawableH);
        minScale = scale;
        currentScale = scale;

        matrix.reset();
        matrix.postScale(scale, scale);
        // Center the image.
        float dx = (viewW - drawableW * scale) / 2f;
        float dy = (viewH - drawableH * scale) / 2f;
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging && !scaleDetector.isInProgress() && currentScale > minScale) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    matrix.postTranslate(dx, dy);
                    setImageMatrix(matrix);
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float projected = currentScale * scaleFactor;
            if (projected < minScale) scaleFactor = minScale / currentScale;
            else if (projected > maxScale) scaleFactor = maxScale / currentScale;
            currentScale *= scaleFactor;
            matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            setImageMatrix(matrix);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (currentScale > minScale) {
                fitToScreen();
            } else {
                float target = minScale * 3f;
                float factor = target / currentScale;
                currentScale = target;
                matrix.postScale(factor, factor, e.getX(), e.getY());
                setImageMatrix(matrix);
            }
            return true;
        }
    }
}
