package com.beemdevelopment.aegis.ui.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.models.AssignIconEntry;

import java.util.ArrayList;
import java.util.Collection;

public class AssignIconAdapter extends RecyclerView.Adapter<AssignIconHolder> {
    private AssignIconAdapter.Listener _listener;
    private ArrayList<AssignIconEntry> _entries;

    public AssignIconAdapter(AssignIconAdapter.Listener listener) {
        _listener = listener;
        _entries = new ArrayList<>();
    }

    public void addEntries(Collection<AssignIconEntry> entries) {
        _entries.addAll(entries);
    }

    @Override
    public AssignIconHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_assign_icon_entry, parent, false);
        AssignIconHolder holder = new AssignIconHolder(view);
        // NOTE: This assumes that the old and new icon views are the same size
        _listener.onSetPreloadView(holder.getOldIconView());
        return holder;
    }

    @Override
    public void onBindViewHolder(AssignIconHolder holder, int position) {
        holder.setData(_entries.get(position));
        holder.itemView.setOnClickListener(view -> {
            _listener.onAssignIconEntryClick(_entries.get(position));
        });
        _entries.get(position).setOnResetListener(holder);
    }

    @Override
    public int getItemCount() {
        return _entries.size();
    }

    public interface Listener {
        void onAssignIconEntryClick(AssignIconEntry entry);
        void onSetPreloadView(View view);
    }
}
