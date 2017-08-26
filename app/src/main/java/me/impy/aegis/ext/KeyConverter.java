package me.impy.aegis.ext;

import java.io.InputStream;
import java.util.List;

import me.impy.aegis.KeyProfile;

public abstract class KeyConverter {
    protected InputStream _stream;

    public KeyConverter(InputStream stream) {
        _stream = stream;
    }

    public abstract List<KeyProfile> convert() throws Exception;
}
