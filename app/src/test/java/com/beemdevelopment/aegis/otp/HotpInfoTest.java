package com.beemdevelopment.aegis.otp;

import static org.junit.Assert.assertEquals;

import com.beemdevelopment.aegis.crypto.otp.HOTPTest;

import org.junit.Test;

public class HotpInfoTest {
    @Test
    public void testHotpInfoOtp() throws OtpInfoException {
        for (int i = 0; i < HOTPTest.VECTORS.length; i++) {
            HotpInfo info = new HotpInfo(HOTPTest.SECRET, OtpInfo.DEFAULT_ALGORITHM, OtpInfo.DEFAULT_DIGITS, i);
            assertEquals(HOTPTest.VECTORS[i], info.getOtp());
        }
    }
}
