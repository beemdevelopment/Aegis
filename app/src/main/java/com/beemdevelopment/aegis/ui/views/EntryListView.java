package com.beemdevelopment.aegis.ui.views;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.helpers.MetricsHelper;
import com.beemdevelopment.aegis.helpers.SimpleItemTouchHelperCallback;
import com.beemdevelopment.aegis.helpers.ThemeHelper;
import com.beemdevelopment.aegis.helpers.UiRefresher;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.glide.IconLoader;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.util.ViewPreloadSizeProvider;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.divider.MaterialDividerItemDecoration;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

public class EntryListView extends Fragment implements EntryAdapter.Listener {
    private EntryAdapter _adapter;
    private Listener _listener;
    private SimpleItemTouchHelperCallback _touchCallback;
    private ItemTouchHelper _touchHelper;

    private RecyclerView _recyclerView;
    private RecyclerView.ItemDecoration _dividerDecoration;
    private ViewPreloadSizeProvider<VaultEntry> _preloadSizeProvider;
    private TotpProgressBar _progressBar;
    private boolean _showProgress;
    private ViewMode _viewMode;
    private TreeSet<String> _groups;
    private LinearLayout _emptyStateView;
    private Chip _groupChip;
    private List<String> _groupFilter;
    private List<String> _prefGroupFilter;

