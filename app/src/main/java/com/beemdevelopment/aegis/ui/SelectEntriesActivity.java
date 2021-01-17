package com.beemdevelopment.aegis.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.FabScrollHelper;
import com.beemdevelopment.aegis.importers.DatabaseImporterEntryException;
import com.beemdevelopment.aegis.ui.models.ImportEntry;
import com.beemdevelopment.aegis.ui.views.ImportEntriesAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class SelectEntriesActivity extends AegisActivity {
    private ImportEntriesAdapter _adapter;
    private FabScrollHelper _fabScrollHelper;
    private boolean _vaultContainsEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_entries);
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
                onScroll(dx, dy);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        entriesView.setLayoutManager(layoutManager);
        entriesView.setAdapter(_adapter);
        entriesView.setNestedScrollingEnabled(false);

        Intent intent = getIntent();
        List<ImportEntry> entries = (ArrayList<ImportEntry>) intent.getSerializableExtra("entries");
        List<DatabaseImporterEntryException> errors = (ArrayList<DatabaseImporterEntryException>) intent.getSerializableExtra("errors");
        _vaultContainsEntries = intent.getBooleanExtra("vaultContainsEntries", false);

        for (ImportEntry entry : entries) {
            _adapter.addEntry(entry);
        }

        if (errors.size() > 0) {
            showErrorDialog(errors);
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            if (_vaultContainsEntries) {
                showWipeEntriesDialog();
            } else {
                returnSelectedEntries(false);
            }
        });
        _fabScrollHelper = new FabScrollHelper(fab);
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
                    ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
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
                this::returnSelectedEntries
        );
    }

    private void returnSelectedEntries(boolean wipeEntries) {
        Intent intent = new Intent();
        List<ImportEntry> entries = _adapter.getCheckedEntries();
        intent.putExtra("entries", (ArrayList<ImportEntry>) entries);
        intent.putExtra("wipeEntries", wipeEntries);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void onScroll(int dx, int dy) {
        _fabScrollHelper.onScroll(dx, dy);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_select_entries, menu);
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
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }
}
