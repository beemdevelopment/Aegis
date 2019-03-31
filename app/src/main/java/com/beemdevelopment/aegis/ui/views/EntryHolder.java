package com.beemdevelopment.aegis.ui.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.beemdevelopment.aegis.helpers.TextDrawableHelper;
import com.beemdevelopment.aegis.helpers.UiRefresher;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.db.DatabaseEntry;
import android.os.Handler;

public class EntryHolder extends RecyclerView.ViewHolder {
    private TextView _profileName;
    private TextView _profileCode;
    private TextView _profileIssuer;
    private ImageView _profileDrawable;
    private DatabaseEntry _entry;
    private ImageView _buttonRefresh;

    private View _entryDivider;

    private View _currentView;

    private boolean _hidden;
    private int _tapToRevealTime;

    private PeriodProgressBar _progressBar;

    private UiRefresher _refresher;
    private Handler _hiddenHandler;

    public EntryHolder(final View view) {
        super(view);
        _currentView = view;

        _profileName = view.findViewById(R.id.profile_account_name);
        _profileCode = view.findViewById(R.id.profile_code);
        _profileIssuer = view.findViewById(R.id.profile_issuer);
        _profileDrawable = view.findViewById(R.id.ivTextDrawable);
        _buttonRefresh = view.findViewById(R.id.buttonRefresh);

        _entryDivider = view.findViewById(R.id.entryDivider);

        _progressBar = view.findViewById(R.id.progressBar);
        int primaryColorId = view.getContext().getResources().getColor(R.color.colorPrimary);
        _progressBar.getProgressDrawable().setColorFilter(primaryColorId, PorterDuff.Mode.SRC_IN);

        _refresher = new UiRefresher(new UiRefresher.Listener() {
            @Override
            public void onRefresh() {
                if (!isCodeHidden()) {
                    refreshCode();
                }

                _progressBar.refresh();
            }

            @Override
            public long getMillisTillNextRefresh() {
                return ((TotpInfo)_entry.getInfo()).getMillisTillNextRotation();
            }
        });

        _hiddenHandler = new Handler();
    }

    public void setData(DatabaseEntry entry, boolean showAccountName, boolean showProgress, boolean hidden) {
        _entry = entry;
        _hidden = hidden;

        // only show the progress bar if there is no uniform period and the entry type is TotpInfo
        _progressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        if (showProgress) {
            _progressBar.setPeriod(((TotpInfo)entry.getInfo()).getPeriod());

            if (_entryDivider != null) {
                _entryDivider.setVisibility(View.GONE);
            }
        }

        // only show the button if this entry is of type HotpInfo
        _buttonRefresh.setVisibility(entry.getInfo() instanceof HotpInfo ? View.VISIBLE : View.GONE);

        _profileIssuer.setText(entry.getIssuer());
        _profileName.setText("");
        if (showAccountName) {
            _profileName.setText(" - " + entry.getName());
        }

        if (_entry.hasIcon()) {
            byte[] imageBytes = entry.getIcon();
            Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            _profileDrawable.setImageBitmap(image);
        } else {
            TextDrawable drawable = TextDrawableHelper.generate(entry.getIssuer(), entry.getName(), _profileDrawable);
            _profileDrawable.setImageDrawable(drawable);
        }

        // cancel any scheduled hideCode calls
        _hiddenHandler.removeCallbacksAndMessages(null);

        if (_hidden) {
            hideCode();
        } else {
            refreshCode();
        }
    }

    public void setTapToRevealTime(int number) {
        _tapToRevealTime = number;
    }

    public void setOnRefreshClickListener(View.OnClickListener listener) {
        _buttonRefresh.setOnClickListener(listener);
    }

    public void startRefreshLoop() {
        _refresher.start();
    }

    public void stopRefreshLoop() {
        _refresher.stop();
    }

    public void refreshCode() {
        if (!isCodeHidden()) {
            updateCode();
        }
    }

    private void updateCode() {
        OtpInfo info = _entry.getInfo();

        String text;
        if (info instanceof SteamInfo) {
            text = info.getOtp();
        } else {
            String otp = info.getOtp();
            text = otp.substring(0, (otp.length() / 2)
                    + (otp.length() % 2)) + " "
                    + otp.substring(otp.length() / 2);
        }

        _profileCode.setText(text);
    }

    public void revealCode() {
        updateCode();
        _hiddenHandler.postDelayed(this::hideCode, _tapToRevealTime * 1000);
        _hidden = false;
    }

    private void hideCode() {
        _profileCode.setText(_currentView.getContext().getResources().getString(R.string.tap_to_reveal));
        _hidden = true;
    }

    public boolean isCodeHidden() {
        return _hidden;
    }
}
