package com.beemdevelopment.aegis.ui.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;

import java.util.ArrayList;

public class GroupAdapter extends RecyclerView.Adapter<GroupHolder> {
    private GroupAdapter.Listener _listener;
    private ArrayList<String> _groups;

    public GroupAdapter(GroupAdapter.Listener listener) {
        _listener = listener;
        _groups = new ArrayList<>();
    }

    public void addGroup(String group) {
        _groups.add(group);

        int position = getItemCount() - 1;
        if (position == 0) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(position);
        }
    }

    public void removeGroup(String group) {
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
        holder.setOnDeleteClickListener(v -> {
            int position12 = holder.getAdapterPosition();
            _listener.onRemoveGroup(_groups.get(position12));
        });
    }

    @Override
    public int getItemCount() {
        return _groups.size();
    }

    public interface Listener {
        void onRemoveGroup(String group);
    }
}
