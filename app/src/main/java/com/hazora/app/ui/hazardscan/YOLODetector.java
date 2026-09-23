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
    private final float confidenceThreshold = 0.10f; // Lowered threshold for reliable headshot & close-up recognition
    private final float iouThreshold = 0.45f;

    public YOLODetector(Context context) {
        this.context = context;
        try {
            // Loading the model trained from ppe.ndjson
            if (assetExists("ppe_model.tflite")) {
                tflite = new Interpreter(loadModelFile("ppe_model.tflite"));
                Log.d("YOLODetector", "ppe_model.tflite loaded successfully. Input shape: " 
                        + Arrays.toString(tflite.getInputTensor(0).shape())
                        + ", Output shape: " + Arrays.toString(tflite.getOutputTensor(0).shape()));
                if (assetExists("yolo_labels.txt")) {
                    labels = FileUtil.loadLabels(context, "yolo_labels.txt");
                }
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

        // Preprocess image
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        List<Recognition> recognitions = new ArrayList<>();

        try {
            int[] outputShape = tflite.getOutputTensor(0).shape(); // e.g. [1, 7, 8400] or [1, 8400, 7]
            int dim1 = outputShape.length > 1 ? outputShape[1] : 7;
            int dim2 = outputShape.length > 2 ? outputShape[2] : 8400;

            int detectionsCount = 0;
            float maxSeenProb = 0;

            if (dim1 < dim2) {
                // Layout [1, 4 + numClasses, 8400] (e.g. [1, 7, 8400])
                float[][][] output = new float[1][dim1][dim2];
                tflite.run(inputBuffer, output);
                float[][] data = output[0]; // [dim1][dim2]
                int numAnchors = dim2;
                int channels = dim1;
                int actualNumClasses = channels - 4;

                for (int i = 0; i < numAnchors; i++) {
                    float maxClassProb = 0;
                    int classId = -1;

                    for (int c = 0; c < actualNumClasses; c++) {
                        float prob = data[4 + c][i];
                        if (prob > maxClassProb) {
                            maxClassProb = prob;
                            classId = c;
                        }
                    }

                    if (maxClassProb > maxSeenProb) maxSeenProb = maxClassProb;

                    if (maxClassProb > confidenceThreshold) {
                        detectionsCount++;
                        float xCenter = data[0][i];
                        float yCenter = data[1][i];
                        float w = data[2][i];
                        float h = data[3][i];

                        float left = Math.max(0, xCenter - w / 2);
                        float top = Math.max(0, yCenter - h / 2);
                        float right = Math.min(inputWidth, xCenter + w / 2);
                        float bottom = Math.min(inputHeight, yCenter + h / 2);

                        RectF location = new RectF(
                                left * bitmap.getWidth() / inputWidth,
                                top * bitmap.getHeight() / inputHeight,
                                right * bitmap.getWidth() / inputWidth,
                                bottom * bitmap.getHeight() / inputHeight
                        );

                        String label = labels != null && classId < labels.size() ? labels.get(classId) : "Object " + classId;
                        recognitions.add(new Recognition(label, maxClassProb, location));
                    }
                }
            } else {
                // Layout [1, 8400, 4 + numClasses] (e.g. [1, 8400, 7])
                float[][][] output = new float[1][dim1][dim2];
                tflite.run(inputBuffer, output);
                float[][] data = output[0]; // [dim1][dim2] = [8400][7]
                int numAnchors = dim1;
                int channels = dim2;
                int actualNumClasses = channels - 4;

                for (int i = 0; i < numAnchors; i++) {
                    float maxClassProb = 0;
                    int classId = -1;

                    for (int c = 0; c < actualNumClasses; c++) {
                        float prob = data[i][4 + c];
                        if (prob > maxClassProb) {
                            maxClassProb = prob;
                            classId = c;
                        }
                    }

                    if (maxClassProb > maxSeenProb) maxSeenProb = maxClassProb;

                    if (maxClassProb > confidenceThreshold) {
                        detectionsCount++;
                        float xCenter = data[i][0];
                        float yCenter = data[i][1];
                        float w = data[i][2];
                        float h = data[i][3];

                        float left = Math.max(0, xCenter - w / 2);
                        float top = Math.max(0, yCenter - h / 2);
                        float right = Math.min(inputWidth, xCenter + w / 2);
                        float bottom = Math.min(inputHeight, yCenter + h / 2);

                        RectF location = new RectF(
                                left * bitmap.getWidth() / inputWidth,
                                top * bitmap.getHeight() / inputHeight,
                                right * bitmap.getWidth() / inputWidth,
                                bottom * bitmap.getHeight() / inputHeight
                        );

                        String label = labels != null && classId < labels.size() ? labels.get(classId) : "Object " + classId;
                        recognitions.add(new Recognition(label, maxClassProb, location));
                    }
                }
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

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(inputWidth * inputHeight * 3 * numBytesPerChannel);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputWidth * inputHeight];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        
        for (int pixelValue : intValues) {
            int r = (pixelValue >> 16) & 0xFF;
            int g = (pixelValue >> 8) & 0xFF;
            int b = pixelValue & 0xFF;

            if (dataType == DataType.UINT8) {
                byteBuffer.put((byte) r);
                byteBuffer.put((byte) g);
                byteBuffer.put((byte) b);
            } else if (dataType == DataType.INT8) {
                byteBuffer.put((byte) (r - 128));
                byteBuffer.put((byte) (g - 128));
                byteBuffer.put((byte) (b - 128));
            } else {
                byteBuffer.putFloat(r / 255.0f);
                byteBuffer.putFloat(g / 255.0f);
                byteBuffer.putFloat(b / 255.0f);
            }
        }
        return byteBuffer;
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
