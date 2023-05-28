package com.beemdevelopment.aegis.ui;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.helpers.QrCodeHelper;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.otp.Transferable;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.google.zxing.WriterException;
import java.util.ArrayList;
import java.util.List;

public class TransferEntriesActivity extends AegisActivity {

    private List<Transferable> _authInfos;

    private ImageView _qrImage;

    private TextView _description;

    private TextView _issuer;

    private TextView _accountName;

    private TextView _entriesCount;

    private Button _nextButton;

    private Button _previousButton;

    private Button _copyButton;

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
        _description = findViewById(R.id.tvDescription);
        _issuer = findViewById(R.id.tvIssuer);
        _accountName = findViewById(R.id.tvAccountName);
        _entriesCount = findViewById(R.id.tvEntriesCount);
        _nextButton = findViewById(R.id.btnNext);
        _previousButton = findViewById(R.id.btnPrevious);
        _copyButton = findViewById(R.id.btnCopyClipboard);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        Intent intent = getIntent();
        _authInfos = (ArrayList<Transferable>) intent.getSerializableExtra("authInfos");
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
            if (_currentEntryCount > 1) {
                _nextButton.setText(R.string.next);
                _currentEntryCount--;
                generateQR();
                if (_currentEntryCount == 1) {
                    _previousButton.setVisibility(View.INVISIBLE);
                }
            }
        });
        if (_authInfos.get(0) instanceof GoogleAuthInfo) {
            _copyButton.setVisibility(View.VISIBLE);
        }
        _copyButton.setOnClickListener(v -> {
            Transferable selectedEntry = _authInfos.get(_currentEntryCount - 1);
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text/plain", selectedEntry.getUri().toString());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    PersistableBundle extras = new PersistableBundle();
                    extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
                    clip.getDescription().setExtras(extras);
                }
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }
                Toast.makeText(this, R.string.uri_copied_to_clipboard, Toast.LENGTH_SHORT).show();
            } catch (GoogleAuthInfoException e) {
                Dialogs.showErrorDialog(this, R.string.unable_to_copy_uri_to_clipboard, e);
            }
        });
        generateQR();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void generateQR() {
        Transferable selectedEntry = _authInfos.get(_currentEntryCount - 1);
        if (selectedEntry instanceof GoogleAuthInfo) {
            GoogleAuthInfo entry = (GoogleAuthInfo) selectedEntry;
            _issuer.setText(entry.getIssuer());
            _accountName.setText(entry.getAccountName());
        } else if (selectedEntry instanceof GoogleAuthInfo.Export) {
            _description.setText(R.string.google_auth_compatible_transfer_description);
        }
        _entriesCount.setText(getResources().getQuantityString(R.plurals.qr_count, _authInfos.size(), _currentEntryCount, _authInfos.size()));
        @ColorInt
        int backgroundColor = Color.WHITE;
        if (getConfiguredTheme() == Theme.LIGHT) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.background, typedValue, true);
            backgroundColor = typedValue.data;
        }
        Bitmap bitmap;
        try {
            bitmap = QrCodeHelper.encodeToBitmap(selectedEntry.getUri().toString(), 512, 512, backgroundColor);
        } catch (WriterException | GoogleAuthInfoException e) {
            Dialogs.showErrorDialog(this, R.string.unable_to_generate_qrcode, e);
            return;
        }
        _qrImage.setImageBitmap(bitmap);
    }
}
