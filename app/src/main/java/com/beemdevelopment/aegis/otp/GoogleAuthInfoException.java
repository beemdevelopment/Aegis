package com.beemdevelopment.aegis.otp;

import android.net.Uri;

public class GoogleAuthInfoException extends Exception {
    private final Uri _uri;

    public GoogleAuthInfoException(Uri uri, Throwable cause) {
        super(cause);
        _uri = uri;
    }

    public GoogleAuthInfoException(Uri uri, String message) {
        super(message);
        _uri = uri;
    }

    public GoogleAuthInfoException(Uri uri, String message, Throwable cause) {
        super(message, cause);
        _uri = uri;
    }

    /**
     * Reports whether the scheme of the URI is phonefactor://.
     */
    public boolean isPhoneFactor() {
        return _uri != null && _uri.getScheme() != null && _uri.getScheme().equals("phonefactor");
    }

    @Override
    public String getMessage() {
        Throwable cause = getCause();
        if (cause == null
                || this == cause
                || (super.getMessage() != null && super.getMessage().equals(cause.getMessage()))) {
            return super.getMessage();
        }

        return String.format("%s (%s)", super.getMessage(), cause.getMessage());
    }
}
