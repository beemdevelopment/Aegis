package com.beemdevelopment.aegis.encoding;

import com.google.common.io.BaseEncoding;

public class Base32 {
    private static final BaseEncoding _encoding = BaseEncoding.base32().omitPadding();

    private Base32() {

    }

    public static byte[] decode(String s) throws EncodingException {
        try {
            return _encoding.decode(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new EncodingException(e);
        }
    }

    public static String encode(byte[] data) {
        return _encoding.encode(data);
    }
}
