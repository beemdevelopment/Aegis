package com.beemdevelopment.aegis.otp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beemdevelopment.aegis.crypto.otp.MOTP;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class MotpInfo extends TotpInfo {
    public static final String ID = "motp";
    public static final String SCHEME = "motp";
    public static final String ALGORITHM = "MD5";

    public static final int PERIOD = 10;
    public static final int DIGITS = 6;

    private String _pin;

    public MotpInfo(@NonNull byte[] secret) throws OtpInfoException {
        this(secret, null);
    }

    public MotpInfo(byte[] secret, String pin) throws OtpInfoException {
        super(secret, ALGORITHM, DIGITS, PERIOD);
        setPin(pin);
    }

    @Override
    public String getOtp(long time) {
        if (_pin == null) {
            throw new IllegalStateException("PIN must be set before generating an OTP");
        }

        try {
            MOTP otp = MOTP.generateOTP(getSecret(), getAlgorithm(false), getDigits(), getPeriod(), getPin(), time);
            return otp.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTypeId() {
        return ID;
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

    @Nullable
    public String getPin() {
        return _pin;
    }

    public void setPin(@NonNull String pin) {
        this._pin = pin;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MotpInfo)) {
            return false;
        }

        MotpInfo info = (MotpInfo) o;
        return super.equals(o) && Objects.equals(getPin(), info.getPin());
    }
}
