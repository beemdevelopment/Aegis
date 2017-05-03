package me.impy.aegis;

import org.junit.Test;

import me.impy.aegis.crypto.otp.TOTP;

import static org.junit.Assert.*;

public class TOTPTest {
    private class testVector {
        public String Time;
        public String Mode;
        public String OTP;
    }

    // https://tools.ietf.org/html/rfc6238#appendix-B
    private final testVector[] _vectors = {
            new testVector(){{ Time = "0000000000000001"; OTP = "94287082"; Mode = "HmacSHA1"; }},
            new testVector(){{ Time = "0000000000000001"; OTP = "46119246"; Mode = "HmacSHA256"; }},
            new testVector(){{ Time = "0000000000000001"; OTP = "90693936"; Mode = "HmacSHA512"; }},
            new testVector(){{ Time = "00000000023523EC"; OTP = "07081804"; Mode = "HmacSHA1"; }},
            new testVector(){{ Time = "00000000023523EC"; OTP = "68084774"; Mode = "HmacSHA256"; }},
            new testVector(){{ Time = "00000000023523EC"; OTP = "25091201"; Mode = "HmacSHA512"; }},
            new testVector(){{ Time = "00000000023523ED"; OTP = "14050471"; Mode = "HmacSHA1"; }},
            new testVector(){{ Time = "00000000023523ED"; OTP = "67062674"; Mode = "HmacSHA256"; }},
            new testVector(){{ Time = "00000000023523ED"; OTP = "99943326"; Mode = "HmacSHA512"; }},
            new testVector(){{ Time = "000000000273EF07"; OTP = "89005924"; Mode = "HmacSHA1"; }},
            new testVector(){{ Time = "000000000273EF07"; OTP = "91819424"; Mode = "HmacSHA256"; }},
            new testVector(){{ Time = "000000000273EF07"; OTP = "93441116"; Mode = "HmacSHA512"; }},
            new testVector(){{ Time = "0000000003F940AA"; OTP = "69279037"; Mode = "HmacSHA1"; }},
            new testVector(){{ Time = "0000000003F940AA"; OTP = "90698825"; Mode = "HmacSHA256"; }},
            new testVector(){{ Time = "0000000003F940AA"; OTP = "38618901"; Mode = "HmacSHA512"; }},
            new testVector(){{ Time = "0000000027BC86AA"; OTP = "65353130"; Mode = "HmacSHA1"; }},
            new testVector(){{ Time = "0000000027BC86AA"; OTP = "77737706"; Mode = "HmacSHA256"; }},
            new testVector(){{ Time = "0000000027BC86AA"; OTP = "47863826"; Mode = "HmacSHA512"; }}
    };

    private final byte[] _seed = new byte[] { 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
            0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30
    };
    private final byte[] _seed32 = new byte[] { 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30, 0x31, 0x32,
            0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30, 0x31, 0x32
    };
    private final byte[] _seed64 = new byte[] { 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30, 0x31, 0x32,
            0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36,
            0x37, 0x38, 0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30,
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30, 0x31, 0x32, 0x33, 0x34
    };

    @Test
    public void vectors_match() throws Exception {
        for (testVector v : _vectors) {
            byte[] seed;

            switch (v.Mode) {
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

            String otp = TOTP.generateTOTP(seed, v.Time, 8, v.Mode);
            assertEquals(v.OTP, otp);
        }
    }
}
