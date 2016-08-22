package me.impy.aegis.crypto;

import android.net.Uri;

import java.io.Serializable;

import me.impy.aegis.encoding.Base32;

public class KeyInfo implements Serializable {
    private String type;
    private byte[] secret;
    private String accountName;
    private String issuer;
    private long counter;
    private String algorithm = "SHA1";
    private int digits = 6;
    private int period = 30;

    public String getType() {
        return type;
    }
    public byte[] getSecret() {
        return secret;
    }
    public String getAccountName() {
        return accountName;
    }
    public String getIssuer() {
        return issuer;
    }
    public String getAlgorithm() {
        return "Hmac" + algorithm;
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

    private KeyInfo() {
    }

    public void setCounter(long count) {
        counter = count;
    }

    public String getURL() throws Exception {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("otpauth");
        builder.authority(type);

        builder.appendQueryParameter("period", Integer.toString(period));
        builder.appendQueryParameter("algorithm", algorithm);
        builder.appendQueryParameter("secret", Base32.encodeOriginal(secret));
        if (type.equals("hotp")) {
            builder.appendQueryParameter("counter", Long.toString(counter));
        }

        if (!issuer.equals("")) {
            builder.path(String.format("%s:%s", issuer, accountName));
            builder.appendQueryParameter("issuer", issuer);
        } else {
            builder.path(accountName);
        }

        return builder.build().toString();
    }

    public long getMillisTillNextRotation() {
        long p = period * 1000;
        return p - (System.currentTimeMillis() % p);
    }

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
        String path = url.getPath();
        String label = path != null ? path.substring(1) : "";

        if (label.contains(":")) {
            // a label can only contain one colon
            // it's ok to fail if that's not the case
            String[] strings = label.split(":");

            if (strings.length == 2) {
                info.issuer = strings[0];
                info.accountName = strings[1];
            } else {
                // at this point, just dump the whole thing into the accountName
                info.accountName = label;
            }
        } else {
            // label only contains the account name
            // grab the issuer's info from the 'issuer' parameter if it's present
            String issuer = url.getQueryParameter("issuer");
            info.issuer = issuer != null ? issuer : "";
            info.accountName = label;
        }

        // just use the defaults if these parameters aren't set
        String algorithm = url.getQueryParameter("algorithm");
        if (algorithm != null) {
            info.algorithm = algorithm;
        }
        String period = url.getQueryParameter("period");
        if (period != null) {
            info.period = Integer.parseInt(period);
        }
        String digits = url.getQueryParameter("digits");
        if (digits != null) {
            info.digits = Integer.parseInt(digits);
        }

        // 'counter' is required if the type is 'hotp'
        String counter = url.getQueryParameter("counter");
        if (counter != null) {
            info.counter = Long.parseLong(counter);
        } else if (info.type.equals("hotp")) {
            throw new Exception("'counter' was not set which is required for 'hotp'");
        }

        return info;
    }
}
