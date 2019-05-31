package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.util.Xml;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base32Exception;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;

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

    public State read(FileReader reader) throws DatabaseImporterException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(reader.getStream(), null);
            parser.nextTag();

            JSONArray entries = parse(parser);
            return new State(entries);
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
                        DatabaseEntry entry = convertEntry(entryObj);
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

        private static DatabaseEntry convertEntry(JSONObject entry) throws DatabaseImporterEntryException {
            try {
                AuthyEntryInfo authyEntryInfo = new AuthyEntryInfo();
                authyEntryInfo.OriginalName = entry.getString("originalName");
                authyEntryInfo.AccountType = entry.getString("accountType");
                authyEntryInfo.Name = entry.optString("name");

                sanitizeEntryInfo(authyEntryInfo);

                int digits = entry.getInt("digits");
                byte[] secret = Base32.decode(entry.getString("decryptedSecret").toCharArray());

                OtpInfo info = new TotpInfo(secret, "SHA1", digits, 30);

                return new DatabaseEntry(info, authyEntryInfo.Name, authyEntryInfo.Issuer);
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

    private static JSONArray parse(XmlPullParser parser)
            throws IOException, XmlPullParserException, JSONException {
        JSONArray entries = new JSONArray();

        parser.require(XmlPullParser.START_TAG, null, "map");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            if (!parser.getName().equals("string")) {
                skip(parser);
                continue;
            }

            return new JSONArray(parseEntry(parser).Value);
        }

        return entries;
    }

    private static XmlEntry parseEntry(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "string");
        String name = parser.getAttributeValue(null, "name");
        String value = parseText(parser);
        parser.require(XmlPullParser.END_TAG, null, "string");

        XmlEntry entry = new XmlEntry();
        entry.Name = name;
        entry.Value = value;
        return entry;
    }

    private static String parseText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String text = "";
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
            parser.nextTag();
        }
        return text;
    }

    private static void skip(XmlPullParser parser) throws IOException, XmlPullParserException {
        // source: https://developer.android.com/training/basics/network-ops/xml.html
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }

        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private static class XmlEntry {
        String Name;
        String Value;
    }

    private static class AuthyEntryInfo {
        String OriginalName;
        String AccountType;
        String Issuer;
        String Name;
    }
}
