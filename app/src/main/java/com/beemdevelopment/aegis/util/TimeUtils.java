package com.beemdevelopment.aegis.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.PluralsRes;

import java.util.Date;

public class TimeUtils {
    private TimeUtils() {

    }

    public static String getElapsedSince(Context context, Date date) {
        long since = (new Date().getTime() - date.getTime()) / 1000;
        if (since < 60) {
            return formatElapsedSince(context, since, "seconds");
        }
        since /= 60;
        if (since < 60) {
            return formatElapsedSince(context, since, "minutes");
        }
        since /= 60;
        if (since < 24) {
            return formatElapsedSince(context, since, "hours");
        }
        since /= 24;
        if (since < 365) {
            return formatElapsedSince(context, since, "days");
        }
        since /= 365;
        return formatElapsedSince(context, since, "years");
    }

    @SuppressLint("DiscouragedApi")
    private static String formatElapsedSince(Context context, long since, String unit) {
        Resources res = context.getResources();
        @PluralsRes int id = res.getIdentifier(String.format("time_elapsed_%s", unit), "plurals", context.getPackageName());
        return res.getQuantityString(id, (int) since, (int) since);
    }
}
