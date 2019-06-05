package com.beemdevelopment.aegis.ui.views;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.helpers.SimpleItemTouchHelperCallback;
import com.beemdevelopment.aegis.helpers.UiRefresher;
import com.beemdevelopment.aegis.otp.TotpInfo;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class EntryListView extends Fragment implements EntryAdapter.Listener {
    private EntryAdapter _adapter;
    private Listener _listener;
    private SimpleItemTouchHelperCallback _touchCallback;

    private RecyclerView _recyclerView;
    private RecyclerView.ItemDecoration _dividerDecoration;
    private PeriodProgressBar _progressBar;
    private boolean _showProgress;
    private ViewMode _viewMode;

    private UiRefresher _refresher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _adapter = new EntryAdapter(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(view.getContext());
        _recyclerView.setLayoutManager(mLayoutManager);
        _touchCallback = new SimpleItemTouchHelperCallback(_adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(_touchCallback);
        touchHelper.attachToRecyclerView(_recyclerView);
        _recyclerView.setAdapter(_adapter);

        int resId = R.anim.layout_animation_fall_down;
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getContext(), resId);
        _recyclerView.setLayoutAnimation(animation);

        _refresher = new UiRefresher(new UiRefresher.Listener() {
            @Override
            public void onRefresh() {
                refresh(false);
            }

            @Override
            public long getMillisTillNextRefresh() {
                return TotpInfo.getMillisTillNextRotation(_adapter.getUniformPeriod());
            }
        });

        return view;
    }

    public void setGroupFilter(String group, boolean apply) {
        _touchCallback.setIsLongPressDragEnabled(group == null);
        _adapter.setGroupFilter(group, apply);

        if (apply) {
            runEntriesAnimation();
        }
    }

    public void setSortCategory(SortCategory sortCategory, boolean apply) {
        _touchCallback.setIsLongPressDragEnabled(sortCategory == SortCategory.CUSTOM);
        _adapter.setSortCategory(sortCategory, apply);

        if (apply) {
            runEntriesAnimation();
        }
    }

    public void setSearchFilter(String search) {
        _touchCallback.setIsLongPressDragEnabled(search == null);
        _adapter.setSearchFilter(search);
    }

    public void setViewMode(ViewMode mode) {
        _viewMode = mode;
        updateDividerDecoration();
        _adapter.setViewMode(_viewMode);
    }

    public void refresh(boolean hard) {
        if (_showProgress) {
            _progressBar.refresh();
        }
        _adapter.refresh(hard);
    }

    public void setListener(Listener listener) {
        _listener = listener;
    }

    @Override
    public void onEntryClick(DatabaseEntry entry) {
        _listener.onEntryClick(entry);
    }

    @Override
    public boolean onLongEntryClick(DatabaseEntry entry) {
        return false;
    }

    @Override
    public void onEntryMove(DatabaseEntry entry1, DatabaseEntry entry2) {
        _listener.onEntryMove(entry1, entry2);
    }

    @Override
    public void onEntryDrop(DatabaseEntry entry) {
        _listener.onEntryDrop(entry);
    }

    @Override
    public void onEntryChange(DatabaseEntry entry) {
        _listener.onEntryChange(entry);
    }

    @Override
    public void onPeriodUniformityChanged(boolean isUniform) {
        setShowProgress(isUniform);
        if (_showProgress) {
            _progressBar.setVisibility(View.VISIBLE);
            _progressBar.setPeriod(_adapter.getUniformPeriod());
            _refresher.start();
        } else {
            _progressBar.setVisibility(View.GONE);
            _refresher.stop();
        }
    }

    public void setShowAccountName(boolean showAccountName) {
        _adapter.setShowAccountName(showAccountName);
    }

    public void setTapToReveal(boolean tapToReveal) {
        _adapter.setTapToReveal(tapToReveal);
    }

    public void setTapToRevealTime(int number) {
        _adapter.setTapToRevealTime(number);
    }

    public void addEntry(DatabaseEntry entry) {
        _adapter.addEntry(entry);
    }

    public void addEntries(List<DatabaseEntry> entries) {
        _adapter.addEntries(entries);
    }

    public void removeEntry(DatabaseEntry entry) {
        _adapter.removeEntry(entry);
    }

    public void clearEntries() {
        _adapter.clearEntries();
    }

    public void replaceEntry(DatabaseEntry entry) {
        _adapter.replaceEntry(entry);
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

    public interface Listener {
        void onEntryClick(DatabaseEntry entry);
        void onEntryMove(DatabaseEntry entry1, DatabaseEntry entry2);
        void onEntryDrop(DatabaseEntry entry);
        void onEntryChange(DatabaseEntry entry);
        void onScroll(int dx, int dy);
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
}
