package com.beemdevelopment.aegis.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.BuildConfig;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.crypto.KeyStoreHandle;
import com.beemdevelopment.aegis.crypto.KeyStoreHandleException;
import com.beemdevelopment.aegis.helpers.BiometricSlotInitializer;
import com.beemdevelopment.aegis.helpers.BiometricsHelper;
import com.beemdevelopment.aegis.helpers.SpinnerHelper;
import com.beemdevelopment.aegis.importers.AegisImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporterEntryException;
import com.beemdevelopment.aegis.importers.DatabaseImporterException;
import com.beemdevelopment.aegis.services.NotificationService;
import com.beemdevelopment.aegis.ui.models.ImportEntry;
import com.beemdevelopment.aegis.ui.preferences.SwitchPreference;
import com.beemdevelopment.aegis.ui.tasks.ExportTask;
import com.beemdevelopment.aegis.ui.tasks.PasswordSlotDecryptTask;
import com.beemdevelopment.aegis.util.UUIDMap;
import com.beemdevelopment.aegis.vault.VaultBackupManager;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultFileException;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultManagerException;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.beemdevelopment.aegis.vault.slots.SlotList;
import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.crypto.Cipher;

import static android.text.TextUtils.isDigitsOnly;

public class PreferencesFragment extends PreferenceFragmentCompat {
    // activity request codes
    private static final int CODE_IMPORT = 0;
    private static final int CODE_IMPORT_DECRYPT = 1;
    private static final int CODE_SLOTS = 2;
    private static final int CODE_GROUPS = 3;
    private static final int CODE_SELECT_ENTRIES = 4;
    private static final int CODE_EXPORT = 5;
    private static final int CODE_EXPORT_PLAIN = 6;
    private static final int CODE_EXPORT_GOOGLE_URI = 7;
    private static final int CODE_BACKUPS = 8;

    private Intent _result;
    private Preferences _prefs;
    private VaultManager _vault;

    // keep a reference to the type of database converter the user selected
    private Class<? extends DatabaseImporter> _importerType;
    private AegisImporter.State _importerState;
    private UUIDMap<VaultEntry> _importerEntries;

    private SwitchPreference _encryptionPreference;
    private SwitchPreference _biometricsPreference;
    private Preference _autoLockPreference;
    private Preference _setPasswordPreference;
    private Preference _slotsPreference;
    private Preference _groupsPreference;
    private Preference _passwordReminderPreference;
    private SwitchPreferenceCompat _pinKeyboardPreference;
    private SwitchPreferenceCompat _backupsPreference;
    private Preference _backupsLocationPreference;
    private Preference _backupsTriggerPreference;
    private Preference _backupsVersionsPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        AegisApplication app = (AegisApplication) getActivity().getApplication();
        _prefs = app.getPreferences();
        _vault = app.getVaultManager();

        // set the result intent in advance
        setResult(new Intent());

