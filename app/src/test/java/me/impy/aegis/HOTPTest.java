package me.impy.aegis;

import org.junit.Test;

import me.impy.aegis.crypto.HOTP;

import static org.junit.Assert.*;

public class HOTPTest {
    private class testVector {
        public long Count;
        public String OTP;
    }

    // https://tools.ietf.org/html/rfc4226#page-32
    private final testVector[] _vectors = {
            new testVector(){{ Count = 0; OTP = "755224"; }},
            new testVector(){{ Count = 1; OTP = "287082"; }},
            new testVector(){{ Count = 2; OTP = "359152"; }},
            new testVector(){{ Count = 3; OTP = "969429"; }},
            new testVector(){{ Count = 4; OTP = "338314"; }},
            new testVector(){{ Count = 5; OTP = "254676"; }},
            new testVector(){{ Count = 6; OTP = "287922"; }},
            new testVector(){{ Count = 7; OTP = "162583"; }},
            new testVector(){{ Count = 8; OTP = "399871"; }},
            new testVector(){{ Count = 9; OTP = "520489"; }},
    };

    private final byte[] _secret = new byte[] {0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30};

    @Test
    public void vectors_match() throws Exception {
        for (testVector v : _vectors) {
            String otp = HOTP.generateOTP(_secret, v.Count, 6, false, -1);
            assertEquals(v.OTP, otp);
        }
    }
}
