package com.beemdevelopment.aegis.otp;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.beemdevelopment.aegis.GoogleAuthProtos;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base64;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.encoding.Hex;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GoogleAuthInfo implements Transferable, Serializable {
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

    public static GoogleAuthInfo parseUri(String s) throws GoogleAuthInfoException {
        Uri uri = Uri.parse(s);
        if (uri == null) {
            throw new GoogleAuthInfoException(uri, String.format("Bad URI format: %s", s));
        }
        return GoogleAuthInfo.parseUri(uri);
    }

    public static GoogleAuthInfo parseUri(Uri uri) throws GoogleAuthInfoException {
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equals(SCHEME) || scheme.equals(MotpInfo.SCHEME))) {
            throw new GoogleAuthInfoException(uri, String.format("Unsupported protocol: %s", scheme));
        }

        // 'secret' is a required parameter
        String encodedSecret = uri.getQueryParameter("secret");
        if (encodedSecret == null) {
            throw new GoogleAuthInfoException(uri, "Parameter 'secret' is not present");
        }

        byte[] secret;
        try {
            secret = (scheme.equals(MotpInfo.SCHEME)) ? Hex.decode(encodedSecret) : parseSecret(encodedSecret);
        } catch (EncodingException e) {
            throw new GoogleAuthInfoException(uri, "Bad secret", e);
        }
        if (secret.length == 0) {
            throw new GoogleAuthInfoException(uri, "Secret is empty");
        }

        OtpInfo info;
        String issuer = "";
        try {
            String type = (scheme.equals(MotpInfo.SCHEME)) ? MotpInfo.ID : uri.getHost();
            if (type == null) {
                throw new GoogleAuthInfoException(uri, String.format("Host not present in URI: %s", uri.toString()));
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
                        throw new GoogleAuthInfoException(uri, "Parameter 'counter' is not present");
                    }
                    hotpInfo.setCounter(Long.parseLong(counter));
                    info = hotpInfo;
                    break;
                case YandexInfo.HOST_ID:
                    String pin = uri.getQueryParameter("pin");
                    if (pin != null) {
                        pin = new String(parseSecret(pin), StandardCharsets.UTF_8);
                    }

                    info = new YandexInfo(secret, pin);
                    issuer = info.getType();
                    break;
                case MotpInfo.ID:
                    info = new MotpInfo(secret);
                    break;
                default:
                    throw new GoogleAuthInfoException(uri, String.format("Unsupported OTP type: %s", type));
            }
        } catch (OtpInfoException | NumberFormatException | EncodingException e) {
            throw new GoogleAuthInfoException(uri, e);
        }

        // provider info used to disambiguate accounts
        String path = uri.getPath();
        String label = path != null && path.length() > 0 ? path.substring(1) : "";

        String accountName = "";

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
            if (issuer.isEmpty()) {
                issuer = issuerParam != null ? issuerParam : "";
            }
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
            throw new GoogleAuthInfoException(uri, e);
        }

        return new GoogleAuthInfo(info, accountName, issuer);
    }

    /**
     * Decodes the given base 32 secret, while being tolerant of whitespace and dashes.
     */
    public static byte[] parseSecret(String s) throws EncodingException {
        s = s.trim().replace("-", "").replace(" ", "");
        return Base32.decode(s);
    }

    public static Export parseExportUri(String s) throws GoogleAuthInfoException {
        Uri uri = Uri.parse(s);
        if (uri == null) {
            throw new GoogleAuthInfoException(uri, "Bad URI format");
        }
        return GoogleAuthInfo.parseExportUri(uri);
    }

    public static Export parseExportUri(Uri uri) throws GoogleAuthInfoException {
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals(SCHEME_EXPORT)) {
            throw new GoogleAuthInfoException(uri, "Unsupported protocol");
        }

        String host = uri.getHost();
        if (host == null || !host.equals("offline")) {
            throw new GoogleAuthInfoException(uri, "Unsupported host");
        }

        String data = uri.getQueryParameter("data");
        if (data == null) {
            throw new GoogleAuthInfoException(uri, "Parameter 'data' is not set");
        }

        GoogleAuthProtos.MigrationPayload payload;
        try {
            byte[] bytes = Base64.decode(data);
            payload = GoogleAuthProtos.MigrationPayload.parseFrom(bytes);
        } catch (EncodingException | InvalidProtocolBufferException e) {
            throw new GoogleAuthInfoException(uri, e);
        }

        List<GoogleAuthInfo> infos = new ArrayList<>();
        for (GoogleAuthProtos.MigrationPayload.OtpParameters params : payload.getOtpParametersList()) {
            OtpInfo otp;
            try {
                int digits;
                switch (params.getDigits()) {
                    case DIGIT_COUNT_UNSPECIFIED:
                        // intentional fallthrough
                    case DIGIT_COUNT_SIX:
                        digits = TotpInfo.DEFAULT_DIGITS;
                        break;
                    case DIGIT_COUNT_EIGHT:
                        digits = 8;
                        break;
                    default:
                        throw new GoogleAuthInfoException(uri, String.format("Unsupported digits: %d", params.getDigits().ordinal()));
                }

                String algo;
                switch (params.getAlgorithm()) {
                    case ALGORITHM_UNSPECIFIED:
                        // intentional fallthrough
                    case ALGORITHM_SHA1:
                        algo = "SHA1";
                        break;
                    case ALGORITHM_SHA256:
                        algo = "SHA256";
                        break;
                    case ALGORITHM_SHA512:
                        algo = "SHA512";
                        break;
                    default:
                        throw new GoogleAuthInfoException(uri, String.format("Unsupported hash algorithm: %d", params.getAlgorithm().ordinal()));
                }

                byte[] secret = params.getSecret().toByteArray();
                if (secret.length == 0) {
                    throw new GoogleAuthInfoException(uri, "Secret is empty");
                }

                switch (params.getType()) {
                    case OTP_TYPE_UNSPECIFIED:
                        // intentional fallthrough
                    case OTP_TYPE_TOTP:
                        otp = new TotpInfo(secret, algo, digits, TotpInfo.DEFAULT_PERIOD);
                        break;
                    case OTP_TYPE_HOTP:
                        otp = new HotpInfo(secret, algo, digits, params.getCounter());
                        break;
                    default:
                        throw new GoogleAuthInfoException(uri, String.format("Unsupported algorithm: %d", params.getType().ordinal()));
                }
            } catch (OtpInfoException e) {
                throw new GoogleAuthInfoException(uri, e);
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

    public OtpInfo getOtpInfo() {
        return _info;
    }

    @Override
    public Uri getUri() {
        Uri.Builder builder = new Uri.Builder();

        if (_info instanceof MotpInfo) {
            builder.scheme(MotpInfo.SCHEME);
            builder.appendQueryParameter("secret", Hex.encode(_info.getSecret()));
        } else {
            builder.scheme(SCHEME);

            if (_info instanceof TotpInfo) {
                if (_info instanceof SteamInfo) {
                    builder.authority("steam");
                } else if (_info instanceof YandexInfo) {
                    builder.authority(YandexInfo.HOST_ID);
                } else {
                    builder.authority("totp");
                }
                builder.appendQueryParameter("period", Integer.toString(((TotpInfo) _info).getPeriod()));
            } else if (_info instanceof HotpInfo) {
                builder.authority("hotp");
                builder.appendQueryParameter("counter", Long.toString(((HotpInfo) _info).getCounter()));
            } else {
                throw new RuntimeException(String.format("Unsupported OtpInfo type: %s", _info.getClass()));
            }

            builder.appendQueryParameter("digits", Integer.toString(_info.getDigits()));
            builder.appendQueryParameter("algorithm", _info.getAlgorithm(false));
            builder.appendQueryParameter("secret", Base32.encode(_info.getSecret()));

            if (_info instanceof YandexInfo) {
                builder.appendQueryParameter("pin", Base32.encode(((YandexInfo) _info).getPin()));
            }
        }

        if (_issuer != null && !_issuer.equals("")) {
            builder.path(String.format("%s:%s", _issuer, _accountName));
            builder.appendQueryParameter("issuer", _issuer);
        } else {
            builder.path(_accountName);
        }

        return builder.build();
    }

    public String getIssuer() {
        return _issuer;
    }

    public String getAccountName() {
        return _accountName;
    }

    public static class Export implements Transferable, Serializable {
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

        public static List<Integer> getMissingIndices(@NonNull List<Export> exports) throws IllegalArgumentException {
            if (!isSingleBatch(exports)) {
                throw new IllegalArgumentException("Export list contains entries from different batches");
            }

            List<Integer> indicesMissing = new ArrayList<>();
            if (exports.isEmpty()) {
                return indicesMissing;
            }

            Set<Integer> indicesPresent = exports.stream()
                    .map(Export::getBatchIndex)
                    .collect(Collectors.toSet());

            for (int i = 0; i < exports.get(0).getBatchSize(); i++) {
                if (!indicesPresent.contains(i)) {
                    indicesMissing.add(i);
                }
            }

            return indicesMissing;
        }

        public static boolean isSingleBatch(@NonNull List<Export> exports) {
            if (exports.isEmpty()) {
                return true;
            }

            int batchId = exports.get(0).getBatchId();
            for (Export export : exports) {
                if (export.getBatchId() != batchId) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public Uri getUri() throws GoogleAuthInfoException {
            GoogleAuthProtos.MigrationPayload.Builder builder = GoogleAuthProtos.MigrationPayload.newBuilder();
            builder.setBatchId(_batchId)
                    .setBatchIndex(_batchIndex)
                    .setBatchSize(_batchSize)
                    .setVersion(1);

            for (GoogleAuthInfo info: _entries) {
                GoogleAuthProtos.MigrationPayload.OtpParameters.Builder parameters = GoogleAuthProtos.MigrationPayload.OtpParameters.newBuilder()
                        .setSecret(ByteString.copyFrom(info.getOtpInfo().getSecret()))
                        .setName(info.getAccountName())
                        .setIssuer(info.getIssuer());

                switch (info.getOtpInfo().getAlgorithm(false)) {
                    case "SHA1":
                        parameters.setAlgorithm(GoogleAuthProtos.MigrationPayload.Algorithm.ALGORITHM_SHA1);
                        break;
                    case "SHA256":
                        parameters.setAlgorithm(GoogleAuthProtos.MigrationPayload.Algorithm.ALGORITHM_SHA256);
                        break;
                    case "SHA512":
                        parameters.setAlgorithm(GoogleAuthProtos.MigrationPayload.Algorithm.ALGORITHM_SHA512);
                        break;
                    case "MD5":
                        parameters.setAlgorithm(GoogleAuthProtos.MigrationPayload.Algorithm.ALGORITHM_MD5);
                        break;
                    default:
                        throw new GoogleAuthInfoException(info.getUri(), String.format("Unsupported Algorithm: %s", info.getOtpInfo().getAlgorithm(false)));
                }

                switch (info.getOtpInfo().getDigits()) {
                    case 6:
                        parameters.setDigits(GoogleAuthProtos.MigrationPayload.DigitCount.DIGIT_COUNT_SIX);
                        break;
                    case 8:
                        parameters.setDigits(GoogleAuthProtos.MigrationPayload.DigitCount.DIGIT_COUNT_EIGHT);
                        break;
                    default:
                        throw new GoogleAuthInfoException(info.getUri(), String.format("Unsupported number of digits: %s", info.getOtpInfo().getDigits()));
                }

                switch (info.getOtpInfo().getType().toLowerCase()) {
                    case HotpInfo.ID:
                        parameters.setType(GoogleAuthProtos.MigrationPayload.OtpType.OTP_TYPE_HOTP);
                        parameters.setCounter(((HotpInfo) info.getOtpInfo()).getCounter());
                        break;
                    case TotpInfo.ID:
                        parameters.setType(GoogleAuthProtos.MigrationPayload.OtpType.OTP_TYPE_TOTP);
                        break;
                    default:
                        throw new GoogleAuthInfoException(info.getUri(), String.format("Type unsupported by GoogleAuthProtos: %s", info.getOtpInfo().getType()));
                }

                builder.addOtpParameters(parameters.build());
            }

            Uri.Builder exportUriBuilder = new Uri.Builder()
                    .scheme(SCHEME_EXPORT)
                    .authority("offline");

            String data = Base64.encode(builder.build().toByteArray());
            exportUriBuilder.appendQueryParameter("data", data);

            return exportUriBuilder.build();
        }
    }
}
