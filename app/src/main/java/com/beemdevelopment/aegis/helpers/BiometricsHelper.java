package com.beemdevelopment.aegis.helpers;

import android.content.Context;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

public class BiometricsHelper {
    private BiometricsHelper() {

    }

    public static BiometricManager getManager(Context context) {
        BiometricManager manager = BiometricManager.from(context);
        if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            return manager;
        }
        return null;
    }

    public static boolean isCanceled(int errorCode) {
        return errorCode == BiometricPrompt.ERROR_CANCELED
                || errorCode == BiometricPrompt.ERROR_USER_CANCELED
                || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON;
    }

    public static boolean isAvailable(Context context) {
        return getManager(context) != null;
    }
}
