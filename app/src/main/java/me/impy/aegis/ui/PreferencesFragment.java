package me.impy.aegis.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import me.impy.aegis.AegisApplication;
import me.impy.aegis.R;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.db.DatabaseManager;
import me.impy.aegis.db.DatabaseManagerException;
import me.impy.aegis.helpers.PermissionHelper;
import me.impy.aegis.importers.AegisImporter;
import me.impy.aegis.importers.DatabaseImporter;
import me.impy.aegis.importers.DatabaseImporterException;
import me.impy.aegis.util.ByteInputStream;

public class PreferencesFragment extends PreferenceFragment {
    // activity request codes
    private static final int CODE_IMPORT = 0;
    private static final int CODE_IMPORT_DECRYPT = 1;

    // permission request codes
    private static final int CODE_PERM_IMPORT = 0;

    // action codes
    public static final int ACTION_EXPORT = 0;
    public static final int ACTION_SLOTS = 1;

    private Intent _result = new Intent();
    private DatabaseManager _db;

    // this is used to keep a reference to a database converter
    // while the user provides credentials to decrypt it
    private DatabaseImporter _converter;

    private void setResult() {
        getActivity().setResult(Activity.RESULT_OK, _result);
    }

    private void finish() {
        setResult();
        getActivity().finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        AegisApplication app = (AegisApplication) getActivity().getApplication();
        _db = app.getDatabaseManager();

        // set the result intent in advance
        setResult();

        Preference nightModePreference = findPreference("pref_dark_mode");
        nightModePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Toast.makeText(getActivity(), "Night mode will be enabled after closing this screen", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        Preference exportPreference = findPreference("pref_import");
        exportPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (PermissionHelper.request(getActivity(), CODE_PERM_IMPORT, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    onImport();
                }
                return true;
            }
        });

        Preference importPreference = findPreference("pref_export");
        importPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                _result.putExtra("action", ACTION_EXPORT);
                finish();
                return true;
            }
        });

        Preference slotsPreference = findPreference("pref_slots");
        slotsPreference.setEnabled(getArguments().getBoolean("encrypted"));
        slotsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                _result.putExtra("action", ACTION_SLOTS);
                finish();
                return true;
            }
        });

        EditTextPreference timeoutPreference = (EditTextPreference) findPreference("pref_timeout");
        timeoutPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(String.format(getString(R.string.pref_timeout_summary), (String) newValue));
                return true;
            }
        });
        timeoutPreference.getOnPreferenceChangeListener().onPreferenceChange(timeoutPreference, timeoutPreference.getText());

        Preference issuerPreference = findPreference("pref_issuer");
        issuerPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                _result.putExtra("needsRefresh", true);
                return true;
            }
        });
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
        }
    }

    private void onImport() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, CODE_IMPORT);
    }

    private void onImportDecryptResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            _converter = null;
            return;
        }

        MasterKey key = (MasterKey) data.getSerializableExtra("key");
        ((AegisImporter)_converter).setKey(key);

        try {
            importDatabase(_converter);
        } catch (DatabaseImporterException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "An error occurred while trying to parse the file", Toast.LENGTH_SHORT).show();
        }
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

        boolean imported = false;
        for (DatabaseImporter converter : DatabaseImporter.create(stream)) {
            try {
                converter.parse();

                // special case to decrypt encrypted aegis databases
                if (converter.isEncrypted() && converter instanceof AegisImporter) {
                    _converter = converter;

                    Intent intent = new Intent(getActivity(), AuthActivity.class);
                    intent.putExtra("slots", ((AegisImporter)_converter).getFile().getSlots());
                    startActivityForResult(intent, CODE_IMPORT_DECRYPT);
                    return;
                }

                importDatabase(converter);
                imported = true;
                break;
            } catch (DatabaseImporterException e) {
                e.printStackTrace();
                stream.reset();
            }
        }

        if (!imported) {
            Toast.makeText(getActivity(), "An error occurred while trying to parse the file", Toast.LENGTH_SHORT).show();
        }
    }

    private void importDatabase(DatabaseImporter converter) throws DatabaseImporterException {
        List<DatabaseEntry> entries = converter.convert();
        for (DatabaseEntry entry : entries) {
            _db.addKey(entry);
        }

        try {
            _db.save();
        } catch (DatabaseManagerException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "An error occurred while trying to save the database", Toast.LENGTH_LONG).show();
            return;
        }

        _result.putExtra("needsReload", true);
        Toast.makeText(getActivity(), String.format(Locale.getDefault(), "Imported %d entries", entries.size()), Toast.LENGTH_LONG).show();
    }
}
