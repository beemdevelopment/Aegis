package me.impy.aegis.ui;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Cipher;

import me.impy.aegis.AegisApplication;
import me.impy.aegis.R;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.db.DatabaseManager;
import me.impy.aegis.db.DatabaseManagerException;
import me.impy.aegis.db.slots.Slot;
import me.impy.aegis.db.slots.SlotList;
import me.impy.aegis.db.slots.SlotException;
import me.impy.aegis.helpers.PermissionHelper;
import me.impy.aegis.importers.AegisImporter;
import me.impy.aegis.importers.DatabaseImporter;
import me.impy.aegis.importers.DatabaseImporterException;
import me.impy.aegis.ui.dialogs.PasswordDialogFragment;
import me.impy.aegis.ui.preferences.SwitchPreference;
import me.impy.aegis.util.ByteInputStream;

public class PreferencesFragment extends PreferenceFragmentCompat implements PasswordDialogFragment.Listener {
    // activity request codes
    private static final int CODE_IMPORT = 0;
    private static final int CODE_IMPORT_DECRYPT = 1;
    private static final int CODE_SLOTS = 2;

    // permission request codes
    private static final int CODE_PERM_IMPORT = 0;
    private static final int CODE_PERM_EXPORT = 1;

    private Intent _result;
    private DatabaseManager _db;

    // this is used to keep a reference to a database converter
    // while the user provides credentials to decrypt it
    private DatabaseImporter _importer;
    private Class<? extends DatabaseImporter> _importerType;

    private SwitchPreference _encryptionPreference;
    private Preference _slotsPreference;

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        AegisApplication app = (AegisApplication) getActivity().getApplication();
        _db = app.getDatabaseManager();

        // set the result intent in advance
        setResult(new Intent());

        Preference darkModePreference = findPreference("pref_dark_mode");
        darkModePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                _result.putExtra("needsRecreate", true);
                getActivity().recreate();
                return true;
            }
        });

        Preference exportPreference = findPreference("pref_import");
        exportPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onImport();
                return true;
            }
        });

        Preference importPreference = findPreference("pref_export");
        importPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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

        _encryptionPreference = (SwitchPreference) findPreference("pref_encryption");
        _encryptionPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!_db.getFile().isEncrypted()) {
                    PasswordDialogFragment dialog = new PasswordDialogFragment();
                    // TODO: find a less ugly way to obtain the fragment manager
                    dialog.show(getActivity().getSupportFragmentManager(), null);
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Disable encryption")
                            .setMessage("Are you sure you want to disable encryption? This will cause the database to be stored in plain text")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    _db.disableEncryption();
                                    saveDatabase();
                                    updateEncryptionPreference();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                }
                return false;
            }
        });
        _slotsPreference = findPreference("pref_slots");
        _slotsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MasterKey masterKey = _db.getMasterKey();
                Intent intent = new Intent(getActivity(), SlotManagerActivity.class);
                intent.putExtra("masterKey", masterKey);
                intent.putExtra("slots", _db.getFile().getSlots());
                startActivityForResult(intent, CODE_SLOTS);
                return true;
            }
        });
        updateEncryptionPreference();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (!PermissionHelper.checkResults(grantResults)) {
            Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_SHORT).show();
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
        String[] names = importers.keySet().toArray(new String[importers.size()]);

        new AlertDialog.Builder(getActivity())
                .setTitle("Select the application you'd like to import a database from")
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
                .show();
    }

    private void onImportDecryptResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            _importer = null;
            return;
        }

        MasterKey key = (MasterKey) data.getSerializableExtra("key");
        ((AegisImporter)_importer).setKey(key);

        try {
            importDatabase(_importer);
        } catch (DatabaseImporterException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "An error occurred while trying to parse the file", Toast.LENGTH_SHORT).show();
        }

        _importer = null;
    }

    private void onImportResult(int resultCode, Intent data) {
        Uri uri = data.getData();
        if (resultCode != Activity.RESULT_OK || uri == null) {
            return;
        }

        ByteInputStream stream;
        InputStream fileStream = null;

        try {
            fileStream = getActivity().getContentResolver().openInputStream(uri);
            stream = ByteInputStream.create(fileStream);
        } catch (FileNotFoundException e) {
            Toast.makeText(getActivity(), "Error: File not found", Toast.LENGTH_SHORT).show();
            return;
        } catch (IOException e) {
            Toast.makeText(getActivity(), "An error occurred while trying to read the file", Toast.LENGTH_SHORT).show();
            return;
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            DatabaseImporter importer = DatabaseImporter.create(stream, _importerType);
            importer.parse();

            // special case to decrypt encrypted aegis databases
            if (importer.isEncrypted() && importer instanceof AegisImporter) {
                _importer = importer;

                Intent intent = new Intent(getActivity(), AuthActivity.class);
                intent.putExtra("slots", ((AegisImporter)_importer).getFile().getSlots());
                startActivityForResult(intent, CODE_IMPORT_DECRYPT);
                return;
            }

            importDatabase(importer);
        } catch (DatabaseImporterException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "An error occurred while trying to parse the file", Toast.LENGTH_SHORT).show();
        }
    }

    private void importDatabase(DatabaseImporter importer) throws DatabaseImporterException {
        List<DatabaseEntry> entries = importer.convert();
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
        Toast.makeText(getActivity(), String.format(Locale.getDefault(), "Imported %d entries", entries.size()), Toast.LENGTH_LONG).show();
    }

    private void onExport() {
        if (!PermissionHelper.request(getActivity(), CODE_PERM_EXPORT, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return;
        }

        // TODO: create a custom layout to show a message AND a checkbox
        final boolean[] checked = {true};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle("Export the database")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String filename;
                    try {
                        filename = _db.export(checked[0]);
                    } catch (DatabaseManagerException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "An error occurred while trying to export the database", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // make sure the new file is visible
                    MediaScannerConnection.scanFile(getActivity(), new String[]{filename}, null, null);

                    Toast.makeText(getActivity(), "The database has been exported to: " + filename, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null);
        if (_db.getFile().isEncrypted()) {
            final String[] items = {"Keep the database encrypted"};
            final boolean[] checkedItems = {true};
            builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int index, boolean isChecked) {
                    checked[0] = isChecked;
                }
            });
        } else {
            builder.setMessage("This action will export the database out of Android's private storage.");
        }
        builder.show();
    }

    private void onSlotManagerResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        SlotList slots = (SlotList) data.getSerializableExtra("slots");
        _db.getFile().setSlots(slots);
        saveDatabase();
    }

    private boolean saveDatabase() {
        try {
            _db.save();
        } catch (DatabaseManagerException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "An error occurred while trying to save the database", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    @Override
    public void onSlotResult(Slot slot, Cipher cipher) {
        MasterKey masterKey = MasterKey.generate();

        SlotList slots = new SlotList();
        try {
            slot.setKey(masterKey, cipher);
        } catch (SlotException e) {
            onException(e);
            return;
        }
        slots.add(slot);
        _db.enableEncryption(masterKey, slots);

        saveDatabase();
        updateEncryptionPreference();
    }

    @Override
    public void onException(Exception e) {
        updateEncryptionPreference();
        Toast.makeText(getActivity(), "An error occurred while trying to set the password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void updateEncryptionPreference() {
        boolean encrypted = _db.getFile().isEncrypted();
        _encryptionPreference.setChecked(encrypted, true);
        _slotsPreference.setEnabled(encrypted);
    }
}
