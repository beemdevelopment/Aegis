package com.beemdevelopment.aegis.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.views.GroupAdapter;
import com.beemdevelopment.aegis.vault.VaultEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class GroupManagerActivity extends AegisActivity implements GroupAdapter.Listener {
    private GroupAdapter _adapter;
    private HashSet<String> _removedGroups;
    private RecyclerView _slotsView;
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        _backPressHandler = new BackPressHandler();
        getOnBackPressedDispatcher().addCallback(this, _backPressHandler);

        if (savedInstanceState != null) {
            List<String> groups = savedInstanceState.getStringArrayList("removedGroups");
            _removedGroups = new HashSet<>(Objects.requireNonNull(groups));
        } else {
            _removedGroups = new HashSet<>();
        }

        _adapter = new GroupAdapter(this);
        _slotsView= findViewById(R.id.list_slots);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        _slotsView.setLayoutManager(layoutManager);
        _slotsView.setAdapter(_adapter);
        _slotsView.setNestedScrollingEnabled(false);

        for (String group : _vaultManager.getVault().getGroups()) {
            _adapter.addGroup(group);
        }

        _emptyStateView = findViewById(R.id.vEmptyList);
        updateEmptyState();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("removedGroups", new ArrayList<>(_removedGroups));
    }

    @Override
    public void onRemoveGroup(String group) {
        Dialogs.showSecureDialog(new AlertDialog.Builder(this)
                .setTitle(R.string.remove_group)
                .setMessage(R.string.remove_group_description)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    _removedGroups.add(group);
                    _adapter.removeGroup(group);
                    _backPressHandler.setEnabled(true);
                    updateEmptyState();
                })
                .setNegativeButton(android.R.string.no, null)
                .create());
    }

    private void saveAndFinish() {
        if (!_removedGroups.isEmpty()) {
            for (VaultEntry entry : _vaultManager.getVault().getEntries()) {
                if (_removedGroups.contains(entry.getGroup())) {
                    entry.setGroup(null);
                }
            }

            saveAndBackupVault();
        }

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
        switch (item.getItemId()) {
            case android.R.id.home:
                discardAndFinish();
                break;
            case R.id.action_save:
                saveAndFinish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void updateEmptyState() {
        if (_adapter.getItemCount() > 0) {
            _slotsView.setVisibility(View.VISIBLE);
            _emptyStateView.setVisibility(View.GONE);
        } else {
            _slotsView.setVisibility(View.GONE);
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
