package com.beemdevelopment.aegis.encoding;

import com.google.common.io.BaseEncoding;

public class Base32 {
    private Base32() {

    }

    public static byte[] decode(String s) throws EncodingException {
        try {
            return BaseEncoding.base32().decode(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new EncodingException(e);
        }
    }

    public static String encode(byte[] data) {
        return BaseEncoding.base32().omitPadding().encode(data);
    }
}
