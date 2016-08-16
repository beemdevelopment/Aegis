package me.impy.aegis;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.security.Key;
import java.util.ArrayList;

public class KeyProfileAdapter extends RecyclerView.Adapter<KeyProfileAdapter.KeyProfileHolder>  {
    private ArrayList<KeyProfile> mKeyProfiles;

    public static class KeyProfileHolder extends RecyclerView.ViewHolder {
        TextView profileName;
        TextView profileCode;

        KeyProfileHolder(View itemView) {
            super(itemView);
            profileName = (TextView) itemView.findViewById(R.id.profile_name);
            profileCode = (TextView) itemView.findViewById(R.id.profile_code);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public KeyProfileAdapter(ArrayList<KeyProfile> keyProfiles) {
        mKeyProfiles = keyProfiles;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public KeyProfileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_keyprofile, parent, false);
        // set the view's size, margins, paddings and layout parameters

        KeyProfileHolder vh = new KeyProfileHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(KeyProfileHolder holder, int position) {
        holder.profileName.setText(mKeyProfiles.get(position).Name);
        holder.profileCode.setText(mKeyProfiles.get(position).Code);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mKeyProfiles.size();
    }
}