package com.beemdevelopment.aegis.helpers;

import android.content.Context;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

public class BiometricsHelper {
    private BiometricsHelper() {

    }

    public static BiometricManager getManager(Context context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return null;
        }

        BiometricManager manager = BiometricManager.from(context);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                return manager;
            }
        } else {
            if (manager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
                return manager;
            }
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
