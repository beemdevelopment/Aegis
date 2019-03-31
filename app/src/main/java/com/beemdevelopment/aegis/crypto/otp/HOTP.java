package com.beemdevelopment.aegis.crypto.otp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HOTP {
    private HOTP() {
    }

    public static OTP generateOTP(byte[] secret, String algo, int digits, long counter)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(secret, "RAW");

        // encode counter in big endian
        byte[] counterBytes = ByteBuffer.allocate(8)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(counter)
                .array();

        // calculate the hash of the counter
        Mac mac = Mac.getInstance(algo);
        mac.init(key);
        byte[] hash = mac.doFinal(counterBytes);

        // truncate hash to get the HTOP value
        // http://tools.ietf.org/html/rfc4226#section-5.4
        int offset = hash[hash.length - 1] & 0xf;
        int otp = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);

        return new OTP(otp, digits);
    }
}
