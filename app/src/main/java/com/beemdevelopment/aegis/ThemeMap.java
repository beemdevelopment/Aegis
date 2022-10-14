package com.beemdevelopment.aegis;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ThemeMap {
    private ThemeMap() {

    }

    public static final Map<Theme, Integer> DEFAULT = ImmutableMap.of(
            Theme.LIGHT, R.style.Theme_Aegis_Light,
            Theme.DARK, R.style.Theme_Aegis_Dark,
            Theme.AMOLED, R.style.Theme_Aegis_Amoled
    );

    public static final Map<Theme, Integer> FULLSCREEN = ImmutableMap.of(
            Theme.LIGHT, R.style.Theme_Aegis_Light_Fullscreen,
            Theme.DARK, R.style.Theme_Aegis_Dark_Fullscreen,
            Theme.AMOLED, R.style.Theme_Aegis_Amoled_Fullscreen
    );
}
