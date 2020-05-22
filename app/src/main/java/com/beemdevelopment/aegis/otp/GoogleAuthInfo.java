package com.beemdevelopment.aegis.otp;

import android.net.Uri;

import com.beemdevelopment.aegis.GoogleAuthProtos;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base64;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GoogleAuthInfo implements Serializable {
    public static final String SCHEME = "otpauth";
    public static final String SCHEME_EXPORT = "otpauth-migration";

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
        builder.scheme(SCHEME);

        if (_info instanceof TotpInfo) {
            if (_info instanceof SteamInfo) {
                builder.authority("steam");
            } else {
                builder.authority("totp");
            }
            builder.appendQueryParameter("period", Integer.toString(((TotpInfo)_info).getPeriod()));
        } else if (_info instanceof HotpInfo) {
            builder.authority("hotp");
            builder.appendQueryParameter("counter", Long.toString(((HotpInfo)_info).getCounter()));
        } else {
            throw new RuntimeException(String.format("Unsupported OtpInfo type: %s", _info.getClass()));
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
            throw new GoogleAuthInfoException(String.format("Bad URI format: %s", s));
        }
        return GoogleAuthInfo.parseUri(uri);
    }

    public static GoogleAuthInfo parseUri(Uri uri) throws GoogleAuthInfoException {
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals(SCHEME)) {
            throw new GoogleAuthInfoException("Unsupported protocol");
        }

        // 'secret' is a required parameter
        String encodedSecret = uri.getQueryParameter("secret");
        if (encodedSecret == null) {
            throw new GoogleAuthInfoException("Parameter 'secret' is not present");
        }

        // decode secret
        byte[] secret;
        try {
            secret = Base32.decode(encodedSecret);
        } catch (EncodingException e) {
            throw new GoogleAuthInfoException("Bad secret", e);
        }

        // check the otp type
        OtpInfo info;
        try {
            String type = uri.getHost();
            if (type == null) {
                throw new GoogleAuthInfoException(String.format("Host not present in URI: %s", uri.toString()));
            }

            switch (type) {
                case "totp":
                    TotpInfo totpInfo = new TotpInfo(secret);
                    String period = uri.getQueryParameter("period");
                    if (period != null) {
                        totpInfo.setPeriod(Integer.parseInt(period));
                    }
                    info = totpInfo;
                    break;
                case "steam":
                    SteamInfo steamInfo = new SteamInfo(secret);
                    period = uri.getQueryParameter("period");
                    if (period != null) {
                        steamInfo.setPeriod(Integer.parseInt(period));
                    }
                    info = steamInfo;
                    break;
                case "hotp":
                    HotpInfo hotpInfo = new HotpInfo(secret);
                    String counter = uri.getQueryParameter("counter");
                    if (counter == null) {
                        throw new GoogleAuthInfoException("Parameter 'counter' is not present");
                    }
                    hotpInfo.setCounter(Long.parseLong(counter));
                    info = hotpInfo;
                    break;
                default:
                    throw new GoogleAuthInfoException(String.format("Unsupported OTP type: %s", type));
            }
        } catch (OtpInfoException | NumberFormatException e) {
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
        } catch (OtpInfoException | NumberFormatException e) {
            throw new GoogleAuthInfoException(e);
        }

        return new GoogleAuthInfo(info, accountName, issuer);
    }

    public static Export parseExportUri(String s) throws GoogleAuthInfoException {
        Uri uri = Uri.parse(s);
        if (uri == null) {
            throw new GoogleAuthInfoException("Bad URI format");
        }
        return GoogleAuthInfo.parseExportUri(uri);
    }

    public static Export parseExportUri(Uri uri) throws GoogleAuthInfoException {
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals(SCHEME_EXPORT)) {
            throw new GoogleAuthInfoException("Unsupported protocol");
        }

        String host = uri.getHost();
        if (host == null || !host.equals("offline")) {
            throw new GoogleAuthInfoException("Unsupported host");
        }

        String data = uri.getQueryParameter("data");
        if (data == null) {
            throw new GoogleAuthInfoException("Parameter 'data' is not set");
        }

        GoogleAuthProtos.MigrationPayload payload;
        try {
            byte[] bytes = Base64.decode(data);
            payload = GoogleAuthProtos.MigrationPayload.parseFrom(bytes);
        } catch (EncodingException | InvalidProtocolBufferException e) {
            throw new GoogleAuthInfoException(e);
        }

        List<GoogleAuthInfo> infos = new ArrayList<>();
        for (GoogleAuthProtos.MigrationPayload.OtpParameters params : payload.getOtpParametersList()) {
            OtpInfo otp;
            try {
                byte[] secret = params.getSecret().toByteArray();
                switch (params.getType()) {
                    case OTP_HOTP:
                        otp = new HotpInfo(secret, params.getCounter());
                        break;
                    case OTP_TOTP:
                        otp = new TotpInfo(secret);
                        break;
                    default:
                        throw new GoogleAuthInfoException(String.format("Unsupported algorithm: %d", params.getType().ordinal()));
                }
            } catch (OtpInfoException e){
                throw new GoogleAuthInfoException(e);
            }

            String name = params.getName();
            String issuer = params.getIssuer();
            int colonI = name.indexOf(':');
            if (issuer.isEmpty() && colonI != -1) {
                issuer = name.substring(0, colonI);
                name = name.substring(colonI + 1);
            }

            GoogleAuthInfo info = new GoogleAuthInfo(otp, name, issuer);
            infos.add(info);
        }

        return new Export(infos, payload.getBatchId(), payload.getBatchIndex(), payload.getBatchSize());
    }

    public String getIssuer() {
        return _issuer;
    }

    public String getAccountName() {
        return _accountName;
    }

    public static class Export {
        private int _batchId;
        private int _batchIndex;
        private int _batchSize;
        private List<GoogleAuthInfo> _entries;

        public Export(List<GoogleAuthInfo> entries, int batchId, int batchIndex, int batchSize) {
            _batchId = batchId;
            _batchIndex = batchIndex;
            _batchSize = batchSize;
            _entries = entries;
        }

        public List<GoogleAuthInfo> getEntries() {
            return _entries;
        }

        public int getBatchSize() {
            return _batchSize;
        }

        public int getBatchIndex() {
            return _batchIndex;
        }

        public int getBatchId() {
            return _batchId;
        }
    }
}
