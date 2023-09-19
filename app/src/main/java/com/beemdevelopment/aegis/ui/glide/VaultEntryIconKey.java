package com.beemdevelopment.aegis.ui.glide;

import androidx.annotation.NonNull;

import com.beemdevelopment.aegis.vault.VaultEntryIcon;
import com.bumptech.glide.load.Key;

import java.security.MessageDigest;

public class VaultEntryIconKey implements Key {
    private final VaultEntryIcon _icon;

    public VaultEntryIconKey(VaultEntryIcon icon) {
        _icon = icon;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(_icon.getHash());
    }

    @Override
    public boolean equals(Object o) {
        return _icon.equals(o);
    }

    @Override
    public int hashCode() {
        return _icon.hashCode();
    }
}
