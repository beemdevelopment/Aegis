package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.util.Xml;

import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base32Exception;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.util.PreferenceParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class AuthyImporter extends DatabaseImporter {
    private static final String _subPath = "shared_prefs/com.authy.storage.tokens.authenticator.xml";
    private static final String _pkgName = "com.authy.authy";

    public AuthyImporter(Context context) {
        super(context);
    }

    @Override
    protected String getAppPkgName() {
        return _pkgName;
    }

    @Override
    protected String getAppSubPath() {
        return _subPath;
    }

    @Override
    public State read(FileReader reader) throws DatabaseImporterException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(reader.getStream(), null);
            parser.nextTag();

            JSONArray array = new JSONArray();
            for (PreferenceParser.XmlEntry entry : PreferenceParser.parse(parser)) {
                if (entry.Name.equals("com.authy.storage.tokens.authenticator.key")) {
                    array = new JSONArray(entry.Value);
                }
            }

            return new State(array);
        } catch (XmlPullParserException | JSONException | IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class State extends DatabaseImporter.State {
        private JSONArray _obj;

        private State(JSONArray obj) {
            super(false);
            _obj = obj;
        }

        @Override
        public Result convert() throws DatabaseImporterException {
            Result result = new Result();

            try {
                for (int i = 0; i < _obj.length(); i++) {
                    JSONObject entryObj = _obj.getJSONObject(i);
                    try {
                        VaultEntry entry = convertEntry(entryObj);
                        result.addEntry(entry);
                    } catch (DatabaseImporterEntryException e) {
                        result.addError(e);
                    }
                }
            } catch (JSONException e) {
                throw new DatabaseImporterException(e);
            }

            return result;
        }

        private static VaultEntry convertEntry(JSONObject entry) throws DatabaseImporterEntryException {
            try {
                AuthyEntryInfo authyEntryInfo = new AuthyEntryInfo();
                authyEntryInfo.OriginalName = entry.getString("originalName");
                authyEntryInfo.AccountType = entry.getString("accountType");
                authyEntryInfo.Name = entry.optString("name");

                sanitizeEntryInfo(authyEntryInfo);

                int digits = entry.getInt("digits");
                byte[] secret = Base32.decode(entry.getString("decryptedSecret").toCharArray());

                OtpInfo info = new TotpInfo(secret, "SHA1", digits, 30);

                return new VaultEntry(info, authyEntryInfo.Name, authyEntryInfo.Issuer);
            } catch (OtpInfoException | JSONException | Base32Exception e) {
                throw new DatabaseImporterEntryException(e, entry.toString());
            }
        }

        private static void sanitizeEntryInfo(AuthyEntryInfo info) {
            String seperator = "";

            if (info.OriginalName.contains(":")) {
                info.Issuer = info.OriginalName.substring(0, info.OriginalName.indexOf(":"));
                seperator = ":";
            } else if (info.Name.contains(" - ")) {
                info.Issuer = info.Name.substring(0, info.Name.indexOf(" - "));
                seperator = " - ";
            } else {
                info.Issuer = info.AccountType.substring(0, 1).toUpperCase() + info.AccountType.substring(1);
            }

            info.Name = info.Name.replace(info.Issuer + seperator, "");
        }
    }

    private static class AuthyEntryInfo {
        String OriginalName;
        String AccountType;
        String Issuer;
        String Name;
    }
}
