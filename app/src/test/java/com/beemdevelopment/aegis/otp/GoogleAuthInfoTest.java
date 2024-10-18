package com.beemdevelopment.aegis.otp;

import static org.junit.Assert.assertThrows;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
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
