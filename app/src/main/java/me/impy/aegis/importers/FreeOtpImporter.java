package me.impy.aegis.importers;

import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.otp.HotpInfo;
import me.impy.aegis.otp.OtpInfo;
import me.impy.aegis.otp.OtpInfoException;
import me.impy.aegis.otp.TotpInfo;
import me.impy.aegis.util.ByteInputStream;

public class FreeOtpImporter extends DatabaseImporter {
    private List<XmlEntry> _xmlEntries;

    public FreeOtpImporter(ByteInputStream stream) {
        super(stream);
    }

    private static class XmlEntry {
        String Name;
        String Value;
    }

    @Override
    public void parse() throws DatabaseImporterException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(_stream, null);
            parser.nextTag();
            _xmlEntries = parse(parser);
        } catch (XmlPullParserException | IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    @Override
    public List<DatabaseEntry> convert() throws DatabaseImporterException {
        List<DatabaseEntry> entries = new ArrayList<>();

        try {
            for (XmlEntry xmlEntry : _xmlEntries) {
                if (xmlEntry.Name.equals("tokenOrder")) {
                    // TODO: order
                    JSONArray array = new JSONArray(xmlEntry.Value);
                } else {
                    JSONObject obj = new JSONObject(xmlEntry.Value);

                    String type = obj.getString("type");
                    String algo = obj.getString("algo");
                    int digits = obj.getInt("digits");
                    byte[] secret = toBytes(obj.getJSONArray("secret"));

                    OtpInfo info;
                    if (type.equals("totp")) {
                        info = new TotpInfo(secret, algo, digits, obj.getInt("period"));
                    } else if (type.equals("hotp")) {
                        info = new HotpInfo(secret, algo, digits, obj.getLong("counter"));
                    } else {
                        throw new DatabaseImporterException("unsupported otp type: " + type);
                    }

                    String issuer = obj.getString("issuerExt");
                    String name = obj.optString("label");

                    DatabaseEntry entry = new DatabaseEntry(info, name, issuer);
                    entries.add(entry);
                }
            }
        } catch (OtpInfoException | JSONException e) {
            throw new DatabaseImporterException(e);
        }

        return entries;
    }

    @Override
    public boolean isEncrypted() {
        return false;
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
}
