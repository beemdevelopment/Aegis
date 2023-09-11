package com.beemdevelopment.aegis.ui.models;

import com.beemdevelopment.aegis.icons.IconPack;
import com.beemdevelopment.aegis.vault.VaultEntry;

import java.io.Serializable;

public class AssignIconEntry implements Serializable {
    private final VaultEntry _entry;

    private IconPack.Icon _newIcon;

    private transient AssignIconEntry.Listener _listener;

    public void setOnResetListener(AssignIconEntry.Listener listener) {
        _listener = listener;
    }

    public AssignIconEntry(VaultEntry entry) {
        _entry = entry;
    }

    public VaultEntry getEntry() {
        return _entry;
    }

    public IconPack.Icon getNewIcon() { return _newIcon; }

    public void setNewIcon(IconPack.Icon icon) {
        _newIcon = icon;

        if (_listener != null) {
            _listener.onNewIconChanged();
        }
    }

    public interface Listener {
        void onNewIconChanged();
    }
}