package me.impy.aegis.ui;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.takisoft.preferencex.PreferenceFragmentCompat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Cipher;

import me.impy.aegis.AegisApplication;
import me.impy.aegis.R;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.db.DatabaseFileCredentials;
import me.impy.aegis.db.DatabaseFileException;
import me.impy.aegis.db.DatabaseManager;
import me.impy.aegis.db.DatabaseManagerException;
import me.impy.aegis.db.slots.Slot;
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
                if (!_db.isEncryptionEnabled()) {
                    PasswordDialogFragment dialog = new PasswordDialogFragment();
                    // TODO: find a less ugly way to obtain the fragment manager
                    dialog.show(getActivity().getSupportFragmentManager(), null);
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.disable_encryption))
                            .setMessage(getString(R.string.disable_encryption_description))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        _db.disableEncryption();
                                    } catch (DatabaseManagerException e) {
                                        Toast.makeText(getActivity(), getString(R.string.encrypting_error), Toast.LENGTH_SHORT).show();
                                    }
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
                Intent intent = new Intent(getActivity(), SlotManagerActivity.class);
                intent.putExtra("creds", _db.getCredentials());
                startActivityForResult(intent, CODE_SLOTS);
                return true;
            }
        });
        updateEncryptionPreference();
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
                .show();
    }

    private void onImportDecryptResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            _importer = null;
            return;
        }

        DatabaseFileCredentials creds = (DatabaseFileCredentials) data.getSerializableExtra("creds");
        ((AegisImporter)_importer).setCredentials(creds);

        try {
            importDatabase(_importer);
        } catch (DatabaseImporterException e) {
            Toast.makeText(getActivity(), getString(R.string.parsing_file_error), Toast.LENGTH_SHORT).show();
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
            DatabaseImporter importer = DatabaseImporter.create(stream, _importerType);
            importer.parse();

            // special case to decrypt encrypted aegis databases
            if (importer.isEncrypted() && importer instanceof AegisImporter) {
                _importer = importer;

                Intent intent = new Intent(getActivity(), AuthActivity.class);
                intent.putExtra("slots", ((AegisImporter)_importer).getFile().getHeader().getSlots());
                startActivityForResult(intent, CODE_IMPORT_DECRYPT);
                return;
            }

            importDatabase(importer);
        } catch (DatabaseImporterException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), getString(R.string.parsing_file_error), Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getActivity(), String.format(Locale.getDefault(), getString(R.string.imported_entries_count), entries.size()), Toast.LENGTH_LONG).show();
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
                    checked[0] = isChecked;
                }
            });
        } else {
            builder.setMessage(getString(R.string.export_warning));
        }
        builder.show();
    }

    private void onSlotManagerResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        DatabaseFileCredentials creds = (DatabaseFileCredentials) data.getSerializableExtra("creds");
        _db.setCredentials(creds);
        saveDatabase();
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

        updateEncryptionPreference();
    }

    @Override
    public void onException(Exception e) {
        updateEncryptionPreference();
        Toast.makeText(getActivity(), getString(R.string.encryption_set_password_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void updateEncryptionPreference() {
        boolean encrypted = _db.isEncryptionEnabled();
        _encryptionPreference.setChecked(encrypted, true);
        _slotsPreference.setEnabled(encrypted);
    }
}
