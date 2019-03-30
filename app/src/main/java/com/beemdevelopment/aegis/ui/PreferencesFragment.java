package com.beemdevelopment.aegis.ui;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.db.DatabaseFileCredentials;
import com.beemdevelopment.aegis.helpers.FingerprintHelper;
import com.beemdevelopment.aegis.helpers.PermissionHelper;
import com.beemdevelopment.aegis.importers.AegisFileImporter;
import com.beemdevelopment.aegis.importers.DatabaseAppImporter;
import com.beemdevelopment.aegis.importers.DatabaseFileImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporterEntryException;
import com.beemdevelopment.aegis.importers.DatabaseImporterException;
import com.beemdevelopment.aegis.importers.DatabaseImporterResult;
import com.beemdevelopment.aegis.ui.preferences.SwitchPreference;
import com.beemdevelopment.aegis.util.ByteInputStream;
import com.google.android.material.snackbar.Snackbar;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.BuildConfig;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.db.DatabaseManager;
import com.beemdevelopment.aegis.db.DatabaseManagerException;
import com.beemdevelopment.aegis.db.slots.FingerprintSlot;
import com.beemdevelopment.aegis.db.slots.PasswordSlot;
import com.beemdevelopment.aegis.db.slots.Slot;
import com.beemdevelopment.aegis.db.slots.SlotException;
import com.beemdevelopment.aegis.db.slots.SlotList;
import com.topjohnwu.superuser.Shell;

public class PreferencesFragment extends PreferenceFragmentCompat {
    // activity request codes
    private static final int CODE_IMPORT = 0;
    private static final int CODE_IMPORT_DECRYPT = 1;
    private static final int CODE_SLOTS = 2;
    private static final int CODE_GROUPS = 3;

    // permission request codes
    private static final int CODE_PERM_IMPORT = 0;
    private static final int CODE_PERM_EXPORT = 1;

    private Intent _result;
    private DatabaseManager _db;

    // this is used to keep a reference to a database converter
    // while the user provides credentials to decrypt it
    private DatabaseFileImporter _importer;
    private Class<? extends DatabaseFileImporter> _importerType;

    private SwitchPreference _encryptionPreference;
    private SwitchPreference _fingerprintPreference;
    private Preference _setPasswordPreference;
    private Preference _slotsPreference;
    private Preference _groupsPreference;

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        AegisApplication app = (AegisApplication) getActivity().getApplication();
        _db = app.getDatabaseManager();

        // set the result intent in advance
        setResult(new Intent());

