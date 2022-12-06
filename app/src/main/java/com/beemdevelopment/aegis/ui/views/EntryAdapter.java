package com.beemdevelopment.aegis.ui.views;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.graphics.Typeface;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.helpers.ItemTouchHelperAdapter;
import com.beemdevelopment.aegis.helpers.comparators.FavoriteComparator;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.vault.VaultEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

public class EntryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemTouchHelperAdapter {
    private EntryListView _view;
    private List<VaultEntry> _entries;
    private List<VaultEntry> _shownEntries;
    private List<VaultEntry> _selectedEntries;
    private Map<UUID, Integer> _usageCounts;
    private VaultEntry _focusedEntry;
    private VaultEntry _copiedEntry;
    private Preferences.CodeGrouping _codeGroupSize;
    private boolean _showAccountName;
    private boolean _showIcon;
    private boolean _highlightEntry;
    private boolean _tempHighlightEntry;
    private boolean _tapToReveal;
    private int _tapToRevealTime;
    private boolean _copyOnTap;
    private List<String> _groupFilter;
    private SortCategory _sortCategory;
    private ViewMode _viewMode;
    private String _searchFilter;
    private boolean _isPeriodUniform = true;
    private int _uniformPeriod = -1;
    private Handler _dimHandler;
    private boolean _pauseFocused;

    // keeps track of the EntryHolders that are currently bound
    private List<EntryHolder> _holders;

    public EntryAdapter(EntryListView view) {
        _entries = new ArrayList<>();
        _shownEntries = new ArrayList<>();
        _selectedEntries = new ArrayList<>();
        _groupFilter = new ArrayList<>();
        _holders = new ArrayList<>();
        _dimHandler = new Handler();
        _view = view;
    }

    public void destroy() {
        for (EntryHolder holder : _holders) {
            holder.destroy();
        }
        _view = null;
    }

    public void setCodeGroupSize(Preferences.CodeGrouping codeGroupSize) {
        _codeGroupSize = codeGroupSize;
    }

    public void setShowAccountName(boolean showAccountName) {
        _showAccountName = showAccountName;
    }

    public void setShowIcon(boolean showIcon) {
        _showIcon = showIcon;
    }

    public void setTapToReveal(boolean tapToReveal) {
        _tapToReveal = tapToReveal;
    }

    public void setTapToRevealTime(int number) {
        _tapToRevealTime = number;
    }

    public void setHighlightEntry(boolean highlightEntry) {
        _highlightEntry = highlightEntry;
    }

    public void setTempHighlightEntry(boolean highlightEntry) {
        _tempHighlightEntry = highlightEntry;
    }

    public void setIsCopyOnTapEnabled(boolean enabled) {
        _copyOnTap = enabled;
    }

    public void setPauseFocused(boolean pauseFocused) {
        _pauseFocused = pauseFocused;
    }

    public VaultEntry getEntryAt(int position) {
        return _shownEntries.get(position);
    }

    public int addEntry(VaultEntry entry) {
        _entries.add(entry);
        if (isEntryFiltered(entry)) {
            return -1;
        }

        int position = -1;
        Comparator<VaultEntry> comparator = _sortCategory.getComparator();
        if (comparator != null) {
            // insert the entry in the correct order
            // note: this assumes that _shownEntries has already been sorted
            for (int i = getShownFavoritesCount(); i < _shownEntries.size(); i++) {
                if (comparator.compare(_shownEntries.get(i), entry) > 0) {
                    _shownEntries.add(i, entry);
                    notifyItemInserted(i);
                    position = i;
                    break;
                }
            }
        }

        if (position < 0){
            _shownEntries.add(entry);

            position = getItemCount() - 1;
            if (position == 0) {
                notifyDataSetChanged();
            } else {
                notifyItemInserted(position);
            }
        }

        _view.onListChange();
        checkPeriodUniformity();
        updateFooter();
        return position;
    }

