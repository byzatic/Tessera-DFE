package io.github.byzatic.tessera.engine.application.commons.exceptions;

/** Thrown when XSD or business validation fails. */
public class ConfigurationValidationException extends Exception {
    public ConfigurationValidationException(String message) { super(message); }
    public ConfigurationValidationException(String message, Throwable cause) { super(message, cause); }
}