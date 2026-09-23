package com.hazora.app.ui.hazardscan;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * YOLOv8 Object Detector implementation using TFLite.
 * Optimized for real-time hazard detection.
 */
public class YOLODetector {
    private Interpreter tflite;
    private List<String> labels;
    private final int inputWidth = 640;
    private final int inputHeight = 640;
    private final Context context;
    // Lower than the web dashboard (0.45): phone snapshots are single frames
    // (often blurry) and the subject is usually smaller in frame, so raw
    // confidence runs lower. 0.15 catches real PPE while NMS removes dupes.
    private final float confidenceThreshold = 0.15f;
    private final float iouThreshold = 0.45f;
    // Some YOLOv8 TFLite exports expect NCHW ([1,3,H,W]) instead of NHWC ([1,H,W,3]).
    // Detected once from the model's declared input shape.
    private boolean channelsFirst = false;

    public YOLODetector(Context context) {
        this.context = context;
        try {
            // Loading the model trained from ppe.ndjson
            if (assetExists("ppe_model.tflite")) {
                tflite = new Interpreter(loadModelFile("ppe_model.tflite"));
                int[] inputShape = tflite.getInputTensor(0).shape();
                // NCHW looks like [1, 3, 640, 640]; NHWC looks like [1, 640, 640, 3].
                channelsFirst = inputShape.length == 4 && inputShape[1] == 3;
                Log.d("YOLODetector", "ppe_model.tflite loaded successfully. Input shape: "
                        + Arrays.toString(inputShape)
                        + " (channelsFirst=" + channelsFirst + ")"
                        + ", Output shape: " + Arrays.toString(tflite.getOutputTensor(0).shape()));
                if (assetExists("yolo_labels.txt")) {
                    labels = FileUtil.loadLabels(context, "yolo_labels.txt");
                }
                // Fallback so class names always map correctly even if the label
                // asset is missing or fails to load.
                if (labels == null || labels.isEmpty()) {
                    labels = Arrays.asList("Safety Helmet", "Safety Vest", "Safety Shoes");
                }
                Log.d("AI_DEBUG", "Labels loaded: " + labels);
            } else {
                Log.e("YOLODetector", "ppe_model.tflite asset not found!");
            }
        } catch (Exception e) {
            Log.e("YOLODetector", "Error initializing TFLite Interpreter", e);
        }
    }

