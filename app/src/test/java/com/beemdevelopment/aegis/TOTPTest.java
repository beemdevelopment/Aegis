package com.beemdevelopment.aegis;

import org.junit.Test;

import com.beemdevelopment.aegis.crypto.otp.OTP;
import com.beemdevelopment.aegis.crypto.otp.TOTP;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.aegis.encoding.HexException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

public class TOTPTest {
    private static class Vector {
        public long Time;
        public String OTP;
        public String Algo;

        public Vector(long time, String otp, String algo) {
            Time = time;
            OTP = otp;
            Algo = algo;
        }
    }

    // https://tools.ietf.org/html/rfc6238#appendix-B
    private final Vector[] _vectors = {
            new Vector(59, "94287082", "HmacSHA1"),
            new Vector(59, "46119246", "HmacSHA256"),
            new Vector(59, "90693936", "HmacSHA512"),
            new Vector(1111111109, "07081804", "HmacSHA1"),
            new Vector(1111111109, "68084774", "HmacSHA256"),
            new Vector(1111111109, "25091201", "HmacSHA512"),
            new Vector(1111111111, "14050471", "HmacSHA1"),
            new Vector(1111111111, "67062674", "HmacSHA256"),
            new Vector(1111111111, "99943326", "HmacSHA512"),
            new Vector(1234567890, "89005924", "HmacSHA1"),
            new Vector(1234567890, "91819424", "HmacSHA256"),
            new Vector(1234567890, "93441116", "HmacSHA512"),
            new Vector(2000000000, "69279037", "HmacSHA1"),
            new Vector(2000000000, "90698825", "HmacSHA256"),
            new Vector(2000000000, "38618901", "HmacSHA512"),
            new Vector(20000000000L, "65353130", "HmacSHA1"),
            new Vector(20000000000L, "77737706", "HmacSHA256"),
            new Vector(20000000000L, "47863826", "HmacSHA512")
    };

    private final byte[] _seed = new byte[]{
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30
    };

    private final byte[] _seed32 = new byte[]{
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36,
            0x37, 0x38, 0x39, 0x30, 0x31, 0x32, 0x33, 0x34,
            0x35, 0x36, 0x37, 0x38, 0x39, 0x30, 0x31, 0x32
    };

    private final byte[] _seed64 = new byte[]{
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36,
            0x37, 0x38, 0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30, 0x31, 0x32,
            0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30, 0x31, 0x32, 0x33, 0x34
    };

    @Test
    public void vectorsMatch() throws NoSuchAlgorithmException, InvalidKeyException, HexException {
        for (Vector vector : _vectors) {
            byte[] seed;

            switch (vector.Algo) {
                case "HmacSHA1":
                    seed = _seed;
                    break;
                case "HmacSHA256":
                    seed = _seed32;
                    break;
                case "HmacSHA512":
                    seed = _seed64;
                    break;
                default:
                    fail("unsupported mode");
                    return;
            }

            OTP otp = TOTP.generateOTP(seed, vector.Algo, 8, 30, vector.Time);
            assertEquals(vector.OTP, otp.toString());
        }
    }
}
