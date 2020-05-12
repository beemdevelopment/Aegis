package com.beemdevelopment.aegis.ui;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.helpers.SquareFinderView;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

public class ScannerActivity extends AegisActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView _scannerView;
    private Menu _menu;
    private int _facing = CAMERA_FACING_BACK;

    private int _batchId = 0;
    private int _batchIndex = -1;
    private List<VaultEntry> _entries;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        _entries = new ArrayList<>();
        _scannerView = new ZXingScannerView(this) {
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                return new SquareFinderView(context);
            }
        };
        _scannerView.setResultHandler(this);
        _scannerView.setFormats(Collections.singletonList(BarcodeFormat.QR_CODE));

        int camera = getRearCameraId();
        if (camera == -1) {
            camera = getFrontCameraId();
            if (camera == -1) {
                Toast.makeText(this, getString(R.string.no_cameras_available), Toast.LENGTH_LONG).show();
                finish();
            }
            _facing = CAMERA_FACING_FRONT;
        }
        _scannerView.startCamera(camera);

        setContentView(_scannerView);
    }

    @Override
    protected void setPreferredTheme(Theme theme) {
        setTheme(R.style.AppTheme_Fullscreen);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_scanner, menu);
        updateCameraIcon();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_camera:
                _scannerView.stopCamera();
                switch (_facing) {
                    case CAMERA_FACING_BACK:
                        _facing = CAMERA_FACING_FRONT;
                        break;
                    case CAMERA_FACING_FRONT:
                        _facing = CAMERA_FACING_BACK;
                        break;
                }
                updateCameraIcon();
                _scannerView.startCamera(getCameraId(_facing));
                return true;
            case R.id.action_lock:
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        _scannerView.startCamera(getCameraId(_facing));
    }

    @Override
    public void onPause() {
        super.onPause();
        _scannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        try {
            Uri uri = Uri.parse(rawResult.getText().trim());
            if (uri.getScheme() != null && uri.getScheme().equals(GoogleAuthInfo.SCHEME_EXPORT)) {
                handleExportUri(uri);
            } else {
                handleUri(uri);
            }

            _scannerView.resumeCameraPreview(this);
        } catch (GoogleAuthInfoException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.read_qr_error, e, (dialog, which) -> {
                _scannerView.resumeCameraPreview(this);
            });
        }
    }

    private void handleUri(Uri uri) throws GoogleAuthInfoException {
        GoogleAuthInfo info = GoogleAuthInfo.parseUri(uri);
        List<VaultEntry> entries = new ArrayList<>();
        entries.add(new VaultEntry(info));
        finish(entries);
    }

    private void handleExportUri(Uri uri) throws GoogleAuthInfoException {
        GoogleAuthInfo.Export export = GoogleAuthInfo.parseExportUri(uri);

        if (_batchId == 0) {
            _batchId = export.getBatchId();
        }

        int batchIndex = export.getBatchIndex();
        if (_batchId != export.getBatchId()) {
            Toast.makeText(this, R.string.google_qr_export_unrelated, Toast.LENGTH_SHORT).show();
        } else if (_batchIndex == -1 || _batchIndex == batchIndex - 1) {
            for (GoogleAuthInfo info : export.getEntries()) {
                VaultEntry entry = new VaultEntry(info);
                _entries.add(entry);
            }

            _batchIndex = batchIndex;
            if (_batchIndex + 1 == export.getBatchSize()) {
                finish(_entries);
            }

            Toast.makeText(this, getString(R.string.google_qr_export_scanned, _batchIndex + 1, export.getBatchSize()), Toast.LENGTH_SHORT).show();
        } else if (_batchIndex != batchIndex) {
            Toast.makeText(this, getString(R.string.google_qr_export_unexpected, _batchIndex + 1, batchIndex + 1), Toast.LENGTH_SHORT).show();
        }
    }

    private void finish(List<VaultEntry> entries) {
        Intent intent = new Intent();
        intent.putExtra("entries", (ArrayList<VaultEntry>) entries);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void updateCameraIcon() {
        if (_menu != null) {
            MenuItem item = _menu.findItem(R.id.action_camera);
            boolean dual = getFrontCameraId() != -1 && getRearCameraId() != -1;
            if (dual) {
                switch (_facing) {
                    case CAMERA_FACING_BACK:
                        item.setIcon(R.drawable.ic_camera_front_24dp);
                        break;
                    case CAMERA_FACING_FRONT:
                        item.setIcon(R.drawable.ic_camera_rear_24dp);
                        break;
                }
            }
            item.setVisible(dual);
        }
    }

    private static int getCameraId(int facing) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.getCameraInfo(i, info);
            if (info.facing == facing) {
                return i;
            }
        }

        return -1;
    }

    private static int getRearCameraId() {
        return getCameraId(CAMERA_FACING_BACK);
    }

    private static int getFrontCameraId() {
        return getCameraId(CAMERA_FACING_FRONT);
    }
}
