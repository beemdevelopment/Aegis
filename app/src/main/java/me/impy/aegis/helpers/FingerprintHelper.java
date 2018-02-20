package me.impy.aegis.helpers;

import android.Manifest;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;

public class FingerprintHelper {
    private FingerprintHelper() {

    }

    public static FingerprintManager getManager(Context context) {
        if (isSupported() && PermissionHelper.granted(context, Manifest.permission.USE_FINGERPRINT)) {
            FingerprintManager manager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
            if (manager != null && manager.isHardwareDetected() && manager.hasEnrolledFingerprints()) {
                return manager;
            }
        }
        return null;
    }

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}