    private boolean assetExists(String filename) {
        try {
            String[] assets = context.getAssets().list("");
            if (assets == null) return false;
            for (String asset : assets) {
                if (asset.equals(filename)) return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
        try (AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
             FileChannel fileChannel = inputStream.getChannel()) {
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    public static class Recognition {
        public final String title;
        public final Float confidence;
        public final RectF location;

        public Recognition(String title, Float confidence, RectF location) {
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }
    }

    public List<Recognition> detect(Bitmap bitmap) {
        if (tflite == null) return new ArrayList<>();

        // Match the web dashboard preprocessing exactly (proven to work):
        // a straight bilinear resize to 640x640, values normalized to 0..1.
        // No letterbox padding, so coordinate back-mapping is a simple scale.
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(scaledBitmap);

        List<Recognition> recognitions = new ArrayList<>();

        try {
            int[] outputShape = tflite.getOutputTensor(0).shape(); // e.g. [1, 7, 8400] or [1, 8400, 7]
            Log.d("AI_DEBUG", "Output shape: " + Arrays.toString(outputShape)
                    + " inputShape: " + Arrays.toString(tflite.getInputTensor(0).shape())
                    + " inputType: " + tflite.getInputTensor(0).dataType()
                    + " outputType: " + tflite.getOutputTensor(0).dataType());
            int dim1 = outputShape.length > 1 ? outputShape[1] : 7;
            int dim2 = outputShape.length > 2 ? outputShape[2] : 8400;

            int detectionsCount = 0;
            float maxSeenProb = 0;

            boolean channelsFirstOutput = dim1 < dim2;
            int numAnchors = channelsFirstOutput ? dim2 : dim1;
            int channels = channelsFirstOutput ? dim1 : dim2;
            int actualNumClasses = channels - 4;

            float[][][] output = new float[1][dim1][dim2];
            tflite.run(inputBuffer, output);
            float[][] data = output[0];

            // Scan ALL anchors/classes for the true raw max (not sampled) so we
            // don't miss the peak. Also inspect the box-coordinate range to see
            // whether coords are normalized (0..1) or pixel (0..640).
            float rawMax = 0;
            float coordMax = 0;
            for (int i = 0; i < numAnchors; i++) {
                for (int c = 0; c < actualNumClasses; c++) {
                    float v = channelsFirstOutput ? data[4 + c][i] : data[i][4 + c];
                    if (v > rawMax) rawMax = v;
                }
                float cx = channelsFirstOutput ? data[0][i] : data[i][0];
                if (cx > coordMax) coordMax = cx;
            }
            boolean needsSigmoid = rawMax > 1.05f;
            Log.d("AI_DEBUG", "rawMax=" + rawMax + " coordMax=" + coordMax
                    + " needsSigmoid=" + needsSigmoid
                    + " layoutChannelsFirst=" + channelsFirstOutput + " classes=" + actualNumClasses);

            // Helper to read a channel value for anchor i regardless of layout.
            // channelsFirstOutput: data[channel][anchor]; else data[anchor][channel].
            for (int i = 0; i < numAnchors; i++) {
                float maxClassProb = 0;
                int classId = -1;
                for (int c = 0; c < actualNumClasses; c++) {
                    float raw = channelsFirstOutput ? data[4 + c][i] : data[i][4 + c];
                    float prob = needsSigmoid ? sigmoid(raw) : raw;
                    if (prob > maxClassProb) {
                        maxClassProb = prob;
                        classId = c;
                    }
                }

                if (maxClassProb > maxSeenProb) maxSeenProb = maxClassProb;
                if (maxClassProb <= confidenceThreshold) continue;

                detectionsCount++;
                float xCenter = channelsFirstOutput ? data[0][i] : data[i][0];
                float yCenter = channelsFirstOutput ? data[1][i] : data[i][1];
                float w = channelsFirstOutput ? data[2][i] : data[i][2];
                float h = channelsFirstOutput ? data[3][i] : data[i][3];

                // YOLOv8 TFLite exports usually output normalized coords (0..1).
                // Detect that and scale to letterboxed input pixels.
                if (xCenter <= 1.5f && w <= 1.5f) {
                    xCenter *= inputWidth;
                    yCenter *= inputHeight;
                    w *= inputWidth;
                    h *= inputHeight;
                }

                // Coords are in 640x640 stretched space; map back to the
                // original bitmap by simple axis scaling.
                float left = Math.max(0, (xCenter - w / 2) * bitmap.getWidth() / inputWidth);
                float top = Math.max(0, (yCenter - h / 2) * bitmap.getHeight() / inputHeight);
                float right = Math.min(bitmap.getWidth(), (xCenter + w / 2) * bitmap.getWidth() / inputWidth);
                float bottom = Math.min(bitmap.getHeight(), (yCenter + h / 2) * bitmap.getHeight() / inputHeight);

                RectF location = new RectF(left, top, right, bottom);

                String label = labels != null && classId < labels.size() ? labels.get(classId) : "Object " + classId;
                Log.d("AI_DEBUG", "DET " + label + " conf=" + String.format("%.2f", maxClassProb)
                        + " box=[" + (int) location.left + "," + (int) location.top + ","
                        + (int) location.right + "," + (int) location.bottom + "]");
                recognitions.add(new Recognition(label, maxClassProb, location));
            }

            Log.d("AI_DEBUG", "Scan Complete. Found " + detectionsCount + " items. Max Confidence seen: " + (maxSeenProb * 100) + "%");
        } catch (Exception e) {
            Log.e("YOLODetector", "Inference error during detection", e);
        }

        return nms(recognitions);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        DataType dataType = tflite != null ? tflite.getInputTensor(0).dataType() : DataType.FLOAT32;
        boolean isQuantized = (dataType == DataType.UINT8 || dataType == DataType.INT8);
        int numBytesPerChannel = isQuantized ? 1 : 4;
        int pixelCount = inputWidth * inputHeight;

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(pixelCount * 3 * numBytesPerChannel);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[pixelCount];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        if (channelsFirst) {
            // NCHW: write all R values, then all G, then all B.
            for (int channel = 0; channel < 3; channel++) {
                for (int pixelValue : intValues) {
                    int value;
                    if (channel == 0) value = (pixelValue >> 16) & 0xFF; // R
                    else if (channel == 1) value = (pixelValue >> 8) & 0xFF; // G
                    else value = pixelValue & 0xFF; // B
                    putChannel(byteBuffer, dataType, value);
                }
            }
        } else {
            // NHWC: write R,G,B interleaved per pixel.
            for (int pixelValue : intValues) {
                putChannel(byteBuffer, dataType, (pixelValue >> 16) & 0xFF);
                putChannel(byteBuffer, dataType, (pixelValue >> 8) & 0xFF);
                putChannel(byteBuffer, dataType, pixelValue & 0xFF);
            }
        }
        return byteBuffer;
    }

    private static float sigmoid(float x) {
        return (float) (1.0 / (1.0 + Math.exp(-x)));
    }

    private void putChannel(ByteBuffer byteBuffer, DataType dataType, int value) {
        if (dataType == DataType.UINT8) {
            byteBuffer.put((byte) value);
        } else if (dataType == DataType.INT8) {
            byteBuffer.put((byte) (value - 128));
        } else {
            byteBuffer.putFloat(value / 255.0f);
        }
    }

    private List<Recognition> nms(List<Recognition> recognitions) {
        List<Recognition> nmsList = new ArrayList<>();
        
        // Sort by confidence
        Collections.sort(recognitions, (o1, o2) -> o2.confidence.compareTo(o1.confidence));

        while (!recognitions.isEmpty()) {
            Recognition best = recognitions.get(0);
            nmsList.add(best);
            recognitions.remove(0);

            List<Recognition> toRemove = new ArrayList<>();
            for (Recognition next : recognitions) {
                if (boxIou(best.location, next.location) > iouThreshold) {
                    toRemove.add(next);
                }
            }
            recognitions.removeAll(toRemove);
        }
        
        return nmsList;
    }

    private float boxIou(RectF a, RectF b) {
        float intersectionLeft = Math.max(a.left, b.left);
        float intersectionTop = Math.max(a.top, b.top);
        float intersectionRight = Math.min(a.right, b.right);
        float intersectionBottom = Math.min(a.bottom, b.bottom);
        
        float intersectionArea = Math.max(0, intersectionRight - intersectionLeft) * 
                               Math.max(0, intersectionBottom - intersectionTop);
        
        float unionArea = (a.width() * a.height()) + (b.width() * b.height()) - intersectionArea;
        return intersectionArea / unionArea;
    }
}
