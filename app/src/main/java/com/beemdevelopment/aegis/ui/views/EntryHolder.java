package com.beemdevelopment.aegis.ui.views;

import android.graphics.PorterDuff;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.TextDrawableHelper;
import com.beemdevelopment.aegis.helpers.ThemeHelper;
import com.beemdevelopment.aegis.helpers.UiRefresher;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class EntryHolder extends RecyclerView.ViewHolder {
    private static final float DEFAULT_ALPHA = 1.0f;
    private static final float DIMMED_ALPHA = 0.2f;

    private TextView _profileName;
    private TextView _profileCode;
    private TextView _profileIssuer;
    private TextView _profileCopied;
    private ImageView _profileDrawable;
    private VaultEntry _entry;
    private ImageView _buttonRefresh;
    private RelativeLayout _description;
  
    private final ImageView _selected;
    private final Handler _selectedHandler;

    private int _codeGroupSize = 6;

    private boolean _hidden;

    private TotpProgressBar _progressBar;
    private View _view;

    private UiRefresher _refresher;
    private Handler _animationHandler;

    private Animation _scaleIn;
    private Animation _scaleOut;

    public EntryHolder(final View view) {
        super(view);

        _view = view.findViewById(R.id.rlCardEntry);

        _profileName = view.findViewById(R.id.profile_account_name);
        _profileCode = view.findViewById(R.id.profile_code);
        _profileIssuer = view.findViewById(R.id.profile_issuer);
        _profileCopied = view.findViewById(R.id.profile_copied);
        _description = view.findViewById(R.id.description);
        _profileDrawable = view.findViewById(R.id.ivTextDrawable);
        _buttonRefresh = view.findViewById(R.id.buttonRefresh);
        _selected = view.findViewById(R.id.ivSelected);
        _selectedHandler = new Handler();
        _animationHandler = new Handler();

        _progressBar = view.findViewById(R.id.progressBar);
        int primaryColorId = view.getContext().getResources().getColor(R.color.colorPrimary);
        _progressBar.getProgressDrawable().setColorFilter(primaryColorId, PorterDuff.Mode.SRC_IN);
        _view.setBackground(_view.getContext().getResources().getDrawable(R.color.card_background));

        _scaleIn = AnimationUtils.loadAnimation(view.getContext(), R.anim.item_scale_in);
        _scaleOut = AnimationUtils.loadAnimation(view.getContext(), R.anim.item_scale_out);

        _refresher = new UiRefresher(new UiRefresher.Listener() {
            @Override
            public void onRefresh() {
                if (!_hidden) {
                    refreshCode();
                }
            }

            @Override
            public long getMillisTillNextRefresh() {
                return ((TotpInfo) _entry.getInfo()).getMillisTillNextRotation();
            }
        });
    }

    public void setData(VaultEntry entry, int codeGroupSize, boolean showAccountName, boolean showProgress, boolean hidden, boolean dimmed) {
        _entry = entry;
        _hidden = hidden;

        if (codeGroupSize <= 0)
            throw new IllegalArgumentException("Code group size cannot be zero or negative");

        _codeGroupSize = codeGroupSize;

        _selected.clearAnimation();
        _selected.setVisibility(View.GONE);
        _selectedHandler.removeCallbacksAndMessages(null);
        _animationHandler.removeCallbacksAndMessages(null);

        // only show the progress bar if there is no uniform period and the entry type is TotpInfo
        setShowProgress(showProgress);

        // only show the button if this entry is of type HotpInfo
        _buttonRefresh.setVisibility(entry.getInfo() instanceof HotpInfo ? View.VISIBLE : View.GONE);

        String profileIssuer = entry.getIssuer();
        String profileName = showAccountName ? entry.getName() : "";
        if (!profileIssuer.isEmpty() && !profileName.isEmpty()) {
            profileName = String.format(" (%s)", profileName);
        }
        _profileIssuer.setText(profileIssuer);
        _profileName.setText(profileName);

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
            _selected.setVisibility(View.VISIBLE);
            _view.setBackgroundColor(ThemeHelper.getThemeColor(R.attr.cardBackgroundFocused, _view.getContext().getTheme()));
        } else {
            _view.setBackgroundColor(ThemeHelper.getThemeColor(R.attr.cardBackground, _view.getContext().getTheme()));
        }
        _view.setSelected(focused);
    }

    public void setFocusedAndAnimate(boolean focused) {
        setFocused(focused);

        if (focused) {
            _selected.startAnimation(_scaleIn);
        } else {
            _selected.startAnimation(_scaleOut);
            _selectedHandler.postDelayed(() -> _selected.setVisibility(View.GONE), 150);
        }
    }

    public void destroy() {
        _refresher.destroy();
    }

    public void startRefreshLoop() {
        _refresher.start();
        _progressBar.start();
    }

    public void stopRefreshLoop() {
        _refresher.stop();
        _progressBar.stop();
    }

    public void refresh() {
        _progressBar.restart();
        refreshCode();
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
                if (i != 0 && i % _codeGroupSize == 0) {
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

    public void animateCopyText() {
        _animationHandler.removeCallbacksAndMessages(null);

        Animation slideDownFadeIn = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.slide_down_fade_in);
        Animation slideDownFadeOut = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.slide_down_fade_out);
        Animation fadeOut = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.fade_out);
        Animation fadeIn = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.fade_in);

        _profileCopied.startAnimation(slideDownFadeIn);
        _description.startAnimation(slideDownFadeOut);

        _animationHandler.postDelayed(() -> {
            _profileCopied.startAnimation(fadeOut);
            _description.startAnimation(fadeIn);
        }, 3000);
    }

    private void animateAlphaTo(float alpha) {
        itemView.animate().alpha(alpha).setDuration(200).start();
    }
}
