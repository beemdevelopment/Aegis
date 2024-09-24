package com.beemdevelopment.aegis.ui.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beemdevelopment.aegis.GroupPlaceholderType;
import com.beemdevelopment.aegis.vault.VaultGroup;

import java.io.Serializable;
import java.util.UUID;

public class VaultGroupModel implements Serializable {
    private final VaultGroup _group;
    private final GroupPlaceholderType _placeholderType;
    private final String _placeholderText;

    public VaultGroupModel(VaultGroup group) {
        _group = group;
        _placeholderText = null;
        _placeholderType = null;
    }

    public VaultGroupModel(Context context, GroupPlaceholderType placeholderType) {
        _group = null;
        _placeholderType = placeholderType;
        _placeholderText = context.getString(placeholderType.getStringRes());
    }

    public VaultGroup getGroup() {
        return _group;
    }

    public String getName() {
        return _group != null ? _group.getName() : _placeholderText;
    }

    public GroupPlaceholderType getPlaceholderType() {
        return _placeholderType;
    }

    public boolean isPlaceholder() {
        return _placeholderType != null;
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
