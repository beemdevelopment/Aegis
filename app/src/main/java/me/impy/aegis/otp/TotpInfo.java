package me.impy.aegis.otp;

import org.json.JSONException;
import org.json.JSONObject;

import me.impy.aegis.crypto.otp.TOTP;

public class TotpInfo extends OtpInfo {
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
        String time = Long.toHexString(System.currentTimeMillis() / 1000 / getPeriod());
        return TOTP.generateTOTP(getSecret(), time, getDigits(), getAlgorithm(true));
    }

    @Override
    public String getType() {
        return "totp";
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
}
