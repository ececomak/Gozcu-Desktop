package com.gozcu.inference;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;

import java.util.Map;
import java.util.Arrays;

public class OnnxInspector {
    public static void main(String[] args) {
        String modelPath = "src/main/java/com/gozcu/img_proc_model/best.onnx";
        try (OrtEnvironment env = OrtEnvironment.getEnvironment();
             OrtSession session = env.createSession(modelPath, new OrtSession.SessionOptions())) {

            System.out.println("=== ONNX MODEL INSPECTION START ===");
            for (Map.Entry<String, NodeInfo> entry : session.getInputInfo().entrySet()) {
                System.out.println("Input Name: " + entry.getKey());
                if (entry.getValue().getInfo() instanceof TensorInfo) {
                    TensorInfo info = (TensorInfo) entry.getValue().getInfo();
                    System.out.println("Input Shape: " + Arrays.toString(info.getShape()));
                }
            }
            
            for (Map.Entry<String, NodeInfo> entry : session.getOutputInfo().entrySet()) {
                System.out.println("Output Name: " + entry.getKey());
                if (entry.getValue().getInfo() instanceof TensorInfo) {
                    TensorInfo info = (TensorInfo) entry.getValue().getInfo();
                    System.out.println("Output Shape: " + Arrays.toString(info.getShape()));
                }
            }
            System.out.println("=== ONNX MODEL INSPECTION END ===");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
