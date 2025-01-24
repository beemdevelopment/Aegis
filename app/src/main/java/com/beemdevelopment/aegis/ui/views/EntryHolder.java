package com.beemdevelopment.aegis.ui.views;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.AccountNamePosition;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.helpers.AnimationsHelper;
import com.beemdevelopment.aegis.helpers.CenterVerticalSpan;
import com.beemdevelopment.aegis.helpers.SimpleAnimationEndListener;
import com.beemdevelopment.aegis.helpers.UiRefresher;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.otp.YandexInfo;
import com.beemdevelopment.aegis.ui.glide.GlideHelper;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

public class EntryHolder extends RecyclerView.ViewHolder {
    private static final float DEFAULT_ALPHA = 1.0f;
    private static final float DIMMED_ALPHA = 0.2f;
    private static final char HIDDEN_CHAR = '●';

    private View _favoriteIndicator;
    private TextView _profileName;
    private TextView _profileCode;
    private TextView _nextProfileCode;
    private TextView _profileIssuer;
    private TextView _profileCopied;
    private ImageView _profileDrawable;
    private VaultEntry _entry;
    private ImageView _buttonRefresh;
    private RelativeLayout _description;
    private ImageView _dragHandle;
    private ViewMode _viewMode;

    private final ImageView _selected;

    private Preferences.CodeGrouping _codeGrouping = Preferences.CodeGrouping.NO_GROUPING;
    private AccountNamePosition _accountNamePosition = AccountNamePosition.HIDDEN;

    private boolean _hidden;
    private boolean _paused;

    private TotpProgressBar _progressBar;
    private MaterialCardView _view;

    private UiRefresher _refresher;
    private Handler _copyAnimationHandler;
    private Handler _expirationHandler;
    private AnimatorSet _expirationAnimSet;
    private boolean _showNextCode;
    private boolean _showExpirationState;

    private Animation _scaleIn;
    private Animation _scaleOut;

    public EntryHolder(final View view) {
        super(view);

        _view = (MaterialCardView) view;
        _profileName = view.findViewById(R.id.profile_account_name);
        _profileCode = view.findViewById(R.id.profile_code);
        _nextProfileCode = view.findViewById(R.id.next_profile_code);
        _profileIssuer = view.findViewById(R.id.profile_issuer);
        _profileCopied = view.findViewById(R.id.profile_copied);
        _description = view.findViewById(R.id.description);
        _profileDrawable = view.findViewById(R.id.ivTextDrawable);
        _buttonRefresh = view.findViewById(R.id.buttonRefresh);
        _selected = view.findViewById(R.id.ivSelected);
        _dragHandle = view.findViewById(R.id.drag_handle);
        _favoriteIndicator = view.findViewById(R.id.favorite_indicator);

        _copyAnimationHandler = new Handler();
        _expirationHandler = new Handler();

        _progressBar = view.findViewById(R.id.progressBar);

        _scaleIn = AnimationsHelper.loadScaledAnimation(view.getContext(), R.anim.item_scale_in);
        _scaleOut = AnimationsHelper.loadScaledAnimation(view.getContext(), R.anim.item_scale_out);

        _refresher = new UiRefresher(new UiRefresher.Listener() {
            @Override
            public void onRefresh() {
                refreshCode();
            }

            @Override
            public long getMillisTillNextRefresh() {
                return ((TotpInfo) _entry.getInfo()).getMillisTillNextRotation();
            }
        });
    }

