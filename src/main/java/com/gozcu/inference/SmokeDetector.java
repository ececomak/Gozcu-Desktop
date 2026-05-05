package com.gozcu.inference;

import ai.onnxruntime.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;

/**
 * Saf Java ile YOLO ONNX inference.
 * OpenCV bağımlılığı yok — BufferedImage üzerinde çalışır.
 */
public class SmokeDetector implements AutoCloseable {

    private static final int   INPUT_SIZE    = 640;
    private static final float NMS_THRESHOLD = 0.45f;

    private final OrtEnvironment env;
    private final OrtSession     session;
    private final String[]       classNames;
    private final String         inputName;

    // Buffer'ı bir kez ayır, her frame'de re-use et (GC baskısını azaltır)
    private final float[] inputBuffer = new float[3 * INPUT_SIZE * INPUT_SIZE];

    private static final Color[] CLASS_COLORS = {
            new Color(220, 38, 38),  // fire  → kırmızı
            new Color(234, 88, 12),  // smoke → turuncu
            new Color(37, 99, 235),  // diğer → mavi
    };

    public SmokeDetector(String modelPath, String[] classNames) throws OrtException {
        this.classNames = classNames;
        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        // Çoklu kamera kullanımında CPU kitlenmesini önlemek için thread sayısını sınırla
        opts.setIntraOpNumThreads(1);
        opts.setInterOpNumThreads(1);
        session   = env.createSession(modelPath, opts);
        inputName = session.getInputNames().iterator().next();
        System.out.printf("Model yüklendi  input=%s  sınıf=%d%n", inputName, classNames.length);
    }

    // ── Ana metod ────────────────────────────────────────────────────────────

