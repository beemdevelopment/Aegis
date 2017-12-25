package me.impy.aegis;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.helpers.SimpleItemTouchHelperCallback;

public class KeyProfileView extends Fragment implements KeyProfileAdapter.Listener {
    private KeyProfileAdapter _adapter;
    private Listener _listener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _adapter = new KeyProfileAdapter(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_keyprofile_view, container, false);

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
    }

    public void removeKey(KeyProfile profile) {
        _adapter.removeKey(profile);
    }

    public void clearKeys() {
        _adapter.clearKeys();
    }

    public interface Listener {
        void onEntryClick(KeyProfile profile);
        void onEntryMove(DatabaseEntry entry1, DatabaseEntry entry2);
        void onEntryDrop(DatabaseEntry entry);
    }
}
