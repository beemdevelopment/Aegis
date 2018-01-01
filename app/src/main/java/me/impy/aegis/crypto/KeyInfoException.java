package me.impy.aegis.crypto;

public class KeyInfoException extends Exception {
    public KeyInfoException(String message) {
        super(message);
    }

    public KeyInfoException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        Throwable cause = getCause();
        if (cause == null) {
            return super.getMessage();
        }
        return String.format("%s (%s)", super.getMessage(), cause.getMessage());
    }
}
