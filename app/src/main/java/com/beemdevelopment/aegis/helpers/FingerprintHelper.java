package com.beemdevelopment.aegis.helpers;

import android.Manifest;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class FingerprintHelper {
    private FingerprintHelper() {

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static FingerprintManager getManager(Context context) {
        if (PermissionHelper.granted(context, Manifest.permission.USE_FINGERPRINT)) {
            FingerprintManager manager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
            if (manager != null && manager.isHardwareDetected() && manager.hasEnrolledFingerprints()) {
                return manager;
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean isAvailable(Context context) {
        return getManager(context) != null;
    }

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}
