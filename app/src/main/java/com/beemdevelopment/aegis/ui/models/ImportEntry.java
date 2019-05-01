package com.beemdevelopment.aegis.ui.models;

import com.beemdevelopment.aegis.db.DatabaseEntry;

public class ImportEntry {
    private DatabaseEntry _entry;
    private boolean _isChecked = true;
    private Listener _listener;

    public ImportEntry(DatabaseEntry entry) {
        _entry = entry;
    }

    public void setOnCheckedChangedListener(Listener listener) {
        _listener = listener;
    }

    public DatabaseEntry getDatabaseEntry() {
        return _entry;
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
