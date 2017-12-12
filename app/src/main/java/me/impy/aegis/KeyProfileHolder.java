package me.impy.aegis;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

public class KeyProfileHolder extends RecyclerView.ViewHolder {
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
        _profileName = itemView.findViewById(R.id.profile_name);
        _profileCode = itemView.findViewById(R.id.profile_code);
        _profileIssuer = itemView.findViewById(R.id.profile_issuer);
        _profileDrawable = itemView.findViewById(R.id.ivTextDrawable);
        _progressBar = itemView.findViewById(R.id.progressBar);
    }

    public void setData(KeyProfile profile) {
        if ((_keyProfile = profile) == null) {
            return;
        }

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

    public boolean updateCode() {
        _progressBar.setProgress(1000);
        if (_keyProfile == null) {
            return false;
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
        return true;
    }

    private TextDrawable generateTextDrawable(KeyProfile profile) {
        if (_profileName == null) {
            return null;
        }

        ColorGenerator generator = ColorGenerator.MATERIAL;
        int profileKeyColor = generator.getColor(profile.getEntry().getName());

        return TextDrawable.builder().buildRound(profile.getEntry().getName().substring(0, 1).toUpperCase(), profileKeyColor);
    }
}
