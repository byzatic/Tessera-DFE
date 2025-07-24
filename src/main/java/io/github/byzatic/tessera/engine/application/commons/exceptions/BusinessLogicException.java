package io.github.byzatic.tessera.engine.application.commons.exceptions;

public class BusinessLogicException extends Exception {
    public BusinessLogicException(String message) {
        super(message);
    }

    public BusinessLogicException(String message, Throwable cause) {
        super(message, cause);
    }
}
