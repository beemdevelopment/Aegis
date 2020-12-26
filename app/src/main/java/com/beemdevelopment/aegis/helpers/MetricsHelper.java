package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.util.DisplayMetrics;

public class MetricsHelper {
    private MetricsHelper() {

    }

    public static int convertDpToPixels(Context context, float dp) {
        return (int) (dp * (context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
