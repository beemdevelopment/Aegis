package me.impy.aegis;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.util.Collections;

import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.zxing.ZXingScannerView;
import me.impy.aegis.crypto.KeyInfo;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.helpers.SquareFinderView;

public class ScannerActivity extends AegisActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView _scannerView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        _scannerView = new ZXingScannerView(this) {
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                return new SquareFinderView(context);
            }
        };
        _scannerView.setFormats(Collections.singletonList(BarcodeFormat.QR_CODE));
        setContentView(_scannerView);
    }

    @Override
    protected void setPreferredTheme(boolean nightMode) {

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        _scannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        try {
            KeyInfo info = KeyInfo.fromURL(rawResult.getText());
            KeyProfile profile = new KeyProfile(new DatabaseEntry(info));

            Intent resultIntent = new Intent();
            resultIntent.putExtra("KeyProfile", profile);

            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "An error occurred while trying to parse the QR code contents", Toast.LENGTH_SHORT).show();
        }

        _scannerView.resumeCameraPreview(this);
    }
}
