package com.beemdevelopment.aegis.otp;

import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GoogleAuthInfoTest {
    @Test
    public void testGoogleAuthInfoEmptySecret() throws GoogleAuthInfoException {
        String uri = "otpauth://totp/test:test?secret=%s&algo=SHA1&digits=6&period=30";
        GoogleAuthInfo.parseUri(String.format(uri, "AA"));
        assertThrows(GoogleAuthInfoException.class, () -> GoogleAuthInfo.parseUri(String.format(uri, "")));
    }

    @Test
    public void testOtpInfoEmptySecret() throws OtpInfoException {
        OtpInfo info = new TotpInfo(new byte[0]);
        assertThrows(OtpInfoException.class, info::getOtp);
        info = new HotpInfo(new byte[0]);
        assertThrows(OtpInfoException.class, info::getOtp);
        info = new SteamInfo(new byte[0]);
        assertThrows(OtpInfoException.class, info::getOtp);
    }
}
