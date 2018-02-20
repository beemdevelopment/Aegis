package me.impy.aegis.ui.views;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;

import me.impy.aegis.R;

public class KeyProfileHolder extends RecyclerView.ViewHolder {
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

        int primaryColorId = view.getContext().getResources().getColor(R.color.colorPrimary);
        _progressBar.getProgressDrawable().setColorFilter(primaryColorId, PorterDuff.Mode.SRC_IN);
    }

    public void setData(KeyProfile profile, boolean showIssuer) {
        if (profile == null) {
            _profile = null;
            _running = false;
            return;
        }
        _profile = profile;

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

        refreshCode();
        _uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (_running) {
                    refreshCode();
                    _uiHandler.postDelayed(this, _profile.getEntry().getInfo().getMillisTillNextRotation());
                }
            }
        }, _profile.getEntry().getInfo().getMillisTillNextRotation());
    }

    public void refreshCode() {
        String otp = _profile.refreshCode();

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
