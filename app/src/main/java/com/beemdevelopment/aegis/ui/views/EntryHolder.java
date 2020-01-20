package com.beemdevelopment.aegis.ui.views;

import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.helpers.TextDrawableHelper;
import com.beemdevelopment.aegis.helpers.ThemeHelper;
import com.beemdevelopment.aegis.helpers.UiRefresher;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class EntryHolder extends RecyclerView.ViewHolder {
    private static final float DEFAULT_ALPHA = 1.0f;
    private static final float DIMMED_ALPHA = 0.2f;

    private TextView _profileName;
    private TextView _profileCode;
    private TextView _profileIssuer;
    private ImageView _profileDrawable;
    private VaultEntry _entry;
    private ImageView _buttonRefresh;

    private boolean _hidden;

    private PeriodProgressBar _progressBar;
    private View _view;

    private UiRefresher _refresher;

    public EntryHolder(final View view) {
        super(view);

        _view = view.findViewById(R.id.rlCardEntry);

        _profileName = view.findViewById(R.id.profile_account_name);
        _profileCode = view.findViewById(R.id.profile_code);
        _profileIssuer = view.findViewById(R.id.profile_issuer);
        _profileDrawable = view.findViewById(R.id.ivTextDrawable);
        _buttonRefresh = view.findViewById(R.id.buttonRefresh);

        _progressBar = view.findViewById(R.id.progressBar);
        int primaryColorId = view.getContext().getResources().getColor(R.color.colorPrimary);
        _progressBar.getProgressDrawable().setColorFilter(primaryColorId, PorterDuff.Mode.SRC_IN);
        _view.setBackground(_view.getContext().getResources().getDrawable(R.color.card_background));

        _refresher = new UiRefresher(new UiRefresher.Listener() {
            @Override
            public void onRefresh() {
                if (!_hidden) {
                    refreshCode();
                }

                _progressBar.refresh();
            }

            @Override
            public long getMillisTillNextRefresh() {
                return ((TotpInfo)_entry.getInfo()).getMillisTillNextRotation();
            }
        });
    }

    public void setData(VaultEntry entry, boolean showAccountName, boolean showProgress, boolean hidden, boolean dimmed) {
        _entry = entry;
        _hidden = hidden;

        // only show the progress bar if there is no uniform period and the entry type is TotpInfo
        setShowProgress(showProgress);

        // only show the button if this entry is of type HotpInfo
        _buttonRefresh.setVisibility(entry.getInfo() instanceof HotpInfo ? View.VISIBLE : View.GONE);

        _profileIssuer.setText(entry.getIssuer());
        _profileName.setText("");
        if (showAccountName) {
            _profileName.setText(" - " + entry.getName());
        }

        if (_hidden) {
            hideCode();
        } else {
            refreshCode();
        }

        itemView.setAlpha(dimmed ? DIMMED_ALPHA : DEFAULT_ALPHA);
    }

    public VaultEntry getEntry() {
        return _entry;
    }

    public void loadIcon(Fragment fragment) {
        if (_entry.hasIcon()) {
            Glide.with(fragment)
                .asDrawable()
                .load(_entry)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(false)
                .into(_profileDrawable);
        } else {
            TextDrawable drawable = TextDrawableHelper.generate(_entry.getIssuer(), _entry.getName(), _profileDrawable);
            _profileDrawable.setImageDrawable(drawable);
        }
    }

    public ImageView getIconView() {
        return _profileDrawable;
    }

    public void setOnRefreshClickListener(View.OnClickListener listener) {
        _buttonRefresh.setOnClickListener(listener);
    }

    public void setShowProgress(boolean showProgress) {
        if (_entry.getInfo() instanceof HotpInfo) {
            showProgress = false;
        }

        _progressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        if (showProgress) {
            _progressBar.setPeriod(((TotpInfo) _entry.getInfo()).getPeriod());
            startRefreshLoop();
        } else {
            stopRefreshLoop();
        }
    }

    public void setFocused(boolean focused) {
        if (focused) {
            _view.setBackgroundColor(ThemeHelper.getThemeColor(R.attr.cardBackgroundFocused, _view.getContext().getTheme()));
        } else {
            _view.setBackgroundColor(ThemeHelper.getThemeColor(R.attr.cardBackground, _view.getContext().getTheme()));
        }
        _view.setSelected(focused);
    }

    public void destroy() {
        _refresher.destroy();
    }

    public void startRefreshLoop() {
        _refresher.start();
    }

    public void stopRefreshLoop() {
        _refresher.stop();
    }

    public void refreshCode() {
        if (!_hidden) {
            updateCode();
        }
    }

    private void updateCode() {
        OtpInfo info = _entry.getInfo();

        String otp = info.getOtp();
        if (!(info instanceof SteamInfo)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < otp.length(); i++) {
                if (i != 0 && i % 3 == 0) {
                    sb.append(" ");
                }
                sb.append(otp.charAt(i));
            }
            otp = sb.toString();
        }

        _profileCode.setText(otp);
    }

    public void revealCode() {
        updateCode();
        _hidden = false;
    }

    public void hideCode() {
        _profileCode.setText(R.string.tap_to_reveal);
        _hidden = true;
    }

    public void dim() {
        animateAlphaTo(DIMMED_ALPHA);
    }

    public void highlight() {
        animateAlphaTo(DEFAULT_ALPHA);
    }

    private void animateAlphaTo(float alpha) {
        itemView.animate().alpha(alpha).setDuration(200).start();
    }
}
