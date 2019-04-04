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

    public static String getThemeName(int x) {
        switch (x) {
            case 0:
                return "Light theme";
            case 1:
                return "Dark theme";
            case 2:
                return "Amoled theme";
        }
        return null;
    }

    public static String[] getThemeNames() {
        return new String[]{
                "Light theme",
                "Dark theme",
                "Amoled theme"
        };
    }
}
