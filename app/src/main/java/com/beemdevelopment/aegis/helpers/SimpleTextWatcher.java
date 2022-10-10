package com.beemdevelopment.aegis.helpers;

import android.text.Editable;
import android.text.TextWatcher;

public final class SimpleTextWatcher implements TextWatcher {
    private final Listener _listener;

    public SimpleTextWatcher(Listener listener) {
        _listener = listener;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (_listener != null) {
            _listener.afterTextChanged(s);
        }
    }

    public interface Listener {
        void afterTextChanged(Editable s);
    }
}
