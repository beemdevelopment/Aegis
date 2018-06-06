package me.impy.aegis.ui.views;

import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;

import me.impy.aegis.R;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.helpers.TextDrawableHelper;
import me.impy.aegis.helpers.UiRefresher;
import me.impy.aegis.otp.HotpInfo;
import me.impy.aegis.otp.OtpInfoException;
import me.impy.aegis.otp.TotpInfo;

public class EntryHolder extends RecyclerView.ViewHolder {
    private TextView _profileName;
    private TextView _profileCode;
    private TextView _profileIssuer;
    private ImageView _profileDrawable;
    private DatabaseEntry _entry;
    private ImageView _buttonRefresh;

    private PeriodProgressBar _progressBar;

    private UiRefresher _refresher;

    public EntryHolder(final View view) {
        super(view);
        _profileName = view.findViewById(R.id.profile_name);
        _profileCode = view.findViewById(R.id.profile_code);
        _profileIssuer = view.findViewById(R.id.profile_issuer);
        _profileDrawable = view.findViewById(R.id.ivTextDrawable);
        _buttonRefresh = view.findViewById(R.id.buttonRefresh);

        _progressBar = view.findViewById(R.id.progressBar);
        int primaryColorId = view.getContext().getResources().getColor(R.color.colorPrimary);
        _progressBar.getProgressDrawable().setColorFilter(primaryColorId, PorterDuff.Mode.SRC_IN);

        _refresher = new UiRefresher(new UiRefresher.Listener() {
            @Override
            public void onRefresh() {
                refreshCode();
                _progressBar.refresh();
            }

            @Override
            public long getMillisTillNextRefresh() {
                return ((TotpInfo)_entry.getInfo()).getMillisTillNextRotation();
            }
        });
    }

    public void setData(DatabaseEntry entry, boolean showIssuer, boolean showProgress) {
        _entry = entry;

        // only show the progress bar if there is no uniform period and the entry type is TotpInfo
        _progressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        if (showProgress) {
            _progressBar.setPeriod(((TotpInfo)entry.getInfo()).getPeriod());
        }

        // only show the button if this entry is of type HotpInfo
        _buttonRefresh.setVisibility(entry.getInfo() instanceof HotpInfo ? View.VISIBLE : View.GONE);

        _profileName.setText(entry.getName());
        _profileIssuer.setText("");
        if (showIssuer) {
            _profileIssuer.setText(" - " + entry.getIssuer());
        }

        TextDrawable drawable = TextDrawableHelper.generate(entry.getIssuer(), entry.getName());
        _profileDrawable.setImageDrawable(drawable);

        refreshCode();
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
        String otp = _entry.getInfo().getOtp();
        _profileCode.setText(otp.substring(0, otp.length() / 2) + " " + otp.substring(otp.length() / 2));
    }
}