        int currentTheme = app.getPreferences().getCurrentTheme();
        Preference darkModePreference = findPreference("pref_dark_mode");
        darkModePreference.setSummary("Selected: " + Theme.getThemeName(currentTheme));
        darkModePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String[] themeNames = Theme.getThemeNames();
                int checkedTheme = app.getPreferences().getCurrentTheme();

                Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.choose_theme))
                        .setSingleChoiceItems(themeNames, checkedTheme, (dialog, which) -> {
                            int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            app.getPreferences().setCurrentTheme(Theme.fromInteger(i));

                            dialog.dismiss();

                            _result.putExtra("needsRecreate", true);
                            getActivity().recreate();
                        })
                        .setPositiveButton(android.R.string.ok, null)
                        .create());

                return true;
            }
        });

        darkModePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                _result.putExtra("needsRecreate", true);
                getActivity().recreate();
                return true;
            }
        });

        Preference importPreference = findPreference("pref_import");
        importPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onImport();
                return true;
            }
        });

        Preference importAppPreference = findPreference("pref_import_app");
        importAppPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onImportApp();
                return true;
            }
        });


        Preference exportPreference = findPreference("pref_export");
        exportPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onExport();
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

        Preference issuerPreference = findPreference("pref_account_name");
        issuerPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                _result.putExtra("needsRefresh", true);
                return true;
            }
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
                if ((boolean)newValue) {
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
                if (!_db.isEncryptionEnabled()) {
                    Dialogs.showSetPasswordDialog(getActivity(), new EnableEncryptionListener());
                } else {
                    Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.disable_encryption))
                            .setMessage(getString(R.string.disable_encryption_description))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        _db.disableEncryption();
                                    } catch (DatabaseManagerException e) {
                                        Toast.makeText(getActivity(), getString(R.string.encrypting_error), Toast.LENGTH_SHORT).show();
                                    }
                                    updateEncryptionPreferences();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .create());
                }
                return false;
            }
        });

        _fingerprintPreference = (SwitchPreference) findPreference("pref_fingerprint");
        _fingerprintPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                DatabaseFileCredentials creds = _db.getCredentials();
                SlotList slots = creds.getSlots();

                if (!slots.has(FingerprintSlot.class)) {
                    Dialogs.showFingerprintDialog(getActivity(), new RegisterFingerprintListener());
                } else {
                    // remove the fingerprint slot
                    FingerprintSlot slot = slots.find(FingerprintSlot.class);
                    slots.remove(slot);
                    _db.setCredentials(creds);

                    saveDatabase();
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
                intent.putExtra("creds", _db.getCredentials());
                startActivityForResult(intent, CODE_SLOTS);
                return true;
            }
        });

        _groupsPreference = findPreference("pref_groups");
        _groupsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), GroupManagerActivity.class);
                intent.putExtra("groups", new ArrayList<>(_db.getGroups()));
                startActivityForResult(intent, CODE_GROUPS);
                return true;
            }
        });
        updateEncryptionPreferences();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (!PermissionHelper.checkResults(grantResults)) {
            Toast.makeText(getActivity(), getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            return;
        }

        switch (requestCode) {
            case CODE_PERM_IMPORT:
                onImport();
                break;
            case CODE_PERM_EXPORT:
                onExport();
                break;
        }
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
        }
    }

    public Intent getResult() {
        return _result;
    }

    public void setResult(Intent result) {
        _result = result;
        getActivity().setResult(Activity.RESULT_OK, _result);
    }

    private void onImport() {
        if (!PermissionHelper.request(getActivity(), CODE_PERM_IMPORT, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return;
        }

        Map<String, Class<? extends DatabaseFileImporter>> importers = DatabaseFileImporter.getImporters();
        String[] names = importers.keySet().toArray(new String[importers.size()]);

        Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.choose_application))
                .setSingleChoiceItems(names, 0, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _importerType = importers.get(names[i]);

                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        startActivityForResult(intent, CODE_IMPORT);
                    }
                })
                .create());
    }

    private void onImportApp() {
        Map<String, Class<? extends DatabaseAppImporter>> importers = DatabaseAppImporter.getImporters();
        String[] names = importers.keySet().toArray(new String[importers.size()]);

        Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.choose_application))
                .setSingleChoiceItems(names, 0, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        try {
                            DatabaseAppImporter importer;
                            // obtain the global root shell and close it immediately after we're done
                            // TODO: find a way to use SuFileInputStream with Shell.newInstance()
                            try (Shell shell = Shell.getShell()) {
                                if (!shell.isRoot()) {
                                    Toast.makeText(getActivity(), getString(R.string.root_error), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                importer = DatabaseAppImporter.create(getContext(), importers.get(names[i]));
                                importer.parse();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), getString(R.string.root_error), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            importDatabase(importer);
                        } catch (DatabaseImporterException e) {
                            e.printStackTrace();

                            String msg = String.format("%s: %s", getString(R.string.parsing_file_error), e.getMessage());
                            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .create());
    }

    private void onImportDecryptResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            _importer = null;
            return;
        }

        DatabaseFileCredentials creds = (DatabaseFileCredentials) data.getSerializableExtra("creds");
        ((AegisFileImporter)_importer).setCredentials(creds);

        try {
            importDatabase(_importer);
        } catch (DatabaseImporterException e) {
            e.printStackTrace();

            String msg = String.format("%s: %s", getString(R.string.parsing_file_error), e.getMessage());
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        }

        _importer = null;
    }

    private void onImportResult(int resultCode, Intent data) {
        Uri uri = data.getData();
        if (resultCode != Activity.RESULT_OK || uri == null) {
            return;
        }

        ByteInputStream stream;

        try (InputStream fileStream = getActivity().getContentResolver().openInputStream(uri)) {
            stream = ByteInputStream.create(fileStream);
        } catch (FileNotFoundException e) {
            Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), getString(R.string.reading_file_error), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            DatabaseFileImporter importer = DatabaseFileImporter.create(getContext(), stream, _importerType);
            importer.parse();

            // special case to decrypt encrypted aegis databases
            if (importer.isEncrypted() && importer instanceof AegisFileImporter) {
                _importer = importer;

                Intent intent = new Intent(getActivity(), AuthActivity.class);
                intent.putExtra("slots", ((AegisFileImporter)_importer).getFile().getHeader().getSlots());
                startActivityForResult(intent, CODE_IMPORT_DECRYPT);
                return;
            }

            importDatabase(importer);
        } catch (DatabaseImporterException e) {
            e.printStackTrace();

            String msg = String.format("%s: %s", getString(R.string.parsing_file_error), e.getMessage());
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void importDatabase(DatabaseImporter importer) throws DatabaseImporterException {
        DatabaseImporterResult result = importer.convert();
        List<DatabaseEntry> entries = result.getEntries();
        List<DatabaseImporterEntryException> errors = result.getErrors();

        for (DatabaseEntry entry : entries) {
            // temporary: randomize the UUID of duplicate entries and add them anyway
            if (_db.getEntryByUUID(entry.getUUID()) != null) {
                entry.resetUUID();
            }

            _db.addEntry(entry);
        }

        if (!saveDatabase()) {
            return;
        }

        _result.putExtra("needsRecreate", true);
        Snackbar bar = Snackbar.make(getView(), String.format(Locale.getDefault(), getString(R.string.imported_entries_count), entries.size(), errors.size()), Snackbar.LENGTH_LONG);
        if (errors.size() == 0) {
            bar.setAction(R.string.details, v -> {

            });
        }
        bar.show();
    }

    private void onExport() {
        if (!PermissionHelper.request(getActivity(), CODE_PERM_EXPORT, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return;
        }

        // TODO: create a custom layout to show a message AND a checkbox
        final AtomicReference<Boolean> checked = new AtomicReference<>(true);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle("Export the database")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String filename;
                    try {
                        filename = _db.export(checked.get());
                    } catch (DatabaseManagerException e) {
                        Toast.makeText(getActivity(), getString(R.string.exporting_database_error), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // make sure the new file is visible
                    MediaScannerConnection.scanFile(getActivity(), new String[]{filename}, null, null);

                    Toast.makeText(getActivity(), getString(R.string.export_database_location) + filename, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null);
        if (_db.isEncryptionEnabled()) {
            final String[] items = {"Keep the database encrypted"};
            final boolean[] checkedItems = {true};
            builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int index, boolean isChecked) {
                    checked.set(isChecked);
                }
            });
        } else {
            builder.setMessage(getString(R.string.export_warning));
        }
        Dialogs.showSecureDialog(builder.create());
    }

    private void onSlotManagerResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        DatabaseFileCredentials creds = (DatabaseFileCredentials) data.getSerializableExtra("creds");
        _db.setCredentials(creds);
        saveDatabase();
        updateEncryptionPreferences();
    }

    private void onGroupManagerResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        HashSet<String> groups = new HashSet<>(data.getStringArrayListExtra("groups"));

        for (DatabaseEntry entry : _db.getEntries()) {
            if (!groups.contains(entry.getGroup())) {
                entry.setGroup(null);
            }
        }
    }

    private boolean saveDatabase() {
        try {
            _db.save();
        } catch (DatabaseManagerException e) {
            Toast.makeText(getActivity(), getString(R.string.saving_error), Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private void updateEncryptionPreferences() {
        boolean encrypted = _db.isEncryptionEnabled();
        _encryptionPreference.setChecked(encrypted, true);
        _setPasswordPreference.setVisible(encrypted);
        _fingerprintPreference.setVisible(encrypted);
        _slotsPreference.setEnabled(encrypted);

        if (encrypted) {
            SlotList slots = _db.getCredentials().getSlots();
            boolean multiPassword = slots.findAll(PasswordSlot.class).size() > 1;
            boolean multiFinger = slots.findAll(FingerprintSlot.class).size() > 1;
            boolean showSlots = BuildConfig.DEBUG || multiPassword || multiFinger;
            _setPasswordPreference.setEnabled(!multiPassword);
            _fingerprintPreference.setEnabled(FingerprintHelper.getManager(getContext()) != null && !multiFinger);
            _fingerprintPreference.setChecked(slots.has(FingerprintSlot.class), true);
            _slotsPreference.setVisible(showSlots);
        } else {
            _setPasswordPreference.setEnabled(false);
            _fingerprintPreference.setEnabled(false);
            _fingerprintPreference.setChecked(false, true);
            _slotsPreference.setVisible(false);
        }
    }

    private class SetPasswordListener implements Dialogs.SlotListener {
        @Override
        public void onSlotResult(Slot slot, Cipher cipher) {
            DatabaseFileCredentials creds = _db.getCredentials();
            SlotList slots = creds.getSlots();

            try {
                // encrypt the master key for this slot
                slot.setKey(creds.getKey(), cipher);

                // remove the old master password slot
                PasswordSlot oldSlot = creds.getSlots().find(PasswordSlot.class);
                slots.remove(oldSlot);

                // add the new master password slot
                slots.add(slot);
            } catch (SlotException e) {
                onException(e);
                return;
            }

            _db.setCredentials(creds);
            saveDatabase();
        }

        @Override
        public void onException(Exception e) {
            updateEncryptionPreferences();
            Toast.makeText(getActivity(), getString(R.string.encryption_set_password_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private class RegisterFingerprintListener implements Dialogs.SlotListener {
        @Override
        public void onSlotResult(Slot slot, Cipher cipher) {
            DatabaseFileCredentials creds = _db.getCredentials();
            SlotList slots = creds.getSlots();

            try {
                slot.setKey(creds.getKey(), cipher);
            } catch (SlotException e) {
                onException(e);
                return;
            }

            slots.add(slot);
            _db.setCredentials(creds);

            saveDatabase();
            updateEncryptionPreferences();
        }

        @Override
        public void onException(Exception e) {
            Toast.makeText(getActivity(), getString(R.string.encryption_enable_fingerprint_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private class EnableEncryptionListener implements Dialogs.SlotListener {
        @Override
        public void onSlotResult(Slot slot, Cipher cipher) {
            DatabaseFileCredentials creds = new DatabaseFileCredentials();

            try {
                slot.setKey(creds.getKey(), cipher);
                creds.getSlots().add(slot);
                _db.enableEncryption(creds);
            } catch (DatabaseManagerException | SlotException e) {
                onException(e);
                return;
            }

            updateEncryptionPreferences();
        }

        @Override
        public void onException(Exception e) {
            Toast.makeText(getActivity(), getString(R.string.encryption_set_password_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
