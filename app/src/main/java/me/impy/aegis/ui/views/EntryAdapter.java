package me.impy.aegis.ui.views;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import me.impy.aegis.R;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.helpers.ItemTouchHelperAdapter;
import me.impy.aegis.otp.HotpInfo;
import me.impy.aegis.otp.OtpInfo;
import me.impy.aegis.otp.OtpInfoException;
import me.impy.aegis.otp.TotpInfo;

public class EntryAdapter extends RecyclerView.Adapter<EntryHolder> implements ItemTouchHelperAdapter {
    private List<DatabaseEntry> _entries;
    private static Listener _listener;
    private boolean _showIssuer;

    public EntryAdapter(Listener listener) {
        _entries = new ArrayList<>();
        _listener = listener;
    }

    public void setShowIssuer(boolean showIssuer) {
        _showIssuer = showIssuer;
    }

    public void addEntry(DatabaseEntry entry) {
        _entries.add(entry);

        int position = getItemCount() - 1;
        if (position == 0) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(position);
        }
    }

    public void addEntries(List<DatabaseEntry> entries) {
        _entries.addAll(entries);
        notifyDataSetChanged();
    }

    public void removeEntry(DatabaseEntry entry) {
        entry = getEntryByUUID(entry.getUUID());
        int position = _entries.indexOf(entry);
        _entries.remove(position);
        notifyItemRemoved(position);
    }

    public void clearEntries() {
        _entries.clear();
        notifyDataSetChanged();
    }

    public void replaceEntry(DatabaseEntry newEntry) {
        DatabaseEntry oldEntry = getEntryByUUID(newEntry.getUUID());
        int position = _entries.indexOf(oldEntry);
        _entries.set(position, newEntry);
        notifyItemChanged(position);
    }

    private DatabaseEntry getEntryByUUID(UUID uuid) {
        for (DatabaseEntry entry : _entries) {
            if (entry.getUUID().equals(uuid)) {
                return entry;
            }
        }
        throw new AssertionError("no entry found with the same id");
    }

    @Override
    public void onItemDismiss(int position) {

    }

    @Override
    public void onItemDrop(int position) {
        _listener.onEntryDrop(_entries.get(position));
    }

    @Override
    public void onItemMove(int firstPosition, int secondPosition) {
        // notify the database first
        _listener.onEntryMove(_entries.get(firstPosition), _entries.get(secondPosition));

        // update our side of things
        Collections.swap(_entries, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);
    }

    @Override
    public EntryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_entry, parent, false);
        return new EntryHolder(view);
    }

    @Override
    public void onViewRecycled(EntryHolder holder) {
        holder.stopRefreshLoop();
        super.onViewRecycled(holder);
    }

    @Override
    public void onBindViewHolder(final EntryHolder holder, int position) {
        DatabaseEntry entry = _entries.get(position);
        boolean showProgress = !isPeriodUniform() && entry.getInfo() instanceof TotpInfo;
        holder.setData(entry, _showIssuer, showProgress);
        if (showProgress) {
            holder.startRefreshLoop();
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                _listener.onEntryClick(_entries.get(position));
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int position = holder.getAdapterPosition();
                return _listener.onLongEntryClick(_entries.get(position));
            }
        });
        holder.setOnRefreshClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // this will only be called if the entry is of type HotpInfo
                try {
                    ((HotpInfo)entry.getInfo()).incrementCounter();
                } catch (OtpInfoException e) {
                    throw new RuntimeException(e);
                }

                // notify the listener that the counter has been incremented
                // this gives it a chance to save the database
                _listener.onEntryChange(entry);

                // finally, refresh the code in the UI
                holder.refreshCode();
            }
        });
    }

    public int getUniformPeriod() {
        List<TotpInfo> infos = new ArrayList<>();
        for (DatabaseEntry entry : _entries) {
            OtpInfo info = entry.getInfo();
            if (info instanceof TotpInfo) {
                infos.add((TotpInfo) info);
            }
        }

        if (infos.isEmpty()) {
            return -1;
        }

        int period = infos.get(0).getPeriod();
        for (TotpInfo info : infos) {
            if (period != info.getPeriod()) {
                return -1;
            }
        }

        return period;
    }

    public boolean isPeriodUniform() {
        return getUniformPeriod() != -1;
    }

    @Override
    public int getItemCount() {
        return _entries.size();
    }

    public interface Listener {
        void onEntryClick(DatabaseEntry entry);
        boolean onLongEntryClick(DatabaseEntry entry);
        void onEntryMove(DatabaseEntry entry1, DatabaseEntry entry2);
        void onEntryDrop(DatabaseEntry entry);
        void onEntryChange(DatabaseEntry entry);
    }
}
