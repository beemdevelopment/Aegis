package com.beemdevelopment.aegis.ui.fragments.preferences;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.AccountNamePosition;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.ui.GroupManagerActivity;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.List;

public class AppearancePreferencesFragment extends PreferencesFragment {
    private Preference _groupsPreference;
    private Preference _resetUsageCountPreference;
    private Preference _currentAccountNamePositionPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_appearance);

        _groupsPreference = requirePreference("pref_groups");
        _groupsPreference.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(requireContext(), GroupManagerActivity.class);
            startActivity(intent);
            return true;
        });

        _resetUsageCountPreference = requirePreference("pref_reset_usage_count");
        _resetUsageCountPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.preference_reset_usage_count)
                    .setMessage(R.string.preference_reset_usage_count_dialog)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> _prefs.clearUsageCount())
                    .setNegativeButton(android.R.string.no, null)
                    .create());
            return true;
        });

        int currentTheme = _prefs.getCurrentTheme().ordinal();
        Preference darkModePreference = requirePreference("pref_dark_mode");
        darkModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.theme_titles)[currentTheme]));
        darkModePreference.setOnPreferenceClickListener(preference -> {
            int currentTheme1 = _prefs.getCurrentTheme().ordinal();

            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.choose_theme)
                    .setSingleChoiceItems(R.array.theme_titles, currentTheme1, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setCurrentTheme(Theme.fromInteger(i));

                        dialog.dismiss();

                        requireActivity().recreate();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        Preference appIconPreference = requirePreference("pref_app_icon");
        String[] appIconTitles = getResources().getStringArray(R.array.app_icon_titles);
        String[] appIconValues = getResources().getStringArray(R.array.app_icon_values);
        int currentAppIconIndex = Arrays.asList(appIconValues).indexOf(_prefs.getAppIcon());
        if (currentAppIconIndex == -1) currentAppIconIndex = 0;
        appIconPreference.setSummary(String.format("%s: %s", getString(R.string.selected), appIconTitles[currentAppIconIndex]));
        
        appIconPreference.setOnPreferenceClickListener(preference -> {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_app_icon_picker, null);
            RecyclerView recyclerView = dialogView.findViewById(R.id.app_icon_recycler);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

            AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_app_icon_title)
                    .setView(dialogView)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();

            recyclerView.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_icon, parent, false);
                    return new RecyclerView.ViewHolder(view) {};
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                    String selectedIconValue = appIconValues[position];
                    String selectedIconTitle = appIconTitles[position];
                    ImageView imageView = holder.itemView.findViewById(R.id.app_icon_image);
                    TextView textView = holder.itemView.findViewById(R.id.app_icon_name);
                    View borderView = holder.itemView.findViewById(R.id.app_icon_border);

                    textView.setText(selectedIconTitle);
                    if (selectedIconValue.equals(_prefs.getAppIcon())) {
                        borderView.setVisibility(View.VISIBLE);
                    } else {
                        borderView.setVisibility(View.GONE);
                    }
                    
                    int resId = R.mipmap.ic_launcher;
                    if (!selectedIconValue.equals("Default")) {
                        String resName = "ic_launcher_" + selectedIconValue.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
                        int dynamicResId = getResources().getIdentifier(resName, "mipmap", requireContext().getPackageName());
                        if (dynamicResId != 0) resId = dynamicResId;
                    }
                    imageView.setImageResource(resId);
                    
                    holder.itemView.setOnClickListener(v -> {
                        dialog.dismiss();
                        if (!selectedIconValue.equals(_prefs.getAppIcon())) {
                            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.pref_app_icon_title)
                                    .setMessage(R.string.pref_app_icon_warning)
                                    .setPositiveButton(android.R.string.ok, (dialog2, which2) -> {
                                        _prefs.setAppIcon(selectedIconValue);
                                        
                                        PackageManager pm = requireContext().getPackageManager();
                                        String pkg = requireContext().getPackageName();
                                        
                                        // Disable old ones with DONT_KILL_APP
                                        for (String val : appIconValues) {
                                            if (!val.equals(selectedIconValue)) {
                                                ComponentName comp = new ComponentName(pkg, "com.beemdevelopment.aegis.Alias" + val);
                                                pm.setComponentEnabledSetting(comp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                            }
                                        }
                                        
                                        // Enable the new one without DONT_KILL_APP so Android forces the launcher refresh and kills the app
                                        ComponentName comp = new ComponentName(pkg, "com.beemdevelopment.aegis.Alias" + selectedIconValue);
                                        pm.setComponentEnabledSetting(comp, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
                                        
                                        dialog2.dismiss();
                                    })
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .create());
                        }
                    });
                }

                @Override
                public int getItemCount() {
                    return appIconValues.length;
                }
            });

            Dialogs.showSecureDialog(dialog);
            return true;
        });

        Preference dynamicColorsPreference = requirePreference("pref_dynamic_colors");
        dynamicColorsPreference.setEnabled(DynamicColors.isDynamicColorAvailable());
        dynamicColorsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            requireActivity().recreate();
            return true;
        });

        Preference langPreference = requirePreference("pref_lang");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            String[] langs = getResources().getStringArray(R.array.pref_lang_values);
            String[] langNames = getResources().getStringArray(R.array.pref_lang_entries);
            List<String> langList = Arrays.asList(langs);
            int curLangIndex = langList.contains(_prefs.getLanguage()) ? langList.indexOf(_prefs.getLanguage()) : 0;
            langPreference.setSummary(langNames[curLangIndex]);
            langPreference.setOnPreferenceClickListener(preference -> {
                Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.pref_lang_title)
                        .setSingleChoiceItems(langNames, curLangIndex, (dialog, which) -> {
                            int newLangIndex = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            _prefs.setLanguage(langs[newLangIndex]);
                            langPreference.setSummary(langNames[newLangIndex]);

                            dialog.dismiss();

                            requireActivity().recreate();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create());
                return true;
            });
        } else {
            // Setting locale doesn't work on Marshmallow or below
            langPreference.setVisible(false);
        }

        int currentViewMode = _prefs.getCurrentViewMode().ordinal();
        Preference viewModePreference = requirePreference("pref_view_mode");
        viewModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.view_mode_titles)[currentViewMode]));
        viewModePreference.setOnPreferenceClickListener(preference -> {
            int currentViewMode1 = _prefs.getCurrentViewMode().ordinal();

            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.choose_view_mode)
                    .setSingleChoiceItems(R.array.view_mode_titles, currentViewMode1, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setCurrentViewMode(ViewMode.fromInteger(i));
                        viewModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.view_mode_titles)[i]));
                        refreshAccountNamePositionText();
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        Preference showExpirationStatePreference = requirePreference("pref_expiration_state");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            showExpirationStatePreference.setSummary(getString(R.string.pref_expiration_state_fallback));
        }

        String[] codeGroupings = getResources().getStringArray(R.array.pref_code_groupings_values);
        String[] codeGroupingNames = getResources().getStringArray(R.array.pref_code_groupings);
        Preference codeDigitGroupingPreference = requirePreference("pref_code_group_size_string");
        codeDigitGroupingPreference.setOnPreferenceClickListener(preference -> {
            int currentCodeGroupingIndex = Arrays.asList(codeGroupings).indexOf(_prefs.getCodeGroupSize().name());

            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_code_group_size_title)
                    .setSingleChoiceItems(codeGroupingNames, currentCodeGroupingIndex, (dialog, which) -> {
                        int newCodeGroupingIndex = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setCodeGroupSize(Preferences.CodeGrouping.valueOf(codeGroupings[newCodeGroupingIndex]));

                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());
            return true;
        });

        int currentAccountNamePosition = _prefs.getAccountNamePosition().ordinal();
        _currentAccountNamePositionPreference = requirePreference("pref_account_name_position");
        _currentAccountNamePositionPreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.account_name_position_titles)[currentAccountNamePosition]));
        _currentAccountNamePositionPreference.setOnPreferenceClickListener(preference -> {
            int currentAccountNamePosition1 = _prefs.getAccountNamePosition().ordinal();

            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.choose_account_name_position))
                    .setSingleChoiceItems(R.array.account_name_position_titles, currentAccountNamePosition1, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setAccountNamePosition(AccountNamePosition.fromInteger(i));
                        _currentAccountNamePositionPreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.account_name_position_titles)[i]));
                        refreshAccountNamePositionText();
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        refreshAccountNamePositionText();
    }

    private void refreshAccountNamePositionText() {
        boolean override = (_prefs.getCurrentViewMode() == ViewMode.TILES && _prefs.getAccountNamePosition() == AccountNamePosition.END);

        if (override) {
            _currentAccountNamePositionPreference.setSummary(String.format("%s: %s. %s", getString(R.string.selected), getResources().getStringArray(R.array.account_name_position_titles)[_prefs.getAccountNamePosition().ordinal()], getString(R.string.pref_account_name_position_summary_override)));
        } else {
            _currentAccountNamePositionPreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.account_name_position_titles)[_prefs.getAccountNamePosition().ordinal()]));
        }
    }
}
