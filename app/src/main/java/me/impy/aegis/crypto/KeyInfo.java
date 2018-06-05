package me.impy.aegis.crypto;

import android.net.Uri;

import java.io.Serializable;
import java.util.Arrays;

import me.impy.aegis.encoding.Base32;
import me.impy.aegis.encoding.Base32Exception;

public class KeyInfo implements Serializable {
    private String _type = "totp";
    private byte[] _secret;
    private String _accountName = "";
    private String _issuer = "";
    private long _counter = 0;
    private String _algorithm = "SHA1";
    private int _digits = 6;
    private int _period = 30;

    public String getURL() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("otpauth");
        builder.authority(_type);

        builder.appendQueryParameter("digits", Integer.toString(_digits));
        builder.appendQueryParameter("period", Integer.toString(_period));
        builder.appendQueryParameter("algorithm", _algorithm);
        builder.appendQueryParameter("secret", new String(Base32.encode(_secret)));
        if (_type.equals("hotp")) {
            builder.appendQueryParameter("counter", Long.toString(_counter));
        }

        if (_issuer != null && !_issuer.equals("")) {
            builder.path(String.format("%s:%s", _issuer, _accountName));
            builder.appendQueryParameter("issuer", _issuer);
        } else {
            builder.path(_accountName);
        }

        return builder.build().toString();
    }

    public long getMillisTillNextRotation() {
        return KeyInfo.getMillisTillNextRotation(_period);
    }

    public static long getMillisTillNextRotation(int period) {
        long p = period * 1000;
        return p - (System.currentTimeMillis() % p);
    }

    public static KeyInfo fromURL(String s) throws KeyInfoException {
        final Uri url = Uri.parse(s);
        if (!url.getScheme().equals("otpauth")) {
            throw new KeyInfoException("unsupported protocol");
        }

        KeyInfo info = new KeyInfo();
        info.setType(url.getHost());

        // 'secret' is a required parameter
        String secret = url.getQueryParameter("secret");
        if (secret == null) {
            throw new KeyInfoException("'secret' is not set");
        }
        info.setSecret(secret.toCharArray());

        // provider info used to disambiguate accounts
        String path = url.getPath();
        String label = path != null && path.length() > 0 ? path.substring(1) : "";

        if (label.contains(":")) {
            // a label can only contain one colon
            // it's ok to fail if that's not the case
            String[] strings = label.split(":");

            if (strings.length == 2) {
                info.setIssuer(strings[0]);
                info.setAccountName(strings[1]);
            } else {
                // at this point, just dump the whole thing into the accountName
                info.setAccountName(label);
            }
        } else {
            // label only contains the account name
            // grab the issuer's info from the 'issuer' parameter if it's present
            String issuer = url.getQueryParameter("issuer");
            info.setIssuer(issuer != null ? issuer : "");
            info.setAccountName(label);
        }

        // just use the defaults if these parameters aren't set
        String algorithm = url.getQueryParameter("algorithm");
        if (algorithm != null) {
            info.setAlgorithm(algorithm);
        }
        String period = url.getQueryParameter("period");
        if (period != null) {
            info.setPeriod(Integer.parseInt(period));
        }
        String digits = url.getQueryParameter("digits");
        if (digits != null) {
            info.setDigits(Integer.parseInt(digits));
        }

        // 'counter' is required if the type is 'hotp'
        String counter = url.getQueryParameter("counter");
        if (counter != null) {
            info.setCounter(Long.parseLong(counter));
        } else if (info.getType().equals("hotp")) {
            throw new KeyInfoException("'counter' was not set which is required for 'hotp'");
        }

        return info;
    }

    public String getType() {
        return _type;
    }

    public byte[] getSecret() {
        return _secret;
    }

    public String getAccountName() {
        return _accountName;
    }

    public String getIssuer() {
        return _issuer;
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

    public long getCounter() {
        return _counter;
    }

    public int getPeriod() {
        return _period;
    }

    public boolean isTypeValid(String type) {
        return type.equals("totp") || type.equals("hotp");
    }

    public void setType(String type) throws KeyInfoException {
        type = type.toLowerCase();
        if (!isTypeValid(type)) {
            throw new KeyInfoException(String.format("unsupported otp type: %s", type));
        }
        _type = type;
    }

    public void setSecret(char[] base32) throws KeyInfoException {
        byte[] secret;
        try {
            secret = Base32.decode(base32);
        } catch (Base32Exception e) {
            throw new KeyInfoException("bad secret", e);
        }

        setSecret(secret);
    }

    public void setSecret(byte[] secret) {
        _secret = secret;
    }

    public void setAccountName(String accountName) {
        _accountName = accountName;
    }

    public void setIssuer(String issuer) {
        _issuer = issuer;
    }

    public boolean isAlgorithmValid(String algorithm) {
        return algorithm.equals("SHA1") || algorithm.equals("SHA256") || algorithm.equals("SHA512");
    }

    public void setAlgorithm(String algorithm) throws KeyInfoException {
        if (algorithm.startsWith("Hmac")) {
            algorithm = algorithm.substring(4);
        }
        algorithm = algorithm.toUpperCase();

        if (!isAlgorithmValid(algorithm)) {
            throw new KeyInfoException(String.format("unsupported algorithm: %s", algorithm));
        }
        _algorithm = algorithm;
    }

    public boolean isDigitsValid(int digits) {
        return digits == 6 || digits == 8;
    }

    public void setDigits(int digits) throws KeyInfoException {
        if (!isDigitsValid(digits)) {
            throw new KeyInfoException(String.format("unsupported amount of digits: %d", digits));
        }
        _digits = digits;
    }

    public boolean isCounterValid(long count) {
        return count >= 0;
    }

    public void setCounter(long count) throws KeyInfoException {
        if (!isCounterValid(count)) {
            throw new KeyInfoException(String.format("bad count: %d", count));
        }
        _counter = count;
    }

    public boolean isPeriodValid(int period) {
        return period > 0;
    }

    public void setPeriod(int period) throws KeyInfoException {
        if (!isPeriodValid(period)) {
            throw new KeyInfoException(String.format("bad period: %d", period));
        }
        _period = period;
    }
}
