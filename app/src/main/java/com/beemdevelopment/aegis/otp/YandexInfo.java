package com.beemdevelopment.aegis.otp;

import com.beemdevelopment.aegis.crypto.otp.YAOTP;
import com.beemdevelopment.aegis.encoding.Base32;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;

public class YandexInfo extends TotpInfo {
    public static final String DEFAULT_ALGORITHM = "SHA256";
    public static final int DIGITS = 8;

    public static final int SECRET_LENGTH = 26;
    public static final int SECRET_FULL_LENGTH = 42;
    public static final String ID = "yandex";
    public static final String OTP_SCHEMA_ID = "yaotp";

    private byte[] _pin;

    public YandexInfo(byte[] secret) throws OtpInfoException {
        super(secret, DEFAULT_ALGORITHM, DIGITS, TotpInfo.DEFAULT_PERIOD);
    }

    public YandexInfo(byte[] secret, byte[] pin) throws OtpInfoException {
        super(secret, DEFAULT_ALGORITHM, DIGITS, TotpInfo.DEFAULT_PERIOD);
        this._pin = pin;
    }

    @Override
    public String getOtp() {
        try {
            YAOTP otp = YAOTP.generateOTP(getSecret(), _pin, getDigits(), getAlgorithm(true), getPeriod());
            return otp.toString();
        } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPin() {
        return _pin != null ? new String(_pin, StandardCharsets.UTF_8) : "";
    }

    public byte[] getPinBytes() {
        return _pin;
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
            result.put("pin", Base32.encode(getPinBytes()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof YandexInfo)) return false;

        YandexInfo that = (YandexInfo) o;
        return super.equals(o) && Arrays.equals(_pin, that._pin);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Arrays.hashCode(_pin);
    }
}
