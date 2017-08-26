package me.impy.aegis;

import java.io.Serializable;

import me.impy.aegis.db.DatabaseEntry;

public class KeyProfile implements Serializable {
    private String _code;
    private DatabaseEntry _entry;

    public KeyProfile(DatabaseEntry entry) {
        _entry = entry;
    }

    public DatabaseEntry getEntry() {
        return _entry;
    }
    public String getCode() {
        return _code;
    }

    public void setCode(String code) {
        _code = code;
    }
}
