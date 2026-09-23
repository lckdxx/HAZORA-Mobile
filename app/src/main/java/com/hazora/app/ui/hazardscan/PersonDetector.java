package com.hazora.app.ui.hazardscan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects people in a frame using an SSD MobileNet TFLite model (COCO).
 * Mirrors the web dashboard's coco-ssd person detection so PPE can be grouped
 * per person and evaluated on a focused crop (which greatly improves accuracy).
 */
public class PersonDetector {

    private ObjectDetector detector;
    private final float scoreThreshold = 0.35f;

    public static class PersonBox {
        public final RectF box;
        public final float score;

        public PersonBox(RectF box, float score) {
            this.box = box;
            this.score = score;
        }
    }

    public PersonDetector(Context context) {
        try {
            ObjectDetector.ObjectDetectorOptions options =
                    ObjectDetector.ObjectDetectorOptions.builder()
                            .setBaseOptions(BaseOptions.builder().setNumThreads(2).build())
                            .setMaxResults(10)
                            .setScoreThreshold(scoreThreshold)
                            .build();
            detector = ObjectDetector.createFromFileAndOptions(context, "person_detect.tflite", options);
            Log.d("AI_DEBUG", "PersonDetector loaded (person_detect.tflite)");
        } catch (Exception e) {
            Log.e("PersonDetector", "Failed to load person detection model", e);
            detector = null;
        }
    }

    /** Returns bounding boxes for all detected people in the bitmap. */
    public List<PersonBox> detect(Bitmap bitmap) {
        List<PersonBox> people = new ArrayList<>();
        if (detector == null) return people;

        try {
            TensorImage image = TensorImage.fromBitmap(bitmap);
            List<Detection> results = detector.detect(image);
            for (Detection detection : results) {
                if (detection.getCategories().isEmpty()) continue;
                String label = detection.getCategories().get(0).getLabel();
                float score = detection.getCategories().get(0).getScore();
                if (label != null && label.toLowerCase().contains("person")) {
                    people.add(new PersonBox(detection.getBoundingBox(), score));
                }
            }
            Log.d("AI_DEBUG", "PersonDetector found " + people.size() + " person(s)");
        } catch (Exception e) {
            Log.w("PersonDetector", "Person detection error: " + e.getMessage());
        }
        return people;
    }

    public boolean isReady() {
        return detector != null;
    }
}
