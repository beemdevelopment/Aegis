package com.beemdevelopment.aegis.ui.glide;

import android.content.Context;

import androidx.annotation.NonNull;

import com.beemdevelopment.aegis.vault.VaultEntry;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import java.nio.ByteBuffer;

@GlideModule
public class AegisGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.prepend(VaultEntry.class, ByteBuffer.class, new IconLoader.Factory());
    }
}
