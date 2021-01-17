package com.beemdevelopment.aegis;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ThemeMap {
    private ThemeMap() {

    }

    public static final Map<Theme, Integer> DEFAULT = ImmutableMap.of(
            Theme.LIGHT, R.style.Theme_Aegis_Light_Default,
            Theme.DARK, R.style.Theme_Aegis_Dark_Default,
            Theme.AMOLED, R.style.Theme_Aegis_TrueDark_Default
    );

    public static final Map<Theme, Integer> NO_ACTION_BAR = ImmutableMap.of(
            Theme.LIGHT, R.style.Theme_Aegis_Light_NoActionBar,
            Theme.DARK, R.style.Theme_Aegis_Dark_NoActionBar,
            Theme.AMOLED, R.style.Theme_Aegis_TrueDark_NoActionBar
    );

    public static final Map<Theme, Integer> FULLSCREEN = ImmutableMap.of(
            Theme.LIGHT, R.style.Theme_Aegis_Light_Fullscreen,
            Theme.DARK, R.style.Theme_Aegis_Dark_Fullscreen,
            Theme.AMOLED, R.style.Theme_Aegis_TrueDark_Fullscreen
    );
}
