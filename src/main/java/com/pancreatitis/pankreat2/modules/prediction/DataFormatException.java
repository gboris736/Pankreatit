package com.pancreatitis.pankreat2.modules.prediction;

class DataFormatException extends RuntimeException {
    public DataFormatException(String message) {
        super(message);
    }

    public DataFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}