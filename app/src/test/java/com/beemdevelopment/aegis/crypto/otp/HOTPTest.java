package com.beemdevelopment.aegis.crypto.otp;

import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class HOTPTest {
    // https://tools.ietf.org/html/rfc4226#page-32
    public static final String[] VECTORS = {
            "755224", "287082",
            "359152", "969429",
            "338314", "254676",
            "287922", "162583",
            "399871", "520489"
    };

    public static final byte[] SECRET = new byte[]{
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30
    };

    @Test
    public void vectorsMatch() throws InvalidKeyException, NoSuchAlgorithmException {
        for (int i = 0; i < VECTORS.length; i++) {
            OTP otp = HOTP.generateOTP(SECRET, "HmacSHA1", 6, i);
            assertEquals(VECTORS[i], otp.toString());
        }
    }
}
