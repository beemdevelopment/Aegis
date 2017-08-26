package me.impy.aegis.crypto;

import android.net.Uri;

import java.io.Serializable;

import me.impy.aegis.encoding.Base32;

public class KeyInfo implements Serializable {
    private String _type;
    private byte[] _secret;
    private String _accountName;
    private String _issuer;
    private long _counter;
    private String _algorithm = "SHA1";
    private int _digits = 6;
    private int _period = 30;

    public String getURL() throws Exception {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("otpauth");
        builder.authority(_type);

        builder.appendQueryParameter("period", Integer.toString(_period));
        builder.appendQueryParameter("algorithm", _algorithm);
        builder.appendQueryParameter("secret", Base32.encodeOriginal(_secret));
        if (_type.equals("hotp")) {
            builder.appendQueryParameter("counter", Long.toString(_counter));
        }

        if (!_issuer.equals("")) {
            builder.path(String.format("%s:%s", _issuer, _accountName));
            builder.appendQueryParameter("issuer", _issuer);
        } else {
            builder.path(_accountName);
        }

        return builder.build().toString();
    }

    public long getMillisTillNextRotation() {
        long p = _period * 1000;
        return p - (System.currentTimeMillis() % p);
    }

    public static KeyInfo fromURL(String s) throws Exception {
        final Uri url = Uri.parse(s);
        if (!url.getScheme().equals("otpauth")) {
            throw new Exception("unsupported protocol");
        }

        KeyInfo info = new KeyInfo();

        // only 'totp' and 'hotp' are supported
        info._type = url.getHost();
        if (info._type.equals("totp") && info._type.equals("hotp")) {
            throw new Exception("unsupported type");
        }

        // 'secret' is a required parameter
        String secret = url.getQueryParameter("secret");
        if (secret == null) {
            throw new Exception("'secret' is not set");
        }
        info._secret = Base32.decode(secret);

        // provider info used to disambiguate accounts
        String path = url.getPath();
        String label = path != null ? path.substring(1) : "";

        if (label.contains(":")) {
            // a label can only contain one colon
            // it's ok to fail if that's not the case
            String[] strings = label.split(":");

            if (strings.length == 2) {
                info._issuer = strings[0];
                info._accountName = strings[1];
            } else {
                // at this point, just dump the whole thing into the accountName
                info._accountName = label;
            }
        } else {
            // label only contains the account name
            // grab the issuer's info from the 'issuer' parameter if it's present
            String issuer = url.getQueryParameter("issuer");
            info._issuer = issuer != null ? issuer : "";
            info._accountName = label;
        }

        // just use the defaults if these parameters aren't set
        String algorithm = url.getQueryParameter("algorithm");
        if (algorithm != null) {
            info._algorithm = algorithm;
        }
        String period = url.getQueryParameter("period");
        if (period != null) {
            info._period = Integer.parseInt(period);
        }
        String digits = url.getQueryParameter("digits");
        if (digits != null) {
            info._digits = Integer.parseInt(digits);
        }

        // 'counter' is required if the type is 'hotp'
        String counter = url.getQueryParameter("counter");
        if (counter != null) {
            info._counter = Long.parseLong(counter);
        } else if (info._type.equals("hotp")) {
            throw new Exception("'counter' was not set which is required for 'hotp'");
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
    public String getAlgorithm() {
        return "Hmac" + _algorithm;
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

    public void setType(String type) {
        _type = type.toLowerCase();
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
    public void setAlgorithm(String algorithm) {
        if (algorithm.startsWith("Hmac")) {
            algorithm = algorithm.substring(4);
        }
        _algorithm = algorithm.toUpperCase();
    }
    public void setDigits(int digits) {
        _digits = digits;
    }
    public void setCounter(long count) {
        _counter = count;
    }
    public void setPeriod(int period) {
        _period = period;
    }
}
