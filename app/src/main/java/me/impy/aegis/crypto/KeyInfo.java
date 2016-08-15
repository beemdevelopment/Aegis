package me.impy.aegis.crypto;

import android.net.Uri;

import me.impy.aegis.encoding.Base32;

public class KeyInfo {
    private String _type;
    private String _label;
    private byte[] _secret;
    private String _issuer;
    private String _algo;
    private int _digits;
    private long _counter;
    private String _period;

    private KeyInfo() {

    }

    public static KeyInfo FromURL(String s) throws Exception {
        final Uri url = Uri.parse(s);

        if (!url.getScheme().equals("otpauth")) {
            throw new Exception("unsupported protocol");
        }

        KeyInfo info = new KeyInfo();
        info._type = url.getHost();

        String secret = url.getQueryParameter("secret");
        info._secret = Base32.decode(secret);
        info._issuer = url.getQueryParameter("issuer");
        info._label = url.getPath();
        info._algo = url.getQueryParameter("algorithm");
        info._period = url.getQueryParameter("period");
        info._digits = url.getQueryParameter("digits") != null ? Integer.getInteger(url.getQueryParameter("digits")) : 6;
        info._counter = url.getQueryParameter("counter") != null ? Long.getLong(url.getQueryParameter("counter")) : 0;

        return info;
    }
}
