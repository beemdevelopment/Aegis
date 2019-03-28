package com.beemdevelopment.aegis.encoding;

import java.nio.charset.StandardCharsets;

public class Base64 {
    private static final int _flags = android.util.Base64.NO_WRAP;

    private Base64() {

    }

    public static byte[] decode(String s) throws Base64Exception {
        try {
            return android.util.Base64.decode(s, _flags);
        } catch (IllegalArgumentException e) {
            throw new Base64Exception(e);
        }
    }

    public static String encode(byte[] data) {
        byte[] encoded = android.util.Base64.encode(data, _flags);
        return new String(encoded, StandardCharsets.UTF_8);
    }
}
