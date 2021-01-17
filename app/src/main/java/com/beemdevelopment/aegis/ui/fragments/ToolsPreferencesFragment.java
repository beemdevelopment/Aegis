package com.beemdevelopment.aegis.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ArrayRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;

import com.beemdevelopment.aegis.BuildConfig;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.DropdownHelper;
import com.beemdevelopment.aegis.importers.AegisImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporterEntryException;
import com.beemdevelopment.aegis.importers.DatabaseImporterException;
import com.beemdevelopment.aegis.ui.AuthActivity;
import com.beemdevelopment.aegis.ui.Dialogs;
import com.beemdevelopment.aegis.ui.SelectEntriesActivity;
import com.beemdevelopment.aegis.ui.models.ImportEntry;
import com.beemdevelopment.aegis.ui.tasks.ExportTask;
import com.beemdevelopment.aegis.util.UUIDMap;
import com.beemdevelopment.aegis.vault.VaultBackupManager;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultManagerException;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;

public class ToolsPreferencesFragment extends PreferencesFragment {
    // keep a reference to the type of database converter the user selected
    private Class<? extends DatabaseImporter> _importerType;
    private AegisImporter.State _importerState;
    private UUIDMap<VaultEntry> _importerEntries;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_tools);

        Preference importPreference = findPreference("pref_import");
        importPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showImportersDialog(getContext(), false, definition -> {
                _importerType = definition.getType();

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, CODE_IMPORT);
            });
            return true;
        });

        Preference importAppPreference = findPreference("pref_import_app");
        importAppPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showImportersDialog(getContext(), true, definition -> {
                DatabaseImporter importer = DatabaseImporter.create(getContext(), definition.getType());
                importApp(importer);
            });
            return true;
        });

        Preference exportPreference = findPreference("pref_export");
        exportPreference.setOnPreferenceClickListener(preference -> {
            startExport();
            return true;
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            switch (requestCode) {
                case CODE_IMPORT:
                    onImportResult(resultCode, data);
                    break;
                case CODE_IMPORT_DECRYPT:
                    onImportDecryptResult(resultCode, data);
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
            }
        }
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
        } catch (DatabaseImporterException e) {
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
        intent.putExtra("vaultContainsEntries", getVault().getEntries().size() > 0);
        startActivityForResult(intent, CODE_SELECT_ENTRIES);
    }

    private void startExport() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_export, null);
        TextView warningText = view.findViewById(R.id.text_export_warning);
        CheckBox checkBoxEncrypt = view.findViewById(R.id.checkbox_export_encrypt);
        CheckBox checkBoxAccept = view.findViewById(R.id.checkbox_accept);
        AutoCompleteTextView dropdown = view.findViewById(R.id.dropdown_export_format);
        DropdownHelper.fillDropdown(getContext(), dropdown, R.array.export_formats);
        dropdown.setText(getString(R.string.export_format_aegis), false);
        dropdown.setOnItemClickListener((parent, view1, position, id) -> {
            checkBoxEncrypt.setChecked(position == 0);
            checkBoxEncrypt.setEnabled(position == 0);
            warningText.setVisibility(checkBoxEncrypt.isChecked() ? View.GONE : View.VISIBLE);
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

                int pos = getStringResourceIndex(R.array.export_formats, dropdown.getText().toString());
                int requestCode = getExportRequestCode(pos, checkBoxEncrypt.isChecked());
                VaultBackupManager.FileInfo fileInfo = getExportFileInfo(pos, checkBoxEncrypt.isChecked());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(getExportMimeType(requestCode))
                        .putExtra(Intent.EXTRA_TITLE, fileInfo.toString());
                startActivityForResult(intent, requestCode);
            });

            btnNeutral.setOnClickListener(v -> {
                dialog.dismiss();

                int pos = getStringResourceIndex(R.array.export_formats, dropdown.getText().toString());
                if (!checkBoxEncrypt.isChecked() && !checkBoxAccept.isChecked()) {
                    return;
                }

                File file;
                try {
                    VaultBackupManager.FileInfo fileInfo = getExportFileInfo(pos, checkBoxEncrypt.isChecked());
                    file = File.createTempFile(fileInfo.getFilename() + "-", "." + fileInfo.getExtension(), getExportCacheDir());
                } catch (IOException e) {
                    e.printStackTrace();
                    Dialogs.showErrorDialog(getContext(), R.string.exporting_vault_error, e);
                    return;
                }

                int requestCode = getExportRequestCode(pos, checkBoxEncrypt.isChecked());
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

    private void onSelectEntriesResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        boolean wipeEntries = data.getBooleanExtra("wipeEntries", false);
        if (wipeEntries) {
            getVault().wipeEntries();
        }

        List<ImportEntry> selectedEntries = (ArrayList<ImportEntry>) data.getSerializableExtra("entries");
        for (ImportEntry selectedEntry : selectedEntries) {
            VaultEntry savedEntry = _importerEntries.getByUUID(selectedEntry.getUUID());

            // temporary: randomize the UUID of duplicate entries and add them anyway
            if (getVault().isEntryDuplicate(savedEntry)) {
                savedEntry.resetUUID();
            }

            getVault().addEntry(savedEntry);
        }

        _importerEntries = null;
        if (!saveVault()) {
            return;
        }

        String toastMessage = getResources().getQuantityString(R.plurals.imported_entries_count, selectedEntries.size(), selectedEntries.size());
        Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();

        getResult().putExtra("needsRecreate", true);
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
                if (getVault().isEncryptionEnabled()) {
                    cb.exportVault(stream -> getVault().export(stream));
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

                            cb.exportVault(stream -> getVault().export(stream, creds));
                        }

                        @Override
                        public void onException(Exception e) {

                        }
                    });
                }
                break;
            case CODE_EXPORT_PLAIN:
                cb.exportVault((stream) -> getVault().export(stream, null));
                break;
            case CODE_EXPORT_GOOGLE_URI:
                cb.exportVault((stream) -> getVault().exportGoogleUris(stream));
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

    private int getStringResourceIndex(@ArrayRes int id, String string) {
        String[] res = getResources().getStringArray(id);
        for (int i = 0; i < res.length; i++) {
            if (res[i].equalsIgnoreCase(string)) {
                return i;
            }
        }
        return -1;
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