    public void setData(VaultEntry entry, Preferences.CodeGrouping groupSize, ViewMode viewMode, AccountNamePosition accountNamePosition, boolean showIcon, boolean showProgress, boolean hidden, boolean paused, boolean dimmed, boolean showExpirationState, boolean showNextCode) {
        _entry = entry;
        _hidden = hidden;
        _paused = paused;
        _codeGrouping = groupSize;
        _viewMode = viewMode;

        _accountNamePosition = accountNamePosition;
        if (viewMode.equals(ViewMode.TILES) && _accountNamePosition == AccountNamePosition.END) {
            _accountNamePosition = AccountNamePosition.BELOW;
        }

        _selected.clearAnimation();
        _selected.setVisibility(View.GONE);
        _copyAnimationHandler.removeCallbacksAndMessages(null);
        _expirationHandler.removeCallbacksAndMessages(null);
        _showNextCode = entry.getInfo() instanceof TotpInfo && showNextCode;
        _showExpirationState = _entry.getInfo() instanceof TotpInfo && showExpirationState;

        _favoriteIndicator.setVisibility(_entry.isFavorite() ? View.VISIBLE : View.INVISIBLE);

        // only show the progress bar if there is no uniform period and the entry type is TotpInfo
        setShowProgress(showProgress);

        // only show the button if this entry is of type HotpInfo
        _buttonRefresh.setVisibility(entry.getInfo() instanceof HotpInfo ? View.VISIBLE : View.GONE);
        _nextProfileCode.setVisibility(_showNextCode ? View.VISIBLE : View.GONE);

        String profileIssuer = entry.getIssuer();
        String profileName = entry.getName();
        if (!profileIssuer.isEmpty() && !profileName.isEmpty() && _accountNamePosition == AccountNamePosition.END) {
            profileName = _viewMode.getFormattedAccountName(profileName);
        }
        _profileIssuer.setText(profileIssuer);
        _profileName.setText(profileName);
        setAccountNameLayout(_accountNamePosition, !profileIssuer.isEmpty() && !profileName.isEmpty());

        if (_hidden) {
            hideCode();
        } else if (!_paused) {
            refreshCode();
        }

        showIcon(showIcon);
        itemView.setAlpha(dimmed ? DIMMED_ALPHA : DEFAULT_ALPHA);
    }

    private void setAccountNameLayout(AccountNamePosition accountNamePosition, Boolean hasBothIssuerAndName) {
        RelativeLayout.LayoutParams profileNameLayoutParams;

        switch (accountNamePosition) {
            case HIDDEN:
                _profileName.setVisibility(View.GONE);

                if (_viewMode == ViewMode.TILES) {
                    _profileCopied.setGravity(Gravity.CENTER_VERTICAL);
                    ((RelativeLayout.LayoutParams)_profileCopied.getLayoutParams()).removeRule(RelativeLayout.BELOW);
                    _profileCopied.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                    _profileCopied.setTextSize(14);

                    _profileIssuer.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                    _profileIssuer.setGravity(Gravity.CENTER_VERTICAL);
                    _profileIssuer.setTextSize(14);

                    _profileName.setVisibility(View.GONE);
                }

                break;

            case BELOW:
                profileNameLayoutParams = (RelativeLayout.LayoutParams) _profileName.getLayoutParams();
                profileNameLayoutParams.removeRule(RelativeLayout.END_OF);
                profileNameLayoutParams.addRule(RelativeLayout.BELOW, R.id.profile_issuer);
                profileNameLayoutParams.setMarginStart(0);
                _profileName.setLayoutParams(profileNameLayoutParams);
                _profileName.setVisibility(View.VISIBLE);
                break;

            case END:
            default:
                profileNameLayoutParams = (RelativeLayout.LayoutParams) _profileName.getLayoutParams();
                profileNameLayoutParams.addRule(RelativeLayout.END_OF, R.id.profile_issuer);
                profileNameLayoutParams.removeRule(RelativeLayout.BELOW);
                if (hasBothIssuerAndName) {
                    profileNameLayoutParams.setMarginStart(24);
                }
                _profileName.setLayoutParams(profileNameLayoutParams);
                _profileName.setVisibility(View.VISIBLE);
                break;
        }
    }

    public VaultEntry getEntry() {
        return _entry;
    }

    public void loadIcon(Fragment fragment) {
        GlideHelper.loadEntryIcon(Glide.with(fragment), _entry, _profileDrawable);
    }

    public ImageView getIconView() {
        return _profileDrawable;
    }

    public void setOnRefreshClickListener(View.OnClickListener listener) {
        _buttonRefresh.setOnClickListener(listener);
    }

