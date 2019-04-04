package com.beemdevelopment.aegis.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {
    private PermissionHelper() {

    }

    public static boolean granted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean request(Activity activity, int requestCode, String... perms) {
        List<String> deniedPerms = new ArrayList<>();
        for (String permission : perms) {
            if (!granted(activity, permission)) {
                deniedPerms.add(permission);
            }
        }

        int size = deniedPerms.size();
        if (size > 0) {
            String[] array = new String[size];
            ActivityCompat.requestPermissions(activity, deniedPerms.toArray(array), requestCode);
        }
        return size == 0;
    }

    public static boolean checkResults(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
