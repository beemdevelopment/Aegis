package me.impy.aegis;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;

import me.impy.aegis.crypto.otp.OTP;
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

    public String refreshCode() {
        try {
            _code = OTP.generateOTP(_entry.getInfo());
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
        return _code;
    }
}
