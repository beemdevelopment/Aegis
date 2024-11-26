package com.beemdevelopment.aegis.ui.fragments.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.database.AuditLogRepository;
import com.beemdevelopment.aegis.helpers.AnimationsHelper;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public abstract class PreferencesFragment extends PreferenceFragmentCompat {
    // activity request codes
    public static final int CODE_EXPORT = 5;
    public static final int CODE_EXPORT_PLAIN = 6;
    public static final int CODE_EXPORT_GOOGLE_URI = 7;
    public static final int CODE_EXPORT_HTML = 8;

    @Inject
    Preferences _prefs;

    @Inject
    VaultManager _vaultManager;

    @Inject
    protected AuditLogRepository _auditLogRepository;

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

    @Override
    @Nullable
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (nextAnim != 0) {
            return AnimationsHelper.loadScaledAnimation(requireContext(), nextAnim, AnimationsHelper.Scale.TRANSITION);
        }

        return super.onCreateAnimation(transit, enter, nextAnim);
    }

    @NonNull
    @Override
    public RecyclerView onCreateRecyclerView(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (targetView, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            targetView.setPadding(
                    0,
                    0,
                    0,
                    insets.bottom
            );
            return WindowInsetsCompat.CONSUMED;
        });
        return recyclerView;
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
