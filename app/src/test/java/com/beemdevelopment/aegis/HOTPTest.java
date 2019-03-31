package com.beemdevelopment.aegis;

import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import com.beemdevelopment.aegis.crypto.otp.HOTP;
import com.beemdevelopment.aegis.crypto.otp.OTP;

import static org.junit.Assert.*;

public class HOTPTest {
    // https://tools.ietf.org/html/rfc4226#page-32
    private final String[] _vectors = {
            "755224", "287082",
            "359152", "969429",
            "338314", "254676",
            "287922", "162583",
            "399871", "520489"
    };

    private final byte[] _secret = new byte[]{
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30
    };

    @Test
    public void vectorsMatch() throws InvalidKeyException, NoSuchAlgorithmException {
        for (int i = 0; i < _vectors.length; i++) {
            OTP otp = HOTP.generateOTP(_secret, "HmacSHA1", 6, i);
            assertEquals(_vectors[i], otp.toString());
        }
    }
}