    public synchronized List<DetectionResult> detect(BufferedImage frame, float confThreshold) throws OrtException {
        int origW = frame.getWidth(), origH = frame.getHeight();

        // 1. Letterbox → 640×640
        LetterboxInfo lb = letterbox(frame);

        // 2. getRGB() → CHW float buffer  (getRGB her zaman ARGB döner, tip bağımsız)
        int total = INPUT_SIZE * INPUT_SIZE;
        int[] argb = lb.image.getRGB(0, 0, INPUT_SIZE, INPUT_SIZE, null, 0, INPUT_SIZE);
        for (int i = 0; i < total; i++) {
            int p = argb[i];
            inputBuffer[           i] = ((p >> 16) & 0xFF) / 255.0f; // R
            inputBuffer[total    + i] = ((p >>  8) & 0xFF) / 255.0f; // G
            inputBuffer[total*2  + i] = ( p        & 0xFF) / 255.0f; // B
        }

        // 3. Inference
        long[] shape = {1, 3, INPUT_SIZE, INPUT_SIZE};
        OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputBuffer), shape);
        OrtSession.Result result = session.run(Collections.singletonMap(inputName, tensor));
        tensor.close();

        float[][][] raw = (float[][][]) result.get(0).getValue();
        result.close();

        // 4. output[1][nc+4][8400] parse
        int numAnchors = raw[0][0].length;
        int numClasses  = classNames.length;
        List<float[]> candidates = new ArrayList<>();

        for (int a = 0; a < numAnchors; a++) {
            float cx = raw[0][0][a], cy = raw[0][1][a];
            float  w = raw[0][2][a],  h = raw[0][3][a];

            float maxScore = 0; int maxClass = 0;
            for (int c = 0; c < numClasses; c++) {
                float s = raw[0][4 + c][a];
                if (s > maxScore) { maxScore = s; maxClass = c; }
            }
            if (maxScore < confThreshold) continue;

            // letterbox koordinatları → orijinal frame koordinatları
            float x1 = ((cx - w / 2f) - lb.padX) / lb.ratio;
            float y1 = ((cy - h / 2f) - lb.padY) / lb.ratio;
            float x2 = ((cx + w / 2f) - lb.padX) / lb.ratio;
            float y2 = ((cy + h / 2f) - lb.padY) / lb.ratio;

            x1 = Math.max(0, Math.min(x1, origW));
            y1 = Math.max(0, Math.min(y1, origH));
            x2 = Math.max(0, Math.min(x2, origW));
            y2 = Math.max(0, Math.min(y2, origH));

            candidates.add(new float[]{x1, y1, x2, y2, maxScore, maxClass});
        }

        // 5. NMS
        return applyNms(candidates);
    }

    /** Orijinal frame üzerine bounding box + etiket çizer (kopyasına) */
    public BufferedImage annotate(BufferedImage frame, List<DetectionResult> detections) {
        if (detections.isEmpty()) return frame;

        BufferedImage out = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(frame, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (DetectionResult d : detections) {
            Color color = CLASS_COLORS[d.getClassId() % CLASS_COLORS.length];
            g.setColor(color);
            g.setStroke(new BasicStroke(3));
            g.drawRect(d.getX1(), d.getY1(), d.getWidth(), d.getHeight());

            String label = d.getClassName() + " %" + d.getConfidencePct();
            g.setFont(new Font("Segoe UI", Font.BOLD, 14));
            FontMetrics fm = g.getFontMetrics();
            int lw = fm.stringWidth(label) + 10, lh = fm.getHeight() + 6;
            g.fillRect(d.getX1(), d.getY1() - lh, lw, lh);
            g.setColor(Color.WHITE);
            g.drawString(label, d.getX1() + 5, d.getY1() - 4);
        }
        g.dispose();
        return out;
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────

    private LetterboxInfo letterbox(BufferedImage src) {
        float ratio = Math.min((float) INPUT_SIZE / src.getWidth(),
                               (float) INPUT_SIZE / src.getHeight());
        int nw = (int)(src.getWidth()  * ratio);
        int nh = (int)(src.getHeight() * ratio);
        int padX = (INPUT_SIZE - nw) / 2;
        int padY = (INPUT_SIZE - nh) / 2;

        BufferedImage dst = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setColor(new Color(114, 114, 114));
        g.fillRect(0, 0, INPUT_SIZE, INPUT_SIZE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, padX, padY, nw, nh, null);
        g.dispose();

        return new LetterboxInfo(dst, ratio, padX, padY);
    }

    private List<DetectionResult> applyNms(List<float[]> boxes) {
        boxes.sort((a, b) -> Float.compare(b[4], a[4])); // score azalan
        boolean[] suppressed = new boolean[boxes.size()];
        List<DetectionResult> out = new ArrayList<>();

        for (int i = 0; i < boxes.size(); i++) {
            if (suppressed[i]) continue;
            float[] b = boxes.get(i);
            int cid = (int) b[5];
            out.add(new DetectionResult(cid,
                    cid < classNames.length ? classNames[cid] : "unknown",
                    b[4], (int)b[0], (int)b[1], (int)b[2], (int)b[3]));

            for (int j = i + 1; j < boxes.size(); j++) {
                if (!suppressed[j] && iou(b, boxes.get(j)) > NMS_THRESHOLD)
                    suppressed[j] = true;
            }
        }
        return out;
    }

    private float iou(float[] a, float[] b) {
        float ix1 = Math.max(a[0], b[0]), iy1 = Math.max(a[1], b[1]);
        float ix2 = Math.min(a[2], b[2]), iy2 = Math.min(a[3], b[3]);
        float inter = Math.max(0, ix2-ix1) * Math.max(0, iy2-iy1);
        float aA = (a[2]-a[0])*(a[3]-a[1]), bA = (b[2]-b[0])*(b[3]-b[1]);
        return inter / (aA + bA - inter + 1e-6f);
    }

    @Override
    public void close() {
        try { if (session != null) session.close(); } catch (OrtException ignored) {}
        if (env != null) env.close();
    }

    // ── İç sınıf ─────────────────────────────────────────────────────────────

    private record LetterboxInfo(BufferedImage image, float ratio, int padX, int padY) {}
}
