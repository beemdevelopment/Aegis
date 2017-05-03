package me.impy.aegis.crypto.otp;

import me.impy.aegis.crypto.KeyInfo;

public class OTP {
    private OTP() {
    }

    public static String generateOTP(KeyInfo info) throws Exception {
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
                // this should never happen
                throw new Exception("unsupported type");
        }

        return otp;
    }
}
