package com.beemdevelopment.aegis.ui.views;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.helpers.ItemTouchHelperAdapter;
import com.beemdevelopment.aegis.helpers.comparators.IssuerNameComparator;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.db.DatabaseEntry;

public class EntryAdapter extends RecyclerView.Adapter<EntryHolder> implements ItemTouchHelperAdapter {
    private List<DatabaseEntry> _entries;
    private List<DatabaseEntry> _shownEntries;
    private static Listener _listener;
    private boolean _showAccountName;
    private boolean _tapToReveal;
    private int _tapToRevealTime;
    private String _groupFilter;
    private SortCategory _sortCategory;

    // keeps track of the viewholders that are currently bound
    private List<EntryHolder> _holders;

    public EntryAdapter(Listener listener) {
        _entries = new ArrayList<>();
        _shownEntries = new ArrayList<>();
        _holders = new ArrayList<>();
        _listener = listener;
    }

    public void setShowAccountName(boolean showAccountName) {
        _showAccountName = showAccountName;
    }

    public void setTapToReveal(boolean tapToReveal) {
        _tapToReveal = tapToReveal;
    }

    public void setTapToRevealTime(int number) {
        _tapToRevealTime = number;
    }

    public void addEntry(DatabaseEntry entry) {
        _entries.add(entry);
        if (!isEntryFiltered(entry)) {
            _shownEntries.add(entry);
        }

        int position = getItemCount() - 1;
        if (position == 0) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(position);
        }
    }

    public void addEntries(List<DatabaseEntry> entries) {
        _entries.addAll(entries);
        for (DatabaseEntry entry : entries) {
            if (!isEntryFiltered(entry)) {
                _shownEntries.add(entry);
            }
        }
        notifyDataSetChanged();
    }

    public void removeEntry(DatabaseEntry entry) {
        entry = getEntryByUUID(entry.getUUID());
        _entries.remove(entry);

        if (_shownEntries.contains(entry)) {
            int position = _shownEntries.indexOf(entry);
            _shownEntries.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clearEntries() {
        _entries.clear();
        _shownEntries.clear();
        notifyDataSetChanged();
    }

    public void replaceEntry(DatabaseEntry newEntry) {
        DatabaseEntry oldEntry = getEntryByUUID(newEntry.getUUID());
        _entries.set(_entries.indexOf(oldEntry), newEntry);

        if (_shownEntries.contains(oldEntry)) {
            int position = _shownEntries.indexOf(oldEntry);
            if (isEntryFiltered(newEntry)) {
                _shownEntries.remove(position);
                notifyItemRemoved(position);
            } else {
                _shownEntries.set(position, newEntry);
                notifyItemChanged(position);
            }
        } else if (!isEntryFiltered(newEntry)) {
            // TODO: preserve order
            _shownEntries.add(newEntry);

            int position = getItemCount() - 1;
            notifyItemInserted(position);
        }
    }

    private boolean isEntryFiltered(DatabaseEntry entry) {
        String group = entry.getGroup();
        if (_groupFilter == null) {
            return false;
        }
        return group == null || !group.equals(_groupFilter);
    }

    private DatabaseEntry getEntryByUUID(UUID uuid) {
        for (DatabaseEntry entry : _entries) {
            if (entry.getUUID().equals(uuid)) {
                return entry;
            }
        }
        throw new AssertionError("no entry found with the same id");
    }

    public void refresh(boolean hard) {
        if (hard) {
            notifyDataSetChanged();
        } else {
            for (EntryHolder holder : _holders) {
                holder.refreshCode();
            }
        }
    }

    public void setGroupFilter(String group) {
        _groupFilter = group;
        _shownEntries.clear();
        for (DatabaseEntry entry : _entries) {
            if (!isEntryFiltered(entry)) {
                _shownEntries.add(entry);
            }
        }
        notifyDataSetChanged();
    }

    public void setSortCategory(SortCategory sortCategory) {
        if (_sortCategory != sortCategory) {
            _sortCategory = sortCategory;
            Collections.sort(_shownEntries, new IssuerNameComparator());

            if(SortCategory.isReversed(sortCategory))
            {
                Collections.reverse(_shownEntries);
            }

            notifyDataSetChanged();
        }
    }

    @Override
    public void onItemDismiss(int position) {

    }

    @Override
    public void onItemDrop(int position) {
        // moving entries is not allowed when a filter is applied
        if (_groupFilter != null) {
            return;
        }

        _listener.onEntryDrop(_shownEntries.get(position));
    }

    @Override
    public void onItemMove(int firstPosition, int secondPosition) {
        // moving entries is not allowed when a filter is applied
        if (_groupFilter != null) {
            return;
        }

        // notify the database first
        _listener.onEntryMove(_entries.get(firstPosition), _entries.get(secondPosition));

        // update our side of things
        Collections.swap(_entries, firstPosition, secondPosition);
        Collections.swap(_shownEntries, firstPosition, secondPosition);
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
        _holders.remove(holder);
    }

    @Override
    public void onBindViewHolder(final EntryHolder holder, int position) {
        DatabaseEntry entry = _shownEntries.get(position);
        boolean showProgress = !isPeriodUniform() && entry.getInfo() instanceof TotpInfo;
        holder.setData(entry, _showAccountName, showProgress, _tapToReveal);
        holder.setTapToRevealTime(_tapToRevealTime);
        if (showProgress) {
            holder.startRefreshLoop();
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                if (_tapToReveal && holder.isCodeHidden()) {
                    holder.revealCode();
                } else {
                    _listener.onEntryClick(_shownEntries.get(position));
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int position = holder.getAdapterPosition();
                return _listener.onLongEntryClick(_shownEntries.get(position));
            }
        });
        holder.setOnRefreshClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // this will only be called if the entry is of type HotpInfo
                try {
                    ((HotpInfo) entry.getInfo()).incrementCounter();
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

        _holders.add(holder);
    }

    public int getUniformPeriod() {
        List<TotpInfo> infos = new ArrayList<>();
        for (DatabaseEntry entry : _shownEntries) {
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
        return _shownEntries.size();
    }

    public interface Listener {
        void onEntryClick(DatabaseEntry entry);
        boolean onLongEntryClick(DatabaseEntry entry);
        void onEntryMove(DatabaseEntry entry1, DatabaseEntry entry2);
        void onEntryDrop(DatabaseEntry entry);
        void onEntryChange(DatabaseEntry entry);
    }
}
