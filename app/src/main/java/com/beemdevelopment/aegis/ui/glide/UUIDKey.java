package com.beemdevelopment.aegis.ui.glide;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Key;

import java.security.MessageDigest;
import java.util.UUID;

public class UUIDKey implements Key {
    private UUID _uuid;

    public UUIDKey(UUID uuid) {
        _uuid = uuid;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(_uuid.toString().getBytes(CHARSET));
    }

    @Override
    public boolean equals(Object o) {
        return _uuid.equals(o);
    }

    @Override
    public int hashCode() {
        return _uuid.hashCode();
    }
}
