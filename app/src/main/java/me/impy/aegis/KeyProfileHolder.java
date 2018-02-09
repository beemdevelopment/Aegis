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

public class KeyProfileHolder extends RecyclerView.ViewHolder implements KeyProfile.Listener {
    private TextView _profileName;
    private TextView _profileCode;
    private TextView _profileIssuer;
    private ImageView _profileDrawable;
    private KeyProfile _profile;
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
        if (profile == null) {
            _profile.setListener(null);
            _profile = null;
            _running = false;
            return;
        }
        _profile = profile;

        profile.setListener(this);
        _profileName.setText(profile.getEntry().getName());
        _profileCode.setText(profile.getCode());
        _profileIssuer.setText("");
        if (showIssuer) {
            _profileIssuer.setText(" - " + profile.getEntry().getInfo().getIssuer());
        }

        TextDrawable drawable = profile.getDrawable();
        _profileDrawable.setImageDrawable(drawable);
    }

    public void startRefreshLoop() {
        if (_running) {
            return;
        }
        _running = true;

        _profile.refreshCode();
        _uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (_running) {
                    _profile.refreshCode();
                    _uiHandler.postDelayed(this, _profile.getEntry().getInfo().getMillisTillNextRotation());
                }
            }
        }, _profile.getEntry().getInfo().getMillisTillNextRotation());
    }

    @Override
    public void onRefreshCode(String otp) {
        // reset the progress bar
        int maxProgress = _progressBar.getMax();
        _progressBar.setProgress(maxProgress);
        _profileCode.setText(otp.substring(0, otp.length() / 2) + " " + otp.substring(otp.length() / 2));

        // calculate the progress the bar should start at
        long millisTillRotation = _profile.getEntry().getInfo().getMillisTillNextRotation();
        long period = _profile.getEntry().getInfo().getPeriod() * maxProgress;
        int currentProgress = maxProgress - (int) ((((double) period - millisTillRotation) / period) * maxProgress);

        // start progress animation
        ObjectAnimator animation = ObjectAnimator.ofInt(_progressBar, "progress", currentProgress, 0);
        animation.setDuration(millisTillRotation);
        animation.setInterpolator(new LinearInterpolator());
        animation.start();
    }
}
