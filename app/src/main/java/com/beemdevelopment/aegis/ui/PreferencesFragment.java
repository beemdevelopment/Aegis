package com.beemdevelopment.aegis.ui;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.BuildConfig;
import com.beemdevelopment.aegis.CancelAction;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.crypto.KeyStoreHandle;
import com.beemdevelopment.aegis.crypto.KeyStoreHandleException;
import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.db.DatabaseFileCredentials;
import com.beemdevelopment.aegis.db.DatabaseFileException;
import com.beemdevelopment.aegis.db.DatabaseManager;
import com.beemdevelopment.aegis.db.DatabaseManagerException;
import com.beemdevelopment.aegis.db.slots.FingerprintSlot;
import com.beemdevelopment.aegis.db.slots.PasswordSlot;
import com.beemdevelopment.aegis.db.slots.Slot;
import com.beemdevelopment.aegis.db.slots.SlotException;
import com.beemdevelopment.aegis.db.slots.SlotList;
import com.beemdevelopment.aegis.helpers.FingerprintHelper;
import com.beemdevelopment.aegis.helpers.PermissionHelper;
import com.beemdevelopment.aegis.importers.AegisImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporterEntryException;
import com.beemdevelopment.aegis.importers.DatabaseImporterException;
import com.beemdevelopment.aegis.services.NotificationService;
import com.beemdevelopment.aegis.ui.models.ImportEntry;
import com.beemdevelopment.aegis.ui.preferences.SwitchPreference;
import com.beemdevelopment.aegis.util.UUIDMap;
import com.takisoft.preferencex.PreferenceFragmentCompat;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.io.SuFile;
import com.topjohnwu.superuser.io.SuFileInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

public class PreferencesFragment extends PreferenceFragmentCompat {
    // activity request codes
    private static final int CODE_IMPORT = 0;
    private static final int CODE_IMPORT_DECRYPT = 1;
    private static final int CODE_SLOTS = 2;
    private static final int CODE_GROUPS = 3;
    private static final int CODE_SELECT_ENTRIES = 4;

    // permission request codes
    private static final int CODE_PERM_IMPORT = 0;
    private static final int CODE_PERM_EXPORT = 1;

    private Intent _result;
    private DatabaseManager _db;

    // keep a reference to the type of database converter the user selected
    private Class<? extends DatabaseImporter> _importerType;
    private AegisImporter.State _importerState;
    private UUIDMap<DatabaseEntry> _importerEntries;

    private SwitchPreference _encryptionPreference;
    private SwitchPreference _fingerprintPreference;
    private Preference _autoLockPreference;
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

