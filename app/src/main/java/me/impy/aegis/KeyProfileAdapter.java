package me.impy.aegis;

import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import github.nisrulz.recyclerviewhelper.RVHAdapter;
import me.impy.aegis.crypto.OTP;
import me.impy.aegis.helpers.ItemClickListener;

public class KeyProfileAdapter extends RecyclerView.Adapter<KeyProfileAdapter.KeyProfileHolder> implements RVHAdapter {
    private ArrayList<KeyProfile> mKeyProfiles;
    private final List<KeyProfileHolder> lstHolders;
    private final ItemClickListener itemClickListener;


    private Handler mHandler = new Handler();
    private Runnable updateRemainingTimeRunnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (lstHolders) {
                        for (KeyProfileHolder holder : lstHolders) {
                            holder.updateCode();
                        }
                    }
        }
    };

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        swap(fromPosition, toPosition);
        return false;
    }

    @Override
    public void onItemDismiss(int position, int direction) {
        remove(position);
    }


    // Helper functions you might want to implement to make changes in the list as an event is fired
    private void remove(int position) {
        mKeyProfiles.remove(position);
        notifyItemRemoved(position);
    }

    private void swap(int firstPosition, int secondPosition) {
        Collections.swap(mKeyProfiles, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);
    }

    public static class KeyProfileHolder extends RecyclerView.ViewHolder {
        TextView profileName;
        TextView profileCode;
        ImageView profileDrawable;
        KeyProfile keyProfile;

        KeyProfileHolder(final View itemView) {
            super(itemView);
            profileName = (TextView) itemView.findViewById(R.id.profile_name);
            profileCode = (TextView) itemView.findViewById(R.id.profile_code);
            profileDrawable = (ImageView) itemView.findViewById(R.id.ivTextDrawable);
        }

        public void setData(KeyProfile profile) {
            this.keyProfile = profile;
            profileName.setText(profile.Name);
            profileCode.setText(profile.Code);
            profileDrawable.setImageDrawable(generateTextDrawable(profile));
        }

        public void updateCode() {
            if (this.keyProfile == null) {
                return;
            }
            String otp = "";
            try {
                otp = OTP.generateOTP(this.keyProfile.KeyInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.keyProfile.Code = otp;
            profileCode.setText(otp.substring(0, 3) + " " + otp.substring(3));
        }

        private TextDrawable generateTextDrawable(KeyProfile profile)
        {
            if(profileName == null)
                return null;

            ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
            // generate color based on a key (same key returns the same color), useful for list/grid views
            int profileKeyColor = generator.getColor(profile.Name);

            TextDrawable newDrawable = TextDrawable.builder().buildRound(profile.Name.substring(0, 1), profileKeyColor);
            return newDrawable;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public KeyProfileAdapter(ArrayList<KeyProfile> keyProfiles, @NonNull ItemClickListener listener) {
        mKeyProfiles = keyProfiles;
        lstHolders = new ArrayList<>();

        this.itemClickListener = listener;
        startUpdateTimer();
    }

    private void startUpdateTimer() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(updateRemainingTimeRunnable);
            }
            //TODO: Replace delay with seconds that are left
        }, 0, 5000);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public KeyProfileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_keyprofile, parent, false);
        // set the view's size, margins, paddings and layout parameters

        final KeyProfileHolder vh = new KeyProfileHolder(v);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemClickListener.onItemClicked(vh.keyProfile, view);
            }
        });
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(KeyProfileHolder holder, int position) {
        holder.setData(mKeyProfiles.get(position));
        synchronized (lstHolders) {
            lstHolders.add(holder);
        }
        holder.updateCode();
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mKeyProfiles.size();
    }
}