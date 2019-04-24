package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.util.Xml;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FreeOtpImporter extends DatabaseImporter {
    private static final String _subPath = "shared_prefs/tokens.xml";
    private static final String _pkgName = "org.fedorahosted.freeotp";

    public FreeOtpImporter(Context context) {
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

            List<XmlEntry> entries = parse(parser);
            return new State(entries);
        } catch (XmlPullParserException | IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class State extends DatabaseImporter.State {
        private List<XmlEntry> _entries;

        private State(List<XmlEntry> entries) {
            super(false);
            _entries = entries;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            for (XmlEntry xmlEntry : _entries) {
                // TODO: order
                if (!xmlEntry.Name.equals("tokenOrder")) {
                    try {
                        DatabaseEntry entry = convertEntry(xmlEntry);
                        result.addEntry(entry);
                    } catch (DatabaseImporterEntryException e) {
                        result.addError(e);
                    }
                }
            }

            return result;
        }

        private static DatabaseEntry convertEntry(XmlEntry entry) throws DatabaseImporterEntryException {
            try {
                JSONObject obj = new JSONObject(entry.Value);

                String type = obj.getString("type").toLowerCase();
                String algo = obj.getString("algo");
                int digits = obj.getInt("digits");
                byte[] secret = toBytes(obj.getJSONArray("secret"));

                OtpInfo info;
                switch (type) {
                    case "totp":
                        info = new TotpInfo(secret, algo, digits, obj.getInt("period"));
                        break;
                    case "hotp":
                        info = new HotpInfo(secret, algo, digits, obj.getLong("counter"));
                        break;
                    default:
                        throw new DatabaseImporterException("unsupported otp type: " + type);
                }

                String issuer = obj.getString("issuerExt");
                String name = obj.optString("label");
                return new DatabaseEntry(info, name, issuer);
            } catch (DatabaseImporterException | OtpInfoException | JSONException e) {
                throw new DatabaseImporterEntryException(e, entry.Value);
            }
        }
    }

    private static List<XmlEntry> parse(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        List<XmlEntry> entries = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, null, "map");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            if (!parser.getName().equals("string")) {
                skip(parser);
                continue;
            }

            entries.add(parseEntry(parser));
        }

        return entries;
    }

    private static byte[] toBytes(JSONArray array) throws JSONException {
        byte[] bytes = new byte[array.length()];
        for (int i = 0; i < array.length(); i++) {
            bytes[i] = (byte)array.getInt(i);
        }
        return bytes;
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
}
