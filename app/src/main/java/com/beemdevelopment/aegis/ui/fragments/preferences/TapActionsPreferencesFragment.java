package com.beemdevelopment.aegis.ui.fragments.preferences;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.TapAction;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TapActionsPreferencesFragment extends PreferencesFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_tap_actions, rootKey);

        boolean isTapToRevealEnabled = _prefs.isTapToRevealEnabled();
        SwitchPreferenceCompat reserveFirstTap = requirePreference("pref_reserve_first_tap");
        Preference tapActionsHint = requirePreference("pref_tap_actions_hint");

        if (!isTapToRevealEnabled && reserveFirstTap.isChecked()) {
            reserveFirstTap.setChecked(false);
            _prefs.setReserveFirstTapEnabled(false);
        }

        reserveFirstTap.setEnabled(isTapToRevealEnabled);
        tapActionsHint.setVisible(isTapToRevealEnabled);

        Preference singleTapAction = requirePreference("pref_single_tap_action");
        singleTapAction.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.pref_tap_action_entries)[_prefs.getSingleTapAction().ordinal()]));
        singleTapAction.setOnPreferenceClickListener(preference -> {
            int currentSingleTapAction = _prefs.getSingleTapAction().ordinal();
            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.choose_single_tap_action))
                    .setSingleChoiceItems(R.array.pref_tap_action_entries, currentSingleTapAction, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        singleTapAction.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.pref_tap_action_entries)[i]));

                        _prefs.setSingleTapAction(TapAction.fromInteger(i));
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        Preference doubleTapAction = requirePreference("pref_double_tap_action");
        doubleTapAction.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.pref_tap_action_entries)[_prefs.getDoubleTapAction().ordinal()]));
        doubleTapAction.setOnPreferenceClickListener(preference -> {
            int currentDoubleTapAction = _prefs.getDoubleTapAction().ordinal();

            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.choose_double_tap_action))
                    .setSingleChoiceItems(R.array.pref_tap_action_entries, currentDoubleTapAction, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        doubleTapAction.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.pref_tap_action_entries)[i]));

                        _prefs.setDoubleTapAction(TapAction.fromInteger(i));
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });
    }
}
