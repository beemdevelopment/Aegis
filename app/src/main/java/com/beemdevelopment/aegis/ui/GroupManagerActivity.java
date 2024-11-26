package com.beemdevelopment.aegis.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.views.GroupAdapter;
import com.beemdevelopment.aegis.util.Cloner;
import com.beemdevelopment.aegis.helpers.ViewHelper;
import com.beemdevelopment.aegis.vault.VaultGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GroupManagerActivity extends AegisActivity implements GroupAdapter.Listener {
    private GroupAdapter _adapter;
    private HashSet<UUID> _removedGroups;
    private RecyclerView _groupsView;
    private View _emptyStateView;
    private BackPressHandler _backPressHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }
        setContentView(R.layout.activity_groups);
        setSupportActionBar(findViewById(R.id.toolbar));
        ViewHelper.setupAppBarInsets(findViewById(R.id.app_bar_layout));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        _backPressHandler = new BackPressHandler();
        getOnBackPressedDispatcher().addCallback(this, _backPressHandler);

        _removedGroups = new HashSet<>();
        if (savedInstanceState != null) {
            List<String> removedGroups = savedInstanceState.getStringArrayList("removedGroups");
            if (removedGroups != null) {
                for (String uuid : removedGroups) {
                    _removedGroups.add(UUID.fromString(uuid));
                }
            }
        }

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder) {

                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int draggedItemIndex = viewHolder.getBindingAdapterPosition();
                int targetIndex = target.getBindingAdapterPosition();

                _adapter.onItemMove(draggedItemIndex, targetIndex);

                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }
        });

        _adapter = new GroupAdapter(this);
        _groupsView = findViewById(R.id.list_groups);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        _groupsView.setLayoutManager(layoutManager);
        _groupsView.setAdapter(_adapter);
        _groupsView.setNestedScrollingEnabled(false);
        touchHelper.attachToRecyclerView(_groupsView);

        for (VaultGroup group : _vaultManager.getVault().getGroups()) {
            if (!_removedGroups.contains(group.getUUID())) {
                _adapter.addGroup(group);
            }
        }

        _emptyStateView = findViewById(R.id.vEmptyList);
        updateEmptyState();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<String> removed = new ArrayList<>();
        for (UUID uuid : _removedGroups) {
            removed.add(uuid.toString());
        }

        outState.putStringArrayList("removedGroups", removed);
    }

    @Override
    public void onEditGroup(VaultGroup group) {
        Dialogs.TextInputListener onEditGroup = text -> {
            String newGroupName = new String(text).trim();
            if (!newGroupName.isEmpty()) {
                VaultGroup newGroup = Cloner.clone(group);
                newGroup.setName(newGroupName);
                _adapter.replaceGroup(group.getUUID(), newGroup);
                _backPressHandler.setEnabled(true);
            }
        };

        Dialogs.showTextInputDialog(GroupManagerActivity.this, R.string.rename_group, R.string.group_name_hint, onEditGroup, group.getName());
    }

    @Override
    public void onRemoveGroup(VaultGroup group) {
        Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Aegis_AlertDialog_Warning)
                .setTitle(R.string.remove_group)
                .setMessage(R.string.remove_group_description)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    _removedGroups.add(group.getUUID());
                    _adapter.removeGroup(group);
                    _backPressHandler.setEnabled(true);
                    updateEmptyState();
                })
                .setNegativeButton(android.R.string.no, null)
                .create());
    }

    public void onRemoveUnusedGroups() {
        Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Aegis_AlertDialog_Warning)
                .setTitle(R.string.remove_unused_groups)
                .setMessage(R.string.remove_unused_groups_description)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    Set<VaultGroup> unusedGroups = new HashSet<>(_vaultManager.getVault().getGroups());
                    unusedGroups.removeAll(_vaultManager.getVault().getUsedGroups());

                    for (VaultGroup group : unusedGroups) {
                        _removedGroups.add(group.getUUID());
                        _adapter.removeGroup(group);
                    }
                    _backPressHandler.setEnabled(true);
                    updateEmptyState();
                })
                .setNegativeButton(android.R.string.no, null)
                .create());
    }

    private void saveAndFinish() {
        if (!_removedGroups.isEmpty()) {
            for (UUID uuid : _removedGroups) {
                _vaultManager.getVault().removeGroup(uuid);
            }
        }

        _vaultManager.getVault().replaceGroups(_adapter.getGroups());
        saveAndBackupVault();

        finish();
    }

    private void discardAndFinish() {
        if (_removedGroups.isEmpty()) {
            finish();
            return;
        }

        Dialogs.showDiscardDialog(this,
                (dialog, which) -> saveAndFinish(),
                (dialog, which) -> finish());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_groups, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            discardAndFinish();
        } else if (itemId == R.id.action_save) {
            saveAndFinish();
        } else if (itemId == R.id.action_delete_unused_groups) {
            onRemoveUnusedGroups();
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void updateEmptyState() {
        if (_adapter.getItemCount() > 0) {
            _groupsView.setVisibility(View.VISIBLE);
            _emptyStateView.setVisibility(View.GONE);
        } else {
            _groupsView.setVisibility(View.GONE);
            _emptyStateView.setVisibility(View.VISIBLE);
        }
    }

    private class BackPressHandler extends OnBackPressedCallback {
        public BackPressHandler() {
            super(false);
        }

        @Override
        public void handleOnBackPressed() {
            discardAndFinish();
        }
    }
}
