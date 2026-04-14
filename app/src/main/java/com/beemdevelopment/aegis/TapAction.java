package com.beemdevelopment.aegis;

public enum TapAction {
    COPY,
    EDIT,
    NONE;

    private static TapAction[] _values;

    static {
        _values = values();
    }

    public static TapAction fromInteger(int x) {
        if (x < 0 || x >= _values.length) {
            return NONE;
        }

        return _values[x];
    }
}
