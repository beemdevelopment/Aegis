package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransferEntriesActivity extends AegisActivity {
    private List<GoogleAuthInfo> _authInfos;
    private ImageView _qrImage;
    private TextView _issuer;
    private TextView _accountName;
    private TextView _entriesCount;
    private Button _nextButton;
    private Button _previousButton;

    private VaultManager _vault;
    private int _currentEntryCount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_entry);
        _vault = getApp().getVaultManager();

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
        _entriesCount.setVisibility(controlVisibility);
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
        _entriesCount.setText(String.format(getString(R.string.entries_count), _currentEntryCount, _authInfos.size()));

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = null;
        try {
            bitMatrix = writer.encode(selectedEntry.getUri().toString(), BarcodeFormat.QR_CODE, 512, 512);
        } catch (WriterException e) {
            Dialogs.showErrorDialog(this, R.string.unable_to_generate_qrcode, e);
            return;
        }

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        _qrImage.setImageBitmap(bitmap);
    }
}
