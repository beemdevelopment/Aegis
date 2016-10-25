package me.impy.aegis;

import android.animation.ObjectAnimator;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.impy.aegis.crypto.OTP;
import me.impy.aegis.db.Database;
import me.impy.aegis.helpers.ItemClickListener;
import me.impy.aegis.helpers.ItemTouchHelperAdapter;

public class KeyProfileAdapter extends RecyclerView.Adapter<KeyProfileAdapter.KeyProfileHolder> implements ItemTouchHelperAdapter {
    private final List<KeyProfileHolder> lstHolders;
    private ArrayList<KeyProfile> mKeyProfiles;
    private Handler uiHandler;
    private static ItemClickListener itemClickListener;

    public KeyProfileAdapter(ArrayList<KeyProfile> keyProfiles) {
        mKeyProfiles = keyProfiles;
        lstHolders = new ArrayList<>();
        uiHandler = new Handler();
    }

    @Override
    public void onItemDismiss(int position) {
        return;
    }

    private void remove(int position) {
        mKeyProfiles.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public void onItemMove(int firstPosition, int secondPosition) {
        Collections.swap(mKeyProfiles, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);

        mKeyProfiles.get(firstPosition).Order = secondPosition;
        adjustOrder(secondPosition);
    }

    private void adjustOrder(int startPosition) {
        Comparator<KeyProfile> comparator = new Comparator<KeyProfile>() {
            @Override
            public int compare(KeyProfile keyProfile, KeyProfile t1) {
                return keyProfile.Order - t1.Order;
            }
        };

        for (int i = startPosition; i < mKeyProfiles.size(); i++) {
            mKeyProfiles.get(i).Order = i + 1;
        }
    }

    @Override
    public KeyProfileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_keyprofile, parent, false);
        KeyProfileHolder vh = new KeyProfileHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final KeyProfileHolder holder, int position) {
        holder.setData(mKeyProfiles.get(position));
        holder.updateCode();
        lstHolders.add(holder);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // check if this key profile still exists
                if (lstHolders.contains(holder)) {
                    holder.updateCode();
                }

                uiHandler.postDelayed(this, holder.keyProfile.Info.getPeriod() * 1000);
            }
        };
        uiHandler.postDelayed(runnable, holder.keyProfile.Info.getMillisTillNextRotation());
    }

    @Override
    public int getItemCount() {
        return mKeyProfiles.size();
    }

    public static class KeyProfileHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView profileName;
        TextView profileCode;
        ImageView profileDrawable;
        KeyProfile keyProfile;
        ProgressBar progressBar;

        KeyProfileHolder(final View itemView) {
            super(itemView);
            profileName = (TextView) itemView.findViewById(R.id.profile_name);
            profileCode = (TextView) itemView.findViewById(R.id.profile_code);
            profileDrawable = (ImageView) itemView.findViewById(R.id.ivTextDrawable);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);

            itemView.setOnClickListener(this);
        }

        public void setData(KeyProfile profile) {
            this.keyProfile = profile;
            profileName.setText(profile.Name);
            profileCode.setText(profile.Code);
            profileDrawable.setImageDrawable(generateTextDrawable(profile));
        }

        public void updateCode() {
            progressBar.setProgress(1000);
            if (this.keyProfile == null) {
                return;
            }
            String otp = "";
            try {
                otp = OTP.generateOTP(this.keyProfile.Info);
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.keyProfile.Code = otp;
            profileCode.setText(otp.substring(0, 3) + " " + otp.substring(3));

            long millisTillRotation = keyProfile.Info.getMillisTillNextRotation();
            long period = keyProfile.Info.getPeriod() * 1000;
            int currentProgress = 1000 - (int) ((((double) period - millisTillRotation) / period) * 1000);
            ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", currentProgress, 0);
            animation.setDuration(millisTillRotation);
            animation.setInterpolator(new LinearInterpolator());
            animation.start();
        }

        private TextDrawable generateTextDrawable(KeyProfile profile) {
            if (profileName == null)
                return null;

            ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
            // generate color based on a key (same key returns the same color), useful for list/grid views
            int profileKeyColor = generator.getColor(profile.Name);

            TextDrawable newDrawable = TextDrawable.builder().buildRound(profile.Name.substring(0, 1).toUpperCase(), profileKeyColor);
            return newDrawable;
        }

        @Override
        public void onClick(View view) {
            itemClickListener.onItemClick(getAdapterPosition(), view);
        }
    }

    public void setOnItemClickListener(ItemClickListener clickListener) {
        KeyProfileAdapter.itemClickListener = clickListener;
    }

    public interface ItemClickListener
    {
        void onItemClick(int position, View v);
    }
}