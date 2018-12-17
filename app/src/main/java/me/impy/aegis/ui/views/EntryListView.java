package me.impy.aegis.ui.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import java.util.List;

import me.impy.aegis.R;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.helpers.SimpleItemTouchHelperCallback;
import me.impy.aegis.helpers.UiRefresher;
import me.impy.aegis.otp.TotpInfo;

public class EntryListView extends Fragment implements EntryAdapter.Listener {
    private EntryAdapter _adapter;
    private Listener _listener;
    private SimpleItemTouchHelperCallback _touchCallback;

    private RecyclerView _rvKeyProfiles;
    private PeriodProgressBar _progressBar;
    private boolean _showProgress;

    private UiRefresher _refresher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _adapter = new EntryAdapter(this);
        _showProgress = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entry_list_view, container, false);

        _progressBar = view.findViewById(R.id.progressBar);
        int primaryColorId = getResources().getColor(R.color.colorPrimary);
        _progressBar.getProgressDrawable().setColorFilter(primaryColorId, PorterDuff.Mode.SRC_IN);

        // set up the recycler view
        _rvKeyProfiles = view.findViewById(R.id.rvKeyProfiles);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(view.getContext());
        _rvKeyProfiles.setLayoutManager(mLayoutManager);
        _touchCallback = new SimpleItemTouchHelperCallback(_adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(_touchCallback);
        touchHelper.attachToRecyclerView(_rvKeyProfiles);
        _rvKeyProfiles.setAdapter(_adapter);

        int resId = R.anim.layout_animation_fall_down;
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getContext(), resId);
        _rvKeyProfiles.setLayoutAnimation(animation);
        
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

    public void setGroupFilter(String group) {
        _adapter.setGroupFilter(group);
        _touchCallback.setIsLongPressDragEnabled(group == null);
        checkPeriodUniformity();

        runLayoutAnimation(_rvKeyProfiles);
    }

    public void refresh(boolean hard) {
        if (_showProgress) {
            _progressBar.refresh();
        }
        _adapter.refresh(hard);
    }

    private void checkPeriodUniformity() {
        boolean uniform = _adapter.isPeriodUniform();
        if (uniform == _showProgress) {
            return;
        }
        _showProgress = uniform;

        if (_showProgress) {
            _progressBar.setVisibility(View.VISIBLE);
            _progressBar.setPeriod(_adapter.getUniformPeriod());
            startRefreshLoop();
        } else {
            _progressBar.setVisibility(View.GONE);
            stopRefreshLoop();
        }
    }

    private void startRefreshLoop() {
        refresh(true);
        _refresher.start();
    }

    private void stopRefreshLoop() {
        refresh(true);
        _refresher.stop();
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

    public void setShowAccountName(boolean showAccountName) {
        _adapter.setShowAccountName(showAccountName);
    }

    public void addEntry(DatabaseEntry entry) {
        _adapter.addEntry(entry);
        checkPeriodUniformity();
    }

    public void addEntries(List<DatabaseEntry> entries) {
        _adapter.addEntries(entries);
        checkPeriodUniformity();
    }

    public void removeEntry(DatabaseEntry entry) {
        _adapter.removeEntry(entry);
        checkPeriodUniformity();
    }

    public void clearEntries() {
        _adapter.clearEntries();
        checkPeriodUniformity();
    }

    public void replaceEntry(DatabaseEntry entry) {
        _adapter.replaceEntry(entry);
        checkPeriodUniformity();
    }

    private void runLayoutAnimation(final RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();
        final LayoutAnimationController controller =
                AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down);

        recyclerView.setLayoutAnimation(controller);
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    public interface Listener {
        void onEntryClick(DatabaseEntry entry);
        void onEntryMove(DatabaseEntry entry1, DatabaseEntry entry2);
        void onEntryDrop(DatabaseEntry entry);
        void onEntryChange(DatabaseEntry entry);
    }
}
