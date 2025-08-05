package io.github.byzatic.tessera.engine.application.commons.exceptions;

public class ExternalProcessException extends Exception {
    public ExternalProcessException(String message) {
        super(message);
    }

    public ExternalProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
