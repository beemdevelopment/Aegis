package com.beemdevelopment.aegis.crypto.otp;

import androidx.annotation.NonNull;

public class OTP {
    private static final String STEAM_ALPHABET = "23456789BCDFGHJKMNPQRTVWXY";

    private int _code;
    private int _digits;

    public OTP(int code, int digits) {
        _code = code;
        _digits = digits;
    }

    public int getCode() {
        return _code;
    }

    public int getDigits() {
        return _digits;
    }

    @NonNull
    @Override
    public String toString() {
        int code = _code % (int) Math.pow(10, _digits);

        // prepend zeroes if needed
        StringBuilder res = new StringBuilder(Long.toString(code));
        while (res.length() < _digits) {
            res.insert(0, "0");
        }

        return res.toString();
    }

    public String toSteamString() {
        int code = _code;
        StringBuilder res = new StringBuilder();

        for (int i = 0; i < _digits; i++) {
            char c = STEAM_ALPHABET.charAt(code % STEAM_ALPHABET.length());
            res.append(c);
            code /= STEAM_ALPHABET.length();
        }

        return res.toString();
    }
}
