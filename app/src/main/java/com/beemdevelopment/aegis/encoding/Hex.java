package com.beemdevelopment.aegis.encoding;

import com.google.common.io.BaseEncoding;

public class Hex {
    private Hex() {

    }

    public static byte[] decode(String s) throws EncodingException {
        try {
            return BaseEncoding.base16().decode(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new EncodingException(e);
        }
    }

    public static String encode(byte[] data) {
        return BaseEncoding.base16().lowerCase().encode(data);
    }
}
