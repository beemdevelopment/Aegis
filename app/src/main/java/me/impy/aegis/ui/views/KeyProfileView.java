package me.impy.aegis.ui.views;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.impy.aegis.R;
import me.impy.aegis.crypto.KeyInfo;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.helpers.SimpleItemTouchHelperCallback;

public class KeyProfileView extends Fragment implements KeyProfileAdapter.Listener {
    private KeyProfileAdapter _adapter;
    private Listener _listener;

    private PeriodProgressBar _progressBar;
    private Handler _uiHandler;
    private boolean _running = false;
    private boolean _showProgress = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _adapter = new KeyProfileAdapter(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_keyprofile_view, container, false);

        _uiHandler = new Handler();
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

    public void startRefreshLoop() {
        if (_running) {
            return;
        }
        _running = true;

        refresh();
        _uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (_running) {
                    refresh();
                    _uiHandler.postDelayed(this, KeyInfo.getMillisTillNextRotation(_adapter.getUniformPeriod()));
                }
            }
        }, KeyInfo.getMillisTillNextRotation(_adapter.getUniformPeriod()));
    }

    private void stopRefreshLoop() {
        refresh();
        _running = false;
    }

    public void setListener(Listener listener) {
        _listener = listener;
    }

    @Override
    public void onKeyProfileClick(KeyProfile profile) {
        _listener.onEntryClick(profile);
    }

    @Override
    public boolean onLongKeyProfileClick(KeyProfile profile) {
        return false;
    }

    @Override
    public void onKeyProfileMove(KeyProfile profile1, KeyProfile profile2) {
        _listener.onEntryMove(profile1.getEntry(), profile2.getEntry());
    }

    @Override
    public void onKeyProfileDrop(KeyProfile profile) {
        _listener.onEntryDrop(profile.getEntry());
    }

    public void setShowIssuer(boolean showIssuer) {
        _adapter.setShowIssuer(showIssuer);
        _adapter.notifyDataSetChanged();
    }

    public void addKey(KeyProfile profile) {
        _adapter.addKey(profile);
        checkPeriodUniformity();
    }

    public void removeKey(KeyProfile profile) {
        _adapter.removeKey(profile);
        checkPeriodUniformity();
    }

    public void clearKeys() {
        _adapter.clearKeys();
        checkPeriodUniformity();
    }

    public void replaceKey(KeyProfile profile) {
        _adapter.replaceKey(profile);
        checkPeriodUniformity();
    }

    public interface Listener {
        void onEntryClick(KeyProfile profile);
        void onEntryMove(DatabaseEntry entry1, DatabaseEntry entry2);
        void onEntryDrop(DatabaseEntry entry);
    }
}
