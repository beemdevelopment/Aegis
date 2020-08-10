package com.beemdevelopment.aegis.licenses;

import android.content.Context;

import com.beemdevelopment.aegis.R;

import de.psdev.licensesdialog.licenses.License;

public class ProtobufLicense extends License  {
    @Override
    public String getName() {
        return "Protocol Buffers License";
    }

    @Override
    public String readSummaryTextFromResources(Context context) {
        return getContent(context, R.raw.protobuf_license);
    }

    @Override
    public String readFullTextFromResources(Context context) {
        return getContent(context, R.raw.protobuf_license);
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public String getUrl() {
        return "https://raw.githubusercontent.com/protocolbuffers/protobuf/master/LICENSE";
    }
}
