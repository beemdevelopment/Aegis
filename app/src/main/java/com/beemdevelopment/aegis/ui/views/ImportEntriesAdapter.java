package com.beemdevelopment.aegis.ui.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.ui.models.ImportEntry;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ImportEntriesAdapter extends RecyclerView.Adapter<ImportEntryHolder> {
    private List<ImportEntry> _entries;

    public ImportEntriesAdapter() {
        _entries = new ArrayList<>();
    }

    public void addEntry(ImportEntry entry) {
        _entries.add(entry);

        int position = getItemCount() - 1;
        if (position == 0) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(position);
        }
    }

    @NonNull
    @Override
    public ImportEntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_import_entry, parent, false);
        return new ImportEntryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImportEntryHolder holder, int position) {
        ImportEntry entry = _entries.get(position);
        entry.setOnCheckedChangedListener(holder);
        holder.setData(entry);
    }

    @Override
    public void onViewRecycled(@NonNull ImportEntryHolder holder) {
        holder.getEntry().setOnCheckedChangedListener(null);
    }

    @Override
    public int getItemCount() {
        return _entries.size();
    }

    public List<DatabaseEntry> getSelectedEntries() {
        List<DatabaseEntry> entries = new ArrayList<>();

        for (ImportEntry entry : getCheckedEntries()) {
            entries.add(entry.getDatabaseEntry());
        }

        return entries;
    }

    private List<ImportEntry> getCheckedEntries() {
        List<ImportEntry> entries = new ArrayList<>();

        for (ImportEntry entry : _entries) {
            if (entry.isChecked()) {
                entries.add(entry);
            }
        }

        return entries;
    }

    public void toggleCheckboxes() {
        int checkedEntries = getCheckedEntries().size();
        if (checkedEntries == 0 || checkedEntries != _entries.size()) {
            setCheckboxStates(true);
        } else {
            setCheckboxStates(false);
        }
    }

    private void setCheckboxStates(boolean checked) {
        for (ImportEntry entry: _entries) {
            if (entry.isChecked() != checked) {
                entry.setIsChecked(checked);
            }
        }
    }
}
