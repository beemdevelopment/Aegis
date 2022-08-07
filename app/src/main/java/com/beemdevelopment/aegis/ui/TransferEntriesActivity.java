package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.helpers.QrCodeHelper;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.google.zxing.WriterException;

import java.util.ArrayList;
import java.util.List;

public class TransferEntriesActivity extends AegisActivity {
    private List<GoogleAuthInfo> _authInfos;
    private ImageView _qrImage;
    private TextView _issuer;
    private TextView _accountName;
    private TextView _entriesCount;
    private Button _nextButton;
    private Button _previousButton;
    private int _currentEntryCount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }
        setContentView(R.layout.activity_share_entry);
        setSupportActionBar(findViewById(R.id.toolbar));

        _qrImage = findViewById(R.id.ivQrCode);
        _issuer = findViewById(R.id.tvIssuer);
        _accountName = findViewById(R.id.tvAccountName);
        _entriesCount = findViewById(R.id.tvEntriesCount);
        _nextButton = findViewById(R.id.btnNext);
        _previousButton = findViewById(R.id.btnPrevious);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        Intent intent = getIntent();
        _authInfos = (ArrayList<GoogleAuthInfo>) intent.getSerializableExtra("authInfos");

        int controlVisibility = _authInfos.size() != 1 ? View.VISIBLE : View.INVISIBLE;
        _nextButton.setVisibility(controlVisibility);

        _nextButton.setOnClickListener(v -> {
            if (_currentEntryCount < _authInfos.size()) {
                _previousButton.setVisibility(View.VISIBLE);
                _currentEntryCount++;
                generateQR();

                if (_currentEntryCount == _authInfos.size()) {
                    _nextButton.setText(R.string.done);
                }
            } else {
                finish();
            }
        });

        _previousButton.setOnClickListener(v -> {
            if (_currentEntryCount > 1 ) {
                _nextButton.setText(R.string.next);
                _currentEntryCount--;
                generateQR();

                if (_currentEntryCount == 1) {
                    _previousButton.setVisibility(View.INVISIBLE);
                }
            }
        });

        generateQR();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void generateQR() {
        GoogleAuthInfo selectedEntry = _authInfos.get(_currentEntryCount - 1);
        _issuer.setText(selectedEntry.getIssuer());
        _accountName.setText(selectedEntry.getAccountName());
        _entriesCount.setText(getResources().getQuantityString(R.plurals.entries_count, _authInfos.size(), _currentEntryCount, _authInfos.size()));

        @ColorInt int backgroundColor = Color.WHITE;
        if (getConfiguredTheme() == Theme.LIGHT) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.background, typedValue, true);
            backgroundColor = typedValue.data;
        }

        Bitmap bitmap;
        try {
            bitmap = QrCodeHelper.encodeToBitmap(selectedEntry.getUri().toString(), 512, 512, backgroundColor);
        } catch (WriterException e) {
            Dialogs.showErrorDialog(this, R.string.unable_to_generate_qrcode, e);
            return;
        }

        _qrImage.setImageBitmap(bitmap);
    }
}