    public void addEntries(Collection<VaultEntry> entries) {
        for (VaultEntry entry: entries) {
            entry.setUsageCount(_usageCounts.containsKey(entry.getUUID()) ? _usageCounts.get(entry.getUUID()) : 0);
        }

        _entries.addAll(entries);
        updateShownEntries();
        checkPeriodUniformity(true);
    }

    public void removeEntry(VaultEntry entry) {
        _entries.remove(entry);

        if (_shownEntries.contains(entry)) {
            int position = _shownEntries.indexOf(entry);
            _shownEntries.remove(position);
            notifyItemRemoved(position);
            updateFooter();
        }

        _view.onListChange();
        checkPeriodUniformity();
    }

    public void removeEntry(UUID uuid) {
        VaultEntry entry = getEntryByUUID(uuid);
        removeEntry(entry);
    }

    public void clearEntries() {
        _entries.clear();
        _shownEntries.clear();
        notifyDataSetChanged();
        checkPeriodUniformity();
    }

    public void replaceEntry(UUID uuid, VaultEntry newEntry) {
        VaultEntry oldEntry = getEntryByUUID(uuid);
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

            sortShownEntries();
            int newPosition = _shownEntries.indexOf(newEntry);
            if (newPosition != NO_POSITION && position != newPosition) {
                notifyItemMoved(position, newPosition);
            }
        } else if (!isEntryFiltered(newEntry)) {
            _shownEntries.add(newEntry);

            int position = getItemCount() - 1;
            notifyItemInserted(position);
        }
        checkPeriodUniformity();
        updateFooter();
    }

    private VaultEntry getEntryByUUID(UUID uuid) {
        for (VaultEntry entry : _entries) {
            if (entry.getUUID().equals(uuid)) {
                return entry;
            }
        }

        return null;
    }

    private boolean isEntryFiltered(VaultEntry entry) {
        String group = entry.getGroup();
        String issuer = entry.getIssuer().toLowerCase();
        String name = entry.getName().toLowerCase();

        if (!_groupFilter.isEmpty()) {
            if (!_groupFilter.contains(group)) {
                return true;
            }
        }

        if (_searchFilter == null) {
            return false;
        }

        return !issuer.contains(_searchFilter) && !name.contains(_searchFilter);
    }

    public void refresh(boolean hard) {
        if (hard) {
            updateShownEntries();
        } else {
            for (EntryHolder holder : _holders) {
                holder.refresh();
                holder.showIcon(_showIcon);
            }
        }
    }

    public void setGroupFilter(@NonNull List<String> groups) {
        if (_groupFilter.equals(groups)) {
            return;
        }

        _groupFilter = groups;
        updateShownEntries();
        checkPeriodUniformity();
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
        _searchFilter = (search != null && !search.isEmpty()) ? search.toLowerCase() : null;
        updateShownEntries();
    }

    private void updateShownEntries() {
        // clear the list of shown entries first
        _shownEntries.clear();

        // add entries back that are not filtered out
        for (VaultEntry entry : _entries) {
            if (!isEntryFiltered(entry)) {
                _shownEntries.add(entry);
            }
        }

        sortShownEntries();
        _view.onListChange();
        notifyDataSetChanged();
    }

    private boolean isEntryDraggable(VaultEntry entry) {
        return entry != null
                && isDragAndDropAllowed()
                && _selectedEntries.size() == 1
                && !_selectedEntries.get(0).isFavorite()
                && _selectedEntries.get(0) == entry;
    }

    private void sortShownEntries() {
        if (_sortCategory != null) {
            Comparator<VaultEntry> comparator = _sortCategory.getComparator();
            if (comparator != null) {
                Collections.sort(_shownEntries, comparator);
            }
        }

        Comparator<VaultEntry> favoriteComparator = new FavoriteComparator();
        Collections.sort(_shownEntries, favoriteComparator);
    }

    public void setViewMode(ViewMode viewMode) {
        _viewMode = viewMode;
    }

    public void setUsageCounts(Map<UUID, Integer> usageCounts) { _usageCounts = usageCounts; }

    public Map<UUID, Integer> getUsageCounts() { return _usageCounts; }

    public int getShownFavoritesCount() {
        return (int) _shownEntries.stream().filter(VaultEntry::isFavorite).count();
    }

    public void setGroups(TreeSet<String> groups) {
        _view.setGroups(groups);
    }

    @Override
    public void onItemDismiss(int position) {

    }

    @Override
    public void onItemDrop(int position) {
        // moving entries is not allowed when a filter is applied
        // footer cant be moved, nor can items be moved below it
        if (!_groupFilter.isEmpty() || isPositionFooter(position)) {
            return;
        }

        _view.onEntryDrop(_shownEntries.get(position));
    }

    @Override
    public void onItemMove(int firstPosition, int secondPosition) {
        // moving entries is not allowed when a filter is applied
        // footer cant be moved, nor can items be moved below it
        if (!_groupFilter.isEmpty() || isPositionFooter(firstPosition) || isPositionFooter(secondPosition)) {
            return;
        }

        // notify the vault first
        _view.onEntryMove(_entries.get(firstPosition), _entries.get(secondPosition));

        // update our side of things
        Collections.swap(_entries, firstPosition, secondPosition);
        Collections.swap(_shownEntries, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionFooter(position)) {
            return R.layout.card_footer;
        }

        return _viewMode.getLayoutId();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        RecyclerView.ViewHolder holder;
        View view = inflater.inflate(viewType, parent, false);
        holder = viewType == R.layout.card_footer ? new FooterView(view) : new EntryHolder(view);
        if (_showIcon && holder instanceof EntryHolder) {
            _view.setPreloadView(((EntryHolder) holder).getIconView());
        }

        return holder;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof EntryHolder) {
            ((EntryHolder) holder).stopRefreshLoop();
            _holders.remove(holder);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EntryHolder) {
            EntryHolder entryHolder = (EntryHolder) holder;
            VaultEntry entry = _shownEntries.get(position);

            boolean hidden = _tapToReveal && entry != _focusedEntry;
            boolean paused = _pauseFocused && entry == _focusedEntry;
            boolean dimmed = (_highlightEntry || _tempHighlightEntry) && _focusedEntry != null && _focusedEntry != entry;
            boolean showProgress = entry.getInfo() instanceof TotpInfo && ((TotpInfo) entry.getInfo()).getPeriod() != getMostFrequentPeriod();
            entryHolder.setData(entry, _codeGroupSize, _showAccountName, _showIcon, showProgress, hidden, paused, dimmed);
            entryHolder.setFocused(_selectedEntries.contains(entry));
            entryHolder.setShowDragHandle(isEntryDraggable(entry));

            if (_showIcon) {
                entryHolder.loadIcon(_view);
            }

            entryHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean handled = false;

                    if (_selectedEntries.isEmpty()) {
                        boolean copiedThisClick = false;

                        if (_copyOnTap && !entryHolder.isHidden() && !(entry == _copiedEntry)) {
                            _view.onEntryCopy(entry);
                            entryHolder.animateCopyText();
                            _copiedEntry = entry;
                            copiedThisClick = true;
                            handled = true;
                        }

                        if (_highlightEntry || _tempHighlightEntry || _tapToReveal) {
                            if (_focusedEntry == entry && !copiedThisClick) {
                                resetFocus();
                                _copiedEntry = null;
                                handled = true;
                            } else {
                                focusEntry(entry, _tapToRevealTime);
                            }
                        } else {
                            _copiedEntry = null;
                        }

                        incrementUsageCount(entry);
                    } else {
                        if (_selectedEntries.contains(entry)) {
                            _view.onDeselect(entry);
                            removeSelectedEntry(entry);
                            entryHolder.setFocusedAndAnimate(false);
                        } else {
                            entryHolder.setFocusedAndAnimate(true);
                            addSelectedEntry(entry);
                            _view.onSelect(entry);
                        }
                    }

                    if (!handled) {
                        _view.onEntryClick(entry);
                    }
                }
            });
            entryHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = holder.getAdapterPosition();
                    if (_selectedEntries.isEmpty()) {
                        entryHolder.setFocusedAndAnimate(true);
                    }

                    boolean returnVal = _view.onLongEntryClick(_shownEntries.get(position));
                    if (_selectedEntries.size() == 0 || isEntryDraggable(entry)) {
                        _view.startDrag(entryHolder);
                    }

                    return returnVal;
                }
            });
            entryHolder.itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Start drag if this is the only item selected
                    if (event.getActionMasked() == MotionEvent.ACTION_MOVE
                            && isEntryDraggable(entryHolder.getEntry())) {
                        _view.startDrag(entryHolder);
                        return true;
                    }
                    return false;
                }
            });
            entryHolder.setOnRefreshClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // this will only be called if the entry is of type HotpInfo
                    try {
                        ((HotpInfo) entry.getInfo()).incrementCounter();
                        focusEntry(entry, _tapToRevealTime);
                    } catch (OtpInfoException e) {
                        throw new RuntimeException(e);
                    }

                    // notify the listener that the counter has been incremented
                    // this gives it a chance to save the vault
                    _view.onEntryChange(entry);

                    // finally, refresh the code in the UI
                    entryHolder.refreshCode();
                }
            });

            _holders.add(entryHolder);
        } else if (holder instanceof FooterView) {
            ((FooterView) holder).refresh();
        }
    }

    private void checkPeriodUniformity() {
        checkPeriodUniformity(false);
    }

    private void checkPeriodUniformity(boolean force) {
        int mostFrequentPeriod = getMostFrequentPeriod();
        boolean uniform = isPeriodUniform();

        if (!force && uniform == _isPeriodUniform && mostFrequentPeriod == _uniformPeriod) {
            return;
        }

        _isPeriodUniform = uniform;
        _uniformPeriod = mostFrequentPeriod;

        for (EntryHolder holder : _holders) {
            if ((holder.getEntry().getInfo() instanceof TotpInfo)) {
                holder.setShowProgress(((TotpInfo) holder.getEntry().getInfo()).getPeriod() != mostFrequentPeriod);
            }
        }

        _view.onPeriodUniformityChanged(_isPeriodUniform, _uniformPeriod);
    }

    public int getMostFrequentPeriod() {
        List<TotpInfo> infos = new ArrayList<>();
        for (VaultEntry entry : _shownEntries) {
            OtpInfo info = entry.getInfo();
            if (info instanceof TotpInfo) {
                infos.add((TotpInfo) info);
            }
        }

        if (infos.isEmpty()) {
            return -1;
        }

        Map<Integer, Integer> occurrences = new HashMap<>();
        for (TotpInfo info : infos) {
            int period = info.getPeriod();
            if(occurrences.containsKey(period)) {
                occurrences.put(period, occurrences.get(period) + 1);
            } else {
                occurrences.put(period, 1);
            }
        }

        Integer maxValue = 0;
        Integer maxKey = 0;
        for (Map.Entry<Integer, Integer> entry : occurrences.entrySet()){
            if(entry.getValue() > maxValue){
                maxValue = entry.getValue();
                maxKey = entry.getKey();
            }
        }

        return maxValue > 1 ? maxKey : -1;
    }

    public void focusEntry(VaultEntry entry, int secondsToFocus) {
        _focusedEntry = entry;
        _dimHandler.removeCallbacksAndMessages(null);

        for (EntryHolder holder : _holders) {
            if (holder.getEntry() != _focusedEntry) {
                if (_highlightEntry || _tempHighlightEntry) {
                    holder.dim();
                }
                if (_tapToReveal) {
                    holder.hideCode();
                }
                if (_pauseFocused) {
                    holder.setPaused(false);
                }
            } else {
                if (_highlightEntry || _tempHighlightEntry) {
                    holder.highlight();
                }
                if (_tapToReveal) {
                    holder.revealCode();
                }
                if (_pauseFocused) {
                    holder.setPaused(true);
                }
            }
        }

        _dimHandler.postDelayed(this::resetFocus, secondsToFocus * 1000);
    }

    private void resetFocus() {
        for (EntryHolder holder : _holders) {
            if (_focusedEntry != null) {
                holder.highlight();
            }
            if (_tapToReveal) {
                holder.hideCode();
            }
            if (_pauseFocused) {
                holder.setPaused(false);
            }
        }

        _focusedEntry = null;
        _tempHighlightEntry = false;
    }

    private void updateDraggableStatus() {
        for (EntryHolder holder : _holders) {
            VaultEntry entry = holder.getEntry();
            if (isEntryDraggable(entry)) {
                holder.setShowDragHandle(true);
                _view.setSelectedEntry(entry);
                break;
            }

            holder.setShowDragHandle(false);
        }
    }

    public void removeSelectedEntry(VaultEntry entry) {
        _selectedEntries.remove(entry);
        updateDraggableStatus();
    }

    public void addSelectedEntry(VaultEntry entry) {
        if (_focusedEntry != null) {
            resetFocus();
        }

        _selectedEntries.add(entry);
        updateDraggableStatus();
    }

    public void deselectAllEntries() {
        for (VaultEntry entry: _selectedEntries) {
            for (EntryHolder holder : _holders) {
                if (holder.getEntry() == entry) {
                    holder.setFocusedAndAnimate(false);
                    break;
                }
            }
        }

        _selectedEntries.clear();

        updateDraggableStatus();
    }

    private void incrementUsageCount(VaultEntry entry) {
        if (!_usageCounts.containsKey(entry.getUUID())) {
            _usageCounts.put(entry.getUUID(), 1);
        } else {
            int usageCount = _usageCounts.get(entry.getUUID());
            _usageCounts.put(entry.getUUID(), ++usageCount);
        }
    }

    public boolean isDragAndDropAllowed() {
        return _sortCategory == SortCategory.CUSTOM && _groupFilter.isEmpty() && _searchFilter == null;
    }

    public boolean isPeriodUniform() {
        return isPeriodUniform(getMostFrequentPeriod());
    }

    private static boolean isPeriodUniform(int period) {
        return period != -1;
    }

    @Override
    public int getItemCount() {
        return getEntriesCount() + 1;
    }

    public int getEntriesCount() {
        return _shownEntries.size();
    }

    public boolean isPositionFooter(int position) {
        return position == getEntriesCount();
    }

    private void updateFooter() {
        notifyItemChanged(getItemCount() - 1);
    }

    private class FooterView extends RecyclerView.ViewHolder {
        View _footerView;

        public FooterView(@NonNull View itemView) {
            super(itemView);
            _footerView = itemView;
        }

        public void refresh() {
            int entriesShown = getEntriesCount();
            SpannableString entriesShownSpannable = new SpannableString(_footerView.getResources().getQuantityString(R.plurals.entries_shown, entriesShown, entriesShown));

            String entriesShownString = String.format("%d", entriesShown);
            int spanStart = entriesShownSpannable.toString().indexOf(entriesShownString);
            int spanEnd = spanStart + entriesShownString.length();
            entriesShownSpannable.setSpan(new StyleSpan(Typeface.BOLD), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            TextView textView = _footerView.findViewById(R.id.entries_shown_count);
            textView.setText(entriesShownSpannable);
        }
    }

    public interface Listener {
        void onEntryClick(VaultEntry entry);
        boolean onLongEntryClick(VaultEntry entry);
        void onEntryMove(VaultEntry entry1, VaultEntry entry2);
        void onEntryDrop(VaultEntry entry);
        void onEntryChange(VaultEntry entry);
        void onEntryCopy(VaultEntry entry);
        void onPeriodUniformityChanged(boolean uniform, int period);
        void onSelect(VaultEntry entry);
        void onDeselect(VaultEntry entry);
        void onListChange();
    }
}
