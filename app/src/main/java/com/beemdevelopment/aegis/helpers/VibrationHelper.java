package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.beemdevelopment.aegis.Preferences;

import dagger.hilt.InstallIn;
import dagger.hilt.android.EarlyEntryPoint;
import dagger.hilt.android.EarlyEntryPoints;
import dagger.hilt.components.SingletonComponent;

public class VibrationHelper {
    public static void vibratePattern(Context context, long[] pattern) {
        if (!isHapticFeedbackEnabled(context)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vibratorManager != null) {
                Vibrator vibrator = vibratorManager.getDefaultVibrator();
                VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
                vibrator.vibrate(effect);
            }
        } else {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
                    vibrator.vibrate(effect);
                }
            }
        }
    }

    public static boolean isHapticFeedbackEnabled(Context context) {
        Preferences _prefs = EarlyEntryPoints.get(context.getApplicationContext(), PrefEntryPoint.class).getPreferences();

        return _prefs.isHapticFeedbackEnabled();
    }

    @EarlyEntryPoint
    @InstallIn(SingletonComponent.class)
    public interface PrefEntryPoint {
        Preferences getPreferences();
    }
}
