package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.views.TrashEntryAdapter;
import com.beemdevelopment.aegis.vault.VaultEntry;
import java.util.Collection;
import java.util.Collections;

public class TrashActivity extends AegisActivity implements TrashEntryAdapter.TrashEntryListener {

    private RecyclerView recyclerView;
    private TrashEntryAdapter adapter;

    private final androidx.activity.result.ActivityResultLauncher<Intent> authResultLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), activityResult -> {
                if (activityResult.getResultCode() == RESULT_OK) {
                    loadDeletedEntries();
                } else {
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.trash);

        recyclerView = findViewById(R.id.trash_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!_vaultManager.isVaultLoaded()) {
            Intent intent = new Intent(this, AuthActivity.class);
            authResultLauncher.launch(intent);
        } else {
            loadDeletedEntries();
        }
    }

    private void loadDeletedEntries() {
        Collection<VaultEntry> deletedEntries = _vaultManager.getVault().getDeletedEntries();
        adapter = new TrashEntryAdapter(this, deletedEntries, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onRestore(VaultEntry entry) {
        _vaultManager.getVault().restoreEntry(entry);
        saveAndBackupVault();
        adapter.removeItem(entry);
    }

    @Override
    public void onDeletePermanently(VaultEntry entry) {
        Dialogs.showDeleteEntriesDialog(this, Collections.singletonList(entry), (dialog, which) -> {
            _vaultManager.getVault().hardDeleteEntry(entry);
            saveAndBackupVault();
            adapter.removeItem(entry);
        });
    }

    @Override
    public void onLocked(boolean userInitiated) {
        // Do nothing. This will prevent the activity from being finished when the vault is locked.
        // The onStart() method will handle the case where the vault is not loaded.
    }
}
