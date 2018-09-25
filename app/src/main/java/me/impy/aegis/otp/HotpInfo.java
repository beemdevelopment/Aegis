package me.impy.aegis.otp;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import me.impy.aegis.crypto.otp.HOTP;

public class HotpInfo extends OtpInfo {
    private long _counter;

    public HotpInfo(byte[] secret) throws OtpInfoException {
        super(secret);
        setCounter(0);
    }

    public HotpInfo(byte[] secret, String algorithm, int digits, long counter) throws OtpInfoException {
        super(secret, algorithm, digits);
        setCounter(counter);
    }

    @Override
    public String getOtp() {
        try {
            return HOTP.generateOTP(getSecret(), getCounter(), getDigits(), false, -1);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public String getType() {
        return "hotp";
    }

    @Override
    public JSONObject toJson() {
        JSONObject obj = super.toJson();
        try {
            obj.put("counter", getCounter());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return obj;
    }

    public long getCounter() {
        return _counter;
    }

    public static boolean isCounterValid(long counter) {
        return counter >= 0;
    }

    public void setCounter(long counter) throws OtpInfoException {
        if (!isCounterValid(counter)) {
            throw new OtpInfoException(String.format("bad counter: %d", counter));
        }
        _counter = counter;
    }

    public void incrementCounter() throws OtpInfoException {
        setCounter(getCounter() + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HotpInfo)) {
            return false;
        }

        HotpInfo info = (HotpInfo) o;
        return super.equals(o) && getCounter() == info.getCounter();
    }
}
