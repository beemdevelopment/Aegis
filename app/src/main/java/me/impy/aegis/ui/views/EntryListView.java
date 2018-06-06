package me.impy.aegis.ui.views;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.impy.aegis.R;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.helpers.SimpleItemTouchHelperCallback;
import me.impy.aegis.helpers.UiRefresher;
import me.impy.aegis.otp.TotpInfo;

public class EntryListView extends Fragment implements EntryAdapter.Listener {
    private EntryAdapter _adapter;
    private Listener _listener;

    private PeriodProgressBar _progressBar;
    private boolean _showProgress = false;

    private UiRefresher _refresher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _adapter = new EntryAdapter(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entry_list_view, container, false);

        _progressBar = view.findViewById(R.id.progressBar);
        int primaryColorId = getResources().getColor(R.color.colorPrimary);
        _progressBar.getProgressDrawable().setColorFilter(primaryColorId, PorterDuff.Mode.SRC_IN);

        // set up the recycler view
        RecyclerView rvKeyProfiles = view.findViewById(R.id.rvKeyProfiles);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(view.getContext());
        rvKeyProfiles.setLayoutManager(mLayoutManager);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(_adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(rvKeyProfiles);
        rvKeyProfiles.setAdapter(_adapter);

        _refresher = new UiRefresher(new UiRefresher.Listener() {
            @Override
            public void onRefresh() {
                refresh();
            }

            @Override
            public long getMillisTillNextRefresh() {
                return TotpInfo.getMillisTillNextRotation(_adapter.getUniformPeriod());
            }
        });

        return view;
    }

    public void refresh() {
        if (_showProgress) {
            _progressBar.refresh();
        }
        _adapter.notifyDataSetChanged();
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
            _progressBar.setVisibility(View.INVISIBLE);
            stopRefreshLoop();
        }
    }

    private void startRefreshLoop() {
        _refresher.start();
    }

    private void stopRefreshLoop() {
        refresh();
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

    public void setShowIssuer(boolean showIssuer) {
        _adapter.setShowIssuer(showIssuer);
        _adapter.notifyDataSetChanged();
    }

    public void addEntry(DatabaseEntry entry) {
        _adapter.addEntry(entry);
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

    public interface Listener {
        void onEntryClick(DatabaseEntry entry);
        void onEntryMove(DatabaseEntry entry1, DatabaseEntry entry2);
        void onEntryDrop(DatabaseEntry entry);
        void onEntryChange(DatabaseEntry entry);
    }
}
