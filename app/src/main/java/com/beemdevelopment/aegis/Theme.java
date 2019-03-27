package com.beemdevelopment.aegis;

public enum Theme {
    LIGHT,
    DARK,
    AMOLED;

    public static Theme fromInteger(int x) {
        switch(x) {
            case 0:
                return LIGHT;
            case 1:
                return DARK;
            case 2:
                return AMOLED;
        }
        return null;
    }
}
