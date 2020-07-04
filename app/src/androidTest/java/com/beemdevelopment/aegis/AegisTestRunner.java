package com.beemdevelopment.aegis;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnitRunner;

import java.io.File;

public class AegisTestRunner extends AndroidJUnitRunner {
    @Override
    public void callApplicationOnCreate(Application app) {
        Context context = app.getApplicationContext();

        // clear internal storage so that there is no vault file
        clearDirectory(context.getFilesDir(), false);

        // clear preferences so that the intro is started from MainActivity
        ApplicationProvider.getApplicationContext().getFilesDir();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .clear()
                .apply();

        super.callApplicationOnCreate(app);
    }

    private static void clearDirectory(File dir, boolean deleteParent) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    clearDirectory(file, true);
                } else {
                    file.delete();
                }
            }
        }

        if (deleteParent) {
            dir.delete();
        }
    }
}
