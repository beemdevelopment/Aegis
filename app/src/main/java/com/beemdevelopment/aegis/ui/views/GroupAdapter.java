package com.beemdevelopment.aegis.ui.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.ItemTouchHelperAdapter;
import com.beemdevelopment.aegis.util.CollectionUtils;
import com.beemdevelopment.aegis.vault.VaultGroup;

import java.util.ArrayList;
import java.util.UUID;

public class GroupAdapter extends RecyclerView.Adapter<GroupHolder> implements ItemTouchHelperAdapter {
    private GroupAdapter.Listener _listener;
    private ArrayList<VaultGroup> _groups;

    public GroupAdapter(GroupAdapter.Listener listener) {
        _listener = listener;
        _groups = new ArrayList<>();
    }

    public void addGroup(VaultGroup group) {
        _groups.add(group);

        int position = getItemCount() - 1;
        if (position == 0) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(position);
        }
    }

    public ArrayList<VaultGroup> getGroups() {
        return _groups;
    }

    public void replaceGroup(UUID uuid, VaultGroup newGroup) {
        VaultGroup oldGroup = getGroupByUUID(uuid);
        int position = _groups.indexOf(oldGroup);
        _groups.set(position, newGroup);
        notifyItemChanged(position);
    }

    public void removeGroup(VaultGroup group) {
        int position = _groups.indexOf(group);
        _groups.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public GroupHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_group, parent, false);
        return new GroupHolder(view);
    }

    @Override
    public void onBindViewHolder(GroupHolder holder, int position) {
        holder.setData(_groups.get(position));
        holder.setOnEditClickListener(v -> {
            int position12 = holder.getAdapterPosition();
            _listener.onEditGroup(_groups.get(position12));
        });
        holder.setOnDeleteClickListener(v -> {
            int position12 = holder.getAdapterPosition();
            _listener.onRemoveGroup(_groups.get(position12));
        });
    }

    @Override
    public void onItemMove(int firstPosition, int secondPosition) {
        CollectionUtils.move(_groups, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);
    }

    @Override
    public void onItemDismiss(int position) { }

    @Override
    public void onItemDrop(int position) { }

    private VaultGroup getGroupByUUID(UUID uuid) {
        for (VaultGroup group : _groups) {
            if (group.getUUID().equals(uuid)) {
                return group;
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return _groups.size();
    }

    public interface Listener {
        void onEditGroup(VaultGroup group);
        void onRemoveGroup(VaultGroup group);
    }
}
