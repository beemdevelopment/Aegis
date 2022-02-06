package com.beemdevelopment.aegis.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.FabScrollHelper;
import com.beemdevelopment.aegis.importers.DatabaseImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporterEntryException;
import com.beemdevelopment.aegis.importers.DatabaseImporterException;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.models.ImportEntry;
import com.beemdevelopment.aegis.ui.views.ImportEntriesAdapter;
import com.beemdevelopment.aegis.util.UUIDMap;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImportEntriesActivity extends AegisActivity {
    private Menu _menu;
    private ImportEntriesAdapter _adapter;
    private FabScrollHelper _fabScrollHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }
        setContentView(R.layout.activity_import_entries);
        setSupportActionBar(findViewById(R.id.toolbar));

        ActionBar bar = getSupportActionBar();
        bar.setHomeAsUpIndicator(R.drawable.ic_close);
        bar.setDisplayHomeAsUpEnabled(true);

        _adapter = new ImportEntriesAdapter();
        RecyclerView entriesView = findViewById(R.id.list_entries);
        entriesView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                _fabScrollHelper.onScroll(dx, dy);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        entriesView.setLayoutManager(layoutManager);
        entriesView.setAdapter(_adapter);
        entriesView.setNestedScrollingEnabled(false);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            if (_vaultManager.getVault().getEntries().size() > 0
                    && _menu.findItem(R.id.toggle_wipe_vault).isChecked()) {
                showWipeEntriesDialog();
            } else {
                saveAndFinish(false);
            }
        });
        _fabScrollHelper = new FabScrollHelper(fab);

        Class<? extends DatabaseImporter> importerType = (Class<? extends DatabaseImporter>) getIntent().getSerializableExtra("importerType");
        startImport(importerType, (File) getIntent().getSerializableExtra("file"));
    }

    private void startImport(@NonNull Class<? extends DatabaseImporter> importerType, @Nullable File file) {
        if (file == null) {
            startImportApp(importerType);
        } else {
            startImportFile(importerType, file);
        }
    }

    private void startImportFile(@NonNull Class<? extends DatabaseImporter> importerType, @NonNull File file) {
        try (InputStream stream = new FileInputStream(file)) {
            DatabaseImporter importer = DatabaseImporter.create(this, importerType);
            DatabaseImporter.State state = importer.read(stream);
            processImporterState(state);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show();
        } catch (DatabaseImporterException | IOException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.reading_file_error, e, (dialog, which) -> finish());
        }
    }

    private void startImportApp(@NonNull Class<? extends DatabaseImporter> importerType) {
        DatabaseImporter importer = DatabaseImporter.create(this, importerType);

        // obtain the global root shell and close it immediately after we're done
        // TODO: find a way to use SuFileInputStream with Shell.newInstance()
        try (Shell shell = Shell.getShell()) {
            if (!shell.isRoot()) {
                Toast.makeText(this, R.string.root_error, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            DatabaseImporter.State state = importer.readFromApp();
            processImporterState(state);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.app_lookup_error, Toast.LENGTH_SHORT).show();
            finish();
        } catch (IOException | DatabaseImporterException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.reading_file_error, e, (dialog, which) -> finish());
        }
    }

    private void processImporterState(DatabaseImporter.State state) {
        try {
            if (state.isEncrypted()) {
                state.decrypt(this, new DatabaseImporter.DecryptListener() {
                    @Override
                    public void onStateDecrypted(DatabaseImporter.State state) {
                        importDatabase(state);
                    }

                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                        Dialogs.showErrorDialog(ImportEntriesActivity.this, R.string.decryption_error, e, (dialog, which) -> finish());
                    }

                    @Override
                    public void onCanceled() {
                        finish();
                    }
                });
            } else {
                importDatabase(state);
            }
        } catch (DatabaseImporterException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.parsing_file_error, e, (dialog, which) -> finish());
        }
    }

    private void importDatabase(DatabaseImporter.State state) {
        DatabaseImporter.Result result;
        try {
            result = state.convert();
        } catch (DatabaseImporterException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.parsing_file_error, e, (dialog, which) -> finish());
            return;
        }

        UUIDMap<VaultEntry> entries = result.getEntries();
        for (VaultEntry entry : entries.getValues()) {
            _adapter.addEntry(new ImportEntry(entry));
        }

        List<DatabaseImporterEntryException> errors = result.getErrors();
        if (errors.size() > 0) {
            showErrorDialog(errors);
        }
    }

    private void showErrorDialog(List<DatabaseImporterEntryException> errors) {
        Dialogs.showSecureDialog(new AlertDialog.Builder(this)
                .setTitle(R.string.import_error_title)
                .setMessage(getResources().getQuantityString(R.plurals.import_error_dialog, errors.size(), errors.size()))
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(getString(R.string.details), (dialog, which) -> showDetailedErrorDialog(errors))
                .create());
    }

    private void showDetailedErrorDialog(List<DatabaseImporterEntryException> errors) {
        List<String> messages = new ArrayList<>();
        for (DatabaseImporterEntryException e : errors) {
            messages.add(e.getMessage());
        }

        String message = TextUtils.join("\n\n", messages);
        Dialogs.showSecureDialog(new AlertDialog.Builder(this)
                .setTitle(R.string.import_error_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(android.R.string.copy, (dialog2, which2) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("text/plain", message);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, R.string.errors_copied, Toast.LENGTH_SHORT).show();
                })
                .create());
    }

    private void showWipeEntriesDialog() {
        Dialogs.showCheckboxDialog(this, R.string.dialog_wipe_entries_title,
                R.string.dialog_wipe_entries_message,
                R.string.dialog_wipe_entries_checkbox,
                this::saveAndFinish
        );
    }

    private void saveAndFinish(boolean wipeEntries) {
        VaultRepository vault = _vaultManager.getVault();
        if (wipeEntries) {
            vault.wipeEntries();
        }

        List<ImportEntry> selectedEntries = _adapter.getCheckedEntries();
        for (ImportEntry selectedEntry : selectedEntries) {
            VaultEntry entry = selectedEntry.getEntry();

            // temporary: randomize the UUID of duplicate entries and add them anyway
            if (vault.isEntryDuplicate(entry)) {
                entry.resetUUID();
            }

            vault.addEntry(entry);
        }

        if (saveAndBackupVault()) {
            String toastMessage = getResources().getQuantityString(R.plurals.imported_entries_count, selectedEntries.size(), selectedEntries.size());
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();

            setResult(RESULT_OK, null);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_import_entries, _menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.toggle_checkboxes:
                _adapter.toggleCheckboxes();
                break;
            case R.id.toggle_wipe_vault:
                item.setChecked(!item.isChecked());
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }
}
