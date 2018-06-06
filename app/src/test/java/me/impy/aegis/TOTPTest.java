package me.impy.aegis;

import org.junit.Test;

import me.impy.aegis.crypto.otp.TOTP;

import static org.junit.Assert.*;

public class TOTPTest {
    // https://tools.ietf.org/html/rfc6238#appendix-B
    private final String[][] _vectors = {
            // time, OPT, algorithm
            {"0000000000000001", "94287082", "HmacSHA1"},
            {"0000000000000001", "46119246", "HmacSHA256"},
            {"0000000000000001", "90693936", "HmacSHA512"},
            {"00000000023523EC", "07081804", "HmacSHA1"},
            {"00000000023523EC", "68084774", "HmacSHA256"},
            {"00000000023523EC", "25091201", "HmacSHA512"},
            {"00000000023523ED", "14050471", "HmacSHA1"},
            {"00000000023523ED", "67062674", "HmacSHA256"},
            {"00000000023523ED", "99943326", "HmacSHA512"},
            {"000000000273EF07", "89005924", "HmacSHA1"},
            {"000000000273EF07", "91819424", "HmacSHA256"},
            {"000000000273EF07", "93441116", "HmacSHA512"},
            {"0000000003F940AA", "69279037", "HmacSHA1"},
            {"0000000003F940AA", "90698825", "HmacSHA256"},
            {"0000000003F940AA", "38618901", "HmacSHA512"},
            {"0000000027BC86AA", "65353130", "HmacSHA1"},
            {"0000000027BC86AA", "77737706", "HmacSHA256"},
            {"0000000027BC86AA", "47863826", "HmacSHA512"}
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
    public void vectorsMatch() {
        for (String[] vector : _vectors) {
            byte[] seed;

            switch (vector[2]) {
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

            String otp = TOTP.generateTOTP(seed, vector[0], 8, vector[2]);
            assertEquals(vector[1], otp);
        }
    }
}
