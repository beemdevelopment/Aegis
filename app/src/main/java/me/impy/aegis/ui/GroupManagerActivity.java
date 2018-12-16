package me.impy.aegis.ui;

import android.os.Bundle;

import java.util.TreeSet;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.impy.aegis.AegisApplication;
import me.impy.aegis.R;
import me.impy.aegis.db.DatabaseFileCredentials;
import me.impy.aegis.db.DatabaseManager;
import me.impy.aegis.db.slots.Slot;
import me.impy.aegis.ui.views.GroupAdapter;
import me.impy.aegis.ui.views.SlotAdapter;

public class GroupManagerActivity extends AegisActivity implements GroupAdapter.Listener {
    private AegisApplication _app;
    private DatabaseManager _db;
    private GroupAdapter _adapter;

    TreeSet<String> groups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        _app = (AegisApplication) getApplication();
        _db = _app.getDatabaseManager();

        groups = _db.getGroups();

        ActionBar bar = getSupportActionBar();
        bar.setHomeAsUpIndicator(R.drawable.ic_close);
        bar.setDisplayHomeAsUpEnabled(true);

        // set up the recycler view
        _adapter = new GroupAdapter(this);
        RecyclerView slotsView = findViewById(R.id.list_slots);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        slotsView.setLayoutManager(layoutManager);
        slotsView.setAdapter(_adapter);
        slotsView.setNestedScrollingEnabled(false);

        // load the slots and masterKey
        for (String group : groups) {
            _adapter.addGroup(group);
        }
    }

    @Override
    public void onRemoveGroup(String group) {
        Dialogs.showSecureDialog(new AlertDialog.Builder(this)
                .setTitle(R.string.remove_group)
                .setMessage(R.string.remove_group_description)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    _db.removeGroup(group);
                    groups.remove(group);
                    _adapter.removeGroup(group);
                })
                .setNegativeButton(android.R.string.no, null)
                .create());
    }
}
