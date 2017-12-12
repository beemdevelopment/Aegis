package me.impy.aegis;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.impy.aegis.helpers.ItemTouchHelperAdapter;

public class KeyProfileAdapter extends RecyclerView.Adapter<KeyProfileHolder> implements ItemTouchHelperAdapter {
    private ArrayList<KeyProfile> _keyProfiles;
    private Handler _uiHandler;
    private static Listener _listener;

    public KeyProfileAdapter(Listener listener) {
        _keyProfiles = new ArrayList<>();
        _uiHandler = new Handler();
        _listener = listener;
    }

    public void addKey(KeyProfile profile) {
        _keyProfiles.add(profile);
        notifyDataSetChanged();
    }

    public void addKeys(List<KeyProfile> profiles) {
        _keyProfiles.addAll(profiles);
        notifyDataSetChanged();
    }

    public void removeKey(KeyProfile profile) {
        int position = _keyProfiles.indexOf(profile);
        _keyProfiles.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public void onItemDismiss(int position) {

    }

    @Override
    public void onItemDrop(int position) {
        _listener.onKeyProfileDrop(_keyProfiles.get(position));
    }

    @Override
    public void onItemMove(int firstPosition, int secondPosition) {
        // notify the database first
        _listener.onKeyProfileMove(_keyProfiles.get(firstPosition), _keyProfiles.get(secondPosition));

        // update our side of things
        Collections.swap(_keyProfiles, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);
    }

    @Override
    public KeyProfileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_keyprofile, parent, false);
        return new KeyProfileHolder(view);
    }

    @Override
    public void onViewRecycled(KeyProfileHolder holder) {
        holder.setData(null);
        super.onViewRecycled(holder);
    }

    @Override
    public void onBindViewHolder(final KeyProfileHolder holder, int position) {
        final KeyProfile profile = _keyProfiles.get(position);
        holder.setData(profile);
        holder.updateCode();
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                _listener.onKeyProfileClick(_keyProfiles.get(position));
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int position = holder.getAdapterPosition();
                return _listener.onLongKeyProfileClick(_keyProfiles.get(position));
            }
        });

        _uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (holder.updateCode()) {
                    _uiHandler.postDelayed(this, profile.getEntry().getInfo().getPeriod() * 1000);
                }
            }
        }, profile.getEntry().getInfo().getMillisTillNextRotation());
    }

    @Override
    public int getItemCount() {
        return _keyProfiles.size();
    }

    public interface Listener {
        void onKeyProfileClick(KeyProfile profile);
        boolean onLongKeyProfileClick(KeyProfile profile);
        void onKeyProfileMove(KeyProfile profile1, KeyProfile profile2);
        void onKeyProfileDrop(KeyProfile profile);
    }
}
