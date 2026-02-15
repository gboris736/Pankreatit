package com.pancreatitis.modules.prediction;

import java.util.Map;
import java.util.HashMap;

public class PredictionResult {
    private final Map<Integer, Float> probabilities;

    public PredictionResult(Map<Integer, Float> probabilities) {
        this.probabilities = new HashMap<>(probabilities);
    }

    public Map<Integer, Float> getProbabilities() {
        return new HashMap<>(probabilities);
    }

    public int getPredictedClass() {
        int predictedClass = -1;
        float maxProb = -1;
        for (Map.Entry<Integer, Float> entry : probabilities.entrySet()) {
            if (entry.getValue() > maxProb) {
                maxProb = entry.getValue();
                predictedClass = entry.getKey();
            }
        }
        return predictedClass;
    }

    @Override
    public String toString() {
        return "PredictionResult{" +
                "probabilities=" + probabilities +
                ", predictedClass=" + getPredictedClass() +
                '}';
    }
}