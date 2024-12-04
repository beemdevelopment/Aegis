package com.beemdevelopment.aegis.ui.views;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LayoutAnimationController;
import android.widget.LinearLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.AccountNamePosition;
import com.beemdevelopment.aegis.CopyBehavior;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.helpers.AnimationsHelper;
import com.beemdevelopment.aegis.helpers.MetricsHelper;
import com.beemdevelopment.aegis.helpers.SimpleItemTouchHelperCallback;
import com.beemdevelopment.aegis.helpers.UiRefresher;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.ui.glide.GlideHelper;
import com.beemdevelopment.aegis.ui.models.ErrorCardInfo;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultGroup;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.ViewPreloadSizeProvider;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EntryListView extends Fragment implements EntryAdapter.Listener {
    private EntryAdapter _adapter;
    private Listener _listener;
    private SimpleItemTouchHelperCallback _touchCallback;
    private ItemTouchHelper _touchHelper;

    private RecyclerView _recyclerView;
    private RecyclerView.ItemDecoration _itemDecoration;
    private ViewPreloadSizeProvider<VaultEntry> _preloadSizeProvider;
    private TotpProgressBar _progressBar;
    private boolean _showProgress;
    private boolean _showExpirationState;
    private ViewMode _viewMode;
    private LinearLayout _emptyStateView;

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

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 1);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (_viewMode == ViewMode.TILES
                        && (_adapter.isPositionFooter(position)
                        || _adapter.isPositionErrorCard(position))) {
                    return 2;
                }

                return 1;
            }
        });
        _recyclerView.setLayoutManager(layoutManager);
        _touchCallback = new SimpleItemTouchHelperCallback(_adapter);
        _touchHelper = new ItemTouchHelper(_touchCallback);
        _touchHelper.attachToRecyclerView(_recyclerView);
        _recyclerView.setAdapter(_adapter);

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

        final int rvInitialPaddingLeft = _recyclerView.getPaddingLeft();
        final int rvInitialPaddingTop = _recyclerView.getPaddingTop();
        final int rvInitialPaddingRight = _recyclerView.getPaddingRight();
        final int rvInitialPaddingBottom = _recyclerView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(_recyclerView, (targetView, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            // left and right padding seems to be handled by fitsSystemWindows="true" on the CoordinatorLayout in activity_main.xml
            targetView.setPadding(
                    rvInitialPaddingLeft,
                    rvInitialPaddingTop,
                    rvInitialPaddingRight,
                    rvInitialPaddingBottom + insets.bottom
            );
            return WindowInsetsCompat.CONSUMED;
        });

        _emptyStateView = view.findViewById(R.id.vEmptyList);
        return view;
    }

    public void setPreloadView(View view) {
        _preloadSizeProvider.setView(view);
    }

    public int getScrollPosition() {
        return ((LinearLayoutManager) _recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
    }

    public void scrollToPosition(int position) {
        _recyclerView.getLayoutManager().scrollToPosition(position);
    }

    @Override
    public void onDestroyView() {
        _refresher.destroy();
        super.onDestroyView();
    }

    public void setGroups(Collection<VaultGroup> groups) {
        _adapter.setGroups(groups);
        updateDividerDecoration();
    }

    public void setGroupFilter(Set<UUID> groups) {
        _adapter.setGroupFilter(groups);
        _touchCallback.setIsLongPressDragEnabled(_adapter.isDragAndDropAllowed());
        updateEmptyState();
    }

    public void setIsLongPressDragEnabled(boolean enabled) {
        _touchCallback.setIsLongPressDragEnabled(enabled && _adapter.isDragAndDropAllowed());
    }

    public void setCopyBehavior(CopyBehavior copyBehavior) {
        _adapter.setCopyBehavior(copyBehavior);
    }

    public void setSearchBehaviorMask(int searchBehaviorMask) {
        _adapter.setSearchBehaviorMask(searchBehaviorMask);
    }

    public List<VaultEntry> selectAllEntries() {
        return _adapter.selectAllEntries();
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
    }

    public void setUsageCounts(Map<UUID, Integer> usageCounts) {
        _adapter.setUsageCounts(usageCounts);
    }

    public Map<UUID, Integer> getUsageCounts() {
        return _adapter.getUsageCounts();
    }

    public void setLastUsedTimestamps(Map<UUID, Long> lastUsedTimestamps) {
        _adapter.setLastUsedTimestamps(lastUsedTimestamps);
    }

    public Map<UUID, Long> getLastUsedTimestamps() {
        return  _adapter.getLastUsedTimestamps();
    }

    public void setSearchFilter(String search) {
        _adapter.setSearchFilter(search);
        _touchCallback.setIsLongPressDragEnabled(_adapter.isDragAndDropAllowed());

        updateEmptyState();
    }

    public void setSelectedEntry(VaultEntry entry) {
        _touchCallback.setSelectedEntry(entry);
    }

    public void setViewMode(ViewMode mode) {
        _viewMode = mode;
        updateDividerDecoration();
        _adapter.setViewMode(_viewMode);
        if (_viewMode == ViewMode.TILES) {
            _touchCallback.setDragFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        } else {
            _touchCallback.setDragFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN);
        }

        ((GridLayoutManager)_recyclerView.getLayoutManager()).setSpanCount(mode.getSpanCount());
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

    public void setCodeGroupSize(Preferences.CodeGrouping codeGrouping) {
        _adapter.setCodeGroupSize(codeGrouping);
    }

    public void setAccountNamePosition(AccountNamePosition accountNamePosition) {
        _adapter.setAccountNamePosition(accountNamePosition);
    }

    public void setOnlyShowNecessaryAccountNames(boolean onlyShowNecessaryAccountNames) {
        _adapter.setOnlyShowNecessaryAccountNames(onlyShowNecessaryAccountNames);
    }

    public void setShowIcon(boolean showIcon) {
        _adapter.setShowIcon(showIcon);
    }

    public void setShowNextCode(boolean showNextCode) {
        _adapter.setShowNextCode(showNextCode);
    }

    public void setShowExpirationState(boolean showExpirationState) {
        _showExpirationState = showExpirationState;
        _adapter.setShowExpirationState(showExpirationState);
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

    public void setErrorCardInfo(ErrorCardInfo info) {
        _adapter.setErrorCardInfo(info);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onEntryAdded(VaultEntry entry) {
        int position = _adapter.getEntryPosition(entry);
        if (position < 0) {
            return;
        }

        LinearLayoutManager layoutManager = (LinearLayoutManager) _recyclerView.getLayoutManager();
        if ((_recyclerView.canScrollVertically(1) && position > layoutManager.findLastCompletelyVisibleItemPosition())
                || (_recyclerView.canScrollVertically(-1) && position < layoutManager.findFirstCompletelyVisibleItemPosition())) {
            boolean smoothScroll = !AnimationsHelper.Scale.TRANSITION.isZero(requireContext());
            RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
                private void handleScroll() {
                    _recyclerView.removeOnScrollListener(this);
                    _recyclerView.setOnTouchListener(null);
                    tempHighlightEntry(entry);
                }

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (smoothScroll && newState == RecyclerView.SCROLL_STATE_IDLE) {
                        handleScroll();
                    }
                }

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (!smoothScroll) {
                        handleScroll();
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
            // We can't easily control the speed of the smooth scroll animation, but we
            // can at least disable it if animations are disabled
            if (smoothScroll) {
                _recyclerView.smoothScrollToPosition(position);
            } else {
                _recyclerView.scrollToPosition(position);
            }
        } else {
            tempHighlightEntry(entry);
        }
    }

    public void tempHighlightEntry(VaultEntry entry) {
        _adapter.setTempHighlightEntry(true);

        final int secondsToFocus = 3;
        _adapter.focusEntry(entry, secondsToFocus);
    }

    public void setEntries(Collection<VaultEntry> entries) {
        _adapter.setEntries(new ArrayList<>(entries));
        updateEmptyState();
    }

    public void clearEntries() {
        _adapter.clearEntries();
        updateEmptyState();
    }

    public void runEntriesAnimation() {
        LayoutAnimationController animationController = AnimationsHelper.loadScaledLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down);

        _recyclerView.setLayoutAnimation(animationController);
        _recyclerView.scheduleLayoutAnimation();
    }

    private void setShowProgress(boolean showProgress) {
        _showProgress = showProgress;
        updateDividerDecoration();
    }

    private void updateDividerDecoration() {
        if (_itemDecoration != null) {
            _recyclerView.removeItemDecoration(_itemDecoration);
        }

        float offset = _viewMode.getItemOffset();
        if (_viewMode == ViewMode.TILES) {
            _itemDecoration = new TileSpaceItemDecoration(offset);
        } else {
            _itemDecoration = new VerticalSpaceItemDecoration(offset);
        }

        _recyclerView.addItemDecoration(_itemDecoration);
    }

    private void updateEmptyState() {
        if (_adapter.getShownEntriesCount() > 0) {
            _recyclerView.setVisibility(View.VISIBLE);
            _emptyStateView.setVisibility(View.GONE);
        } else {
            if (Strings.isNullOrEmpty(_adapter.getSearchFilter())) {
                _recyclerView.setVisibility(View.GONE);
                _emptyStateView.setVisibility(View.VISIBLE);
            }
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
        void onSaveGroupFilter(Set<UUID> groupFilter);
        void onEntryListTouch();
    }

    private class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int _offset;
        private final ShapeAppearanceModel _defaultShapeModel;

        private VerticalSpaceItemDecoration(float offset) {
            _offset = MetricsHelper.convertDpToPixels(requireContext(), offset);

            int shapeAppearanceId = getStyledAttrs(R.style.Widget_Aegis_EntryCardView,
                    com.google.android.material.R.attr.shapeAppearance);

            _defaultShapeModel = ShapeAppearanceModel.builder(
                    requireContext(), shapeAppearanceId, 0).build();
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int adapterPosition = parent.getChildAdapterPosition(view);
            if (adapterPosition == NO_POSITION) {
                return;
            }

            // The error card and the footer always have a top and bottom margin
            if (_adapter.isPositionErrorCard(adapterPosition)) {
                outRect.top = _viewMode == ViewMode.COMPACT ? _offset * 4 : _offset;
                outRect.bottom = _offset;
                return;
            }
            if (_adapter.isPositionFooter(adapterPosition)) {
                outRect.top = _offset * 2;
                outRect.bottom = _offset;
                return;
            }

            int entryIndex = _adapter.translateEntryPosToIndex(adapterPosition);
            // The first entry should have a top margin, but only if the error card is not shown
            if (entryIndex == 0 && !_adapter.isErrorCardShown()) {
                outRect.top = _offset;
            }

            // Only non-favorite entries have a bottom margin, except for the final favorite entry
            int totalFavorites = _adapter.getShownFavoritesCount();
            if (totalFavorites == 0
                    || (entryIndex < _adapter.getShownEntriesCount() && !_adapter.getEntryAtPosition(adapterPosition).isFavorite())
                    || totalFavorites == entryIndex + 1) {
                outRect.bottom = _offset;
            }

            // The last entry should never have a bottom margin
            if (_adapter.getShownEntriesCount() == entryIndex + 1) {
                outRect.bottom = 0;
            }

            decorateFavoriteEntries((MaterialCardView) view, parent);
        }

        private void decorateFavoriteEntries(@NonNull MaterialCardView view, @NonNull RecyclerView parent) {
            int adapterPosition = parent.getChildAdapterPosition(view);
            int entryIndex = _adapter.translateEntryPosToIndex(adapterPosition);
            int totalFavorites = _adapter.getShownFavoritesCount();

            ShapeAppearanceModel.Builder builder = _defaultShapeModel.toBuilder();
            if (entryIndex < totalFavorites) {
                if ((entryIndex == 0 && totalFavorites > 1) || (entryIndex < (totalFavorites - 1))) {
                    builder.setBottomLeftCorner(CornerFamily.ROUNDED, 0);
                    builder.setBottomRightCorner(CornerFamily.ROUNDED, 0);
                }
                if (entryIndex > 0) {
                    builder.setTopLeftCorner(CornerFamily.ROUNDED, 0);
                    builder.setTopRightCorner(CornerFamily.ROUNDED, 0);
                }
            }

            view.setShapeAppearanceModel(builder.build());
            view.setClipToOutline(true);
        }

        private int getStyledAttrs(@StyleRes int styleId, @AttrRes int attrId) {
            TypedArray cardAttrs = null;
            try {
                cardAttrs = requireContext().obtainStyledAttributes(styleId, new int[]{attrId});
                TypedValue value = new TypedValue();
                cardAttrs.getValue(0, value);
                return value.data;
            } finally {
                if (cardAttrs != null) {
                    cardAttrs.recycle();
                }
            }
        }
    }

    private class TileSpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int _offset;

        private TileSpaceItemDecoration(float offset) {
            _offset = MetricsHelper.convertDpToPixels(requireContext(), offset);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int adapterPosition = parent.getChildAdapterPosition(view);
            if (adapterPosition == NO_POSITION) {
                return;
            }

            outRect.left = _offset;
            outRect.right = _offset;
            outRect.top = _offset;
            outRect.bottom = _offset;

            if (_adapter.isPositionErrorCard(adapterPosition)
                    || (isInFirstEntryRow(adapterPosition) && !_adapter.isErrorCardShown())
                    || _adapter.isPositionFooter(adapterPosition)) {
                outRect.top *= 2;
            }
        }

        private boolean isInFirstEntryRow(int pos) {
            int index = _adapter.translateEntryPosToIndex(pos);
            return index >= 0 && index < _viewMode.getSpanCount();
        }
    }

    private class IconPreloadProvider implements ListPreloader.PreloadModelProvider<VaultEntry> {
        @NonNull
        @Override
        public List<VaultEntry> getPreloadItems(int position) {
            if (_adapter.getItemViewType(position) == R.layout.card_footer) {
                return Collections.emptyList();
            }

            if (_adapter.getItemViewType(position) == R.layout.card_error) {
                return Collections.emptyList();
            }

            VaultEntry entry = _adapter.getEntryAtPosition(position);
            if (!entry.hasIcon()) {
                return Collections.emptyList();
            }

            return Collections.singletonList(entry);
        }

        @Nullable
        @Override
        public RequestBuilder<Drawable> getPreloadRequestBuilder(@NonNull VaultEntry entry) {
            RequestBuilder<Drawable> rb = Glide.with(EntryListView.this)
                    .load(entry.getIcon());
            return GlideHelper.setCommonOptions(rb, entry.getIcon().getType());
        }
    }
}
