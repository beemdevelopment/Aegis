package me.impy.aegis.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import me.impy.aegis.AegisApplication;
import me.impy.aegis.R;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.db.DatabaseManager;
import me.impy.aegis.db.DatabaseManagerException;
import me.impy.aegis.helpers.PermissionHelper;
import me.impy.aegis.importers.DatabaseImporter;
import me.impy.aegis.importers.DatabaseImporterException;
import me.impy.aegis.util.ByteInputStream;

public class PreferencesFragment extends PreferenceFragment {
    // activity request codes
    private static final int CODE_IMPORT = 0;

    // permission request codes
    private static final int CODE_PERM_IMPORT = 0;

    // action codes
    public static final int ACTION_EXPORT = 0;
    public static final int ACTION_SLOTS = 1;

    private Intent _result = new Intent();
    private AegisApplication _app;
    private DatabaseManager _db;

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
        _app = (AegisApplication) getActivity().getApplication();
        _db = _app.getDatabaseManager();

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
        }
    }

    private void onImport() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, CODE_IMPORT);
    }

    private void onImportResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        InputStream fileStream = null;
        List<DatabaseEntry> entries = null;

        try {
            try {
                fileStream = getActivity().getContentResolver().openInputStream(data.getData());
            } catch (FileNotFoundException e) {
                Toast.makeText(getActivity(), "Error: File not found", Toast.LENGTH_SHORT).show();
                return;
            }

            ByteInputStream stream;
            try {
                int read;
                byte[] buf = new byte[4096];
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                while ((read = fileStream.read(buf, 0, buf.length)) != -1) {
                    outStream.write(buf, 0, read);
                }
                stream = new ByteInputStream(outStream.toByteArray());
            } catch (IOException e) {
                Toast.makeText(getActivity(), "An error occurred while trying to read the file", Toast.LENGTH_SHORT).show();
                return;
            }

            for (DatabaseImporter converter : DatabaseImporter.create(stream)) {
                try {
                    entries = converter.convert();
                    break;
                } catch (DatabaseImporterException e) {
                    stream.reset();
                }
            }
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (entries == null) {
            Toast.makeText(getActivity(), "An error occurred while trying to parse the file", Toast.LENGTH_SHORT).show();
            return;
        }

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
        Toast.makeText(getActivity(), String.format(Locale.getDefault(), "Imported %d entries", entries.size()), Toast.LENGTH_SHORT).show();
    }
}