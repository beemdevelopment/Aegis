package com.beemdevelopment.aegis.helpers;

import android.content.Context;

import com.beemdevelopment.aegis.R;

public class PasswordStrengthHelper {
    // Material design color palette
    private static String[] COLORS = {"#FF5252", "#FF5252", "#FFC107", "#8BC34A", "#4CAF50"};

    public static String getString(int score, Context context) {
        if (score < 0 || score > 4) {
            throw new IllegalArgumentException("Not a valid zxcvbn score");
        }

        String[] strings = context.getResources().getStringArray(R.array.password_strength);
        return strings[score];
    }

    public static String getColor(int score) {
        if (score < 0 || score > 4) {
            throw new IllegalArgumentException("Not a valid zxcvbn score");
        }

        return COLORS[score];
    }
}
