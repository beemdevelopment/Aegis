package com.beemdevelopment.aegis.ui.fragments.preferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public abstract class PreferencesFragment extends PreferenceFragmentCompat {
    // activity request codes
    public static final int CODE_IMPORT_SELECT = 0;
    public static final int CODE_GROUPS = 3;
    public static final int CODE_IMPORT = 4;
    public static final int CODE_EXPORT = 5;
    public static final int CODE_EXPORT_PLAIN = 6;
    public static final int CODE_EXPORT_GOOGLE_URI = 7;
    public static final int CODE_EXPORT_HTML = 8;
    public static final int CODE_BACKUPS = 9;

    private Intent _result;

    @Inject
    Preferences _prefs;

    @Inject
    VaultManager _vaultManager;

    @Override
    @CallSuper
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setResult(new Intent());
    }

    @Override
    @CallSuper
    public void onResume() {
        super.onResume();

        Intent intent = requireActivity().getIntent();
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
        requireActivity().setResult(Activity.RESULT_OK, _result);
    }

    protected boolean saveAndBackupVault() {
        try {
            _vaultManager.saveAndBackup();
        } catch (VaultRepositoryException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(requireContext(), R.string.saving_error, e);
            return false;
        }

        return true;
    }

    @NonNull
    protected <T extends Preference> T requirePreference(@NonNull CharSequence key) {
        T pref = findPreference(key);
        if (pref == null) {
            throw new IllegalStateException(String.format("Preference %s not found", key));
        }

        return pref;
    }
}
