package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.util.Xml;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.util.PreferenceParser;

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

    @Override
    public State read(FileReader reader) throws DatabaseImporterException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(reader.getStream(), null);
            parser.nextTag();

            List<JSONObject> entries = new ArrayList<>();
            for (PreferenceParser.XmlEntry entry : PreferenceParser.parse(parser)) {
                if (!entry.Name.equals("tokenOrder")) {
                    entries.add(new JSONObject(entry.Value));
                }
            }
            return new State(entries);
        } catch (XmlPullParserException | IOException | JSONException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class State extends DatabaseImporter.State {
        private List<JSONObject> _entries;

        public State(List<JSONObject> entries) {
            super(false);
            _entries = entries;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            for (JSONObject obj : _entries) {
                try {
                    DatabaseEntry entry = convertEntry(obj);
                    result.addEntry(entry);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static DatabaseEntry convertEntry(JSONObject obj) throws DatabaseImporterEntryException {
            try {
                String type = obj.getString("type").toLowerCase();
                String algo = obj.getString("algo");
                int digits = obj.getInt("digits");
                byte[] secret = toBytes(obj.getJSONArray("secret"));

                String issuer = obj.getString("issuerExt");
                String name = obj.optString("label");

                OtpInfo info;
                switch (type) {
                    case "totp":
                        int period = obj.getInt("period");
                        if (issuer.equals("Steam")) {
                            info = new SteamInfo(secret, algo, digits, period);
                        } else {
                            info = new TotpInfo(secret, algo, digits, period);
                        }
                        break;
                    case "hotp":
                        info = new HotpInfo(secret, algo, digits, obj.getLong("counter"));
                        break;
                    default:
                        throw new DatabaseImporterException("unsupported otp type: " + type);
                }

                return new DatabaseEntry(info, name, issuer);
            } catch (DatabaseImporterException | OtpInfoException | JSONException e) {
                throw new DatabaseImporterEntryException(e, obj.toString());
            }
        }
    }

    private static List<JSONObject> parseXml(XmlPullParser parser)
            throws IOException, XmlPullParserException, JSONException {
        List<JSONObject> entries = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, null, "map");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            if (!parser.getName().equals("string")) {
                skip(parser);
                continue;
            }

            JSONObject entry = parseXmlEntry(parser);
            if (entry != null) {
                entries.add(entry);
            }
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

    private static JSONObject parseXmlEntry(XmlPullParser parser)
            throws IOException, XmlPullParserException, JSONException {
        parser.require(XmlPullParser.START_TAG, null, "string");
        String name = parser.getAttributeValue(null, "name");
        String value = parseXmlText(parser);
        parser.require(XmlPullParser.END_TAG, null, "string");

        if (name.equals("tokenOrder")) {
            return null;
        }

        return new JSONObject(value);
    }

    private static String parseXmlText(XmlPullParser parser) throws IOException, XmlPullParserException {
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
}
