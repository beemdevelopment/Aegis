package com.beemdevelopment.aegis.otp;

import static org.junit.Assert.assertEquals;

import com.beemdevelopment.aegis.crypto.otp.HOTPTest;

import org.junit.Test;

public class HotpInfoTest {
    @Test
    public void testHotpInfoOtp() throws OtpInfoException {
        for (int i = 0; i < HOTPTest.VECTORS.length; i++) {
            HotpInfo info = new HotpInfo(HOTPTest.SECRET, OtpInfo.DEFAULT_ALGORITHM, OtpInfo.DEFAULT_DIGITS, i);
            assertEquals(info.getOtp(), HOTPTest.VECTORS[i]);
        }
    }

    @Test
    public void testHotpMd5Override() throws OtpInfoException {
        final byte[] secret = new byte[]{1, 2, 3, 4};
        MotpInfo motpInfo = new MotpInfo(secret, "1234");
        motpInfo = (MotpInfo) OtpInfo.fromJson("motp", motpInfo.toJson());
        assertEquals("MD5", motpInfo.getAlgorithm(false));

        HotpInfo info = new HotpInfo(secret);
        info.setAlgorithm("MD5");
        info = (HotpInfo) OtpInfo.fromJson("hotp", info.toJson());
        assertEquals(OtpInfo.DEFAULT_ALGORITHM, info.getAlgorithm(false));

        info.setAlgorithm("SHA256");
        info = (HotpInfo) OtpInfo.fromJson("hotp", info.toJson());
        assertEquals("SHA256", info.getAlgorithm(false));
    }
}
