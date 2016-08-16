package me.impy.aegis.crypto;

import android.net.Uri;

import java.io.Serializable;

import me.impy.aegis.encoding.Base32;

public class KeyInfo implements Serializable {
    private String type;
    private String label;
    private byte[] secret;
    private String issuer;
    private String algorithm = "HmacSHA1";
    private int digits = 6;
    private long counter;
    private int period = 30;

    public String getType() {
        return type;
    }
    public String getLabel() {
        return label;
    }
    public byte[] getSecret() {
        return secret;
    }
    public String getIssuer() {
        return issuer;
    }
    public String getAlgorithm() {
        return algorithm;
    }
    public int getDigits() {
        return digits;
    }
    public long getCounter() {
        return counter;
    }
    public int getPeriod() {
        return period;
    }

    private KeyInfo() { }

    public static KeyInfo FromURL(String s) throws Exception {
        final Uri url = Uri.parse(s);
        if (!url.getScheme().equals("otpauth")) {
            throw new Exception("unsupported protocol");
        }

        KeyInfo info = new KeyInfo();

        // only 'totp' and 'hotp' are supported
        info.type = url.getHost();
        if (info.type.equals("totp") && info.type.equals("hotp")) {
            throw new Exception("unsupported type");
        }

        // 'secret' is a required parameter
        String secret = url.getQueryParameter("secret");
        if (secret == null) {
            throw new Exception("'secret' is not set");
        }
        info.secret = Base32.decode(secret);

        // provider info used to disambiguate accounts
        // these parameters are not required but I don't want them to be null either
        String issuer = url.getQueryParameter("issuer");
        String label = url.getPath();
        info.issuer = issuer != null ? issuer : "";
        info.label = label != null ? label : "";

        // just use the defaults if these parameters aren't set
        String algorithm = url.getQueryParameter("algorithm");
        if (algorithm != null) {
            info.algorithm = "Hmac" + algorithm;
        }
        String period = url.getQueryParameter("period");
        if (period != null) {
            info.period = Integer.getInteger(period);
        }
        String digits = url.getQueryParameter("digits");
        if (digits != null) {
            info.digits = Integer.getInteger(digits);
        }

        // 'counter' is required if the type is 'hotp'
        String counter = url.getQueryParameter("counter");
        if (counter != null) {
            info.counter = Long.getLong(counter);
        } else if (info.type.equals("hotp")) {
            throw new Exception("'counter' was not set which is required for 'hotp'");
        }

        return info;
    }
}
