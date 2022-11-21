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
import com.topjohnwu.superuser.io.SuFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BattleNetImporter extends DatabaseImporter {
    private static final String _pkgName = "com.blizzard.bma";
    private static final String _subPath = "shared_prefs/com.blizzard.bma.AUTH_STORE.xml";

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
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(stream, null);
            parser.nextTag();

            List<String> entries = new ArrayList<>();
            for (PreferenceParser.XmlEntry entry : PreferenceParser.parse(parser)) {
                if (entry.Name.equals("com.blizzard.bma.AUTH_STORE.HASH")) {
                    entries.add(entry.Value);
                    break;
                }
            }
            return new BattleNetImporter.State(entries);
        } catch (XmlPullParserException | IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class State extends DatabaseImporter.State {
        private final List<String> _entries;

        public State(List<String> entries) {
            super(false);
            _entries = entries;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            for (String str : _entries) {
                try {
                    VaultEntry entry = convertEntry(str);
                    result.addEntry(entry);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static VaultEntry convertEntry(String hashString) throws DatabaseImporterEntryException {
            try {
                byte[] hash = Hex.decode(hashString);
                if (hash.length != _key.length) {
                    throw new DatabaseImporterEntryException(String.format("Unexpected hash length: %d", hash.length), hashString);
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < hash.length; i++) {
                    char c = (char) (hash[i] ^ _key[i]);
                    sb.append(c);
                }

                final int secretLen = 40;
                byte[] secret = Hex.decode(sb.substring(0, secretLen));
                String serial = sb.substring(secretLen);

                OtpInfo info = new TotpInfo(secret, OtpInfo.DEFAULT_ALGORITHM, 8, TotpInfo.DEFAULT_PERIOD);
                return new VaultEntry(info, serial, "Battle.net");
            } catch (OtpInfoException | EncodingException e) {
                throw new DatabaseImporterEntryException(e, hashString);
            }
        }
    }
}
