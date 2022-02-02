package com.beemdevelopment.aegis.crypto.otp;

import androidx.annotation.NonNull;

import com.beemdevelopment.aegis.util.YandexUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class YAOTP {
    private static final int EN_ALPHABET_LENGTH = 26;
    private final long _code;
    private final int _digits;

    private YAOTP(long code, int digits) {
        _code = code;
        _digits = digits;
    }

    public static YAOTP generateOTP(byte[] secret, byte[] pin, int digits, String otpAlgo, long period)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        long seconds = System.currentTimeMillis() / 1000;
        return generateOTP(secret, pin, digits, otpAlgo, seconds, period);
    }

    public static YAOTP generateOTP(byte[] secret, byte[] pin, int digits, String otpAlgo, long seconds, long period)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException {

        long counter = (long) Math.floor((double) seconds / period);

        try (ByteArrayOutputStream pinWithHashStream =
                     new ByteArrayOutputStream(pin.length + secret.length)) {

            pinWithHashStream.write(pin);
            pinWithHashStream.write(secret, 0, YandexUtils.APPROVED_SECRET_LENGTH);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] keyHash = md.digest(pinWithHashStream.toByteArray());

            if (keyHash[0] == 0) {
                keyHash = Arrays.copyOfRange(keyHash, 1, keyHash.length);
            }

            byte[] periodHash = HOTP.getHash(keyHash, otpAlgo, counter);
            int offset = periodHash[periodHash.length - 1] & 0xf;

            periodHash[offset] &= 0x7f;
            long otp = ByteBuffer.wrap(periodHash)
                    .order(ByteOrder.BIG_ENDIAN)
                    .getLong(offset);

            return new YAOTP(otp, digits);
        }
    }

    public long getCode() {
        return _code;
    }

    public int getDigits() {
        return _digits;
    }

    @NonNull
    @Override
    public String toString() {
        long code = _code % (long) Math.pow(EN_ALPHABET_LENGTH, _digits);
        char[] chars = new char[_digits];

        for (int i = _digits - 1; i >= 0; i--) {
            chars[i] = (char) ('a' + (code % EN_ALPHABET_LENGTH));
            code /= EN_ALPHABET_LENGTH;
        }

        return new String(chars);
    }
}
