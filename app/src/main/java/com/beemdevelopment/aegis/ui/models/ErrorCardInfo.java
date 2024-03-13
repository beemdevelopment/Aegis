package com.beemdevelopment.aegis.ui.models;

import android.view.View;

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
}
