package com.beemdevelopment.aegis.otp;

import android.net.Uri;

import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base32Exception;

public class GoogleAuthInfo {
    private OtpInfo _info;
    private String _accountName;
    private String _issuer;

    public GoogleAuthInfo(OtpInfo info, String accountName, String issuer) {
        _info = info;
        _accountName = accountName;
        _issuer = issuer;
    }

    public OtpInfo getOtpInfo() {
        return _info;
    }

    public Uri getUri() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("otpauth");

        if (_info instanceof TotpInfo) {
            builder.authority("totp");
            builder.appendQueryParameter("period", Integer.toString(((TotpInfo)_info).getPeriod()));
        } else if (_info instanceof HotpInfo) {
            builder.authority("hotp");
            builder.appendQueryParameter("counter", Long.toString(((HotpInfo)_info).getCounter()));
        } else {
            throw new RuntimeException();
        }

        builder.appendQueryParameter("digits", Integer.toString(_info.getDigits()));
        builder.appendQueryParameter("algorithm", _info.getAlgorithm(false));
        builder.appendQueryParameter("secret", new String(Base32.encode(_info.getSecret())));

        if (_issuer != null && !_issuer.equals("")) {
            builder.path(String.format("%s:%s", _issuer, _accountName));
            builder.appendQueryParameter("issuer", _issuer);
        } else {
            builder.path(_accountName);
        }

        return builder.build();
    }

    public static GoogleAuthInfo parseUri(String s) throws GoogleAuthInfoException {
        Uri uri = Uri.parse(s);
        if (uri == null) {
            throw new GoogleAuthInfoException("bad uri format");
        }
        return GoogleAuthInfo.parseUri(uri);
    }

    public static GoogleAuthInfo parseUri(Uri uri) throws GoogleAuthInfoException {
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals("otpauth")) {
            throw new GoogleAuthInfoException("unsupported protocol");
        }

        // 'secret' is a required parameter
        String encodedSecret = uri.getQueryParameter("secret");
        if (encodedSecret == null) {
            throw new GoogleAuthInfoException("'secret' is not set");
        }

        // decode secret
        byte[] secret;
        try {
            secret = Base32.decode(encodedSecret.toCharArray());
        } catch (Base32Exception e) {
            throw new GoogleAuthInfoException("bad secret", e);
        }

        // check the otp type
        OtpInfo info;
        try {
            String type = uri.getHost();
            switch (type) {
                case "totp":
                    TotpInfo totpInfo = new TotpInfo(secret);
                    String period = uri.getQueryParameter("period");
                    if (period != null) {
                        totpInfo.setPeriod(Integer.parseInt(period));
                    }
                    info = totpInfo;
                    break;
                case "hotp":
                    HotpInfo hotpInfo = new HotpInfo(secret);
                    String counter = uri.getQueryParameter("counter");
                    if (counter == null) {
                        throw new GoogleAuthInfoException("'counter' was not set");
                    }
                    hotpInfo.setCounter(Long.parseLong(counter));
                    info = hotpInfo;
                    break;
                default:
                    throw new GoogleAuthInfoException(String.format("unsupported otp type: %s", type));
            }
        } catch (OtpInfoException e) {
            throw new GoogleAuthInfoException(e);
        }

        // provider info used to disambiguate accounts
        String path = uri.getPath();
        String label = path != null && path.length() > 0 ? path.substring(1) : "";

        String accountName = "";
        String issuer = "";

        if (label.contains(":")) {
            // a label can only contain one colon
            // it's ok to fail if that's not the case
            String[] strings = label.split(":");
            if (strings.length == 2) {
                issuer = strings[0];
                accountName = strings[1];
            } else {
                // at this point, just dump the whole thing into the accountName
                accountName = label;
            }
        } else {
            // label only contains the account name
            // grab the issuer's info from the 'issuer' parameter if it's present
            String issuerParam = uri.getQueryParameter("issuer");
            issuer = issuerParam != null ? issuerParam : "";
            accountName = label;
        }

        // just use the defaults if these parameters aren't set
        try {
            String algorithm = uri.getQueryParameter("algorithm");
            if (algorithm != null) {
                info.setAlgorithm(algorithm);
            }
            String digits = uri.getQueryParameter("digits");
            if (digits != null) {
                info.setDigits(Integer.parseInt(digits));
            }
        } catch (OtpInfoException e) {
            throw new GoogleAuthInfoException(e);
        }

        return new GoogleAuthInfo(info, accountName, issuer);
    }

    public String getIssuer() {
        return _issuer;
    }

    public String getAccountName() {
        return _accountName;
    }
}