    private UiRefresher _refresher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _adapter = new EntryAdapter(this);
        _showProgress = false;
    }

    @Override
    public void onDestroy() {
        _adapter.destroy();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entry_list_view, container, false);
        _progressBar = view.findViewById(R.id.progressBar);
        _groupChip = view.findViewById(R.id.chip_group);
        initializeGroupChip();

        // set up the recycler view
        _recyclerView = view.findViewById(R.id.rvKeyProfiles);
        _recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (_listener != null) {
                    _listener.onScroll(dx, dy);
                }
            }
        });

        _recyclerView.setOnTouchListener((v, event) -> {
            if (_listener != null) {
                _listener.onEntryListTouch();
            }
            return false;
        });

        // set up icon preloading
        _preloadSizeProvider = new ViewPreloadSizeProvider<>();
        IconPreloadProvider modelProvider = new IconPreloadProvider();
        RecyclerViewPreloader<VaultEntry> preloader = new RecyclerViewPreloader<>(Glide.with(this), modelProvider, _preloadSizeProvider, 10);
        _recyclerView.addOnScrollListener(preloader);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        _recyclerView.setLayoutManager(layoutManager);
        _touchCallback = new SimpleItemTouchHelperCallback(_adapter);
        _touchHelper = new ItemTouchHelper(_touchCallback);
        _touchHelper.attachToRecyclerView(_recyclerView);
        _recyclerView.setAdapter(_adapter);

        int resId = R.anim.layout_animation_fall_down;
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(requireContext(), resId);
        _recyclerView.setLayoutAnimation(animation);

        _refresher = new UiRefresher(new UiRefresher.Listener() {
            @Override
            public void onRefresh() {
                refresh(false);
            }

            @Override
            public long getMillisTillNextRefresh() {
                return TotpInfo.getMillisTillNextRotation(_adapter.getMostFrequentPeriod());
            }
        });

        _emptyStateView = view.findViewById(R.id.vEmptyList);
        return view;
    }

    public void setPreloadView(View view) {
        _preloadSizeProvider.setView(view);
    }

    @Override
    public void onDestroyView() {
        _refresher.destroy();
        super.onDestroyView();
    }

    public void setGroupFilter(List<String> groups, boolean animate) {
        _groupFilter = groups;
        _adapter.setGroupFilter(groups);
        _touchCallback.setIsLongPressDragEnabled(_adapter.isDragAndDropAllowed());
        updateEmptyState();
        updateGroupChip();

        if (animate) {
            runEntriesAnimation();
        }
    }

    public void setIsLongPressDragEnabled(boolean enabled) {
        _touchCallback.setIsLongPressDragEnabled(enabled && _adapter.isDragAndDropAllowed());
    }

    public void setIsCopyOnTapEnabled(boolean enabled) {
       _adapter.setIsCopyOnTapEnabled(enabled);
    }

    public void setActionModeState(boolean enabled, VaultEntry entry) {
        _touchCallback.setSelectedEntry(entry);
        _touchCallback.setIsLongPressDragEnabled(enabled && _adapter.isDragAndDropAllowed());

        if (enabled) {
            _adapter.addSelectedEntry(entry);
        } else {
            _adapter.deselectAllEntries();
        }
    }

    public void setSortCategory(SortCategory sortCategory, boolean apply) {
        _adapter.setSortCategory(sortCategory, apply);
        _touchCallback.setIsLongPressDragEnabled(_adapter.isDragAndDropAllowed());

        if (apply) {
            runEntriesAnimation();
        }
    }

    public void setUsageCounts(Map<UUID, Integer> usageCounts) {
        _adapter.setUsageCounts(usageCounts);
    }

    public Map<UUID, Integer> getUsageCounts() {
        return _adapter.getUsageCounts();
    }

    public void setSearchFilter(String search) {
        _adapter.setSearchFilter(search);
        _touchCallback.setIsLongPressDragEnabled(_adapter.isDragAndDropAllowed());
    }

    public void setSelectedEntry(VaultEntry entry) {
        _touchCallback.setSelectedEntry(entry);
    }

    public void setViewMode(ViewMode mode) {
        _viewMode = mode;
        updateDividerDecoration();
        _adapter.setViewMode(_viewMode);
    }

    public void startDrag(RecyclerView.ViewHolder viewHolder) {
        _touchHelper.startDrag(viewHolder);
    }

    public void refresh(boolean hard) {
        if (_showProgress) {
            _progressBar.restart();
        }

        _adapter.refresh(hard);
    }

    public void setListener(Listener listener) {
        _listener = listener;
    }

    @Override
    public void onEntryClick(VaultEntry entry) {
        if (_listener != null) {
            _listener.onEntryClick(entry);
        }
    }

    public boolean onLongEntryClick(VaultEntry entry) {
        if (_listener != null) {
            _listener.onLongEntryClick(entry);
        }
        return true;
    }

    @Override
    public void onEntryMove(VaultEntry entry1, VaultEntry entry2) {
        if (_listener != null) {
            _listener.onEntryMove(entry1, entry2);
        }
    }

    @Override
    public void onEntryDrop(VaultEntry entry) {
        if (_listener != null) {
            _listener.onEntryDrop(entry);
        }
    }

    @Override
    public void onEntryChange(VaultEntry entry) {
        if (_listener != null) {
            _listener.onEntryChange(entry);
        }
    }

    @Override
    public void onEntryCopy(VaultEntry entry) {
        if (_listener != null) {
            _listener.onEntryCopy(entry);
        }
    }

    @Override
    public void onSelect(VaultEntry entry) {
        if (_listener != null) {
            _listener.onSelect(entry);
        }
    }

    @Override
    public void onDeselect(VaultEntry entry) {
        if (_listener != null) {
            _listener.onDeselect(entry);
        }
    }

    @Override
    public void onPeriodUniformityChanged(boolean isUniform, int period) {
        setShowProgress(isUniform);
        if (_showProgress) {
            _progressBar.setVisibility(View.VISIBLE);
            _progressBar.setPeriod(period);
            _progressBar.start();
            _refresher.start();
        } else {
            _progressBar.setVisibility(View.GONE);
            _progressBar.stop();
            _refresher.stop();
        }
    }

    @Override
    public void onListChange() {
        if (_listener != null) {
            _listener.onListChange();
        }
    }

    public void setPrefGroupFilter(List<String> groupFilter) {
        _prefGroupFilter = groupFilter;
    }

    public void setCodeGroupSize(Preferences.CodeGrouping codeGrouping) {
        _adapter.setCodeGroupSize(codeGrouping);
    }

    public void setShowAccountName(boolean showAccountName) {
        _adapter.setShowAccountName(showAccountName);
    }

    public void setShowIcon(boolean showIcon) {
        _adapter.setShowIcon(showIcon);
    }

    public void setHighlightEntry(boolean highlightEntry) {
        _adapter.setHighlightEntry(highlightEntry);
    }

    public void setPauseFocused(boolean pauseFocused) {
        _adapter.setPauseFocused(pauseFocused);
    }

    public void setTapToReveal(boolean tapToReveal) {
        _adapter.setTapToReveal(tapToReveal);
    }

    public void setTapToRevealTime(int number) {
        _adapter.setTapToRevealTime(number);
    }

    public void addEntry(VaultEntry entry) {
        addEntry(entry, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void addEntry(VaultEntry entry, boolean focusEntry) {
        int position = _adapter.addEntry(entry);
        updateEmptyState();

        LinearLayoutManager layoutManager = (LinearLayoutManager) _recyclerView.getLayoutManager();
        if (focusEntry && position >= 0) {
            if ((_recyclerView.canScrollVertically(1) && position > layoutManager.findLastCompletelyVisibleItemPosition())
                    || (_recyclerView.canScrollVertically(-1) && position < layoutManager.findFirstCompletelyVisibleItemPosition())) {
                RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            _recyclerView.removeOnScrollListener(this);
                            _recyclerView.setOnTouchListener(null);
                            tempHighlightEntry(entry);
                        }
                    }
                };
                _recyclerView.addOnScrollListener(scrollListener);
                _recyclerView.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        _recyclerView.removeOnScrollListener(scrollListener);
                        _recyclerView.stopScroll();
                        _recyclerView.setOnTouchListener(null);
                    }

                    return false;
                });
                _recyclerView.smoothScrollToPosition(position);
            } else {
                tempHighlightEntry(entry);
            }
        }
    }

    public void tempHighlightEntry(VaultEntry entry) {
        _adapter.setTempHighlightEntry(true);

        final int secondsToFocus = 3;
        _adapter.focusEntry(entry, secondsToFocus);
    }

    public void addEntries(Collection<VaultEntry> entries) {
        _adapter.addEntries(entries);
        updateEmptyState();
    }

    public void removeEntry(VaultEntry entry) {
        _adapter.removeEntry(entry);
        updateEmptyState();
    }

    public void removeEntry(UUID uuid) {
        _adapter.removeEntry(uuid);
        updateEmptyState();
    }

    public void clearEntries() {
        _adapter.clearEntries();
    }

    public void replaceEntry(UUID uuid, VaultEntry newEntry) {
        _adapter.replaceEntry(uuid, newEntry);
    }

    public void runEntriesAnimation() {
        final LayoutAnimationController controller =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down);

        _recyclerView.setLayoutAnimation(controller);
        _recyclerView.scheduleLayoutAnimation();
    }

    private void addChipTo(ChipGroup chipGroup, String group) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_material, null, false);
        chip.setText(group == null ? getString(R.string.no_group) : group);
        chip.setCheckable(true);
        chip.setChecked(_groupFilter != null && _groupFilter.contains(group));
        chip.setCheckedIconVisible(true);
        chip.setOnCheckedChangeListener((group1, checkedId) -> {
            List<String> groupFilter = getGroupFilter(chipGroup);
            setGroupFilter(groupFilter, true);
        });
        chip.setTag(group == null ? new Object() : null);
        chipGroup.addView(chip);
    }

    private void initializeGroupChip() {
        View view = getLayoutInflater().inflate(R.layout.dialog_select_groups, null);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(view);

        ChipGroup chipGroup = view.findViewById(R.id.groupChipGroup);
        Button clearButton = view.findViewById(R.id.btnClear);
        Button saveButton = view.findViewById(R.id.btnSave);
        clearButton.setOnClickListener(v -> {
            chipGroup.clearCheck();
            List<String> groupFilter = Collections.emptyList();
            if (_listener != null) {
                _listener.onSaveGroupFilter(groupFilter);
            }
            setGroupFilter(groupFilter, true);
            dialog.dismiss();
        });

        saveButton.setOnClickListener(v -> {
            List<String> groupFilter = getGroupFilter(chipGroup);
            if (_listener != null) {
                _listener.onSaveGroupFilter(groupFilter);
            }
            setGroupFilter(groupFilter, true);
            dialog.dismiss();
        });

        _groupChip.setOnClickListener(v -> {
            chipGroup.removeAllViews();

            for (String group : _groups) {
                addChipTo(chipGroup, group);
            }
            addChipTo(chipGroup, null);

            Dialogs.showSecureDialog(dialog);
        });
    }

    private static List<String> getGroupFilter(ChipGroup chipGroup) {
        return chipGroup.getCheckedChipIds().stream()
                .map(i -> {
                    Chip chip = chipGroup.findViewById(i);
                    if (chip.getTag() != null) {
                        return null;
                    }
                    return chip. getText().toString();
                })
                .collect(Collectors.toList());
    }

    private void updateGroupChip() {
        if (_groupFilter.isEmpty()) {
            _groupChip.setText(R.string.groups);
        } else {
            _groupChip.setText(String.format("%s (%d)", getString(R.string.groups), _groupFilter.size()));
        }
    }

    private void setShowProgress(boolean showProgress) {
        _showProgress = showProgress;
        updateDividerDecoration();
    }

    public void setGroups(TreeSet<String> groups) {
        _groups = groups;
        _groupChip.setVisibility(_groups.isEmpty() ? View.GONE : View.VISIBLE);
        updateDividerDecoration();

        if (_prefGroupFilter != null) {
            List<String> groupFilter = cleanGroupFilter(_prefGroupFilter);
            _prefGroupFilter = null;
            if (!groupFilter.isEmpty()) {
                setGroupFilter(groupFilter, false);
            }
        } else if (_groupFilter != null) {
            List<String> groupFilter = cleanGroupFilter(_groupFilter);
            if (!_groupFilter.equals(groupFilter)) {
                setGroupFilter(groupFilter, true);
            }
        }
    }

    private List<String> cleanGroupFilter(List<String> groupFilter) {
       return groupFilter.stream()
                .filter(g -> g == null || _groups.contains(g))
                .collect(Collectors.toList());
    }

    private void updateDividerDecoration() {
        if (_dividerDecoration != null) {
            _recyclerView.removeItemDecoration(_dividerDecoration);
        }

        float height = _viewMode.getDividerHeight();
        if (_showProgress && height == 0) {
            _dividerDecoration = new CompactDividerDecoration();
        } else {
            _dividerDecoration = new VerticalSpaceItemDecoration(height);
        }

        _recyclerView.addItemDecoration(_dividerDecoration);
    }

    private void updateEmptyState() {
        if (_adapter.getEntriesCount() > 0) {
            _recyclerView.setVisibility(View.VISIBLE);
            _emptyStateView.setVisibility(View.GONE);
        } else {
            _recyclerView.setVisibility(View.GONE);
            _emptyStateView.setVisibility(View.VISIBLE);
        }
    }

    public interface Listener {
        void onEntryClick(VaultEntry entry);
        void onEntryMove(VaultEntry entry1, VaultEntry entry2);
        void onEntryDrop(VaultEntry entry);
        void onEntryChange(VaultEntry entry);
        void onEntryCopy(VaultEntry entry);
        void onLongEntryClick(VaultEntry entry);
        void onScroll(int dx, int dy);
        void onSelect(VaultEntry entry);
        void onDeselect(VaultEntry entry);
        void onListChange();
        void onSaveGroupFilter(List<String> groupFilter);
        void onEntryListTouch();
    }

    private class CompactDividerDecoration extends MaterialDividerItemDecoration {
        public CompactDividerDecoration() {
            super(requireContext(), DividerItemDecoration.VERTICAL);
            setDividerColor(ThemeHelper.getThemeColor(R.attr.divider, requireContext().getTheme()));
            setLastItemDecorated(false);
            setDividerThickness(MetricsHelper.convertDpToPixels(requireContext(), 0.5f));
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            if (_adapter.isPositionFooter(parent.getChildAdapterPosition(view))) {
                int pixels = MetricsHelper.convertDpToPixels(requireContext(), 20);
                outRect.top = pixels;
                outRect.bottom = pixels;
                return;
            }

            super.getItemOffsets(outRect, view, parent, state);
        }
    }

    private class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int _height;

        private VerticalSpaceItemDecoration(float dp) {
            // convert dp to pixels
            _height = MetricsHelper.convertDpToPixels(requireContext(), dp);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int adapterPosition = parent.getChildAdapterPosition(view);
            if (adapterPosition == NO_POSITION) {
                return;
            }

            // The footer always has a top and bottom margin
            if (_adapter.isPositionFooter(adapterPosition)) {
                outRect.top = _height;
                outRect.bottom = _height;
                return;
            }

            // The first entry should have a top margin, but only if the group chip is not shown
            if (adapterPosition == 0 && (_groups == null || _groups.isEmpty())) {
                outRect.top = _height;
            }

            // Only non-favorite entries have a bottom margin, except for the final favorite entry
            int totalFavorites = _adapter.getShownFavoritesCount();
            if (totalFavorites == 0
                    || (adapterPosition < _adapter.getEntriesCount() && !_adapter.getEntryAt(adapterPosition).isFavorite())
                    || totalFavorites == adapterPosition + 1) {
                outRect.bottom = _height;
            }

            if (totalFavorites > 0) {
                // If this entry is the last favorite entry in the list, it should always have
                // a bottom margin, regardless of the view mode
                if (adapterPosition == totalFavorites - 1) {
                    outRect.bottom = _height;
                }

                // If this is the first non-favorite entry, it should have a top margin
                if (adapterPosition == totalFavorites) {
                    outRect.top = _height;
                }
            }

            // The last entry should never have a bottom margin
            if (_adapter.getEntriesCount() == adapterPosition + 1) {
                outRect.bottom = 0;
            }
        }
    }

    private class IconPreloadProvider implements ListPreloader.PreloadModelProvider<VaultEntry> {
        @NonNull
        @Override
        public List<VaultEntry> getPreloadItems(int position) {
            if (_adapter.getItemViewType(position) == R.layout.card_footer) {
                return Collections.emptyList();
            }

            VaultEntry entry = _adapter.getEntryAt(position);
            if (!entry.hasIcon()) {
                return Collections.emptyList();
            }
            return Collections.singletonList(entry);
        }

        @Nullable
        @Override
        public RequestBuilder<Drawable> getPreloadRequestBuilder(@NonNull VaultEntry entry) {
            return Glide.with(EntryListView.this)
                        .asDrawable()
                        .load(entry)
                        .set(IconLoader.ICON_TYPE, entry.getIconType())
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(false);
        }
    }
}
