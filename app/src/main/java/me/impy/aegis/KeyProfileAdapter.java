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
import java.util.List;

import me.impy.aegis.helpers.ItemTouchHelperAdapter;

public class KeyProfileAdapter extends RecyclerView.Adapter<KeyProfileAdapter.KeyProfileHolder> implements ItemTouchHelperAdapter {
    private final List<KeyProfileHolder> _holders;
    private ArrayList<KeyProfile> _keyProfiles;
    private Handler _uiHandler;
    private static Listener _listener;

    public KeyProfileAdapter(ArrayList<KeyProfile> keyProfiles, Listener listener) {
        _keyProfiles = keyProfiles;
        _holders = new ArrayList<>();
        _uiHandler = new Handler();
        _listener = listener;
    }

    @Override
    public void onItemDismiss(int position) {

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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_keyprofile, parent, false);
        return new KeyProfileHolder(v);
    }

    @Override
    public void onBindViewHolder(final KeyProfileHolder holder, int position) {
        holder.setData(_keyProfiles.get(position));
        holder.updateCode();
        _holders.add(holder);

        _uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // check if this key profile still exists
                if (_holders.contains(holder)) {
                    holder.updateCode();
                }

                _uiHandler.postDelayed(this, holder._keyProfile.getEntry().getInfo().getPeriod() * 1000);
            }
        }, holder._keyProfile.getEntry().getInfo().getMillisTillNextRotation());
    }

    @Override
    public int getItemCount() {
        return _keyProfiles.size();
    }

    public class KeyProfileHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private TextView _profileName;
        private TextView _profileCode;
        private TextView _profileIssuer;
        private ImageView _profileDrawable;
        private KeyProfile _keyProfile;
        private ProgressBar _progressBar;
        private View _itemView;

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
            if (sharedPreferences.getBoolean("pref_issuer", false)) {
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
            if (_profileName == null) {
                return null;
            }

            ColorGenerator generator = ColorGenerator.MATERIAL;
            int profileKeyColor = generator.getColor(profile.getEntry().getName());

            return TextDrawable.builder().buildRound(profile.getEntry().getName().substring(0, 1).toUpperCase(), profileKeyColor);
        }

        @Override
        public void onClick(View view) {
            _listener.onKeyProfileClick(_keyProfiles.get(getAdapterPosition()));
        }

        @Override
        public boolean onLongClick(View view) {
            return _listener.onLongKeyProfileClick(_keyProfiles.get(getAdapterPosition()));
        }
    }

    public interface Listener {
        void onKeyProfileClick(KeyProfile profile);
        boolean onLongKeyProfileClick(KeyProfile profile);
        void onKeyProfileMove(KeyProfile profile1, KeyProfile profile2);
    }
}
