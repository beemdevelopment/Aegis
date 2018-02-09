package me.impy.aegis;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;

import me.impy.aegis.crypto.otp.OTP;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.helpers.TextDrawableHelper;

public class KeyProfile implements Serializable {
    private String _code;
    private DatabaseEntry _entry;
    private Listener _listener;

    public KeyProfile() {
        this(new DatabaseEntry());
    }

    public KeyProfile(DatabaseEntry entry) {
        _entry = entry;
    }

    public void setListener(Listener listener) {
        _listener = listener;
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
        if (_listener != null) {
            _listener.onRefreshCode(_code);
        }
        return _code;
    }

    public TextDrawable getDrawable() {
        return TextDrawableHelper.generate(getEntry().getName());
    }

    public interface Listener {
        void onRefreshCode(String code);
    }
}
