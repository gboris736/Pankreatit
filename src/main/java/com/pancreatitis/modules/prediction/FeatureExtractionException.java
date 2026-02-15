package com.pancreatitis.modules.prediction;

public class FeatureExtractionException extends RuntimeException {
    public FeatureExtractionException(String message) {
        super(message);
    }

    public FeatureExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
