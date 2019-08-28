package com.beemdevelopment.aegis.helpers;

import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;

import androidx.annotation.ColorInt;

import com.beemdevelopment.aegis.R;

public class ThemeHelper {
    private ThemeHelper() {

    }

    public static int getThemeColor(int attributeId, Resources.Theme currentTheme) {
        TypedValue typedValue = new TypedValue();
        currentTheme.resolveAttribute(attributeId, typedValue, true);
        @ColorInt int color = typedValue.data;

        return color;
    }
}
