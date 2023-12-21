package com.beemdevelopment.aegis.ui.slides;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.importers.AegisImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporterException;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.intro.SlideFragment;
import com.beemdevelopment.aegis.ui.tasks.ImportFileTask;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class WelcomeSlide extends SlideFragment {
    private boolean _imported;
    private VaultFileCredentials _creds;

    private final ActivityResultLauncher<Intent> vaultImportResultLauncher =
            registerForActivityResult(new StartActivityForResult(), activityResult -> {
                Intent data = activityResult.getData();
                if (data != null && data.getData() != null) {
                    startImportVault(data.getData());
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome_slide, container, false);
        view.findViewById(R.id.btnImport).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            vaultImportResultLauncher.launch(intent);
        });
        return view;
    }

    @Override
    public void onSaveIntroState(@NonNull Bundle introState) {
        introState.putBoolean("imported", _imported);
        introState.putSerializable("creds", _creds);
    }

    private void startImportVault(Uri uri) {
        ImportFileTask.Params params = new ImportFileTask.Params(uri, "intro-import", null);
        ImportFileTask task = new ImportFileTask(requireContext(), result -> {
            if (result.getError() != null) {
                Dialogs.showErrorDialog(requireContext(), R.string.reading_file_error, result.getError());
                return;
            }

            try (FileInputStream inStream = new FileInputStream(result.getFile())) {
                AegisImporter importer = new AegisImporter(requireContext());
                DatabaseImporter.State state = importer.read(inStream, false);
                if (state.isEncrypted()) {
                    state.decrypt(requireContext(), new DatabaseImporter.DecryptListener() {
                        @Override
                        protected void onStateDecrypted(DatabaseImporter.State state) {
                            _creds = ((AegisImporter.DecryptedState) state).getCredentials();
                            importVault(result.getFile());
                        }

                        @Override
                        protected void onError(Exception e) {
                            e.printStackTrace();
                            Dialogs.showErrorDialog(requireContext(), R.string.decryption_error, e);
                        }

                        @Override
                        protected void onCanceled() {

                        }
                    });
                } else {
                    importVault(result.getFile());
                }
            } catch (DatabaseImporterException | IOException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(requireContext(), R.string.intro_import_error_title, e);
            }
        });
        task.execute(getLifecycle(), params);
    }

    private void importVault(File file) {
        try (FileInputStream inStream = new FileInputStream(file)) {
            VaultRepository.writeToFile(requireContext(), inStream);
        } catch (IOException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(requireContext(), R.string.intro_import_error_title, e);
            return;
        }

        _imported = true;
        goToNextSlide();
    }
}
