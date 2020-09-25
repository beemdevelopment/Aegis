package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.QrCodeAnalyzer;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ScannerActivity extends AegisActivity implements QrCodeAnalyzer.Listener {
    private ProcessCameraProvider _cameraProvider;
    private ListenableFuture<ProcessCameraProvider> _cameraProviderFuture;

    private List<Integer> _lenses;
    private int _currentLens;

    private Menu _menu;
    private PreviewView _previewView;

    private int _batchId = 0;
    private int _batchIndex = -1;
    private List<VaultEntry> _entries;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        _entries = new ArrayList<>();
        _lenses = new ArrayList<>();
        _previewView = findViewById(R.id.preview_view);

        _cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        _cameraProviderFuture.addListener(() -> {
            try {
                _cameraProvider = _cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                // if we're to believe the Android documentation, this should never happen
                // https://developer.android.com/training/camerax/preview#check-provider
                throw new RuntimeException(e);
            }

            addCamera(CameraSelector.LENS_FACING_BACK);
            addCamera(CameraSelector.LENS_FACING_FRONT);
            if (_lenses.size() == 0) {
                Toast.makeText(this, getString(R.string.no_cameras_available), Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            _currentLens = _lenses.get(0);
            updateCameraIcon();

            bindPreview(_cameraProvider);
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onSetTheme() {
        setTheme(R.style.AppTheme_Fullscreen);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_scanner, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_camera) {
            _cameraProvider.unbindAll();
            _currentLens = _currentLens == CameraSelector.LENS_FACING_BACK ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            bindPreview(_cameraProvider);
            updateCameraIcon();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addCamera(int lens) {
        try {
            CameraSelector camera = new CameraSelector.Builder().requireLensFacing(lens).build();
            if (_cameraProvider.hasCamera(camera)) {
                _lenses.add(lens);
            }
        } catch (CameraInfoUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void updateCameraIcon() {
        if (_menu != null) {
            MenuItem item = _menu.findItem(R.id.action_camera);
            boolean dual = _lenses.size() > 1;
            if (dual) {
                switch (_currentLens) {
                    case CameraSelector.LENS_FACING_BACK:
                        item.setIcon(R.drawable.ic_camera_front_24dp);
                        break;
                    case CameraSelector.LENS_FACING_FRONT:
                        item.setIcon(R.drawable.ic_camera_rear_24dp);
                        break;
                }
            }
            item.setVisible(dual);
        }
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(_previewView.createSurfaceProvider());

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(_currentLens)
                .build();

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analysis.setAnalyzer(ContextCompat.getMainExecutor(this), new QrCodeAnalyzer(this));

        cameraProvider.bindToLifecycle(this, selector, preview, analysis);
    }

    @Override
    public void onQrCodeDetected(Result result) {
        try {
            Uri uri = Uri.parse(result.getText().trim());
            if (uri.getScheme() != null && uri.getScheme().equals(GoogleAuthInfo.SCHEME_EXPORT)) {
                handleExportUri(uri);
            } else {
                handleUri(uri);
            }
        } catch (GoogleAuthInfoException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.read_qr_error, e, ((dialog, which) -> bindPreview(_cameraProvider)));
            _cameraProvider.unbindAll();
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

            Toast.makeText(this, getResources().getQuantityString(R.plurals.google_qr_export_scanned, export.getBatchSize(), _batchIndex + 1, export.getBatchSize()), Toast.LENGTH_SHORT).show();
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
}
