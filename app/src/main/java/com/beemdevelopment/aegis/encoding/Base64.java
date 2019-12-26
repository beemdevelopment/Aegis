package com.beemdevelopment.aegis.encoding;

import com.google.common.io.BaseEncoding;

public class Base64 {
    private Base64() {

    }

    public static byte[] decode(String s) throws EncodingException {
        try {
            return BaseEncoding.base64().decode(s);
        } catch (IllegalArgumentException e) {
            throw new EncodingException(e);
        }
    }

    public static String encode(byte[] data) {
        return BaseEncoding.base64().encode(data);
    }
}
