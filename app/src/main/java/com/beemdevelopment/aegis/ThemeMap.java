package com.beemdevelopment.aegis;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ThemeMap {
    private ThemeMap() {

    }

    public static final Map<Theme, Integer> DEFAULT = ImmutableMap.of(
            Theme.LIGHT, R.style.AppTheme,
            Theme.DARK, R.style.AppTheme_Dark,
            Theme.AMOLED, R.style.AppTheme_TrueBlack
    );

    public static final Map<Theme, Integer> NO_ACTION_BAR = ImmutableMap.of(
            Theme.LIGHT, R.style.AppTheme_Light_NoActionBar,
            Theme.DARK, R.style.AppTheme_Dark_NoActionBar,
            Theme.AMOLED, R.style.AppTheme_TrueBlack_NoActionBar
    );
}
