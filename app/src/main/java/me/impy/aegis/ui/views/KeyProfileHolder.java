package me.impy.aegis.ui.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;

import me.impy.aegis.R;
import me.impy.aegis.helpers.UIRefresher;

public class KeyProfileHolder extends RecyclerView.ViewHolder {
    private TextView _profileName;
    private TextView _profileCode;
    private TextView _profileIssuer;
    private ImageView _profileDrawable;
    private KeyProfile _profile;

    private PeriodProgressBar _progressBar;

    private UIRefresher _refresher;

    public KeyProfileHolder(final View view) {
        super(view);
        _profileName = view.findViewById(R.id.profile_name);
        _profileCode = view.findViewById(R.id.profile_code);
        _profileIssuer = view.findViewById(R.id.profile_issuer);
        _profileDrawable = view.findViewById(R.id.ivTextDrawable);

        _progressBar = view.findViewById(R.id.progressBar);
        int primaryColorId = view.getContext().getResources().getColor(R.color.colorPrimary);
        _progressBar.getProgressDrawable().setColorFilter(primaryColorId, PorterDuff.Mode.SRC_IN);

        _refresher = new UIRefresher(new UIRefresher.Listener() {
            @Override
            public void onRefresh() {
                refreshCode();
                _progressBar.refresh();
            }

            @Override
            public long getMillisTillNextRefresh() {
                return _profile.getEntry().getInfo().getMillisTillNextRotation();
            }
        });
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

        if (profile.getEntry().getInfo().getImage() != null)
        {
            byte[] imageBytes = profile.getEntry().getInfo().getImage();
            Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            _profileDrawable.setImageBitmap(image);
        }
        else
        {
            TextDrawable drawable = profile.getDrawable();
            _profileDrawable.setImageDrawable(drawable);
        }

        refreshCode();
    }

    public void startRefreshLoop() {
        _refresher.start();
    }

    public void stopRefreshLoop() {
        _refresher.stop();
    }

    private void refreshCode() {
        String otp = _profile.refreshCode();
        _profileCode.setText(otp.substring(0, otp.length() / 2) + " " + otp.substring(otp.length() / 2));
    }
}
