package com.beemdevelopment.aegis.util;

import org.json.JSONObject;

import javax.annotation.Nullable;

public class JsonUtils {
    private JsonUtils() {

    }

    @Nullable
    public static String optString(JSONObject obj, String key) {
        return obj.isNull(key) ? null : obj.optString(key, null);
    }
}