        int currentTheme = app.getPreferences().getCurrentTheme().ordinal();
        Preference darkModePreference = findPreference("pref_dark_mode");
        darkModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.theme_titles)[currentTheme]));
        darkModePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                int currentTheme = app.getPreferences().getCurrentTheme().ordinal();

                Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.choose_theme)
                        .setSingleChoiceItems(R.array.theme_titles, currentTheme, (dialog, which) -> {
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

        Preference langPreference = findPreference("pref_lang");
        langPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            _result.putExtra("needsRecreate", true);
            getActivity().recreate();
            return true;
        });

        int currentViewMode = app.getPreferences().getCurrentViewMode().ordinal();
        Preference viewModePreference = findPreference("pref_view_mode");
        viewModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.view_mode_titles)[currentViewMode]));
        viewModePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                int currentViewMode = app.getPreferences().getCurrentViewMode().ordinal();

                Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.choose_view_mode)
                        .setSingleChoiceItems(R.array.view_mode_titles, currentViewMode, (dialog, which) -> {
                            int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            app.getPreferences().setCurrentViewMode(ViewMode.fromInteger(i));
                            viewModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.view_mode_titles)[i]));
                            _result.putExtra("needsRefresh", true);
                            dialog.dismiss();
                        })
                        .setPositiveButton(android.R.string.ok, null)
                        .create());

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
                            .setTitle(R.string.disable_encryption)
                            .setMessage(getString(R.string.disable_encryption_description))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        _db.disableEncryption();
                                    } catch (DatabaseManagerException e) {
                                        Toast.makeText(getActivity(), R.string.disable_encryption_error, Toast.LENGTH_SHORT).show();
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

        _fingerprintPreference = (SwitchPreference) findPreference("pref_fingerprint");
        _fingerprintPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                DatabaseFileCredentials creds = _db.getCredentials();
                SlotList slots = creds.getSlots();

                if (!slots.has(FingerprintSlot.class)) {
                    if (FingerprintHelper.isSupported() && FingerprintHelper.isAvailable(getContext())) {
                        Dialogs.showFingerprintDialog(getActivity(), new RegisterFingerprintListener());
                    }
                } else {
                    // remove the fingerprint slot
                    FingerprintSlot slot = slots.find(FingerprintSlot.class);
                    slots.remove(slot);
                    _db.setCredentials(creds);

                    // remove the KeyStore key
                    try {
                        KeyStoreHandle handle = new KeyStoreHandle();
                        handle.deleteKey(slot.getUUID().toString());
                    } catch (KeyStoreHandleException e) {
                        e.printStackTrace();
                    }

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

        _autoLockPreference = findPreference("pref_auto_lock");
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updateEncryptionPreferences();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (!PermissionHelper.checkResults(grantResults)) {
            Toast.makeText(getActivity(), R.string.permission_denied, Toast.LENGTH_SHORT).show();
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
            case CODE_SELECT_ENTRIES:
                onSelectEntriesResult(resultCode, data);
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

        Map<String, Class<? extends DatabaseImporter>> importers = DatabaseImporter.getImporters();
        String[] names = importers.keySet().toArray(new String[0]);

        Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                .setTitle(R.string.choose_application)
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
        Map<String, Class<? extends DatabaseImporter>> importers = DatabaseImporter.getAppImporters();
        String[] names = importers.keySet().toArray(new String[0]);

        Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                .setTitle(R.string.choose_application)
                .setSingleChoiceItems(names, 0, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    Class<? extends DatabaseImporter> importerType = Objects.requireNonNull(importers.get(names[i]));
                    DatabaseImporter importer = DatabaseImporter.create(getContext(), importerType);
                    importApp(importer);
                })
                .create());
    }

    private void importApp(DatabaseImporter importer) {
        // obtain the global root shell and close it immediately after we're done
        // TODO: find a way to use SuFileInputStream with Shell.newInstance()
        try (Shell shell = Shell.getShell()) {
            if (!shell.isRoot()) {
                Toast.makeText(getActivity(), R.string.root_error, Toast.LENGTH_SHORT).show();
                return;
            }

            SuFile file = importer.getAppPath();
            try (SuFileInputStream stream = new SuFileInputStream(file)) {
                DatabaseImporter.FileReader reader = new DatabaseImporter.FileReader(stream, true);
                importDatabase(importer, reader);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.app_lookup_error, Toast.LENGTH_SHORT).show();
        } catch (IOException | DatabaseImporterException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.reading_file_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void importDatabase(DatabaseImporter importer, DatabaseImporter.FileReader reader) {
        try {
            DatabaseImporter.State state = importer.read(reader);
            if (state.isEncrypted()) {
                // temporary special case for encrypted Aegis databases
                if (state instanceof AegisImporter.EncryptedState) {
                    _importerState = state;

                    Intent intent = new Intent(getActivity(), AuthActivity.class);
                    intent.putExtra("slots", ((AegisImporter.EncryptedState) state).getSlots());
                    intent.putExtra("cancelAction", CancelAction.CLOSE);
                    startActivityForResult(intent, CODE_IMPORT_DECRYPT);
                } else {
                    state.decrypt(getActivity(), new DatabaseImporter.DecryptListener() {
                        @Override
                        public void onStateDecrypted(DatabaseImporter.State state) {
                            importDatabase(state);
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(getActivity(), R.string.decryption_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                importDatabase(state);
            }
        } catch (DatabaseImporterException e) {
            e.printStackTrace();
            String msg = String.format("%s: %s", getString(R.string.parsing_file_error), e.getMessage());
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void onImportDecryptResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            _importerState = null;
            return;
        }

        DatabaseFileCredentials creds = (DatabaseFileCredentials) data.getSerializableExtra("creds");
        DatabaseImporter.State state;
        try {
            state = ((AegisImporter.EncryptedState) _importerState).decrypt(creds);
        } catch (DatabaseFileException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.decryption_error, Toast.LENGTH_SHORT).show();
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
            DatabaseImporter.FileReader reader = new DatabaseImporter.FileReader(stream);
            importDatabase(importer, reader);
        } catch (FileNotFoundException e) {
            Toast.makeText(getActivity(), R.string.file_not_found, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.reading_file_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void importDatabase(DatabaseImporter.State state) {
        DatabaseImporter.Result result;
        try {
            result = state.convert();
        } catch (DatabaseImporterException e) {
            e.printStackTrace();
            String msg = String.format("%s: %s", getString(R.string.parsing_file_error), e.getMessage());
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            return;
        }

        _importerEntries = result.getEntries();
        List<ImportEntry> entries = new ArrayList<>();
        for (DatabaseEntry entry : _importerEntries) {
            entries.add(new ImportEntry(entry));
        }

        Intent intent = new Intent(getActivity(), SelectEntriesActivity.class);
        intent.putExtra("entries", (ArrayList<ImportEntry>) entries);
        intent.putExtra("errors", (ArrayList<DatabaseImporterEntryException>) result.getErrors());
        startActivityForResult(intent, CODE_SELECT_ENTRIES);
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
                        Toast.makeText(getActivity(), R.string.exporting_database_error, Toast.LENGTH_SHORT).show();
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
            builder.setMessage(R.string.export_warning);
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

    private void onSelectEntriesResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        List<ImportEntry> selectedEntries = (ArrayList<ImportEntry>) data.getSerializableExtra("entries");
        for (ImportEntry selectedEntry : selectedEntries) {
            DatabaseEntry savedEntry = _importerEntries.getByUUID(selectedEntry.getUUID());

            // temporary: randomize the UUID of duplicate entries and add them anyway
            if (_db.isEntryDuplicate(savedEntry)) {
                savedEntry.resetUUID();
            }

            _db.addEntry(savedEntry);
        }

        _importerEntries = null;
        if (!saveDatabase()) {
            return;
        }

        String toastMessage = getResources().getString(R.string.imported_entries_count, selectedEntries.size());
        Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();

        _result.putExtra("needsRecreate", true);
    }

    private boolean saveDatabase() {
        try {
            _db.save();
        } catch (DatabaseManagerException e) {
            Toast.makeText(getActivity(), R.string.saving_error, Toast.LENGTH_LONG).show();
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
        _autoLockPreference.setVisible(encrypted);

        if (encrypted) {
            SlotList slots = _db.getCredentials().getSlots();
            boolean multiPassword = slots.findAll(PasswordSlot.class).size() > 1;
            boolean multiFinger = slots.findAll(FingerprintSlot.class).size() > 1;
            boolean showSlots = BuildConfig.DEBUG || multiPassword || multiFinger;
            boolean canUseFinger = FingerprintHelper.isSupported() && FingerprintHelper.isAvailable(getContext());
            _setPasswordPreference.setEnabled(!multiPassword);
            _fingerprintPreference.setEnabled(canUseFinger && !multiFinger);
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

            getActivity().startService(new Intent(getActivity(), NotificationService.class));
            updateEncryptionPreferences();
        }

        @Override
        public void onException(Exception e) {
            Toast.makeText(getActivity(), getString(R.string.encryption_set_password_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
