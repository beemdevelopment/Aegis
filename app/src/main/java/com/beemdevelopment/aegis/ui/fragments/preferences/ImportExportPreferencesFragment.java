package com.beemdevelopment.aegis.ui.fragments.preferences;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;

import com.beemdevelopment.aegis.BuildConfig;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.DropdownHelper;
import com.beemdevelopment.aegis.importers.DatabaseImporter;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.ui.ImportEntriesActivity;
import com.beemdevelopment.aegis.ui.TransferEntriesActivity;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.tasks.ExportTask;
import com.beemdevelopment.aegis.ui.tasks.ImportFileTask;
import com.beemdevelopment.aegis.vault.Vault;
import com.beemdevelopment.aegis.vault.VaultBackupManager;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.SlotException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import javax.crypto.Cipher;

public class ImportExportPreferencesFragment extends PreferencesFragment {
    // keep a reference to the type of database converter that was selected
    private DatabaseImporter.Definition _importerDef;
    private Vault.EntryFilter _exportFilter;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_import_export);

        if (savedInstanceState != null) {
            _importerDef = (DatabaseImporter.Definition) savedInstanceState.getSerializable("importerDef");
        }

        Preference importPreference = requirePreference("pref_import");
        importPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showImportersDialog(requireContext(), false, definition -> {
                _importerDef = definition;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                _vaultManager.startActivityForResult(this, intent, CODE_IMPORT_SELECT);
            });
            return true;
        });

        Preference importAppPreference = requirePreference("pref_import_app");
        importAppPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showImportersDialog(requireContext(), true, definition -> {
                startImportEntriesActivity(definition, null);
            });
            return true;
        });

        Preference exportPreference = requirePreference("pref_export");
        exportPreference.setOnPreferenceClickListener(preference -> {
            startExport();
            return true;
        });

        Preference googleAuthStyleExportPreference = requirePreference("pref_google_auth_style_export");
        googleAuthStyleExportPreference.setOnPreferenceClickListener(preference -> {
            startGoogleAuthenticatorStyleExport();
            return true;
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("importerDef", _importerDef);
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

        ImportFileTask.Params params = new ImportFileTask.Params(uri, "import", null);
        ImportFileTask task = new ImportFileTask(requireContext(), result -> {
            if (result.getException() == null) {
                startImportEntriesActivity(_importerDef, result.getFile());
            } else {
                Dialogs.showErrorDialog(requireContext(), R.string.reading_file_error, result.getException());
            }
        });
        task.execute(getLifecycle(), params);
    }

    private void startImportEntriesActivity(DatabaseImporter.Definition importerDef, File file) {
        Intent intent = new Intent(requireActivity(), ImportEntriesActivity.class);
        intent.putExtra("importerDef", importerDef);
        intent.putExtra("file", file);
        startActivityForResult(intent, CODE_IMPORT);
    }

    private void startExport() {
        boolean isBackupPasswordSet = _vaultManager.getVault().isBackupPasswordSet();
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_export, null);
        TextView warningText = view.findViewById(R.id.text_export_warning);
        CheckBox checkBoxEncrypt = view.findViewById(R.id.checkbox_export_encrypt);
        CheckBox checkBoxAccept = view.findViewById(R.id.checkbox_accept);
        CheckBox checkBoxExportAllGroups = view.findViewById(R.id.export_selected_groups);
        LinearLayout groupsSelection = view.findViewById(R.id.select_groups);
        TextView groupsSelectionDescriptor = view.findViewById(R.id.select_groups_hint);
        TextView passwordInfoText = view.findViewById(R.id.text_separate_password);
        passwordInfoText.setVisibility(checkBoxEncrypt.isChecked() && isBackupPasswordSet ? View.VISIBLE : View.GONE);
        AutoCompleteTextView dropdown = view.findViewById(R.id.dropdown_export_format);
        DropdownHelper.fillDropdown(requireContext(), dropdown, R.array.export_formats);
        dropdown.setText(getString(R.string.export_format_aegis), false);
        dropdown.setOnItemClickListener((parent, view1, position, id) -> {
            checkBoxEncrypt.setChecked(position == 0);
            checkBoxEncrypt.setEnabled(position == 0);
            warningText.setVisibility(checkBoxEncrypt.isChecked() ? View.GONE : View.VISIBLE);
            passwordInfoText.setVisibility(checkBoxEncrypt.isChecked() && isBackupPasswordSet ? View.VISIBLE : View.GONE);
        });

        for (String group: _vaultManager.getVault().getGroups()) {
            CheckBox box = new CheckBox(requireContext());
            box.setText(group);
            box.setChecked(false);
            groupsSelection.addView(box);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
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
                passwordInfoText.setVisibility(isChecked && isBackupPasswordSet ? View.VISIBLE : View.GONE);
                checkBoxAccept.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                checkBoxAccept.setChecked(false);
                btnPos.setEnabled(isChecked);
                btnNeutral.setEnabled(isChecked);
            });

            checkBoxAccept.setOnCheckedChangeListener((buttonView, isChecked) -> {
                btnPos.setEnabled(isChecked);
                btnNeutral.setEnabled(isChecked);
            });

            checkBoxExportAllGroups.setOnCheckedChangeListener((button, isChecked) -> {
                int visibility = isChecked ? View.GONE : View.VISIBLE;
                groupsSelection.setVisibility(visibility);
                groupsSelectionDescriptor.setVisibility(visibility);
            });

            btnPos.setOnClickListener(v -> {
                dialog.dismiss();

                if (!checkBoxEncrypt.isChecked() && !checkBoxAccept.isChecked()) {
                    return;
                }

                if (!checkBoxExportAllGroups.isChecked()) {
                    _exportFilter = getVaultEntryFilter(groupsSelection);
                    if (_exportFilter == null) {
                        Toast noGroupsSelected = new Toast(requireContext());
                        noGroupsSelected.setText(R.string.export_no_groups_selected);
                        noGroupsSelected.show();
                        return;
                    }
                }

                int pos = getStringResourceIndex(R.array.export_formats, dropdown.getText().toString());
                int requestCode = getExportRequestCode(pos, checkBoxEncrypt.isChecked());
                VaultBackupManager.FileInfo fileInfo = getExportFileInfo(pos, checkBoxEncrypt.isChecked());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(getExportMimeType(requestCode))
                        .putExtra(Intent.EXTRA_TITLE, fileInfo.toString());

                _vaultManager.startActivityForResult(this, intent, requestCode);
            });

            btnNeutral.setOnClickListener(v -> {
                dialog.dismiss();

                int pos = getStringResourceIndex(R.array.export_formats, dropdown.getText().toString());
                if (!checkBoxEncrypt.isChecked() && !checkBoxAccept.isChecked()) {
                    return;
                }

                if (!checkBoxExportAllGroups.isChecked()) {
                    _exportFilter = getVaultEntryFilter(groupsSelection);
                    if (_exportFilter == null) {
                        Toast noGroupsSelected = new Toast(requireContext());
                        noGroupsSelected.setText(R.string.export_no_groups_selected);
                        noGroupsSelected.show();
                        return;
                    }
                }

                File file;
                try {
                    VaultBackupManager.FileInfo fileInfo = getExportFileInfo(pos, checkBoxEncrypt.isChecked());
                    file = File.createTempFile(fileInfo.getFilename() + "-", "." + fileInfo.getExtension(), getExportCacheDir());
                } catch (IOException e) {
                    e.printStackTrace();
                    Dialogs.showErrorDialog(requireContext(), R.string.exporting_vault_error, e);
                    return;
                }

                int requestCode = getExportRequestCode(pos, checkBoxEncrypt.isChecked());
                startExportVault(requestCode, cb -> {
                    try (OutputStream stream = new FileOutputStream(file)) {
                        cb.exportVault(stream);
                    } catch (IOException | VaultRepositoryException e) {
                        e.printStackTrace();
                        Dialogs.showErrorDialog(requireContext(), R.string.exporting_vault_error, e);
                        return;
                    }

                    // if the user creates an export, hide the backup reminder
                    _prefs.setIsBackupReminderNeeded(false);

                    Uri uri = FileProvider.getUriForFile(requireContext(), BuildConfig.FILE_PROVIDER_AUTHORITY, file);
                    Intent intent = new Intent(Intent.ACTION_SEND)
                            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            .setType(getExportMimeType(requestCode))
                            .putExtra(Intent.EXTRA_STREAM, uri);
                    Intent chooser = Intent.createChooser(intent, getString(R.string.pref_export_summary));
                    _vaultManager.startActivity(this, chooser);
                }, _exportFilter);
                _exportFilter = null;
            });
        });

        Dialogs.showSecureDialog(dialog);
    }

    private Vault.EntryFilter getVaultEntryFilter(LinearLayout view) {
        Set<String> groups = new HashSet<>();
        for (int i=0; i<view.getChildCount(); i++) {
            CheckBox group = (CheckBox) view.getChildAt(i);
            if (group.isChecked() && group.getText().toString().equals(getString(R.string.no_group))) {
                groups.add(null);
            } else if (group.isChecked()) {
                groups.add(group.getText().toString());
            }
        }

        return groups.isEmpty() ? null : entry -> groups.contains(entry.getGroup());
    }

    private void startGoogleAuthenticatorStyleExport() {
        ArrayList<GoogleAuthInfo> toExport = new ArrayList<>();
        for (VaultEntry entry : _vaultManager.getVault().getEntries()) {
            String type = entry.getInfo().getType().toLowerCase();
            String algo = entry.getInfo().getAlgorithm(false);
            int digits = entry.getInfo().getDigits();

            if ((Objects.equals(type, TotpInfo.ID) || Objects.equals(type, HotpInfo.ID))
                    && digits == OtpInfo.DEFAULT_DIGITS
                    && Objects.equals(algo, OtpInfo.DEFAULT_ALGORITHM)) {
                GoogleAuthInfo info = new GoogleAuthInfo(entry.getInfo(), entry.getName(), entry.getIssuer());
                toExport.add(info);
            }
        }

        int entriesSkipped = _vaultManager.getVault().getEntries().size() - toExport.size();
        if (entriesSkipped > 0) {
            Toast a = new Toast(requireContext());
            a.setText(requireContext().getResources().getQuantityString(R.plurals.pref_google_auth_export_incompatible_entries, entriesSkipped, entriesSkipped));
            a.show();
        }

        int qrSize = 10;
        int batchId = new Random().nextInt();
        int batchSize = toExport.size() / qrSize + (toExport.size() % qrSize == 0 ? 0 : 1);
        List<GoogleAuthInfo> infos = new ArrayList<>();
        ArrayList<GoogleAuthInfo.Export> exports = new ArrayList<>();
        for (int i = 0, batchIndex = 0; i < toExport.size(); i++) {
            infos.add(toExport.get(i));
            if (infos.size() == qrSize || toExport.size() == i + 1) {
                exports.add(new GoogleAuthInfo.Export(infos, batchId, batchIndex++, batchSize));
                infos = new ArrayList<>();
            }
        }

        if (exports.size() == 0) {
            Toast t = new Toast(requireContext());
            t.setText(R.string.pref_google_auth_export_no_data);
            t.show();
        } else {
            Intent intent = new Intent(requireContext(), TransferEntriesActivity.class);
            intent.putExtra("authInfos", exports);
            startActivity(intent);
        }
    }

    private static int getExportRequestCode(int spinnerPos, boolean encrypt) {
        if (spinnerPos == 0) {
            return encrypt ? CODE_EXPORT : CODE_EXPORT_PLAIN;
        }

        return CODE_EXPORT_GOOGLE_URI;
    }

    private static VaultBackupManager.FileInfo getExportFileInfo(int spinnerPos, boolean encrypt) {
        if (spinnerPos == 0) {
            String filename = encrypt ? VaultRepository.FILENAME_PREFIX_EXPORT : VaultRepository.FILENAME_PREFIX_EXPORT_PLAIN;
            return new VaultBackupManager.FileInfo(filename);
        }

        return new VaultBackupManager.FileInfo(VaultRepository.FILENAME_PREFIX_EXPORT_URI, "txt");
    }

    private static String getExportMimeType(int requestCode) {
        return requestCode == CODE_EXPORT_GOOGLE_URI ? "text/plain" : "application/json";
    }

    private File getExportCacheDir() throws IOException {
        File dir = new File(requireContext().getCacheDir(), "export");
        if (!dir.exists() && !dir.mkdir()) {
            throw new IOException(String.format("Unable to create directory %s", dir));
        }

        return dir;
    }

    private void startExportVault(int requestCode, StartExportCallback cb, @Nullable Vault.EntryFilter filter) {
        switch (requestCode) {
            case CODE_EXPORT:
                if (_vaultManager.getVault().isEncryptionEnabled()) {
                    cb.exportVault(stream -> {
                        if (filter != null) {
                            _vaultManager.getVault().exportFiltered(stream, filter);
                        } else {
                            _vaultManager.getVault().export(stream);
                        }
                    });
                } else {
                    Dialogs.showSetPasswordDialog(requireActivity(), new Dialogs.PasswordSlotListener() {
                        @Override
                        public void onSlotResult(PasswordSlot slot, Cipher cipher) {
                            VaultFileCredentials creds = new VaultFileCredentials();

                            try {
                                slot.setKey(creds.getKey(), cipher);
                                creds.getSlots().add(slot);
                            } catch (SlotException e) {
                                onException(e);
                                return;
                            }

                            cb.exportVault(stream -> {
                                if (filter != null) {
                                    _vaultManager.getVault().exportFiltered(stream, creds, filter);
                                } else {
                                    _vaultManager.getVault().export(stream, creds);
                                }
                            });
                        }

                        @Override
                        public void onException(Exception e) {

                        }
                    });
                }
                break;
            case CODE_EXPORT_PLAIN:
                cb.exportVault(stream -> {
                    if (filter != null) {
                        _vaultManager.getVault().exportFiltered(stream, null, filter);
                    } else {
                        _vaultManager.getVault().export(stream, null);
                    }
                });

                _prefs.setIsPlaintextBackupWarningNeeded(true);
                break;
            case CODE_EXPORT_GOOGLE_URI:
                cb.exportVault((stream) -> _vaultManager.getVault().exportGoogleUris(stream, filter));
                _prefs.setIsPlaintextBackupWarningNeeded(true);
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
                file = File.createTempFile(VaultRepository.FILENAME_PREFIX_EXPORT + "-", ".json", getExportCacheDir());
                outStream = new FileOutputStream(file);
                cb.exportVault(outStream);

                new ExportTask(requireContext(), new ExportResultListener()).execute(getLifecycle(), new ExportTask.Params(file, uri));
            } catch (VaultRepositoryException | IOException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(requireContext(), R.string.exporting_vault_error, e);
            } finally {
                try {
                    if (outStream != null) {
                        outStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, _exportFilter);
        _exportFilter = null;
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
                Dialogs.showErrorDialog(requireContext(), R.string.exporting_vault_error, e);
            } else {
                // if the user creates an export, hide the backup reminder
                _prefs.setIsBackupReminderNeeded(false);

                Toast.makeText(requireContext(), getString(R.string.exported_vault), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private interface FinishExportCallback {
        void exportVault(OutputStream stream) throws IOException, VaultRepositoryException;
    }

    private interface StartExportCallback {
        void exportVault(FinishExportCallback exportCb);
    }
}
