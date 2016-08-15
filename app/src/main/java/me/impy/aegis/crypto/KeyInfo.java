package me.impy.aegis.crypto;

import android.net.Uri;

import java.io.Serializable;

import me.impy.aegis.encoding.Base32;

public class KeyInfo implements Serializable {
    private String type;
    private String label;
    private byte[] secret;
    private String issuer;
    private String algo;
    private int digits;
    private long counter;
    private int period;

    public String getType() {
        return type;
    }

    public KeyInfo setType(String type) {
        this.type = type;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public KeyInfo setLabel(String label) {
        this.label = label;
        return this;
    }

    public byte[] getSecret() {
        return secret;
    }

    public KeyInfo setSecret(byte[] secret) {
        this.secret = secret;
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public KeyInfo setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public String getAlgo() {
        return algo;
    }

    public KeyInfo setAlgo(String algo) {
        this.algo = algo;
        return this;
    }

    public int getDigits() {
        return digits;
    }

    public KeyInfo setDigits(int digits) {
        this.digits = digits;
        return this;
    }

    public long getCounter() {
        return counter;
    }

    public KeyInfo setCounter(long counter) {
        this.counter = counter;
        return this;
    }

    public int getPeriod() {
        return period;
    }

    public KeyInfo setPeriod(int period) {
        this.period = period;
        return this;
    }

    private KeyInfo() {

    }

    public static KeyInfo FromURL(String s) throws Exception {
        final Uri url = Uri.parse(s);

        if (!url.getScheme().equals("otpauth")) {
            throw new Exception("unsupported protocol");
        }

        KeyInfo info = new KeyInfo();
        info.type = url.getHost();

        String secret = url.getQueryParameter("secret");
        info.secret = Base32.decode(secret);
        info.issuer = url.getQueryParameter("issuer");
        info.label = url.getPath();
        info.algo = url.getQueryParameter("algorithm") != null ? url.getQueryParameter("algorithm") : "HmacSHA1";
        info.period = url.getQueryParameter("period") != null ? Integer.getInteger(url.getQueryParameter("period")) : 30;
        info.digits = url.getQueryParameter("digits") != null ? Integer.getInteger(url.getQueryParameter("digits")) : 6;
        info.counter = url.getQueryParameter("counter") != null ? Long.getLong(url.getQueryParameter("counter")) : 0;

        return info;
    }
}
