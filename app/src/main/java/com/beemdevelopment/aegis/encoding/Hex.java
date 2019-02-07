package com.beemdevelopment.aegis.encoding;

// The hexadecimal utility functions in this file were taken and modified from: http://www.docjar.com/html/api/com/sun/xml/internal/bind/DatatypeConverterImpl.java.html
// It is licensed under GPLv2 with a classpath exception.
public class Hex {
    private Hex() {
    }

    private static int hexToBin(char ch) {
        if ('0' <= ch && ch <= '9') return ch - '0';
        if ('A' <= ch && ch <= 'F') return ch - 'A' + 10;
        if ('a' <= ch && ch <= 'f') return ch - 'a' + 10;
        return -1;
    }

    private static final char[] hexCode = "0123456789abcdef".toCharArray();

    public static byte[] decode(String s) throws HexException {
        final int len = s.length();

        if (len % 2 != 0)
            throw new HexException("hexBinary needs to be even-length: " + s);

        byte[] out = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            int h = hexToBin(s.charAt(i));
            int l = hexToBin(s.charAt(i + 1));
            if (h == -1 || l == -1)
                throw new HexException("contains illegal character for hexBinary: " + s);

            out[i / 2] = (byte) (h * 16 + l);
        }

        return out;
    }

    public static String encode(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        return r.toString();
    }
}
