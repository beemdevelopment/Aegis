package com.beemdevelopment.aegis.helpers;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.beemdevelopment.aegis.ui.views.EntryAdapter;
import com.beemdevelopment.aegis.vault.VaultEntry;

public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private VaultEntry _selectedEntry;

    private final EntryAdapter _adapter;

    private boolean _positionChanged = false;

    private boolean _isLongPressDragEnabled = true;

    public SimpleItemTouchHelperCallback(EntryAdapter adapter) {
        _adapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return _isLongPressDragEnabled;
    }

    public void setIsLongPressDragEnabled(boolean enabled) {
        _isLongPressDragEnabled = enabled;
    }

    public void setSelectedEntry(VaultEntry entry) {
        if (entry == null) {
            _selectedEntry = null;
            return;
        }
        if (!entry.isFavorite()) {
            _selectedEntry = entry;
        }
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // It's not clear when this can happen, but sometimes the ViewHolder
        // that's passed to this function has a position of -1, leading
        // to a crash down the line.
        int position = viewHolder.getAdapterPosition();
        if (position == NO_POSITION) {
            return 0;
        }
        int swipeFlags = 0;
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        EntryAdapter adapter = (EntryAdapter) recyclerView.getAdapter();
        if (adapter.isPositionFooter(position) || adapter.getEntryAt(position) != _selectedEntry || !isLongPressDragEnabled()) {
            dragFlags = 0;
        }
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        if (target.getAdapterPosition() < _adapter.getShownFavoritesCount()) {
            return false;
        }
        _adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        _positionChanged = true;
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        _adapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (_positionChanged) {
            _adapter.onItemDrop(viewHolder.getAdapterPosition());
            _positionChanged = false;
        }
    }
}
