package com.beemdevelopment.aegis.otp;

import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base32Exception;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Arrays;

public abstract class OtpInfo implements Serializable {
    private byte[] _secret;
    private String _algorithm;
    private int _digits;

    public OtpInfo(byte[] secret) throws OtpInfoException {
        this(secret, "SHA1", 6);
    }

    public OtpInfo(byte[] secret, String algorithm, int digits) throws OtpInfoException {
        setSecret(secret);
        setAlgorithm(algorithm);
        setDigits(digits);
    }

    public abstract String getOtp();

    public abstract String getType();

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("secret", new String(Base32.encode(getSecret())));
            obj.put("algo", getAlgorithm(false));
            obj.put("digits", getDigits());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    public byte[] getSecret() {
        return _secret;
    }

    public String getAlgorithm(boolean java) {
        if (java) {
            return "Hmac" + _algorithm;
        }
        return _algorithm;
    }

    public int getDigits() {
        return _digits;
    }

    public void setSecret(byte[] secret) {
        _secret = secret;
    }

    public static boolean isAlgorithmValid(String algorithm) {
        return algorithm.equals("SHA1") || algorithm.equals("SHA256") || algorithm.equals("SHA512");
    }

    public void setAlgorithm(String algorithm) throws OtpInfoException {
        if (algorithm.startsWith("Hmac")) {
            algorithm = algorithm.substring(4);
        }
        algorithm = algorithm.toUpperCase();

        if (!isAlgorithmValid(algorithm)) {
            throw new OtpInfoException(String.format("unsupported algorithm: %s", algorithm));
        }
        _algorithm = algorithm;
    }

    public static boolean isDigitsValid(int digits) {
        // allow a max of 10 digits, as truncation will only extract 31 bits
        return digits > 0 && digits <= 10;
    }

    public void setDigits(int digits) throws OtpInfoException {
        if (!isDigitsValid(digits)) {
            throw new OtpInfoException(String.format("unsupported amount of digits: %d", digits));
        }
        _digits = digits;
    }

    public static OtpInfo fromJson(String type, JSONObject obj) throws OtpInfoException {
        OtpInfo info;

        try {
            byte[] secret = Base32.decode(obj.getString("secret").toCharArray());
            String algo = obj.getString("algo");
            int digits = obj.getInt("digits");

            switch (type) {
                case TotpInfo.ID:
                    info = new TotpInfo(secret, algo, digits, obj.getInt("period"));
                    break;
                case SteamInfo.ID:
                    info = new SteamInfo(secret, algo, digits, obj.getInt("period"));
                    break;
                case HotpInfo.ID:
                    info = new HotpInfo(secret, algo, digits, obj.getLong("counter"));
                    break;
                default:
                    throw new OtpInfoException("unsupported otp type: " + type);
            }
        } catch (Base32Exception | JSONException e) {
            throw new OtpInfoException(e);
        }

        return info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OtpInfo)) {
            return false;
        }

        OtpInfo info = (OtpInfo) o;
        return getType().equals(info.getType())
                && Arrays.equals(getSecret(), info.getSecret())
                && getAlgorithm(false).equals(info.getAlgorithm(false))
                && getDigits() == info.getDigits();
    }
}
