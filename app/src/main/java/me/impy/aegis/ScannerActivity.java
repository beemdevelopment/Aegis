package me.impy.aegis;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
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

public class ScannerActivity extends Activity implements ZXingScannerView.ResultHandler {
    private static final int CODE_ASK_PERMS = 0;

    private ZXingScannerView _scannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        _scannerView = new ZXingScannerView(this) {
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                return new SquareFinderView(context);
            }
        };

        setContentView(_scannerView);
        _scannerView.setFormats(Collections.singletonList(BarcodeFormat.QR_CODE));

        ActivityCompat.requestPermissions(ScannerActivity.this, new String[]{Manifest.permission.CAMERA}, CODE_ASK_PERMS);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CODE_ASK_PERMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    _scannerView.setResultHandler(this);
                    _scannerView.startCamera();
                } else {
                    Toast.makeText(ScannerActivity.this, "Permission denied to get access to the camera", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }
}
