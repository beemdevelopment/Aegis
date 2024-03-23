package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Xml;

import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.util.PreferenceParser;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.google.common.base.Strings;
import com.topjohnwu.superuser.io.SuFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class BattleNetImporter extends DatabaseImporter {
    private static final String _pkgName = "com.blizzard.messenger";
    private static final String _subPath = "shared_prefs/com.blizzard.messenger.authenticator_preferences.xml";

    private static final byte[] _key;

    public BattleNetImporter(Context context) {
        super(context);
    }

    static {
        try {
            _key = Hex.decode("398e27fc50276a656065b0e525f4c06c04c61075286b8e7aeda59da9813b5dd6c80d2fb38068773fa59ba47c17ca6c6479015c1d5b8b8f6b9a");
        } catch (EncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected SuFile getAppPath() throws DatabaseImporterException, PackageManager.NameNotFoundException {
        return getAppPath(_pkgName, _subPath);
    }

    @Override
    protected State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        final String serialKey = "com.blizzard.messenger.AUTHENTICATOR_SERIAL";
        final String secretKey = "com.blizzard.messenger.AUTHENTICATOR_DEVICE_SECRET";

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(stream, null);
            parser.nextTag();

            String serial = "";
            String secretValue = null;
            for (PreferenceParser.XmlEntry entry : PreferenceParser.parse(parser)) {
                if (entry.Name.equals(secretKey)) {
                    secretValue = entry.Value;
                } else if (entry.Name.equals(serialKey)) {
                    serial = entry.Value;
                }
            }

            if (secretValue == null) {
                throw new DatabaseImporterException(String.format("Key not found: %s", secretKey));
            }

            return new BattleNetImporter.State(serial, secretValue);
        } catch (XmlPullParserException | IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class State extends DatabaseImporter.State {
        private final String _serial;
        private final String _secretValue;

        public State(String serial, String secretValue) {
            super(false);
            _serial = serial;
            _secretValue = secretValue;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            try {
                VaultEntry entry = convertEntry(_serial, _secretValue);
                result.addEntry(entry);
            } catch (DatabaseImporterEntryException e) {
                result.addError(e);
            }

            return result;
        }

        private static VaultEntry convertEntry(String serial, String secretString) throws DatabaseImporterEntryException {
            try {
                if (!Strings.isNullOrEmpty(serial)) {
                    serial = unmask(serial);
                }
                byte[] secret = Hex.decode(unmask(secretString));
                OtpInfo info = new TotpInfo(secret, OtpInfo.DEFAULT_ALGORITHM, 8, TotpInfo.DEFAULT_PERIOD);
                return new VaultEntry(info, serial, "Battle.net");
            } catch (OtpInfoException | EncodingException e) {
                throw new DatabaseImporterEntryException(e, secretString);
            }
        }

        private static String unmask(String s) throws EncodingException {
            byte[] ds = Hex.decode(s);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ds.length; i++) {
                char c = (char) (ds[i] ^ _key[i]);
                sb.append(c);
            }
            return sb.toString();
        }
    }
}
