package com.beemdevelopment.aegis;

public enum Theme {
    LIGHT,
    DARK,
    AMOLED;

    public static Theme fromInteger(int x) {
        switch (x) {
            case 0:
                return LIGHT;
            case 1:
                return DARK;
            case 2:
                return AMOLED;
        }
        return null;
    }

    public static int getThemeNameResource(int x) {
        switch (x) {
            case 0:
                return R.string.light_theme_title;
            case 1:
                return R.string.dark_theme_title;
            case 2:
                return R.string.amoled_theme_title;
        }
        return R.string.light_theme_title;
    }

    public static String[] getThemeNames() {
        return new String[]{
                "Light theme",
                "Dark theme",
                "Amoled theme"
        };
    }

    public static int[] getThemeNameResources() {
        return new int[] {
            R.string.light_theme_title,
            R.string.dark_theme_title,
            R.string.amoled_theme_title
        };
    }
}
