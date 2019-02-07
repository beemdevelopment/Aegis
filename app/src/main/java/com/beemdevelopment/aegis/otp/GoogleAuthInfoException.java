package com.beemdevelopment.aegis.otp;

public class GoogleAuthInfoException extends Exception {
    public GoogleAuthInfoException(Throwable cause) {
        super(cause);
    }

    public GoogleAuthInfoException(String message) {
        super(message);
    }

    public GoogleAuthInfoException(String message, Throwable cause) {
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
