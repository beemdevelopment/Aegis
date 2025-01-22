package com.beemdevelopment.aegis;

import java.util.Arrays;

public class VibrationPatterns {
    public static final long[] EXPIRING = {475, 20, 5, 20, 965, 20, 5, 20, 965, 20, 5, 20, 420};
    public static final long[] REFRESH_CODE = {0, 100};

    public static long getLengthInMillis(long[] pattern) {
        return Arrays.stream(pattern).sum();
    }
}