        int currentTheme = _prefs.getCurrentTheme().ordinal();
        Preference darkModePreference = findPreference("pref_dark_mode");
        darkModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.theme_titles)[currentTheme]));
        darkModePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                int currentTheme = _prefs.getCurrentTheme().ordinal();

                Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.choose_theme)
                        .setSingleChoiceItems(R.array.theme_titles, currentTheme, (dialog, which) -> {
                            int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            _prefs.setCurrentTheme(Theme.fromInteger(i));

                            dialog.dismiss();

                            _result.putExtra("needsRecreate", true);
                            getActivity().recreate();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create());

                return true;
            }
        });

        Preference langPreference = findPreference("pref_lang");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            langPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                _result.putExtra("needsRecreate", true);
                getActivity().recreate();
                return true;
            });
        } else {
            // Setting locale doesn't work on Marshmallow or below
            langPreference.setVisible(false);
        }

        int currentViewMode = _prefs.getCurrentViewMode().ordinal();
        Preference viewModePreference = findPreference("pref_view_mode");
        viewModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.view_mode_titles)[currentViewMode]));
        viewModePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                int currentViewMode = _prefs.getCurrentViewMode().ordinal();

                Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.choose_view_mode)
                        .setSingleChoiceItems(R.array.view_mode_titles, currentViewMode, (dialog, which) -> {
                            int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            _prefs.setCurrentViewMode(ViewMode.fromInteger(i));
                            viewModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.view_mode_titles)[i]));
                            _result.putExtra("needsRefresh", true);
                            dialog.dismiss();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create());

                return true;
            }
        });

        Preference importPreference = findPreference("pref_import");
        importPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Dialogs.showImportersDialog(getContext(), false, definition -> {
                    _importerType = definition.getType();

                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    startActivityForResult(intent, CODE_IMPORT);
                });
                return true;
            }
        });

        Preference importAppPreference = findPreference("pref_import_app");
        importAppPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Dialogs.showImportersDialog(getContext(), true, definition -> {
                    DatabaseImporter importer = DatabaseImporter.create(getContext(), definition.getType());
                    importApp(importer);
                });
                return true;
            }
        });

        Preference exportPreference = findPreference("pref_export");
        exportPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startExport();
                return true;
            }
        });

        /*EditTextPreference timeoutPreference = (EditTextPreference) findPreference("pref_timeout");
        timeoutPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(String.format(getString(R.string.pref_timeout_summary), (String) newValue));
                return true;
            }
        });
        timeoutPreference.getOnPreferenceChangeListener().onPreferenceChange(timeoutPreference, timeoutPreference.getText());*/

        Preference codeDigitGroupingPreference = findPreference("pref_code_group_size");
        codeDigitGroupingPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            _result.putExtra("needsRefresh", true);
            return true;
        });

        Preference issuerPreference = findPreference("pref_account_name");
        issuerPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                _result.putExtra("needsRefresh", true);
                return true;
            }
        });

        Preference searchAccountNamePreference = findPreference("pref_search_names");
        searchAccountNamePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            _result.putExtra("needsRefresh", true);
            return true;
        });

        Preference copyOnTapPreference = findPreference("pref_copy_on_tap");
        copyOnTapPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            _result.putExtra("needsRefresh", true);
            return true;
        });

        Preference entryHighlightPreference = findPreference("pref_highlight_entry");
        entryHighlightPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            _result.putExtra("needsRefresh", true);
            return true;
        });

        Preference tapToRevealPreference = findPreference("pref_tap_to_reveal");
        tapToRevealPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            _result.putExtra("needsRefresh", true);
            return true;
        });

        Preference screenPreference = findPreference("pref_secure_screen");
        screenPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                _result.putExtra("needsRecreate", true);
                Window window = getActivity().getWindow();
                if ((boolean) newValue) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                }
                return true;
            }
        });

        Preference tapToRevealTimePreference = findPreference("pref_tap_to_reveal_time");
        tapToRevealTimePreference.setSummary(app.getPreferences().getTapToRevealTime() + " seconds");
        tapToRevealTimePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Dialogs.showNumberPickerDialog(getActivity(), new Dialogs.NumberInputListener() {
                    @Override
                    public void onNumberInputResult(int number) {
                        app.getPreferences().setTapToRevealTime(number);
                        tapToRevealTimePreference.setSummary(number + " seconds");
                        _result.putExtra("needsRefresh", true);
                    }
                });
                return false;
            }
        });

        _encryptionPreference = (SwitchPreference) findPreference("pref_encryption");
        _encryptionPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!_vault.isEncryptionEnabled()) {
                    Dialogs.showSetPasswordDialog(getActivity(), new EnableEncryptionListener());
                } else {
                    Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.disable_encryption)
                            .setMessage(getString(R.string.disable_encryption_description))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        _vault.disableEncryption();
                                    } catch (VaultManagerException e) {
                                        e.printStackTrace();
                                        Dialogs.showErrorDialog(getContext(), R.string.disable_encryption_error, e);
                                        return;
                                    }

                                    // clear the KeyStore
                                    try {
                                        KeyStoreHandle handle = new KeyStoreHandle();
                                        handle.clear();
                                    } catch (KeyStoreHandleException e) {
                                        e.printStackTrace();
                                    }

                                    getActivity().stopService(new Intent(getActivity(), NotificationService.class));
                                    updateEncryptionPreferences();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .create());
                }
                return false;
            }
        });

        _biometricsPreference = (SwitchPreference) findPreference("pref_biometrics");
        _biometricsPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                VaultFileCredentials creds = _vault.getCredentials();
                SlotList slots = creds.getSlots();

                if (!slots.has(BiometricSlot.class)) {
                    if (BiometricsHelper.isAvailable(getContext())) {
                        BiometricSlotInitializer initializer = new BiometricSlotInitializer(PreferencesFragment.this, new RegisterBiometricsListener());
                        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                                .setTitle(getString(R.string.set_up_biometric))
                                .setNegativeButtonText(getString(android.R.string.cancel))
                                .build();
                        initializer.authenticate(info);
                    }
                } else {
                    // remove the biometric slot
                    BiometricSlot slot = slots.find(BiometricSlot.class);
                    slots.remove(slot);
                    _vault.setCredentials(creds);

                    // remove the KeyStore key
                    try {
                        KeyStoreHandle handle = new KeyStoreHandle();
                        handle.deleteKey(slot.getUUID().toString());
                    } catch (KeyStoreHandleException e) {
                        e.printStackTrace();
                    }

                    saveVault();
                    updateEncryptionPreferences();
                }

                return false;
            }
        });

        _setPasswordPreference = findPreference("pref_password");
        _setPasswordPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showSetPasswordDialog(getActivity(), new SetPasswordListener());
            return false;
        });

        _slotsPreference = findPreference("pref_slots");
        _slotsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), SlotManagerActivity.class);
                intent.putExtra("creds", _vault.getCredentials());
                startActivityForResult(intent, CODE_SLOTS);
                return true;
            }
        });

        _pinKeyboardPreference = findPreference("pref_pin_keyboard");
        _pinKeyboardPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(boolean) newValue) {
                return true;
            }

            Dialogs.showPasswordInputDialog(getActivity(), R.string.set_password_confirm, R.string.pin_keyboard_description, password -> {
                if (isDigitsOnly(new String(password))) {
                    List<PasswordSlot> slots = _vault.getCredentials().getSlots().findAll(PasswordSlot.class);
                    PasswordSlotDecryptTask.Params params = new PasswordSlotDecryptTask.Params(slots, password);
                    PasswordSlotDecryptTask task = new PasswordSlotDecryptTask(getActivity(), new PasswordConfirmationListener());
                    task.execute(getLifecycle(), params);
                } else {
                    setPinKeyboardPreference(false);
                    Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.pin_keyboard_error)
                            .setMessage(R.string.pin_keyboard_error_description)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, null)
                            .create());
                }
            }, dialog -> {
                setPinKeyboardPreference(false);
            });
            return false;
        });

        _groupsPreference = findPreference("pref_groups");
        _groupsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), GroupManagerActivity.class);
                intent.putExtra("groups", new ArrayList<>(_vault.getGroups()));
                startActivityForResult(intent, CODE_GROUPS);
                return true;
            }
        });

        _backupsPreference = findPreference("pref_backups");
        _backupsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                selectBackupsLocation();
            } else {
                _prefs.setIsBackupsEnabled(false);
                updateBackupPreference();
            }

            return false;
        });

        Uri backupLocation = _prefs.getBackupsLocation();
        _backupsLocationPreference = findPreference("pref_backups_location");
        if (backupLocation != null) {
            _backupsLocationPreference.setSummary(String.format("%s: %s", getString(R.string.pref_backups_location_summary), Uri.decode(backupLocation.toString())));
        }
        _backupsLocationPreference.setOnPreferenceClickListener(preference -> {
            selectBackupsLocation();
            return false;
        });

        _backupsTriggerPreference = findPreference("pref_backups_trigger");
        _backupsTriggerPreference.setOnPreferenceClickListener(preference -> {
            if (_prefs.isBackupsEnabled()) {
                try {
                    _vault.backup();
                    Toast.makeText(getActivity(), R.string.backup_successful, Toast.LENGTH_LONG).show();
                } catch (VaultManagerException e) {
                    e.printStackTrace();
                    Dialogs.showErrorDialog(getContext(), R.string.backup_error, e);
                }
            }
            return true;
        });

        _backupsVersionsPreference = findPreference("pref_backups_versions");
        _backupsVersionsPreference.setSummary(getResources().getQuantityString(R.plurals.pref_backups_versions_summary, _prefs.getBackupsVersionCount(), _prefs.getBackupsVersionCount()));
        _backupsVersionsPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showBackupVersionsPickerDialog(getActivity(), number -> {
                number = number * 5 + 5;
                _prefs.setBackupsVersionCount(number);
                _backupsVersionsPreference.setSummary(getResources().getQuantityString(R.plurals.pref_backups_versions_summary, _prefs.getBackupsVersionCount(), _prefs.getBackupsVersionCount()));
            });
            return false;
        });

        _autoLockPreference = findPreference("pref_auto_lock");
        _autoLockPreference.setSummary(getAutoLockSummary());
        _autoLockPreference.setOnPreferenceClickListener((preference) -> {
            final int[] items = Preferences.AUTO_LOCK_SETTINGS;
            final String[] textItems = getResources().getStringArray(R.array.pref_auto_lock_types);
            final boolean[] checkedItems = new boolean[items.length];
            for (int i = 0; i < items.length; i++) {
                checkedItems[i] = _prefs.isAutoLockTypeEnabled(items[i]);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.pref_auto_lock_prompt)
                    .setMultiChoiceItems(textItems, checkedItems, (dialog, index, isChecked) -> checkedItems[index] = isChecked)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        int autoLock = Preferences.AUTO_LOCK_OFF;
                        for (int i = 0; i < checkedItems.length; i++) {
                            if (checkedItems[i]) {
                                autoLock |= items[i];
                            }
                        }

                        _prefs.setAutoLockMask(autoLock);
                        _autoLockPreference.setSummary(getAutoLockSummary());
                    })
                    .setNegativeButton(android.R.string.cancel, null);
            Dialogs.showSecureDialog(builder.create());

            return false;
        });

        _passwordReminderPreference = findPreference("pref_password_reminder");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEncryptionPreferences();
        updateBackupPreference();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return;
        }

        switch (requestCode) {
            case CODE_IMPORT:
                onImportResult(resultCode, data);
                break;
            case CODE_IMPORT_DECRYPT:
                onImportDecryptResult(resultCode, data);
                break;
            case CODE_SLOTS:
                onSlotManagerResult(resultCode, data);
                break;
            case CODE_GROUPS:
                onGroupManagerResult(resultCode, data);
                break;
            case CODE_SELECT_ENTRIES:
                onSelectEntriesResult(resultCode, data);
                break;
            case CODE_EXPORT:
                // intentional fallthrough
            case CODE_EXPORT_PLAIN:
                // intentional fallthrough
            case CODE_EXPORT_GOOGLE_URI:
                onExportResult(requestCode, resultCode, data);
                break;
            case CODE_BACKUPS:
                onSelectBackupsLocationResult(resultCode, data);
                break;
        }
    }

    public Intent getResult() {
        return _result;
    }

    public void setResult(Intent result) {
        _result = result;
        getActivity().setResult(Activity.RESULT_OK, _result);
    }

    private void importApp(DatabaseImporter importer) {
        // obtain the global root shell and close it immediately after we're done
        // TODO: find a way to use SuFileInputStream with Shell.newInstance()
        try (Shell shell = Shell.getShell()) {
            if (!shell.isRoot()) {
                Toast.makeText(getActivity(), R.string.root_error, Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseImporter.State state = importer.readFromApp();
            processImporterState(state);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.app_lookup_error, Toast.LENGTH_SHORT).show();
        } catch (IOException | DatabaseImporterException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.reading_file_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void processImporterState(DatabaseImporter.State state) {
        try {
            if (state.isEncrypted()) {
                // temporary special case for encrypted Aegis vaults
                if (state instanceof AegisImporter.EncryptedState) {
                    _importerState = state;

                    Intent intent = new Intent(getActivity(), AuthActivity.class);
                    intent.putExtra("slots", ((AegisImporter.EncryptedState) state).getSlots());
                    startActivityForResult(intent, CODE_IMPORT_DECRYPT);
                } else {
                    state.decrypt(getActivity(), new DatabaseImporter.DecryptListener() {
                        @Override
                        public void onStateDecrypted(DatabaseImporter.State state) {
                            importDatabase(state);
                        }

                        @Override
                        public void onError(Exception e) {
                            e.printStackTrace();
                            Dialogs.showErrorDialog(getContext(), R.string.decryption_error, e);
                        }
                    });
                }
            } else {
                importDatabase(state);
            }
        } catch (DatabaseImporterException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(getContext(), R.string.parsing_file_error, e);
        }
    }

    private void onImportDecryptResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            _importerState = null;
            return;
        }

        VaultFileCredentials creds = (VaultFileCredentials) data.getSerializableExtra("creds");
        DatabaseImporter.State state;
        try {
            state = ((AegisImporter.EncryptedState) _importerState).decrypt(creds);
        } catch (VaultFileException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(getContext(), R.string.decryption_error, e);
            return;
        }

        importDatabase(state);
        _importerState = null;
    }

    private void onImportResult(int resultCode, Intent data) {
        Uri uri = data.getData();
        if (resultCode != Activity.RESULT_OK || uri == null) {
            return;
        }

        try (InputStream stream = getContext().getContentResolver().openInputStream(uri)) {
            DatabaseImporter importer = DatabaseImporter.create(getContext(), _importerType);
            DatabaseImporter.State state = importer.read(stream);
            processImporterState(state);
        } catch (FileNotFoundException e) {
            Toast.makeText(getActivity(), R.string.file_not_found, Toast.LENGTH_SHORT).show();
        } catch (DatabaseImporterException | IOException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(getContext(), R.string.reading_file_error, e);
        }
    }

    private void importDatabase(DatabaseImporter.State state) {
        DatabaseImporter.Result result;
        try {
            result = state.convert();
        } catch (DatabaseImporterException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(getContext(), R.string.parsing_file_error, e);
            return;
        }

        _importerEntries = result.getEntries();
        List<ImportEntry> entries = new ArrayList<>();
        for (VaultEntry entry : _importerEntries) {
            entries.add(new ImportEntry(entry));
        }

        Intent intent = new Intent(getActivity(), SelectEntriesActivity.class);
        intent.putExtra("entries", (ArrayList<ImportEntry>) entries);
        intent.putExtra("errors", (ArrayList<DatabaseImporterEntryException>) result.getErrors());
        intent.putExtra("vaultContainsEntries", _vault.getEntries().size() > 0);
        startActivityForResult(intent, CODE_SELECT_ENTRIES);
    }

    private void startExport() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_export, null);
        TextView warningText = view.findViewById(R.id.text_export_warning);
        CheckBox checkBoxEncrypt = view.findViewById(R.id.checkbox_export_encrypt);
        CheckBox checkBoxAccept = view.findViewById(R.id.checkbox_accept);
        Spinner spinner = view.findViewById(R.id.spinner_export_format);
        SpinnerHelper.fillSpinner(getContext(), spinner, R.array.export_formats);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                checkBoxEncrypt.setChecked(position == 0);
                checkBoxEncrypt.setEnabled(position == 0);
                warningText.setVisibility(checkBoxEncrypt.isChecked() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.pref_export_summary)
                .setView(view)
                .setNeutralButton(R.string.share, null)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnPos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

            checkBoxEncrypt.setOnCheckedChangeListener((buttonView, isChecked) -> {
                warningText.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                checkBoxAccept.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                checkBoxAccept.setChecked(false);
                btnPos.setEnabled(isChecked);
                btnNeutral.setEnabled(isChecked);
            });

            checkBoxAccept.setOnCheckedChangeListener((buttonView, isChecked) -> {
                btnPos.setEnabled(isChecked);
                btnNeutral.setEnabled(isChecked);
            });

            btnPos.setOnClickListener(v -> {
                dialog.dismiss();

                if (!checkBoxEncrypt.isChecked() && !checkBoxAccept.isChecked()) {
                    return;
                }

                int requestCode = getExportRequestCode(spinner.getSelectedItemPosition(), checkBoxEncrypt.isChecked());
                VaultBackupManager.FileInfo fileInfo = getExportFileInfo(spinner.getSelectedItemPosition(), checkBoxEncrypt.isChecked());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(getExportMimeType(requestCode))
                        .putExtra(Intent.EXTRA_TITLE, fileInfo.toString());

                startActivityForResult(intent, requestCode);
            });

            btnNeutral.setOnClickListener(v -> {
                dialog.dismiss();

                if (!checkBoxEncrypt.isChecked() && !checkBoxAccept.isChecked()) {
                    return;
                }

                File file;
                try {
                    VaultBackupManager.FileInfo fileInfo = getExportFileInfo(spinner.getSelectedItemPosition(), checkBoxEncrypt.isChecked());
                    file = File.createTempFile(fileInfo.getFilename() + "-", "." + fileInfo.getExtension(), getExportCacheDir());
                } catch (IOException e) {
                    e.printStackTrace();
                    Dialogs.showErrorDialog(getContext(), R.string.exporting_vault_error, e);
                    return;
                }

                int requestCode = getExportRequestCode(spinner.getSelectedItemPosition(), checkBoxEncrypt.isChecked());
                startExportVault(requestCode, cb -> {
                    try (OutputStream stream = new FileOutputStream(file)) {
                        cb.exportVault(stream);
                    } catch (IOException | VaultManagerException e) {
                        e.printStackTrace();
                        Dialogs.showErrorDialog(getContext(), R.string.exporting_vault_error, e);
                        return;
                    }

                    Uri uri = FileProvider.getUriForFile(getContext(), BuildConfig.FILE_PROVIDER_AUTHORITY, file);
                    Intent intent = new Intent(Intent.ACTION_SEND)
                            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            .setType(getExportMimeType(requestCode))
                            .putExtra(Intent.EXTRA_STREAM, uri);
                    Intent chooser = Intent.createChooser(intent, getString(R.string.pref_export_summary));
                    startActivity(chooser);
                });
            });
        });

        Dialogs.showSecureDialog(dialog);
    }

    private void onSlotManagerResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        VaultFileCredentials creds = (VaultFileCredentials) data.getSerializableExtra("creds");
        _vault.setCredentials(creds);
        saveVault();
        updateEncryptionPreferences();
    }

    private void onGroupManagerResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        HashSet<String> groups = new HashSet<>(data.getStringArrayListExtra("groups"));

        for (VaultEntry entry : _vault.getEntries()) {
            if (!groups.contains(entry.getGroup())) {
                entry.setGroup(null);
            }
        }

        saveVault();
    }

    private void onSelectEntriesResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        boolean wipeEntries = data.getBooleanExtra("wipeEntries", false);
        if (wipeEntries) {
            _vault.wipeEntries();
        }

        List<ImportEntry> selectedEntries = (ArrayList<ImportEntry>) data.getSerializableExtra("entries");
        for (ImportEntry selectedEntry : selectedEntries) {
            VaultEntry savedEntry = _importerEntries.getByUUID(selectedEntry.getUUID());

            // temporary: randomize the UUID of duplicate entries and add them anyway
            if (_vault.isEntryDuplicate(savedEntry)) {
                savedEntry.resetUUID();
            }

            _vault.addEntry(savedEntry);
        }

        _importerEntries = null;
        if (!saveVault()) {
            return;
        }

        String toastMessage = getResources().getQuantityString(R.plurals.imported_entries_count, selectedEntries.size(), selectedEntries.size());
        Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();

        _result.putExtra("needsRecreate", true);
    }

    private static int getExportRequestCode(int spinnerPos, boolean encrypt) {
        if (spinnerPos == 0) {
            return encrypt ? CODE_EXPORT : CODE_EXPORT_PLAIN;
        }

        return CODE_EXPORT_GOOGLE_URI;
    }

    private static VaultBackupManager.FileInfo getExportFileInfo(int spinnerPos, boolean encrypt) {
        if (spinnerPos == 0) {
            String filename = encrypt ? VaultManager.FILENAME_PREFIX_EXPORT : VaultManager.FILENAME_PREFIX_EXPORT_PLAIN;
            return new VaultBackupManager.FileInfo(filename);
        }

        return new VaultBackupManager.FileInfo(VaultManager.FILENAME_PREFIX_EXPORT_URI, "txt");
    }

    private static String getExportMimeType(int requestCode) {
        return requestCode == CODE_EXPORT_GOOGLE_URI ? "text/plain" : "application/json";
    }

    private File getExportCacheDir() throws IOException {
        File dir = new File(getContext().getCacheDir(), "export");
        if (!dir.exists() && !dir.mkdir()) {
            throw new IOException(String.format("Unable to create directory %s", dir));
        }

        return dir;
    }

    private void startExportVault(int requestCode, StartExportCallback cb) {
        switch (requestCode) {
            case CODE_EXPORT:
                if (_vault.isEncryptionEnabled()) {
                    cb.exportVault(stream -> _vault.export(stream));
                } else {
                    Dialogs.showSetPasswordDialog(getActivity(), new Dialogs.SlotListener() {
                        @Override
                        public void onSlotResult(Slot slot, Cipher cipher) {
                            VaultFileCredentials creds = new VaultFileCredentials();

                            try {
                                slot.setKey(creds.getKey(), cipher);
                                creds.getSlots().add(slot);
                            } catch (SlotException e) {
                                onException(e);
                                return;
                            }

                            cb.exportVault(stream -> _vault.export(stream, creds));
                        }

                        @Override
                        public void onException(Exception e) {

                        }
                    });
                }
                break;
            case CODE_EXPORT_PLAIN:
                cb.exportVault((stream) -> _vault.export(stream, null));
                break;
            case CODE_EXPORT_GOOGLE_URI:
                cb.exportVault((stream) -> _vault.exportGoogleUris(stream));
                break;
        }
    }

    private void onExportResult(int requestCode, int resultCode, Intent data) {
        Uri uri = data.getData();
        if (resultCode != Activity.RESULT_OK || uri == null) {
            return;
        }


        startExportVault(requestCode, cb -> {
            File file;
            OutputStream outStream = null;
            try {
                file = File.createTempFile(VaultManager.FILENAME_PREFIX_EXPORT + "-", ".json", getExportCacheDir());
                outStream = new FileOutputStream(file);
                cb.exportVault(outStream);

                new ExportTask(getContext(), new ExportResultListener()).execute(getLifecycle(), new ExportTask.Params(file, uri));
            } catch (VaultManagerException | IOException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(getContext(), R.string.exporting_vault_error, e);
            } finally {
                try {
                    if (outStream != null) {
                        outStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void onSelectBackupsLocationResult(int resultCode, Intent data) {
        Uri uri = data.getData();
        if (resultCode != Activity.RESULT_OK || uri == null) {
            return;
        }

        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContext().getContentResolver().takePersistableUriPermission(data.getData(), flags);

        _prefs.setBackupsLocation(uri);
        _prefs.setIsBackupsEnabled(true);
        _prefs.setBackupsError(null);
        _backupsLocationPreference.setSummary(String.format("%s: %s", getString(R.string.pref_backups_location_summary), Uri.decode(uri.toString())));
        updateBackupPreference();
    }

    private boolean saveVault() {
        try {
            _vault.save(true);
        } catch (VaultManagerException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(getContext(), R.string.saving_error, e);
            return false;
        }

        return true;
    }

    private void updateEncryptionPreferences() {
        boolean encrypted = _vault.isEncryptionEnabled();
        _encryptionPreference.setChecked(encrypted, true);
        _setPasswordPreference.setVisible(encrypted);
        _biometricsPreference.setVisible(encrypted);
        _slotsPreference.setEnabled(encrypted);
        _autoLockPreference.setVisible(encrypted);
        _pinKeyboardPreference.setVisible(encrypted);

        if (encrypted) {
            SlotList slots = _vault.getCredentials().getSlots();
            boolean multiPassword = slots.findAll(PasswordSlot.class).size() > 1;
            boolean multiBio = slots.findAll(BiometricSlot.class).size() > 1;
            boolean showSlots = BuildConfig.DEBUG || multiPassword || multiBio;
            boolean canUseBio = BiometricsHelper.isAvailable(getContext());
            _setPasswordPreference.setEnabled(!multiPassword);
            _biometricsPreference.setEnabled(canUseBio && !multiBio);
            _biometricsPreference.setChecked(slots.has(BiometricSlot.class), true);
            _slotsPreference.setVisible(showSlots);
            _passwordReminderPreference.setVisible(slots.has(BiometricSlot.class));
        } else {
            _setPasswordPreference.setEnabled(false);
            _biometricsPreference.setEnabled(false);
            _biometricsPreference.setChecked(false, true);
            _slotsPreference.setVisible(false);
            _passwordReminderPreference.setVisible(false);
        }

        updateBackupPreference();
    }

    private void updateBackupPreference() {
        boolean encrypted = _vault.isEncryptionEnabled();
        boolean backupEnabled = _prefs.isBackupsEnabled() && encrypted;
        _backupsPreference.setChecked(backupEnabled);
        _backupsPreference.setEnabled(encrypted);
        _backupsLocationPreference.setVisible(backupEnabled);
        _backupsTriggerPreference.setVisible(backupEnabled);
        _backupsVersionsPreference.setVisible(backupEnabled);
    }

    private void selectBackupsLocation() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        startActivityForResult(intent, CODE_BACKUPS);
    }

    private void setPinKeyboardPreference(boolean enable) {
        _pinKeyboardPreference.setChecked(enable);
    }

    private String getAutoLockSummary() {
        final int[] settings = Preferences.AUTO_LOCK_SETTINGS;
        final String[] descriptions = getResources().getStringArray(R.array.pref_auto_lock_types);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < settings.length; i++) {
            if (_prefs.isAutoLockTypeEnabled(settings[i])) {
                if (builder.length() != 0) {
                    builder.append(", ");
                }

                builder.append(descriptions[i].toLowerCase());
            }
        }

        if (builder.length() == 0) {
            return getString(R.string.pref_auto_lock_summary_disabled);
        }

        return getString(R.string.pref_auto_lock_summary, builder.toString());
    }

    private class SetPasswordListener implements Dialogs.SlotListener {
        @Override
        public void onSlotResult(Slot slot, Cipher cipher) {
            VaultFileCredentials creds = _vault.getCredentials();
            SlotList slots = creds.getSlots();

            try {
                // encrypt the master key for this slot
                slot.setKey(creds.getKey(), cipher);

                // remove the old master password slot
                PasswordSlot oldSlot = creds.getSlots().find(PasswordSlot.class);
                if (oldSlot != null) {
                    slots.remove(oldSlot);
                }

                // add the new master password slot
                slots.add(slot);
            } catch (SlotException e) {
                onException(e);
                return;
            }

            _vault.setCredentials(creds);
            saveVault();

            if (_prefs.isPinKeyboardEnabled()) {
                setPinKeyboardPreference(false);
                Toast.makeText(getContext(), R.string.pin_keyboard_disabled, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onException(Exception e) {
            e.printStackTrace();
            updateEncryptionPreferences();
            Dialogs.showErrorDialog(getContext(), R.string.encryption_set_password_error, e);
        }
    }

    private class RegisterBiometricsListener implements BiometricSlotInitializer.Listener {
        @Override
        public void onInitializeSlot(BiometricSlot slot, Cipher cipher) {
            VaultFileCredentials creds = _vault.getCredentials();
            try {
                slot.setKey(creds.getKey(), cipher);
            } catch (SlotException e) {
                e.printStackTrace();
                onSlotInitializationFailed(0, e.toString());
                return;
            }
            creds.getSlots().add(slot);
            _vault.setCredentials(creds);

            saveVault();
            updateEncryptionPreferences();
        }

        @Override
        public void onSlotInitializationFailed(int errorCode, @NonNull CharSequence errString) {
            if (!BiometricsHelper.isCanceled(errorCode)) {
                Dialogs.showErrorDialog(getContext(), R.string.encryption_enable_biometrics_error, errString);
            }
        }
    }

    private class EnableEncryptionListener implements Dialogs.SlotListener {
        @Override
        public void onSlotResult(Slot slot, Cipher cipher) {
            VaultFileCredentials creds = new VaultFileCredentials();

            try {
                slot.setKey(creds.getKey(), cipher);
                creds.getSlots().add(slot);
                _vault.enableEncryption(creds);
            } catch (VaultManagerException | SlotException e) {
                onException(e);
                return;
            }

            getActivity().startService(new Intent(getActivity(), NotificationService.class));
            updateEncryptionPreferences();
        }

        @Override
        public void onException(Exception e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(getContext(), R.string.encryption_set_password_error, e);
        }
    }

    private class PasswordConfirmationListener implements PasswordSlotDecryptTask.Callback {
        @Override
        public void onTaskFinished(PasswordSlotDecryptTask.Result result) {
            if (result != null) {
                setPinKeyboardPreference(true);
            } else {
                Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.pin_keyboard_error)
                        .setMessage(R.string.invalid_password)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, null)
                        .create());
                setPinKeyboardPreference(false);
            }
        }
    }

    private class ExportResultListener implements ExportTask.Callback {
        @Override
        public void onTaskFinished(Exception e) {
            if (e != null) {
                e.printStackTrace();
                Dialogs.showErrorDialog(getContext(), R.string.exporting_vault_error, e);
            } else {
                Toast.makeText(getContext(), getString(R.string.exported_vault), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private interface FinishExportCallback {
        void exportVault(OutputStream stream) throws IOException, VaultManagerException;
    }

    private interface StartExportCallback {
        void exportVault(FinishExportCallback exportCb);
    }
}