    public void setShowDragHandle(boolean showDragHandle) {
        if (showDragHandle) {
            _dragHandle.setVisibility(View.VISIBLE);
        } else {
            _dragHandle.setVisibility(View.INVISIBLE);
        }
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
        }
        _view.setChecked(focused);
    }

    public void setFocusedAndAnimate(boolean focused) {
        setFocused(focused);

        if (focused) {
            _selected.startAnimation(_scaleIn);
        } else {
            _selected.startAnimation(_scaleOut);
            _scaleOut.setAnimationListener(new SimpleAnimationEndListener(animation -> {
                _selected.setVisibility(View.GONE);
            }));
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
        if (!_hidden && !_paused) {
            updateCodes();
            startExpirationAnimation();
        }
    }

    private void updateCodes() {
        _profileCode.setText(getOtp());

        if (_showNextCode) {
            _nextProfileCode.setText(getOtp(1));
        }
    }

    private String getOtp() {
        return getOtp(0);
    }

    private String getOtp(int offset) {
        OtpInfo info = _entry.getInfo();

        // In previous versions of Aegis, it was possible to import entries with an empty
        // secret. Attempting to generate OTP's for such entries would result in a crash.
        // In case we encounter an old entry that has this issue, we display "ERROR" as
        // the OTP, instead of crashing.
        String otp;
        try {
            if (info instanceof TotpInfo) {
                otp = ((TotpInfo)info).getOtp((System.currentTimeMillis() / 1000) + ((long) (offset) * ((TotpInfo) _entry.getInfo()).getPeriod()));
            } else {
                otp = info.getOtp();
            }

            if (!(info instanceof SteamInfo || info instanceof YandexInfo)) {
                otp = formatCode(otp);
            }
        } catch (OtpInfoException e) {
            otp = _view.getResources().getString(R.string.error_all_caps);
        }

        return otp;
    }

    private String formatCode(String code) {
        int groupSize;
        StringBuilder sb = new StringBuilder();

        switch (_codeGrouping) {
            case NO_GROUPING:
                groupSize = code.length();
                break;
            case HALVES:
                groupSize = (code.length() / 2) + (code.length() % 2);
                break;
            default:
                groupSize = _codeGrouping.getValue();
                if (groupSize <= 0) {
                    throw new IllegalArgumentException("Code group size cannot be zero or negative");
                }
        }

        for (int i = 0; i < code.length(); i++) {
            if (i != 0 && i % groupSize == 0) {
                sb.append(" ");
            }
            sb.append(code.charAt(i));
        }
        code = sb.toString();

        return code;
    }

    public void revealCode() {
        updateCodes();
        startExpirationAnimation();
        _hidden = false;
    }

    public void hideCode() {
        String code = getOtp();
        String hiddenText = code.replaceAll("\\S", Character.toString(HIDDEN_CHAR));
        stopExpirationAnimation();

        updateTextViewWithDots(_profileCode,  hiddenText, code);
        updateTextViewWithDots(_nextProfileCode,  hiddenText, code);

        _hidden = true;
    }

    private void updateTextViewWithDots(TextView textView, String hiddenCode, String code) {
        Paint paint = new Paint();
        paint.setTextSize(_profileCode.getTextSize());

        // Calculate the difference between the actual code width and the dots width
        float codeWidth = paint.measureText(code);
        float dotsWidth = paint.measureText(hiddenCode);
        float scaleFactor = codeWidth / dotsWidth;
        scaleFactor = (float)(Math.round(scaleFactor * 10.0) / 10.0);
        textView.setTextColor(MaterialColors.getColor(textView, R.attr.colorCodeHidden));

        // If scale is higher or equal to 0.8, do nothing and proceed with the normal text rendering
        if (scaleFactor >= 0.8) {
            textView.setText(hiddenCode);
            return;
        }

        // We need to use an invisible character in order to get the height of the profileCode textview consistent
        // Tokens without a space (ie Steam TOTP) will get misaligned without this
        SpannableString dotsString = new SpannableString("\u200B" + hiddenCode);

        // Only scale the digits/characters, skip the spaces
        int start = 1;
        for (int i = 0; i <= dotsString.length(); i++) {
            if (i == dotsString.length() || dotsString.charAt(i) == ' ') {
                if (i > start) {
                    dotsString.setSpan(new RelativeSizeSpan(scaleFactor), start, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                start = i + 1;
            }
        }

        Rect dotsRectBounds = new Rect();
        paint.getTextBounds(hiddenCode, 1, hiddenCode.length(), dotsRectBounds);

        // Use custom CenterVerticalSpan to make sure the dots are vertically aligned
        dotsString.setSpan(new CenterVerticalSpan(dotsRectBounds), 1, dotsString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(dotsString);
    }

    public void startExpirationAnimation() {
        stopExpirationAnimation();
        if (!_showExpirationState) {
            return;
        }

        final int totalStateDuration = 7000;
        TotpInfo info = (TotpInfo) _entry.getInfo();
        if (info.getPeriod() * 1000 < totalStateDuration) {
            _profileCode.setTextColor(MaterialColors.getColor(_profileCode, com.google.android.material.R.attr.colorError));
            return;
        }

        // Workaround for when animations are disabled or Android version being too old
        float durationScale = AnimationsHelper.Scale.ANIMATOR.getValue(itemView.getContext());
        if (durationScale == 0.0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            int color = MaterialColors.getColor(_profileCode, com.google.android.material.R.attr.colorError);
            if (info.getMillisTillNextRotation() < totalStateDuration) {
                _profileCode.setTextColor(color);
            } else {
                _expirationHandler.postDelayed(() -> {
                    _profileCode.setTextColor(color);
                }, info.getMillisTillNextRotation() - totalStateDuration);
            }

            return;
        }

        final int colorShiftDuration = 300;
        long delayAnimDuration = info.getPeriod() * 1000L - totalStateDuration - colorShiftDuration;
        ValueAnimator delayAnim = ValueAnimator.ofFloat(0f, 0f);
        delayAnim.setDuration((long) (delayAnimDuration / durationScale));

        int colorFrom = _profileCode.getCurrentTextColor();
        int colorTo = MaterialColors.getColor(_profileCode, com.google.android.material.R.attr.colorError);
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnim.setDuration((long) (colorShiftDuration / durationScale));
        colorAnim.addUpdateListener(a -> _profileCode.setTextColor((int) a.getAnimatedValue()));

        final int blinkDuration = 3000;
        ValueAnimator delayAnim2 = ValueAnimator.ofFloat(0f, 0f);
        delayAnim2.setDuration((long) ((totalStateDuration - blinkDuration) / durationScale));

        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(_profileCode, "alpha", 1f, .5f);
        alphaAnim.setDuration((long) (500 / durationScale));
        alphaAnim.setRepeatCount(blinkDuration / 500 - 1);
        alphaAnim.setRepeatMode(ValueAnimator.REVERSE);

        _expirationAnimSet = new AnimatorSet();
        _expirationAnimSet.playSequentially(delayAnim, colorAnim, delayAnim2, alphaAnim);
        _expirationAnimSet.start();
        long currentPlayTime = (info.getPeriod() * 1000L) - info.getMillisTillNextRotation();
        _expirationAnimSet.setCurrentPlayTime((long) (currentPlayTime / durationScale));
    }

    private void stopExpirationAnimation() {
        _expirationHandler.removeCallbacksAndMessages(null);
        if (_expirationAnimSet != null) {
            _expirationAnimSet.cancel();
            _expirationAnimSet = null;
        }

        int colorTo = MaterialColors.getColor(_profileCode, R.attr.colorCode);
        _profileCode.setTextColor(colorTo);
        _profileCode.setAlpha(1f);
    }

    public void showIcon(boolean show) {
        if (show) {
            _profileDrawable.setVisibility(View.VISIBLE);
        } else {
            _profileDrawable.setVisibility(View.GONE);
        }
    }

    public boolean isHidden() {
        return _hidden;
    }

    public void setPaused(boolean paused) {
        _paused = paused;

        if (_paused) {
            stopExpirationAnimation();
        } else if (!_hidden) {
            updateCodes();
            startExpirationAnimation();
        }
    }

    public void dim() {
        animateAlphaTo(DIMMED_ALPHA);
    }

    public void highlight() {
        animateAlphaTo(DEFAULT_ALPHA);
    }

    public void animateCopyText() {
        _copyAnimationHandler.removeCallbacksAndMessages(null);

        Animation slideDownFadeIn = AnimationsHelper.loadScaledAnimation(itemView.getContext(), R.anim.slide_down_fade_in);
        Animation slideDownFadeOut = AnimationsHelper.loadScaledAnimation(itemView.getContext(), R.anim.slide_down_fade_out);
        Animation fadeOut = AnimationsHelper.loadScaledAnimation(itemView.getContext(), R.anim.fade_out);
        Animation fadeIn = AnimationsHelper.loadScaledAnimation(itemView.getContext(), R.anim.fade_in);

        // Use slideDown animation when user is not using Tiles mode
        if (_viewMode != ViewMode.TILES) {
            _profileCopied.startAnimation(slideDownFadeIn);
            View fadeOutView = (_accountNamePosition == AccountNamePosition.BELOW) ? _profileName : _description;
            fadeOutView.startAnimation(slideDownFadeOut);

            _copyAnimationHandler.postDelayed(() -> {
                _profileCopied.startAnimation(fadeOut);
                fadeOutView.startAnimation(fadeIn);
            }, 3000);
        } else {
            View visibleProfileText = _accountNamePosition == AccountNamePosition.BELOW ? _profileName : _profileIssuer;

            _profileCopied.startAnimation(fadeIn);
            visibleProfileText.startAnimation(fadeOut);

            _copyAnimationHandler.postDelayed(() -> {
                _profileCopied.startAnimation(fadeOut);
                visibleProfileText.startAnimation(fadeIn);
            }, 3000);
        }
    }

    private void animateAlphaTo(float alpha) {
        itemView.animate().alpha(alpha).setDuration(200).start();
    }
}
