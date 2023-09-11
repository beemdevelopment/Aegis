package com.beemdevelopment.aegis.ui.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.beemdevelopment.aegis.vault.VaultGroup;
import java.io.Serializable;
import java.util.UUID;

public class VaultGroupModel implements Serializable {
    private final VaultGroup _group;
    private final String _placeholderName;

    public VaultGroupModel(VaultGroup group) {
        _group = group;
        _placeholderName = null;
    }

    public VaultGroupModel(String placeholderName) {
        _group = null;
        _placeholderName = placeholderName;
    }

    public VaultGroup getGroup() {
        return _group;
    }

    public String getName() {
        return _group != null ? _group.getName() : _placeholderName;
    }

    public boolean isPlaceholder() {
        return _group == null;
    }

    @Nullable
    public UUID getUUID() {
        return _group == null ? null : _group.getUUID();
    }

    @NonNull
    @Override
    public String toString() {
        return getName();
    }
}
