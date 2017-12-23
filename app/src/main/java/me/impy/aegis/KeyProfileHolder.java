package me.impy.aegis;

import android.animation.ObjectAnimator;
import android.os.Handler;
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

    private Handler _uiHandler;
    private boolean _running = false;

    KeyProfileHolder(final View view) {
        super(view);
        _profileName = view.findViewById(R.id.profile_name);
        _profileCode = view.findViewById(R.id.profile_code);
        _profileIssuer = view.findViewById(R.id.profile_issuer);
        _profileDrawable = view.findViewById(R.id.ivTextDrawable);
        _progressBar = view.findViewById(R.id.progressBar);
        _uiHandler = new Handler();
    }

    public void setData(KeyProfile profile, boolean showIssuer) {
        if ((_keyProfile = profile) == null) {
            _running = false;
            return;
        }

        _profileName.setText(profile.getEntry().getName());
        _profileCode.setText(profile.getCode());
        _profileIssuer.setText("");
        if (showIssuer) {
            _profileIssuer.setText(" - " + profile.getEntry().getInfo().getIssuer());
        }

        _profileDrawable.setImageDrawable(generateTextDrawable(profile));
    }

    public void startUpdateLoop() {
        if (_running) {
            return;
        }
        _running = true;

        updateCode();
        _uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (_running) {
                    updateCode();
                    _uiHandler.postDelayed(this, _keyProfile.getEntry().getInfo().getMillisTillNextRotation());
                }
            }
        }, _keyProfile.getEntry().getInfo().getMillisTillNextRotation());
    }

    private boolean updateCode() {
        // reset the progress bar
        int maxProgress = _progressBar.getMax();
        _progressBar.setProgress(maxProgress);

        // refresh the code
        String otp = _keyProfile.refreshCode();
        _profileCode.setText(otp.substring(0, 3) + " " + otp.substring(3));

        // calculate the progress the bar should start at
        long millisTillRotation = _keyProfile.getEntry().getInfo().getMillisTillNextRotation();
        long period = _keyProfile.getEntry().getInfo().getPeriod() * maxProgress;
        int currentProgress = maxProgress - (int) ((((double) period - millisTillRotation) / period) * maxProgress);

        // start progress animation
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
