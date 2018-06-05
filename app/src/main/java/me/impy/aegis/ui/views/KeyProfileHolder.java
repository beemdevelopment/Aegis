package me.impy.aegis.ui.views;

import android.graphics.PorterDuff;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;

import me.impy.aegis.R;

public class KeyProfileHolder extends RecyclerView.ViewHolder {
    private TextView _profileName;
    private TextView _profileCode;
    private TextView _profileIssuer;
    private ImageView _profileDrawable;
    private KeyProfile _profile;

    private PeriodProgressBar _progressBar;

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

    public void setData(KeyProfile profile, boolean showIssuer, boolean showProgress) {
        _profile = profile;

        _progressBar.setVisibility(showProgress ? View.VISIBLE : View.INVISIBLE);
        if (showProgress) {
            _progressBar.setPeriod(profile.getEntry().getInfo().getPeriod());
        }

        _profileName.setText(profile.getEntry().getName());
        _profileIssuer.setText("");
        if (showIssuer) {
            _profileIssuer.setText(" - " + profile.getEntry().getInfo().getIssuer());
        }

        TextDrawable drawable = profile.getDrawable();
        _profileDrawable.setImageDrawable(drawable);

        refreshCode();
    }

    public void startRefreshLoop() {
        if (_running) {
            return;
        }
        _running = true;

        refresh();
        _uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (_running) {
                    refresh();
                    _uiHandler.postDelayed(this, _profile.getEntry().getInfo().getMillisTillNextRotation());
                }
            }
        }, _profile.getEntry().getInfo().getMillisTillNextRotation());
    }

    public void stopRefreshLoop() {
        _running = false;
    }

    private void refresh() {
         refreshCode();
        _progressBar.refresh();
    }

    private void refreshCode() {
        String otp = _profile.refreshCode();
        _profileCode.setText(otp.substring(0, otp.length() / 2) + " " + otp.substring(otp.length() / 2));
    }
}
