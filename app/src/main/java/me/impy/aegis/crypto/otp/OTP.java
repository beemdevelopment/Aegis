package me.impy.aegis.crypto.otp;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import me.impy.aegis.crypto.KeyInfo;

public class OTP {
    private OTP() {
    }

    public static String generateOTP(KeyInfo info) throws InvalidKeyException, NoSuchAlgorithmException {
        String otp;

        switch (info.getType()) {
            case "totp":
                String time = Long.toHexString(System.currentTimeMillis() / 1000 / info.getPeriod());
                otp = TOTP.generateTOTP(info.getSecret(), time, info.getDigits(), info.getAlgorithm());
                break;
            case "hotp":
                otp = HOTP.generateOTP(info.getSecret(), info.getCounter(), info.getDigits(), false, -1);
                break;
            default:
                throw new RuntimeException();
        }

        return otp;
    }
}
