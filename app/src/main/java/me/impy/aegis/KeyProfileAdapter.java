package me.impy.aegis;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.impy.aegis.crypto.OTP;

public class KeyProfileAdapter extends RecyclerView.Adapter<KeyProfileAdapter.KeyProfileHolder> {
    private ArrayList<KeyProfile> mKeyProfiles;
    private final List<KeyProfileHolder> lstHolders;
    private Timer timer;
    private Handler uiHandler;

    public static class KeyProfileHolder extends RecyclerView.ViewHolder {
        TextView profileName;
        TextView profileCode;
        KeyProfile keyProfile;

        KeyProfileHolder(View itemView) {
            super(itemView);
            profileName = (TextView) itemView.findViewById(R.id.profile_name);
            profileCode = (TextView) itemView.findViewById(R.id.profile_code);
        }

        public void setData(KeyProfile profile) {
            this.keyProfile = profile;
            profileName.setText(profile.Name);
            profileCode.setText(profile.Code);
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
            profileCode.setText(otp.substring(0, 3) + " " + otp.substring(3));
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public KeyProfileAdapter(ArrayList<KeyProfile> keyProfiles) {
        mKeyProfiles = keyProfiles;
        lstHolders = new ArrayList<>();
        timer = new Timer();
        uiHandler = new Handler();
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
    public void onBindViewHolder(final KeyProfileHolder holder, int position) {
        holder.setData(mKeyProfiles.get(position));
        holder.updateCode();
        lstHolders.add(holder);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // check if this key profile still exists
                        if (lstHolders.contains(holder)) {
                            holder.updateCode();
                        }
                    }
                });
            }
        }, holder.keyProfile.KeyInfo.getMillisTillNextRotation(), holder.keyProfile.KeyInfo.getPeriod() * 1000);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mKeyProfiles.size();
    }
}