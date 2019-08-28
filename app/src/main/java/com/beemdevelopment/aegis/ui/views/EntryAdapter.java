package com.beemdevelopment.aegis.ui.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.helpers.ItemTouchHelperAdapter;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class EntryAdapter extends RecyclerView.Adapter<EntryHolder> implements ItemTouchHelperAdapter {
    private EntryListView _view;
    private List<DatabaseEntry> _entries;
    private List<DatabaseEntry> _shownEntries;
    private DatabaseEntry _selectedEntry;
    private boolean _showAccountName;
    private boolean _tapToReveal;
    private int _tapToRevealTime;
    private String _groupFilter;
    private SortCategory _sortCategory;
    private ViewMode _viewMode;
    private String _searchFilter;
    private boolean _isPeriodUniform = true;

    // keeps track of the viewholders that are currently bound
    private List<EntryHolder> _holders;

    public EntryAdapter(EntryListView view) {
        _entries = new ArrayList<>();
        _shownEntries = new ArrayList<>();
        _holders = new ArrayList<>();
        _view = view;
    }

    public void destroy() {
        for (EntryHolder holder : _holders) {
            holder.destroy();
        }
        _view = null;
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

    public DatabaseEntry getEntryAt(int position) {
        return _shownEntries.get(position);
    }

    public void addEntry(DatabaseEntry entry) {
        _entries.add(entry);
        if (isEntryFiltered(entry)) {
            return;
        }

        boolean added = false;
        Comparator<DatabaseEntry> comparator = _sortCategory.getComparator();
        if (comparator != null) {
            // insert the entry in the correct order
            // note: this assumes that _shownEntries has already been sorted
            for (int i = 0; i < _shownEntries.size(); i++) {
                if (comparator.compare(_shownEntries.get(i), entry) > 0) {
                    _shownEntries.add(i, entry);
                    notifyItemInserted(i);
                    added = true;
                    break;
                }
            }
        }

        if (!added){
            _shownEntries.add(entry);

            int position = getItemCount() - 1;
            if (position == 0) {
                notifyDataSetChanged();
            } else {
                notifyItemInserted(position);
            }
        }

        checkPeriodUniformity();
    }

    public void addEntries(List<DatabaseEntry> entries) {
        _entries.addAll(entries);
        updateShownEntries();
        checkPeriodUniformity(true);
    }

    public void removeEntry(DatabaseEntry entry) {
        _entries.remove(entry);

        if (_shownEntries.contains(entry)) {
            int position = _shownEntries.indexOf(entry);
            _shownEntries.remove(position);
            notifyItemRemoved(position);
        }

        checkPeriodUniformity();
    }

    public void clearEntries() {
        _entries.clear();
        _shownEntries.clear();
        notifyDataSetChanged();
        checkPeriodUniformity();
    }

    public void replaceEntry(DatabaseEntry oldEntry, DatabaseEntry newEntry) {
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

        checkPeriodUniformity();
    }

    private boolean isEntryFiltered(DatabaseEntry entry) {
        String group = entry.getGroup();
        String issuer = entry.getIssuer().toLowerCase();

        if (_groupFilter != null && (group == null || !group.equals(_groupFilter))) {
            return true;
        }

        return _searchFilter != null && !issuer.contains(_searchFilter);
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

    public void setGroupFilter(String group, boolean apply) {
        if (_groupFilter != null && _groupFilter.equals(group)) {
            return;
        }
        _groupFilter = group;
        if (apply) {
            updateShownEntries();
            checkPeriodUniformity();
        }
    }

    public void setSortCategory(SortCategory category, boolean apply) {
        if (_sortCategory == category) {
            return;
        }

        _sortCategory = category;
        if (apply) {
            updateShownEntries();
        }
    }

    public void setSearchFilter(String search) {
        _searchFilter = search != null ? search.toLowerCase() : null;
        updateShownEntries();
    }

    private void updateShownEntries() {
        // clear the list of shown entries first
        _shownEntries.clear();

        // add entries back that are not filtered out
        for (DatabaseEntry entry : _entries) {
            if (!isEntryFiltered(entry)) {
                _shownEntries.add(entry);
            }
        }

        // sort the remaining list of entries
        Comparator<DatabaseEntry> comparator = _sortCategory.getComparator();
        if (comparator != null) {
            Collections.sort(_shownEntries, comparator);
        }

        notifyDataSetChanged();
    }

    public void setViewMode(ViewMode viewMode) {
        _viewMode = viewMode;
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

        _view.onEntryDrop(_shownEntries.get(position));
    }

    @Override
    public void onItemMove(int firstPosition, int secondPosition) {
        // moving entries is not allowed when a filter is applied
        if (_groupFilter != null) {
            return;
        }

        // notify the database first
        _view.onEntryMove(_entries.get(firstPosition), _entries.get(secondPosition));

        // update our side of things
        Collections.swap(_entries, firstPosition, secondPosition);
        Collections.swap(_shownEntries, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);
    }

    @Override
    public int getItemViewType(int position) {
        return _viewMode.getLayoutId();
    }

    @Override
    public EntryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(_viewMode.getLayoutId(), parent, false);
        EntryHolder holder = new EntryHolder(view);
        _view.setPreloadView(holder.getIconView());
        return holder;
    }

    @Override
    public void onViewRecycled(EntryHolder holder) {
        holder.stopRefreshLoop();
        _holders.remove(holder);
    }

    @Override
    public void onBindViewHolder(final EntryHolder holder, int position) {
        DatabaseEntry entry = _shownEntries.get(position);
        holder.setFocused(entry == _selectedEntry);

        boolean showProgress = !isPeriodUniform() && entry.getInfo() instanceof TotpInfo;
        holder.setData(entry, _showAccountName, showProgress, _tapToReveal);
        holder.setTapToRevealTime(_tapToRevealTime);
        holder.loadIcon(_view);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                if (_tapToReveal && holder.isCodeHidden() && _selectedEntry == null) {
                    holder.revealCode();
                } else {
                    _view.onEntryClick(_shownEntries.get(position));
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int position = holder.getAdapterPosition();
                if (_selectedEntry == null) {
                    setSelectedEntry(_shownEntries.get(position));
                    holder.setFocused(true);
                }

                return _view.onLongEntryClick(_shownEntries.get(position));
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
                _view.onEntryChange(entry);

                // finally, refresh the code in the UI
                holder.refreshCode();
            }
        });

        _holders.add(holder);
    }

    private void checkPeriodUniformity() {
        checkPeriodUniformity(false);
    }

    private void checkPeriodUniformity(boolean force) {
        boolean uniform = isPeriodUniform();
        if (!force && uniform == _isPeriodUniform) {
            return;
        }
        _isPeriodUniform = uniform;

        for (EntryHolder holder : _holders) {
            holder.setShowProgress(!_isPeriodUniform);
        }

        _view.onPeriodUniformityChanged(_isPeriodUniform);
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

    public void setSelectedEntry(DatabaseEntry entry) {
        if (entry == null) {
            notifyItemChanged(_shownEntries.indexOf(_selectedEntry));
        }

        _selectedEntry = entry;
    }

    public boolean isDragAndDropAllowed() {
        return _sortCategory == SortCategory.CUSTOM && _groupFilter == null && _searchFilter == null;
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
        void onPeriodUniformityChanged(boolean uniform);
    }
}
