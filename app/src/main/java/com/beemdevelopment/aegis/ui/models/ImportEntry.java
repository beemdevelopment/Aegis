package com.beemdevelopment.aegis.ui.models;

import com.beemdevelopment.aegis.db.DatabaseEntry;

import java.io.Serializable;
import java.util.UUID;

public class ImportEntry implements Serializable {
    private UUID _uuid;
    private String _name;
    private String _issuer;

    private transient Listener _listener;
    private boolean _isChecked = true;

    public ImportEntry(DatabaseEntry entry) {
        _uuid = entry.getUUID();
        _name = entry.getName();
        _issuer = entry.getIssuer();
    }

    public UUID getUUID() {
        return _uuid;
    }

    public String getName() {
        return _name;
    }

    public String getIssuer() {
        return _issuer;
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
