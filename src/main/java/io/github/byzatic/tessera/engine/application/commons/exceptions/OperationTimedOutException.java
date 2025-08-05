package io.github.byzatic.tessera.engine.application.commons.exceptions;

public class OperationTimedOutException extends Exception {
    public OperationTimedOutException(String message) {
        super(message);
    }

    public OperationTimedOutException(String message, Throwable cause) {
        super(message, cause);
    }
}
