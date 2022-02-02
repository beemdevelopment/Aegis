package com.beemdevelopment.aegis.encoding;

import com.google.common.io.BaseEncoding;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class Base32 {
    private Base32() {

    }

    public static byte[] decode(String s) throws EncodingException {
        try {
            return BaseEncoding.base32().decode(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new EncodingException(e);
        }
    }

    public static String encode(byte[] data) {
        return BaseEncoding.base32().omitPadding().encode(data);
    }

    public static String encode(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        return encode(bytes);
    }
}
