package com.beemdevelopment.aegis.otp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beemdevelopment.aegis.crypto.otp.YAOTP;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class YandexInfo extends TotpInfo {
    public static final String DEFAULT_ALGORITHM = "SHA256";
    public static final int DIGITS = 8;

    public static final int SECRET_LENGTH = 16;
    public static final int SECRET_FULL_LENGTH = 26;
    public static final String ID = "yandex";
    public static final String HOST_ID = "yaotp";

    @Nullable
    private String _pin;

    public YandexInfo(@NonNull byte[] secret) throws OtpInfoException {
        this(secret, null);
    }

    public YandexInfo(@NonNull byte[] secret, @Nullable String pin) throws OtpInfoException {
        super(secret, DEFAULT_ALGORITHM, DIGITS, TotpInfo.DEFAULT_PERIOD);
        setSecret(parseSecret(secret));
        _pin = pin;
    }

    @Override
    public String getOtp(long time) {
        if (_pin == null) {
            throw new IllegalStateException("PIN must be set before generating an OTP");
        }

        try {
            YAOTP otp = YAOTP.generateOTP(getSecret(), getPin(), getDigits(), getAlgorithm(true), getPeriod(), time);
            return otp.toString();
        } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public String getPin() {
        return _pin;
    }

    public void setPin(@NonNull String pin) {
        _pin = pin;
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

    @Override
    public JSONObject toJson() {
        JSONObject result = super.toJson();
        try {
            result.put("pin", getPin());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof YandexInfo)) {
            return false;
        }

        YandexInfo info = (YandexInfo) o;
        return super.equals(o) && Objects.equals(getPin(), info.getPin());
    }

    public static byte[] parseSecret(byte[] secret) throws OtpInfoException {
        validateSecret(secret);

        if (secret.length != SECRET_LENGTH) {
            return Arrays.copyOfRange(secret, 0, SECRET_LENGTH);
        }

        return secret;
    }

    /**
     * Java implementation of ChecksumIsValid
     * From: https://github.com/norblik/KeeYaOtp/blob/188a1a99f13f82e4ef8df8a1b9b9351ba236e2a1/KeeYaOtp/Core/Secret.cs
     * License: GPLv3+
     */
    public static void validateSecret(byte[] secret) throws OtpInfoException {
        if (secret.length != SECRET_LENGTH && secret.length != SECRET_FULL_LENGTH) {
            throw new OtpInfoException(String.format("Invalid Yandex secret length: %d bytes", secret.length));
        }

        // Secrets originating from a QR code do not have a checksum, so we assume those are valid
        if (secret.length == SECRET_LENGTH) {
            return;
        }

        char originalChecksum = (char) ((secret[secret.length - 2] & 0x0F) << 8 | secret[secret.length - 1] & 0xff);

        char accum = 0;
        int accumBits = 0;

        int inputTotalBitsAvailable = secret.length * 8 - 12;
        int inputIndex = 0;
        int inputBitsAvailable = 8;

        while (inputTotalBitsAvailable > 0) {
            int requiredBits = 13 - accumBits;
            if (inputTotalBitsAvailable < requiredBits) {
                requiredBits = inputTotalBitsAvailable;
            }

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

            if (accumBits == 13) {
                accum ^= 0b1_1000_1111_0011;
            }
            accumBits = 16 - getNumberOfLeadingZeros(accum);
        }

        if (accum != originalChecksum) {
            throw new OtpInfoException("Yandex secret checksum invalid");
        }
    }

    private static int getNumberOfLeadingZeros(char value) {
        if (value == 0) {
            return 16;
        }

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
}
