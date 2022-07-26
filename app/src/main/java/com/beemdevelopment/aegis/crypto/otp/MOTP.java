package com.beemdevelopment.aegis.crypto.otp;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.beemdevelopment.aegis.encoding.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MOTP {
    private final String _code;
    private final int _digits;

    private MOTP(String code, int digits) {
        _code = code;
        _digits = digits;
    }

    @NonNull
    public static MOTP generateOTP(byte[] secret, String algo, int digits, int period, String pin)
            throws NoSuchAlgorithmException {

        return generateOTP(secret, algo, digits, period, pin, System.currentTimeMillis() / 1000);
    }

    @NonNull
    public static MOTP generateOTP(byte[] secret, String algo, int digits, int period, String pin, long time)
            throws NoSuchAlgorithmException {

        long timeBasedCounter = time / period;
        String secretAsString = Hex.encode(secret);
        String toDigest =  timeBasedCounter + secretAsString + pin;
        String code = getDigest(algo, toDigest.getBytes(StandardCharsets.UTF_8));

        return new MOTP(code, digits);
    }

    @VisibleForTesting
    @NonNull
    protected static String getDigest(String algo, byte[] toDigest) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algo);
        byte[] digest = md.digest(toDigest);

        return Hex.encode(digest);
    }

    @NonNull
    @Override
    public String toString() {
        return _code.substring(0, _digits);
    }
}