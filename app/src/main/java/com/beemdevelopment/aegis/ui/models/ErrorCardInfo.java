package com.beemdevelopment.aegis.ui.models;

import android.view.View;

import com.google.common.hash.HashCode;

import java.util.Objects;

public class ErrorCardInfo {
    private final String _message;
    private final View.OnClickListener _listener;

    public ErrorCardInfo(String message, View.OnClickListener listener) {
        _message = message;
        _listener = listener;
    }

    public String getMessage() {
        return _message;
    }

    public View.OnClickListener getListener() {
        return _listener;
    }

    @Override
    public int hashCode() {
        return HashCode.fromString(_message).asInt();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ErrorCardInfo)) {
            return false;
        }

        // This equality check purposefully ignores the onclick listener
        ErrorCardInfo info = (ErrorCardInfo) o;
        return Objects.equals(getMessage(), info.getMessage());
    }
}
