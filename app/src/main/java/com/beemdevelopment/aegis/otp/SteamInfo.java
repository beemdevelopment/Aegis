package com.beemdevelopment.aegis.otp;

import com.beemdevelopment.aegis.crypto.otp.OTP;
import com.beemdevelopment.aegis.crypto.otp.TOTP;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SteamInfo extends TotpInfo {
    public static final String ID = "steam";

    public SteamInfo(byte[] secret) throws OtpInfoException {
        super(secret, "SHA1", 5, 30);
    }

    public SteamInfo(byte[] secret, String algorithm, int digits, int period) throws OtpInfoException {
        super(secret, algorithm, digits, period);
    }

    @Override
    public String getOtp() {
        try {
            OTP otp = TOTP.generateOTP(getSecret(), getAlgorithm(true), getDigits(), getPeriod());
            return otp.toSteamString();
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getType() {
        return ID;
    }
}
