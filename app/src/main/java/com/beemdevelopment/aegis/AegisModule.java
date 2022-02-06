package com.beemdevelopment.aegis;

import android.content.Context;

import com.beemdevelopment.aegis.icons.IconPackManager;
import com.beemdevelopment.aegis.vault.VaultManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AegisModule {
    @Provides
    @Singleton
    public static IconPackManager provideIconPackManager(@ApplicationContext Context context) {
        return new IconPackManager(context);
    }

    @Provides
    @Singleton
    public static VaultManager provideVaultManager(@ApplicationContext Context context) {
        return new VaultManager(context);
    }

    @Provides
    public static Preferences providePreferences(@ApplicationContext Context context) {
        return new Preferences(context);
    }
}
