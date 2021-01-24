package com.beemdevelopment.aegis.ui.fragments;

import android.app.Activity;
import android.content.Intent;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;

import com.beemdevelopment.aegis.BuildConfig;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.DropdownHelper;
import com.beemdevelopment.aegis.importers.DatabaseImporter;
import com.beemdevelopment.aegis.ui.Dialogs;
import com.beemdevelopment.aegis.ui.ImportEntriesActivity;
import com.beemdevelopment.aegis.ui.tasks.ExportTask;
import com.beemdevelopment.aegis.vault.VaultBackupManager;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultManagerException;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.crypto.Cipher;

public class ImportExportPreferencesFragment extends PreferencesFragment {
    // keep a reference to the type of database converter that was selected
    private Class<? extends DatabaseImporter> _importerType;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_import_export);

        if (savedInstanceState != null) {
            _importerType = (Class<? extends DatabaseImporter>) savedInstanceState.getSerializable("importerType");
        }

        Preference importPreference = findPreference("pref_import");
        importPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showImportersDialog(getContext(), false, definition -> {
                _importerType = definition.getType();

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, CODE_IMPORT_SELECT);
            });
            return true;
        });

        Preference importAppPreference = findPreference("pref_import_app");
        importAppPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showImportersDialog(getContext(), true, definition -> {
                startImportEntriesActivity(definition.getType(), null);
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("importerType", _importerType);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_IMPORT) {
            getResult().putExtra("needsRecreate", true);
        } else if (data != null) {
            switch (requestCode) {
                case CODE_IMPORT_SELECT:
                    onImportSelectResult(resultCode, data);
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

    private void onImportSelectResult(int resultCode, Intent data) {
        Uri uri = data.getData();
        if (resultCode != Activity.RESULT_OK || uri == null) {
            return;
        }

        startImportEntriesActivity(_importerType, uri);
    }

    private void startImportEntriesActivity(Class<? extends DatabaseImporter> importerType, Uri fileUri) {
        Intent intent = new Intent(getActivity(), ImportEntriesActivity.class);
        intent.putExtra("importerType", importerType);
        intent.putExtra("fileUri", fileUri == null ? null : fileUri.toString());
        startActivityForResult(intent, CODE_IMPORT);
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
