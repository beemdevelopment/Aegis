package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.BitmapHelper;
import com.beemdevelopment.aegis.helpers.FabScrollHelper;
import com.beemdevelopment.aegis.helpers.ViewHelper;
import com.beemdevelopment.aegis.icons.IconType;
import com.beemdevelopment.aegis.importers.DatabaseImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporterEntryException;
import com.beemdevelopment.aegis.importers.DatabaseImporterException;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.models.ImportEntry;
import com.beemdevelopment.aegis.ui.tasks.IconOptimizationTask;
import com.beemdevelopment.aegis.ui.tasks.RootShellTask;
import com.beemdevelopment.aegis.ui.views.ImportEntriesAdapter;
import com.beemdevelopment.aegis.util.UUIDMap;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultEntryIcon;
import com.beemdevelopment.aegis.vault.VaultGroup;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ImportEntriesActivity extends AegisActivity {
    private View _view;
    private Menu _menu;
    private RecyclerView _entriesView;
    private ImportEntriesAdapter _adapter;
    private FabScrollHelper _fabScrollHelper;

    private UUIDMap<VaultGroup> _importedGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }
        setContentView(R.layout.activity_import_entries);
        setSupportActionBar(findViewById(R.id.toolbar));
        ViewHelper.setupAppBarInsets(findViewById(R.id.app_bar_layout));

        _view = findViewById(R.id.importEntriesRootView);

        ActionBar bar = getSupportActionBar();
        bar.setHomeAsUpIndicator(R.drawable.ic_outline_close_24);
        bar.setDisplayHomeAsUpEnabled(true);

        _adapter = new ImportEntriesAdapter();
        _entriesView = findViewById(R.id.list_entries);
        _entriesView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                _fabScrollHelper.onScroll(dx, dy);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        _entriesView.setLayoutManager(layoutManager);
        _entriesView.setAdapter(_adapter);
        _entriesView.setNestedScrollingEnabled(false);

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

        DatabaseImporter.Definition importerDef = (DatabaseImporter.Definition) getIntent().getSerializableExtra("importerDef");
        startImport(importerDef, (File) getIntent().getSerializableExtra("file"));
    }

    private void startImport(DatabaseImporter.Definition importerDef, @Nullable File file) {
        DatabaseImporter importer = DatabaseImporter.create(this, importerDef.getType());
        if (file == null) {
            if (importer.isInstalledAppVersionSupported()) {
                startImportApp(importer);
            } else {
                Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Aegis_AlertDialog_Warning)
                        .setTitle(R.string.warning)
                        .setMessage(getString(R.string.app_version_error, importerDef.getName()))
                        .setCancelable(false)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(R.string.yes, (dialog1, which) -> {
                            startImportApp(importer);
                        })
                        .setNegativeButton(R.string.no, (dialog1, which) -> {
                            finish();
                        })
                        .create());
            }
        } else {
            startImportFile(importer, file);
        }
    }

    private void startImportFile(@NonNull DatabaseImporter importer, @NonNull File file) {
        try (InputStream stream = new FileInputStream(file)) {
            DatabaseImporter.State state = importer.read(stream);
            processImporterState(state);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show();
        } catch (DatabaseImporterException | IOException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.reading_file_error, e, (dialog, which) -> finish());
        }
    }

    private void startImportApp(@NonNull DatabaseImporter importer) {
        RootShellTask task = new RootShellTask(this, shell -> {
            if (isFinishing()) {
                return;
            }

            if (shell == null || !shell.isRoot()) {
                Toast.makeText(this, R.string.root_error, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            try {
                DatabaseImporter.State state = importer.readFromApp(shell);
                processImporterState(state);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.app_lookup_error, Toast.LENGTH_SHORT).show();
                finish();
            } catch (DatabaseImporterException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.reading_file_error, e, (dialog, which) -> finish());
            } finally {
                try {
                    shell.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        task.execute(this);
    }

    private void processImporterState(DatabaseImporter.State state) {
        try {
            if (state.isEncrypted()) {
                state.decrypt(this, new DatabaseImporter.DecryptListener() {
                    @Override
                    public void onStateDecrypted(DatabaseImporter.State state) {
                        processDecryptedImporterState(state);
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
                processDecryptedImporterState(state);
            }
        } catch (DatabaseImporterException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.parsing_file_error, e, (dialog, which) -> finish());
        }
    }

    private void processDecryptedImporterState(DatabaseImporter.State state) {
        DatabaseImporter.Result result;
        try {
            result = state.convert();
        } catch (DatabaseImporterException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.parsing_file_error, e, (dialog, which) -> finish());
            return;
        }

        Map<UUID, VaultEntryIcon> icons = result.getEntries().getValues().stream()
                .filter(e -> e.getIcon() != null
                        && !e.getIcon().getType().equals(IconType.SVG)
                        && !BitmapHelper.isVaultEntryIconOptimized(e.getIcon()))
                .collect(Collectors.toMap(VaultEntry::getUUID, VaultEntry::getIcon));
        if (!icons.isEmpty()) {
            IconOptimizationTask task = new IconOptimizationTask(this, newIcons -> {
                for (Map.Entry<UUID, VaultEntryIcon> mapEntry : newIcons.entrySet()) {
                    VaultEntry entry = result.getEntries().getByUUID(mapEntry.getKey());
                    entry.setIcon(mapEntry.getValue());
                }

                processImporterResult(result);
            });
            task.execute(getLifecycle(), icons);
        } else {
            processImporterResult(result);
        }
    }

    private void processImporterResult(DatabaseImporter.Result result) {
        List<ImportEntry> importEntries = new ArrayList<>();
        for (VaultEntry entry : result.getEntries().getValues()) {
            ImportEntry importEntry = new ImportEntry(entry);
            _adapter.addEntry(importEntry);
            importEntries.add(importEntry);
        }

        _importedGroups = result.getGroups();

        List<DatabaseImporterEntryException> errors = result.getErrors();
        if (errors.size() > 0) {
            String message = getResources().getQuantityString(R.plurals.import_error_dialog, errors.size(), errors.size());
            Dialogs.showMultiExceptionDialog(this, R.string.import_error_title, message, errors, null);
        }

        findDuplicates(importEntries);
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
            vault.wipeContents();
        }

        // Given the list of selected entries, collect the UUID's of all groups
        // that we're actually going to import
        List<ImportEntry> selectedEntries = _adapter.getCheckedEntries();
        List<UUID> selectedGroupUuids = new ArrayList<>();
        for (ImportEntry entry : selectedEntries) {
            selectedGroupUuids.addAll(entry.getEntry().getGroups());
        }

        // Add all of the new groups to the vault. If a group with the same name already
        // exists in the vault, rewrite all entries in that group to reference the existing group.
        for (VaultGroup importedGroup : _importedGroups) {
            if (!selectedGroupUuids.contains(importedGroup.getUUID())) {
                continue;
            }

            VaultGroup existingGroup = vault.findGroupByUUID(importedGroup.getUUID());
            if (existingGroup != null) {
                continue;
            }

            existingGroup = vault.findGroupByName(importedGroup.getName());
            if (existingGroup == null) {
                vault.addGroup(importedGroup);
            } else {
                for (ImportEntry entry : selectedEntries) {
                    Set<UUID> entryGroups = entry.getEntry().getGroups();
                    if (entryGroups.contains(importedGroup.getUUID())) {
                        entryGroups.remove(importedGroup.getUUID());
                        entryGroups.add(existingGroup.getUUID());
                    }
                }
            }
        }

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

            if (_iconPackManager.hasIconPack()) {
                ArrayList<UUID> assignIconEntriesIds = new ArrayList<>();
                Intent assignIconIntent = new Intent(getBaseContext(), AssignIconsActivity.class);
                for (ImportEntry entry : selectedEntries) {
                    assignIconEntriesIds.add(entry.getEntry().getUUID());
                }

                assignIconIntent.putExtra("entries", assignIconEntriesIds);

                Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.import_assign_icons_dialog_title)
                        .setMessage(R.string.import_assign_icons_dialog_text)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            startActivity(assignIconIntent);
                            finish();
                        })
                        .setNegativeButton(android.R.string.no, ((dialogInterface, i) -> finish()))
                        .create());
            } else {
                finish();
            }
        }
    }

    private void findDuplicates(List<ImportEntry> importEntries) {
        List<UUID> duplicateEntries = new ArrayList<>();
        for (ImportEntry importEntry: importEntries) {
            boolean exists = _vaultManager.getVault().getEntries().stream().anyMatch(item ->
                    item.getIssuer().equals(importEntry.getEntry().getIssuer()) &&
                    Arrays.equals(item.getInfo().getSecret(), importEntry.getEntry().getInfo().getSecret()));

            if (exists) {
                duplicateEntries.add(importEntry.getEntry().getUUID());
            }
        }

        if (duplicateEntries.size() == 0) {
            return;
        }

        _adapter.setCheckboxStates(duplicateEntries, false);
        Snackbar snackbar = Snackbar.make(_view, getResources().getQuantityString(R.plurals.import_duplicate_toast, duplicateEntries.size(), duplicateEntries.size()), Snackbar.LENGTH_INDEFINITE);
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onShown(Snackbar sb) {
                int snackbarHeight = sb.getView().getHeight();

                _entriesView.setPadding(
                        _entriesView.getPaddingLeft(),
                        _entriesView.getPaddingTop(),
                        _entriesView.getPaddingRight(),
                        _entriesView.getPaddingBottom() + snackbarHeight * 2
                );
            }

            @Override
            public void onDismissed(Snackbar sb, int event) {
                int snackbarHeight = sb.getView().getHeight();

                _entriesView.setPadding(
                        _entriesView.getPaddingLeft(),
                        _entriesView.getPaddingTop(),
                        _entriesView.getPaddingRight(),
                        _entriesView.getPaddingBottom() - snackbarHeight * 2
                );
            }
        });
        snackbar.setAction(R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _adapter.setCheckboxStates(duplicateEntries, true);
            }
        });
        snackbar.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_import_entries, _menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
        } else if (itemId == R.id.toggle_checkboxes) {
            _adapter.toggleCheckboxes();
        } else if (itemId == R.id.toggle_wipe_vault) {
            item.setChecked(!item.isChecked());
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }
}
