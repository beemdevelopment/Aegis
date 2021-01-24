package com.beemdevelopment.aegis.ui.models;

import com.beemdevelopment.aegis.vault.VaultEntry;

import java.io.Serializable;

public class ImportEntry implements Serializable {
    private final VaultEntry _entry;

    private transient Listener _listener;
    private boolean _isChecked = true;

    public ImportEntry(VaultEntry entry) {
        _entry = entry;
    }

    public VaultEntry getEntry() {
        return _entry;
    }

    public void setOnCheckedChangedListener(Listener listener) {
        _listener = listener;
    }

    public boolean isChecked() {
        return _isChecked;
    }

    public void setIsChecked(boolean isChecked) {
        _isChecked = isChecked;

        if (_listener != null) {
            _listener.onCheckedChanged(_isChecked);
        }
    }

    public interface Listener {
        void onCheckedChanged(boolean value);
    }
}
