package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.beemdevelopment.aegis.Preferences;

public class VibrationHelper {
    private Preferences _preferences;

    public VibrationHelper(Context context) {
        _preferences = new Preferences(context);
    }

    public void vibratePattern(Context context, long[] pattern) {
        if (!isHapticFeedbackEnabled()) {
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

    public boolean isHapticFeedbackEnabled() {
        return _preferences.isHapticFeedbackEnabled();
    }
}
