package com.beemdevelopment.aegis;

public enum Theme {
    LIGHT,
    DARK,
    AMOLED;

    private static Theme[] _values;

    static {
        _values = values();
    }

    public static Theme fromInteger(int x) {
        return _values[x];
    }
}
