package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.views.GroupAdapter;

import java.text.Collator;
import java.util.ArrayList;
import java.util.TreeSet;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GroupManagerActivity extends AegisActivity implements GroupAdapter.Listener {
    private GroupAdapter _adapter;
    private TreeSet<String> _groups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        Intent intent = getIntent();
        _groups = new TreeSet<>(Collator.getInstance());
        _groups.addAll(intent.getStringArrayListExtra("groups"));

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

        for (String group : _groups) {
            _adapter.addGroup(group);
        }
    }

    @Override
    public void onRemoveGroup(String group) {
        Dialogs.showSecureDialog(new AlertDialog.Builder(this)
                .setTitle(R.string.remove_group)
                .setMessage(R.string.remove_group_description)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    _groups.remove(group);
                    _adapter.removeGroup(group);
                })
                .setNegativeButton(android.R.string.no, null)
                .create());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("groups", new ArrayList<>(_groups));
        setResult(RESULT_OK, intent);
        finish();
    }
}
