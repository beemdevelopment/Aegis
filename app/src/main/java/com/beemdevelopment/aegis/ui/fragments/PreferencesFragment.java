package com.beemdevelopment.aegis.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.preference.PreferenceFragmentCompat;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.Dialogs;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultManagerException;

public abstract class PreferencesFragment extends PreferenceFragmentCompat {
    // activity request codes
    public static final int CODE_IMPORT = 0;
    public static final int CODE_IMPORT_DECRYPT = 1;
    public static final int CODE_SLOTS = 2;
    public static final int CODE_GROUPS = 3;
    public static final int CODE_SELECT_ENTRIES = 4;
    public static final int CODE_EXPORT = 5;
    public static final int CODE_EXPORT_PLAIN = 6;
    public static final int CODE_EXPORT_GOOGLE_URI = 7;
    public static final int CODE_BACKUPS = 8;

    private AegisApplication _app;
    private Intent _result;
    private Preferences _prefs;
    private VaultManager _vault;

    @Override
    @CallSuper
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        _app = (AegisApplication) getActivity().getApplication();
        _prefs = _app.getPreferences();
        _vault = _app.getVaultManager();

        setResult(new Intent());
    }

    @Override
    @CallSuper
    public void onResume() {
        super.onResume();

        Intent intent = getActivity().getIntent();
        String preference = intent.getStringExtra("pref");
        if (preference != null) {
            scrollToPreference(preference);
            intent.removeExtra("pref");
        }
    }

    public Intent getResult() {
        return _result;
    }

    public void setResult(Intent result) {
        _result = result;
        getActivity().setResult(Activity.RESULT_OK, _result);
    }

    protected AegisApplication getApp() {
        return _app;
    }

    protected Preferences getPreferences() {
        return _prefs;
    }

    protected VaultManager getVault() {
        return _vault;
    }

    protected boolean saveVault() {
        try {
            _vault.save(true);
        } catch (VaultManagerException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(getContext(), R.string.saving_error, e);
            return false;
        }

        return true;
    }
}
