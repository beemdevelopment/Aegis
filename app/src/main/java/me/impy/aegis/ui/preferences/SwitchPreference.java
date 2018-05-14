package me.impy.aegis.ui.preferences;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;

public class SwitchPreference extends android.preference.SwitchPreference {
    private OnPreferenceChangeListener _listener;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchPreference(Context context) {
        super(context);
    }

    @Override
    public void setOnPreferenceChangeListener(OnPreferenceChangeListener listener) {
        super.setOnPreferenceChangeListener(listener);
        _listener = listener;
    }

    @Override
    public void setChecked(boolean checked) {
        setChecked(true, false);
    }

    public void setChecked(boolean checked, boolean silent) {
        if (silent) {
            super.setOnPreferenceChangeListener(null);
        }
        super.setChecked(checked);
        if (silent) {
            super.setOnPreferenceChangeListener(_listener);
        }
    }
}
