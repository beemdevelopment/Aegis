package me.impy.aegis;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
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

import me.impy.aegis.helpers.ItemTouchHelperAdapter;

public class KeyProfileAdapter extends RecyclerView.Adapter<KeyProfileAdapter.KeyProfileHolder> implements ItemTouchHelperAdapter {
    private final List<KeyProfileHolder> _holders;
    private ArrayList<KeyProfile> _keyProfiles;
    private Handler _uiHandler;
    private static ItemClickListener _itemClickListener;
    private static LongItemClickListener _longItemClickListener;

    public KeyProfileAdapter(ArrayList<KeyProfile> keyProfiles) {
        _keyProfiles = keyProfiles;
        _holders = new ArrayList<>();
        _uiHandler = new Handler();
    }

    @Override
    public void onItemDismiss(int position) {

    }

    private void remove(int position) {
        _keyProfiles.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public void onItemMove(int firstPosition, int secondPosition) {
        Collections.swap(_keyProfiles, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);

        _keyProfiles.get(firstPosition).getEntry().setOrder(secondPosition);
        adjustOrder(secondPosition);
    }

    private void adjustOrder(int startPosition) {
        Comparator<KeyProfile> comparator = new Comparator<KeyProfile>() {
            @Override
            public int compare(KeyProfile keyProfile, KeyProfile t1) {
                return keyProfile.getEntry().getOrder() - t1.getEntry().getOrder();
            }
        };

        for (int i = startPosition; i < _keyProfiles.size(); i++) {
            _keyProfiles.get(i).getEntry().setOrder(i + 1);
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
        holder.setData(_keyProfiles.get(position));
        holder.updateCode();
        _holders.add(holder);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // check if this key profile still exists
                if (_holders.contains(holder)) {
                    holder.updateCode();
                }

                _uiHandler.postDelayed(this, holder._keyProfile.getEntry().getInfo().getPeriod() * 1000);
            }
        };
        _uiHandler.postDelayed(runnable, holder._keyProfile.getEntry().getInfo().getMillisTillNextRotation());
    }

    @Override
    public int getItemCount() {
        return _keyProfiles.size();
    }

    public static class KeyProfileHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView _profileName;
        TextView _profileCode;
        TextView _profileIssuer;
        ImageView _profileDrawable;
        KeyProfile _keyProfile;
        ProgressBar _progressBar;
        View _itemView;

        KeyProfileHolder(final View itemView) {
            super(itemView);
            _itemView = itemView;
            _profileName = (TextView) itemView.findViewById(R.id.profile_name);
            _profileCode = (TextView) itemView.findViewById(R.id.profile_code);
            _profileIssuer = (TextView) itemView.findViewById(R.id.profile_issuer);
            _profileDrawable = (ImageView) itemView.findViewById(R.id.ivTextDrawable);
            _progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        public void setData(KeyProfile profile) {
            _keyProfile = profile;
            _profileName.setText(profile.getEntry().getName());
            _profileCode.setText(profile.getCode());

            // So that we can have text in the designer without showing it to our user
            _profileIssuer.setText("");

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_itemView.getContext());
            if(sharedPreferences.getBoolean("pref_issuer", false))
            {
                _profileIssuer.setText(" - " + profile.getEntry().getInfo().getIssuer());
            }

            _profileDrawable.setImageDrawable(generateTextDrawable(profile));
        }

        public void updateCode() {
            _progressBar.setProgress(1000);
            if (_keyProfile == null) {
                return;
            }
            String otp = _keyProfile.refreshCode();
            _profileCode.setText(otp.substring(0, 3) + " " + otp.substring(3));

            long millisTillRotation = _keyProfile.getEntry().getInfo().getMillisTillNextRotation();
            long period = _keyProfile.getEntry().getInfo().getPeriod() * 1000;
            int currentProgress = 1000 - (int) ((((double) period - millisTillRotation) / period) * 1000);
            ObjectAnimator animation = ObjectAnimator.ofInt(_progressBar, "progress", currentProgress, 0);
            animation.setDuration(millisTillRotation);
            animation.setInterpolator(new LinearInterpolator());
            animation.start();
        }

        private TextDrawable generateTextDrawable(KeyProfile profile) {
            if (_profileName == null)
                return null;

            ColorGenerator generator = ColorGenerator.MATERIAL;
            int profileKeyColor = generator.getColor(profile.getEntry().getName());

            return TextDrawable.builder().buildRound(profile.getEntry().getName().substring(0, 1).toUpperCase(), profileKeyColor);
        }

        @Override
        public void onClick(View view) {
            _itemClickListener.onItemClick(getAdapterPosition(), view);
        }

        @Override
        public boolean onLongClick(View view) {
            _longItemClickListener.onLongItemClick(getAdapterPosition(), view);
            return true;
        }
    }

    public void setOnItemClickListener(ItemClickListener clickListener) {
        KeyProfileAdapter._itemClickListener = clickListener;
    }

    public void setOnLongItemClickListener(LongItemClickListener clickListener) {
        KeyProfileAdapter._longItemClickListener = clickListener;
    }

    public interface ItemClickListener {
        void onItemClick(int position, View v);
    }

    public interface LongItemClickListener {
        void onLongItemClick(int position, View v);
    }
}
