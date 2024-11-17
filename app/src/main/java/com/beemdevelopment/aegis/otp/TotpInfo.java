package com.beemdevelopment.aegis.otp;

import com.beemdevelopment.aegis.crypto.otp.OTP;
import com.beemdevelopment.aegis.crypto.otp.TOTP;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TotpInfo extends OtpInfo {
    public static final String ID = "totp";
    public static final int DEFAULT_PERIOD = 30;

    private int _period;

    public TotpInfo(byte[] secret) throws OtpInfoException {
        super(secret);
        setPeriod(DEFAULT_PERIOD);
    }

    public TotpInfo(byte[] secret, String algorithm, int digits, int period) throws OtpInfoException {
        super(secret, algorithm, digits);
        setPeriod(period);
    }

    @Override
    public String getOtp() throws OtpInfoException {
        return getOtp(System.currentTimeMillis() / 1000);
    }

    public String getOtp(long time) throws OtpInfoException {
        checkSecret();

        try {
            OTP otp = TOTP.generateOTP(getSecret(), getAlgorithm(true), getDigits(), getPeriod(), time);
            return otp.toString();
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTypeId() {
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
        if (period <= 0) {
            return false;
        }

        // check for the possibility of an overflow when converting to milliseconds
        return period <= Integer.MAX_VALUE / 1000;
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
