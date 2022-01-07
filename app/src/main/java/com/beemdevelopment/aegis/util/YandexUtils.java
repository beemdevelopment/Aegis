package com.beemdevelopment.aegis.util;

import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.YandexInfo;

public class YandexUtils {
    private static final char CHECKSUM_POLY = 0b1_1000_1111_0011;
    public static final int APPROVED_SECRET_LENGTH = 16;

    private YandexUtils() {
    }

    private static int getNumberOfLeadingZeros(char value) {
        if (value == 0) return 16;

        int n = 0;
        if ((value & 0xFF00) == 0) {
            n += 8;
            value <<= 8;
        }
        if ((value & 0xF000) == 0) {
            n += 4;
            value <<= 4;
        }
        if ((value & 0xC000) == 0) {
            n += 2;
            value <<= 2;
        }
        if ((value & 0x8000) == 0) {
            n++;
        }

        return n;
    }

    /**
     * Java implementation of ChecksumIsValid
     * from https://github.com/norblik/KeeYaOtp/blob/dev/KeeYaOtp/Core/Secret.cs
     */
    public static void validateSecret(byte[] secret) throws OtpInfoException {
        /*
            When secret comes from QR code - we can't test it,
            cause it's only 16 byte long.
         */
        if (secret.length == APPROVED_SECRET_LENGTH) return;

        if (secret.length != YandexInfo.SECRET_LENGTH)
            throw new OtpInfoException("Wrong secret size");

        char originalChecksum = (char) ((secret[secret.length - 2] & 0x0F) << 8 | secret[secret.length - 1] & 0xff);

        char accum = 0;
        int accumBits = 0;

        int inputTotalBitsAvailable = secret.length * 8 - 12;
        int inputIndex = 0;
        int inputBitsAvailable = 8;

        while (inputTotalBitsAvailable > 0) {
            int requiredBits = 13 - accumBits;
            if (inputTotalBitsAvailable < requiredBits) requiredBits = inputTotalBitsAvailable;

            while (requiredBits > 0) {
                int curInput = (secret[inputIndex] & (1 << inputBitsAvailable) - 1) & 0xff;
                int bitsToRead = Math.min(requiredBits, inputBitsAvailable);

                curInput >>= inputBitsAvailable - bitsToRead;
                accum = (char) (accum << bitsToRead | curInput);

                inputTotalBitsAvailable -= bitsToRead;
                requiredBits -= bitsToRead;
                inputBitsAvailable -= bitsToRead;
                accumBits += bitsToRead;

                if (inputBitsAvailable == 0) {
                    inputIndex += 1;
                    inputBitsAvailable = 8;
                }
            }

            if (accumBits == 13) accum ^= CHECKSUM_POLY;
            accumBits = 16 - getNumberOfLeadingZeros(accum);
        }

        if (accum != originalChecksum) {
            throw new OtpInfoException("Secret is corrupted. Checksum is not valid");
        }
    }
}
