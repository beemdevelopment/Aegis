package com.beemdevelopment.aegis.crypto.otp;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TOTP {

    private TOTP() {
    }

    public static OTP generateOTP(byte[] secret, String algo, int digits, long period, long seconds)
            throws InvalidKeyException, NoSuchAlgorithmException {
        long counter = (long) Math.floor((double) seconds / period);
        return HOTP.generateOTP(secret, algo, digits, counter);
    }

    public static OTP generateOTP(byte[] secret, String algo, int digits, long period)
            throws InvalidKeyException, NoSuchAlgorithmException {
        return generateOTP(secret, algo, digits, period, System.currentTimeMillis() / 1000);
    }
}
