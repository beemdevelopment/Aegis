package com.beemdevelopment.aegis;

import androidx.annotation.StringRes;

import java.util.concurrent.TimeUnit;

public enum PassReminderFreq {
    NEVER,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    QUARTERLY;

    public long getDurationMillis() {
        long weeks;
        switch (this) {
            case WEEKLY:
                weeks = 1;
                break;
            case BIWEEKLY:
                weeks = 2;
                break;
            case MONTHLY:
                weeks = 4;
                break;
            case QUARTERLY:
                weeks = 13;
                break;
            default:
                weeks = 0;
                break;
        }

        return TimeUnit.MILLISECONDS.convert(weeks * 7L, TimeUnit.DAYS);
    }

    @StringRes
    public int getStringRes() {
        switch (this) {
            case WEEKLY:
                return R.string.password_reminder_freq_weekly;
            case BIWEEKLY:
                return R.string.password_reminder_freq_biweekly;
            case MONTHLY:
                return R.string.password_reminder_freq_monthly;
            case QUARTERLY:
                return R.string.password_reminder_freq_quarterly;
            default:
                return R.string.password_reminder_freq_never;
        }
    }

    public static PassReminderFreq fromInteger(int i) {
        return PassReminderFreq.values()[i];
    }
}
