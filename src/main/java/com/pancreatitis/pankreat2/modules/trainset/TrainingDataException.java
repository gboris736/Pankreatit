package com.pancreatitis.pankreat2.modules.trainset;

public class TrainingDataException extends RuntimeException {
    public TrainingDataException(String message) {
        super(message);
    }
    public TrainingDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
