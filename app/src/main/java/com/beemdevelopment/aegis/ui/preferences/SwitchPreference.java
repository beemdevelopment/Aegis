package com.beemdevelopment.aegis.ui.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;
import com.beemdevelopment.aegis.R;

public class SwitchPreference extends SwitchPreferenceCompat {
    private Preference.OnPreferenceChangeListener _listener;

    public SwitchPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setWidgetLayoutResource(R.layout.preference_widget_switch_custom);
    }

    public SwitchPreference(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWidgetLayoutResource(R.layout.preference_widget_switch_custom);
    }

    public SwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_widget_switch_custom);
    }

    public SwitchPreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_widget_switch_custom);
    }

    @Override
    public void setOnPreferenceChangeListener(
            OnPreferenceChangeListener listener) {
        super.setOnPreferenceChangeListener(listener);
        _listener = listener;
    }

    @Override
    public void setChecked(boolean checked) {
        setChecked(checked, false);
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
