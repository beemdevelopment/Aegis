package com.beemdevelopment.aegis.otp;

import com.beemdevelopment.aegis.crypto.otp.HOTPTest;
import com.beemdevelopment.aegis.crypto.otp.TOTPTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OtpTest {
    @Test
    public void testHotpInfoOtp() throws OtpInfoException {
        for (int i = 0; i < HOTPTest.VECTORS.length; i++) {
            HotpInfo info = new HotpInfo(HOTPTest.SECRET, "SHA1", 6, i);
            assertEquals(HOTPTest.VECTORS[i], info.getOtp());
        }
    }

    @Test
    public void testTotpInfoOtp() throws OtpInfoException {
        for (TOTPTest.Vector vector : TOTPTest.VECTORS) {
            byte[] seed = TOTPTest.getSeed(vector.Algo);
            TotpInfo info = new TotpInfo(seed, vector.Algo, 8, 30);
            assertEquals(vector.OTP, info.getOtp(vector.Time));
        }
    }
}
