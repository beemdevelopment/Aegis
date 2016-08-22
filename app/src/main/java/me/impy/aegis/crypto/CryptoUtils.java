package me.impy.aegis.crypto;

import java.util.Arrays;

public class CryptoUtils {
    private CryptoUtils() {
    }

    public static void zero(char[] data) {
        Arrays.fill(data, '\0');
    }

    public static void zero(byte[] data) {
        Arrays.fill(data, (byte)0);
    }
}
