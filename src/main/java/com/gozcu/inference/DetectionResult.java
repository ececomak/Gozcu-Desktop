package com.gozcu.inference;

public class DetectionResult {

    private final int    classId;
    private final String className;
    private final float  confidence;
    private final int    x1, y1, x2, y2;

    public DetectionResult(int classId, String className, float confidence,
                           int x1, int y1, int x2, int y2) {
        this.classId    = classId;
        this.className  = className;
        this.confidence = confidence;
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
    }

    public int    getClassId()        { return classId; }
    public String getClassName()      { return className; }
    public float  getConfidence()     { return confidence; }
    public int    getX1()             { return x1; }
    public int    getY1()             { return y1; }
    public int    getX2()             { return x2; }
    public int    getY2()             { return y2; }
    public int    getWidth()          { return x2 - x1; }
    public int    getHeight()         { return y2 - y1; }
    public int    getConfidencePct()  { return Math.round(confidence * 100); }

    @Override
    public String toString() {
        return String.format("%s %.0f%% [%d,%d,%d,%d]",
                className, confidence * 100, x1, y1, x2, y2);
    }
}
