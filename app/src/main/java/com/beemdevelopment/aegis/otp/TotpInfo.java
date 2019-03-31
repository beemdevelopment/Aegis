package com.beemdevelopment.aegis.otp;

import com.beemdevelopment.aegis.crypto.otp.OTP;
import com.beemdevelopment.aegis.crypto.otp.TOTP;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TotpInfo extends OtpInfo {
    public static final String ID = "totp";

    private int _period;

    public TotpInfo(byte[] secret) throws OtpInfoException {
        super(secret);
        setPeriod(30);
    }

    public TotpInfo(byte[] secret, String algorithm, int digits, int period) throws OtpInfoException {
        super(secret, algorithm, digits);
        setPeriod(period);
    }

    @Override
    public String getOtp() {
        try {
            OTP otp = TOTP.generateOTP(getSecret(), getAlgorithm(true), getDigits(), getPeriod());
            return otp.toString();
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getType() {
        return ID;
    }

    @Override
    public JSONObject toJson() {
        JSONObject obj = super.toJson();
        try {
            obj.put("period", getPeriod());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return obj;
    }

    public int getPeriod() {
        return _period;
    }

    public static boolean isPeriodValid(int period) {
        return period > 0;
    }

    public void setPeriod(int period) throws OtpInfoException {
        if (!isPeriodValid(period)) {
            throw new OtpInfoException(String.format("bad period: %d", period));
        }
        _period = period;
    }

    public long getMillisTillNextRotation() {
        return TotpInfo.getMillisTillNextRotation(_period);
    }

    public static long getMillisTillNextRotation(int period) {
        long p = period * 1000;
        return p - (System.currentTimeMillis() % p);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TotpInfo)) {
            return false;
        }

        TotpInfo info = (TotpInfo) o;
        return super.equals(o) && getPeriod() == info.getPeriod();
    }
}
