package com.beemdevelopment.aegis.util;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PreferenceParser {
    private PreferenceParser() {
        
    }

    public static List<XmlEntry> parse(XmlPullParser parser) throws IOException, XmlPullParserException {
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

    public static class XmlEntry {
        public String Name;
        public String Value;
    }
}
