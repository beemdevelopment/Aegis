package com.beemdevelopment.aegis.otp;

import com.beemdevelopment.aegis.crypto.otp.OTP;
import com.beemdevelopment.aegis.crypto.otp.TOTP;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class SteamInfo extends TotpInfo {
    public static final String ID = "steam";
    public static final int DIGITS = 5;

    public SteamInfo(byte[] secret) throws OtpInfoException {
        super(secret, OtpInfo.DEFAULT_ALGORITHM, DIGITS, TotpInfo.DEFAULT_PERIOD);
    }

    public SteamInfo(byte[] secret, String algorithm, int digits, int period) throws OtpInfoException {
        super(secret, algorithm, digits, period);
    }

    @Override
    public String getOtp(long time) throws OtpInfoException {
        checkSecret();

        try {
            OTP otp = TOTP.generateOTP(getSecret(), getAlgorithm(true), getDigits(), getPeriod(), time);
            return otp.toSteamString();
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTypeId() {
        return ID;
    }

    @Override
    public String getType() {
        String id = getTypeId();
        return id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1);
    }
}
