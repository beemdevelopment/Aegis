package me.impy.aegis;

import org.junit.Test;

import me.impy.aegis.crypto.TOTP;

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

    private final String _seed = "3132333435363738393031323334353637383930";
    private final String _seed32 = "3132333435363738393031323334353637383930313233343536373839303132";
    private final String _seed64 = "31323334353637383930313233343536373839303132333435363738393031323334353637383930313233343536373839303132333435363738393031323334";

    @Test
    public void vectors_match() throws Exception {
        for (testVector v : _vectors) {
            String seed;

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

            String otp = TOTP.generateTOTP(seed, v.Time, "8", v.Mode);
            assertEquals(v.OTP, otp);
        }
    }
}
