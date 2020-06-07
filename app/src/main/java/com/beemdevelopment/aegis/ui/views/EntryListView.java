package com.beemdevelopment.aegis.ui.views;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.helpers.SimpleItemTouchHelperCallback;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.util.ViewPreloadSizeProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class EntryListView extends Fragment implements EntryAdapter.Listener {
    private EntryAdapter _adapter;
    private Listener _listener;
    private SimpleItemTouchHelperCallback _touchCallback;

    private RecyclerView _recyclerView;
    private RecyclerView.ItemDecoration _dividerDecoration;
    private ViewPreloadSizeProvider<VaultEntry> _preloadSizeProvider;
    private TotpProgressBar _progressBar;
    private boolean _showProgress;
    private ViewMode _viewMode;
    private LinearLayout _emptyStateView;

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
                _listener.onScroll(dx, dy);
            }
        });

        // set up icon preloading
        _preloadSizeProvider = new ViewPreloadSizeProvider<>();
        IconPreloadProvider modelProvider = new IconPreloadProvider();
        RecyclerViewPreloader<VaultEntry> preloader = new RecyclerViewPreloader<>(Glide.with(this), modelProvider, _preloadSizeProvider, 10);
        _recyclerView.addOnScrollListener(preloader);

        LinearLayoutManager layoutManager = new LinearLayoutManager(view.getContext());
        _recyclerView.setLayoutManager(layoutManager);
        _touchCallback = new SimpleItemTouchHelperCallback(_adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(_touchCallback);
        touchHelper.attachToRecyclerView(_recyclerView);
        _recyclerView.setAdapter(_adapter);

        int resId = R.anim.layout_animation_fall_down;
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getContext(), resId);
        _recyclerView.setLayoutAnimation(animation);
        _emptyStateView = view.findViewById(R.id.vEmptyList);

        return view;
    }

    public void setPreloadView(View view) {
        _preloadSizeProvider.setView(view);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void setGroupFilter(String group, boolean apply) {
        _adapter.setGroupFilter(group, apply);
        _touchCallback.setIsLongPressDragEnabled(_adapter.isDragAndDropAllowed());

        if (apply) {
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

    public void setSearchFilter(String search) {
        _adapter.setSearchFilter(search);
        _touchCallback.setIsLongPressDragEnabled(_adapter.isDragAndDropAllowed());
    }

    public void setViewMode(ViewMode mode) {
        _viewMode = mode;
        updateDividerDecoration();
        _adapter.setViewMode(_viewMode);
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
        _listener.onEntryClick(entry);
    }

    public boolean onLongEntryClick(VaultEntry entry) {
        _listener.onLongEntryClick(entry);
        return true;
    }

    @Override
    public void onEntryMove(VaultEntry entry1, VaultEntry entry2) {
        _listener.onEntryMove(entry1, entry2);
    }

    @Override
    public void onEntryDrop(VaultEntry entry) {
        _listener.onEntryDrop(entry);
    }

    @Override
    public void onEntryChange(VaultEntry entry) {
        _listener.onEntryChange(entry);
    }

    @Override
    public void onEntryCopy(VaultEntry entry) {
        _listener.onEntryCopy(entry);
    }

    @Override
    public void onSelect(VaultEntry entry) { _listener.onSelect(entry); }

    @Override
    public void onDeselect(VaultEntry entry) { _listener.onDeselect(entry); }

    @Override
    public void onPeriodUniformityChanged(boolean isUniform, int period) {
        setShowProgress(isUniform);
        if (_showProgress) {
            _progressBar.setVisibility(View.VISIBLE);
            _progressBar.setPeriod(period);
            _progressBar.start();
        } else {
            _progressBar.setVisibility(View.GONE);
            _progressBar.stop();
        }
    }

    public void setCodeGroupSize(int codeGrouping) {
        _adapter.setCodeGroupSize(codeGrouping);
    }

    public void setShowAccountName(boolean showAccountName) {
        _adapter.setShowAccountName(showAccountName);
    }

    public void setSearchAccountName(boolean searchAccountName) {
        _adapter.setSearchAccountName(searchAccountName);
    }

    public void setHighlightEntry(boolean highlightEntry) {
        _adapter.setHighlightEntry(highlightEntry);
    }

    public void setTapToReveal(boolean tapToReveal) {
        _adapter.setTapToReveal(tapToReveal);
    }

    public void setTapToRevealTime(int number) {
        _adapter.setTapToRevealTime(number);
    }

    public void addEntry(VaultEntry entry) {
        _adapter.addEntry(entry);
        updateEmptyState();
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
        final Context context = _recyclerView.getContext();
        final LayoutAnimationController controller =
                AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down);

        _recyclerView.setLayoutAnimation(controller);
        _recyclerView.scheduleLayoutAnimation();
    }

    private void setShowProgress(boolean showProgress) {
        _showProgress = showProgress;
        updateDividerDecoration();
    }

    private void updateDividerDecoration() {
        if (_dividerDecoration != null) {
            _recyclerView.removeItemDecoration(_dividerDecoration);
        }

        float height = _viewMode.getDividerHeight();
        if (_showProgress && height == 0) {
            DividerItemDecoration divider = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
            divider.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.entry_divider));
            _dividerDecoration = divider;
        } else {
            _dividerDecoration = new VerticalSpaceItemDecoration(height);
        }

        _recyclerView.addItemDecoration(_dividerDecoration);
    }

    private void updateEmptyState() {
        if (_adapter.getItemCount() > 0) {
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
    }

    private class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {
        private int _height;

        private VerticalSpaceItemDecoration(float dp) {
            // convert dp to pixels
            _height = (int) (dp * (getContext().getResources().getDisplayMetrics().densityDpi / 160f));
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            if (parent.getChildAdapterPosition(view) == 0) {
                // the first item should also have a top margin
                outRect.top = _height;
            }
            outRect.bottom = _height;
        }
    }

    private class IconPreloadProvider implements ListPreloader.PreloadModelProvider<VaultEntry> {
        @NonNull
        @Override
        public List<VaultEntry> getPreloadItems(int position) {
            VaultEntry entry = _adapter.getEntryAt(position);
            if (!entry.hasIcon()) {
                return Collections.emptyList();
            }
            return Collections.singletonList(entry);
        }

        @Nullable
        @Override
        public RequestBuilder getPreloadRequestBuilder(@NonNull VaultEntry entry) {
            return Glide.with(EntryListView.this)
                        .asDrawable()
                        .load(entry)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(false);
        }
    }
}
