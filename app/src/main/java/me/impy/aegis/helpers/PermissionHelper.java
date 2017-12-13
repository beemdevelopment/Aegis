package me.impy.aegis.helpers;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {
    private PermissionHelper() {

    }

    public static boolean request(Activity activity, int requestCode, String... perms) {
        List<String> deniedPerms = new ArrayList<>();
        for (String permission : perms) {
            if (ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED) {
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
